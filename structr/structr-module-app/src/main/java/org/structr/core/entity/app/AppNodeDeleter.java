/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.Map;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.DeleteNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeDeleter extends ActiveNode
{
	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		AbstractNode toDelete = getNodeFromLoader();
		if(toDelete != null)
		{
			// FIXME: is this the right way to delete a node?
			Services.command(DeleteNodeCommand.class).execute(toDelete, null);
		}

		return(true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		return(null);
	}

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
	public String getIconSrc()
	{
		return("/images/brick_delete.png");
	}
}
