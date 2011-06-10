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
package org.structr.core.cloud;

import java.util.logging.Logger;

/**
 *
 * @author Christian Morgner
 */
public class FileChunkContainer extends DataContainer
{
	private static final Logger logger = Logger.getLogger(FileNodeDataContainer.class.getName());
	protected int sequenceNumber = 0;
	protected long containerId = 0L;
	protected int chunkSize = 0;
	protected int fileSize = 0;
	protected byte[] binaryContent;

	public FileChunkContainer()
	{
	}

	public FileChunkContainer(long containerId, int fileSize, int sequenceNumber, int chunkSize)
	{
		this.containerId = containerId;
		this.sequenceNumber = sequenceNumber;
		this.chunkSize = chunkSize;
		this.fileSize = fileSize;

		binaryContent = new byte[chunkSize];

		estimatedSize = chunkSize;
	}

	public byte[] getBuffer()
	{
		return(binaryContent);
	}

	public int getSequenceNumber()
	{
		return(sequenceNumber);
	}

	public int getFileSize()
	{
		return(fileSize);
	}

	public byte[] getBinaryContent()
	{
		return binaryContent;
	}

	public long getContainerId()
	{
		return(containerId);
	}
}
