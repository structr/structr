/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeView extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(AppNodeView.class.getName());

	@Override
	public String getIconSrc()
	{
		return("/images/brick_go.png");
	}

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		AbstractNode sourceNode = getNodeFromLoader();
		if(sourceNode != null)
		{
			if(hasTemplate(user))
			{
				String html = template.getContent();
				if(StringUtils.isNotBlank(html))
				{
					StringWriter content = new StringWriter(100);

					// process content with Freemarker
					AbstractNode.staticReplaceByFreeMarker(html, content, sourceNode, editUrl, editNodeId, user);
					out.append(content.toString());

				} else
				{
					logger.log(Level.WARNING, "No template!");
				}

			} else
			{
				logger.log(Level.WARNING, "No template!");
			}

		} else
		{
			logger.log(Level.WARNING, "sourceNode was null");
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
