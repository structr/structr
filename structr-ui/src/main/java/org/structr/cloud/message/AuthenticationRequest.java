package org.structr.cloud.message;

import java.io.IOException;
import javax.crypto.Cipher;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */


public class AuthenticationRequest extends Message {

	private String userName = null;
	private String salt     = null;
	private int keyLength   = 128;

	public AuthenticationRequest() {}

	public AuthenticationRequest(String userName, final int keyLength) {

		this.userName  = userName;
		this.keyLength = keyLength;
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

	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		final Principal user = serverConnection.getUser(userName);
		if (user != null) {

			try {
				this.keyLength = Math.min(keyLength, Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER));
				this.salt      = user.getProperty(Principal.salt);

				serverConnection.impersonateUser(user);
				serverConnection.send(new AuthenticationResponse(userName, user.getEncryptedPassword(), salt, keyLength));

			} catch (Throwable t) {
				t.printStackTrace();
			}

		} else {

			serverConnection.send(new Error(401, "Wrong username or password."));
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
