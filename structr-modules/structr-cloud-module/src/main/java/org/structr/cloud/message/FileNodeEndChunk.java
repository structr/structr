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
 * Marks the end of a <code>FileNodeDataContainer</code>. This class does not contain binary content itself, its a marker only.
 *
 *
 */
public class FileNodeEndChunk extends DataContainer {

	protected String containerId = null;
	protected long fileSize = 0;

	public FileNodeEndChunk() {
		super(0);
	}

	public FileNodeEndChunk(String containerId, long fileSize) {

		super(0);

		this.containerId = containerId;
		this.fileSize = fileSize;
	}

	public long getFileSize() {
		return (fileSize);
	}

	public String getContainerId() {
		return (containerId);
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
		serverConnection.finishFile(this);
		sendKeepalive(serverConnection);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.containerId = (String)SyncCommand.deserialize(inputStream);
		this.fileSize    = (Long)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, containerId);
		SyncCommand.serialize(outputStream, fileSize);

		super.serializeTo(outputStream);
	}
}
