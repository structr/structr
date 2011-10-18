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



package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 * A Template is basically a HtmlSource node.
 *
 * The difference is that it uses the calling node's parameters
 * for replacement of its placeholders.
 *
 * @author amorgner
 *
 */
public class Template extends PlainText {

	static {

		EntityContext.registerPropertySet(Template.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private AbstractNode callingNode;

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/page_white_gear.png";
	}

	public AbstractNode getCallingNode() {
		return callingNode;
	}

	//~--- set methods ----------------------------------------------------

	public void setCallingNode(final AbstractNode callingNode) {
		this.callingNode = callingNode;
	}
}
