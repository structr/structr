/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.core.NodeSource;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.SessionValue;
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
public class AppNodeCreator extends ActionNode implements NodeSource
{
	private static final Logger logger = Logger.getLogger(AppNodeCreator.class.getName());

	private static final String CREATOR_ICON_SRC =		"/images/brick_add.png";
	private static final String TARGET_TYPE_KEY =		"targetType";

	private SessionValue<AbstractNode> currentNode = null;

	@Override
	public boolean doAction(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
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

		if(targetType != null) // && parentNode != null)
		{
			List<InteractiveNode> dataSource = getInteractiveSourceNodes();
			attributes.add(new NodeAttribute("type", targetType));
			AbstractNode storeNode = getNodeFromLoader();
			boolean error = false;

			// add attributes from data sources
			for(InteractiveNode src : dataSource)
			{
				Object value = src.getValue();
				if(value != null && value.toString().length() > 0)
				{
					attributes.add(new NodeAttribute(src.getMappedName(), value));

				} else
				{
					setErrorValue(src.getName(), "Please enter a value for ".concat(src.getName()));
					error = true;
				}
			}

			if(error)
			{
				return(false);
			}

			// if no node provided by data source,
			if(storeNode == null)
			{
				// create node
				storeNode = createNewNode(parentNode, targetType);

			}

			// node exists / successfully created
			if(storeNode != null)
			{
				ret = storeNodeAttributes(storeNode, attributes);

			} else
			{
				ret = false;
			}

			logger.log(Level.INFO, "Saving newly created node {0}", storeNode);

			currentNode.set(storeNode);
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

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
		currentNode = new SessionValue<AbstractNode>(createUniqueIdentifier("currentNode"));
	}

	// ----- interface NodeSource -----
	@Override
	public AbstractNode loadNode()
	{
		logger.log(Level.INFO, "Returning newly created node {0}", currentNode.get());
		
		return(currentNode.get());
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
				
				// FIXME: temporary
				attributes.add(new NodeAttribute(PUBLIC_KEY, "true"));

				AbstractNode newNode = (AbstractNode)Services.command(CreateNodeCommand.class).execute(attributes);
				if(newNode != null)
				{
					if(parentNode != null)
					{
						Command createRelationship = Services.command(CreateRelationshipCommand.class);
						createRelationship.execute(parentNode, newNode, RelType.HAS_CHILD);
					}

					return(newNode);
				}

				return(null);
			}
		});

		return(ret);
	}
}
