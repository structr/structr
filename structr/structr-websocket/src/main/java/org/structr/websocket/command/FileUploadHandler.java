/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.websocket.command;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Path;
import org.structr.core.Services;
import org.structr.core.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class FileUploadHandler {

	private static final Logger logger  = Logger.getLogger(FileUploadHandler.class.getName());

	private FileChannel privateFileChannel = null;
	private File file = null;
	private long size = 0;

	public FileUploadHandler(File file) {
		this.size = file.getLongProperty(File.Key.size);
		this.file = file;
	}

	public void handleChunk(int sequenceNumber, int chunkSize, byte[] data) throws IOException {

		FileChannel channel = getChannel();
		if(channel != null) {

			channel.position(sequenceNumber * chunkSize);
			channel.write(ByteBuffer.wrap(data));

			// file size reached? upload finished
			if(channel.position() == this.size) {
				finish();
			}
		}
	}

	/**
	 * Called when the WebSocket connection is closed
	 */
	public void finish() {

		try {
			FileChannel channel = getChannel();
			if(channel != null && channel.isOpen()) {
				channel.force(true);
				channel.close();
			}

		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to finish file upload", t);
		}
	}

	// ----- private methods -----
	private FileChannel getChannel() throws IOException {

		if(this.privateFileChannel == null) {

			String relativeFilePath = file.getRelativeFilePath();
			if (relativeFilePath != null) {

				if (relativeFilePath.contains("..")) {
					throw new IOException("Security violation: File path contains ..");
				}

				String filePath         = Services.getFilePath(Path.Files, relativeFilePath);
				java.io.File fileOnDisk = new java.io.File(filePath);
				fileOnDisk.getParentFile().mkdirs();

				this.privateFileChannel = new FileOutputStream(fileOnDisk).getChannel();
			}
		}

		return this.privateFileChannel;
	}
}
