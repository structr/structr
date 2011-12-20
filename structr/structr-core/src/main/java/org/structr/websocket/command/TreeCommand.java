/*
 *  Copyright (C) 2011 Axel Morgner
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
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.ResourceExpander;
import org.structr.common.SecurityContext;
import org.structr.common.TreeNode;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.StructrNodeFactory;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

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

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		AbstractNode rootNode                 = getNode(webSocketData.getId());
		TraversalDescription localDesc        =
			Traversal.description().depthFirst().uniqueness(Uniqueness.NODE_GLOBAL).expand(new ResourceExpander(rootNode.getStringProperty(AbstractNode.Key.uuid.name())));
		final StructrNodeFactory factory = new StructrNodeFactory(securityContext);
		final TreeNode root              = new TreeNode(null, null);

		localDesc = localDesc.evaluator(new Evaluator() {

			TreeNode currentNode = root;
			@Override
			public Evaluation evaluate(Path path) {

				Node node            = path.endNode();
				int nodeDepth        = path.length();
				int currentTreeDepth = currentNode.depth();

				System.out.println();
				System.out.println("Node depth: " + nodeDepth);
				System.out.println("Current tree depth: " + currentTreeDepth);
				System.out.println(node.getProperty("name") + ": " + node.getProperty("type") + "[" + node.getProperty("uuid") + "]");

				try {

					if (node.hasProperty(AbstractNode.Key.type.name())) {

						String type          = (String) node.getProperty(AbstractNode.Key.type.name());
						TreeNode newTreeNode = new TreeNode(currentNode, factory.createNode(securityContext, node, type));

						if (nodeDepth > currentTreeDepth) {

							currentNode.addChild(newTreeNode);

							currentNode = newTreeNode;

						} else if (nodeDepth < currentTreeDepth) {

							currentNode.getParent().getParent().addChild(newTreeNode);

							currentNode = newTreeNode;

						} else {

							currentNode.getParent().addChild(newTreeNode);

						}

						newTreeNode.depth(nodeDepth);

						return Evaluation.INCLUDE_AND_CONTINUE;

					}

				} catch (Throwable t) {

					// fail fast, no check
					logger.log(Level.SEVERE, "While evaluating path " + path, t);
				}

				return Evaluation.EXCLUDE_AND_CONTINUE;
			}

		});

		// do traversal
		for (Node node : localDesc.traverse(rootNode.getNode()).nodes()) {

			System.out.println(node.getProperty("type") + "[" + node.getProperty("uuid") + "]: " + node.getProperty("name"));

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
