/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app.tests;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;


/**
 *
 * @author chrisi
 */
public abstract class AppNodeTestCase extends AbstractNode
{
	public abstract void buildTestCase();

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
	}

	@Override
	public String getIconSrc()
	{
		if(!getNode().hasRelationship(Direction.OUTGOING))
		{
			// build test case here
			Services.command(TransactionCommand.class).execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					buildTestCase();

					return(null);
				}
			});
		}

		return("/images/plugin.png");
	}

	// ----- protected methods -----
	protected AbstractNode createNode(String nodeType, String name)
	{
		Command createNodeCommand = Services.command(CreateNodeCommand.class);
		List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();
		attrs.add(new NodeAttribute(TYPE_KEY, nodeType));
		attrs.add(new NodeAttribute(NAME_KEY, name));

		return((AbstractNode)createNodeCommand.execute(attrs));
	}

	protected void linkNodes(AbstractNode startNode, AbstractNode endNode, RelType relType)
	{
		Services.command(CreateRelationshipCommand.class).execute(startNode, endNode, relType);
	}
}
