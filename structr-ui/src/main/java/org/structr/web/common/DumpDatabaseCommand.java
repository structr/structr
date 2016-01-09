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
package org.structr.web.common;

import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.SyncCommand;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.rest.resource.MaintenanceParameterResource;
public class DumpDatabaseCommand extends NodeServiceCommand implements MaintenanceCommand {

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("dumpDatabase", DumpDatabaseCommand.class);
	}

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		try {

			final GraphDatabaseService graphDb = Services.getInstance().getService(NodeService.class).getGraphDb();
			final GlobalGraphOperations ggop  = GlobalGraphOperations.at(graphDb);
			final Iterable<Relationship> rels = ggop.getAllRelationships();
			final Iterable<Node> nodes        = ggop.getAllNodes();
			final App app                     = StructrApp.getInstance();
			final String fileName             = (String)attributes.get("name");

			if (fileName == null || fileName.isEmpty()) {

				throw new FrameworkException(400, "Please specify name.");
			}

			try (final Tx tx = app.tx()) {

				final File file = FileHelper.createFile(securityContext, new byte[0], "application/zip", File.class, fileName);

				// make file visible for auth. users
				file.setProperty(File.visibleToAuthenticatedUsers, true);

				// Don't include files
				SyncCommand.exportToStream(file.getOutputStream(), app.nodeQuery(NodeInterface.class).getAsList(), app.relationshipQuery(RelationshipInterface.class).getAsList(), null, false);

				tx.success();
			}

		} catch (Throwable t) {

			throw new FrameworkException(500, t.getMessage());
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}
}
