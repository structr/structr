/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Password hashing utility supporting Argon2id (preferred) and legacy SHA-512.
 *
 * New passwords are always hashed with Argon2id. Legacy SHA-512 hashes, and
 * Argon2id hashes made with parameters other than the configured ones, still
 * verify; the next successful login replaces them, see {@link #needsRehash(String)}.
 *
 * Default Argon2id parameters, above every minimum configuration that OWASP
 * recommends, and configurable in the security.passwordhash.* settings:
 *   memory: 64 MB (65536 KB)
 *   iterations: 3
 *   parallelism: 1
 *   salt: 16 bytes from SecureRandom
 *   hash length: 32 bytes
 *
 * Every Argon2id computation holds its full memory while it runs, so the number
 * of concurrent computations is limited, see {@link #getMaxConcurrentComputations()}.
 *
 * Output format: $argon2id$v=19$m=65536,t=3,p=1$<base64-salt>$<base64-hash>
 */
public class HashHelper {

	private static final Logger logger = LoggerFactory.getLogger(HashHelper.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// see getVerificationCount()
	private static final AtomicLong VERIFICATION_COUNT = new AtomicLong();

	private static final String ARGON2_PREFIX = "$argon2id$";

	// replaced when the limit changes, see acquireComputationPermit(); its own lock, because
	// getTimingReferenceHash() waits for a permit while it holds the class monitor
	private static final Object COMPUTATION_LIMIT_LOCK        = new Object();
	private static volatile ComputationLimit computationLimit = null;

	// ----- Argon2id methods -----

	/**
	 * Hash a password with Argon2id. Returns a self-contained string
	 * including algorithm, parameters, salt, and hash.
	 *
	 * @param password the cleartext password
	 * @return encoded hash string in PHC format
	 */
	public static String hashPassword(final String password) {

		final byte[] salt = new byte[Settings.PasswordHashArgon2idSaltLength.getValue()];
		SECURE_RANDOM.nextBytes(salt);

		final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
			.withSalt(salt)
			.withMemoryAsKB(Settings.PasswordHashArgon2idMemory.getValue())
			.withIterations(Settings.PasswordHashArgon2idIterations.getValue())
			.withParallelism(Settings.PasswordHashArgon2idParallelism.getValue())
			.build();

		final byte[] hash = new byte[Settings.PasswordHashArgon2idHashLength.getValue()];

		computeArgon2(params, password, hash);

		return encodeArgon2Hash(params, salt, hash);
	}

	/**
	 * Verify a password against a stored hash. Automatically detects
	 * whether the hash is Argon2id or legacy SHA-512.
	 *
	 * @param password the cleartext password to verify
	 * @param storedHash the stored hash string
	 * @param legacySalt the legacy salt (only used for SHA-512 hashes, may be null)
	 * @return true if the password matches
	 */
	public static boolean verifyPassword(final String password, final String storedHash, final String legacySalt) {

		if (storedHash == null || password == null) {

			return false;
		}

		if (isArgon2Hash(storedHash)) {

			return verifyArgon2(password, storedHash);
		}

		// Legacy SHA-512 verification
		if (legacySalt != null) {

			return storedHash.equals(getHash(password, legacySalt));
		}

		// Very old unsalted SHA-512

		return storedHash.equals(getSimpleHash(password));
	}

	/**
	 * Check if a hash string is in Argon2 format.
	 */
	public static boolean isArgon2Hash(final String hash) {

		return hash != null && hash.startsWith(ARGON2_PREFIX);
	}

	/**
	 * Whether a stored hash should be replaced by a fresh one on the next successful login: it is a
	 * legacy SHA-512 hash, or an Argon2id hash whose parameters differ from the configured ones.
	 * The parameters are part of every Argon2id hash, so a hash made before a settings change still
	 * verifies; re-hashing is what makes the new settings reach existing accounts.
	 *
	 * @param storedHash the stored hash string
	 * @return true if the hash does not match the current configuration
	 */
	public static boolean needsRehash(final String storedHash) {

		if (!isArgon2Hash(storedHash)) {

			return true;
		}

		final Argon2Hash parsed = parseArgon2Hash(storedHash);
		if (parsed == null) {

			return true;
		}

		return parsed.memory()      != Settings.PasswordHashArgon2idMemory.getValue()
			|| parsed.iterations()  != Settings.PasswordHashArgon2idIterations.getValue()
			|| parsed.parallelism() != Settings.PasswordHashArgon2idParallelism.getValue()
			|| parsed.salt().length != Settings.PasswordHashArgon2idSaltLength.getValue()
			|| parsed.hash().length != Settings.PasswordHashArgon2idHashLength.getValue();
	}

	/**
	 * The number of Argon2id computations allowed to run at the same time.
	 *
	 * Each computation allocates its full memory parameter (64 MB by default) up front, so a few dozen
	 * simultaneous logins exhaust a small heap with an OutOfMemoryError. The automatic value allows one
	 * computation per processor, since more than that only makes each one slower, but no more than
	 * fit into a quarter of the maximum heap.
	 */
	public static int getMaxConcurrentComputations() {

		final int configured = Settings.PasswordHashMaxConcurrent.getValue();
		if (configured > 0) {

			return configured;
		}

		final Runtime runtime   = Runtime.getRuntime();
		final long memoryBytes  = Math.max(1L, Settings.PasswordHashArgon2idMemory.getValue()) * 1024L;
		final long fittingCount = (runtime.maxMemory() / 4L) / memoryBytes;

		return (int)Math.max(1L, Math.min(runtime.availableProcessors(), fittingCount));
	}

	/**
	 * Spend the same verification work as a real password check, and return false.
	 *
	 * Call this on a login path that has NO principal to check against, so that a failed attempt
	 * costs the same whether or not the account exists. Without it, "no such user" returns
	 * immediately while an existing user pays for a full Argon2 verification (64 MB and 3
	 * iterations by default), and that difference is easily measurable: an attacker can enumerate
	 * valid user names by timing alone, even though both cases answer with the same error message.
	 * Structr is open source, so the shape of that difference is not a secret and must not be
	 * relied upon.
	 *
	 * @param password the cleartext password that was offered, may be null
	 * @return always false, so this can be used directly in place of a verification result
	 */
	public static boolean spendVerificationTime(final String password) {

		verifyArgon2(password != null ? password : "", getTimingReferenceHash());

		return false;
	}

	/**
	 * A hash of a random value, purely to give {@link #spendVerificationTime(String)} something
	 * realistic to verify against. It never matches any password, and is created lazily so that
	 * starting the server does not pay for it. A change of the hash settings replaces it, so that it
	 * keeps costing what an account costs once the next login has re-hashed it.
	 */
	private static String getTimingReferenceHash() {

		String hash = timingReferenceHash;
		if (hash == null || needsRehash(hash)) {

			synchronized (HashHelper.class) {

				hash = timingReferenceHash;

				if (hash == null || needsRehash(hash)) {

					final byte[] random = new byte[32];
					SECURE_RANDOM.nextBytes(random);

					hash                 = hashPassword(java.util.Base64.getEncoder().encodeToString(random));
					timingReferenceHash  = hash;
				}
			}
		}

		return hash;
	}

	private static volatile String timingReferenceHash = null;

	// ----- Legacy SHA-512 methods (kept for backward compatibility) -----

	/**
	 * Calculate a SHA-512 hash of the given password string.
	 * If salt is given, use salt.
	 *
	 * @param password
	 * @param salt
	 * @return hash
	 * @deprecated Use {@link #hashPassword(String)} for new passwords
	 */
	@Deprecated
	public static String getHash(final String password, final String salt) {

		if (StringUtils.isEmpty(salt)) {

			return getSimpleHash(password);
		}

		return DigestUtils.sha512Hex(DigestUtils.sha512Hex(password).concat(salt));
	}

	/**
	 * Calculate a SHA-512 hash without salt.
	 *
	 * @param password
	 * @return simple hash
	 * @deprecated Use {@link #hashPassword(String)} instead
	 */
	@Deprecated
	public static String getSimpleHash(final String password) {

		return DigestUtils.sha512Hex(password);
	}

	/**
	 * Number of Argon2 verifications performed since the counter was last reset.
	 *
	 * A failed login must cost exactly one verification whatever the reason it failed, otherwise the
	 * cheaper cases are identifiable by how fast they answer, see
	 * {@link #spendVerificationTime(String)}. That invariant is what tests assert: counting the
	 * operations is deterministic, while measuring elapsed time is not and would make the test flaky
	 * enough to be ignored. Not a metric, and not synchronized beyond the atomic itself.
	 */
	public static long getVerificationCount() {

		return VERIFICATION_COUNT.get();
	}

	/**
	 * Resets {@link #getVerificationCount()}, for a test that is about to make one attempt.
	 */
	public static void resetVerificationCount() {

		VERIFICATION_COUNT.set(0);
	}

	// ----- private methods -----

	private static boolean verifyArgon2(final String password, final String encodedHash) {

		VERIFICATION_COUNT.incrementAndGet();

		try {

			final Argon2Hash parsed = parseArgon2Hash(encodedHash);
			if (parsed == null) {

				logger.warn("Invalid Argon2 hash format");

				return false;
			}

			final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withSalt(parsed.salt())
				.withMemoryAsKB(parsed.memory())
				.withIterations(parsed.iterations())
				.withParallelism(parsed.parallelism())
				.build();

			final byte[] computedHash = new byte[parsed.hash().length];

			computeArgon2(params, password, computedHash);

			// Constant-time comparison

			return java.security.MessageDigest.isEqual(parsed.hash(), computedHash);

		} catch (Exception e) {

			logger.warn("Error verifying Argon2 hash: {}", e.getMessage());

			return false;
		}
	}

	/**
	 * Splits an encoded Argon2id hash into its parameters, salt and hash, or returns null if it is
	 * malformed.
	 */
	private static Argon2Hash parseArgon2Hash(final String encodedHash) {

		final String[] parts = encodedHash.split("\\$");
		// Format: $argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>
		// parts[0] = "" (before first $)
		// parts[1] = "argon2id"
		// parts[2] = "v=19"
		// parts[3] = "m=65536,t=3,p=1"
		// parts[4] = base64 salt
		// parts[5] = base64 hash

		if (parts.length != 6) {

			return null;
		}

		try {

			int memory = 0, iterations = 0, parallelism = 0;

			for (final String param : parts[3].split(",")) {

				final String[] kv = param.split("=");
				switch (kv[0]) {
					case "m" -> memory      = Integer.parseInt(kv[1]);
					case "t" -> iterations   = Integer.parseInt(kv[1]);
					case "p" -> parallelism  = Integer.parseInt(kv[1]);
				}
			}

			final byte[] salt = Base64.getDecoder().decode(parts[4]);
			final byte[] hash = Base64.getDecoder().decode(parts[5]);

			return new Argon2Hash(memory, iterations, parallelism, salt, hash);

		} catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {

			// NumberFormatException is an IllegalArgumentException, as is invalid base64

			return null;
		}
	}

	/**
	 * Runs one Argon2id computation once a permit is free. A caller waits rather than being refused:
	 * refusing a login would count against the account as a failed attempt and could block it.
	 */
	private static void computeArgon2(final Argon2Parameters params, final String password, final byte[] output) {

		final Semaphore permits = acquireComputationPermit();

		try {

			final Argon2BytesGenerator generator = new Argon2BytesGenerator();

			generator.init(params);
			generator.generateBytes(password.toCharArray(), output);

		} finally {

			permits.release();
		}
	}

	/**
	 * Acquires a permit and returns the semaphore it came from, which is the one to release it to. A
	 * change of the limit replaces the semaphore. Computations still holding a permit of the old one
	 * release it there and are not counted by the new one, so until they finish, more computations
	 * than the new limit can run at the same time.
	 */
	private static Semaphore acquireComputationPermit() {

		final int max           = getMaxConcurrentComputations();
		ComputationLimit limit  = computationLimit;

		if (limit == null || limit.max() != max) {

			synchronized (COMPUTATION_LIMIT_LOCK) {

				limit = computationLimit;

				if (limit == null || limit.max() != max) {

					limit            = new ComputationLimit(max, new Semaphore(max, true));
					computationLimit = limit;
				}
			}
		}

		limit.semaphore().acquireUninterruptibly();

		return limit.semaphore();
	}

	private static String encodeArgon2Hash(final Argon2Parameters params, final byte[] salt, final byte[] hash) {

		final StringBuilder sb = new StringBuilder();
		sb.append("$argon2id$v=").append(params.getVersion());
		sb.append("$m=").append(params.getMemory());
		sb.append(",t=").append(params.getIterations());
		sb.append(",p=").append(params.getLanes());
		sb.append("$").append(Base64.getEncoder().withoutPadding().encodeToString(salt));
		sb.append("$").append(Base64.getEncoder().withoutPadding().encodeToString(hash));

		return sb.toString();
	}

	private record Argon2Hash(int memory, int iterations, int parallelism, byte[] salt, byte[] hash) {}

	private record ComputationLimit(int max, Semaphore semaphore) {}
}
