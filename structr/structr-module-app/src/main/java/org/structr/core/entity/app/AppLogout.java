/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import javax.servlet.http.HttpSession;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author chrisi
 */
public class AppLogout extends ActiveNode
{
	private static final String ICON_SRC = "/images/door_out.png";

	@Override
	public String getIconSrc()
	{
		return (ICON_SRC);
	}

	@Override
	public void execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		HttpSession session = this.getSession();
		
		if(session == null)
		{
			session = this.getRequest().getSession();
		}
		
		if(session != null)
		{
			session.invalidate();
		}
	}
}
