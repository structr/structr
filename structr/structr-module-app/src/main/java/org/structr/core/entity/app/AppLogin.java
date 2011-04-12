/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.entity.app.slots.StringSlot;

/**
 *
 * @author Christian Morgner
 */
public class AppLogin extends ActiveNode
{
	private static final Logger logger = Logger.getLogger(AppLogin.class.getName());

	@Override
	public void execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// do login
		logger.log(Level.INFO, "login: {0}, password: {1}", new Object[] { getValue("login"), getValue("password") } );
	}

	@Override
	public String getIconSrc()
	{
		return("/images/door_in.png");
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new LinkedHashMap<String, Slot>();

		ret.put("login", new StringSlot());
		ret.put("password", new StringSlot());

		return(ret);
	}
}
