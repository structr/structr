/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
import org.structr.core.entity.app.slots.TypedDataSlot;
import org.structr.core.module.GetEntityClassCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeCreator extends ActiveNode
{
	private static final Logger logger = Logger.getLogger(AppNodeCreator.class.getName());

	private static final String CREATOR_ICON_SRC =		"/images/brick_add.png";
	private static final String TARGET_TYPE_KEY =		"targetType";

	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		final List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
		final AbstractNode parentNode = getCreateDestination();
		final String targetType = getTargetType();

		if(targetType != null && parentNode != null)
		{
			List<InteractiveNode> dataSource = getDataSources();
			attributes.add(new NodeAttribute("type", targetType));

			// add attributes from data sources
			for(InteractiveNode src : dataSource)
			{
				attributes.add(new NodeAttribute(src.getMappedName(), src.getValue()));
			}

			Services.command(TransactionCommand.class).execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					Command createNode = Services.command(CreateNodeCommand.class);
					AbstractNode newNode = (AbstractNode)createNode.execute(attributes);
					if(newNode != null)
					{
						Command storeNode = Services.command(CreateRelationshipCommand.class);
						storeNode.execute(parentNode, newNode, RelType.HAS_CHILD);

						return(true);
					}

					return(null);
				}

			});

		}

		return(false);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new HashMap<String, Slot>();
		List<InteractiveNode> dataSource = getDataSources();

		// add attributes from data sources
		for(InteractiveNode src : dataSource)
		{
			ret.put(src.getMappedName(), new TypedDataSlot(src.getParameterType()));
		}

		return(ret);
	}

	@Override
	public String getIconSrc()
	{
		return(CREATOR_ICON_SRC);
	}

	public String getTargetType()
	{
		return((String)getProperty(TARGET_TYPE_KEY));
	}

	public void setTargetType(String targetType)
	{
		setProperty(TARGET_TYPE_KEY, targetType);
	}

	// ----- private methods -----
	private Class getFullTargetType()
	{
		String targetType = getTargetType();
		Class ret = null;

		if(targetType != null)
		{
			ret = (Class)Services.command(GetEntityClassCommand.class).execute(targetType);
		}

		return(ret);
	}

	private AbstractNode getCreateDestination()
	{
		List<StructrRelationship> rels = getRelationships(RelType.CREATE_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return(ret);
	}
}
