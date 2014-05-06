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
package org.structr.cloud;

/**
 * Represents a single chunk of a <code>FileNodeDataContainer</code> that can be transmitted to a remote structr instance.
 *
 * @author Christian Morgner
 */
public class FileNodeChunk extends DataContainer {

	protected int sequenceNumber = 0;
	protected String containerId = null;
	protected int chunkSize = 0;
	protected long fileSize = 0;
	protected byte[] binaryContent;

	public FileNodeChunk() {}

	public FileNodeChunk(String containerId, long fileSize, int sequenceNumber, int chunkSize) {
		this.containerId = containerId;
		this.sequenceNumber = sequenceNumber;
		this.chunkSize = chunkSize;
		this.fileSize = fileSize;

		binaryContent = new byte[chunkSize];

		estimatedSize = chunkSize;
	}

	public byte[] getBuffer() {
		return (binaryContent);
	}

	public int getSequenceNumber() {
		return (sequenceNumber);
	}

	public long getFileSize() {
		return (fileSize);
	}

	public byte[] getBinaryContent() {
		return binaryContent;
	}

	public String getContainerId() {
		return (containerId);
	}

	@Override
	public String toString() {
		return "FileNodeChunk()";
	}

	@Override
	public Message process(final ServerContext context) {

		context.fileChunk(this);

		return new AckPacket("FileChunk", sequenceNumber);
	}
}
