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
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.nio.file.Files;
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
			final Long fileNodeDate       = fileNode.getProperty(StructrApp.key(File.class, "fileModificationDate"));

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
		final Folder folder = StructrApp.getInstance().nodeQuery(Folder.class).uuid(rootFolderUUID).getFirst();

		if (folder != null) {

			Class<? extends File> targetFileType      = null;
			Class<? extends Folder> targetFolderType    = null;

			try {
				if (folder.getMountTargetFileType() != null) {
					final Class clazz = StructrApp.getConfiguration().getNodeEntityClass(folder.getMountTargetFileType());
					if (clazz != null && clazz != GenericNode.class && File.class.isAssignableFrom(clazz)) {
						targetFileType = clazz;
					}
				}

				if (folder.getMountTargetFolderType() != null) {
					final Class clazz = StructrApp.getConfiguration().getNodeEntityClass(folder.getMountTargetFolderType());
					if (clazz != null && clazz != GenericNode.class && Folder.class.isAssignableFrom(clazz)) {
						targetFolderType = clazz;
					}
				}
			} catch (ClassCastException ex) {

				logger.error("Given target type for mounted files or folders was not extending AbstractFile.", ex);
			}

			final String mountFolderPath = folder.getProperty(StructrApp.key(Folder.class, "path"));
			if (mountFolderPath != null) {

				final Path relativePathParent   = relativePath.getParent();
				final boolean isFile            = new java.io.File(relativePath.toUri()).isFile();

				if (relativePathParent == null) {

					return new FolderAndFile(folder, getOrCreate(folder, relativePath.toString(), isFile, create, targetFolderType, targetFileType));
				} else {

					// Virtual path to parent
					final Path relativePathToParentFolder    = root.relativize(relativePathParent);

					// Virtual path to fileOrFolder
					final Path fileOrFolderPath              = relativePathParent.relativize(relativePath);

					final Path virtualPathToParent           = Path.of(mountFolderPath).resolve(relativePathToParentFolder);

					final Folder parentFolder                = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), virtualPathToParent.toString());
					//logger.debug("Trying to create folder/file with parameters path:{}, parent:{}", fileOrFolderPath.toFile(), parentFolder);
					final AbstractFile file                  = getOrCreate(parentFolder, fileOrFolderPath.toString() , isFile, create, targetFolderType, targetFileType);
					//logger.debug("Created or got folder/file with uuid:" + file.getUuid() + " and path: " + file.getPath());
					return new FolderAndFile(folder, file);
				}

			} else {

				logger.warn("Cannot handle watch event, folder {} has no path", folder.getUuid());
			}
		}

		return null;
	}

	private AbstractFile getOrCreate(final Folder parentFolder, final String fileName, final boolean isFile, final boolean doCreate, final Class<? extends Folder> folderType, final Class<? extends File> fileType) throws FrameworkException {

		final PropertyKey<Boolean> isExternalKey = StructrApp.key(AbstractFile.class, "isExternal");
		final PropertyKey<Folder> parentKey      = StructrApp.key(AbstractFile.class, "parent");
		final App app                            = StructrApp.getInstance();
		final Class<? extends AbstractFile> type = isFile ? (fileType != null ? fileType : org.structr.web.entity.File.class) : (folderType != null ? folderType : Folder.class);

		AbstractFile file = app.nodeQuery(type).and(AbstractFile.name, fileName).and(parentKey, parentFolder).getFirst();
		if (file == null && doCreate) {

			file = app.create(type,
				new NodeAttribute<>(AbstractFile.name,       fileName),
				new NodeAttribute<>(parentKey,     parentFolder),
				new NodeAttribute<>(isExternalKey, true)
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

			final PropertyKey<Boolean> doFulltextIndexing = StructrApp.key(Folder.class, "mountDoFulltextIndexing");
			final PropertyKey<Long> lastSeenMounted       = StructrApp.key(AbstractFile.class, "lastSeenMounted");

			if (file instanceof File) {

				final File fileBase = (File)file;

				if (rootFolder != null && rootFolder.getProperty(doFulltextIndexing)) {
					StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(fileBase);
				}

				FileHelper.updateMetadata(fileBase, new PropertyMap(lastSeenMounted, System.currentTimeMillis()), true);

			} else if (file instanceof AbstractFile) {

				final AbstractFile abstractFile = (AbstractFile)file;

				abstractFile.setProperty(lastSeenMounted, System.currentTimeMillis());
			}
		}
	}
}














