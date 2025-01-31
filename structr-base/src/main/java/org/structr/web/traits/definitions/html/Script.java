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
package org.structr.web.traits.definitions.html;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.operations.HandleNewChild;

import java.util.Map;
import java.util.Set;

public class Script extends GenericHtmlElementTraitDefinition {

	public Script() {
		super("Script");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		final Map<Class, LifecycleMethod> methods = super.getLifecycleMethods();

		methods.put(
			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final PropertyKey<String> key = graphObject.getTraits().key("_html_type");
					final String value            = graphObject.getProperty(key);

					if (StringUtils.isBlank(value)) {
						graphObject.setProperty(key, "text/javascript");
					}
				}
			}
		);

		return methods;
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(

			HandleNewChild.class,
			new HandleNewChild() {

				@Override
				public void handleNewChild(final DOMNode node, final DOMNode newChild) throws FrameworkException {

					if (newChild.is("Content")) {

						try {
							final PropertyKey<String> key = node.getTraits().key("_html_type");
							final String scriptType       = node.getProperty(key);

							if (StringUtils.isNotBlank(scriptType) && StringUtils.isBlank(newChild.getContentType())) {

								newChild.as(Content.class).setContentType(scriptType);
							}

						} catch (FrameworkException fex) {

							final Logger logger = LoggerFactory.getLogger(Script.class);
							logger.warn("Unable to set property on new child: {}", fex.getMessage());
						}
					}

					getSuper().handleNewChild(node, newChild);
				}
			}
		);

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> srcProperty = new HtmlProperty("src");
		final PropertyKey<String> asyncProperty = new HtmlProperty("async");
		final PropertyKey<String> deferProperty = new HtmlProperty("defer");
		final PropertyKey<String> typeProperty = new HtmlProperty("type");
		final PropertyKey<String> charsetProperty = new HtmlProperty("charset");

		return newSet(
			srcProperty, asyncProperty, deferProperty, typeProperty, charsetProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"src", "async", "defer", "type", "charset"
			)
		);
	}
}
