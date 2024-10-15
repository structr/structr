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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.w3c.dom.Node;

public class Script extends DOMElement implements LinkSource {

	public static final Property<String> htmlSrcProperty     = new StringProperty("_html_src").partOfBuiltInSchema();
	public static final Property<String> htmlAsyncProperty   = new StringProperty("_html_async").partOfBuiltInSchema();
	public static final Property<String> htmlDeferProperty   = new StringProperty("_html_defer").partOfBuiltInSchema();
	public static final Property<String> htmlTypeProperty    = new StringProperty("_html_type").partOfBuiltInSchema();
	public static final Property<String> htmlCharsetProperty = new StringProperty("_html_charset").partOfBuiltInSchema();

	public static final View htmlView = new View(Script.class, PropertyView.Html,
		htmlSrcProperty, htmlAsyncProperty, htmlDeferProperty, htmlTypeProperty, htmlCharsetProperty
	);

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		final PropertyKey<String> key = StructrApp.key(Script.class, "_html_type");
		final String value            = getProperty(key);

		if (StringUtils.isBlank(value)) {
			setProperty(key, "text/javascript");
		}
	}

	@Override
	public void handleNewChild(final Node newChild) {

		if (newChild instanceof Content) {

			try {
				final String scriptType = getProperty(StructrApp.key(Script.class, "_html_type"));

				if (StringUtils.isNotBlank(scriptType) && StringUtils.isBlank(((Content)newChild).getContentType())) {

					((Content)newChild).setContentType(scriptType);

				}

			} catch (FrameworkException fex) {

				final Logger logger = LoggerFactory.getLogger(Script.class);
				logger.warn("Unable to set property on new child: {}", fex.getMessage());

			}
		}
	}

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}
}
