/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.traits.definitions.html;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.operations.RenderContent;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

public class TemplateElement extends GenericHtmlElementTraitDefinition {

	public TemplateElement() {
		super(StructrTraits.TEMPLATE_ELEMENT);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(
			RenderContent.class,
			new RenderContent() {

				@Override
				public void renderContent(final DOMNode thisElement, final RenderContext renderContext, final int depth) throws FrameworkException {

					if (renderContext.isPartialRendering()) {

						// store request data in render context
						handleRequestData(renderContext);

						// Skip the enclosing template element and render the first child instead
						final DOMNode node = thisElement.getFirstChild();
						if (node != null && node.is(StructrTraits.DOM_ELEMENT)) {

							final DOMElement element = node.as(DOMElement.class);

							// mark template root so it can render its UUID into the HTML
							renderContext.setTemplateRootId(element.getUuid());
							renderContext.setTemplateId(thisElement.getUuid());

							element.renderContent(renderContext, depth);
						}

					} else {

						// use default implementation
						getSuper().renderContent(thisElement, renderContext, depth);
					}
				}
			}
		);

		return frameworkMethods;
	}

	void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
	}

	// ----- private static methods -----
	private static void handleRequestData(final RenderContext renderContext) {

		final HttpServletRequest request = renderContext.getRequest();
		final String contentType         = request.getHeader("content-type");

		if (StringUtils.isEmpty(contentType)) {

			handleUnknownRequestData(renderContext);

		} else {

			handleJsonRequestData(renderContext);
		}
	}

	private static void handleJsonRequestData(final RenderContext renderContext) {

		try {
			final Gson gson                         = new GsonBuilder().create();
			final Map<String, java.lang.Object> map = gson.fromJson(renderContext.getRequest().getReader(), java.util.Map.class);

			for (final Entry<String, java.lang.Object> entry : map.entrySet()) {

				renderContext.setConstant(entry.getKey(), entry.getValue());
			}

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	private static void handleUnknownRequestData(final RenderContext renderContext) {

		final Map<String, String[]> parameters = renderContext.getRequest().getParameterMap();
		final Gson gson                        = new GsonBuilder().create();

		for (final String key : parameters.keySet()) {

			if (StringUtils.isNotBlank(key)) {

				// We want to support both GET and POST here. A JSON payload in a POST request comes with an
				// empty value (""), and JSON in the key, whereas a GET request parameter is stored correctly
				// as a key-value pair.

				final String[] value = parameters.get(key);
				if (value.length == 1 && StringUtils.isBlank(value[0])) {

					// assume JSON
					final java.util.Map<String, java.lang.Object> map = (java.util.Map)gson.fromJson(key, java.util.Map.class);
					for (final Entry<String, java.lang.Object> entry : map.entrySet()) {

						renderContext.setConstant(entry.getKey(), entry.getValue());
					}

				} else {

					// assume GET with one or more values
					if (value.length == 1) {

						renderContext.setConstant(key, value[0]);

					} else {

						renderContext.setConstant(key, Arrays.asList(value));
					}
				}
			}
		}
	}
}
