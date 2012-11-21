/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService.NodeIndex;

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
 * @author Axel Morgner
 */
public class RemoveNodeFromIndex extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(RemoveNodeFromIndex.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<Enum, Index> indices = new HashMap<Enum, Index>();

	//~--- methods --------------------------------------------------------

	public void execute(List<AbstractNode> nodes) throws FrameworkException {
		
		init();
		removeNodesFromAllIndices(nodes);
	}

	public void execute(AbstractNode node) throws FrameworkException {
		
		init();
		removeNodeFromAllIndices(node);
	}

	private void init() {

		for (Enum indexName : (NodeIndex[]) arguments.get("indices")) {

			indices.put(indexName, (Index<Node>) arguments.get(indexName.name()));

		}
		
	}
	
	private void removeNodesFromAllIndices(final List<AbstractNode> nodes) {

		for (AbstractNode node : nodes) {

			removeNodeFromAllIndices(node);

		}
	}

	private void removeNodeFromAllIndices(final AbstractNode node) {

		if (node.getProperty(AbstractNode.uuid) == null) {

			logger.log(Level.WARNING, "Will not remove node from indices which has no UUID");

			return;

		}                
                
		for (Enum indexName : (NodeIndex[]) arguments.get("indices")) {

			Index<Node> index = indices.get(indexName);
			synchronized(index) {
				index.remove(node.getNode());
			}

		}
	}
}
