/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
package org.structr.cloud.message;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 * Marks the end of a <code>FileNodeDataContainer</code>. This class does not contain binary content itself, its a marker only.
 *
 * @author Christian Morgner
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
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		serverConnection.finishFile(this);
		serverConnection.send(ack());

		context.progress();
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}
}
