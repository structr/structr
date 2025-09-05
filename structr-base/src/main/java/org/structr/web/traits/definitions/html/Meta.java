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

import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.traits.operations.IsVoidElement;

import java.util.Map;
import java.util.Set;

public class Meta extends GenericHtmlElementTraitDefinition {

	public static final String NAME_PROPERTY       = getPrefixedHTMLAttributeName("name");
	public static final String HTTP_EQUIV_PROPERTY = getPrefixedHTMLAttributeName("http-equiv");
	public static final String CONTENT_PROPERTY    = getPrefixedHTMLAttributeName("content");
	public static final String CHARSET_PROPERTY    = getPrefixedHTMLAttributeName("charset");

	public Meta() {
		super(StructrTraits.META);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(

			IsVoidElement.class,
			new IsVoidElement() {

				@Override
				public boolean isVoidElement() {
					return true;
				}
			}
		);

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> nameProperty      = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> httpEquivProperty = new StringProperty(HTTP_EQUIV_PROPERTY);
		final PropertyKey<String> contentProperty   = new StringProperty(CONTENT_PROPERTY);
		final PropertyKey<String> charsetProperty   = new StringProperty(CHARSET_PROPERTY);

		return newSet(
			nameProperty, httpEquivProperty, contentProperty, charsetProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					NAME_PROPERTY, HTTP_EQUIV_PROPERTY, CONTENT_PROPERTY, CHARSET_PROPERTY
			)
		);
	}
}
