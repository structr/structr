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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

public class Textarea extends DOMElement {

	public static final Property<String> htmlNameProperty        = new StringProperty("_html_name").partOfBuiltInSchema();
	public static final Property<String> htmlDisabledProperty    = new StringProperty("_html_disabled").partOfBuiltInSchema();
	public static final Property<String> htmlFormProperty        = new StringProperty("_html_form").partOfBuiltInSchema();
	public static final Property<String> htmlReadonlyProperty    = new StringProperty("_html_readonly").partOfBuiltInSchema();
	public static final Property<String> htmlMaxlengthProperty   = new StringProperty("_html_maxlenght").partOfBuiltInSchema();
	public static final Property<String> htmlAutofocusProperty   = new StringProperty("_html_autofocus").partOfBuiltInSchema();
	public static final Property<String> htmlRequiredProperty    = new StringProperty("_html_required").partOfBuiltInSchema();
	public static final Property<String> htmlPlaceholderProperty = new StringProperty("_html_placeholder").partOfBuiltInSchema();
	public static final Property<String> htmlDirnameProperty     = new StringProperty("_html_dirname").partOfBuiltInSchema();
	public static final Property<String> htmlRowsProperty        = new StringProperty("_html_rows").partOfBuiltInSchema();
	public static final Property<String> htmlWrapProperty        = new StringProperty("_html_wrap").partOfBuiltInSchema();
	public static final Property<String> htmlColsProperty        = new StringProperty("_html_cols").partOfBuiltInSchema();

	public static final View htmlView = new View(Textarea.class, PropertyView.Html,
		htmlNameProperty, htmlDisabledProperty, htmlFormProperty, htmlReadonlyProperty, htmlMaxlengthProperty, htmlAutofocusProperty,
		htmlRequiredProperty, htmlPlaceholderProperty, htmlDirnameProperty, htmlRowsProperty, htmlWrapProperty, htmlColsProperty
	);

	@Override
	public boolean avoidWhitespace() {
		return true;
	}

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}
}
