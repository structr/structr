package org.structr.core.cloud;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Christian Morgner
 */


public class CipherProviderImpl implements CipherProvider {

	private static final Logger logger = Logger.getLogger(CipherProviderImpl.class.getName());
	
	private String encryptedPassword = null;
	
	public CipherProviderImpl(String encryptedPassword) {
	
		this.encryptedPassword = encryptedPassword;
	}

	// ----- interface CipherProvider -----
	@Override
	public void enableEncryption(String key) {
		
		this.encryptedPassword = key;
	}
	
	@Override
	public Cipher getEncryptionCipher(Object obj) {

		Cipher ret = null;

		if(encryptedPassword != null) {
			
			try {
				byte[] key = DigestUtils.md5(encryptedPassword);
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

				ret = Cipher.getInstance("Blowfish");
				ret.init(Cipher.ENCRYPT_MODE, keySpec);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Unable to initialize encryption cipher: {0}", t);
			}
		}

		return(ret);

	}

	@Override
	public Cipher getDecryptionCipher(Object obj) {

		Cipher ret = null;

		if(encryptedPassword != null) {
			
			try {
				byte[] key = DigestUtils.md5(encryptedPassword);
				SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

				ret = Cipher.getInstance("Blowfish");
				ret.init(Cipher.DECRYPT_MODE, keySpec);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Unable to initialize decryption cipher: {0}", t);
			}
		}

		return(ret);

	}
}
