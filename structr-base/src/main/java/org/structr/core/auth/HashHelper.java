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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Password hashing utility supporting Argon2id (preferred) and legacy SHA-512.
 *
 * New passwords are always hashed with Argon2id. Legacy SHA-512 hashes are
 * recognized and verified for backward compatibility, enabling transparent
 * migration on login.
 *
 * Argon2id parameters (OWASP 2023 recommendation):
 *   memory: 64 MB (65536 KB)
 *   iterations: 3
 *   parallelism: 1
 *   salt: 16 bytes from SecureRandom
 *   hash length: 32 bytes
 *
 * Output format: $argon2id$v=19$m=65536,t=3,p=1$<base64-salt>$<base64-hash>
 */
public class HashHelper {

	private static final Logger logger = LoggerFactory.getLogger(HashHelper.class);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	// see getVerificationCount()
	private static final AtomicLong VERIFICATION_COUNT = new AtomicLong();

	private static final String ARGON2_PREFIX = "$argon2id$";

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
		final Argon2BytesGenerator generator = new Argon2BytesGenerator();

		generator.init(params);
		generator.generateBytes(password.toCharArray(), hash);

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
	 * Spend the same verification work as a real password check, and return false.
	 *
	 * Call this on a login path that has NO principal to check against, so that a failed attempt
	 * costs the same whether or not the account exists. Without it, "no such user" returns
	 * immediately while an existing user pays for a full Argon2 verification (64 MB, 3 iterations
	 * here), and that difference is easily measurable: an attacker can enumerate valid user names by
	 * timing alone, even though both cases answer with the same error message. Structr is open
	 * source, so the shape of that difference is not a secret and must not be relied upon.
	 *
	 * @param password the cleartext password that was offered, may be null
	 * @return always false, so this can be used directly in place of a verification result
	 */
	public static boolean spendVerificationTime(final String password) {

		verifyArgon2(password != null ? password : "", getTimingReferenceHash());

		return false;
	}

	/**
	 * A hash of a random value, generated once, purely to give
	 * {@link #spendVerificationTime(String)} something realistic to verify against. It never
	 * matches any password, and is created lazily so that starting the server does not pay for it.
	 */
	private static String getTimingReferenceHash() {

		String hash = timingReferenceHash;
		if (hash == null) {

			synchronized (HashHelper.class) {

				hash = timingReferenceHash;

				if (hash == null) {

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

			final String[] parts = encodedHash.split("\\$");
			// Format: $argon2id$v=19$m=65536,t=3,p=1$<salt>$<hash>
			// parts[0] = "" (before first $)
			// parts[1] = "argon2id"
			// parts[2] = "v=19"
			// parts[3] = "m=65536,t=3,p=1"
			// parts[4] = base64 salt
			// parts[5] = base64 hash

			if (parts.length != 6) {

				logger.warn("Invalid Argon2 hash format: unexpected number of segments");

				return false;
			}

			final String paramString = parts[3];
			int memory = 0, iterations = 0, parallelism = 0;

			for (final String param : paramString.split(",")) {

				final String[] kv = param.split("=");
				switch (kv[0]) {
					case "m" -> memory      = Integer.parseInt(kv[1]);
					case "t" -> iterations   = Integer.parseInt(kv[1]);
					case "p" -> parallelism  = Integer.parseInt(kv[1]);
				}
			}

			final byte[] salt         = Base64.getDecoder().decode(parts[4]);
			final byte[] expectedHash = Base64.getDecoder().decode(parts[5]);
			final Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
				.withSalt(salt)
				.withMemoryAsKB(memory)
				.withIterations(iterations)
				.withParallelism(parallelism)
				.build();

			final byte[] computedHash = new byte[expectedHash.length];
			final Argon2BytesGenerator generator = new Argon2BytesGenerator();

			generator.init(params);
			generator.generateBytes(password.toCharArray(), computedHash);

			// Constant-time comparison

			return java.security.MessageDigest.isEqual(expectedHash, computedHash);

		} catch (Exception e) {

			logger.warn("Error verifying Argon2 hash: {}", e.getMessage());

			return false;
		}
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
}
