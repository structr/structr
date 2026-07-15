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
import org.structr.api.config.Settings;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.StorageSyncListener;
import org.structr.storage.sync.StorageSynchronizer;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.VirtualChangeEvent;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Synchronizer for S3-compatible object storage. The bucket keyspace is
 * flat and keys are node UUIDs, so entries are uuid-addressed; the virtual
 * path stored as "path" user metadata at upload time allows importing
 * objects that are not yet known to Structr. Keys that are not valid node
 * UUIDs are skipped - they can never be bound to nodes.
 *
 * S3-compatible stores have no universal push channel, so watching is not
 * supported and synchronization is driven purely by periodic scans
 * (mountScanInterval).
 *
 * Outbound: DELETED events delete the object (mandatory - the direct
 * provider delete is skipped for outbound-governed nodes); MOVED events
 * refresh the object's "path" user metadata via a server-side self-copy;
 * CREATED events are ignored (content materializes through the provider's
 * write path, and object keys do not depend on virtual paths).
 */
public class S3StorageSynchronizer implements StorageSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(S3StorageSynchronizer.class);

	// single-request CopyObject limit
	private static final long MAX_COPY_OBJECT_SIZE = 5L * 1024 * 1024 * 1024;

	// avoids a HeadObject per key and scan: entries are only refreshed when the eTag changes
	private final Map<String, CachedHead> headCache = new ConcurrentHashMap<>();

	private final SyncTarget target;
	private final S3AsyncClient client;
	private final String bucketName;

	public S3StorageSynchronizer(final SyncTarget target, final S3AsyncClient client, final String bucketName) {

		this.target     = target;
		this.client     = client;
		this.bucketName = bucketName;
	}

	@Override
	public SyncTarget getTarget() {
		return target;
	}

	@Override
	public boolean supportsWatching() {
		return false;
	}

	@Override
	public void startWatching(final StorageSyncListener listener) {
		// no push channel for S3-compatible storage
	}

	@Override
	public Iterator<ExternalEntry> enumerate(final String relativePath) throws IOException {

		// the keyspace is flat (keys are node uuids), subtree enumeration is
		// not possible - the whole bucket is enumerated
		return new BucketIterator();
	}

	/**
	 * Applies committed Structr-side changes to the bucket.
	 */
	@Override
	public void onVirtualChange(final VirtualChangeEvent event) {

		// only files have objects (keys are File node uuids)
		if (event.directory()) {
			return;
		}

		try {

			switch (event.type()) {

				case CREATED -> { /* content materializes through the provider write path */ }
				case MOVED   -> handleVirtualMoved(event);
				case DELETED -> handleVirtualDeleted(event);
			}

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
			logger.warn("Interrupted while applying virtual change {} to bucket {}", event, bucketName);

		} catch (Throwable t) {
			logger.warn("Unable to apply virtual change {} to S3 bucket '{}': {}", event, bucketName, S3Errors.describe(t));
		}
	}

	@Override
	public void close() {

		// the client is shared and long-lived, only local state is released
		headCache.clear();
	}

	// ----- private methods -----
	private void handleVirtualDeleted(final VirtualChangeEvent event) throws InterruptedException, ExecutionException {

		// externally created objects live at their native key; Structr-origin
		// objects at the node uuid
		final String key = objectKey(event);

		// mandatory: the direct provider delete is skipped for outbound-governed
		// nodes; DeleteObject on a missing key succeeds, so no existence check
		client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build()).get();

		headCache.remove(key);

		logger.debug("Deleted S3 object {} after virtual deletion of {}", key, event.previousRelativePath());
	}

	private void handleVirtualMoved(final VirtualChangeEvent event) throws InterruptedException, ExecutionException {

		// externally created objects keep their native key (never renamed); only
		// the "path" metadata is refreshed
		final String key              = objectKey(event);
		final HeadObjectResponse head = headOrNull(key);

		if (head == null) {

			// object was never materialized (e.g. metadata-only file)
			logger.debug("No S3 object {} to refresh path metadata for", key);
			return;
		}

		if (head.contentLength() != null && head.contentLength() > MAX_COPY_OBJECT_SIZE) {

			logger.warn("Skipping path-metadata refresh of S3 object {}: size {} exceeds the single-request copy limit", key, head.contentLength());
			return;
		}

		// S3 metadata is immutable - the canonical update is a server-side
		// self-copy with replaced metadata (no data flows through the client).
		// REPLACE discards everything not resent, so the full metadata map and
		// the content type are re-sent explicitly.
		final Map<String, String> metadata = new HashMap<>(head.metadata());

		metadata.put("path", target.syncRootPath() + "/" + event.relativePath());

		final CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
			.sourceBucket(bucketName).sourceKey(key)
			.destinationBucket(bucketName).destinationKey(key)
			.metadataDirective(MetadataDirective.REPLACE)
			.metadata(metadata);

		if (head.contentType() != null) {
			builder.contentType(head.contentType());
		}

		client.copyObject(builder.build()).get();

		// eTag changes on copy - drop the cache entry, it self-heals on the next scan
		headCache.remove(key);

		logger.debug("Refreshed path metadata of S3 object {}: {} -> {}", key, event.previousRelativePath(), event.relativePath());
	}

	/**
	 * The physical object key an outbound event acts on: externally created
	 * objects keep their native key (persisted in the node's storageKey),
	 * Structr-origin objects live at the node uuid.
	 */
	private String objectKey(final VirtualChangeEvent event) {
		return event.nativeKey() != null ? event.nativeKey() : event.nodeUuid();
	}

	private HeadObjectResponse headOrNull(final String key) throws InterruptedException, ExecutionException {

		try {

			return client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build()).get();

		} catch (ExecutionException eex) {

			if (eex.getCause() instanceof S3Exception s3e && s3e.statusCode() == 404) {
				return null;
			}

			throw eex;
		}
	}

	/**
	 * Derives the sync-root-relative virtual path from the "path" user
	 * metadata (which stores the absolute virtual path at write time).
	 *
	 * @return the relative path, "" for the root itself, or null when the
	 * metadata is absent or does not lie below this target's root
	 */
	private String relativizePathMetadata(final String absolutePath) {

		if (absolutePath == null) {
			return null;
		}

		if (absolutePath.equals(target.syncRootPath())) {
			return "";
		}

		final String prefix = target.syncRootPath() + "/";

		return absolutePath.startsWith(prefix) ? absolutePath.substring(prefix.length()) : null;
	}

	private record CachedHead(String eTag, String path) {}

	/**
	 * Lazily pages through the bucket via ListObjectsV2 continuation tokens.
	 * Runs on the scan thread outside any transaction; errors are thrown as
	 * UncheckedIOException so the scan is treated as partial (no pruning).
	 */
	private final class BucketIterator implements Iterator<ExternalEntry> {

		private final Queue<ExternalEntry> buffer = new LinkedList<>();
		private String continuationToken          = null;
		private boolean exhausted                 = false;

		@Override
		public boolean hasNext() {

			while (buffer.isEmpty() && !exhausted) {
				fetchNextPage();
			}

			return !buffer.isEmpty();
		}

		@Override
		public ExternalEntry next() {

			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			return buffer.poll();
		}

		private void fetchNextPage() {

			try {

				final ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucketName);

				if (continuationToken != null) {
					builder.continuationToken(continuationToken);
				}

				final ListObjectsV2Response response = client.listObjectsV2(builder.build()).get();

				for (final S3Object object : response.contents()) {

					final ExternalEntry entry = toEntry(object);
					if (entry != null) {

						buffer.add(entry);
					}
				}

				if (Boolean.TRUE.equals(response.isTruncated())) {

					continuationToken = response.nextContinuationToken();

				} else {

					exhausted = true;
				}

			} catch (InterruptedException iex) {

				Thread.currentThread().interrupt();
				throw new UncheckedIOException(new IOException("Interrupted while listing S3 bucket '" + bucketName + "'"));

			} catch (ExecutionException eex) {
				throw new UncheckedIOException(new IOException("Unable to list S3 bucket '" + bucketName + "': " + S3Errors.describe(eex)));
			}
		}

		private ExternalEntry toEntry(final S3Object object) throws InterruptedException, ExecutionException {

			final String key = object.key();

			if (key.endsWith("/")) {

				// keys ending in a slash are folder placeholder markers, not files
				logger.debug("Skipping S3 object {}: folder placeholder marker", key);
				return null;
			}

			final long lastModified = object.lastModified().toEpochMilli();
			final Long size         = object.size();

			// resolve the "path" user metadata, avoiding a HeadObject per key
			// and scan: only new or changed (eTag) keys are headed
			CachedHead cached = headCache.get(key);

			if (cached == null || !cached.eTag().equals(object.eTag())) {

				final HeadObjectResponse head = headOrNull(key);
				if (head == null) {

					// deleted between list and head
					return null;
				}

				cached = new CachedHead(object.eTag(), head.metadata().get("path"));

				headCache.put(key, cached);
			}

			final String relativePath = relativizePathMetadata(cached.path());

			if (Settings.isValidUuid(key)) {

				// Structr-origin object: the key is the node uuid
				if (relativePath != null && !relativePath.isEmpty()) {

					return ExternalEntry.byUuidAndPath(key, relativePath, false, size, lastModified).withNativeKey(key);
				}

				// no usable path metadata: uuid-only entries update known nodes and
				// are ignored (with a warning) for unknown ones - correct behavior
				// for foreign objects
				return ExternalEntry.byUuid(key, false, size, lastModified).withNativeKey(key);
			}

			// externally created object: not keyed by a node uuid. The "path"
			// metadata takes precedence when present (e.g. an object Structr
			// wrote through a native-keyed node); otherwise the key itself is
			// the initial relative virtual path (slashes delimit folders).
			final String effectivePath = (relativePath != null && !relativePath.isEmpty()) ? relativePath : key;

			// keys that would yield empty path segments (leading, trailing or
			// doubled slashes) cannot map to a valid virtual path
			for (final String segment : effectivePath.split("/")) {

				if (segment.isEmpty()) {

					logger.warn("Skipping S3 object {}: key does not map to a valid virtual path", key);
					return null;
				}
			}

			return ExternalEntry.externalFile(key, effectivePath, size, lastModified);
		}
	}
}
