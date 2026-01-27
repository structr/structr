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
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GenericS3BucketStorageProvider extends AbstractStorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(GenericS3BucketStorageProvider.class);

    private final static String ENDPOINT_KEY    = "endpoint";
    private final static String BUCKET_NAME_KEY = "bucketName";
    private final static String REGION_KEY      = "region";
    private final static String ACCESS_KEY_KEY  = "accessKey";
    private final static String SECRET_KEY_KEY  = "secretKey";

    private S3AsyncClient s3Client;

    private String endpoint;
    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;

    private boolean initialized = false;

    private String key;
    private String path;
    private String contentType;
    private long fileSize = -1;

    public GenericS3BucketStorageProvider(final AbstractFile file, final StorageConfiguration config) {

        super(file, config);

        final Map<String, String> configuration = config.getConfiguration();

        if (configuration.keySet().contains(ENDPOINT_KEY)) {
            endpoint = config.getConfiguration().get(ENDPOINT_KEY);
        }

        if (configuration.keySet().contains(BUCKET_NAME_KEY)) {
            bucketName = config.getConfiguration().get(BUCKET_NAME_KEY);
        }

        if (configuration.keySet().contains(REGION_KEY)) {
            region = config.getConfiguration().get(REGION_KEY);
        }

        if (configuration.keySet().contains(ACCESS_KEY_KEY)) {
            accessKey = config.getConfiguration().get(ACCESS_KEY_KEY);
        }

        if (configuration.keySet().contains(SECRET_KEY_KEY)) {
            secretKey = config.getConfiguration().get(SECRET_KEY_KEY);
        }

        if (endpoint != null) {

            // Custom S3, like e.g. Hetzner S3 Binary Storage
            initialize(accessKey, secretKey, endpoint, region, bucketName);

        } else {

            // AWS
            initialize(accessKey, secretKey, region, bucketName);
        }

    }

    /**
     * Initialize the S3 client (call once during application startup)
     */
    public void initialize(final String accessKey, final String secretKey, final String region, final String bucket) {

        if (!initialized) {

            bucketName = bucket;
            final AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

            s3Client = S3AsyncClient.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .region(Region.of(region))
                    .build();

            initialized = true;
            logger.debug("S3 Storage Provider initialized for bucket {}, region: {}", bucket, region);
        }
    }

    /**
     * Initialize with custom endpoint (for S3-compatible storage)
     */
    public void initialize(final String accessKey, final String secretKey, final String endpoint, final String region, final String bucket) {

        if (!initialized) {

            bucketName = bucket;
            final AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

            // Ensure endpoint has a proper URI scheme
            String normalizedEndpoint = endpoint;
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                // Default to https if no scheme is provided
                normalizedEndpoint = "https://" + endpoint;
                logger.debug("No URI scheme provided for endpoint, using https: {}", normalizedEndpoint);
            }

            s3Client = S3AsyncClient.builder()
                    .endpointOverride(URI.create(normalizedEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .region(Region.of(region))
                    .forcePathStyle(true)
                    .build();

            initialized = true;
            logger.debug("S3 Storage Provider initialized for bucket {}, endpoint: {}, region: {}", bucket, normalizedEndpoint, region);
        }
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
            setKey(file.getUuid());
            setPath(file.getPath());
        }
    }

    @Override
    public InputStream getInputStream() {

        setKeyFromAbstractFile();

        try {

            if (!initialized || key == null) {
                throw new IllegalStateException("S3 provider not initialized or key not set");
            }

            final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Download the entire object into a byte array, then wrap in ByteArrayInputStream
            final CompletableFuture<byte[]> future = s3Client.getObject(
                    getObjectRequest,
                    AsyncResponseTransformer.toBytes()
            ).thenApply(responseBytes -> responseBytes.asByteArray());

            final byte[] data = future.get();
            logger.debug("Opening input stream for S3 object: {}, size: {} bytes", key, data.length);

            return new ByteArrayInputStream(data);

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while getting input stream for S3 object: {}", key, e);
            return null;
        } catch (final ExecutionException e) {
            logger.error("Failed to get input stream for S3 object: {}", key, e.getCause());
            return null;
        } catch (final Throwable t) {
            logger.error("Failed to get input stream for S3 object: {}", key, t);
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

            if (!initialized || key == null) {
                throw new IllegalStateException("S3 provider not initialized or key not set");
            }

            if (append) {
                logger.warn("Append mode not supported for S3, will overwrite existing object: {}", key);
            }

            logger.debug("Opening output stream for S3 object: {}, {}", key, path);
            return new S3OutputStream(s3Client, bucketName, key, path, contentType);

        } catch (final Throwable t) {
            logger.error("Failed to get output stream for S3 object: {}", key, t);
            return null;
        }
    }

    @Override
    public SeekableByteChannel getSeekableByteChannel(final Set<? extends OpenOption> options) {

        setKeyFromAbstractFile();

        try {

            if (!initialized) {
                throw new IllegalStateException("S3 provider not initialized");
            }

            boolean write = options.contains(StandardOpenOption.WRITE) ||
                    options.contains(StandardOpenOption.CREATE) ||
                    options.contains(StandardOpenOption.CREATE_NEW);

            if (write) {
                logger.debug("Opening writable seekable channel for S3 object: {}", key);
                return new S3SeekableByteChannel(s3Client, bucketName, key, contentType, true);
            } else {
                logger.debug("Opening readable seekable channel for S3 object: {}", key);
                return new S3SeekableByteChannel(s3Client, bucketName, key, contentType, false);
            }

        } catch (Exception e) {
            logger.error("Failed to get seekable byte channel for S3 object: {}", key, e);
            return null;
        }
    }

    @Override
    public void delete() {

        setKeyFromAbstractFile();

        try {
            if (!initialized || key == null) {
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
            logger.error("Interrupted while deleting S3 object: {}", key, e);
        } catch (final ExecutionException e) {
            logger.error("Failed to delete S3 object: {}", key, e.getCause());
        } catch (final Exception e) {
            logger.error("Failed to delete S3 object: {}", key, e);
        }
    }

    @Override
    public long size() {

        setKeyFromAbstractFile();

        try {
            if (!initialized || key == null) {
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
            logger.error("Interrupted while getting size for S3 object: {}", key, e);
            return 0;
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof S3Exception) {
                final S3Exception s3e = (S3Exception) e.getCause();
                if (s3e.statusCode() == 404) {
                    return 0;
                }
            }
            logger.error("Failed to get size for S3 object: {}", key, e.getCause());
            return 0;
        } catch (final Exception e) {
            logger.error("Failed to get size for S3 object: {}", key, e);
            return 0;
        }
    }

    @Override
    public String getContentType() {

        setKeyFromAbstractFile();

        try {

            if (!initialized || key == null) {
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
            logger.error("Interrupted while getting content type for S3 object: {}", key, e);
            return "";
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof S3Exception) {
                final S3Exception s3e = (S3Exception) e.getCause();
                if (s3e.statusCode() == 404) {
                    return contentType != null ? contentType : "";
                }
            }
            logger.error("Failed to get content type for S3 object: {}", key, e.getCause());
            return "";
        } catch (final Exception e) {
            logger.error("Failed to get content type for S3 object: {}", key, e);
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
        int lastSlash = path.lastIndexOf('/');
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

            if (!initialized || key == null) {
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
            logger.error("Interrupted while checking existence for S3 object: {}", key, e);
            return false;
        } catch (final ExecutionException e) {
            if (e.getCause() instanceof S3Exception) {
                final S3Exception s3e = (S3Exception) e.getCause();
                if (s3e.statusCode() == 404) {
                    return false;
                }
            }
            logger.error("Failed to check existence for S3 object: {}", key, e.getCause());
            return false;
        } catch (final Exception e) {
            logger.error("Failed to check existence for S3 object: {}", key, e);
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

                final Map<String, String> metadata = new HashMap<>();
                metadata.put("path", path);

                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .metadata(metadata);

                if (contentType != null) {
                    requestBuilder.contentType(contentType);
                }

                final PutObjectRequest request = requestBuilder.build();
                s3Client.putObject(request, AsyncRequestBody.fromBytes(data)).get();

                logger.debug("Uploaded {} bytes to S3 object: {}", data.length, key);

            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while uploading to S3: " + key, e);
            } catch (final ExecutionException e) {
                throw new IOException("Failed to upload to S3: " + key, e.getCause());
            } catch (final Exception e) {
                throw new IOException("Failed to upload to S3: " + key, e);
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
        private final String contentType;
        private final boolean writable;

        private ByteArrayOutputStream writeBuffer;
        private ByteBuffer readBuffer;
        private long position = 0;
        private boolean open = true;

        public S3SeekableByteChannel(final S3AsyncClient s3Client, final String bucketName, final String key, final String contentType, final boolean writable) throws IOException {

            this.s3Client = s3Client;
            this.bucketName = bucketName;
            this.key = key;
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
                    throw new IOException("Interrupted while loading S3 object: " + key, e);
                } catch (final ExecutionException e) {
                    throw new IOException("Failed to load S3 object: " + key, e.getCause());
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

            int bytesToRead = Math.min(dst.remaining(), readBuffer.remaining());
            int originalLimit = readBuffer.limit();

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

            int bytesToWrite = src.remaining();
            byte[] bytes = new byte[bytesToWrite];

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

                if (contentType != null) {
                    requestBuilder.contentType(contentType);
                }

                try {
                    final PutObjectRequest request = requestBuilder.build();
                    s3Client.putObject(request, AsyncRequestBody.fromBytes(data)).get();

                    logger.debug("Uploaded {} bytes to S3 object via channel: {}", data.length, key);

                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while uploading to S3: " + key, e);
                } catch (final ExecutionException e) {
                    throw new IOException("Failed to upload to S3: " + key, e.getCause());
                }
            }

            open = false;
        }
    }
}