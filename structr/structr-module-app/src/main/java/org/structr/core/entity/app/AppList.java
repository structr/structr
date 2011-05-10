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

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppList extends AppNodeView
{
	private static final Logger logger = Logger.getLogger(AppList.class.getName());

	@Override
	public String getIconSrc()
	{
		return ("/images/application_side_list.png");
	}

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		if(isVisible(user))
		{
			if(hasTemplate(user))
			{
				String html = template.getContent();
				if(StringUtils.isNotBlank(html))
				{
					// iterate over children following the DATA relationship
					for(AbstractNode container : getSortedDirectChildren(RelType.DATA, user))
					{
						// iterate over direct children of the given node
						for(AbstractNode node : container.getSortedDirectChildNodes(user))
						{
							doRendering(out, this, node, editUrl, editNodeId, user);
						}
					}

				} else
				{
					logger.log(Level.WARNING, "No template!");
				}
			}

		} else
		{
			logger.log(Level.WARNING, "Node not visible");
		}
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
