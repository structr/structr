/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity.app;

<<<<<<< HEAD
import java.util.Map;
import javax.servlet.http.HttpSession;
=======
import org.structr.common.CurrentRequest;
import org.structr.common.PropertyView;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
import org.structr.common.StructrOutputStream;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppLogout extends ActionNode {

	static {

		EntityContext.registerPropertySet(AppLogout.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
<<<<<<< HEAD
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		HttpSession session = out.getSecurityContext().getSession();
		if(session != null)
		{
=======
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				final Long editNodeId) {

		HttpSession session = CurrentRequest.getSession();

		if (session != null) {
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
			session.invalidate();
		}

		return (true);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/door_out.png";
	}

	@Override
	public Map<String, Slot> getSlots() {
		return (null);
	}
}
