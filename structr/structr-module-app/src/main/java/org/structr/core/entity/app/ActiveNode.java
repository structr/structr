/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private static final Logger logger = Logger.getLogger(ActiveNode.class.getName());
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
							boolean accepted = slot.accepts(value);

							if(accepted)
							{
								values.put(name, value);

							}

							// check if this slot is mandatory and if the value was != null
							boolean errorCondition = (slot.isMandatory() && accepted);
							source.setErrorCondition(errorCondition);

							logger.log(Level.INFO,
								"sourceName: {0}, mappedName: {1}, value: {2}, mandatory: {3}, errorCondition: {4}",
								new Object[]
								{
									source.getName(),
									source.getMappedName(),
									value,
									slot.isMandatory(),
									errorCondition
								}
							);

							slotsSuccessful &= errorCondition;
						} else
						{
							logger.log(Level.INFO, "Parameter type mismatch: expected {0}, found {1}",
								new Object[]
								{
									slot.getParameterType(),
									source.getParameterType()
								}
							);
						}

					} else
					{
						logger.log(Level.INFO, "Slot not found {0}", name );
					}
				}
			}

			if(slotsSuccessful)
			{
				executionSuccessful = execute(out, startNode, editUrl, editNodeId, user);
			}

			logger.log(Level.FINE, "slotsSuccessful: {0}, executionSuccessful: {1}", new Object[] { slotsSuccessful, executionSuccessful } );

			// the next block will be entered if slotsSuccessful was false, or if executionSuccessful was false!
			if(executionSuccessful)
			{
				// redirect to success page
				AbstractNode successTarget = getSuccessTarget();
				if(successTarget != null)
				{
					StructrContext.redirect(user, successTarget);
				}

			} else
			{
				// redirect to error page
				AbstractNode failureTarget = getFailureTarget();
				if(failureTarget != null)
				{
					StructrContext.redirect(user, failureTarget);
				}
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
		List<StructrRelationship> rels = getIncomingDataRelationships();
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

	private AbstractNode getSuccessTarget()
	{
		List<StructrRelationship> rels = getRelationships(RelType.SUCCESS_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return(ret);
	}

	private AbstractNode getFailureTarget()
	{
		List<StructrRelationship> rels = getRelationships(RelType.ERROR_DESTINATION, Direction.OUTGOING);
		AbstractNode ret = null;

		for(StructrRelationship rel : rels)
		{
			// first one wins
			ret = rel.getEndNode();
			break;
		}

		return(ret);
	}

	// ----- protected methods -----
	protected String createUniqueIdentifier(String prefix)
	{
		StringBuilder ret = new StringBuilder(100);

		ret.append(prefix);
		ret.append(getIdString());

		return(ret.toString());
	}
}
