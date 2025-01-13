/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;

public interface Img extends DOMElement, LinkSource {

	/*
	public static final Property<String> htmlAltProperty         = new StringProperty("_html_alt");
	public static final Property<String> htmlSrcProperty         = new StringProperty("_html_src");
	public static final Property<String> htmlCrossoriginProperty = new StringProperty("_html_crossorigin");
	public static final Property<String> htmlUsemapProperty      = new StringProperty("_html_usemap");
	public static final Property<String> htmlIsmapProperty       = new StringProperty("_html_ismap");
	public static final Property<String> htmlWidthProperty       = new StringProperty("_html_width");
	public static final Property<String> htmlHeightProperty      = new StringProperty("_html_height");

	public static final View htmlView = new View(Img.class, PropertyView.Html,
		htmlAltProperty, htmlSrcProperty, htmlCrossoriginProperty, htmlUsemapProperty, htmlIsmapProperty, htmlWidthProperty, htmlHeightProperty
	);

	@Override
	public boolean avoidWhitespace() {
		return true;
	}

	@Override
	public boolean isVoidElement() {
		return true;
	}

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}

	 */
}
