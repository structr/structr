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
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.traits.operations.AvoidWhitespace;

import java.util.Map;
import java.util.Set;

public class Textarea extends GenericHtmlElementTraitDefinition {

	public static final String NAME_PROPERTY        = getPrefixedHTMLAttributeName("name");
	public static final String DISABLED_PROPERTY    = getPrefixedHTMLAttributeName("disabled");
	public static final String FORM_PROPERTY        = getPrefixedHTMLAttributeName("form");
	public static final String READONLY_PROPERTY    = getPrefixedHTMLAttributeName("readonly");
	public static final String MAXLENGTH_PROPERTY   = getPrefixedHTMLAttributeName("maxlength");
	public static final String AUTOFOCUS_PROPERTY   = getPrefixedHTMLAttributeName("autofocus");
	public static final String REQUIRED_PROPERTY    = getPrefixedHTMLAttributeName("required");
	public static final String PLACEHOLDER_PROPERTY = getPrefixedHTMLAttributeName("placeholder");
	public static final String DIRNAME_PROPERTY     = getPrefixedHTMLAttributeName("dirname");
	public static final String ROWS_PROPERTY        = getPrefixedHTMLAttributeName("rows");
	public static final String WRAP_PROPERTY        = getPrefixedHTMLAttributeName("wrap");
	public static final String COLS_PROPERTY        = getPrefixedHTMLAttributeName("cols");

	public Textarea() {
		super(StructrTraits.TEXTAREA);
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

		return frameworkMethods;
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> nameProperty        = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> disabledProperty    = new StringProperty(DISABLED_PROPERTY);
		final PropertyKey<String> formProperty        = new StringProperty(FORM_PROPERTY);
		final PropertyKey<String> readonlyProperty    = new StringProperty(READONLY_PROPERTY);
		final PropertyKey<String> maxlengthProperty   = new StringProperty(MAXLENGTH_PROPERTY);
		final PropertyKey<String> autofocusProperty   = new StringProperty(AUTOFOCUS_PROPERTY);
		final PropertyKey<String> requiredProperty    = new StringProperty(REQUIRED_PROPERTY);
		final PropertyKey<String> placeholderProperty = new StringProperty(PLACEHOLDER_PROPERTY);
		final PropertyKey<String> dirnameProperty     = new StringProperty(DIRNAME_PROPERTY);
		final PropertyKey<String> rowsProperty        = new StringProperty(ROWS_PROPERTY);
		final PropertyKey<String> wrapProperty        = new StringProperty(WRAP_PROPERTY);
		final PropertyKey<String> colsProperty        = new StringProperty(COLS_PROPERTY);

		return newSet(
			nameProperty, disabledProperty, formProperty, readonlyProperty, maxlengthProperty, autofocusProperty,
			requiredProperty, placeholderProperty, dirnameProperty, rowsProperty, wrapProperty, colsProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					NAME_PROPERTY, DISABLED_PROPERTY, FORM_PROPERTY, READONLY_PROPERTY, MAXLENGTH_PROPERTY, AUTOFOCUS_PROPERTY,
					REQUIRED_PROPERTY, PLACEHOLDER_PROPERTY, DIRNAME_PROPERTY, ROWS_PROPERTY, WRAP_PROPERTY, COLS_PROPERTY
			)
		);
	}
}
