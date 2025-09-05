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

public class Embed extends GenericHtmlElementTraitDefinition {

	public static final String SRC_PROPERTY    = getPrefixedHTMLAttributeName("src");
	public static final String TYPE_PROPERTY   = getPrefixedHTMLAttributeName("type");
	public static final String WIDTH_PROPERTY  = getPrefixedHTMLAttributeName("width");
	public static final String HEIGHT_PROPERTY = getPrefixedHTMLAttributeName("height");

	public Embed() {
		super(StructrTraits.EMBED);
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

		final PropertyKey<String> srcProperty    = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> typeProperty   = new StringProperty(TYPE_PROPERTY);
		final PropertyKey<String> widthProperty  = new StringProperty(WIDTH_PROPERTY);
		final PropertyKey<String> heightProperty = new StringProperty(HEIGHT_PROPERTY);

		return newSet(
			srcProperty, typeProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					SRC_PROPERTY, TYPE_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			)
		);
	}
}
