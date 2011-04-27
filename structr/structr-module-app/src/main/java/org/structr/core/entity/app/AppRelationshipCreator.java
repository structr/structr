/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
import org.structr.core.node.CreateRelationshipCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppRelationshipCreator extends ActionNode
{
	private static final Logger logger =			Logger.getLogger(AppRelationshipCreator.class.getName());
	private static final String TARGET_REL_TYPE =		"targetRelType";

	@Override
	public boolean doAction(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		String relType = getStringProperty(TARGET_REL_TYPE);
		AbstractNode relStartNode = getNamedNodeSource("startNode");
		AbstractNode relEndNode = getNamedNodeSource("endNode");

		if(relType == null)
		{
			logger.log(Level.WARNING, "AppRelationshipCreator needs {0} property", TARGET_REL_TYPE);

			return(false);
		}

		if(relStartNode == null)
		{
			logger.log(Level.WARNING, "AppRelationshipCreator needs startNode");
			return(false);
		}

		if(relEndNode == null)
		{
			logger.log(Level.WARNING, "AppRelationshipCreator needs endNode");
			return(false);
		}

		if(relStartNode.getId() == relEndNode.getId())
		{
			logger.log(Level.WARNING, "AppRelationshipCreator can not operate on a single node (start == end!)");
			return(false);
		}

		logger.log(Level.INFO, "All checks passed, creating relationship {0}", relType);

		Services.command(CreateRelationshipCommand.class).execute(relStartNode, relEndNode, relType);

		return(true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new LinkedHashMap<String, Slot>();

		/*
		ret.put("startNode", new StringSlot());
		ret.put("endNode", new StringSlot());
		*/

		return(ret);
	}

	@Override
	public String getIconSrc()
	{
		return("/images/brick_link.png");
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}

	// ----- private methods -----
	private AbstractNode getNamedNodeSource(String name)
	{
		List<StructrRelationship> rels = getIncomingDataRelationships();
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			AbstractNode node = rel.getStartNode();
			if(rel.getRelationship().hasProperty(TARGET_SLOT_NAME_KEY))
			{
				String targetSlot = (String)rel.getRelationship().getProperty(TARGET_SLOT_NAME_KEY);
				if(name.equals(targetSlot))
				{
					ret = node;
					break;
				}
			}
		}

		return(ret);
	}
}
