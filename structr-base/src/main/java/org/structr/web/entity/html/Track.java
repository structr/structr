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

import org.apache.commons.lang.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

public interface Track extends DOMElement {

	/*
	public static final Property<String> htmlKindProperty = new StringProperty("_html_kind");
	public static final Property<String> htmlSrcProperty = new StringProperty("_html_src");
	public static final Property<String> htmlSrcLangProperty = new StringProperty("_html_srclang");
	public static final Property<String> htmlLabelProperty = new StringProperty("_html_label");
	public static final Property<String> htmlDefaultProperty = new StringProperty("_html_default");

	public static final View htmlView = new View(Track.class, PropertyView.Html,
		htmlKindProperty, htmlSrcLangProperty, htmlSrcProperty, htmlLabelProperty, htmlDefaultProperty
	);

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

