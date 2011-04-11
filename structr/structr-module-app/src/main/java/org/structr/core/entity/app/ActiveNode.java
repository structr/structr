/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.web.Page;

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
		HttpServletRequest request = getRequest();

		if(request != null)
		{
			String myNodeUrl = getNodePath(user);

			logger.log(Level.INFO, "myNodeUrl:         {0}", myNodeUrl);
			logger.log(Level.INFO, "requestedNodePath: {0}", requestedNodePath);

			// FIXME:
			//  need to know the path that lead to the current node..
			//  to be found in nodeIdString in StructrPage, but no
			//  luck in getting it down here yet...

			execute(out, startNode, editUrl, editNodeId, user);
		}
	}
}
