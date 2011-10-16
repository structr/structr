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

import java.util.Map;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.AddToCategoryRenderer;

/**
 * Include a node of this type to add objects to categories of the
 * currently logged in user.
 *
 * Nice for tagging objects, or build a store.
 *
 * @author axel
 */
public class AddToCategory extends WebNode {

	private final static String ICON_SRC = "/images/tag_blue_add.png";
	public final static String CATEGORY_PARAMETER_NAME_KEY = "categoryParameterName";

	@Override
	public String getIconSrc() {
		return ICON_SRC;
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new AddToCategoryRenderer());
	}

	/**
	 * Return name of category parameter
	 *
	 * @return
	 */
	public String getCategoryParameterName() {
		return getStringProperty(CATEGORY_PARAMETER_NAME_KEY);
	}

	/**
	 * Set name of category parameter
	 *
	 * @param value
	 */
	public void setCategoryParameterName(final String value) {
		setProperty(CATEGORY_PARAMETER_NAME_KEY, value);
	}
}
