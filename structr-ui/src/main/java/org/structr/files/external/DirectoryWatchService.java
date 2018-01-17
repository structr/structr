/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.files.external;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.Folder;

/**
 */
public class DirectoryWatchService extends Thread implements RunnableService {

	private static final Logger logger                 = LoggerFactory.getLogger(DirectoryWatchService.class);
	private final Map<String, FolderInfo> watchedRoots = new LinkedHashMap<>();
	private final Map<WatchKey, Path> watchKeyMap      = new LinkedHashMap<>();
	private WatchEventListener listener                = null;
	private WatchService watchService                  = null;
	private boolean running                            = false;

	public DirectoryWatchService() {
		super("DirectoryWatchService");
	}

	public void mountFolder(final Folder folder) {

		final boolean watchContents = folder.getProperty(Folder.mountWatchContents);
		final Integer scanInterval  = folder.getProperty(Folder.mountScanInterval);
		final String mountTarget    = folder.getProperty(Folder.mountTarget);
		final String folderPath     = folder.getProperty(Folder.path);
		final String uuid           = folder.getUuid();

		synchronized (watchedRoots) {

			final FolderInfo info = watchedRoots.get(uuid);
			if (info == null) {

				// mount
				if (mountTarget != null) {

					logger.info("Mounting {} to {}..", mountTarget, folderPath);

					watchedRoots.put(uuid, new FolderInfo(uuid, mountTarget, scanInterval));

					new Thread(new ScanWorker(watchContents, Paths.get(mountTarget), mountTarget, true)).start();
				}

			} else {

				final String root = info.getRoot();

				if (mountTarget == null) {

					logger.info("Unmounting {}..", root);

					watchedRoots.remove(uuid);

				} else if (!root.equals(mountTarget)) {

					logger.info("Mounting {} to {}..", mountTarget, folderPath);

					watchedRoots.put(uuid, new FolderInfo(uuid, mountTarget, scanInterval));

					new Thread(new ScanWorker(watchContents, Paths.get(mountTarget), mountTarget, true)).start();

				} else {

					info.setScanInterval(scanInterval);
				}
			}
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
						new Thread(new ScanWorker(false, Paths.get(info.getRoot()), info.getRoot(), true)).start();
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

						final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

						try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, true, false)) {

							for (final WatchEvent event : key.pollEvents()) {

								final Kind kind = event.kind();

								if (OVERFLOW.equals(kind)) {
									continue;
								}

								if (!handleWatchEvent(true, root, (Path)key.watchable(), event)) {

									System.out.println("cancelling watch key for " + key.watchable());

									key.cancel();
								}
							}

							tx.success();

						} catch (Throwable t) {

							t.printStackTrace();

						} finally {

							key.reset();
						}

					} else {

						key.cancel();
					}
				}

			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

		System.out.println("DirectoryWatchService ended..");
	}

	@Override
	public void startService() throws Exception {

		try {

			final Path dir    = Paths.get(Settings.FilesPath.getValue());
			this.watchService = dir.getFileSystem().newWatchService();

			logger.info("Watch service successfully registered");

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx(false, false, false)) {

			// find all folders with mount targets and mount them
			for (final Folder folder : app.nodeQuery(Folder.class).notBlank(Folder.mountTarget).getAsList()) {

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
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		this.listener = new FileSyncWatchEventListener();

		return true;
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
	public String getModuleName() {
		return "ui";
	}

	// ----- private methods -----
	private boolean handleWatchEvent(final boolean registerWatchKey, final Path root, final Path parent, final WatchEvent event) throws IOException {

		boolean result = true; // default is "don't cancel watch key"

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Path path = parent.resolve((Path)event.context());
			final Kind kind = event.kind();

			if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {

				if (Files.isDirectory(path)) {

					scanDirectoryTree(registerWatchKey, root, path);
				}

				result = listener.onCreate(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {

				result = listener.onDelete(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {

				result = listener.onModify(root, parent, path);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return result;
	}

	private void scanDirectoryTree(final boolean registerWatchKey, final Path root, final Path path) throws IOException {

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
		securityContext.disableEnsureCardinality();
		securityContext.disableModificationOfAccessTime();
		securityContext.ignoreResultCount(true);

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
				fex.printStackTrace();
			}
		}

		logger.info("{}: {} files", path.toString(), count);

		// recurse
		for (final File directory : directories) {

			// recurse (but not in a new thread)
			new ScanWorker(registerWatchKey, root, directory.getAbsolutePath(), false).run();
		}
	}

	// ----- nested classes -----
	private class ScanWorker implements Runnable {

		private boolean isRootScanner    = false;
		private boolean registerWatchKey = false;
		private String path              = null;
		private Path root                = null;

		public ScanWorker(final boolean registerWatchKey, final Path root, final String path, final boolean isRootScanner) {

			this.isRootScanner    = isRootScanner;
			this.registerWatchKey = registerWatchKey;
			this.root             = root;
			this.path             = path;
		}

		@Override
		public void run() {

			boolean canStart = false;

			// wait for transaction to finish so we can be
			// sure that the mounted folder exists
			for (int i=0; i<3; i++) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					if (StructrApp.getInstance().nodeQuery(Folder.class).and(Folder.mountTarget, root.toString()).getFirst() != null) {

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
							scanDirectoryTree(registerWatchKey, root, actualPath);

							// set last scanned timestamp on root folder
							if (isRootScanner) {

								try (final Tx tx = StructrApp.getInstance().tx()) {

									final Folder rootFolder = StructrApp.getInstance().nodeQuery(Folder.class).and(Folder.mountTarget, root.toString()).getFirst();
									if (rootFolder != null) {

										rootFolder.setProperty(Folder.mountLastScanned, System.currentTimeMillis());
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

		private long lastScanned  = 0L;
		private long scanInterval = 0L;
		private String root       = null;
		private String uuid       = null;

		public FolderInfo(final String uuid, final String root, final Integer scanInterval) {

			this.lastScanned  = System.currentTimeMillis();
			this.root         = root;
			this.uuid         = uuid;

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
}
