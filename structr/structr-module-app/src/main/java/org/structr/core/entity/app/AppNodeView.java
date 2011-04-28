/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 * AppNodeView loads the node with the ID found in the request parameter specified
 * by the ID_SOURCE_KEY property of this node.
 *
 * @author Christian Morgner
 */
public class AppNodeView extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(AppNodeView.class.getName());

	private static final String ID_SOURCCE_KEY = "idSource";

	@Override
	public String getIconSrc()
	{
		return("/images/brick_go.png");
	}

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		AbstractNode sourceNode = loadNode();
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
	// ----- private methods -----
	private AbstractNode loadNode()
	{
		String idSourceParameter = (String)getProperty(ID_SOURCCE_KEY);
		String idSource = CurrentRequest.getRequest().getParameter(idSourceParameter);

		return((AbstractNode)Services.command(FindNodeCommand.class).execute(null, this, idSource));
	}
}
