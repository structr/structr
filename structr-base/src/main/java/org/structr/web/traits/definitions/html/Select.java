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
import org.structr.core.traits.TraitsInstance;

import java.util.Map;
import java.util.Set;

public class Select extends GenericHtmlElementTraitDefinition {

	public static final String NAME_PROPERTY      = getPrefixedHTMLAttributeName("name");
	public static final String DISABLED_PROPERTY  = getPrefixedHTMLAttributeName("disabled");
	public static final String ACCEPT_PROPERTY    = getPrefixedHTMLAttributeName("accept");
	public static final String FORM_PROPERTY      = getPrefixedHTMLAttributeName("form");
	public static final String SIZE_PROPERTY      = getPrefixedHTMLAttributeName("size");
	public static final String MULTIPLE_PROPERTY  = getPrefixedHTMLAttributeName("multiple");
	public static final String AUTOFOCUS_PROPERTY = getPrefixedHTMLAttributeName("autofocus");
	public static final String REQUIRED_PROPERTY  = getPrefixedHTMLAttributeName("required");

	public Select() {
		super(StructrTraits.SELECT);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> nameProperty      = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> disabledProperty  = new StringProperty(DISABLED_PROPERTY);
		final PropertyKey<String> acceptProperty    = new StringProperty(ACCEPT_PROPERTY);
		final PropertyKey<String> formProperty      = new StringProperty(FORM_PROPERTY);
		final PropertyKey<String> sizeProperty      = new StringProperty(SIZE_PROPERTY);
		final PropertyKey<String> multipleProperty  = new StringProperty(MULTIPLE_PROPERTY);
		final PropertyKey<String> autofocusProperty = new StringProperty(AUTOFOCUS_PROPERTY);
		final PropertyKey<String> requiredProperty  = new StringProperty(REQUIRED_PROPERTY);

		return newSet(
			nameProperty, disabledProperty, acceptProperty, formProperty, sizeProperty, multipleProperty, autofocusProperty, requiredProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					NAME_PROPERTY, DISABLED_PROPERTY, ACCEPT_PROPERTY, FORM_PROPERTY, SIZE_PROPERTY, MULTIPLE_PROPERTY, AUTOFOCUS_PROPERTY, REQUIRED_PROPERTY
			)
		);
	}
}
