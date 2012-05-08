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

import org.neo4j.gis.spatial.indexprovider.SpatialRecordHits;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.Principal;
import org.structr.core.module.GetEntityClassCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
public class NodeFactory<T extends AbstractNode> implements Adapter<Node, T> {

	private static final Logger logger = Logger.getLogger(NodeFactory.class.getName());

	//~--- fields ---------------------------------------------------------

	private SecurityContext securityContext = null;

	//~--- constructors ---------------------------------------------------

	// private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();
	public NodeFactory() {}

	public NodeFactory(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	//~--- methods --------------------------------------------------------

	public AbstractNode createNode(SecurityContext securityContext, final Node node) throws FrameworkException {

		String nodeType = node.hasProperty(AbstractNode.Key.type.name())
				  ? (String) node.getProperty(AbstractNode.Key.type.name())
				  : "";

		return createNode(securityContext, node, nodeType);
	}

	public AbstractNode createNode(final SecurityContext securityContext, final Node node, final String nodeType) throws FrameworkException {

		Class nodeClass      = (Class) Services.command(securityContext, GetEntityClassCommand.class).execute(nodeType);
		AbstractNode newNode = null;

		if (nodeClass != null) {

			try {
				newNode = (AbstractNode) nodeClass.newInstance();
			} catch (Throwable t) {
				newNode = null;
			}

		}

		if (newNode == null) {

			newNode = new GenericNode();

		}

		newNode.init(securityContext, node);
		newNode.onNodeInstantiation();

		return newNode;
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * Include only nodes which are readable in the given security context.
	 * If includeDeleted is true, include nodes with 'deleted' flag
	 * If publicOnly is true, filter by 'public' flag
	 *
	 * @param securityContext
	 * @param input
	 * @param includeDeleted
	 * @param publicOnly
	 * @return
	 */
	public List<AbstractNode> createNodes(final SecurityContext securityContext, final IndexHits<Node> input, final boolean includeDeleted, final boolean publicOnly) throws FrameworkException {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		if (input != null && input instanceof SpatialRecordHits) {

			Command graphDbCommand       = Services.command(securityContext, GraphDatabaseCommand.class);
			GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();

			if (input.iterator().hasNext()) {

				for (Node node : input) {

					AbstractNode n = createNode(securityContext, graphDb.getNodeById((Long) node.getProperty("id")));

					addIfReadable(securityContext, n, nodes, includeDeleted, publicOnly);

					for (AbstractNode nodeAt : getNodesAt(n)) {

						addIfReadable(securityContext, nodeAt, nodes, includeDeleted, publicOnly);

					}

				}

			}

		} else {

			if ((input != null) && input.iterator().hasNext()) {

				for (Node node : input) {

					AbstractNode n = createNode(securityContext, (Node) node);

					addIfReadable(securityContext, n, nodes, includeDeleted, publicOnly);

				}

			}

		}

		return nodes;
	}

	private void addIfReadable(final SecurityContext securityContext, final AbstractNode n, List<AbstractNode> nodes, final boolean includeDeleted, final boolean publicOnly) {

		boolean readableByUser = securityContext.isAllowed(n, Permission.Read);

		if (readableByUser && (includeDeleted ||!n.isDeleted()) && (n.isVisibleToPublicUsers() ||!publicOnly)) {

			nodes.add(n);

		}
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * Include only nodes which are readable in the given security context.
	 * If includeDeleted is true, include nodes with 'deleted' flag
	 *
	 * @param input
	 * @param user
	 * @param includeDeleted
	 * @return
	 */
	public List<AbstractNode> createNodes(final SecurityContext securityContext, final Iterable<Node> input, final boolean includeDeleted) throws FrameworkException {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();
		Principal user                = securityContext.getUser();

		if ((input != null) && input.iterator().hasNext()) {

			for (Node node : input) {

				AbstractNode n                  = createNode(securityContext, node);
				boolean readableByUser          = ((user instanceof SuperUser) || securityContext.isAllowed(n, Permission.Read));
				boolean publicUserAndPublicNode = ((user == null) && n.isVisibleToPublicUsers());

				if ((readableByUser || publicUserAndPublicNode) && (includeDeleted ||!n.isDeleted())) {

					nodes.add(n);

				}

			}

		}

		return nodes;
	}

//
//      /**
//       * Create structr nodes from the underlying database nodes
//       *
//       * If includeDeleted is true, include nodes with 'deleted' flag
//       *
//       * @param input
//       * @param includeDeleted
//       * @return
//       */
//      public List<AbstractNode> createNodes(final Iterable<Node> input, final boolean includeDeleted) {
//
//              List<AbstractNode> nodes = new LinkedList<AbstractNode>();
//
//              if ((input != null) && input.iterator().hasNext()) {
//
//                      for (Node node : input) {
//
//                              AbstractNode n = createNode(node);
//
//                              if (includeDeleted ||!n.isDeleted()) {
//                                      nodes.add(n);
//                              }
//                      }
//              }
//
//              return nodes;
//      }

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

				nodes.add(n);

			}

		}

		return nodes;
	}

//      @Override
//      protected void finalize() throws Throwable {
//          nodeTypeCache.clear();
//      }
	@Override
	public T adapt(Node s) {

		try {
			return ((T) createNode(securityContext, s));
		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to adapt node", fex);
		}

		return null;
	}

//	public AbstractNode createNode(final SecurityContext securityContext, final NodeDataContainer data) throws FrameworkException {
//
//		if (data == null) {
//
//			logger.log(Level.SEVERE, "Could not create node: Empty data container.");
//
//			return null;
//
//		}
//
//		Map properties       = data.getProperties();
//		String nodeType      = properties.containsKey(AbstractNode.Key.type.name())
//				       ? (String) properties.get(AbstractNode.Key.type.name())
//				       : null;
//		Class nodeClass      = (Class) Services.command(securityContext, GetEntityClassCommand.class).execute(nodeType);
//		AbstractNode newNode = null;
//
//		if (nodeClass != null) {
//
//			try {
//				newNode = (AbstractNode) nodeClass.newInstance();
//			} catch (Throwable t) {
//				newNode = null;
//			}
//
//		}
//
//		if (newNode == null) {
//
//			newNode = new GenericNode();
//
//		}
//
//		newNode.init(securityContext, data);
//		newNode.commit(null);
//		newNode.onNodeInstantiation();
//
//		if (data instanceof FileNodeDataContainer) {
//
//			FileNodeDataContainer container = (FileNodeDataContainer) data;
//			File fileNode                   = (File) newNode;
//			String relativeFilePath         = newNode.getId() + "_" + System.currentTimeMillis();
//			String path                     = Services.getFilesPath() + "/" + relativeFilePath;
//
//			// rename temporary file to new location etc.
//			if (container.persistTemporaryFile(path)) {
//
//				fileNode.setSize(container.getFileSize());
//				fileNode.setRelativeFilePath(relativeFilePath);
//
//			}
//
//		}
//
//		return newNode;
//	}

	//~--- get methods ----------------------------------------------------

	private List<AbstractNode> getNodesAt(final AbstractNode locationNode) {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		for (AbstractRelationship rel : locationNode.getRelationships(RelType.IS_AT, Direction.INCOMING)) {

			nodes.add(rel.getStartNode());

		}

		return nodes;
	}
}
