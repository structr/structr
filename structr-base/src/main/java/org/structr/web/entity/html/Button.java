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

public interface Button extends DOMElement {

	static class Impl { static {

		final JsonSchema schema       = SchemaService.getDynamicSchema();
		final JsonObjectType type     = schema.addType("Button");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Button"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_autofocus",      PropertyView.Html);
		type.addStringProperty("_html_disabled",       PropertyView.Html);
		type.addStringProperty("_html_form",           PropertyView.Html);
		type.addStringProperty("_html_formaction",     PropertyView.Html);
		type.addStringProperty("_html_formenctype",    PropertyView.Html);
		type.addStringProperty("_html_formmethod",     PropertyView.Html);
		type.addStringProperty("_html_formnovalidate", PropertyView.Html);
		type.addStringProperty("_html_formtarget",     PropertyView.Html);
		type.addStringProperty("_html_type",           PropertyView.Html);
		type.addStringProperty("_html_value",          PropertyView.Html);
	}}
}
