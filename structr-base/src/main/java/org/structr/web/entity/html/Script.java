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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.w3c.dom.Node;

import java.net.URI;

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

				if (StringUtils.isNotBlank(scriptType) && StringUtils.isBlank(((Content)newChild).getContentType())) {

					((Content)newChild).setContentType(scriptType);

				}

			} catch (FrameworkException fex) {

				final Logger logger = LoggerFactory.getLogger(Script.class);
				logger.warn("Unable to set property on new child: {}", fex.getMessage());

			}
		}
	}
}
