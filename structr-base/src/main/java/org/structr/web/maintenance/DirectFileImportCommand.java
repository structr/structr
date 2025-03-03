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
package org.structr.web.maintenance;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.helper.PathHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DirectFileImportCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(DirectFileImportCommand.class.getName());

	static {

		MaintenanceResource.registerMaintenanceCommand("directFileImport", DirectFileImportCommand.class);
	}

	private enum Mode     { COPY, MOVE }
	private enum Existing { SKIP, OVERWRITE, RENAME }

	private FulltextIndexer indexer = null;
	private Integer folderCount     = 0;
	private Integer fileCount       = 0;
	private Integer stepCounter     = 0;

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();

		final String sourcePath     = getParameterValueAsString(attributes, "source", null);
		final String modeString     = getParameterValueAsString(attributes, "mode", Mode.COPY.name()).toUpperCase();
		final String existingString = getParameterValueAsString(attributes, "existing", Existing.SKIP.name()).toUpperCase();
		final boolean doIndex       = Boolean.parseBoolean(getParameterValueAsString(attributes, "index", Boolean.TRUE.toString()));

		if (StringUtils.isBlank(sourcePath)) {
			throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
		}

		if (!EnumUtils.isValidEnum(Mode.class, modeString)) {
			throw new FrameworkException(422, "Unknown value for 'mode' attribute. Valid values are: copy, move");
		}

		if (!EnumUtils.isValidEnum(Existing.class, existingString)) {
			throw new FrameworkException(422, "Unknown value for 'existing' attribute. Valid values are: skip, overwrite, rename");
		}

		// use actual enums
		final Existing existing = Existing.valueOf(existingString);
		final Mode mode         = Mode.valueOf(modeString);

		final List<Path> paths = new ArrayList<>();

		if (sourcePath.contains(PathHelper.PATH_SEP)) {

			final String folderPart = PathHelper.getFolderPath(sourcePath);
			final String namePart   = PathHelper.getName(sourcePath);

			if (StringUtils.isNotBlank(folderPart)) {

				final Path source = Paths.get(folderPart);
				if (!Files.exists(source)) {

					throw new FrameworkException(422, "Source path " + sourcePath + " does not exist.");
				}

				if (!Files.isDirectory(source)) {

					throw new FrameworkException(422, "Source path " + sourcePath + " is not a directory.");
				}

				try {

					try (final DirectoryStream<Path> stream = Files.newDirectoryStream(source, namePart)) {

						for (final Path entry: stream) {
							paths.add(entry);
						}

					} catch (final DirectoryIteratorException ex) {
						throw ex.getCause();
					}

				} catch (final IOException ioex) {
					throw new FrameworkException(422, "Unable to parse source path " + sourcePath + ".");
				}
			}


		} else {

			// Relative path
			final Path source = Paths.get(Settings.BasePath.getValue()).resolve(sourcePath);
			if (!Files.exists(source)) {

				throw new FrameworkException(422, "Source path " + sourcePath + " does not exist.");
			}

			paths.add(source);

		}

		final SecurityContext ctx  = SecurityContext.getSuperUserInstance();
		final App app              = StructrApp.getInstance(ctx);
		String targetPath          = getParameterValueAsString(attributes, "target", "/");
		NodeInterface targetFolder = null;

		ctx.setDoTransactionNotifications(false);

		if (StringUtils.isNotBlank(targetPath) && !("/".equals(targetPath))) {

			try (final Tx tx = app.tx()) {

				targetFolder = app.nodeQuery(StructrTraits.FOLDER).and(Traits.of(StructrTraits.FOLDER).key("path"), targetPath).getFirst();
				if (targetFolder == null) {

					throw new FrameworkException(422, "Target path " + targetPath + " does not exist.");
				}

				tx.success();
			}
		}

		String msg = "Starting direct file import from source directory " + sourcePath + " into target path " + targetPath;
		logger.info(msg);
		publishProgressMessage(msg);

		paths.forEach((path) -> {

			try {

				final String newTargetPath;

				// If path is a directory, create it and use it as the new target folder
				if (Files.isDirectory(path)) {

					Path parentPath = path.getParent();
					if (parentPath == null) {
						parentPath = path;
					}

					createFileOrFolder(ctx, app, parentPath, path, Files.readAttributes(path, BasicFileAttributes.class), sourcePath, targetPath, mode, existing, doIndex);

					newTargetPath = targetPath + PathHelper.PATH_SEP + PathHelper.clean(path.getFileName().toString());

				} else {

					newTargetPath = targetPath;
				}

				Files.walkFileTree(path, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
						return createFileOrFolder(ctx, app, path, file, attrs, sourcePath, newTargetPath, mode, existing, doIndex);
					}

					@Override
					public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});

			} catch (final IOException ex) {
				logger.debug("Mode: " + modeString + ", path: " + sourcePath, ex);
			}
		});

		msg = "Finished direct file import from source directory " + sourcePath + ". Imported " + folderCount + " folders and " + fileCount + " files.";
		logger.info(msg);

		publishProgressMessage(msg);
	}

	private FileVisitResult createFileOrFolder(final SecurityContext ctx, final App app, final Path path, final Path file, final BasicFileAttributes attrs,
			final String sourcePath, final String targetPath, final Mode mode, final Existing existing, final boolean doIndex) {

		ctx.setDoTransactionNotifications(false);

		final String name = file.getFileName().toString();

		try (final Tx tx = app.tx()) {

			final String relativePath = PathHelper.getRelativeNodePath(path.toString(), file.toString());
			String parentPath         = targetPath + PathHelper.getFolderPath(relativePath);

			// fix broken path concatenation
			if (parentPath.startsWith("//")) {
				parentPath = parentPath.substring(1);
			}

			if (attrs.isDirectory()) {

				final NodeInterface newFolder = app.create(StructrTraits.FOLDER,
						new NodeAttribute(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
						new NodeAttribute(Traits.of(StructrTraits.FILE).key("parent"), FileHelper.createFolderPath(securityContext, parentPath))
				);

				folderCount++;

				logger.info("Created folder " + newFolder.as(Folder.class).getPath());

			} else if (attrs.isRegularFile()) {

				final NodeInterface existingFile = app.nodeQuery(StructrTraits.FILE).and(Traits.of(StructrTraits.ABSTRACT_FILE).key("path"), parentPath + name).getFirst();
				if (existingFile != null) {

					switch (existing) {

						case SKIP:
							logger.info("Skipping import of {}, file exists and mode is SKIP.", parentPath + name);
							return FileVisitResult.CONTINUE;

						case OVERWRITE:
							logger.info("Overwriting {}, file exists and mode is OVERWRITE.", parentPath + name);
							app.delete(existingFile);
							break;

						case RENAME:
							logger.info("Renaming existing file {}, file exists and mode is RENAME.", parentPath + name);
							existingFile.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), existingFile.getName().concat("_").concat(FileHelper.getDateString()));
							break;
					}

				}

				final String contentType = FileHelper.getContentMimeType(file.toFile(), file.getFileName().toString());

				boolean isImage = (contentType != null && contentType.startsWith("image"));
				boolean isVideo = (contentType != null && contentType.startsWith("video"));

				Traits traits = null;

				if (isImage) {

					traits = Traits.of(StructrTraits.IMAGE);

				} else if (isVideo) {

					traits = Traits.of("VideoFile");
					if (traits == null) {

						logger.warn("Unable to create entity of type VideoFile, class is not defined.");
					}

				} else {

					traits = Traits.of(StructrTraits.FILE);
				}

				final NodeInterface newFile = app.create(traits.getName(),
						new NodeAttribute(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
						new NodeAttribute(Traits.of(StructrTraits.FILE).key("parent"), FileHelper.createFolderPath(securityContext, parentPath)),
						new NodeAttribute(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), traits.getName())
				);

				try (final InputStream is = new FileInputStream(file.toFile()); final OutputStream os = StorageProviderFactory.getStorageProvider(newFile.as(File.class)).getOutputStream()) {
					IOUtils.copy(is, os);
				}

				if (mode.equals(Mode.MOVE)) {

					Files.delete(file);
				}

				FileHelper.updateMetadata(newFile.as(File.class));

				if (doIndex) {
					indexer.addToFulltextIndex(newFile);
				}

				fileCount++;
			}

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.debug("File: " + name + ", path: " + sourcePath, ex);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	private String getParameterValueAsString(final Map<String, Object> attributes, final String key, final String defaultValue) {

		Object value = attributes.get(key);

		if (value != null) {

			return value.toString();
		}

		return defaultValue;
	}

	private void publishProgressMessage (final String message) {

		final Map<String, Object> msgData = new HashMap();
		msgData.put("type", "DIRECT_IMPORT_STATUS");
		msgData.put("subtype", "PROGRESS");
		msgData.put("message", message);
		msgData.put("step", ++stepCounter);

		TransactionCommand.simpleBroadcastGenericMessage(msgData);

	}

	private void publishWarnigMessage (final String title, final String text) {

		final Map<String, Object> warningMsgData = new HashMap();
		warningMsgData.put("type", "WARNING");
		warningMsgData.put("title", title);
		warningMsgData.put("text", text);

		TransactionCommand.simpleBroadcastGenericMessage(warningMsgData);

	}

}
