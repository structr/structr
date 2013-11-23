/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.command;

import org.structr.common.Path;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.web.entity.File;

//~--- JDK imports ------------------------------------------------------------

import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class FileUploadHandler {

	private static final Logger logger = Logger.getLogger(FileUploadHandler.class.getName());

	//~--- fields ---------------------------------------------------------

	private File file                      = null;
	private FileChannel privateFileChannel = null;
	private Long size                      = 0L;

	//~--- constructors ---------------------------------------------------

	public FileUploadHandler(File file) {

		this.size = file.getProperty(File.size);
		this.file = file;

		if (this.size == null) {

			FileChannel channel;
			try {
				
				channel = getChannel();
				this.size = channel.size();
				updateSize(this.size);
				
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Could not access file", ex);
			}
			

		}

	}

	//~--- methods --------------------------------------------------------

	public void handleChunk(int sequenceNumber, int chunkSize, byte[] data) throws IOException {

		FileChannel channel = getChannel();

		if (channel != null) {

			channel.position(sequenceNumber * chunkSize);
			channel.write(ByteBuffer.wrap(data));

			if (this.size == null) {
				
				this.size = channel.size();
				updateSize(this.size);
				
			}
			
			// file size reached? upload finished
			if (channel.position() == this.size) {

				finish();
			}

		}

	}

	private void updateSize(final Long size) {
		
		if (size == null) {
			return;
		}

		try {

			file.setSize(size);

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Could not set size to " + size, ex);

		}
		
	}
	
	/**
	 * Called when the WebSocket connection is closed
	 */
	public void finish() {

		try {

			FileChannel channel = getChannel();

			if (channel != null && channel.isOpen()) {

				channel.force(true);
				channel.close();

			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to finish file upload", t);

		}

	}

	//~--- get methods ----------------------------------------------------

	// ----- private methods -----
	private FileChannel getChannel() throws IOException {

		if (this.privateFileChannel == null) {

			String relativeFilePath = file.getRelativeFilePath();

			if (relativeFilePath != null) {

				if (relativeFilePath.contains("..")) {

					throw new IOException("Security violation: File path contains ..");
				}

				String filePath         = Services.getInstance().getFilePath(Path.Files, relativeFilePath);
				java.io.File fileOnDisk = new java.io.File(filePath);

				fileOnDisk.getParentFile().mkdirs();

				this.privateFileChannel = new FileOutputStream(fileOnDisk).getChannel();

			}

		}

		return this.privateFileChannel;

	}

}
