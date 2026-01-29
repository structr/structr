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

public class Style extends GenericHtmlElementTraitDefinition {

	public static final String MEDIA_PROPERTY  = getPrefixedHTMLAttributeName("media");
	public static final String TYPE_PROPERTY   = getPrefixedHTMLAttributeName("type");
	public static final String SCOPED_PROPERTY = getPrefixedHTMLAttributeName("scoped");

	public Style() {
		super(StructrTraits.STYLE);
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
						graphObject.setProperty(key, "text/css");
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

						final Content content = newChild.as(Content.class);

						try {

							final String thisContentType  = node.is(StructrTraits.CONTENT) ? node.as(Content.class).getContentType() : null;
							final String childContentType = content.getContentType();

							if (childContentType == null && thisContentType != null) {

								content.setContentType(thisContentType);
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

		final PropertyKey<String> mediaProperty  = new StringProperty(MEDIA_PROPERTY);
		final PropertyKey<String> typeProperty   = new StringProperty(TYPE_PROPERTY);
		final PropertyKey<String> scopedProperty = new StringProperty(SCOPED_PROPERTY);

		return newSet(
			mediaProperty, typeProperty, scopedProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					MEDIA_PROPERTY, TYPE_PROPERTY, SCOPED_PROPERTY
			)
		);
	}
}
