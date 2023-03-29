/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;

public class FileUploadHandler {

	private static final Logger logger = LoggerFactory.getLogger(FileUploadHandler.class.getName());

	private File file                       = null;
	private SeekableByteChannel privateChannel = null;
	private Long size                       = 0L;
	private boolean isCreation              = false;
	private SecurityContext securityContext = null;

	public FileUploadHandler(final File file, final SecurityContext securityContext, final boolean isCreation) {

		this.size            = StorageProviderFactory.getStorageProvider(file).size();
		this.file            = file;
		this.isCreation      = isCreation;
		this.securityContext = securityContext;

		if (this.size == null) {

			SeekableByteChannel channel;
			try {

				channel = getChannel(false);
				this.size = channel.size();
				updateSize(this.size);

			} catch (IOException ex) {
				logger.error("Could not access file", ex);
			}
		}
	}

	public void handleChunk(int sequenceNumber, int chunkSize, byte[] data, int chunks) throws IOException {

		SeekableByteChannel channel = getChannel(sequenceNumber > 0);

		if (channel != null && channel.isOpen()) {

			channel.position(sequenceNumber * chunkSize);
			channel.write(ByteBuffer.wrap(data));

			if (this.size == null) {

				this.size = channel.size();

			}

			// finish upload
			if (sequenceNumber + 1 == chunks) {

				finish();
				updateSize(this.size);
			}
		}
	}

	private void updateSize(final Long size) {

		if (size == null) {
			return;
		}

		try {

			file.unlockSystemPropertiesOnce();
			file.setProperty(StructrApp.key(File.class, "size"), size);

		} catch (FrameworkException ex) {

			logger.warn("Could not set size to " + size, ex);
		}
	}

	/**
	 * Called when the WebSocket connection is closed
	 */
	public void finish() {

		try {

			Channel channel = getChannel(false);

			if (channel != null && channel.isOpen()) {

				channel.close();

				this.privateChannel = null;

				//file.increaseVersion();
				file.notifyUploadCompletion();

				if (this.isCreation == true) {

					file.callOnUploadHandler(this.securityContext);
				}
			}

		} catch (IOException e) {

			logger.warn("Unable to finish file upload", e);
		}
	}

	// ----- private methods -----
	private SeekableByteChannel getChannel(final boolean append) throws IOException {

		if (this.privateChannel == null) {
			this.privateChannel = StorageProviderFactory.getStorageProvider(this.file).getSeekableByteChannel();
		}

		return this.privateChannel;
	}
}
