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

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 * Implementation of the watch event listener interface that syncs
 * the discovered files with the database.
 */
public class FileSyncWatchEventListener implements WatchEventListener {

	private static final Logger logger = LoggerFactory.getLogger(FileSyncWatchEventListener.class);

	@Override
	public boolean onDiscover(final Path root, final Path context, final Path path) throws FrameworkException {

		// skip root directory
		if (context.equals(path)) {
			return true;
		}

		final FolderAndFile obj = handle(root, root.relativize(path), path, true);
		if (obj != null && obj.file != null && obj.file instanceof FileBase) {

			final FileBase fileNode       = (FileBase)obj.file;
			final java.io.File fileOnDisk = path.toFile();
			final long size               = fileOnDisk.length();
			final long lastModified       = fileOnDisk.lastModified();
			final Long fileNodeSize       = fileNode.getSize();
			final Long fileNodeDate       = fileNode.getProperty(FileBase.fileModificationDate);

			// update metadata only when size or modification time has changed
			if (fileNodeSize == null || fileNodeDate == null || size != fileNodeSize || lastModified != fileNodeDate) {

				obj.handle();
			}
		}

		return true;
	}

	@Override
	public boolean onCreate(final Path root, final Path context, final Path path) throws FrameworkException {

		final FolderAndFile obj = handle(root, root.relativize(path), path, true);
		if (obj != null) {

			obj.handle();
		}

		return true;
	}

	@Override
	public boolean onModify(final Path root, final Path context, final Path path) throws FrameworkException {

		final FolderAndFile obj = handle(root, root.relativize(path), path, false);
		if (obj != null) {

			obj.handle();
		}

		return true;
	}

	@Override
	public boolean onDelete(final Path root, final Path context, final Path path) throws FrameworkException {

		final Path relativePath = root.relativize(path);
		final FolderAndFile obj = handle(root, relativePath, path, false);

		if (obj != null && obj.file != null) {

			StructrApp.getInstance().delete(obj.file);
		}

		return true;
	}

	// ----- private methods -----
	private FolderAndFile handle(final Path root, final Path relativePath, final Path path, final boolean create) throws FrameworkException {

		// identify mounted folder object
		final Folder folder = StructrApp.getInstance().nodeQuery(Folder.class).and(Folder.mountTarget, root.toString()).getFirst();
		if (folder != null) {

			final String mountFolderPath = folder.getProperty(Folder.path);
			if (mountFolderPath != null) {

				final Path relativePathParent = relativePath.getParent();
				if (relativePathParent == null) {

					return new FolderAndFile(folder, getOrCreate(folder, path, relativePath, create));

				} else {

					final String pathRelativeToRoot = folder.getPath() + "/" + relativePathParent.toString();
					final Folder parentFolder       = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), pathRelativeToRoot);
					final AbstractFile file         = getOrCreate(parentFolder, path, relativePath, create);

					return new FolderAndFile(folder, file);
				}

			} else {

				logger.warn("Cannot handle watch event, folder {} has no path", folder.getUuid());
			}
		}

		return null;
	}

	private AbstractFile getOrCreate(final Folder parentFolder, final Path fullPath, final Path relativePath, final boolean doCreate) throws FrameworkException {

		final String fileName                    = relativePath.getFileName().toString();
		final boolean isFile                     = !Files.isDirectory(fullPath);
		final Class<? extends AbstractFile> type = isFile ? org.structr.dynamic.File.class : Folder.class;
		final App app                            = StructrApp.getInstance();

		AbstractFile file = app.nodeQuery(type).and(AbstractFile.name, fileName).and(AbstractFile.parent, parentFolder).getFirst();
		if (file == null && doCreate) {

			file = app.create(type,
				new NodeAttribute<>(AbstractFile.name,       fileName),
				new NodeAttribute<>(AbstractFile.parent,     parentFolder),
				new NodeAttribute<>(AbstractFile.isExternal, true)
			);
		}

		return file;
	}

	private class FolderAndFile {

		public AbstractFile file = null;
		public Folder rootFolder = null;

		public FolderAndFile(final Folder rootFolder, final AbstractFile file) {
			this.rootFolder = rootFolder;
			this.file       = file;
		}

		void handle() throws FrameworkException {

			if (file instanceof FileBase) {

				final FileBase fileBase = (FileBase)file;

				if (rootFolder != null && rootFolder.getProperty(Folder.mountDoFulltextIndexing)) {
					StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(fileBase);
				}

				if (file != null) {
					FileHelper.updateMetadata(fileBase);
				}
			}

			if (file instanceof AbstractFile) {

				final AbstractFile abstractFile = (AbstractFile)file;

				abstractFile.setProperty(AbstractFile.lastSeenMounted, System.currentTimeMillis());
			}
		}
	}
}














