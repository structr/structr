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
package org.structr.core.function;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import javax.crypto.spec.GCMParameterSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public abstract class CryptFunction extends AdvancedScriptingFunction {

	/**
	 * Where set_encryption_key() puts the key it was given: in the context of the evaluation that called
	 * it, not in a field of this class.
	 *
	 * <p>Ticket 1601: the key used to be a public static byte array, and set_encryption_key() replaced it
	 * for the whole process - so one script setting a key changed which key every other request was
	 * encrypting with at that moment, and the last writer won. A thread-local would trade that for
	 * something worse, because request threads are pooled and the key would outlive the request that set
	 * it. The evaluation's own context ends when the request does.
	 */
	public static final String CONTEXT_KEY = "structr.encryption.key";

	public static final String CHARSET    = "UTF-8";
	public static final String HASH_ALGO  = "MD5";
	public static final String BASE_ALGO  = "AES";
	public static final String CRYPT_ALGO = "AES/GCM/NoPadding";

	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	/**
	 * The scheme that derives the key with PBKDF2 and a salt of its own per value. Chosen by name as the
	 * first argument of encrypt() and decrypt().
	 */
	public static final String SCHEME_PBKDF2 = "aes-gcm-pbkdf2";

	/**
	 * What encrypt() and decrypt() did before there was a choice: the key is the MD5 digest of the
	 * passphrase, unsalted and unstretched. Kept so that values encrypted that way stay readable, and
	 * named so that choosing it is a decision rather than the default nobody noticed (ticket 1601).
	 */
	public static final String SCHEME_LEGACY = "legacy";

	private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA512";
	private static final int PBKDF2_SALT_LENGTH = 16;
	private static final int PBKDF2_KEY_LENGTH = 256;

	/* The cost is the point. What ticket 1601 describes is an attacker with a database dump and a
	   ciphertext, guessing a passphrase offline: against an MD5 digest that is millions of guesses a
	   second, and every added round costs the attacker exactly what it costs this instance. Six figures
	   is where current guidance for PBKDF2 sits, and it puts a single derivation in the range of a
	   tenth of a second - noticeable in a loop over many values, which is the trade being made and the
	   reason the legacy scheme is still selectable. */
	private static final int PBKDF2_ITERATIONS = 210000;

	// ----- public static methods -----
	/**
	 * The digest of the given passphrase, as the previous implementation derived it. Public because the
	 * key is no longer held anywhere: callers that have a passphrase pass it in, and there is nothing to
	 * set.
	 */
	public static byte[] keyHashOf(final String key) {

		try {

			return MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes(CHARSET));

		} catch (Throwable t) {

			logger.error("Unable to derive secret key: {}", t.getMessage());
		}

		return null;
	}

	/**
	 * Encrypts with the named scheme. {@link #SCHEME_PBKDF2} derives the key from the passphrase with
	 * PBKDF2 and a random salt, which is stored in front of the ciphertext so that decryption can repeat
	 * the derivation; {@link #SCHEME_LEGACY} keeps the unsalted MD5 digest the function used before.
	 */
	public static String encrypt(final String scheme, final String clearText, final String key) throws FrameworkException {

		if (SCHEME_LEGACY.equals(scheme)) {

			return encrypt(clearText, key);
		}

		assertKnownScheme(scheme);

		try {

			final byte[] salt = new byte[PBKDF2_SALT_LENGTH];

			new SecureRandom().nextBytes(salt);

			final String ciphertext = encryptWithKeyHash(clearText, deriveKey(key, salt));
			if (ciphertext == null) {

				return null;
			}

			/* The salt travels with the value because it has to: it is different for every ciphertext -
			   that is what a salt is for - and decryption cannot repeat the derivation without it. It is
			   not a secret, only a guarantee that two values encrypted with the same passphrase do not
			   share a key. */
			final byte[] body   = Base64.getDecoder().decode(ciphertext);
			final ByteBuffer buf = ByteBuffer.allocate(salt.length + body.length);

			buf.put(salt);
			buf.put(body);

			return Base64.getEncoder().encodeToString(buf.array());

		} catch (Throwable t) {

			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	/**
	 * Decrypts a value produced by {@link #encrypt(String, String, String)} with the same scheme.
	 */
	public static String decrypt(final String scheme, final String encryptedText, final String key) throws FrameworkException {

		if (SCHEME_LEGACY.equals(scheme)) {

			return decrypt(encryptedText, key);
		}

		assertKnownScheme(scheme);

		try {

			final ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedText));
			final byte[] salt       = new byte[PBKDF2_SALT_LENGTH];

			buffer.get(salt);

			final byte[] body = new byte[buffer.remaining()];

			buffer.get(body);

			return decryptWithKeyHash(Base64.getEncoder().encodeToString(body), deriveKey(key, salt));

		} catch (Throwable t) {

			logger.error("Unable to decrypt ciphertext: {}: {}", t.getClass().getSimpleName(), t.getMessage());
		}

		return null;
	}

	/**
	 * The schemes encrypt() and decrypt() accept, for the error message that lists them.
	 */
	public static String getKnownSchemes() {

		return SCHEME_PBKDF2 + ", " + SCHEME_LEGACY;
	}

	public static boolean isKnownScheme(final String scheme) {

		return SCHEME_PBKDF2.equals(scheme) || SCHEME_LEGACY.equals(scheme);
	}

	public static String encrypt(final String clearText, final String key) throws FrameworkException {

		try {

			return encryptWithKeyHash(clearText, MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes(CHARSET)));

		} catch (Throwable t) {

			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	/**
	 * Encrypts with the key the installation is configured with, and with no other.
	 *
	 * <p>Deliberately no way to pass one in: this is the path an EncryptedString property takes, and a
	 * property is a statement about data at rest. A key that came from the request would make the same
	 * stored bytes readable or unreadable depending on what the caller did a moment earlier - write under
	 * a key set in a script, read back in the next request without it, get null, and the value is gone
	 * the moment anybody saves the record again. set_encryption_key() is callable by whoever may write a
	 * script; the secret in structr.conf is the only key that belongs to the installation. A script that
	 * wants a key of its own has encrypt() for that, and then owns the consequences.
	 */
	public static String encrypt(final String clearText) throws FrameworkException {

		// no secret configured yet: generate and persist one on first use so encrypted
		// properties work out of the box (only reached when there is no prior encrypted data)
		final byte[] keyHash = configuredKeyHash(true);
		if (keyHash == null) {

			throw new FrameworkException(422, "Unable to encrypt data, no secret key set.");
		}

		return encryptWithKeyHash(clearText, keyHash);
	}

	public static String encryptWithKeyHash(final String clearText, final byte[] keyHash) {

		try {

			final SecretKeySpec skeySpec   = new SecretKeySpec(keyHash, BASE_ALGO);
			final Cipher cipher            = Cipher.getInstance(CRYPT_ALGO);
			final byte[] iv                = new byte[GCM_IV_LENGTH];
			final SecureRandom random      = new SecureRandom();

			random.nextBytes(iv);
			final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec);

			final byte[] ciphertext = cipher.doFinal(clearText.getBytes(CHARSET));
			final ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);

			buffer.put(iv);
			buffer.put(ciphertext);

			return Base64.getEncoder().encodeToString(buffer.array());

		} catch (Throwable t) {

			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	public static String decrypt(final String encryptedText, final String key) {

		try {

			return decryptWithKeyHash(encryptedText, MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes(CHARSET)));

		} catch (Throwable t) {

			logger.error("Unable to decrypt ciphertext: {}: {}", t.getClass().getSimpleName(), t.getMessage());
		}

		return null;
	}

	public static String decrypt(final String encryptedText) {

		final byte[] keyHash = configuredKeyHash(false);
		if (keyHash == null) {

			logger.warn("Unable to decrypt value, no secret key set.");

			return null;
		}

		return decryptWithKeyHash(encryptedText, keyHash);
	}

	public static String decryptWithKeyHash(final String encryptedText, final byte[] keyHash) {

		try {

			ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encryptedText));
			final byte[] iv = new byte[GCM_IV_LENGTH];

			buffer.get(iv);
			final byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			final SecretKeySpec skeySpec   = new SecretKeySpec(keyHash, BASE_ALGO);
			final Cipher cipher            = Cipher.getInstance(CRYPT_ALGO);
			final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

			cipher.init(Cipher.DECRYPT_MODE, skeySpec, gcmSpec);

			return new String(cipher.doFinal(ciphertext), CHARSET);

		} catch (Throwable t) {

			// WARN, not ERROR: the fallback below usually succeeds, and a condition the code
			// recovers from is not an error -- it is a request to re-encrypt the value.
			logger.warn("Unable to decrypt ciphertext. Falling back to previous method. If this works, it is recommended to update the value by re-encrypting it. Cause: {}: {}", t.getClass().getSimpleName(), t.getMessage());

			return decryptWithKeyHashDeprecated(encryptedText, keyHash);
		}
	}

	@Deprecated
	public static String decryptWithKeyHashDeprecated(final String encryptedText, final byte[] keyHash) {

		try {

			final SecretKeySpec skeySpec = new SecretKeySpec(keyHash, BASE_ALGO);
			final Cipher cipher          = Cipher.getInstance(BASE_ALGO);

			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

			return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)), CHARSET);

		} catch (Throwable t) {

			logger.error("Unable to decrypt ciphertext: {}: {}", t.getClass().getSimpleName(), t.getMessage());
		}

		return null;
	}

	// ----- private methods -----
	private static void assertKnownScheme(final String scheme) throws FrameworkException {

		if (!isKnownScheme(scheme)) {

			throw new FrameworkException(422, "Unknown encryption scheme '" + scheme + "', expected one of: " + getKnownSchemes());
		}
	}

	private static byte[] deriveKey(final String key, final byte[] salt) throws Exception {

		final PBEKeySpec spec = new PBEKeySpec(key.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);

		try {

			return SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).getEncoded();

		} finally {

			spec.clearPassword();
		}
	}

	/**
	 * The key configured in structr.conf, derived on every call rather than cached. An MD5 digest of a
	 * short string costs nothing next to what encryption itself costs, and holding it was the whole
	 * problem: a cached key is a key somebody can replace while another request is using it.
	 */
	private static byte[] configuredKeyHash(final boolean generateIfMissing) {

		String secret = Settings.GlobalSecret.getValue();
		if (StringUtils.isBlank(secret) && generateIfMissing) {

			secret = Settings.getOrGenerateEncryptionSecret();
		}

		if (StringUtils.isBlank(secret)) {

			return null;
		}

		return keyHashOf(secret);
	}
}
