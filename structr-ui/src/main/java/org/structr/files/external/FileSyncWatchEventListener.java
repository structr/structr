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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
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
	private App app                    = null;

	public FileSyncWatchEventListener() {

		this.app = StructrApp.getInstance();
	}

	@Override
	public boolean onDiscover(final Path root, final Path context, final Path path) throws FrameworkException {

		// skip root directory
		if (context.equals(path)) {
			return true;
		}

		final GraphObject obj = handle(root, root.relativize(path), path, true);
		if (obj != null && obj instanceof FileBase) {

			final FileBase fileNode       = (FileBase)obj;
			final java.io.File fileOnDisk = path.toFile();
			final long size               = fileOnDisk.length();
			final long lastModified       = fileOnDisk.lastModified();
			final Long fileNodeSize       = fileNode.getSize();
			final Long fileNodeDate       = fileNode.getProperty(FileBase.fileModificationDate);

			// update metadata only when size or modification time has changed
			if (fileNodeSize == null || fileNodeDate == null || size != fileNodeSize || lastModified != fileNodeDate) {

				final FileBase file = (FileBase)obj;

				StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(file);
				FileHelper.updateMetadata(file);
			}
		}

		return true;
	}

	@Override
	public boolean onCreate(final Path root, final Path context, final Path path) throws FrameworkException {

		final GraphObject obj = handle(root, root.relativize(path), path, true);
		if (obj != null && obj instanceof FileBase) {

			final FileBase file = (FileBase)obj;

			StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(file);
			FileHelper.updateMetadata(file);
		}

		return true;
	}

	@Override
	public boolean onModify(final Path root, final Path context, final Path path) throws FrameworkException {

		final Path relativePath = root.relativize(path);
		final GraphObject obj   = handle(root, relativePath, path, false);

		if (obj != null && obj instanceof FileBase) {

			final FileBase file = (FileBase)obj;

			StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(file);
			FileHelper.updateMetadata(file);
		}

		return true;
	}

	@Override
	public boolean onDelete(final Path root, final Path context, final Path path) throws FrameworkException {

		final Path relativePath = root.relativize(path);
		final GraphObject obj   = handle(root, relativePath, path, false);

		if (obj != null) {

			StructrApp.getInstance().delete((NodeInterface)obj);
		}

		return true;
	}

	// ----- private methods -----
	private GraphObject handle(final Path root, final Path relativePath, final Path path, final boolean create) throws FrameworkException {

		// identify mounted folder object
		final Folder folder = StructrApp.getInstance().nodeQuery(Folder.class).and(Folder.mountTarget, root.toString()).getFirst();
		if (folder != null) {

			final String mountFolderPath = folder.getProperty(Folder.path);
			if (mountFolderPath != null) {

				final Path relativePathParent = relativePath.getParent();
				if (relativePathParent == null) {

					return getOrCreate(folder, path, relativePath, create);

				} else {

					final String pathRelativeToRoot = folder.getPath() + "/" + relativePathParent.toString();
					final Folder parentFolder       = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), pathRelativeToRoot);

					return getOrCreate(parentFolder, path, relativePath, create);
				}

			} else {

				logger.warn("Cannot handle watch event, folder {} has no path", folder.getUuid());
			}

		} else {

			logger.warn("Cannot find Folder object for mount target {}, folder not found.", root);
		}

		return null;
	}

	private GraphObject getOrCreate(final Folder parentFolder, final Path fullPath, final Path relativePath, final boolean doCreate) throws FrameworkException {

		final String fileName = relativePath.getFileName().toString();
		final boolean isFile  = !Files.isDirectory(fullPath);
		final Class type      = isFile ? File.class : Folder.class;

		GraphObject file = app.nodeQuery(type).and(AbstractFile.name, fileName).and(AbstractFile.parent, parentFolder).getFirst();
		if (file == null && doCreate) {

			file = app.create(type,
				new NodeAttribute<>(AbstractFile.name,       fileName),
				new NodeAttribute<>(AbstractFile.parent,     parentFolder),
				new NodeAttribute<>(AbstractFile.isExternal, true)
			);
		}

		return file;
	}
}














