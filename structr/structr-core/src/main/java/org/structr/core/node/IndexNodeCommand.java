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

import org.apache.commons.lang.StringUtils;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;
import org.structr.core.entity.Person;
import org.structr.core.entity.User;
import org.structr.core.node.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Command for indexing nodes
 *
 * @author axel
 */
public class IndexNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(IndexNodeCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<String, Index> indices = new HashMap<String, Index>();

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		for (Enum indexName : (NodeIndex[]) arguments.get("indices")) {

			indices.put(indexName.name(), (Index<Node>) arguments.get(indexName.name()));

		}

		long id           = 0;
		AbstractNode node = null;
		String key        = null;

		switch (parameters.length) {

			case 1 :

				// index all properties of this node
				if (parameters[0] instanceof Long) {

					id = ((Long) parameters[0]).longValue();

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

					indexNode(node);

				} else if (parameters[0] instanceof String) {

					id = Long.parseLong((String) parameters[0]);

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

					indexNode(node);

				} else if (parameters[0] instanceof AbstractNode) {

					node = (AbstractNode) parameters[0];

					indexNode(node);

				} else if (parameters[0] instanceof List) {

					indexNodes((List<AbstractNode>) parameters[0]);

				}

				break;

			case 2 :

				// index a certain property
				if (parameters[0] instanceof Long) {

					id = ((Long) parameters[0]).longValue();

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

				} else if (parameters[0] instanceof String) {

					id = Long.parseLong((String) parameters[0]);

					Command findNode = Services.command(securityContext, FindNodeCommand.class);

					node = (AbstractNode) findNode.execute(id);

				} else if (parameters[0] instanceof AbstractNode) {

					node = (AbstractNode) parameters[0];

					// id   = node.getId();

				}

				if (parameters[1] instanceof String) {

					key = (String) parameters[1];

				}

				indexProperty(node, key);

				break;

			default :
				logger.log(Level.SEVERE, "Wrong number of parameters for the index node command: {0}", parameters);

				return null;

		}

		return null;
	}

	private void indexNodes(final List<AbstractNode> nodes) {

		for (AbstractNode node : nodes) {

			indexNode(node);

		}
	}

	private void indexNode(final AbstractNode node) {

		for (Enum index : (NodeIndex[]) arguments.get("indices")) {

			Set<String> properties = EntityContext.getSearchableProperties(node.getClass(), index.name());

			for (String key : properties) {

				indexProperty(node, key, index.name());

			}

		}

                Node dbNode = node.getNode();
                
		if ((dbNode.hasProperty(Location.Key.latitude.name())) && (dbNode.hasProperty(Location.Key.longitude.name()))) {

			LayerNodeIndex layerIndex = (LayerNodeIndex) indices.get(NodeIndex.layer.name());

			layerIndex.add(node.getNode(), "", "");

		}
	}

	private void indexProperty(final AbstractNode node, final String key) {

		for (Enum index : (NodeIndex[]) arguments.get("indices")) {

			Set<String> properties = EntityContext.getSearchableProperties(node.getClass(), index.name());

			if ((properties != null) && properties.contains(key)) {

				indexProperty(node, key, index.name());

			}

		}
	}

	private void indexProperty(final AbstractNode node, final String key, final String indexName) {

		// String type = node.getClass().getSimpleName();
		Node dbNode = node.getNode();
		long id     = node.getId();

		if (key == null) {

			logger.log(Level.SEVERE, "Node {0} has null key", new Object[] { id });

			return;

		}

		boolean emptyKey = StringUtils.isEmpty((String) key);

		if (emptyKey) {

			logger.log(Level.SEVERE, "Node {0} has empty, not-null key, removing property", new Object[] { id });
			dbNode.removeProperty(key);

			return;

		}

		Object value            = node.getProperty(key);    // dbNode.getProperty(key);
		Object valueForIndexing = node.getPropertyForIndexing(key);
		boolean emptyValue      = ((value instanceof String) && StringUtils.isEmpty((String) value));

		if (value == null) {

			logger.log(Level.FINE, "Node {0} has null value for key {1}, removing property", new Object[] { id, key });
			dbNode.removeProperty(key);
			removeNodePropertyFromIndex(dbNode, key, indexName);

		} else if (emptyValue) {

			logger.log(Level.FINE, "Node {0} has empty, non-null value for key {1}, removing property", new Object[] { id, key });
			dbNode.removeProperty(key);
			removeNodePropertyFromIndex(dbNode, key, indexName);

		} else {

			// index.remove(node, key, value);
			removeNodePropertyFromIndex(dbNode, key, indexName);
			logger.log(Level.FINE, "Node {0}: Old value for key {1} removed from all indices", new Object[] { id, key });
			addNodePropertyToIndex(dbNode, key, valueForIndexing, indexName);

			if ((node instanceof User) && (key.equals(AbstractNode.Key.name.name()) || key.equals(Person.Key.email.name()))) {

				removeNodePropertyFromIndex(dbNode, key, NodeIndex.user.name());
				addNodePropertyToIndex(dbNode, key, valueForIndexing, NodeIndex.user.name());

			}

			if (key.equals(AbstractNode.Key.uuid.name())) {

				removeNodePropertyFromIndex(dbNode, key, NodeIndex.uuid.name());
				addNodePropertyToIndex(dbNode, key, valueForIndexing, NodeIndex.uuid.name());

			}

			logger.log(Level.FINE, "Node {0}: New value {2} added for key {1}", new Object[] { id, key, value });
		}
	}

	private void removeNodePropertyFromIndex(final Node node, final String key, final String indexName) {
		indices.get(indexName).remove(node, key);
	}

	private void addNodePropertyToIndex(final Node node, final String key, final Object value, final String indexName) {
		indices.get(indexName).add(node, key, value);
	}
}
