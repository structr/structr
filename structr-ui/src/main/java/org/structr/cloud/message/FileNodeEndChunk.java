/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.message;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

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

	@Override
	protected void deserializeFrom(Reader reader) throws IOException {

		this.containerId = (String)SyncCommand.deserialize(reader);
		this.fileSize    = (Integer)SyncCommand.deserialize(reader);

		super.deserializeFrom(reader);
	}

	@Override
	protected void serializeTo(Writer writer) throws IOException {

		SyncCommand.serialize(writer, containerId);
		SyncCommand.serialize(writer, fileSize);

		super.serializeTo(writer);
	}
}
