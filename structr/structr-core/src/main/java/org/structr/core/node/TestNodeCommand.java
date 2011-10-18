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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author cmorgner
 */
public class TestNodeCommand extends NodeServiceCommand
{
	private static final Logger logger = Logger.getLogger(TestNodeCommand.class.getName());

	@Override
	public Object execute(final Object... parameters)
	{
		final GraphDatabaseService graphDb = (GraphDatabaseService)arguments.get("graphDb");
		final StructrNodeFactory nodeFactory = (StructrNodeFactory)arguments.get("nodeFactory");

		Command transactionCommand = Services.command(TransactionCommand.class);
		return(transactionCommand.execute(new StructrTransaction()
		{
			@Override
			public Object execute() throws Throwable
			{
				if(graphDb != null)
				{
					Node listNode = graphDb.getNodeById(6);
					if(listNode != null)
					{
						NodeList nodeList = (NodeList)nodeFactory.createNode(securityContext, listNode);
						if(nodeList != null)
						{
							if(parameters.length > 0)
							{
								String cmd = parameters[0].toString();

								logger.log(Level.INFO, "command: {0}", cmd);

								if("add".equals(cmd))
								{
									Node newNode = graphDb.createNode();
									newNode.setProperty("type", "PlainText");

									AbstractNode newStructrNode = nodeFactory.createNode(securityContext, newNode);
									nodeList.add(newStructrNode);

								} else if("del".equals(cmd))
								{
									nodeList.remove(nodeList.size()-1);
								}
							}

							return(nodeList);

						} else
						{
							logger.log(Level.WARNING, "nodeList was null!");
						}

					} else
					{
						logger.log(Level.WARNING, "listNode was null!");
					}
				} else
				{
					logger.log(Level.WARNING, "graphDb was null!");
				}
				
				return null;
			}
		}));
	}
}
