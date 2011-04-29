/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
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
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// just a container
	}

	@Override
	public void onNodeCreation()
	{
		// we cannot use this method to trigger the application unfolding, because
		// the create node dialog starts with an EmptyNode and determines its type
		// later.
	}

	@Override
	public void onNodeInstantiation()
	{
		if(!getNode().hasRelationship(Direction.OUTGOING))
		{
			// build application here
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
	}

	@Override
	public String getIconSrc()
	{
		return("/images/bricks.png");
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

	protected StructrRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, RelType relType)
	{
		return((StructrRelationship)Services.command(CreateRelationshipCommand.class).execute(startNode, endNode, relType));
	}
}
