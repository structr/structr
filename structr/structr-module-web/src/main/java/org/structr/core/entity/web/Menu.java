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



package org.structr.core.entity.web;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.renderer.MenuRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * A Menu contains menu items which will be rendered recursively.
 *
 * @author amorgner
 *
 */
public class Menu extends MenuItem {

	private final static int maxDepthDefault = 3;

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Menu.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ maxDepth; }

	;

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new MenuRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/tree.png";
	}

	public int getMaxDepth() {

		Object depth = getProperty(Key.maxDepth.name());

		// The first time, set default max depth
		if (depth == null) {

			setMaxDepth(maxDepthDefault);

			return maxDepthDefault;

		} else {
			return getIntProperty(Key.maxDepth.name());
		}
	}

	//~--- set methods ----------------------------------------------------

	public void setMaxDepth(final int maxDepth) {

		setProperty(Key.maxDepth.name(),
			    maxDepth);
	}
}
