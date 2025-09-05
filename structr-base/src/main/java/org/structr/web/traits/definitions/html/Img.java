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
import org.structr.web.traits.operations.AvoidWhitespace;
import org.structr.web.traits.operations.IsVoidElement;

import java.util.Map;
import java.util.Set;

public class Img extends GenericHtmlElementTraitDefinition {

	public static final String ALT_PROPERTY         = getPrefixedHTMLAttributeName("alt");
	public static final String SRC_PROPERTY         = getPrefixedHTMLAttributeName("src");
	public static final String CROSSORIGIN_PROPERTY = getPrefixedHTMLAttributeName("crossorigin");
	public static final String USEMAP_PROPERTY      = getPrefixedHTMLAttributeName("usemap");
	public static final String ISMAP_PROPERTY       = getPrefixedHTMLAttributeName("ismap");
	public static final String WIDTH_PROPERTY       = getPrefixedHTMLAttributeName("width");
	public static final String HEIGHT_PROPERTY      = getPrefixedHTMLAttributeName("height");

	public Img() {
		super(StructrTraits.IMG);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(

			AvoidWhitespace.class,
			new AvoidWhitespace() {

				@Override
				public boolean avoidWhitespace() {
					return true;
				}
			}
		);

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

		final PropertyKey<String> altProperty         = new StringProperty(ALT_PROPERTY);
		final PropertyKey<String> srcProperty         = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> crossoriginProperty = new StringProperty(CROSSORIGIN_PROPERTY);
		final PropertyKey<String> usemapProperty      = new StringProperty(USEMAP_PROPERTY);
		final PropertyKey<String> ismapProperty       = new StringProperty(ISMAP_PROPERTY);
		final PropertyKey<String> widthProperty       = new StringProperty(WIDTH_PROPERTY);
		final PropertyKey<String> heightProperty      = new StringProperty(HEIGHT_PROPERTY);

		return newSet(
			altProperty, srcProperty, crossoriginProperty, usemapProperty, ismapProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					ALT_PROPERTY, SRC_PROPERTY, CROSSORIGIN_PROPERTY, USEMAP_PROPERTY, ISMAP_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			)
		);
	}
}
