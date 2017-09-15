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
package org.structr.web.maintenance;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;

/**
 *
 */
public class DirectFileImportCommand extends NodeServiceCommand implements MaintenanceCommand {

	private enum Mode     { COPY, MOVE }
	private enum Existing { SKIP, OVERWRITE, RENAME }

	private static final Logger logger = LoggerFactory.getLogger(DirectFileImportCommand.class.getName());

	private Integer folderCount = 0;
	private Integer fileCount   = 0;
	private Integer stepCounter = 0;

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("directFileImport", DirectFileImportCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
		final String sourcePath       = getParameterValueAsString(attributes, "source", null);
		final String targetPath       = getParameterValueAsString(attributes, "target", null);
		final Mode mode               = getModeEnum(getParameterValueAsString(attributes, "mode", Mode.COPY.name()).toUpperCase());
		final Existing existing       = getExistingEnum(getParameterValueAsString(attributes, "existing", Existing.SKIP.name()).toUpperCase());
		final boolean doIndex         = Boolean.parseBoolean(getParameterValueAsString(attributes, "index", Boolean.TRUE.toString()));

		if (StringUtils.isBlank(sourcePath)) {

			throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
		}

		final Path source = Paths.get(sourcePath);
		if (!Files.exists(source)) {

			throw new FrameworkException(422, "Source path " + sourcePath + " does not exist.");
		}

		if (!Files.isDirectory(source)) {

			throw new FrameworkException(422, "Source path " + sourcePath + " is not a directory.");
		}

		try {

			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app             = StructrApp.getInstance(ctx);
			Folder targetFolder       = null;

			ctx.setDoTransactionNotifications(false);

			if (StringUtils.isNotBlank(targetPath) && !("/".equals(targetPath))) {

				try (final Tx tx = app.tx()) {

					targetFolder = app.nodeQuery(Folder.class).and(Folder.path, targetPath).getFirst();
					if (targetFolder == null) {
						throw new FrameworkException(422, "Target path " + targetPath + " does not exist.");
					}
					tx.success();
				}
			}

			String msg = "Starting direct file import from source directory " + sourcePath + " into target path " + targetPath;

			logger.info(msg);
			publishProgressMessage(msg);

			Files.walkFileTree(source, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

					ctx.setDoTransactionNotifications(false);

					final Path name = file.getFileName();

					try (final Tx tx = app.tx()) {

						final String filesPath    = Settings.FilesPath.getValue();
						final String relativePath = PathHelper.getRelativeNodePath(source.toString(), file.toString());
						final String parentPath   = targetPath + PathHelper.getFolderPath(relativePath);

						if (attrs.isDirectory()) {

							final Folder newFolder = app.create(Folder.class,
									new NodeAttribute(Folder.name, name.toString()),
									new NodeAttribute(FileBase.parent, FileHelper.createFolderPath(securityContext, parentPath))
							);

							folderCount++;

							logger.info("Created folder " + newFolder.getPath());

						} else if (attrs.isRegularFile()) {

							final FileBase existingFile = app.nodeQuery(FileBase.class).and(AbstractFile.path, "/" + relativePath).getFirst();
							if (existingFile != null) {

								switch (existing) {

									case SKIP:
										return FileVisitResult.CONTINUE;

									case OVERWRITE:
										app.delete(existingFile);
										break;

									case RENAME:
										final String newName = existingFile.getProperty(AbstractFile.name).concat("_").concat(FileHelper.getDateString());
										existingFile.setProperty(AbstractFile.name, newName);
										break;

								}
							}

							final FileBase newFile = app.create(org.structr.dynamic.File.class,
								new NodeAttribute(FileBase.name, name.toString()),
								new NodeAttribute(FileBase.parent, FileHelper.createFolderPath(securityContext, parentPath)),
								new NodeAttribute(AbstractNode.type, "File")
							);

							final String uuid             = newFile.getUuid();
							final String relativeFilePath = FileBase.getDirectoryPath(uuid) + "/" + uuid;
							final Path targetPath         = Paths.get(filesPath + "/" + relativeFilePath);

							newFile.setProperty(FileBase.relativeFilePath, relativeFilePath);

							Files.createDirectories(targetPath.getParent());

							switch (mode) {

								case MOVE:
									Files.move(file, targetPath);
									break;

								case COPY:
									Files.copy(file, targetPath);
									break;

							}

							FileHelper.updateMetadata(newFile);

							if (doIndex) {
								indexer.addToFulltextIndex(newFile);
							}

							fileCount++;

							logger.info("Created file " + newFile.getPath());
						}

						tx.success();

					} catch (IOException | FrameworkException ex) {
						logger.debug("File: " + name + ", path: " + sourcePath, ex);
					}

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

			msg = "Finished direct file import from source directory " + sourcePath + ". Imported " + folderCount + " folders and " + fileCount + " files.";
			logger.info(msg);
			publishProgressMessage(msg);

		} catch (IOException ex) {
			logger.debug("Mode: " + mode + ", path: " + sourcePath, ex);
		}

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	// ----- private methods -----
	private Mode getModeEnum(final String src) throws FrameworkException {

		try { return Mode.valueOf(src); } catch (IllegalArgumentException ex) {}

		throw new FrameworkException(422, "Unknown value for 'mode' attribute. Valid values are: copy, move");
	}

	private Existing getExistingEnum(String src) throws FrameworkException {

		try { return Existing.valueOf(src); } catch (IllegalArgumentException ex) {}

		throw new FrameworkException(422, "Unknown value for 'existing' attribute. Valid values are: skip, overwrite, rename");
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
