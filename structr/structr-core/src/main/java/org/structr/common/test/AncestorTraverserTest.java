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

package org.structr.common.test;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class AncestorTraverserTest {

	private static final Logger logger = Logger.getLogger(AncestorTraverserTest.class.getName());

	public static void main(String[] args) {

		StandaloneTestHelper.prepareStandaloneTest("/tmp/structr-test/");

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		// fetch neo4j service
		GraphDatabaseService db = (GraphDatabaseService)Services.command(securityContext, GraphDatabaseCommand.class).execute();

		// define number of nodes & depth
		final int num = 1000;
		final int depth = 3;

		// test for existing relationships and create test structure
		if(!db.getReferenceNode().hasRelationship(Direction.OUTGOING)) {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					// create chain of nodes
					Command linkNodes = Services.command(securityContext, CreateRelationshipCommand.class);
					Command createNode = Services.command(securityContext, CreateNodeCommand.class);

					// get root node
					AbstractNode rootNode = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(null, 0L);

					// create chain of n nodes
					for(int i=0; i<num; i++) {

						String name = "node" + i;
						AbstractNode newNode = (AbstractNode)createNode.execute(new NodeAttribute("type", "Folder"), new NodeAttribute("name", name));

						// link nodes
						linkNodes.execute(rootNode, newNode, RelType.HAS_CHILD);
						AbstractNode currentNode = newNode;

						for(int j=0; j<depth; j++) {

							AbstractNode newNode2 = (AbstractNode)createNode.execute(new NodeAttribute("type", "Folder"), new NodeAttribute("name", name));
							linkNodes.execute(currentNode, newNode, RelType.HAS_CHILD);

							// descend one level
							currentNode = newNode2;
						}
					}


					return(null);
				}
			});
		}

		// create list of structr nodes
		Command factory = Services.command(securityContext, NodeFactoryCommand.class);
		List<AbstractNode> allNodes = new LinkedList<AbstractNode>();
		for(Node node : db.getAllNodes()) {

			if(!node.equals(db.getReferenceNode())) {
				
				allNodes.add((AbstractNode)factory.execute(node));
			}
		}

		logger.log(Level.INFO, "{0} nodes in list", allNodes.size());

		{
			int out = 0;
			int in = 0;
			long start = System.currentTimeMillis();
			for(AbstractNode node : allNodes) {

				if(node.isInTrash()) {

					in++;

				} else {

					out++;
				}
			}

			long end = System.currentTimeMillis();
			logger.log(Level.INFO, "NON-OPTIMIZED: {0} in trash, {1} not in trash, {2} ms", new Object[] { in, out, (end-start) } );
		}

		StandaloneTestHelper.finishStandaloneTest();

	}
}
