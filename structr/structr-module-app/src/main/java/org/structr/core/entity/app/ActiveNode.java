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
import org.structr.common.CurrentRequest;

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
	private Map<String, Slot> inputSlots = null;

	public abstract boolean execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);
	public abstract Map<String, Slot> getSlots();

	@Override
	public abstract String getIconSrc();

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		String currentUrl = CurrentRequest.getCurrentNodePath();
		String myNodeUrl = getNodePath(user);

		if(currentUrl != null)
		{
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
				Map<String, Slot> slots = getInputSlots();
				boolean executionSuccessful = false;

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
								slot.setSource(source);
								Object value = source.getValue();

								logger.log(Level.FINE,
									"sourceName: {0}, mappedName: {1}, value: {2}",
									new Object[]
									{
										source.getName(),
										source.getMappedName(),
										value
									}
								);

							} else
							{
								logger.log(Level.WARNING, "Parameter type mismatch: expected {0}, found {1}",
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

				executionSuccessful = execute(out, startNode, editUrl, editNodeId, user);

				logger.log(Level.INFO, "executionSuccessful: {0}", executionSuccessful );

				// the next block will be entered if slotsSuccessful was false, or if executionSuccessful was false!
				if(executionSuccessful)
				{
					// redirect to success page
					// saved session values can be reset!
					AbstractNode successTarget = getSuccessTarget();
					if(successTarget != null)
					{
						CurrentRequest.redirect(user, successTarget);
					}

				} else
				{
					// redirect to error page
					// saved session values must be kept
					AbstractNode failureTarget = getFailureTarget();
					if(failureTarget != null)
					{
						CurrentRequest.redirect(user, failureTarget);
					}
				}
			}
		}
	}

	public Object getValue(String name)
	{
		Slot slot = getInputSlots().get(name);
		if(slot != null)
		{
			InteractiveNode source = slot.getSource();
			if(source != null)
			{
				return(source.getValue());

			} else
			{
				logger.log(Level.WARNING, "source for {0} was null", name);
			}
		} else
		{
			logger.log(Level.WARNING, "slot for {0} was null", name);
		}

		logger.log(Level.WARNING, "No source found for slot {0}, returning null", name);

		// value not found
		return(null);
	}

        /**
         * Cached list of incoming data relationships
         *
         * @return
         */
        protected List<StructrRelationship> getIncomingDataRelationships() {

            if (incomingDataRelationships == null) {
                incomingDataRelationships = getRelationships(RelType.DATA, Direction.INCOMING);
            }
            return incomingDataRelationships;
        }


	// ----- private methods -----
	protected List<InteractiveNode> getDataSources()
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

	private Map<String, Slot> getInputSlots()
	{
		if(inputSlots == null)
		{
			inputSlots = getSlots();

			if(inputSlots == null)
			{
				// return empty map on failure
				inputSlots = new HashMap<String, Slot>();
			}
		}

		return(inputSlots);
	}

	protected void setErrorValue(String slotName, Object errorValue)
	{
		Slot slot = getInputSlots().get(slotName);
		if(slot != null)
		{
			InteractiveNode source = slot.getSource();
			if(source != null)
			{
				source.setErrorValue(errorValue);
			}
		}
	}
}
