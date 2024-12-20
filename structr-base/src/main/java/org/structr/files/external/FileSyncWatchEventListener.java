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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.nio.file.Path;

/**
 * Implementation of the watch event listener interface that syncs
 * the discovered files with the database.
 */
public class FileSyncWatchEventListener implements WatchEventListener {

	private static final Logger logger = LoggerFactory.getLogger(FileSyncWatchEventListener.class);
	private final String rootFolderUUID;

	public FileSyncWatchEventListener(final String rootFolderUUID) {

		this.rootFolderUUID = rootFolderUUID;
	}

	@Override
	public boolean onDiscover(final Path root, final Path context, final Path path) throws FrameworkException {

		// skip root directory
		if (context.equals(path)) {
			return true;
		}

		final FolderAndFile obj = handle(rootFolderUUID, root, path, true);
		if (obj != null && obj.file != null && obj.file instanceof File) {

			final File fileNode       = (File)obj.file;
			final java.io.File fileOnDisk = path.toFile();
			final long size               = fileOnDisk.length();
			final long lastModified       = fileOnDisk.lastModified();
			final Long fileNodeSize       = StorageProviderFactory.getStorageProvider(fileNode).size();
			final Long fileNodeDate       = fileNode.getFileModificationDate();

			// update metadata only when size or modification time has changed
			if (fileNodeSize == null || fileNodeDate == null || size != fileNodeSize || lastModified != fileNodeDate) {

				obj.handle();
			}
		}

		return true;
	}

	@Override
	public boolean onCreate(final Path root, final Path context, final Path path) throws FrameworkException {

		final FolderAndFile obj = handle(rootFolderUUID, root, path, true);
		if (obj != null) {

			obj.handle();
		}

		return true;
	}

	@Override
	public boolean onModify(final Path root, final Path context, final Path path) throws FrameworkException {

		final FolderAndFile obj = handle(rootFolderUUID, root, path, false);
		if (obj != null) {

			obj.handle();
		}

		return true;
	}

	@Override
	public boolean onDelete(final Path root, final Path context, final Path path) throws FrameworkException {

		final FolderAndFile obj = handle(rootFolderUUID, root, path, false);

		if (obj != null && obj.file != null) {

			StructrApp.getInstance().delete(obj.file);
		}

		return true;
	}

	public String getRootFolderUUID() {

		return this.rootFolderUUID;
	}

	// ----- private methods -----
	private FolderAndFile handle(final String rootFolderUUID, final Path root, final Path relativePath, final boolean create) throws FrameworkException {

		// identify mounted folder object
		final NodeInterface node = StructrApp.getInstance().nodeQuery("Folder").uuid(rootFolderUUID).getFirst();
		if (node != null) {

			final Folder folder     = node.as(Folder.class);
			String targetFileType   = folder.getMountTargetFileType();
			String targetFolderType = folder.getMountTargetFolderType();

			if (targetFileType != null) {

				final Traits traits = Traits.of(targetFileType);
				if (traits == null || !traits.contains("File")) {

					logger.error("Given target type for mounted files or folders was not extending AbstractFile.");
				}
			}

			if (targetFolderType != null) {

				final Traits traits = Traits.of(targetFolderType);
				if (traits == null || !traits.contains("Folder")) {

					logger.error("Given target type for mounted files or folders was not extending AbstractFile.");
				}
			}

			final String mountFolderPath = folder.getPath();
			if (mountFolderPath != null) {

				final Path relativePathParent   = relativePath.getParent();
				final boolean isFile            = new java.io.File(relativePath.toUri()).isFile();

				if (relativePathParent == null) {

					return new FolderAndFile(node, getOrCreate(node, relativePath.toString(), isFile, create, targetFolderType, targetFileType));
				} else {

					// Virtual path to parent
					final Path relativePathToParentFolder    = root.relativize(relativePathParent);
					final Path fileOrFolderPath              = relativePathParent.relativize(relativePath);
					final Path virtualPathToParent           = Path.of(mountFolderPath).resolve(relativePathToParentFolder);
					final NodeInterface parentFolder         = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), virtualPathToParent.toString());
					final NodeInterface file                 = getOrCreate(parentFolder, fileOrFolderPath.toString() , isFile, create, targetFolderType, targetFileType);

					return new FolderAndFile(node, file);
				}

			} else {

				logger.warn("Cannot handle watch event, folder {} has no path", node.getUuid());
			}
		}

		return null;
	}

	private NodeInterface getOrCreate(final NodeInterface parentFolder, final String fileName, final boolean isFile, final boolean doCreate, final String folderType, final String fileType) throws FrameworkException {

		final Traits traits                        = Traits.of("AbstractFile");
		final PropertyKey<Boolean> isExternalKey   = traits.key("isExternal");
		final PropertyKey<NodeInterface> parentKey = traits.key("parent");
		final PropertyKey<String> nameKey          = traits.key("parent");
		final App app                              = StructrApp.getInstance();
		final String type                          = isFile ? (fileType != null ? fileType : "File") : (folderType != null ? folderType : "Folder");

		NodeInterface file = app.nodeQuery(type).and(nameKey, fileName).and(parentKey, parentFolder).getFirst();
		if (file == null && doCreate) {

			file = app.create(type,
				new NodeAttribute<>(nameKey,       fileName),
				new NodeAttribute<>(parentKey,     parentFolder),
				new NodeAttribute<>(isExternalKey, true)
			);
		}

		return file;
	}

	private class FolderAndFile {

		public NodeInterface file = null;
		public NodeInterface rootFolder = null;

		public FolderAndFile(final NodeInterface rootFolder, final NodeInterface file) {
			this.rootFolder = rootFolder;
			this.file       = file;
		}

		void handle() throws FrameworkException {

			final PropertyKey<Boolean> doFulltextIndexing = Traits.of("Folder").key("mountDoFulltextIndexing");
			final PropertyKey<Long> lastSeenMounted       = Traits.of("AbstractFile").key("lastSeenMounted");

			if (file.is("File")) {

				final File fileBase = file.as(File.class);

				if (rootFolder != null && rootFolder.getProperty(doFulltextIndexing)) {
					StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(file);
				}

				FileHelper.updateMetadata(fileBase, new PropertyMap(lastSeenMounted, System.currentTimeMillis()), true);

			} else if (file.is("AbstractFile")) {

				final AbstractFile abstractFile = (AbstractFile)file;

				file.setProperty(lastSeenMounted, System.currentTimeMillis());
			}
		}
	}
}














