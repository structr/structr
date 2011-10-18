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

package org.structr.core.entity;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;


/**
 *
 * @author Christian Morgner
 */
public abstract class ApplicationNode extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(ApplicationNode.class.getName());
	
	public abstract void buildTestCase();

	@Override
	public void onNodeInstantiation()
	{
		if(!getNode().hasRelationship(Direction.OUTGOING))
		{
			// build application here
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					buildTestCase();

					return(null);
				}
			});

			// change this into a Folder
			this.setProperty("type", "Folder");
		}
	}

	@Override
	public String getIconSrc()
	{
		return("/images/bricks.png");
	}

	// ----- protected methods -----
	protected AbstractNode createNode(AbstractNode parent, String nodeType, String name)
	{
		return(createNode(parent, nodeType, name, null));
	}

	protected AbstractNode createNode(AbstractNode parent, String nodeType, String name, Template template)
	{
		Command createNodeCommand = Services.command(securityContext, CreateNodeCommand.class);
		List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();
		attrs.add(new NodeAttribute(Key.type.name(), nodeType));
		attrs.add(new NodeAttribute(Key.name.name(), name));

		AbstractNode ret = (AbstractNode)createNodeCommand.execute(attrs);

		if(parent != null)
		{
			linkNodes(parent, ret, RelType.HAS_CHILD);
		}

		if(template != null)
		{
			linkNodes(ret, template, RelType.USE_TEMPLATE);
		}

		return(ret);
	}

	protected StructrRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, RelType relType)
	{
		return((StructrRelationship)Services.command(securityContext, CreateRelationshipCommand.class).execute(startNode, endNode, relType));
	}
}
