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
package org.structr.net.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


public class KeyHelper {

	private static final Logger logger = LoggerFactory.getLogger(KeyHelper.class.getName());

	public static KeyPair getOrCreateKeyPair(final String algorithm, final int keyLength) {
		return fromFile(algorithm, keyLength, "/tmp/privatekey.bin", "/tmp/publickey.bin");
	}

	public static KeyPair fromFile(final String algorithm, final int keyLength, final String privateKeyFileName, final String publicKeyFileName) {

		final File privateKeyFile = new File(privateKeyFileName);
		final File publicKeyFile  = new File(publicKeyFileName);

		if (privateKeyFile.exists() && publicKeyFile.exists()) {

			final PrivateKey privateKey = getPrivateKey(algorithm, privateKeyFile);
			final PublicKey publicKey   = getPublicKey(algorithm, publicKeyFile);

			return new KeyPair(publicKey, privateKey);

		} else {

			return createKeyPair(algorithm, keyLength, privateKeyFileName, publicKeyFileName);
		}
	}

	public static KeyPair fromBytes(final String algorithm, final byte[] privateKeyBytes, final byte[] publicKeyBytes) {

		final PrivateKey privateKey = getPrivateKeyFromBytes(algorithm, privateKeyBytes);
		final PublicKey publicKey   = getPublicKeyFromBytes(algorithm, publicKeyBytes);

		return new KeyPair(publicKey, privateKey);
	}

	// ----- private methods -----
	private static PrivateKey getPrivateKey(final String algorithm, final File privateKeyFile) {
		return getPrivateKeyFromBytes(algorithm, readBytes(privateKeyFile));
	}

	private static PublicKey getPublicKey(final String algorithm, final File publicKeyFile) {
		return getPublicKeyFromBytes(algorithm, readBytes(publicKeyFile));
	}

	private static PrivateKey getPrivateKeyFromBytes(final String algorithm, final byte[] privateKeyBytes) {

		try {

			final KeyFactory keyFactory     = KeyFactory.getInstance(algorithm);
			final PKCS8EncodedKeySpec spec2 = new PKCS8EncodedKeySpec(privateKeyBytes);

			return keyFactory.generatePrivate(spec2);

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return null;
	}

	private static PublicKey getPublicKeyFromBytes(final String algorithm, final byte[] publicKeyBytes) {

		try {

			final KeyFactory keyFactory   = KeyFactory.getInstance(algorithm);
			final X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);

			return keyFactory.generatePublic(spec);

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return null;
	}

	private static KeyPair createKeyPair(final String algorithm, final int keyLength, final String privateKeyFileName, final String publicKeyFileName) {

		try {

			final File publicKeyFile   = new File(publicKeyFileName);
			final File privateKeyFile  = new File(privateKeyFileName);
			final KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);

			kpg.initialize(keyLength);

			final KeyPair keyPair       = kpg.genKeyPair();
			final PublicKey publicKey   = keyPair.getPublic();
			final PrivateKey privateKey = keyPair.getPrivate();

			if (!publicKeyFile.exists() || !privateKeyFile.exists()) {

				try (final FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
					fos.write(publicKey.getEncoded());
					fos.flush();
				}

				try (final FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
					fos.write(privateKey.getEncoded());
					fos.flush();
				}
			}

			return keyPair;

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return null;
	}

	private static byte[] readBytes(final File file) {

		try {

			return Files.readAllBytes(file.toPath());

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return null;
	}
}
