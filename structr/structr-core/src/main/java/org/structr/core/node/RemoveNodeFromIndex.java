/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Command for removing nodes from the index
 *
 * @author axel
 */
public class RemoveNodeFromIndex extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(RemoveNodeFromIndex.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<String, Index> indices = new HashMap<String, Index>();

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		for (Enum indexName : (NodeIndex[]) arguments.get("indices")) {

			indices.put(indexName.name(), (Index<Node>) arguments.get(indexName.name()));

		}

		long id           = 0;
		AbstractNode node = null;

		switch (parameters.length) {

			case 1 :

				// remove this node from index
				if (parameters[0] instanceof Long) {

					id = ((Long) parameters[0]).longValue();

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

					removeNodeFromAllIndices(node);

				} else if (parameters[0] instanceof String) {

					id = Long.parseLong((String) parameters[0]);

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

					removeNodeFromAllIndices(node);

				} else if (parameters[0] instanceof AbstractNode) {

					node = (AbstractNode) parameters[0];

					removeNodeFromAllIndices(node);

				} else if (parameters[0] instanceof List) {

					removeNodesFromIndex((List<AbstractNode>) parameters[0]);

				}

				break;

			default :
				logger.log(Level.SEVERE, "Wrong number of parameters for the remove node from index command: {0}", parameters);

				return null;

		}

		return null;
	}

	private void removeNodesFromIndex(final List<AbstractNode> nodes) {

		for (AbstractNode node : nodes) {

			removeNodeFromAllIndices(node);

		}
	}

	private void removeNodeFromAllIndices(final AbstractNode node) {

		for (Enum indexName : (NodeIndex[]) arguments.get("indices")) {

			indices.get(indexName.name()).remove(node.getNode());

		}
	}
}
