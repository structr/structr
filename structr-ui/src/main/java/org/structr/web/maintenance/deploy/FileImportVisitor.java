/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.maintenance.DeployCommand;

/**
 *
 */
public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger      = LoggerFactory.getLogger(FileImportVisitor.class.getName());
	private Map<String, Object> metadata    = null;
	private SecurityContext securityContext = null;
	private Path basePath                   = null;
	private App app                         = null;
	private List<File> deferredFiles        = null;
	private Map<String, Folder> folderCache = null;

	public FileImportVisitor(final SecurityContext securityContext, final Path basePath, final Map<String, Object> metadata) {

		this.securityContext = securityContext;
		this.basePath        = basePath;
		this.metadata        = metadata;
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

		final Class<Relation> relType          = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> positionKey = StructrApp.key(relType, "position");

		if (!this.deferredFiles.isEmpty()) {

			for (File file : this.deferredFiles) {

				try (final Tx tx = app.tx(true, false, false)) {

					tx.disableChangelog();

					// set properties from files.json
					final Map<String, Object> rawProperties = getRawPropertiesForFileOrFolder(file.getPath());
					final Map<String, String> sourcesConfig = (Map<String, String>)rawProperties.remove("minificationSources");
					final PropertyMap fileProperties = convertRawPropertiesForFileOrFolder(rawProperties);

					file.unlockSystemPropertiesOnce();
					file.setProperties(securityContext, fileProperties);

					for (String positionString : sourcesConfig.keySet()) {

						final Integer position    = Integer.parseInt(positionString);
						final String sourcePath   = sourcesConfig.get(positionString);
						final AbstractFile source = FileHelper.getFileByAbsolutePath(securityContext, sourcePath);

						if (source != null) {

							app.create(app.get(AbstractMinifiedFile.class, file.getUuid()), (File)source, relType, new PropertyMap(positionKey, position));

						} else {
							logger.warn("Source file {} for minified file {} at position {} not found - please verify that it is included in the export", sourcePath, file.getPath(), positionString);
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

		if (this.folderCache.containsKey(path)) {

			return  this.folderCache.get(path);

		} else {

			final Folder existingFolder = app.nodeQuery(Folder.class).and(StructrApp.key(AbstractFile.class, "path"), path).getFirst();
			if (existingFolder != null) {

				this.folderCache.put(path, existingFolder);
			}

			return existingFolder;
		}
	}

	private void createFolder(final Path folderObj) {

		final String folderPath = harmonizeFileSeparators("/", basePath.relativize(folderObj).toString());

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final Folder existingFolder        = getExistingFolder(folderPath);
			final PropertyMap folderProperties = new PropertyMap(AbstractNode.name, folderObj.getFileName().toString());

			if (!basePath.equals(folderObj.getParent())) {

				final String parentPath = harmonizeFileSeparators("/", basePath.relativize(folderObj.getParent()).toString());
				folderProperties.put(StructrApp.key(Folder.class, "parent"), getExistingFolder(parentPath));
			}

			// load properties from files.json
			final PropertyMap properties = getConvertedPropertiesForFileOrFolder(folderPath);
			if (properties != null) {
				folderProperties.putAll(properties);
			}

			if (existingFolder == null) {

				final Folder newFolder = app.create(Folder.class, folderProperties);

				this.folderCache.put(folderPath, newFolder);

			} else {

				existingFolder.unlockSystemPropertiesOnce();
				existingFolder.setProperties(securityContext, folderProperties);
			}

			tx.success();

		} catch (Exception ex) {
			logger.error("Error occured while importing folder " + folderObj, ex);
		}
	}

	private void createFile(final Path path, final String fileName) throws IOException {

		boolean deferFile  = false;
		String newFileUuid = null;

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final String fullPath                   = harmonizeFileSeparators("/", basePath.relativize(path).toString());
			final Map<String, Object> rawProperties = getRawPropertiesForFileOrFolder(fullPath);

			if (rawProperties == null) {

				if (!fileName.startsWith(".")) {
					logger.info("Ignoring {} (not in files.json)", fullPath);
				}

			} else {

				Object minificationSources = null;
				if (rawProperties.containsKey("minificationSources")) {
					deferFile = true;
					minificationSources = rawProperties.remove("minificationSources");
				}

				final PropertyMap fileProperties = convertRawPropertiesForFileOrFolder(rawProperties);

				final PropertyKey isThumbnailKey = StructrApp.key(Image.class, "isThumbnail");

				Folder parent = null;

				if (!basePath.equals(path.getParent())) {
					final String parentPath  = harmonizeFileSeparators("/", basePath.relativize(path.getParent()).toString());
					parent = getExistingFolder(parentPath);
				}

				boolean skipFile         = false;

				if (fileProperties.containsKey(isThumbnailKey) && (boolean) fileProperties.get(isThumbnailKey)) {

					logger.info("Thumbnail image found: {}, ignoring. Please delete file in files directory and entry in files.json.", fullPath);
					skipFile = true;
				}

				File file = app.nodeQuery(File.class).and(StructrApp.key(File.class, "parent"), parent).and(File.name, fileName).getFirst();

				if (file != null) {

					final Long checksumOfExistingFile = FileHelper.getChecksum(file.getFileOnDisk());
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

						final PropertyMap props = new PropertyMap();

						props.put(StructrApp.key(AbstractFile.class, "name"), fileName);

						if (parent != null) {

							props.put(StructrApp.key(File.class, "hasParent"), true);
							props.put(StructrApp.key(File.class, "parent"), parent);
						}

						newFileUuid = fileProperties.get(GraphObject.id);

						if (newFileUuid != null) {
							props.put(StructrApp.key(GraphObject.class, "id"), newFileUuid);
						}

						// create file in folder structure
						file                     = FileHelper.createFile(securityContext, fis, File.class, props);
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

					if (deferFile) {
						deferredFiles.add(file);
						rawProperties.put("minificationSources", minificationSources);
					} else {
						file.unlockSystemPropertiesOnce();
						file.setProperties(securityContext, fileProperties);
					}
				}

				if (newFileUuid != null) {

					final File createdFile = app.get(File.class, newFileUuid);
					String type            = createdFile.getType();
					boolean isImage        = createdFile instanceof Image;

					logger.debug("File {}: {}, isImage? {}", new Object[] { createdFile.getName(), type, isImage });

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

		final Class<Relation> thumbnailRel = StructrApp.getConfiguration().getRelationshipEntityClass("ImageTHUMBNAILImage");

		if (img.getProperty(StructrApp.key(Image.class, "isThumbnail"))) {

			// thumbnail image
			if (img.getIncomingRelationship(thumbnailRel) == null) {

				ImageHelper.findAndReconnectOriginalImage(img);
			}

		} else {

			// original image
			if (!img.getOutgoingRelationships(thumbnailRel).iterator().hasNext()) {

				ImageHelper.findAndReconnectThumbnails(img);

			}
		}
	}

	private Map<String, Object> getRawPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Object data = metadata.get(path);
		if (data != null && data instanceof Map) {

			return (Map<String, Object>)data;
		}

		return null;
	}

	private PropertyMap getConvertedPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Map<String, Object> data = getRawPropertiesForFileOrFolder(path);
		if (data != null) {

			return convertRawPropertiesForFileOrFolder(data);
		}

		return null;
	}

	private PropertyMap convertRawPropertiesForFileOrFolder(final Map<String, Object> data) throws FrameworkException {

		DeployCommand.checkOwnerAndSecurity(data, false);

		return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), StructrApp.getConfiguration().getNodeEntityClass((String)data.get("type")), data);

	}

	private String harmonizeFileSeparators(final String... sources) {

		final StringBuilder buf = new StringBuilder();

		for (final String src : sources) {
			buf.append(src);
		}

		int pos = buf.indexOf("\\");

		while (pos >= 0) {

			buf.replace(pos, pos+1, "/");
			pos = buf.indexOf("\\");
		}

		return buf.toString();
	}
}
