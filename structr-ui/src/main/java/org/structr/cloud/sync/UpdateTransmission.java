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
package org.structr.cloud.sync;

import java.io.IOException;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.CloudTransmission;
import org.structr.cloud.message.NodeDataContainer;
import org.structr.cloud.message.RelationshipDataContainer;
import org.structr.cloud.transmission.PushTransmission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.dynamic.File;

/**
 *
 *
 */
public class UpdateTransmission implements CloudTransmission {

	private static final Logger logger = Logger.getLogger(UpdateTransmission.class.getName());


	@Override
	public Boolean doRemote(final CloudConnection client) throws IOException, FrameworkException {

		// send synchronization request first
		client.send(new Synchronize());

		// send all node and relationship data
		final GraphDatabaseService graphDb   = StructrApp.getInstance().getGraphDatabaseService();
		final GlobalGraphOperations ggo      = GlobalGraphOperations.at(graphDb);
		final NodeFactory nodeFactory        = new NodeFactory(SecurityContext.getSuperUserInstance());
		final RelationshipFactory relFactory = new RelationshipFactory(SecurityContext.getSuperUserInstance());

		for (final Node neo4jNode : ggo.getAllNodes()) {

			final NodeInterface node = nodeFactory.instantiate(neo4jNode);

			if (node instanceof File) {

				PushTransmission.sendFile(client, (File)node, CloudService.CHUNK_SIZE);

			} else {

				client.send(new NodeDataContainer(node, 0));
			}
		}

		for (final Relationship relationship : ggo.getAllRelationships()) {

			final RelationshipInterface relationshipInterface = relFactory.instantiate(relationship);
			client.send(new RelationshipDataContainer(relationshipInterface, 0));
		}

		// wait for end of transmission
		client.waitForTransmission();

		return true;
	}
}
