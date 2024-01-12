/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public abstract class CryptFunction extends AdvancedScriptingFunction {

	public static byte[] secretKeyHash    = null;
	public static final String CHARSET    = "UTF-8";
	public static final String HASH_ALGO  = "MD5";
	public static final String CRYPT_ALGO = "AES";

	// ----- public static methods -----
	public static void setEncryptionKey(final String key) {

		if (key == null) {

			logger.info("set_encryption_key() was called with null parameter, resetting global secret key");

			// reset hash
			secretKeyHash = null;
			return;
		}

		try {

			secretKeyHash = MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes("UTF-8"));

		} catch (Throwable t) {
			logger.error("Unable to set secret key: {}", t.getMessage());
		}
	}

	public static String encrypt(final String clearText, final String key) throws FrameworkException {

		try {

			final SecretKeySpec skeySpec = new SecretKeySpec(MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes(CHARSET)), CRYPT_ALGO);
			final Cipher cipher          = Cipher.getInstance(CRYPT_ALGO);

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

			return Base64.getEncoder().encodeToString(cipher.doFinal(clearText.getBytes(CHARSET)));

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	public static String encrypt(final String clearText) throws FrameworkException {

		CryptFunction.checkAndTryToLoadEncryptionKey();

		if (secretKeyHash == null) {

			throw new FrameworkException(422, "Unable to encrypt data, no secret key set.");
		}

		try {

			final SecretKeySpec skeySpec = new SecretKeySpec(secretKeyHash, CRYPT_ALGO);
			final Cipher cipher          = Cipher.getInstance(CRYPT_ALGO);

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

			return Base64.getEncoder().encodeToString(cipher.doFinal(clearText.getBytes(CHARSET)));

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	public static String decrypt(final String encryptedText, final String key) {

		try {

			final SecretKeySpec skeySpec = new SecretKeySpec(MessageDigest.getInstance(HASH_ALGO).digest(key.getBytes(CHARSET)), CRYPT_ALGO);
			final Cipher cipher          = Cipher.getInstance(CRYPT_ALGO);

			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

			return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)), CHARSET);

		} catch (Throwable t) {
			logger.error("Unable to decrypt ciphertext: {}: {}", t.getClass().getSimpleName(), t.getMessage());
		}

		return null;
	}

	public static String decrypt(final String encryptedText) {

		CryptFunction.checkAndTryToLoadEncryptionKey();

		if (secretKeyHash == null) {

			logger.warn("Unable to decrypt value, no secret key set.");
			return null;
		}

		try {

			final SecretKeySpec skeySpec = new SecretKeySpec(secretKeyHash, CRYPT_ALGO);
			final Cipher cipher          = Cipher.getInstance(CRYPT_ALGO);

			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

			return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)), CHARSET);

		} catch (Throwable t) {
			logger.error("Unable to decrypt ciphertext: {}: {}", t.getClass().getSimpleName(), t.getMessage());
		}

		return null;
	}

	// ----- private methods -----
	private static void checkAndTryToLoadEncryptionKey() {

		if (secretKeyHash == null) {

			// try to load from setting
			final String valueFromSetting = Settings.GlobalSecret.getValue();
			if (StringUtils.isNotBlank(valueFromSetting)) {

				logger.info("Secret key for encryption read from structr.conf.");
				CryptFunction.setEncryptionKey(valueFromSetting);
			}
		}
	}
}
