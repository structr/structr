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

import org.structr.api.DatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.TypeProperty;

import java.util.Collections;
import java.util.Map;

/**
 * Create labels for all nodes of the given type.
 */
public class BulkCreateLabelsCommand extends NodeServiceCommand implements MaintenanceCommand, TransactionPostProcess {

	@Override
	public void execute(Map<String, Object> attributes) {

		executeWithCount(attributes);
	}

	public long executeWithCount(Map<String, Object> attributes) {

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		final String entityType       = (String) attributes.get("type");
		final boolean removeUnused    = !attributes.containsKey("removeUnused");

		if (entityType == null) {

			info("Node type not set or no entity class found. Starting creation of labels for all nodes.");

		} else {

			info("Starting creation of labels for all nodes of type {}", entityType);
		}

		final long count = bulkGraphOperation(securityContext, getNodeQuery(entityType, true), 10000, "CreateLabels", new BulkGraphOperation<NodeInterface>() {

			@Override
			public boolean handleGraphObject(SecurityContext securityContext, NodeInterface node) {

				TypeProperty.updateLabels(graphDb, node, node.getTraits(), removeUnused);

				return true;
			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, NodeInterface node) {
				warn("Unable to create labels for node {}: {}", node, t.getMessage());
			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
				warn("Unable to create labels for node: {}", t.getMessage());
			}
		});

		info("Done with creating labels on {} nodes", count);
		return count;
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	// ----- interface TransactionPostProcess -----
	@Override
	public boolean execute(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		execute(Collections.EMPTY_MAP);

		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return true;
	}
}
