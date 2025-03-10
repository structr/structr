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

import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.traits.operations.IsVoidElement;

import java.util.Map;
import java.util.Set;

public class Input extends GenericHtmlElementTraitDefinition {

	public static final String ACCEPT_PROPERTY         = getPrefixedHTMLAttributeName("accept");
	public static final String ALT_PROPERTY            = getPrefixedHTMLAttributeName("alt");
	public static final String AUTOCOMPLETE_PROPERTY   = getPrefixedHTMLAttributeName("autocomplete");
	public static final String AUTOFOCUS_PROPERTY      = getPrefixedHTMLAttributeName("autofocus");
	public static final String CHECKED_PROPERTY        = getPrefixedHTMLAttributeName("checked");
	public static final String DIRNAME_PROPERTY        = getPrefixedHTMLAttributeName("dirname");
	public static final String DISABLED_PROPERTY       = getPrefixedHTMLAttributeName("disabled");
	public static final String FORM_PROPERTY           = getPrefixedHTMLAttributeName("form");
	public static final String FORMACTION_PROPERTY     = getPrefixedHTMLAttributeName("formaction");
	public static final String FORMENCTYPE_PROPERTY    = getPrefixedHTMLAttributeName("formenctype");
	public static final String FORMMETHOD_PROPERTY     = getPrefixedHTMLAttributeName("formmethod");
	public static final String FORMNOVALIDATE_PROPERTY = getPrefixedHTMLAttributeName("formnovalidate");
	public static final String FORMTARGET_PROPERTY     = getPrefixedHTMLAttributeName("formtarget");
	public static final String HEIGHT_PROPERTY         = getPrefixedHTMLAttributeName("height");
	public static final String LIST_PROPERTY           = getPrefixedHTMLAttributeName("list");
	public static final String MAX_PROPERTY            = getPrefixedHTMLAttributeName("max");
	public static final String MAXLENGTH_PROPERTY      = getPrefixedHTMLAttributeName("maxlength");
	public static final String MIN_PROPERTY            = getPrefixedHTMLAttributeName("min");
	public static final String MULTIPLE_PROPERTY       = getPrefixedHTMLAttributeName("multiple");
	public static final String NAME_PROPERTY           = getPrefixedHTMLAttributeName("name");
	public static final String PATTERN_PROPERTY        = getPrefixedHTMLAttributeName("pattern");
	public static final String PLACEHOLDER_PROPERTY    = getPrefixedHTMLAttributeName("placeholder");
	public static final String READONLY_PROPERTY       = getPrefixedHTMLAttributeName("readonly");
	public static final String REQUIRED_PROPERTY       = getPrefixedHTMLAttributeName("required");
	public static final String SIZE_PROPERTY           = getPrefixedHTMLAttributeName("size");
	public static final String SRC_PROPERTY            = getPrefixedHTMLAttributeName("src");
	public static final String STEP_PROPERTY           = getPrefixedHTMLAttributeName("step");
	public static final String TYPE_PROPERTY           = getPrefixedHTMLAttributeName("type");
	public static final String VALUE_PROPERTY          = getPrefixedHTMLAttributeName("value");
	public static final String WIDTH_PROPERTY          = getPrefixedHTMLAttributeName("width");

	public Input() {
		super(StructrTraits.INPUT);
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

		final PropertyKey<String> acceptProperty         = new StringProperty(ACCEPT_PROPERTY);
		final PropertyKey<String> altProperty            = new StringProperty(ALT_PROPERTY);
		final PropertyKey<String> autocompleteProperty   = new StringProperty(AUTOCOMPLETE_PROPERTY);
		final PropertyKey<String> autofocusProperty      = new StringProperty(AUTOFOCUS_PROPERTY);
		final PropertyKey<String> checkedProperty        = new StringProperty(CHECKED_PROPERTY);
		final PropertyKey<String> dirnameProperty        = new StringProperty(DIRNAME_PROPERTY);
		final PropertyKey<String> disabledProperty       = new StringProperty(DISABLED_PROPERTY);
		final PropertyKey<String> formProperty           = new StringProperty(FORM_PROPERTY);
		final PropertyKey<String> formactionProperty     = new StringProperty(FORMACTION_PROPERTY);
		final PropertyKey<String> formenctypeProperty    = new StringProperty(FORMENCTYPE_PROPERTY);
		final PropertyKey<String> formmethodProperty     = new StringProperty(FORMMETHOD_PROPERTY);
		final PropertyKey<String> formnovalidateProperty = new StringProperty(FORMNOVALIDATE_PROPERTY);
		final PropertyKey<String> formtargetProperty     = new StringProperty(FORMTARGET_PROPERTY);
		final PropertyKey<String> heightProperty         = new StringProperty(HEIGHT_PROPERTY);
		final PropertyKey<String> listProperty           = new StringProperty(LIST_PROPERTY);
		final PropertyKey<String> maxProperty            = new StringProperty(MAX_PROPERTY);
		final PropertyKey<String> maxlengthProperty      = new StringProperty(MAXLENGTH_PROPERTY);
		final PropertyKey<String> minProperty            = new StringProperty(MIN_PROPERTY);
		final PropertyKey<String> multipleProperty       = new StringProperty(MULTIPLE_PROPERTY);
		final PropertyKey<String> nameProperty           = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> patternProperty        = new StringProperty(PATTERN_PROPERTY);
		final PropertyKey<String> placeholderProperty    = new StringProperty(PLACEHOLDER_PROPERTY);
		final PropertyKey<String> readonlyProperty       = new StringProperty(READONLY_PROPERTY);
		final PropertyKey<String> requiredProperty       = new StringProperty(REQUIRED_PROPERTY);
		final PropertyKey<String> sizeProperty           = new StringProperty(SIZE_PROPERTY);
		final PropertyKey<String> srcProperty            = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> stepProperty           = new StringProperty(STEP_PROPERTY);
		final PropertyKey<String> typeProperty           = new StringProperty(TYPE_PROPERTY);
		final PropertyKey<String> valueProperty          = new StringProperty(VALUE_PROPERTY);
		final PropertyKey<String> widthProperty          = new StringProperty(WIDTH_PROPERTY);

		return newSet(
			acceptProperty, altProperty, autocompleteProperty, autofocusProperty, checkedProperty, dirnameProperty, disabledProperty,
			formProperty, formactionProperty, formenctypeProperty, formmethodProperty, formnovalidateProperty, formtargetProperty,
			heightProperty, listProperty, maxProperty, maxlengthProperty, minProperty, multipleProperty, nameProperty,
			patternProperty, placeholderProperty, readonlyProperty, requiredProperty, sizeProperty, srcProperty, stepProperty,
			typeProperty, valueProperty, widthProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					ACCEPT_PROPERTY, ALT_PROPERTY, AUTOCOMPLETE_PROPERTY, AUTOFOCUS_PROPERTY, CHECKED_PROPERTY, DIRNAME_PROPERTY,
					DISABLED_PROPERTY, FORM_PROPERTY, FORMACTION_PROPERTY, FORMENCTYPE_PROPERTY, FORMMETHOD_PROPERTY,
					FORMNOVALIDATE_PROPERTY, FORMTARGET_PROPERTY, HEIGHT_PROPERTY, LIST_PROPERTY, MAX_PROPERTY, MAXLENGTH_PROPERTY,
					MIN_PROPERTY, MULTIPLE_PROPERTY, NAME_PROPERTY, PATTERN_PROPERTY, PLACEHOLDER_PROPERTY, READONLY_PROPERTY,
					REQUIRED_PROPERTY, SIZE_PROPERTY, SRC_PROPERTY, STEP_PROPERTY, TYPE_PROPERTY, VALUE_PROPERTY, WIDTH_PROPERTY
			)
		);
	}
}
