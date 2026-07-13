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
package org.structr.storage.providers.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.storage.sync.ExternalChangeEvent;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.StorageSyncListener;
import org.structr.storage.sync.StorageSynchronizer;
import org.structr.storage.sync.SyncTarget;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Reference StorageSynchronizer for local filesystem mounts, backed by a
 * java.nio.file.WatchService. One WatchService and one daemon poller thread
 * per synchronizer, i.e. per mounted sync root. Emits purely path-addressed
 * events: disk paths relativized against the mount root are already virtual
 * paths relative to the sync root.
 *
 * Intentionally thin - no debouncing or coalescing here, that is handled
 * centrally by the sync service.
 */
public class LocalFSStorageSynchronizer implements StorageSynchronizer {

	private static final Logger logger = LoggerFactory.getLogger(LocalFSStorageSynchronizer.class);

	private final Map<WatchKey, Path> watchKeyMap = new ConcurrentHashMap<>();
	private final SyncTarget target;
	private final Path mountRoot;

	private volatile boolean closed  = false;
	private WatchService watchService = null;
	private Thread poller             = null;

	public LocalFSStorageSynchronizer(final SyncTarget target, final Path mountRoot) {

		this.target    = target;
		this.mountRoot = mountRoot;
	}

	@Override
	public SyncTarget getTarget() {
		return target;
	}

	@Override
	public boolean supportsWatching() {
		return true;
	}

	@Override
	public void startWatching(final StorageSyncListener listener) throws IOException {

		this.watchService = mountRoot.getFileSystem().newWatchService();

		registerTree(mountRoot, null);

		this.poller = new Thread(() -> pollLoop(listener), "LocalFSStorageSynchronizer-" + target.syncRootUuid());
		this.poller.setDaemon(true);
		this.poller.start();
	}

	@Override
	public Iterator<ExternalEntry> enumerate(final String relativePath) throws IOException {

		final Path base = relativePath != null && !relativePath.isEmpty() ? mountRoot.resolve(relativePath) : mountRoot;

		if (!Files.isDirectory(base)) {
			throw new IOException("Mount target " + base + " does not exist or is not a directory");
		}

		final Stream<Path> stream = Settings.FollowSymlinks.getValue() ? Files.walk(base, FileVisitOption.FOLLOW_LINKS) : Files.walk(base);

		// Files.walk() yields parents before children
		final Stream<ExternalEntry> entries = stream
			.filter(path -> !base.equals(path))
			.map(this::toEntry)
			.filter(Objects::nonNull);

		return new ClosingIterator(stream, entries.iterator());
	}

	@Override
	public void close() {

		closed = true;

		if (poller != null) {
			poller.interrupt();
		}

		if (watchService != null) {

			try {

				watchService.close();

			} catch (IOException ioex) {
				logger.warn("Error while closing watch service for {}: {}", mountRoot, ioex.getMessage());
			}
		}

		watchKeyMap.clear();
	}

	// ----- private methods -----
	private void pollLoop(final StorageSyncListener listener) {

		while (!closed) {

			final WatchKey key;

			try {

				key = watchService.poll(100, TimeUnit.MILLISECONDS);

			} catch (InterruptedException | ClosedWatchServiceException ex) {
				break;
			}

			if (key == null) {
				continue;
			}

			final Path directory = watchKeyMap.get(key);
			if (directory == null) {

				key.cancel();
				continue;
			}

			for (final WatchEvent<?> event : key.pollEvents()) {

				final Kind<?> kind = event.kind();

				if (OVERFLOW.equals(kind)) {

					// events were lost - ask for a full reconciliation scan
					listener.requestFullScan();
					continue;
				}

				try {

					handleEvent(listener, kind, directory.resolve((Path)event.context()));

				} catch (Throwable t) {
					logger.warn("Error while handling watch event for {}: {}", mountRoot, t.getMessage());
				}
			}

			if (!key.reset()) {
				watchKeyMap.remove(key);
			}
		}
	}

	private void handleEvent(final StorageSyncListener listener, final Kind<?> kind, final Path path) throws IOException {

		if (ENTRY_CREATE.equals(kind)) {

			if (Files.isDirectory(path)) {

				// register the new directory and report contents that already
				// exist (they were created before the watch key was active)
				registerTree(path, listener);

			}

			final ExternalEntry entry = toEntry(path);
			if (entry != null) {

				listener.onEvent(ExternalChangeEvent.created(entry));
			}

		} else if (ENTRY_MODIFY.equals(kind)) {

			final ExternalEntry entry = toEntry(path);
			if (entry != null) {

				listener.onEvent(ExternalChangeEvent.modified(entry));
			}

		} else if (ENTRY_DELETE.equals(kind)) {

			// the on-disk kind of a deleted entry is unknowable; the sync
			// handler resolves deletions kind-agnostically by path
			listener.onEvent(ExternalChangeEvent.deleted(new ExternalEntry(toRelativePath(path), null, false, null, null, path.toString())));
		}
	}

	/**
	 * Registers watch keys for the given directory and all subdirectories.
	 * If a listener is given, synthetic CREATED events are emitted for all
	 * discovered entries - used when a whole directory appears at once.
	 */
	private void registerTree(final Path directory, final StorageSyncListener listener) throws IOException {

		final Stream<Path> stream = Settings.FollowSymlinks.getValue() ? Files.walk(directory, FileVisitOption.FOLLOW_LINKS) : Files.walk(directory);

		try (stream) {

			for (final Iterator<Path> it = stream.iterator(); it.hasNext();) {

				final Path path = it.next();

				if (Files.isDirectory(path)) {

					final WatchKey key = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

					watchKeyMap.put(key, path);
				}

				if (listener != null && !directory.equals(path)) {

					final ExternalEntry entry = toEntry(path);
					if (entry != null) {

						listener.onEvent(ExternalChangeEvent.created(entry));
					}
				}
			}
		}
	}

	private ExternalEntry toEntry(final Path path) {

		try {

			if (!Files.exists(path)) {

				// vanished between event/walk and inspection
				return null;
			}

			final String relativePath = toRelativePath(path);

			if (relativePath.isEmpty()) {

				// the mount root itself is represented by the sync root node
				return null;
			}

			if (Files.isDirectory(path)) {

				return ExternalEntry.directory(relativePath, Files.getLastModifiedTime(path).toMillis()).withNativeKey(path.toString());
			}

			return ExternalEntry.file(relativePath, Files.size(path), Files.getLastModifiedTime(path).toMillis()).withNativeKey(path.toString());

		} catch (IOException ioex) {

			logger.warn("Unable to read attributes of {}: {}", path, ioex.getMessage());
			return null;
		}
	}

	private String toRelativePath(final Path path) {
		return mountRoot.relativize(path).toString().replace('\\', '/');
	}

	/**
	 * Iterator over a stream that closes the underlying stream (and its
	 * directory handles) once it is exhausted.
	 */
	private static final class ClosingIterator implements Iterator<ExternalEntry> {

		private final Stream<Path> stream;
		private final Iterator<ExternalEntry> iterator;

		private ClosingIterator(final Stream<Path> stream, final Iterator<ExternalEntry> iterator) {

			this.stream   = stream;
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {

			final boolean hasNext = iterator.hasNext();

			if (!hasNext) {
				stream.close();
			}

			return hasNext;
		}

		@Override
		public ExternalEntry next() {
			return iterator.next();
		}
	}
}
