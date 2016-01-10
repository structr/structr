/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import javax.crypto.Cipher;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.SyncCommand;

/**
 *
 *
 */


public class AuthenticationRequest extends Message {

	private String userName     = null;
	private String salt         = null;
	private int keyLength       = 128;
	private int protocolVersion = 0;

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
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		if (protocolVersion != CloudService.PROTOCOL_VERSION) {

			serverConnection.send(new Error(400, "Unsupported protocol version " + protocolVersion + ", server needs " + CloudService.PROTOCOL_VERSION));
			return;
		}

		final Principal user = serverConnection.getUser(userName);
		if (user != null) {

			try {
				this.keyLength = Math.min(keyLength, Cipher.getMaxAllowedKeyLength(CloudService.STREAM_CIPHER));
				this.salt      = user.getSalt();

				serverConnection.impersonateUser(user);
				serverConnection.send(new AuthenticationResponse(userName, user.getEncryptedPassword(), salt, keyLength));

			} catch (Throwable t) {
				t.printStackTrace();
			}

		} else {

			serverConnection.send(new Error(401, "Authentication failed."));
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.userName        = (String)SyncCommand.deserialize(inputStream);
		this.salt            = (String)SyncCommand.deserialize(inputStream);
		this.keyLength       = (Integer)SyncCommand.deserialize(inputStream);
		this.protocolVersion = (Integer)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, userName);
		SyncCommand.serialize(outputStream, salt);
		SyncCommand.serialize(outputStream, keyLength);
		SyncCommand.serialize(outputStream, CloudService.PROTOCOL_VERSION);
	}
}
