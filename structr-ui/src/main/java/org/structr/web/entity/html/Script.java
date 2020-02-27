/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.w3c.dom.Node;

public interface Script extends LinkSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Script");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Script"));
		type.setExtends(URI.create("#/definitions/LinkSource"));
		type.setCategory("html");

		type.addStringProperty("_html_src",     PropertyView.Html);
		type.addStringProperty("_html_async",   PropertyView.Html);
		type.addStringProperty("_html_defer",   PropertyView.Html);
		type.addStringProperty("_html_type",    PropertyView.Html);
		type.addStringProperty("_html_charset", PropertyView.Html);

		type.overrideMethod("onCreation",        true,  Script.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("handleNewChild",    false, Script.class.getName() + ".handleNewChild(this, arg0);");
		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
	}}


	/*
	public static final Property<String> _src     = new HtmlProperty("src");
	public static final Property<String> _async   = new HtmlProperty("async");
	public static final Property<String> _defer   = new HtmlProperty("defer");
	public static final Property<String> _type    = new HtmlProperty("type");
	public static final Property<String> _charset = new HtmlProperty("charset");

	public static final View uiView = new View(Script.class, PropertyView.Ui,
		linkableId, linkable
	);

	public static final View htmlView = new View(Script.class, PropertyView.Html,
		_src, _async, _defer, _type, _charset
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.isValid(errorBuffer)) {

			setProperty(Script._type, "text/javascript");
			return true;
		}

		return false;
	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
	*/

	static void onCreation(final Script thisScript, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final PropertyKey<String> key = StructrApp.key(Script.class, "_html_type");
		final String value            = thisScript.getProperty(key);

		if (StringUtils.isBlank(value)) {
			thisScript.setProperty(key, "text/javascript");
		}
	}

	static void handleNewChild(final Script thisScript, final Node newChild) {

		if (newChild instanceof Content) {

			try {
				final String scriptType = thisScript.getProperty(StructrApp.key(Script.class, "_html_type"));

				if (StringUtils.isNotBlank(scriptType)) {

					((Content)newChild).setContentType(scriptType);

				}

			} catch (FrameworkException fex) {

				logger.warn("Unable to set property on new child: {}", fex.getMessage());

			}
		}
	}
}
