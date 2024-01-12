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

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.DOMElement;

import java.net.URI;

public interface Embed extends DOMElement {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Embed");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Embed"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_src",    PropertyView.Html);
		type.addStringProperty("_html_type",   PropertyView.Html);
		type.addStringProperty("_html_width",  PropertyView.Html);
		type.addStringProperty("_html_height", PropertyView.Html);

		type.overrideMethod("isVoidElement", false, "return true;");
	}}

	/*
	public static final Property<String> _src		= new HtmlProperty("src");
	public static final Property<String> _type		= new HtmlProperty("type");
	public static final Property<String> _width		= new HtmlProperty("width");
	public static final Property<String> _height		= new HtmlProperty("height");

	public static final org.structr.common.View htmlView	= new org.structr.common.View(Embed.class, PropertyView.Html,
		_src, _type, _width, _height
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}
	*/
}
