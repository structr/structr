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

public class Form extends GenericHtmlElementTraitDefinition {

	public static final String ACCEPT_CHARSET_PROPERTY = getPrefixedHTMLAttributeName("accept-charset");
	public static final String ACTION_PROPERTY         = getPrefixedHTMLAttributeName("action");
	public static final String AUTOCOMPLETE_PROPERTY   = getPrefixedHTMLAttributeName("autocomplete");
	public static final String ENCTYPE_PROPERTY        = getPrefixedHTMLAttributeName("enctype");
	public static final String METHOD_PROPERTY         = getPrefixedHTMLAttributeName("method");
	public static final String NAME_PROPERTY           = getPrefixedHTMLAttributeName("name");
	public static final String NOVALIDATE_PROPERTY     = getPrefixedHTMLAttributeName("novalidate");
	public static final String TARGET_PROPERTY         = getPrefixedHTMLAttributeName("target");

	public Form() {
		super(StructrTraits.FORM);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> acceptCharsetProperty = new StringProperty(ACCEPT_CHARSET_PROPERTY);
		final PropertyKey<String> actionProperty        = new StringProperty(ACTION_PROPERTY);
		final PropertyKey<String> autocompleteProperty  = new StringProperty(AUTOCOMPLETE_PROPERTY);
		final PropertyKey<String> enctypeProperty       = new StringProperty(ENCTYPE_PROPERTY);
		final PropertyKey<String> methodProperty        = new StringProperty(METHOD_PROPERTY);
		final PropertyKey<String> nameProperty          = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> novalidateProperty    = new StringProperty(NOVALIDATE_PROPERTY);
		final PropertyKey<String> targetProperty        = new StringProperty(TARGET_PROPERTY);

		return newSet(
			acceptCharsetProperty, actionProperty, autocompleteProperty, enctypeProperty, methodProperty, nameProperty, novalidateProperty, targetProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					ACCEPT_CHARSET_PROPERTY, ACTION_PROPERTY, AUTOCOMPLETE_PROPERTY, ENCTYPE_PROPERTY, METHOD_PROPERTY,
					NAME_PROPERTY, NOVALIDATE_PROPERTY, TARGET_PROPERTY
			)
		);
	}
}
