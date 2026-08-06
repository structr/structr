/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.storage.providers.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.storage.AbstractStorageProvider;
import org.structr.storage.sync.StorageSynchronizer;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.SynchronizableStorageProvider;
import org.structr.storage.util.VirtualFileChannel;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GenericS3BucketStorageProvider extends AbstractStorageProvider implements SynchronizableStorageProvider {

	private static final Logger logger = LoggerFactory.getLogger(GenericS3BucketStorageProvider.class);

	static final String ENDPOINT_KEY    = "endpoint";
	static final String BUCKET_NAME_KEY = "bucketName";
	static final String REGION_KEY      = "region";
	static final String ACCESS_KEY_KEY  = "accessKey";
	static final String SECRET_KEY_KEY  = "secretKey";

	private final S3AsyncClient s3Client;
	private final String bucketName;

	private String key;
	private String path;
	private String contentType;
	private long fileSize = -1;

	public GenericS3BucketStorageProvider(final AbstractFile file, final StorageConfiguration config) {

		super(file, config);

		final Map<String, String> configuration = config.getConfiguration();

		this.bucketName = configuration.get(BUCKET_NAME_KEY);

		// clients are shared and long-lived - provider instances are created per file operation
		this.s3Client = S3ClientCache.getOrCreate(configuration.get(ENDPOINT_KEY), configuration.get(REGION_KEY), configuration.get(ACCESS_KEY_KEY), configuration.get(SECRET_KEY_KEY));
	}

	@Override
	public StorageSynchronizer createSynchronizer(final SyncTarget target) {

		final Map<String, String> configuration = target.configuration();
		final String bucket                     = configuration.get(BUCKET_NAME_KEY);

		if (bucket == null) {

			// nothing external to synchronize without a bucket

			return null;
		}

		final S3AsyncClient client = S3ClientCache.getOrCreate(
			configuration.get(ENDPOINT_KEY),
			configuration.get(REGION_KEY),
			configuration.get(ACCESS_KEY_KEY),
			configuration.get(SECRET_KEY_KEY)
		);

		if (client == null) {

			return null;
		}

		return new S3StorageSynchronizer(target, client, bucket);
	}

	/**
	 * Set the S3 object key for this provider instance
	 */
	public void setKey(final String key) {

		this.key = key;
	}

	public void setPath(final String path) {

		this.path = path;
	}

	/**
	 * Set the content type for this object
	 */
	public void setContentType(final String contentType) {

		this.contentType = contentType;
	}

	private void setKeyFromAbstractFile() {

		final AbstractFile file = getAbstractFile();
		if (file != null) {

			// externally created objects keep their native key (persisted on the
			// node as storageKey); Structr-origin objects are keyed by node uuid
			final String storageKey = file.getStorageKey();

			setKey(storageKey != null ? storageKey : file.getUuid());
			setPath(file.getPath());
		}
	}

	@Override
	public InputStream getInputStream() {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				throw new IllegalStateException("S3 provider not initialized or key not set");
			}

			final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build();

			// Download the entire object into a byte array, then wrap in ByteArrayInputStream
			final CompletableFuture<byte[]> future = s3Client.getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).thenApply(responseBytes -> responseBytes.asByteArray());
			final byte[] data = future.get();

			logger.debug("Opening input stream for S3 object: {}, size: {} bytes", key, data.length);

			return new ByteArrayInputStream(data);

		} catch (final InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while reading S3 object {}", key);

			return null;

		} catch (final ExecutionException e) {

			if (e.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

				// the object does not exist yet - a newly created file has no
				// content until its first upload; present it as empty
				logger.debug("S3 object {} does not exist yet, returning empty content", key);

				return new ByteArrayInputStream(new byte[0]);
			}

			logger.error("Unable to read S3 object {}: {}", key, S3Errors.describe(e));

			return null;

		} catch (final Throwable t) {

			logger.error("Unable to read S3 object {}: {}", key, S3Errors.describe(t));

			return null;
		}
	}

	@Override
	public OutputStream getOutputStream() {

		return getOutputStream(false);
	}

	@Override
	public OutputStream getOutputStream(final boolean append) {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				throw new IllegalStateException("S3 provider not initialized or key not set");
			}

			if (append) {

				logger.warn("Append mode not supported for S3, will overwrite existing object: {}", key);
			}

			logger.debug("Opening output stream for S3 object: {}, {}", key, path);

			return new S3OutputStream(s3Client, bucketName, key, path, contentType);

		} catch (final Throwable t) {

			logger.error("Unable to open output stream for S3 object {}: {}", key, S3Errors.describe(t));

			return null;
		}
	}

	@Override
	public SeekableByteChannel getSeekableByteChannel(final Set<? extends OpenOption> options) {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null) {

				throw new IllegalStateException("S3 provider not initialized");
			}

			final boolean write = options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW);

			logger.debug("Opening {} seekable channel for S3 object: {}", write ? "writable" : "readable", key);

			final S3SeekableByteChannel channel = new S3SeekableByteChannel(s3Client, bucketName, key, path, contentType, write);

			// wrap channel so upload completion is signalled after closing

			return new VirtualFileChannel(getAbstractFile(), channel);

		} catch (Exception e) {

			logger.error("Unable to open channel for S3 object {}: {}", key, S3Errors.describe(e));

			return null;
		}
	}

	@Override
	public void delete() {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				throw new IllegalStateException("S3 provider not initialized or key not set");
			}

			final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build();

			s3Client.deleteObject(deleteObjectRequest).get();
			logger.debug("Deleted S3 object: {}", key);

		} catch (final InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while deleting S3 object {}", key);

		} catch (final Exception e) {

			logger.error("Unable to delete S3 object {}: {}", key, S3Errors.describe(e));
		}
	}

	@Override
	public long size() {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				return -1;
			}

			if (fileSize < 0) {

				final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

				final HeadObjectResponse metadata = s3Client.headObject(headObjectRequest).get();
				fileSize = metadata.contentLength();
			}

			return fileSize;

		} catch (final InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while getting size of S3 object {}", key);

			return 0;

		} catch (final ExecutionException e) {

			if (e.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

				return 0;
			}

			logger.error("Unable to get size of S3 object {}: {}", key, S3Errors.describe(e));

			return 0;

		} catch (final Exception e) {

			logger.error("Unable to get size of S3 object {}: {}", key, S3Errors.describe(e));

			return 0;
		}
	}

	@Override
	public String getContentType() {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				return contentType != null ? contentType : "";
			}

			if (contentType == null) {

				final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

				final HeadObjectResponse metadata = s3Client.headObject(headObjectRequest).get();
				contentType = metadata.contentType();
			}

			return contentType != null ? contentType : "";

		} catch (final InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while getting content type of S3 object {}", key);

			return "";

		} catch (final ExecutionException e) {

			if (e.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

				return contentType != null ? contentType : "";
			}

			logger.error("Unable to get content type of S3 object {}: {}", key, S3Errors.describe(e));

			return "";

		} catch (final Exception e) {

			logger.error("Unable to get content type of S3 object {}: {}", key, S3Errors.describe(e));

			return "";
		}
	}

	@Override
	public String getName() {

		setKeyFromAbstractFile();

		if (path == null) {

			return "";
		}

		// Extract filename from path (last segment after /)
		final int lastSlash = path.lastIndexOf('/');
		if (lastSlash >= 0 && lastSlash < path.length() - 1) {

			return path.substring(lastSlash + 1);
		}

		return path;
	}

	/**
	 * Check if the object exists in S3
	 */
	public boolean exists() {

		setKeyFromAbstractFile();

		try {

			if (s3Client == null || key == null) {

				return false;
			}

			final HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build();

			s3Client.headObject(headObjectRequest).get();

			return true;

		} catch (final InterruptedException e) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while checking existence of S3 object {}", key);

			return false;

		} catch (final ExecutionException e) {

			if (e.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

				return false;
			}

			logger.error("Unable to check existence of S3 object {}: {}", key, S3Errors.describe(e));

			return false;

		} catch (final Exception e) {

			logger.error("Unable to check existence of S3 object {}: {}", key, S3Errors.describe(e));

			return false;
		}
	}

	// Helper classes

	/**
	 * OutputStream implementation for S3 uploads
	 */
	private static class S3OutputStream extends ByteArrayOutputStream {

		private final S3AsyncClient s3Client;
		private final String bucketName;
		private final String key;
		private final String contentType;

		private String path;

		public S3OutputStream(final S3AsyncClient s3Client, final String bucketName, final String key, final String path, final String contentType) {

			this.s3Client = s3Client;
			this.bucketName = bucketName;
			this.key = key;
			this.path = path;
			this.contentType = contentType;
		}

		@Override
		public void close() throws IOException {

			try {

				final byte[] data = toByteArray();
				final PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(key);

				if (path != null) {

					final Map<String, String> metadata = new HashMap<>();
					metadata.put("path", path);

					requestBuilder.metadata(metadata);
				}

				if (contentType != null) {

					requestBuilder.contentType(contentType);
				}

				final PutObjectRequest request = requestBuilder.build();
				s3Client.putObject(request, AsyncRequestBody.fromBytes(data)).get();

				logger.debug("Uploaded {} bytes to S3 object: {}", data.length, key);

			} catch (final InterruptedException e) {

				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while uploading S3 object " + key);

			} catch (final Exception e) {

				throw new IOException("Unable to upload S3 object " + key + ": " + S3Errors.describe(e));

			} finally {

				super.close();
			}
		}
	}

	/**
	 * SeekableByteChannel implementation for S3
	 */
	private static class S3SeekableByteChannel implements SeekableByteChannel {

		private final S3AsyncClient s3Client;
		private final String bucketName;
		private final String key;
		private final String path;
		private final String contentType;
		private final boolean writable;

		private ByteArrayOutputStream writeBuffer;
		private ByteBuffer readBuffer;
		private long position = 0;
		private boolean open = true;

		public S3SeekableByteChannel(final S3AsyncClient s3Client, final String bucketName, final String key, final String path, final String contentType, final boolean writable) throws IOException {

			this.s3Client = s3Client;
			this.bucketName = bucketName;
			this.key = key;
			this.path = path;
			this.contentType = contentType;
			this.writable = writable;

			if (writable) {

				writeBuffer = new ByteArrayOutputStream();

			} else {

				// Load existing object for reading
				final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

				try {

					final CompletableFuture<byte[]> future = s3Client.getObject(
						getObjectRequest,
						AsyncResponseTransformer.toBytes()
					).thenApply(responseBytes -> responseBytes.asByteArray());

					final byte[] data = future.get();
					readBuffer = ByteBuffer.wrap(data);

				} catch (final InterruptedException e) {

					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while loading S3 object " + key);

				} catch (final ExecutionException e) {

					if (e.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {

						// the object does not exist yet - present an empty channel
						readBuffer = ByteBuffer.wrap(new byte[0]);

					} else {

						throw new IOException("Unable to load S3 object " + key + ": " + S3Errors.describe(e));
					}
				}
			}
		}

		@Override
		public int read(final ByteBuffer dst) throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			if (writable) {

				throw new IOException("Channel is not readable");
			}

			if (!readBuffer.hasRemaining()) {

				return -1;
			}

			final int bytesToRead   = Math.min(dst.remaining(), readBuffer.remaining());
			final int originalLimit = readBuffer.limit();

			readBuffer.limit(readBuffer.position() + bytesToRead);
			dst.put(readBuffer);

			readBuffer.limit(originalLimit);

			position += bytesToRead;

			return bytesToRead;
		}

		@Override
		public int write(final ByteBuffer src) throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			if (!writable) {

				throw new IOException("Channel is not writable");
			}

			final int bytesToWrite = src.remaining();
			final byte[] bytes     = new byte[bytesToWrite];

			src.get(bytes);
			writeBuffer.write(bytes);

			position += bytesToWrite;

			return bytesToWrite;
		}

		@Override
		public long position() throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			return position;
		}

		@Override
		public SeekableByteChannel position(final long newPosition) throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			if (!writable) {

				if (newPosition < 0 || newPosition > readBuffer.capacity()) {

					throw new IllegalArgumentException("Invalid position: " + newPosition);
				}

				readBuffer.position((int) newPosition);
			}

			position = newPosition;

			return this;
		}

		@Override
		public long size() throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			if (writable) {

				return writeBuffer.size();

			} else {

				return readBuffer.capacity();
			}
		}

		@Override
		public SeekableByteChannel truncate(final long size) throws IOException {

			if (!open) {

				throw new IOException("Channel is closed");
			}

			throw new UnsupportedOperationException("Truncate not supported for S3");
		}

		@Override
		public boolean isOpen() {

			return open;
		}

		@Override
		public void close() throws IOException {

			if (!open) {

				return;
			}

			if (writable && writeBuffer != null) {

				final byte[] data = writeBuffer.toByteArray();
				final PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(key);

				if (path != null) {

					final Map<String, String> metadata = new HashMap<>();
					metadata.put("path", path);

					requestBuilder.metadata(metadata);
				}

				if (contentType != null) {

					requestBuilder.contentType(contentType);
				}

				try {

					final PutObjectRequest request = requestBuilder.build();
					s3Client.putObject(request, AsyncRequestBody.fromBytes(data)).get();

					logger.debug("Uploaded {} bytes to S3 object via channel: {}", data.length, key);

				} catch (final InterruptedException e) {

					Thread.currentThread().interrupt();
					throw new IOException("Interrupted while uploading S3 object " + key);

				} catch (final ExecutionException e) {

					throw new IOException("Unable to upload S3 object " + key + ": " + S3Errors.describe(e));
				}
			}

			open = false;
		}
	}
}
