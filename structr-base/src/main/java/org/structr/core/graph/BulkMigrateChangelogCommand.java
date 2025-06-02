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
package org.structr.core.graph;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.function.ChangelogFunction;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Maintenance command to migrate structrChangeLog property values
 * to the new disk-based storage.
 */
public class BulkMigrateChangelogCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkMigrateChangelogCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> properties) throws FrameworkException {

		final long nodeCount = bulkGraphOperation(securityContext, StructrApp.getInstance().nodeQuery(), 1000, "MigrateChangeLog", new BulkGraphOperation<NodeInterface>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, NodeInterface node) {

				handleObject(node);
				return true;
			}
		});

		logger.info("Migrated {} nodes ...", nodeCount);

		final long relCount = bulkGraphOperation(securityContext, StructrApp.getInstance().relationshipQuery(), 1000, "MigrateChangeLog", new BulkGraphOperation<AbstractRelationship>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, AbstractRelationship rel) {

				handleObject(rel);
				return true;
			}
		});

		logger.info("Migrated {} relationships ...", relCount);
		logger.info("Done");
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
	private void handleObject(final GraphObject obj) {

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		final String changeLogName                = "structrChangeLog";

		if (propertyContainer.hasProperty(changeLogName)) {

			final Object changeLogSource   = propertyContainer.getProperty(changeLogName);
			if (changeLogSource instanceof String) {

				final String existingChangeLog = (String)changeLogSource;
				if (StringUtils.isNotBlank(existingChangeLog)) {

					if (writeChangelogToDisk(obj, existingChangeLog)) {

						// remove data in case of success
						propertyContainer.removeProperty(changeLogName);
					}
				}
			}
		}

	}

	private boolean writeChangelogToDisk(final GraphObject obj, final String changeLogValue) {

		try {

			if (obj != null) {

				final String uuid           = obj.getUuid();
				final String typeFolderName = obj.isNode() ? "n" : "r";
				final File file             = ChangelogFunction.getChangeLogFileOnDisk(typeFolderName, uuid, true);
				final StringBuilder buf     = new StringBuilder();

				// prepend existing data
				buf.append(changeLogValue);

				// read file data
				buf.append(FileUtils.readFileToString(file, "utf-8"));

				// write concatenated data
				FileUtils.write(file, buf.toString(), "utf-8", false);

				return true;
			}

		} catch (IOException ioex) {
			logger.error("Unable to write changelog to file: {}", ioex.getMessage());
		} catch (Throwable t) {
			logger.warn("", t);
		}

		return false;
	}
}
