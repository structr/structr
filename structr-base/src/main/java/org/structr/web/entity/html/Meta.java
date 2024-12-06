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

public class Meta extends DOMElement {

	public static final Property<String> htmlNameProperty      = new StringProperty("_html_name").partOfBuiltInSchema();
	public static final Property<String> htmlHttpEquivProperty = new StringProperty("_html_http-equiv").partOfBuiltInSchema();
	public static final Property<String> htmlContentProperty   = new StringProperty("_html_content").partOfBuiltInSchema();
	public static final Property<String> htmlCharsetProperty   = new StringProperty("_html_charset").partOfBuiltInSchema();

	public static final View htmlView = new View(Meta.class, PropertyView.Html,
		htmlNameProperty, htmlHttpEquivProperty, htmlContentProperty, htmlCharsetProperty
	);

	@Override
	public boolean isVoidElement() {
		return true;
	}

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}
}
