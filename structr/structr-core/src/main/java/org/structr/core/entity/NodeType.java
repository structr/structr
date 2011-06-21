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
package org.structr.core.entity;

import java.util.Map;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.NodeSource;

/**
 * A NodeType node defines the type of all connected nodes.
 * 
 * The properties of a node define the possible properties of all nodes of this type.
 * 
 * 
 * @author axel
 */
public class NodeType extends AbstractNode implements NodeSource
{
	@Override
	public String getIconSrc()
	{
		return "/images/database_table.png";
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}

	@Override
	public void initializeRenderers(final Map<RenderMode, NodeRenderer> rendererMap)
	{
	}

	@Override
	public void onNodeDeletion()
	{
	}

	@Override
	public AbstractNode loadNode()
	{
		return(this);
	}
}
