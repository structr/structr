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
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

/**
 *
 */
public class Delete extends Message {

	private String graphObjectId = null;

	public Delete() { }

	public Delete(final String uuid) {
		this.graphObjectId = uuid;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		serverConnection.delete(graphObjectId);
		sendKeepalive(serverConnection);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {
		this.graphObjectId = (String)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {
		SyncCommand.serialize(outputStream, graphObjectId);
	}
}
