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
package org.structr.web.maintenance.deploy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.maintenance.DeployCommand;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static org.structr.core.graph.NodeInterface.name;

public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger      = LoggerFactory.getLogger(FileImportVisitor.class.getName());
	protected Map<String, Object> metadata    = null;
	protected SecurityContext securityContext = null;
	protected Path basePath                   = null;
	protected App app                         = null;
	protected Map<String, Folder> folderCache = null;

	private final ArrayList<String> encounteredPaths               = new ArrayList<>();
	private final Set<String> encounteredButNotConfiguredFilePaths = new HashSet<>();

	public FileImportVisitor(final SecurityContext securityContext, final Path basePath, final Map<String, Object> metadata) {

		this.securityContext = securityContext;
		this.basePath        = basePath;
		this.metadata        = metadata;
		this.app             = StructrApp.getInstance(this.securityContext);
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

		logger.warn("Exception while importing file {}: {}", file.toString(), exc.getMessage());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public FileImportProblems getFileImportProblems() {

		final Set configuredButNotEncounteredPaths = new HashSet(metadata.keySet());
		configuredButNotEncounteredPaths.removeAll(encounteredPaths);

		return new FileImportProblems(configuredButNotEncounteredPaths, encounteredButNotConfiguredFilePaths);
	}

	// ----- private methods -----
	protected Folder getExistingFolder(final String path) throws FrameworkException {

		if (this.folderCache.containsKey(path)) {

			return this.folderCache.get(path);

		} else {

			// get properties to find UUID
			final Map<String, Object> raw = getRawPropertiesForFileOrFolder(path);
			if (raw != null) {

				final Folder existingFolder = app.get(Folder.class, (String) raw.get("id"));
				if (existingFolder != null) {

					this.folderCache.put(path, existingFolder);
				}

				return existingFolder;
			}
		}

		return null;
	}

	protected void createFolder(final Path folderObj) {

		final String folderPath = harmonizeFileSeparators("/", basePath.relativize(folderObj).toString());

		encounteredPaths.add(folderPath);

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

			logger.error("Error occurred while importing folder " + folderObj, ex);
		}
	}

	protected void createFile(final Path path, final String fileName) throws IOException {

		String newFileUuid = null;

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final String fullPath                   = harmonizeFileSeparators("/", basePath.relativize(path).toString());
			final Map<String, Object> rawProperties = getRawPropertiesForFileOrFolder(fullPath);

			encounteredPaths.add(fullPath);

			if (rawProperties == null) {

				logger.info("Ignoring {} (not in files.json)", fullPath);

				encounteredButNotConfiguredFilePaths.add(path.toString());

			} else {

				final PropertyMap fileProperties = new PropertyMap(AbstractNode.name, fileName);
				fileProperties.putAll(convertRawPropertiesForFileOrFolder(rawProperties));

				final PropertyKey isThumbnailKey = StructrApp.key(Image.class, "isThumbnail");

				Folder parent    = null;
				boolean skipFile = false;

				if (!basePath.equals(path.getParent())) {

					final String parentPath  = harmonizeFileSeparators("/", basePath.relativize(path.getParent()).toString());
					parent = getExistingFolder(parentPath);
				}

				if (fileProperties.containsKey(isThumbnailKey) && (boolean) fileProperties.get(isThumbnailKey)) {

					logger.info("Thumbnail image found: {}, ignoring. Please delete file in files directory and entry in files.json.", fullPath);
					skipFile = true;
				}

				File file = app.get(File.class, fileProperties.get(AbstractNode.id));

				if (file != null) {

					final Long checksumOfExistingFile = FileHelper.getChecksum(file);
					final Long checksumOfNewFile      = FileHelper.getChecksum(path.toFile());

					if (checksumOfExistingFile != null && checksumOfNewFile != null && checksumOfExistingFile.equals(checksumOfNewFile) && file.getUuid().equals(rawProperties.get("id"))) {

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
						Class fileType          = File.class;

						props.put(StructrApp.key(AbstractFile.class, "name"), fileName);

						// make sure the file is created with the same UUID
						props.put(AbstractNode.id, fileProperties.get(AbstractNode.id));

						if (parent != null) {

							props.put(StructrApp.key(File.class, "hasParent"), true);
							props.put(StructrApp.key(File.class, "parent"), parent);
						}

						newFileUuid = fileProperties.get(GraphObject.id);

						if (newFileUuid != null) {
							props.put(StructrApp.key(GraphObject.class, "id"), newFileUuid);
						}

						if (fileProperties.containsKey(AbstractNode.type)) {

							final Class typeFromConfig = StructrApp.getConfiguration().getNodeEntityClass(fileProperties.get(AbstractNode.type));
							if (typeFromConfig != null) {

								fileType = typeFromConfig;
							}
						}

						// create file in folder structure
						//final String contentType = file.getContentType();

						// modify file type according to content
						/*
						if (StringUtils.startsWith(contentType, "image") || ImageHelper.isImageType(file.getProperty(name))) {

							file.unlockSystemPropertiesOnce();
							file.setProperties(securityContext, new PropertyMap(NodeInterface.type, Image.class.getSimpleName()));
						}

						 */

						file                     = FileHelper.createFile(securityContext, fis, fileType, props);


						newFileUuid = file.getUuid();
					}
				}

				if (file != null) {

					file.unlockSystemPropertiesOnce();
					file.setProperties(securityContext, fileProperties);
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

	protected Map<String, Object> getRawPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Object data = metadata.get(path);
		if (data != null && data instanceof Map) {

			return (Map<String, Object>)data;
		}

		return null;
	}

	protected PropertyMap getConvertedPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Map<String, Object> data = getRawPropertiesForFileOrFolder(path);
		if (data != null) {

			return convertRawPropertiesForFileOrFolder(data);
		}

		return null;
	}

	protected PropertyMap convertRawPropertiesForFileOrFolder(final Map<String, Object> data) throws FrameworkException {

		DeployCommand.checkOwnerAndSecurity(data, false);

		return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), StructrApp.getConfiguration().getNodeEntityClass((String)data.get("type")), data);

	}

	protected String harmonizeFileSeparators(final String... sources) {

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

	public class FileImportProblems {

		final Set configuredButNotEncountered;
		final Set encounteredButNotConfigured;

		public FileImportProblems(final Set configuredButNotEncountered, final Set encounteredButNotConfigured) {

			this.configuredButNotEncountered = configuredButNotEncountered;
			this.encounteredButNotConfigured = encounteredButNotConfigured;
		}

		public boolean hasAnyProblems() {

			return configuredButNotEncountered.size() > 0 || encounteredButNotConfigured.size() > 0;
		}

		public String getProblemsHtml() {

			final ArrayList<String> problems = new ArrayList();

			if (configuredButNotEncountered.size() > 0) {

				problems.add(
						"The following entries were configured in files.json, but the <b>expected files were not found</b>. The most common cause is that files.json was correctly committed, but the file itself was not added to the repository."
						+ "<ul><li>" + String.join("</li><li>", configuredButNotEncountered) + "</li></ul>"
				);
			}

			if (encounteredButNotConfigured.size() > 0) {

				problems.add(
						"The following files were found, but <b>are missing in files.json</b>. The most common cause is that files.json was not correctly committed."
						+ "<ul><li>" + String.join("</li><li>", encounteredButNotConfigured) + "</li></ul>"
				);
			}

			return String.join("<br>", problems);
		}

		public String getProblemsText() {

			final ArrayList<String> problems = new ArrayList();

			if (configuredButNotEncountered.size() > 0) {

				problems.add("\tThe following entries were configured in files.json, but the expected files were not found. The most common cause is that files.json was correctly committed, but the file itself was not added to the repository.\n\t\t" + String.join("\n\t\t", configuredButNotEncountered));
			}

			if (encounteredButNotConfigured.size() > 0) {

				problems.add("\tThe following files were found, but are missing in files.json. The most common cause is that files.json was not correctly committed.\n\t\t" + String.join("\n\t\t", encounteredButNotConfigured));
			}

			return String.join("\n\n", problems);
		}
	}
}
