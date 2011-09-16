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

import org.structr.common.CurrentRequest;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.Adapter;
import org.structr.core.Services;
import org.structr.core.cloud.FileNodeDataContainer;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ArbitraryNode;
import org.structr.core.entity.File;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntityClassCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.SuperUser;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr nodes. This class exists because we need a fast
 * way to instantiate and initialize structr nodes, as this is the most-
 * used operation.
 *
 * @author cmorgner
 */
public class StructrNodeFactory<T extends AbstractNode> implements Adapter<Node, T> {

	private static final Logger logger = Logger.getLogger(StructrNodeFactory.class.getName());

	//~--- constructors ---------------------------------------------------

	// private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();
	public StructrNodeFactory() {}

	//~--- methods --------------------------------------------------------

	public AbstractNode createNode(final Node node) {

		String nodeType = node.hasProperty(AbstractNode.TYPE_KEY)
				  ? (String) node.getProperty(AbstractNode.TYPE_KEY)
				  : "";

		return createNode(node, nodeType);
	}

	public AbstractNode createNode(final Node node, final String nodeType) {

		Class nodeClass      = (Class) Services.command(GetEntityClassCommand.class).execute(nodeType);
		AbstractNode newNode = null;

		if (nodeClass != null) {

			try {
				newNode = (AbstractNode) nodeClass.newInstance();
			} catch (Throwable t) {
				newNode = null;
			}
		}

		if (newNode == null) {
			newNode = new ArbitraryNode();
		}

		newNode.init(node);
		newNode.onNodeInstantiation();

		return newNode;
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * If user is given, include only nodes which are readable by given user
	 * If includeDeleted is true, include nodes with 'deleted' flag
	 * If publicOnly is true, filter by 'public' flag
	 *
	 * @param input
	 * @param user
	 * @param includeDeleted
	 * @param publicOnly
	 * @return
	 */
	public List<AbstractNode> createNodes(final Iterable<Node> input, final User user,
		final boolean includeDeleted, final boolean publicOnly) {

		SecurityContext securityContext = CurrentRequest.getSecurityContext();
		List<AbstractNode> nodes        = new LinkedList<AbstractNode>();

		if ((input != null) && input.iterator().hasNext()) {

			for (Node node : input) {

				AbstractNode n                  = createNode(node);
				boolean readableByUser          = (user instanceof SuperUser || securityContext.isAllowed(n, Permission.Read));
				boolean publicUserAndPublicNode = ((user == null) && n.isPublic());

				if ((readableByUser || publicUserAndPublicNode) && (includeDeleted ||!n.isDeleted())
					&& (n.isPublic() ||!publicOnly)) {
					nodes.add(n);
				}
			}
		}

		return nodes;
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * If user is given, include only nodes which are readable by given user
	 * If includeDeleted is true, include nodes with 'deleted' flag
	 *
	 * @param input
	 * @param user
	 * @param includeDeleted
	 * @return
	 */
	public List<AbstractNode> createNodes(final Iterable<Node> input, final User user,
		final boolean includeDeleted) {
		return createNodes(input, user, includeDeleted, false);
	}

	/**
	 * Create structr nodes from the underlying database nodes
	 *
	 * If includeDeleted is true, include nodes with 'deleted' flag
	 *
	 * @param input
	 * @param includeDeleted
	 * @return
	 */
	public List<AbstractNode> createNodes(final Iterable<Node> input, final boolean includeDeleted) {
		return createNodes(input, new SuperUser(), includeDeleted);
	}

	/**
	 * Create structr nodes from all given underlying database nodes
	 *
	 * @param input
	 * @return
	 */
	public List<AbstractNode> createNodes(final Iterable<Node> input) {
		return createNodes(input, true);
	}

//      @Override
//      protected void finalize() throws Throwable {
//          nodeTypeCache.clear();
//      }
	@Override
	public T adapt(Node s) {
		return ((T) createNode(s));
	}

	public AbstractNode createNode(final NodeDataContainer data) {

		if (data == null) {

			logger.log(Level.SEVERE, "Could not create node: Empty data container.");

			return null;
		}

		Map properties       = data.getProperties();
		String nodeType      = properties.containsKey(AbstractNode.TYPE_KEY)
				       ? (String) properties.get(AbstractNode.TYPE_KEY)
				       : null;
		Class nodeClass      = (Class) Services.command(GetEntityClassCommand.class).execute(nodeType);
		AbstractNode newNode = null;

		if (nodeClass != null) {

			try {
				newNode = (AbstractNode) nodeClass.newInstance();
			} catch (Throwable t) {
				newNode = null;
			}
		}

		if (newNode == null) {
			newNode = new ArbitraryNode();
		}

		newNode.init(data);
		newNode.commit(null);
		newNode.onNodeInstantiation();

		if (data instanceof FileNodeDataContainer) {

			FileNodeDataContainer container = (FileNodeDataContainer) data;
			File fileNode                   = (File) newNode;
			String relativeFilePath         = newNode.getId() + "_" + System.currentTimeMillis();
			String path                     = Services.getFilesPath() + "/" + relativeFilePath;

			// rename temporary file to new location etc.
			if (container.persistTemporaryFile(path)) {

				fileNode.setSize(container.getFileSize());
				fileNode.setRelativeFilePath(relativeFilePath);
			}
		}

		return newNode;
	}
}
