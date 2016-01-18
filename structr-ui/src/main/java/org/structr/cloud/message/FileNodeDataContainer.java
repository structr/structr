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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.cloud.CloudConnection;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;
import org.structr.dynamic.File;

/**
 * Transport data container for file nodes
 *
 *
 */
public class FileNodeDataContainer extends NodeDataContainer {

	private static final Logger logger = Logger.getLogger(FileNodeDataContainer.class.getName());

	private transient java.io.File temporaryFile = null;
	private transient OutputStream outputStream  = null;
 	private long fileSize                        = 0;

	public FileNodeDataContainer() throws FrameworkException {
		super();
	}

	public FileNodeDataContainer(final File fileNode) throws FrameworkException {

		super(fileNode, 0);

		this.fileSize = fileNode.getSize();
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
		serverConnection.beginFile(this);
		sendKeepalive(serverConnection);
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	/**
	 * Adds a chunk of data to this container's temporary file, creating the file if it does not exist yet.
	 *
	 * @param chunk the chunk to add
	 */
	public void addChunk(FileNodeChunk chunk) {

		// check file size
		if (this.fileSize > 0) {

			if (chunk.getFileSize() != this.fileSize) {
				throw new IllegalStateException("File size mismatch while adding chunk. Expected " + fileSize + ", received " + chunk.getFileSize());
			}

		} else {

			this.fileSize = chunk.getFileSize();
		}

		// check sequence number
		if (chunk.getSequenceNumber() != this.sequenceNumber) {
			throw new IllegalStateException("Sequence number mismatch while adding chunk. Expected " + sequenceNumber + ", received " + chunk.getSequenceNumber());

		} else {

			sequenceNumber++;
		}

		// TODO: check chunk checksum here?
		try {
			if (temporaryFile == null) {

				temporaryFile = java.io.File.createTempFile("structr", "file");
				outputStream = new FileOutputStream(temporaryFile);
			}

			if (outputStream != null) {

				outputStream.write(chunk.getBinaryContent());
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Flushes and closes the temporary file after receiving it from a remote structr instance. This method is called when the cloud service recevies a <code>FileNodeEndChunk</code>.
	 */
	public void flushAndCloseTemporaryFile() {

		// TODO: check file checksum here?!

		if (outputStream != null) {

			try {
				outputStream.flush();
				outputStream.close();

			} catch (Throwable t) {
				t.printStackTrace();
			}

		} else {

			logger.log(Level.WARNING, "outputStream was null, fileSize: " + fileSize + "..");
		}
	}

	/**
	 * Renames / moves the temporary file to its final location. This method is called when the cloud service recevies a <code>FileNodeEndChunk</code>.
	 *
	 * @param finalPath the final path of this file
	 * @return whether the renaming/moving was successful
	 */
	public boolean persistTemporaryFile(String finalPath) throws IOException {

		boolean ret = false;

		if (temporaryFile != null) {

			final java.io.File finalFile = new java.io.File(finalPath);
			final Path source            = temporaryFile.toPath();
			final Path dest              = finalFile.toPath();

			// create parent directories
			finalFile.mkdirs();

			// move file from tmp to final destination
			Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
		}

		return (ret);
	}

	public long getFileSize() {
		return (this.fileSize);
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.fileSize = (Long)SyncCommand.deserialize(inputStream);

		super.deserializeFrom(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, fileSize);

		super.serializeTo(outputStream);
	}

	// ----- public static methods -----
	/**
	 * Creates and returns an Iterable instance whose iterator creates <code<FileNodeChunk</code> instances of the given file.
	 *
	 * @param fileNode the node to read from
	 * @param chunkSize the desired chunk size
	 * @return an Iterable that generates FileNodeChunks
	 */
	public static Iterable<FileNodeChunk> getChunks(final File fileNode, final int chunkSize) {

		return (new Iterable<FileNodeChunk>() {

			@Override
			public Iterator<FileNodeChunk> iterator() {

				return (new ChunkIterator(fileNode, chunkSize));
			}
		});
	}

	// ----- nested classes -----
	/**
	 * An iterator that creates <code>FileNodeChunks</code> while reading a large file from disk.
	 */
	private static class ChunkIterator implements Iterator<FileNodeChunk> {

		private InputStream inputStream = null;
		private File fileNode = null;
		private int sequenceNumber = 0;
		private long fileSize = 0;
		private int chunkSize = 0;

		public ChunkIterator(File fileNode, int chunkSize) {

			this.fileNode = fileNode;
			this.fileSize = fileNode.getSize();
			this.chunkSize = chunkSize;

			this.inputStream = fileNode.getInputStream();
		}

		@Override
		public boolean hasNext() {
			boolean ret = false;

			if (inputStream != null) {
				try {
					ret = inputStream.available() > 0;
					if (!ret) {
						inputStream.close();
					}

				} catch (Throwable t) {
					logger.log(Level.WARNING, "Exception in ChunkIterator: {0}", t);
				}
			}

			return (ret);
		}

		@Override
		public FileNodeChunk next() {

			FileNodeChunk chunk = null;

			if (inputStream != null) {

				try {
					int available = inputStream.available();
					int readSize = available < chunkSize ? available : chunkSize;

					chunk = new FileNodeChunk(fileNode.getUuid(), fileSize, sequenceNumber, readSize);
					inputStream.read(chunk.getBuffer(), 0, readSize);

					sequenceNumber++;

				} catch (Throwable t) {
					logger.log(Level.WARNING, "Exception in ChunkIterator: {0}", t);
				}
			}

			return (chunk);

		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported.");
		}

	}
}
