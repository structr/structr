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
package org.structr.web.maintenance.deploy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.relation.MinificationSource;
import org.structr.web.entity.relation.Thumbnails;

/**
 *
 */
public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger      = LoggerFactory.getLogger(FileImportVisitor.class.getName());
	private Map<String, Object> config      = null;
	private SecurityContext securityContext = null;
	private Path basePath                   = null;
	private App app                         = null;
	private List<FileBase> deferredFiles    = null;
	private Map<String, Folder> folderCache = null;

	public FileImportVisitor(final Path basePath, final Map<String, Object> config) {

		this.securityContext = SecurityContext.getSuperUserInstance();
		this.securityContext.setDoTransactionNotifications(false);
		this.basePath        = basePath;
		this.config          = config;
		this.app             = StructrApp.getInstance(this.securityContext);
		this.deferredFiles   = new ArrayList<>();
		this.folderCache     = new HashMap<>();
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

		if (!basePath.equals(dir)) {
			createFolder(dir);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

		if (attrs.isRegularFile()) {

			createFile(file, file.getFileName().toString());
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {

		logger.warn("Exception while importing file {}: {}", new Object[] { file.toString(), exc.getMessage() });
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public void handleDeferredFiles() {

		if (!this.deferredFiles.isEmpty()) {

			for (FileBase file : this.deferredFiles) {

				try (final Tx tx = app.tx(true, false, false)) {

					// set properties from files.json
					final PropertyMap fileProperties = getPropertiesForFileOrFolder(file.getFolderPath());

					final PropertyKey<Map<String, String>> sourcesPropertyKey = new GenericProperty("minificationSources");
					Map<String, String> sourcesConfig = fileProperties.get(sourcesPropertyKey);
					fileProperties.remove(sourcesPropertyKey);

					file.unlockSystemPropertiesOnce();
					file.setProperties(securityContext, fileProperties);

					for (String positionString : sourcesConfig.keySet()) {

						final Integer position    = Integer.parseInt(positionString);
						final String sourcePath   = sourcesConfig.get(positionString);
						final AbstractFile source = FileHelper.getFileByAbsolutePath(securityContext, sourcePath);

						if (source != null) {

							app.create(app.get(AbstractMinifiedFile.class, file.getUuid()), (FileBase)source, MinificationSource.class, new PropertyMap(MinificationSource.position, position));

						} else {
							logger.warn("Source file {} for minified file {} at position {} not found - please verify that it is included in the export", sourcePath, file.getFolderPath(), positionString);
						}
					}

					tx.success();

				} catch (FrameworkException fxe) {

				}
			}
		}
	}

	// ----- private methods -----
	private Folder getExistingFolder(final String path) throws FrameworkException {

		Folder existingFolder = this.folderCache.get(path);

		if (existingFolder != null) {

			return existingFolder;

		} else {

			existingFolder = app.nodeQuery(Folder.class).and(AbstractFile.path, path).getFirst();
			if (existingFolder != null) {
				this.folderCache.put(path, existingFolder);
			}

			return existingFolder;
		}
	}

	private void createFolder(final Path folderObj) {

		final String folderPath = "/" + basePath.relativize(folderObj).toString();

		try (final Tx tx = app.tx(true, false, false)) {

			if (getExistingFolder(folderPath) == null) {

				final PropertyMap folderProperties = new PropertyMap(AbstractNode.name, folderObj.getFileName().toString());

				if (!basePath.equals(folderObj.getParent())) {

					final String parentPath = "/" + basePath.relativize(folderObj.getParent()).toString();
					folderProperties.put(Folder.parent, getExistingFolder(parentPath));
				}

				// set properties from files.json
				final PropertyMap properties = getPropertiesForFileOrFolder(folderPath);
				if (properties != null) {
					folderProperties.putAll(properties);
				}

				final Folder newFolder = app.create(Folder.class, folderProperties);

				this.folderCache.put(folderPath, newFolder);
			}

			tx.success();

		} catch (Exception ex) {
			logger.error("Error occured while importing folder " + folderObj, ex);
		}
	}

	private void createFile(final Path path, final String fileName) throws IOException {

		String newFileUuid = null;

		try (final Tx tx = app.tx(true, false, false)) {

			final String fullPath = "/" + basePath.relativize(path).toString();
			final PropertyMap fileProperties = getPropertiesForFileOrFolder(fullPath);

			if (fileProperties == null) {

				if (!fileName.startsWith(".")) {
					logger.info("Ignoring {} (not in files.json)", fullPath);
				}

			} else {

				Folder parent = null;

				if (!basePath.equals(path.getParent())) {
					final String parentPath  = "/" + basePath.relativize(path.getParent()).toString();
					parent = getExistingFolder(parentPath);
				}

				boolean skipFile         = false;

				FileBase file = app.nodeQuery(FileBase.class).and(FileBase.parent, parent).and(FileBase.name, fileName).getFirst();

				if (file != null) {

					final Long checksumOfExistingFile = file.getChecksum();
					final Long checksumOfNewFile      = FileHelper.getChecksum(path.toFile());

					if (checksumOfExistingFile != null && checksumOfNewFile != null && checksumOfExistingFile.equals(checksumOfNewFile)) {

						skipFile = true;

					} else {

						// remove existing file first!
						app.delete(file);
					}
				}

				if (!skipFile) {

					logger.info("Importing {}...", fullPath);

					try (final FileInputStream fis = new FileInputStream(path.toFile())) {

						// create file in folder structure
						file                     = FileHelper.createFile(securityContext, fis, null, File.class, fileName, parent);
						final String contentType = file.getContentType();

						// modify file type according to content
						if (StringUtils.startsWith(contentType, "image") || ImageHelper.isImageType(file.getProperty(name))) {

							file.unlockSystemPropertiesOnce();
							file.setProperties(securityContext, new PropertyMap(NodeInterface.type, Image.class.getSimpleName()));
						}

						newFileUuid = file.getUuid();
					}
				}

				if (file != null) {

					if (fileProperties.containsKey(AbstractMinifiedFile.minificationSources)) {
						deferredFiles.add(file);
					} else {
						file.unlockSystemPropertiesOnce();
						file.setProperties(securityContext, fileProperties);
					}
				}

				if (newFileUuid != null) {

					final FileBase createdFile = app.get(FileBase.class, newFileUuid);
					String type                = createdFile.getType();
					boolean isImage            = createdFile.getProperty(Image.isImage);
					boolean isThumbnail        = createdFile.getProperty(Image.isThumbnail);

					logger.debug("File {}: {}, isImage? {}, isThumbnail? {}", new Object[] { createdFile.getName(), type, isImage, isThumbnail});

					if (isImage) {

						try {
							ImageHelper.updateMetadata(createdFile);
							handleThumbnails((Image) createdFile);

						} catch (Throwable t) {
							logger.warn("Unable to update metadata: {}", t.getMessage());
						}
					}
				}
			}

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("Error occured while reading file properties " + fileName, ex);
		}
	}

	private void handleThumbnails(final Image img) {

		if (img.getProperty(Image.isThumbnail)) {

			// thumbnail image
			if (img.getIncomingRelationship(Thumbnails.class) == null) {

				ImageHelper.findAndReconnectOriginalImage(img);
			}

		} else {

			// original image
			if (!img.getOutgoingRelationships(Thumbnails.class).iterator().hasNext()) {

				ImageHelper.findAndReconnectThumbnails(img);

			}
		}
	}

	private PropertyMap getPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Object data = config.get(path);
		if (data != null && data instanceof Map) {

			return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), AbstractFile.class, (Map<String, Object>)data);
		}

		return null;
	}
}
