package org.structr.cloud.message;

import java.io.IOException;
import java.security.InvalidKeyException;
import javax.crypto.Cipher;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;

/**
 *
 * @author Christian Morgner
 */
public class AuthenticationResponse extends Message {

	private transient String encryptionKey = null;

	private String userName = null;
	private String salt     = null;
	private int keyLength   = 128;

	public AuthenticationResponse() {}

	public AuthenticationResponse(String userName, final String encryptionKey, final String salt, final int keyLength) {

		this.encryptionKey = encryptionKey;
		this.userName      = userName;
		this.salt          = salt;
		this.keyLength     = keyLength;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getSalt() {
		return salt;
	}

	public String getEncryptionKey(final String password) {

		if (salt != null) {
			return AuthHelper.getHash(password, salt);
		}

		return AuthHelper.getSimpleHash(password);
	}

	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		try {

			serverConnection.setEncryptionKey(getEncryptionKey(serverConnection.getPassword()), Math.min(keyLength, Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER)));
			serverConnection.setAuthenticated();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection client) {

		if (encryptionKey != null) {

			try {

				client.setEncryptionKey(encryptionKey, keyLength);

			} catch (InvalidKeyException ikex) {
				ikex.printStackTrace();
			}
		}
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
