/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.cloud;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Christian Morgner
 */
public class EncryptionContext {

	private static final Logger logger = Logger.getLogger(EncryptionContext.class.getName());

	private static final Map<Integer, Cipher> encryptionCipherMap = Collections.synchronizedMap(new HashMap<Integer, Cipher>());
	private static final Map<Integer, Cipher> decryptionCipherMap = Collections.synchronizedMap(new HashMap<Integer, Cipher>());
	private static final Map<Integer, String> passwordMap = Collections.synchronizedMap(new HashMap<Integer, String>());
	private static final Map<Integer, String> messageMap = Collections.synchronizedMap(new HashMap<Integer, String>());

	public static final synchronized Cipher getEncryptionCipher(int remoteEntityId) {

		Cipher ret = encryptionCipherMap.get(remoteEntityId);

		if(ret == null && passwordMap.containsKey(remoteEntityId)) {

			String password = passwordMap.get(remoteEntityId);

			try {
				byte[] key = DigestUtils.md5(password);
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

				ret = Cipher.getInstance("Blowfish");
				ret.init(Cipher.ENCRYPT_MODE, keySpec);

				encryptionCipherMap.put(remoteEntityId, ret);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Unable to initialize decryption cipher: {0}", t);
			}

		}

		return(ret);
	}

	public static final synchronized Cipher getDecryptionCipher(int remoteEntityId) {

		Cipher ret = decryptionCipherMap.get(remoteEntityId);

		if(ret == null && passwordMap.containsKey(remoteEntityId)) {

			String password = passwordMap.get(remoteEntityId);

			try {
				byte[] key = DigestUtils.md5(password);
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

				ret = Cipher.getInstance("Blowfish");
				ret.init(Cipher.DECRYPT_MODE, keySpec);

				decryptionCipherMap.put(remoteEntityId, ret);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Unable to initialize decryption cipher: {0}", t);
			}
		}

		return(ret);
	}

	public static synchronized void setErrorMessage(int remoteEntityId, String message) {

		messageMap.put(remoteEntityId, message);
	}

	public static synchronized void setPassword(int remoteEntityId, String password) {

		passwordMap.put(remoteEntityId, password);
	}
}
