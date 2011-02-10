/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.structr.core.entity.StructrNode;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author chrisi
 */
public class NodeListTest
{

	public static void main(String[] args)
	{
		StandaloneTestHelper.prepareStandaloneTest("/tmp/structr-test/");

		final GraphDatabaseService graphDb = (GraphDatabaseService)Services.createCommand(GraphDatabaseCommand.class).execute();
		final Command factory = Services.createCommand(NodeFactoryCommand.class);
		NodeList<StructrNode> nodeList = null;

		for(Node node : graphDb.getAllNodes())
		{
			StructrNode n = (StructrNode)factory.execute(node);

			if(n instanceof NodeList)
			{
				nodeList = (NodeList)n;
				break;
			}
		}

		if(nodeList == null)
		{
			nodeList = (NodeList)Services.createCommand(TransactionCommand.class).execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					Node node = graphDb.createNode();
					node.setProperty(StructrNode.TYPE_KEY, "NodeList");
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
					for(StructrNode node : nodeList)
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
						final List<StructrNode> nodes = new LinkedList<StructrNode>();

						int count = -1;
						try { count = Integer.parseInt(line.substring(line.indexOf(" ") + 1)); } catch(Throwable t) {}
						if(count == -1) count = 3;

						final int finalCount = count;

						Services.createCommand(TransactionCommand.class).execute(new StructrTransaction()
						{
							@Override
							public Object execute() throws Throwable
							{
								for(int i=0; i<finalCount; i++)
								{
									StructrNode node = new PlainText();
									node.init(graphDb.createNode());
									node.setProperty(StructrNode.TYPE_KEY, "PlainText");
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
						final StructrNode node = new PlainText();

						Services.createCommand(TransactionCommand.class).execute(new StructrTransaction()
						{
							@Override
							public Object execute() throws Throwable
							{
								node.init(graphDb.createNode());
								node.setProperty(StructrNode.TYPE_KEY, "PlainText");

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
