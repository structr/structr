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
package org.structr.core.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

/**
 * Deletes nodes in batches.
 */
public class BulkDeleteCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(BulkDeleteCommand.class.getName());

	public void bulkDelete(final Iterable<GraphObject> iterable) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		final long count = bulkOperation(securityContext, iterable, 1000, "DeleteObjects", new BulkGraphOperation<GraphObject>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final GraphObject obj) {

				boolean success = true;

				try {

					if (obj.isNode()) {

						final NodeInterface node = (NodeInterface)obj;

						if (!node.isGranted(Permission.delete, securityContext)) {

							logger.warn("Could not delete {} because {} has no delete permission", obj.getUuid(), securityContext.getUser(false));
							success = false;

						} else {

							app.delete(node);
						}

					} else {

						app.delete((RelationshipInterface)obj);
					}

				} catch (FrameworkException fex) {

					logger.warn("Unable to delete node {}: {}", obj.getUuid(), fex.toString());
					success = false;
				}

				return success;
			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, GraphObject node) {
				logger.warn("Unable to delete node {}", node.getUuid());
			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
				logger.warn("Unable to delete nodes {}", t.toString());
			}
		});

		info("Done with deleting {} nodes", count);

	}
}
