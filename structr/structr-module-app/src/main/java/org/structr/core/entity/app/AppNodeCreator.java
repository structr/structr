/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
import org.structr.core.entity.app.slots.TypedDataSlot;
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
	public boolean isPathSensitive()
	{
		return(true);
	}

	@Override
	public boolean doRedirectAfterExecution()
	{
		return(true);
	}

	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		final List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
		final AbstractNode parentNode = getCreateDestination();
		final String targetType = getTargetType();
		boolean ret = false;

		// display warning messages for common mistakes during test phase
		if(targetType == null)
		{
			logger.log(Level.WARNING, "AppNodeCreator needs {0} property!", TARGET_TYPE_KEY);
			ret = false;
		}

		if(parentNode == null)
		{
			logger.log(Level.WARNING, "AppNodeCreator needs CREATE_DESTINATION relationship!");
			ret = false;
		}

		if(targetType != null && parentNode != null)
		{
			List<InteractiveNode> dataSource = getInteractiveSourceNodes();
			attributes.add(new NodeAttribute("type", targetType));
			AbstractNode storeNode = getNodeFromLoader();

			// add attributes from data sources
			for(InteractiveNode src : dataSource)
			{
				attributes.add(new NodeAttribute(src.getMappedName(), src.getValue()));
			}

			// if no node provided by data source,
			if(storeNode == null)
			{
				// create node
				logger.log(Level.INFO, "storeNode was null, creating..");
				storeNode = createNewNode(parentNode, targetType);

			}

			// node exists / successfully created
			if(storeNode != null)
			{
				logger.log(Level.INFO, "storing attributes in node..");
				ret = storeNodeAttributes(storeNode, attributes);

			} else
			{
				logger.log(Level.WARNING, "Unable to create new node");
				ret = false;
			}
		}

		return(ret);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new HashMap<String, Slot>();
		List<InteractiveNode> dataSource = getInteractiveSourceNodes();

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

	private boolean storeNodeAttributes(final AbstractNode node, final List<NodeAttribute> attributes)
	{
		for(NodeAttribute attr : attributes)
		{
			node.setProperty(attr.getKey(), attr.getValue());
		}

		return(true);
	}

	private AbstractNode createNewNode(final AbstractNode parentNode, final String type)
	{
		AbstractNode ret = (AbstractNode)Services.command(TransactionCommand.class).execute(new StructrTransaction()
		{
			@Override
			public Object execute() throws Throwable
			{
				List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
				attributes.add(new NodeAttribute(TYPE_KEY, type));

				AbstractNode newNode = (AbstractNode)Services.command(CreateNodeCommand.class).execute(attributes);
				if(newNode != null)
				{
					Command createRelationship = Services.command(CreateRelationshipCommand.class);
					createRelationship.execute(parentNode, newNode, RelType.HAS_CHILD);

					return(newNode);
				}

				return(null);
			}
		});

		return(ret);
	}

	private AbstractNode getNodeFromLoader()
	{
		List<StructrRelationship> rels = getIncomingDataRelationships();
		AbstractNode ret = null;

		logger.log(Level.INFO, "{0} incoming DATA relationships", rels.size());

		for(StructrRelationship rel : rels)
		{
			// first one wins
			AbstractNode startNode = rel.getStartNode();
			if(startNode instanceof NodeSource)
			{
				logger.log(Level.INFO, "found NodeSource instance");

				NodeSource source = (NodeSource)startNode;
				ret = source.loadNode();
				break;
			}
		}

		return(ret);
	}
}
