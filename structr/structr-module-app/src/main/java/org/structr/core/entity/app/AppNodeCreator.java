/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.HashMap;
import java.util.Map;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeCreator extends ActiveNode
{
	private static final String CREATOR_ICON_SRC =		"/images/brick_add.png";

	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		//TODO: implement me :)
		
		return(true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		Map<String, Slot> ret = new HashMap<String, Slot>();

		return(ret);
	}

	@Override
	public String getIconSrc()
	{
		return(CREATOR_ICON_SRC);
	}
}
