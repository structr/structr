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
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;

/**
 */
public class DirectoryWatchService extends Thread implements RunnableService {

	private static final Logger logger  = LoggerFactory.getLogger(DirectoryWatchService.class);
	private WatchEventListener listener = null;
	private WatchService watchService   = null;
	private Path root                   = null;
	private boolean running             = false;

	public DirectoryWatchService() {
		super("DirectoryWatchService");
	}

	@Override
	public void run() {

		running = true;

		while (running) {

			try {

				final WatchKey key = watchService.poll(100, TimeUnit.MILLISECONDS);
				if (key != null) {

					try {

						for (final WatchEvent event : key.pollEvents()) {

							final Kind kind = event.kind();

							if (OVERFLOW.equals(kind)) {
								continue;
							}

							handleWatchEvent((Path)key.watchable(), event);
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

			this.root         = Paths.get("/home/chrisi/tmp");
			this.watchService = root.getFileSystem().newWatchService();

			logger.info("Watching {}..", this.root.toString());

			// add watch services for each directory recursively
			watchDirectoryTree(root);

			logger.info("Watch service successfully registered");

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}


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
	private void handleWatchEvent(final Path parent, final WatchEvent event) throws IOException {

		final Path path = parent.resolve((Path)event.context());
		final Kind kind = event.kind();

		if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {

			if (Files.isDirectory(path)) {

				watchDirectoryTree(path);
			}

			listener.onCreate(path);
			return;
		}

		if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {

			listener.onDelete(path);
			return;
		}

		if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {

			listener.onModify(path);
			return;
		}
	}


	private void watchDirectoryTree(final Path root) throws IOException {

		root.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

		Files.walkFileTree(root, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

				// notify listener of directory discovery
				listener.onDiscover(dir);

				// register directory with watch service
				dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

				// notify listener of file discovery
				listener.onDiscover(file);

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
}




















