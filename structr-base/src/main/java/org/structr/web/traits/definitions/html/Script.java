/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.operations.HandleNewChild;

import java.util.Map;
import java.util.Set;

public class Script extends GenericHtmlElementTraitDefinition {

	public static final String SRC_PROPERTY     = getPrefixedHTMLAttributeName("src");
	public static final String ASYNC_PROPERTY   = getPrefixedHTMLAttributeName("async");
	public static final String DEFER_PROPERTY   = getPrefixedHTMLAttributeName("defer");
	public static final String TYPE_PROPERTY    = getPrefixedHTMLAttributeName("type");
	public static final String CHARSET_PROPERTY = getPrefixedHTMLAttributeName("charset");

	public Script() {
		super(StructrTraits.SCRIPT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		final Map<Class, LifecycleMethod> methods = super.createLifecycleMethods(traitsInstance);

		methods.put(
			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final PropertyKey<String> key = graphObject.getTraits().key(TYPE_PROPERTY);
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

					if (newChild.is(StructrTraits.CONTENT)) {

						try {
							final PropertyKey<String> key = node.getTraits().key(TYPE_PROPERTY);
							final String scriptType       = node.getProperty(key);

							if (StringUtils.isNotBlank(scriptType) && StringUtils.isBlank(newChild.as(Content.class).getContentType())) {

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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> srcProperty     = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> asyncProperty   = new StringProperty(ASYNC_PROPERTY);
		final PropertyKey<String> deferProperty   = new StringProperty(DEFER_PROPERTY);
		final PropertyKey<String> typeProperty    = new StringProperty(TYPE_PROPERTY);
		final PropertyKey<String> charsetProperty = new StringProperty(CHARSET_PROPERTY);

		return newSet(
			srcProperty, asyncProperty, deferProperty, typeProperty, charsetProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					SRC_PROPERTY, ASYNC_PROPERTY, DEFER_PROPERTY, TYPE_PROPERTY, CHARSET_PROPERTY
			)
		);
	}
}
