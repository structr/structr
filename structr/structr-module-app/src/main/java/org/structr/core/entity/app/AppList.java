/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
