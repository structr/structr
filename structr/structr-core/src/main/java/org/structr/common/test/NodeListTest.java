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

package org.structr.common.test;

import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.RelType;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.PlainText;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class NodeListTest
{
	public static void main(String[] args)
	{
		StandaloneTestHelper.prepareStandaloneTest("/tmp/structr-test/");

		final GraphDatabaseService graphDb = (GraphDatabaseService)Services.command(GraphDatabaseCommand.class).execute();
		final Command factory = Services.command(NodeFactoryCommand.class);
		NodeList<AbstractNode> nodeList = null;

		for(Node node : graphDb.getAllNodes())
		{
			AbstractNode n = (AbstractNode)factory.execute(node);

			if(n instanceof NodeList)
			{
				nodeList = (NodeList)n;
				break;
			}
		}

		if(nodeList == null)
		{
			nodeList = (NodeList)Services.command(TransactionCommand.class).execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					Node node = graphDb.createNode();
					node.setProperty(AbstractNode.Key.type.name(), "NodeList");
					graphDb.getReferenceNode().createRelationshipTo(node, RelType.HAS_CHILD);

					return(factory.execute(node));
				}
			});
		}

		if(nodeList != null)
		{
			boolean exit = false;

			while(!exit)
			{
				try
				{
					System.out.println("#######################");
					System.out.println("list size: " + nodeList.size());
					for(AbstractNode node : nodeList)
					{
						System.out.println(node.getId() + ": " + node);

						for(Relationship rel : node.getNode().getRelationships(Direction.OUTGOING))
						{
							System.out.println("          " + rel.getId() + ": " + rel.getType() + " -> " + rel.getEndNode());
						}
						System.out.println();

					}

					System.out.println("---------------------");

					String line = JOptionPane.showInputDialog(null, "Kommando (add [pos] | addall [num] | del [pos] | clear):");

					if("exit".equals(line))
					{
						exit = true;
					} else
					if("clear".equals(line))
					{
						nodeList.clear();

					} else
					if(line.startsWith("addall"))
					{
						final List<AbstractNode> nodes = new LinkedList<AbstractNode>();

						int count = -1;
						try { count = Integer.parseInt(line.substring(line.indexOf(" ") + 1)); } catch(Throwable t) {}
						if(count == -1) count = 3;

						final int finalCount = count;

						Services.command(TransactionCommand.class).execute(new StructrTransaction()
						{
							@Override
							public Object execute() throws Throwable
							{
								for(int i=0; i<finalCount; i++)
								{
									AbstractNode node = new PlainText();
									node.init(graphDb.createNode());
									node.setProperty(AbstractNode.Key.type.name(), "PlainText");
									nodes.add(node);
								}

								return(null);
							}

						});

						System.out.println("adding " + count + " nodes");
						nodeList.addAll(nodes);

					} else
					if(line.startsWith("add"))
					{
						final AbstractNode node = new PlainText();

						Services.command(TransactionCommand.class).execute(new StructrTransaction()
						{
							@Override
							public Object execute() throws Throwable
							{
								node.init(graphDb.createNode());
								node.setProperty(AbstractNode.Key.type.name(), "PlainText");

								return(null);
							}
						});

						int index = -1;
						try
						{
							index = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

						} catch(Throwable t) {}

						if(index != -1)
						{
							System.out.println("adding node at " + index);
							nodeList.add(index, node);
						} else
						{
							System.out.println("appending node");
							nodeList.add(node);
						}

					} else
					if(line.startsWith("del"))
					{

						int index = -1;
						try
						{
							index = Integer.parseInt(line.substring(line.indexOf(" ") + 1));

						} catch(Throwable t) {}

						if(index != -1)
						{
							System.out.println("removing node #" + index);
							nodeList.remove(index);

						} else
						{
							System.out.println("removing last node");
							nodeList.remove(nodeList.size() - 1);
						}
					}

				} catch(Throwable t)
				{
					System.out.println(t.getMessage());
					t.printStackTrace();
				}
			}
		}

		StandaloneTestHelper.finishStandaloneTest();
	}
}
