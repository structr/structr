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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public abstract class ActionNode extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(ActionNode.class.getName());

	protected static final String TARGET_SLOT_NAME_KEY = "targetSlotName";

	private Map<String, Slot> inputSlots = null;

	/**
	 * Returns the slots supported by this active node, mapped to their input slot names.
	 *
	 * @return a map containing string to slot mappings for this active node
	 */
	public abstract Map<String, Slot> getSlots();

	public abstract boolean doAction(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);

	public void initialize()
	{
		List<InteractiveNode> dataSources = getInteractiveSourceNodes();
		Map<String, Slot> slots = getInputSlots();

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
							});

					} else
					{
						logger.log(Level.WARNING, "Parameter type mismatch: expected {0}, found {1}",
							new Object[]
							{
								slot.getParameterType(),
								source.getParameterType()
							});
					}

				} else
				{
					logger.log(Level.INFO, "Slot not found {0}", name);
				}
			}
		}
	}

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		// nothing to do here, wont be called
	}

	public Object getValue(String name)
	{
		Slot slot = getInputSlots().get(name);
		if(slot != null)
		{
			InteractiveNode source = slot.getSource();
			if(source != null)
			{
				return (source.getValue());

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
		return (null);
	}

	// ----- protected methods -----
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

	protected List<InteractiveNode> getInteractiveSourceNodes()
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

		return (ret);
	}

	// ----- private methods -----
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

		return (inputSlots);
	}

}
