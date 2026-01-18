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

import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;

import java.util.Map;
import java.util.Set;

public class Object extends GenericHtmlElementTraitDefinition {

	public static final String TYPE_PROPERTY          = getPrefixedHTMLAttributeName("type");
	public static final String TYPEMUSTMATCH_PROPERTY = getPrefixedHTMLAttributeName("typemustmatch");
	public static final String USEMAP_PROPERTY        = getPrefixedHTMLAttributeName("usemap");
	public static final String FORM_PROPERTY          = getPrefixedHTMLAttributeName("form");
	public static final String WIDTH_PROPERTY         = getPrefixedHTMLAttributeName("width");
	public static final String HEIGHT_PROPERTY        = getPrefixedHTMLAttributeName("height");

	public Object() {
		super(StructrTraits.OBJECT);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> typeProperty          = new StringProperty(TYPE_PROPERTY);
		final PropertyKey<String> typemustmatchProperty = new StringProperty(TYPEMUSTMATCH_PROPERTY);
		final PropertyKey<String> usemapProperty        = new StringProperty(USEMAP_PROPERTY);
		final PropertyKey<String> formProperty          = new StringProperty(FORM_PROPERTY);
		final PropertyKey<String> widthProperty         = new StringProperty(WIDTH_PROPERTY);
		final PropertyKey<String> heightProperty        = new StringProperty(HEIGHT_PROPERTY);

		return newSet(
			typeProperty, typemustmatchProperty, usemapProperty, formProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					TYPE_PROPERTY, TYPEMUSTMATCH_PROPERTY, USEMAP_PROPERTY, FORM_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			)
		);
	}
}
