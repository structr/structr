/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import javax.crypto.Cipher;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.HashHelper;
import org.structr.core.graph.SyncCommand;
import org.structr.rest.auth.AuthHelper;

/**
 *
 *
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

		return HashHelper.getHash(password, salt);
	}

	public int getKeyLength() {
		return keyLength;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		try {

			serverConnection.setEncryptionKey(getEncryptionKey(serverConnection.getPassword()), Math.min(keyLength, Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER)));

			// send a CRYPT message which enables the encryption when received
			serverConnection.send(new Crypt());

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
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
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.userName  = (String)SyncCommand.deserialize(inputStream);
		this.salt      = (String)SyncCommand.deserialize(inputStream);
		this.keyLength = (Integer)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, userName);
		SyncCommand.serialize(outputStream, salt);
		SyncCommand.serialize(outputStream, keyLength);
	}
}
