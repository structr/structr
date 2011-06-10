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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.File;

/**
 * Transport data container for file nodes
 * 
 * @author axel
 */
public class FileNodeDataContainer extends NodeDataContainer
{
	private static final Logger logger = Logger.getLogger(FileNodeDataContainer.class.getName());

	protected Map<Integer, FileChunkContainer> chunks = null;
	protected int fileSize = 0;

	public FileNodeDataContainer()
	{
	}

	public FileNodeDataContainer(final File fileNode)
	{
		super(fileNode);

		this.fileSize = (int)fileNode.getSize();

	}

	public byte[] getBinaryContent()
	{
		ByteBuffer ret = ByteBuffer.allocate(fileSize);
		if(chunks != null)
		{
			for(FileChunkContainer container : chunks.values())
			{
				ret.put(container.getBinaryContent());
			}
		}

		return(ret.array());
	}

	public void addChunk(FileChunkContainer chunk)
	{
		if(chunks == null)
		{
			chunks = new TreeMap<Integer, FileChunkContainer>();
		}

		checkFileSize(chunk.getFileSize());
		this.chunks.put(chunk.getSequenceNumber(), chunk);
	}

	public void setFileSize(int fileSize)
	{
		this.fileSize = fileSize;
	}

	// ----- public static methods -----
	public static Iterable<FileChunkContainer> getChunks(File fileNode, int chunkSize)
	{
		Map<Integer, FileChunkContainer> chunks = new TreeMap<Integer, FileChunkContainer>();
		int fileSize = (int)fileNode.getSize();

		try
		{

			InputStream is = fileNode.getInputStream();
			if(is != null)
			{
				int sequenceNumber = 0;
				int readSize = 0;

				for(int available = is.available(); available > 0; available = is.available())
				{
					readSize = available < chunkSize ? available : chunkSize;

					FileChunkContainer chunk = new FileChunkContainer(fileNode.getId(), fileSize, sequenceNumber, readSize);
					is.read(chunk.getBuffer(), 0, readSize);

					chunks.put(sequenceNumber, chunk);

					sequenceNumber++;
				}

				is.close();
			}

		} catch(IOException ex)
		{
			logger.log(Level.SEVERE, "Could not read file", ex);
		}

		return(chunks.values());
	}

	// ----- private methods -----
	private void checkFileSize(int fileSize) throws IllegalStateException
	{
		if(this.fileSize == 0)
		{
			this.fileSize = fileSize;

		} else
		{
			if(fileSize != this.fileSize)
			{
				logger.log(Level.WARNING, "File size mismatch while adding chunk! Got {0}, expected {1}", new Object[] { fileSize, this.fileSize } );
			}
		}

	}
}
