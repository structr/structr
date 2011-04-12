/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.common.StructrContext;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public abstract class ActiveNode extends AbstractNode
{
	private static final String TARGET_SLOT_NAME_KEY =		"targetSlotName";
        private List<StructrRelationship> incomingDataRelationships = null;

	public abstract boolean execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);
	public abstract Map<String, Slot> getSlots();

	private Map<String, Object> values = new LinkedHashMap<String, Object>();

	@Override
	public abstract String getIconSrc();

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		String currentUrl = (String)StructrContext.getAttribute(StructrContext.CURRENT_NODE_PATH);
		String myNodeUrl = getNodePath(user);

		// remove slashes from end of string
		while(currentUrl.endsWith("/"))
		{
			currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
		}

		// execute method if path matches exactly
		if(myNodeUrl.equals(currentUrl))
		{
			// check incoming DATA relationships here
			// endpoint must implement InteractiveNode of the correct type
			List<InteractiveNode> dataSources = getDataSources();
			Map<String, Slot> slots = getSlots();
			boolean executionSuccessful = false;
			boolean slotsSuccessful = true;

			if(slots != null)
			{
				for(InteractiveNode source : dataSources)
				{
					String name = source.getMappedName();
					if(slots.containsKey(name))
					{
						Slot slot = slots.get(name);

						if(slot.getParameterType().equals(source.getParameterType()))
						{
							Object value = source.getValue();
							if(value != null)
							{
								values.put(name, value);
							}

							// check if this slot is mandatory and if the value was != null
							slotsSuccessful &= (slot.isMandatory() && value != null);
						}
					}
				}
			}

			if(slotsSuccessful)
			{
				executionSuccessful = execute(out, startNode, editUrl, editNodeId, user);
			}

			// the next block will be entered if slotsSuccessful was false, or if executionSuccessful was false!
			if(executionSuccessful)
			{
				// TODO: redirect to success page

			} else
			{
				// TODO: redirect to error page
			}
		}
	}

	public Object getValue(String name)
	{
		return(values.get(name));
	}

        /**
         * Cached list of incoming data relationships
         *
         * @return
         */
        public List<StructrRelationship> getIncomingDataRelationships() {

            if (incomingDataRelationships == null) {
                incomingDataRelationships = getRelationships(RelType.DATA, Direction.INCOMING);
            }
            return incomingDataRelationships;
        }


	// ----- private methods -----
	private List<InteractiveNode> getDataSources()
	{
		List<StructrRelationship> rels = getRelationships(RelType.DATA, Direction.INCOMING);
		List<InteractiveNode> ret = new LinkedList<InteractiveNode>();

		for(StructrRelationship rel : rels)
		{
			AbstractNode node = rel.getStartNode();
			if(node instanceof InteractiveNode)
			{
				InteractiveNode interactiveNode = (InteractiveNode)node;

				if(rel.getRelationship().hasProperty(TARGET_SLOT_NAME_KEY))
				{
					String targetSlot = (String)rel.getRelationship().getProperty(TARGET_SLOT_NAME_KEY);
					interactiveNode.setMappedName(targetSlot);
				}

				ret.add(interactiveNode);
			}
		}

		return(ret);
	}
}
