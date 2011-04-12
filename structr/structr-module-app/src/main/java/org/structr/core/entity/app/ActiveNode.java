/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.context.StructrContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author chrisi
 */
public abstract class ActiveNode extends AbstractNode
{
	private static final Logger logger = Logger.getLogger(ActiveNode.class.getName());

	public abstract void execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);

	@Override
	public abstract String getIconSrc();

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		String currentUrl = (String)StructrContext.getAttribute(StructrContext.CURRENT_NODE_PATH);
		String myNodeUrl = getNodePath(user);

		// only execute this active node's method when the path
		// matches exactly
		logger.log(Level.INFO, "Checking '{0}' and '{1}' for equality..", new Object[] { currentUrl, myNodeUrl } );

		if(myNodeUrl.equals(currentUrl))
		{
			execute(out, startNode, editUrl, editNodeId, user);
		}
	}
}
