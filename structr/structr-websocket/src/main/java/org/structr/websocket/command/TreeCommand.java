/*
 *  Copyright (C) 2010-2012 Axel Morgner
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



package org.structr.websocket.command;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.SecurityContext;
import org.structr.common.TreeNode;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeFactory;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.ResourceExpander;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class TreeCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(TreeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		AbstractNode rootNode                 = getNode(webSocketData.getId());
		TraversalDescription localDesc        =	Traversal.description().depthFirst().uniqueness(Uniqueness.NODE_GLOBAL).expand(new ResourceExpander(rootNode.getStringProperty(AbstractNode.Key.uuid.name())));
		final NodeFactory factory             = new NodeFactory(securityContext);
		final TreeNode root                   = new TreeNode(null);

		localDesc = localDesc.evaluator(new Evaluator() {

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				if (node.hasProperty(AbstractNode.Key.type.name())) {

					try {
						TreeNode newTreeNode = new TreeNode(factory.createNode(node));
						Relationship rel     = path.lastRelationship();

						if (rel != null) {

							Node parentNode         = rel.getStartNode();
							TreeNode parentTreeNode = root.getNode((String) parentNode.getProperty("uuid"));

							if (parentTreeNode == null) {

								root.addChild(newTreeNode);
								logger.log(Level.FINEST, "New tree node: {0} --> {1}", new Object[] { newTreeNode, root });
								logger.log(Level.FINE, "New tree node: {0} --> {1}", new Object[] { newTreeNode.getData().getName(), "root" });

							} else {

								parentTreeNode.addChild(newTreeNode);
								logger.log(Level.FINEST, "New tree node: {0} --> {1}", new Object[] { newTreeNode, parentTreeNode });
								logger.log(Level.FINE, "New tree node: {0} --> {1}", new Object[] { newTreeNode.getData().getName(), parentTreeNode.getData().getName() });

							}

						} else {

							root.addChild(newTreeNode);
							logger.log(Level.FINE, "Added {0} to root", newTreeNode);

						}

						return Evaluation.INCLUDE_AND_CONTINUE;

					} catch(FrameworkException fex) {
						logger.log(Level.WARNING, "Unable to instantiate node", fex);
					}
				}

				return Evaluation.EXCLUDE_AND_CONTINUE;
			}

		});

		// do traversal
		for (Node node : localDesc.traverse(rootNode.getNode()).nodes()) {

			//System.out.println(node.getProperty("type") + "[" + node.getProperty("uuid") + "]: " + node.getProperty("name"));

		}

		webSocketData.setResultTree(root);

		// send only over local connection
		getWebSocket().send(webSocketData, true);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "TREE";
	}
}
