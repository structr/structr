/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.web.entity.FileBase;

//~--- classes ----------------------------------------------------------------
/**
 * Move all files on disk which are not referenced by a node to a special folder
 *
 */
public class BulkMoveUnusedFilesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkMoveUnusedFilesCommand.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("moveUnusedFiles", BulkMoveUnusedFilesCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		String mode = (String) properties.get("mode");
		String targetDir = (String) properties.get("target");

		if (StringUtils.isBlank(mode)) {
			// default
			mode = "log";
		}

		if (StringUtils.isBlank(targetDir)) {
			// default
			targetDir = "unused";
		}

		logger.info("Starting moving of unused files...");

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		final App app = StructrApp.getInstance();

		final String filesLocation = StructrApp.getConfigurationValue(Services.FILES_PATH);

		final Set<String> filePaths = new TreeSet<>();

		if (graphDb != null) {

			List<FileBase> fileNodes = null;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				fileNodes = app.nodeQuery(FileBase.class).getAsList();

				for (final FileBase fileNode : fileNodes) {

					final String relativeFilePath = fileNode.getProperty(FileBase.relativeFilePath);

					if (relativeFilePath != null) {
						filePaths.add(relativeFilePath);
					}
				}

				tx.success();
			}

			final List<Path> files = new LinkedList<>();

			try {

				Files.walk(Paths.get(filesLocation), FileVisitOption.FOLLOW_LINKS).filter(Files::isRegularFile).forEach((Path file) -> {
					files.add(file);
				});

			} catch (IOException ex) {
				logger.error("", ex);
			}

			Path targetDirPath = null;

			if (targetDir.startsWith("/")) {
				targetDirPath = Paths.get(targetDir);
			} else {
				targetDirPath = Paths.get(filesLocation, targetDir);

			}

			if (mode.equals("move") && !Files.exists(targetDirPath)) {

				try {
					targetDirPath = Files.createDirectory(targetDirPath);

				} catch (IOException ex) {
					logger.info("Could not create target directory {}: {}", new Object[]{targetDir, ex});
					return;
				}
			}

			for (final Path file : files) {

				final String filePath = file.toString();
				final String relPath = StringUtils.stripStart(filePath.substring(filesLocation.length()), "/");

				//System.out.println("files location: " + filesLocation + ", file path: " + filePath + ", rel path: " + relPath);
				if (!filePaths.contains(relPath)) {

					if (mode.equals("log")) {

						System.out.println("File " + file + " doesn't exist in database (rel path: " + relPath + ")");

					} else if (mode.equals("move")) {

						try {
							final Path targetPath = Paths.get(targetDirPath.toString(), file.getFileName().toString());

							Files.move(file, targetPath);

							System.out.println("File " + file.getFileName() + " moved to " + targetPath);

						} catch (IOException ex) {
							logger.info("Could not move file {} to target directory {}: {}", new Object[]{file, targetDir, ex});
						}

					}
				}

			}

		}

		logger.info("Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
