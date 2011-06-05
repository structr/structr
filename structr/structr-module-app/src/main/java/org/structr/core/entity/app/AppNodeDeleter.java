/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity.app;

import java.util.Map;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.DeleteNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeDeleter extends ActionNode
{
	@Override
	public boolean doAction(final StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		AbstractNode toDelete = getNodeFromLoader();
		if(toDelete != null)
		{
			// FIXME: is this the right way to delete a node?
			Services.command(DeleteNodeCommand.class).execute(toDelete, null);
		}

		return (true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		return (null);
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/brick_delete.png");
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}
}
