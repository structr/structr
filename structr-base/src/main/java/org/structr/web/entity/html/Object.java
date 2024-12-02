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
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.DOMElement;

import java.net.URI;

public class Object extends DOMElement {

	public static final Property<String> htmlTypeProperty          = new StringProperty("_html_type").partOfBuiltInSchema();
	public static final Property<String> htmlTypeMustMatchProperty = new StringProperty("_html_typemustmatch").partOfBuiltInSchema();
	public static final Property<String> htmlUsemapProperty        = new StringProperty("_html_usemap").partOfBuiltInSchema();
	public static final Property<String> htmlFormProperty          = new StringProperty("_html_form").partOfBuiltInSchema();
	public static final Property<String> htmlWidthProperty         = new StringProperty("_html_width").partOfBuiltInSchema();
	public static final Property<String> htmlHeightProperty        = new StringProperty("_html_height").partOfBuiltInSchema();

	public static final View htmlView = new View(Object.class, PropertyView.Html,
		htmlTypeProperty, htmlTypeMustMatchProperty, htmlUsemapProperty, htmlFormProperty, htmlWidthProperty, htmlHeightProperty
	);
}
