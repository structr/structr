/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.common.StructrContext;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author chrisi
 */
public abstract class ActiveNode extends AbstractNode
{
	public abstract void execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user);

	@Override
	public abstract String getIconSrc();

	@Override
	public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		String currentUrl = (String)StructrContext.getAttribute(StructrContext.CURRENT_NODE_PATH);
		String myNodeUrl = getNodePath(user);

		// remove slashes from end of string
		while(currentUrl.endsWith("/"))
		{
			currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
		}

		// execute method if path matches exactly
		if(myNodeUrl.equals(currentUrl))
		{
			execute(out, startNode, editUrl, editNodeId, user);
		}
	}
}
