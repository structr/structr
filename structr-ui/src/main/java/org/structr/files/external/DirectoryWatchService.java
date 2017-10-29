/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.Folder;

/**
 */
public class DirectoryWatchService extends Thread implements RunnableService {

	private static final Logger logger             = LoggerFactory.getLogger(DirectoryWatchService.class);
	private final Map<String, String> watchedRoots = new LinkedHashMap<>();
	private final Map<WatchKey, Path> watchKeyMap  = new LinkedHashMap<>();
	private WatchEventListener listener            = null;
	private WatchService watchService              = null;
	private boolean running                        = false;

	public DirectoryWatchService() {
		super("DirectoryWatchService");
	}

	public void mountFolder(final Folder folder) {

		final String mountTarget = folder.getProperty(Folder.mountTarget);
		final String folderPath  = folder.getProperty(Folder.path);
		final String uuid        = folder.getUuid();
		final String root        = watchedRoots.get(uuid);

		if (root == null) {

			// mount
			if (mountTarget != null && mount(mountTarget, folderPath)) {

				// remember mounted folder
				watchedRoots.put(uuid, mountTarget);
			}

		} else {

			if (mountTarget == null && unmount(folder, root)) {

				// unmount
				watchedRoots.remove(uuid);

			} else if (!root.equals(mountTarget)) {

				// remount
				if (unmount(folder, root) && mount(mountTarget, folderPath)) {

					watchedRoots.put(uuid, mountTarget);
				}
			}
		}
	}

	public void unmountFolder(final Folder folder) {

		final String uuid        = folder.getUuid();
		final String root        = watchedRoots.get(uuid);

		if (root != null && unmount(folder, root)) {

			try {
				folder.deleteRecursively(false);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

			watchedRoots.remove(uuid);
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void run() {

		while (running) {

			try {

				final WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
				if (key != null) {

					final Path root = watchKeyMap.get(key);

					try {

						for (final WatchEvent event : key.pollEvents()) {

							final Kind kind = event.kind();

							if (OVERFLOW.equals(kind)) {
								continue;
							}

							handleWatchEvent(root, (Path)key.watchable(), event);
						}

					} catch (IOException ioex) {

						ioex.printStackTrace();

					} finally {

						key.reset();
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
	private void handleWatchEvent(final Path root, final Path parent, final WatchEvent event) throws IOException {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Path path = parent.resolve((Path)event.context());
			final Kind kind = event.kind();

			if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {

				if (Files.isDirectory(path)) {

					watchDirectoryTree(path);
				}

				listener.onCreate(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {

				listener.onDelete(root, parent, path);

			} else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {

				listener.onModify(root, parent, path);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}


	private void watchDirectoryTree(final Path root) throws IOException {

		final WatchKey key = root.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		if (key != null) {

			watchKeyMap.put(key, root);
		}

		Files.walkFileTree(root, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

				try {
					// notify listener of directory discovery
					listener.onDiscover(root, root, dir);

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				// register directory with watch service
				final WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				if (key != null) {

					watchKeyMap.put(key, root);
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				try {
					// notify listener of file discovery
					listener.onDiscover(root, root, file);

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private boolean mount(final String absolutePath, final String targetPath) {

		final Path root = Paths.get(absolutePath);

		if (Files.exists(root)) {

			if (Files.isDirectory(root)) {

				logger.info("Mounting {} to {}..", absolutePath, targetPath);

				try {

					// add watch services for each directory recursively
					watchDirectoryTree(root);

					return true;

				} catch (IOException ex) {

					logger.warn("Unable to mount {}: {}", absolutePath, ex.getMessage());
				}

			} else {

				logger.warn("Unable to mount {}, not a directory", absolutePath);
			}

		} else {

			logger.warn("Unable to mount {}, directory does not exist", absolutePath);
		}

		return false;
	}

	private boolean unmount(final Folder folder, final String absolutePath) {

		try {
			folder.deleteRecursively(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		return true;
	}
}




















