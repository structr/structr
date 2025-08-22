/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger               = LoggerFactory.getLogger(FileImportVisitor.class.getName());
	protected Map<String, Object> metadata           = null;
	protected SecurityContext securityContext        = null;
	protected Path basePath                          = null;
	protected App app                                = null;
	protected Map<String, NodeInterface> folderCache = null;

	private final ArrayList<String> encounteredPaths              = new ArrayList<>();
	private final Set<String> encounteredButNotConfiguredPaths    = new HashSet<>();
	private final Map<String, String> forceRenamedFilesAndFolders = new HashMap<>();

	public FileImportVisitor(final SecurityContext securityContext, final Path basePath, final Map<String, Object> metadata) {

		this.securityContext = securityContext;
		this.basePath        = basePath;
		this.metadata        = metadata;
		this.app             = StructrApp.getInstance(this.securityContext);
		this.folderCache     = new HashMap<>();
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

		boolean processSubtree = true;

		if (!basePath.equals(dir)) {

			processSubtree = createFolder(dir);
		}

		if (processSubtree) {

			return FileVisitResult.CONTINUE;

		} else {

			return FileVisitResult.SKIP_SUBTREE;
		}
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

		// this is basically "remaining metadata after import" because for encountered files, this metadata is removed
		final Set<String> configuredButNotEncounteredPaths = new HashSet<>(metadata.keySet());
		encounteredPaths.forEach(configuredButNotEncounteredPaths::remove);

		return new FileImportProblems(configuredButNotEncounteredPaths, encounteredButNotConfiguredPaths, forceRenamedFilesAndFolders);
	}

	// ----- private methods -----
	protected NodeInterface getExistingFolder(final String path) throws FrameworkException {

		if (this.folderCache.containsKey(path)) {

			return this.folderCache.get(path);

		} else {

			// get properties to find UUID
			final Map<String, Object> raw = getRawPropertiesForFileOrFolder(path);
			if (raw != null) {

				final NodeInterface existingFolder = app.getNodeById(StructrTraits.FOLDER, (String) raw.get("id"));
				if (existingFolder != null) {

					this.folderCache.put(path, existingFolder);
				}

				return existingFolder;
			}
		}

		return null;
	}

	protected boolean createFolder(final Path path) {

		final String fullPath = harmonizeFileSeparators("/", basePath.relativize(path).toString());
		final Traits traits     = Traits.of(StructrTraits.FOLDER);

		encounteredPaths.add(fullPath);

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final NodeInterface existingFolder = getExistingFolder(fullPath);
			final PropertyMap folderProperties = new PropertyMap(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path.getFileName().toString());

			if (!basePath.equals(path.getParent())) {

				final String parentPath = harmonizeFileSeparators("/", basePath.relativize(path.getParent()).toString());
				folderProperties.put(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), getExistingFolder(parentPath));
			}

			// load properties from files.json
			final PropertyMap properties = getConvertedPropertiesForFileOrFolder(fullPath);
			if (properties != null) {

				folderProperties.putAll(properties);

			} else {

				logger.info("Ignoring folder and subtree: {} (not in files.json)", fullPath);

				encounteredButNotConfiguredPaths.add(path.toString());

				return false;
			}

			if (existingFolder == null) {

				final NodeInterface newFolder = app.create(StructrTraits.FOLDER, folderProperties);

				this.folderCache.put(fullPath, newFolder);

			} else {

				existingFolder.unlockSystemPropertiesOnce();
				existingFolder.setProperties(securityContext, folderProperties);
			}

			tx.success();

		} catch (Exception ex) {

			logger.error("Error occurred while importing folder " + path, ex);
		}

		checkIfFileOrFolderWasForceRenamed(traits, fullPath, path.getFileName().toString());

		return true;
	}

	protected void createFile(final Path path, final String fileName) throws IOException {

		String newFileUuid    = null;
		final Traits traits   = Traits.of(StructrTraits.FILE);
		final String fullPath = harmonizeFileSeparators("/", basePath.relativize(path).toString());
		boolean wasIgnored    = false;

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final Map<String, Object> rawProperties = getRawPropertiesForFileOrFolder(fullPath);
			final PropertyKey<String> idProperty    = traits.key(GraphObjectTraitDefinition.ID_PROPERTY);

			encounteredPaths.add(fullPath);

			if (rawProperties == null) {

				logger.info("Ignoring file: {} (not in files.json)", fullPath);

				encounteredButNotConfiguredPaths.add(path.toString());
				wasIgnored = true;

			} else {

				final PropertyMap fileProperties = new PropertyMap(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName);
				fileProperties.putAll(convertRawPropertiesForFileOrFolder(rawProperties));

				NodeInterface parent = null;
				boolean skipFile     = false;

				if (!basePath.equals(path.getParent())) {

					final String parentPath = harmonizeFileSeparators("/", basePath.relativize(path.getParent()).toString());
					parent = getExistingFolder(parentPath);

					// add parent to fileProperties so a file gets moved if appropriate
					fileProperties.put(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent);
				}

				if (traits.hasKey(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY)) {

					final PropertyKey<Boolean> isThumbnailKey = traits.key(ImageTraitDefinition.IS_THUMBNAIL_PROPERTY);

					if (fileProperties.containsKey(isThumbnailKey) && fileProperties.get(isThumbnailKey)) {

						logger.info("Thumbnail image found: {}, ignoring. Please delete file in files directory and entry in files.json.", fullPath);
						skipFile = true;
					}
				}

				NodeInterface file = app.getNodeById(StructrTraits.FILE, fileProperties.get(traits.key(GraphObjectTraitDefinition.ID_PROPERTY)));
				if (file != null) {

					final Long checksumOfExistingFile = FileHelper.getChecksum(file.as(File.class));
					final Long checksumOfNewFile      = FileHelper.getChecksum(path.toFile());

					if (checksumOfExistingFile != null && checksumOfExistingFile.equals(checksumOfNewFile) && file.getUuid().equals(rawProperties.get("id"))) {

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
						String fileType         = StructrTraits.FILE;

						props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName);

						// make sure the file is created with the same UUID
						props.put(idProperty, fileProperties.get(idProperty));

						if (parent != null) {

							props.put(traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY), true);
							props.put(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent);
						}

						newFileUuid = fileProperties.get(idProperty);

						if (newFileUuid != null) {
							props.put(traits.key(GraphObjectTraitDefinition.ID_PROPERTY), newFileUuid);
						}

						if (fileProperties.containsKey(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY))) {

							final String typeFromConfig = fileProperties.get(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY));
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

						file = FileHelper.createFile(securityContext, fis, fileType, props);


						newFileUuid = file.getUuid();
					}
				}

				if (file != null) {

					file.unlockSystemPropertiesOnce();
					file.setProperties(securityContext, fileProperties);
				}

				if (newFileUuid != null) {

					final NodeInterface createdFile = app.getNodeById(StructrTraits.FILE, newFileUuid);
					String type                     = createdFile.getType();
					boolean isImage                 = createdFile.is(StructrTraits.IMAGE);

					logger.debug("File {}: {}, isImage? {}", createdFile.getName(), type, isImage);

					if (isImage) {

						try {

							ImageHelper.updateMetadata(createdFile.as(File.class));
							handleThumbnails(createdFile.as(Image.class));

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

		if (!wasIgnored) {

			checkIfFileOrFolderWasForceRenamed(traits, fullPath, fileName);
		}
	}

	protected void checkIfFileOrFolderWasForceRenamed(final Traits traits, final String fullPath, final String targetName) {

		try (final Tx tx = app.tx(true, false, false)) {

			final PropertyMap properties = getConvertedPropertiesForFileOrFolder(fullPath);
			final NodeInterface node     = app.getNodeById(properties.get(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY)), properties.get(traits.key(GraphObjectTraitDefinition.ID_PROPERTY)));

			if (node != null) {

				if (!node.getName().equals(targetName)) {

					forceRenamedFilesAndFolders.put(fullPath, node.getProperty(traits.key(AbstractFileTraitDefinition.PATH_PROPERTY)));
				}
			}

			tx.success();

		} catch (Exception ex) {

			logger.error("Error occurred while checking imported file/folder: " + fullPath, ex);
		}
	}

	private void handleThumbnails(final Image img) {

		final String thumbnailRel = StructrTraits.IMAGE_THUMBNAIL_IMAGE;
		final Traits traits       = Traits.of(thumbnailRel);

		if (img.isThumbnail()) {

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

		return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), (String)data.get("type"), data);

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

		final Set<String> configuredButNotEncountered;
		final Set<String> encounteredButNotConfigured;
		final Map<String, String> forceRenamedFilesAndFolders;

		public FileImportProblems(final Set<String> configuredButNotEncountered, final Set<String> encounteredButNotConfigured, final Map<String, String> forceRenamedFilesAndFolders) {

			this.configuredButNotEncountered = configuredButNotEncountered;
			this.encounteredButNotConfigured = encounteredButNotConfigured;
			this.forceRenamedFilesAndFolders = forceRenamedFilesAndFolders;
		}

		public boolean hasAnyProblems() {

			return !(configuredButNotEncountered.isEmpty() && encounteredButNotConfigured.isEmpty() && forceRenamedFilesAndFolders.isEmpty());
		}

		public String getProblemsHtml() {

			final ArrayList<String> problems = new ArrayList<>();

			if (!configuredButNotEncountered.isEmpty()) {

				problems.add(
						"The following entries were configured in files.json, but the <b>expected files/folders were not found</b>. The most common cause is that files.json was correctly committed, but the file itself was not added to the repository."
						+ "<ul><li>" + configuredButNotEncountered.stream().sorted().collect(Collectors.joining("</li><li>")) + "</li></ul>"
				);
			}

			if (!encounteredButNotConfigured.isEmpty()) {

				problems.add(
						"The following files/folders were ignored because they <b>are missing in files.json</b>. For folders the complete subtree is ignored. The most common cause is that files.json was not correctly committed."
						+ "<ul><li>" + encounteredButNotConfigured.stream().sorted().collect(Collectors.joining("</li><li>")) + "</li></ul>"
				);
			}

			if (!forceRenamedFilesAndFolders.isEmpty()) {

				problems.add(
						"The following files/folders were auto-renamed to prevent name clashes. The most common cause is that a previously installed app was not removed completely. If files/folders are linked in the application, this link will not be restored and thus the app will not work as expected. Removing the clashing file/folder and re-importing is recommended."
						+ "<ul><li>" + forceRenamedFilesAndFolders.entrySet().stream().map(entry -> entry.getKey() + " renamed to " + entry.getValue()).collect(Collectors.joining("</li><li>")) + "</li></ul>"
				);
			}

			return String.join("<br>", problems);
		}

		public String getProblemsText() {

			final ArrayList<String> problems = new ArrayList<>();

			if (!configuredButNotEncountered.isEmpty()) {

				problems.add("\tThe following entries were configured in files.json, but the expected files/folders were not found. The most common cause is that files.json was correctly committed, but the file itself was not added to the repository.\n\t\t" + configuredButNotEncountered.stream().sorted().collect(Collectors.joining("\n\t\t")));
			}

			if (!encounteredButNotConfigured.isEmpty()) {

				problems.add("\tThe following files/folders were ignored because they are missing in files.json. For folders the complete subtree is ignored. The most common cause is that files.json was not correctly committed.\n\t\t" + encounteredButNotConfigured.stream().sorted().collect(Collectors.joining("\n\t\t")));
			}

			if (!forceRenamedFilesAndFolders.isEmpty()) {

				problems.add("\tThe following files/folders were auto-renamed to prevent name clashes. The most common cause is that a previously installed app was not removed completely. If files/folders are linked in the application, this link will not be restored and thus the app will not work as expected. Removing the clashing file/folder and re-importing is recommended.\n\t\t" + forceRenamedFilesAndFolders.entrySet().stream().map(entry -> entry.getKey() + " renamed to " + entry.getValue()).collect(Collectors.joining("\n\t\t")));
			}

			return String.join("\n\n", problems);
		}
	}
}
