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
import org.neo4j.graphdb.Node;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.cloud.NodeDataContainer;

/**
 *
 * @author Christian Morgner
 */
public class DefaultNode extends AbstractNode
{
    public DefaultNode()
    {
	    super();
    }

    public DefaultNode(final Map<String, Object> properties)
    {
	    super(properties);
    }

    public DefaultNode(final NodeDataContainer data)
    {
	super(data);
    }

    public DefaultNode(final Node dbNode)
    {
        super(dbNode);
    }

    @Override
    public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
    {
    }

    @Override
    public String getIconSrc() {
        return "/images/error.png";
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
