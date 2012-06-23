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



package org.structr.core.node;

import org.neo4j.gis.spatial.indexprovider.SpatialRecordHits;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalCommand;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.module.GetEntityClassCommand;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.Constructor;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr nodes. This class exists because we need a fast
 * way to instantiate and initialize structr nodes, as this is the most-
 * used operation.
 *
 * @author cmorgner
 */
public class NodeFactory<T extends AbstractNode> {

	private static final Logger logger = Logger.getLogger(NodeFactory.class.getName());

	//~--- fields ---------------------------------------------------------

	private ThreadLocalCommand getEntityClassCommand = new ThreadLocalCommand(GetEntityClassCommand.class);
	private Map<Class, Constructor> constructors     = new LinkedHashMap<Class, Constructor>();

	//~--- constructors ---------------------------------------------------

	// private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();
	public NodeFactory() {}

	//~--- methods --------------------------------------------------------
	public AbstractNode createNode(final SecurityContext securityContext, final Node node) throws FrameworkException {

		return createNode(securityContext, node, false, false);

	}

	public AbstractNode createNode(final SecurityContext securityContext, final Node node, final boolean includeDeletedAndHidden, final boolean publicOnly) throws FrameworkException {

		String type     = AbstractNode.Key.type.name();
		String nodeType = node.hasProperty(type)
				  ? (String) node.getProperty(type)
				  : "";

		return createNode(securityContext, node, nodeType, includeDeletedAndHidden, publicOnly);

	}
	
	public AbstractNode createNode(final SecurityContext securityContext, final Node node, final String nodeType) throws FrameworkException {
		return createNode(securityContext, node, nodeType, false, false);
	}

	public AbstractNode createNode(final SecurityContext securityContext, final Node node, final String nodeType, final boolean includeDeletedAndHidden, final boolean publicOnly) throws FrameworkException {

		/*
		 *  caching disabled for now...
		 * AbstractNode cachedNode = null;
		 *
		 * // only look up node in cache if uuid is already present
		 * if(node.hasProperty(AbstractNode.Key.uuid.name())) {
		 *       String uuid = (String)node.getProperty(AbstractNode.Key.uuid.name());
		 *       cachedNode = NodeService.getNodeFromCache(uuid);
		 * }
		 *
		 * if(cachedNode == null) {
		 */
		Class nodeClass      = (Class) getEntityClassCommand.get().execute(nodeType);
		AbstractNode newNode = null;

		if (nodeClass != null) {

			try {

				Constructor constructor = constructors.get(nodeClass);

				if (constructor == null) {

					constructor = nodeClass.getConstructor();

					constructors.put(nodeClass, constructor);

				}

				// newNode = (AbstractNode) nodeClass.newInstance();
				newNode = (AbstractNode) constructor.newInstance();

			} catch (Throwable t) {

				newNode = null;

			}

		}

		if (newNode == null) {

			newNode = new GenericNode();
		}

		newNode.init(securityContext, node);
		newNode.onNodeInstantiation();

		// check access
		if (isReadable(securityContext, newNode, includeDeletedAndHidden, publicOnly)) {

			return newNode;
		}

		return null;
	}
	
	public List<AbstractNode> createNodes(final SecurityContext securityContext, final IndexHits<Node> input) throws FrameworkException {
		return createNodes(securityContext, input, false, false);
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * Include only nodes which are readable in the given security context.
	 * If includeDeletedAndHidden is true, include nodes with 'deleted' flag
	 * If publicOnly is true, filter by 'visibleToPublicUsers' flag
	 *
	 * @param securityContext
	 * @param input
	 * @param includeDeletedAndHidden
	 * @param publicOnly
	 * @return
	 */
	public List<AbstractNode> createNodes(final SecurityContext securityContext, final IndexHits<Node> input, final boolean includeDeletedAndHidden, final boolean publicOnly)
		throws FrameworkException {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		if (input != null && input instanceof SpatialRecordHits) {

			Command graphDbCommand       = Services.command(securityContext, GraphDatabaseCommand.class);
			GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();

			if (input.iterator().hasNext()) {

				for (Node node : input) {

					AbstractNode n = createNode(securityContext, graphDb.getNodeById((Long) node.getProperty("id")), includeDeletedAndHidden, publicOnly);

					// Check is done in createNode already, so we don't have to do it again
					if (n != null) {// && isReadable(securityContext, n, includeDeletedAndHidden, publicOnly)) {

						nodes.add(n);
					}

					for (AbstractNode nodeAt : getNodesAt(n)) {

						if (nodeAt != null && isReadable(securityContext, nodeAt, includeDeletedAndHidden, publicOnly)) {

							nodes.add(nodeAt);
						}

					}

				}

			}

		} else {

			if ((input != null) && input.iterator().hasNext()) {

				for (Node node : input) {

					AbstractNode n = createNode(securityContext, (Node) node, includeDeletedAndHidden, publicOnly);

					// Check is done in createNode already, so we don't have to do it again
					if (n != null) {//  && isReadable(securityContext, n, includeDeletedAndHidden, publicOnly)) {

						nodes.add(n);
					}

				}

			}

		}

		return nodes;

	}
	
	/**
	 * Create structr nodes from all given underlying database nodes
	 *
	 * @param input
	 * @return
	 */
	public List<AbstractNode> createNodes(final SecurityContext securityContext, final Iterable<Node> input) throws FrameworkException {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		if ((input != null) && input.iterator().hasNext()) {

			for (Node node : input) {

				AbstractNode n = createNode(securityContext, node);

				if (n != null) {
					
					nodes.add(n);
					
				}

			}

		}

		return nodes;

	}

	public AbstractNode createDeletedNode(final SecurityContext securityContext, final Node node, final String nodeType) throws FrameworkException {

		Class nodeClass      = (Class) getEntityClassCommand.get().execute(nodeType);
		AbstractNode newNode = null;

		if (nodeClass != null) {

			try {

				Constructor constructor = constructors.get(nodeClass);

				if (constructor == null) {

					constructor = nodeClass.getConstructor();

					constructors.put(nodeClass, constructor);

				}

				// newNode = (AbstractNode) nodeClass.newInstance();
				newNode = (AbstractNode) constructor.newInstance();

			} catch (Throwable t) {

				newNode = null;

			}

		}

		if (newNode == null) {

			newNode = new GenericNode();
		}
		
		return newNode;
	}

	//~--- get methods ----------------------------------------------------

	private List<AbstractNode> getNodesAt(final AbstractNode locationNode) {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		for (AbstractRelationship rel : locationNode.getRelationships(RelType.IS_AT, Direction.INCOMING)) {

			nodes.add(rel.getStartNode());
		}

		return nodes;

	}

	private boolean isReadable(final SecurityContext securityContext, final AbstractNode node, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		/**
		 * The if-clauses in the following lines have been split
		 * for performance reasons.
		 */

		// deleted and hidden nodes will only be returned if we are told to do so
		if ((node.isDeleted() || node.isHidden()) && !includeDeletedAndHidden) {

			return false;
		}

		// visibleToPublic overrides anything else
		// Publicly visible nodes will always be returned
		if (node.isVisibleToPublicUsers()) {

			return true;
		}

		// Next check is only for non-public nodes, because
		// public nodes are already added one step above.
		if (publicOnly) {

			return false;
		}

		// Ask the security context
		if (securityContext.isAllowed(node, Permission.read)) {

			return true;
		}

		return false;
	}

}
