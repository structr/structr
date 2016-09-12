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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.web.maintenance.deploy.PageImportVisitor;

/**
 *
 * @author Christian Morgner
 */
public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkMoveUnusedFilesCommand.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		String mode = (String)attributes.get("mode");
		if (mode == null) {

			mode = "dir";
		}

		switch (mode) {

			case "dir":
				deployFromDirectory(attributes);
				break;

			case "file":
				deployFromFile(attributes);
				break;
		}

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	// ----- private methods -----
	private void deployFromFile(final Map<String, Object> data) throws FrameworkException {
	}

	private void deployFromDirectory(final Map<String, Object> data) throws FrameworkException {

		final String path = (String)data.get("source");
		if (StringUtils.isBlank(path)) {

			throw new FrameworkException(422, "Please provide source path for deployment.");
		}

		final Path source = Paths.get(path);
		if (!Files.exists(source)) {

			throw new FrameworkException(422, "Source path " + path + " does not exist.");
		}

		if (!Files.isDirectory(source)) {

			throw new FrameworkException(422, "Source path " + path + " is not a directory.");
		}

		final Path pages = source.resolve("pages");
		final Path files = source.resolve("files");

		logger.log(Level.INFO, "Deploying pages from {0}..", pages);

		try {
			Files.walkFileTree(pages, new PageImportVisitor());

		} catch (IOException ioex) {
			logger.log(Level.WARNING, "Exception while importing pages", ioex);
		}

	}
}
