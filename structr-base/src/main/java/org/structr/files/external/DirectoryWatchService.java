/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.external;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;

/**
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class DirectoryWatchService extends Thread implements RunnableService {

	private static final Logger logger                       = LoggerFactory.getLogger(DirectoryWatchService.class);
	private final Map<String, FolderInfo> watchedRoots       = new LinkedHashMap<>();
	private final Map<WatchKey, Path> watchKeyMap            = new LinkedHashMap<>();
	private WatchService watchService                        = null;
	private boolean running                                  = false;

	public DirectoryWatchService() {
		super("DirectoryWatchService");
		setDaemon(true);
	}

	public boolean isMounted(final String folderUuid) {

		boolean isMounted = false;
		synchronized (watchedRoots) {

			isMounted = watchedRoots.containsKey(folderUuid);
		}
		return isMounted;
	}

	public void mountFolder(final Folder folder) {

		final boolean watchContents = folder.getProperty(StructrApp.key(Folder.class, "mountWatchContents"));
		final Integer scanInterval  = folder.getProperty(StructrApp.key(Folder.class, "mountScanInterval"));
		final StorageProvider prov  = StorageProviderFactory.getStorageProvider(folder);
		final String mountTarget    = prov.getConfig() != null ? prov.getConfig().getConfiguration().get("mountTarget") : null;
		final String folderPath     = folder.getProperty(StructrApp.key(Folder.class, "path"));
		final String uuid           = folder.getUuid();

		synchronized (watchedRoots) {

			final FolderInfo info = watchedRoots.get(uuid);
			if (info == null) {

				// mount
				if (mountTarget != null) {

					doMount(folder, uuid, mountTarget, folderPath, scanInterval, watchContents);
				}

			} else {

				final String root = info.getRoot();

				if (mountTarget == null) {

					logger.info("Unmounting {}..", root);

					watchedRoots.remove(uuid);

				} else if (!root.equals(mountTarget)) {

					doMount(folder, uuid, mountTarget, folderPath, scanInterval, watchContents);

				} else {

					info.setScanInterval(scanInterval);
				}
			}
		}
	}

	public void doMount(final Folder folder, final String uuid, final String mountTarget, final String folderPath, final Integer scanInterval, final boolean watchContents) {

		logger.info("Mounting {} to {}..", mountTarget, folderPath);

		watchedRoots.put(uuid, new FolderInfo(uuid, mountTarget, scanInterval, StorageProviderFactory.getStorageProvider(folder)));

		final FolderInfo info = watchedRoots.get(uuid);

		// upon creation, set the last scanned date correctly to prevent early scanning
		final Long lastScanDate       = folder.getProperty(StructrApp.key(Folder.class, "mountLastScanned"));
		final boolean wasNeverScanned = (lastScanDate == null);

		if (!wasNeverScanned) {
			info.setLastScanned(lastScanDate);
		}

		if (wasNeverScanned || info.shouldScan()) {

			info.setLastScanned(System.currentTimeMillis());

			new Thread(new ScanWorker(uuid, watchContents, Paths.get(mountTarget), mountTarget, true)).start();

		} else {

			logger.info("Not scanning {} - scan interval is not yet elapsed.", folderPath);
		}
	}

	public void unmountFolder(final Folder folder) {

		synchronized (watchedRoots) {

			final String uuid     = folder.getUuid();
			final FolderInfo info = watchedRoots.get(uuid);

			if (info != null) {

				watchedRoots.remove(uuid);
			}
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void run() {

		final Map<String, WatchEventItem> eventQueue = new LinkedHashMap<>();

		while (running) {

			if (!Services.getInstance().isInitialized()) {

				try { Thread.sleep(100); } catch (InterruptedException i) {}

				// loop until we are stopped
				continue;
			}

			synchronized (watchedRoots) {

				for (final FolderInfo info : watchedRoots.values()) {

					if (info.shouldScan()) {

						// update last scanned timestamp
						info.setLastScanned(System.currentTimeMillis());

						// start a new scan thread
						new Thread(new ScanWorker(info.getUuid(), false, Paths.get(info.getRoot()), info.getRoot(), true)).start();
					}
				}
			}

			try {

				final WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
				if (key != null && Services.getInstance().isInitialized()) {

					final Path root;

					// synchronize access to map but keep critical section short
					synchronized (watchKeyMap) {
						root = watchKeyMap.get(key);
					}

					if (root != null) {

						for (final WatchEvent event : key.pollEvents()) {

							final Kind kind = event.kind();

							if (OVERFLOW.equals(kind)) {
								continue;
							}

							// Find folder info in watched roots by root path
							for (FolderInfo curInfo : watchedRoots.values()) {

								if (curInfo.root.equals(root.toString())) {
									addToQueue(eventQueue, new WatchEventItem(curInfo , root, (Path)key.watchable(), event));
									break;
								}
							}
						}

						key.reset();

					} else {

						key.cancel();
					}
				}

			} catch (InterruptedException ex) {
				logger.error(ExceptionUtils.getStackTrace(ex));
			}

			final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

			try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, true, false)) {

				// handle all watch events that are older than 2 seconds
				for (final Iterator<WatchEventItem> it = eventQueue.values().iterator(); it.hasNext();) {

					final WatchEventItem item = it.next();
					if (item.olderThan(2000)) {

						handleWatchEvent(true, item);

						it.remove();
					}
				}

				tx.success();

			} catch (Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}

		}

		logger.info("DirectoryWatchService stopped");
	}

	@Override
	public void startService() throws Exception {

		try {

			final Path dir    = Paths.get(Settings.FilesPath.getValue());
			this.watchService = dir.getFileSystem().newWatchService();

			logger.info("Watch service successfully registered");

		} catch (IOException ioex) {
			logger.error(ExceptionUtils.getStackTrace(ioex));
		}

		final PropertyKey<String> storageConfigurationKey = StructrApp.key(AbstractFile.class, "storageConfiguration");
		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx(false, false, false)) {

			// find all folders with storageConfigurations and try to mount them
			for (final Folder folder : app.nodeQuery(Folder.class).not().blank(storageConfigurationKey).getAsList()) {

				mountFolder(folder);
			}

			tx.success();
		}

		running = true;

		this.start();
	}

	@Override
	public void stopService() {
		running = false;
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

		return new ServiceResult(true);
	}

	@Override
	public void shutdown() {
		running = false;
	}

	@Override
	public void initialized() {
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	// ----- private methods -----
	private boolean handleWatchEvent(final boolean registerWatchKey, final WatchEventItem item) throws IOException {

		final WatchEvent event = item.getEvent();
		final Path root        = item.getRoot();
		final Path parent      = item.getPath();
		boolean result         = true; // default is "don't cancel watch key"

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Path path = parent.resolve((Path)event.context());
			final Kind kind = event.kind();

			FileSyncWatchEventListener listener = new FileSyncWatchEventListener(item.folderInfo.uuid);

			if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {

				if (Files.isDirectory(path)) {

					scanDirectoryTree(listener, registerWatchKey, root, path);
				}

				result = listener.onCreate(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {

				result = listener.onDelete(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {

				result = listener.onModify(root, parent, path);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.error(ExceptionUtils.getStackTrace(fex));
		}

		return result;
	}

	private void scanDirectoryTree(final FileSyncWatchEventListener listener, final boolean registerWatchKey, final Path root, final Path path) throws IOException {

		final Set<FileVisitOption> options = new LinkedHashSet<>();

		if (registerWatchKey) {

			final WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			if (key != null) {

				watchKeyMap.put(key, root);
			}
		}

		// follow symlinks?
		if (Settings.FollowSymlinks.getValue()) {

			options.add(FileVisitOption.FOLLOW_LINKS);
		}

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final List<File> directories          = new LinkedList<>();
		int count                             = 0;

		// configure security context for maximum performance
		securityContext.disablePreventDuplicateRelationships();
		securityContext.disableModificationOfAccessTime();

		final List<File> files  = Arrays.asList(path.toFile().listFiles());
		final Iterator<File> it = files.iterator();

		while (it.hasNext()) {

			int batchCount = 0;

			try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, false, false)) {

				while (it.hasNext() && batchCount++ < 1000) {

					final File file     = it.next();
					final Path filePath = file.toPath();

					if (file.isDirectory()) {

						directories.add(file);
					}

					// notify listener of directory discovery
					listener.onDiscover(root, path, filePath);

					count++;
				}

				// commit batch after 1000 files
				tx.success();

			} catch (FrameworkException fex) {
				logger.error(ExceptionUtils.getStackTrace(fex));
			}
		}

		if (Boolean.FALSE.equals(Settings.LogDirectoryWatchServiceQuiet.getValue())) {
			logger.info("{}: {} files", path.toString(), count);
		}

		// recurse
		for (final File directory : directories) {

			// recurse (but not in a new thread)
			new ScanWorker(listener.getRootFolderUUID(), registerWatchKey, root, directory.getAbsolutePath(), false).run();
		}
	}

	private void addToQueue(final Map<String, WatchEventItem> queue, final WatchEventItem item) {

		final String key = item.getKey();
		if (key != null) {

			// queue should contain at most one item for the given key
			final WatchEventItem existingItem = queue.get(key);
			if (existingItem != null) {

				final Kind kindOfExistingItem = existingItem.getKind();
				final Kind kindOfNewItem      = item.getKind();

				if (StandardWatchEventKinds.ENTRY_CREATE.equals(kindOfExistingItem)) {

					if (StandardWatchEventKinds.ENTRY_CREATE.equals(kindOfNewItem)) {

						// delay CREATE (duplicate CREATE?)
						existingItem.setTime(item.getTime());
					}

					if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kindOfNewItem)) {

						// delay CREATE
						existingItem.setTime(item.getTime());
					}

					if (StandardWatchEventKinds.ENTRY_DELETE.equals(kindOfNewItem)) {

						// remove CREATE due to DELETE
						queue.remove(key);
					}
				}

				if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kindOfExistingItem)) {

					if (StandardWatchEventKinds.ENTRY_CREATE.equals(kindOfNewItem)) {

						// CREATE replaces MODIFY
						queue.put(key, item);
					}

					if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kindOfNewItem)) {

						// delay MODIFY
						existingItem.setTime(item.getTime());
					}

					if (StandardWatchEventKinds.ENTRY_DELETE.equals(kindOfNewItem)) {

						// DELETE replaces MODIFY
						queue.put(key, item);
					}
				}

				if (StandardWatchEventKinds.ENTRY_DELETE.equals(kindOfExistingItem)) {

					if (StandardWatchEventKinds.ENTRY_CREATE.equals(kindOfNewItem)) {

						// CREATE removes DELETE (no change?)
						queue.put(key, item);
					}

					if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kindOfNewItem)) {

						// MODIFY replaces DELETE?
						queue.put(key, item);
					}

					if (StandardWatchEventKinds.ENTRY_DELETE.equals(kindOfNewItem)) {

						// earlier DELETE can stay in the queue
					}
				}

			} else {

				queue.put(key, item);
			}
		}
	}

	// ----- nested classes -----
	private class ScanWorker implements Runnable {

		private boolean isRootScanner    = false;
		private boolean registerWatchKey = false;
		private String path              = null;
		private Path root                = null;
		private String uuid              = null;

		public ScanWorker(final String uuid, final boolean registerWatchKey, final Path root, final String path, final boolean isRootScanner) {

			this.isRootScanner    = isRootScanner;
			this.registerWatchKey = registerWatchKey;
			this.root             = root;
			this.path             = path;
			this.uuid             = uuid;
		}

		@Override
		public void run() {

			final PropertyKey<Long> lastScannedKey   = StructrApp.key(Folder.class, "mountLastScanned");
			boolean canStart                         = false;

			// wait for transaction to finish so we can be
			// sure that the mounted folder exists
			for (int i=0; i<3; i++) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					if (uuid == null || StructrApp.getInstance().nodeQuery(Folder.class).uuid(uuid).getFirst() != null) {

						canStart = true;
						break;
					}

					tx.success();

				} catch (FrameworkException fex) {}

				// wait for the transaction in a different thread to finish
				try { Thread.sleep(1000); } catch (InterruptedException ex) {}
			}

			// We need to wait for the creating or modifying transaction to finish before we can
			// start, otherwise the folder will not be available and no files will be created.
			if (canStart) {

				final Path actualPath = Paths.get(path);
				if (Files.exists(actualPath)) {

					if (Files.isDirectory(actualPath)) {

						try {

							// add watch services for each directory recursively

							scanDirectoryTree(new FileSyncWatchEventListener(uuid), registerWatchKey, root, actualPath);

							// set last scanned timestamp on root folder
							if (isRootScanner) {

								try (final Tx tx = StructrApp.getInstance().tx()) {

									final Folder rootFolder = StructrApp.getInstance().nodeQuery(Folder.class).uuid(uuid).getFirst();
									if (rootFolder != null) {

										rootFolder.setProperty(lastScannedKey, System.currentTimeMillis());
									}

									tx.success();

								} catch (FrameworkException fex) {}

							}

						} catch (IOException ex) {

							logger.warn("Unable to mount {}: {}", path, ex.getMessage());
						}

					} else {

						logger.warn("Unable to mount {}, not a directory", path);
					}

				} else {

					logger.warn("Unable to mount {}, directory does not exist", path);
				}

			} else {

				logger.warn("Unable to mount {}, folder was not created or mount target was not set", path);
			}
		}
	}

	// ----- nested classes -----
	private static final class FolderInfo {

		private long lastScanned                = 0L;
		private long scanInterval               = 0L;
		private String root                     = null;
		private String uuid                     = null;
		private StorageProvider effectiveConfig = null;

		public FolderInfo(final String uuid, final String root, final Integer scanInterval, final StorageProvider storageProvider) {

			this.lastScanned  = System.currentTimeMillis();
			this.root         = root;
			this.uuid         = uuid;
			this.effectiveConfig = storageProvider;

			setScanInterval(scanInterval);
		}

		public String getUuid() {
			return uuid;
		}

		public String getRoot() {
			return root;
		}

		public long getScanInterval() {
			return scanInterval;
		}

		public String getFolderUUID() {
			return this.uuid;
		}

		public void setScanInterval(final Integer scanInterval) {
			this.scanInterval = scanInterval != null ? scanInterval * 1000 : 0L;
		}

		public void setLastScanned(final long lastScanned) {
			this.lastScanned = lastScanned;
		}

		public long getLastScanned() {
			return lastScanned;
		}

		public boolean shouldScan() {
			return scanInterval > 0 && System.currentTimeMillis() > (lastScanned + scanInterval);
		}
	}

	private static final class WatchEventItem implements Comparable<WatchEventItem> {

		private WatchEvent event = null;
		private String key       = null;
		private Path root        = null;
		private Path path        = null;
		private long time        = 0L;

		private FolderInfo folderInfo = null;

		public WatchEventItem(final FolderInfo info, final Path root, final Path path, final WatchEvent event) {

			this.time  = System.nanoTime();
			this.event = event;
			this.root  = root;
			this.path  = path;
			this.folderInfo = info;

			if (path != null) {

				this.key = root.resolve((Path)event.context()).toString();
			}
		}

		@Override
		public String toString() {
			return "WatchEventItem(" + event.kind().toString() + ", " + root + ", " + path + ", " + event.context() + ")";
		}

		public WatchEvent getEvent() {
			return event;
		}

		public Path getRoot() {
			return root;
		}

		public Path getPath() {
			return path;
		}

		public Kind getKind() {
			return event.kind();
		}

		public String getKey() {
			return key;
		}

		public long getTime() {
			return time;
		}

		public void setTime(final long time) {
			this.time = time;
		}

		public boolean olderThan(final long milliseconds) {

			final double now = System.nanoTime();
			final double dt  = now - time;

			return dt > (milliseconds * 1_000_000);
		}

		@Override
		public int compareTo(final WatchEventItem o) {
			return Long.valueOf(time).compareTo(o.getTime());
		}
	}
}
