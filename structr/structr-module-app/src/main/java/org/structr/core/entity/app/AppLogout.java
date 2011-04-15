/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import java.util.Map;
import javax.servlet.http.HttpSession;
import org.structr.common.SessionContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
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
	public boolean execute(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
	{
		HttpSession session = SessionContext.getSession();
		if(session != null)
		{
			session.invalidate();
		}

		return(true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		return(null);
	}
}
