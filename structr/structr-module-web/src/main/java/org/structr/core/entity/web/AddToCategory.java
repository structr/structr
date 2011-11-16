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
import org.structr.renderer.AddToCategoryRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Include a node of this type to add objects to categories of the
 * currently logged in user.
 *
 * Nice for tagging objects, or build a store.
 *
 * @author axel
 */
public class AddToCategory extends WebNode {

	static {

		EntityContext.registerPropertySet(AddToCategory.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ categoryParameterName; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new AddToCategoryRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/tag_blue_add.png";
	}

	/**
	 * Return name of category parameter
	 *
	 * @return
	 */
	public String getCategoryParameterName() {
		return getStringProperty(Key.categoryParameterName.name());
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Set name of category parameter
	 *
	 * @param value
	 */
	public void setCategoryParameterName(final String value) {

		setProperty(Key.categoryParameterName.name(),
			    value);
	}
}
