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
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.HtmlProperty;
import org.structr.web.traits.operations.IsVoidElement;

import java.util.Map;
import java.util.Set;

public class Input extends GenericHtmlElementTraitDefinition {

	public Input() {
		super("Input");
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

		final PropertyKey<String> acceptProperty = new HtmlProperty("accept");
		final PropertyKey<String> altProperty = new HtmlProperty("alt");
		final PropertyKey<String> autocompleteProperty = new HtmlProperty("autocomplete");
		final PropertyKey<String> autofocusProperty = new HtmlProperty("autofocus");
		final PropertyKey<String> checkedProperty = new HtmlProperty("checked");
		final PropertyKey<String> dirnameProperty = new HtmlProperty("dirname");
		final PropertyKey<String> disabledProperty = new HtmlProperty("disabled");
		final PropertyKey<String> formProperty = new HtmlProperty("form");
		final PropertyKey<String> formactionProperty = new HtmlProperty("formaction");
		final PropertyKey<String> formenctypeProperty = new HtmlProperty("formenctype");
		final PropertyKey<String> formmethodProperty = new HtmlProperty("formmethod");
		final PropertyKey<String> formnovalidateProperty = new HtmlProperty("formnovalidate");
		final PropertyKey<String> formtargetProperty = new HtmlProperty("formtarget");
		final PropertyKey<String> heightProperty = new HtmlProperty("height");
		final PropertyKey<String> listProperty = new HtmlProperty("list");
		final PropertyKey<String> maxProperty = new HtmlProperty("max");
		final PropertyKey<String> maxlengthProperty = new HtmlProperty("maxlength");
		final PropertyKey<String> minProperty = new HtmlProperty("min");
		final PropertyKey<String> multipleProperty = new HtmlProperty("multiple");
		final PropertyKey<String> nameProperty = new HtmlProperty("name");
		final PropertyKey<String> patternProperty = new HtmlProperty("pattern");
		final PropertyKey<String> placeholderProperty = new HtmlProperty("placeholder");
		final PropertyKey<String> readonlyProperty = new HtmlProperty("readonly");
		final PropertyKey<String> requiredProperty = new HtmlProperty("required");
		final PropertyKey<String> sizeProperty = new HtmlProperty("size");
		final PropertyKey<String> srcProperty = new HtmlProperty("src");
		final PropertyKey<String> stepProperty = new HtmlProperty("step");
		final PropertyKey<String> typeProperty = new HtmlProperty("type");
		final PropertyKey<String> valueProperty = new HtmlProperty("value");
		final PropertyKey<String> widthProperty = new HtmlProperty("width");

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
				"accept", "alt", "autocomplete", "autofocus", "checked", "dirname", "disabled",
				"form", "formaction", "formenctype", "formmethod", "formnovalidate", "formtarget",
				"height", "list", "max", "maxlength", "min", "multiple", "name",
				"pattern", "placeholder", "readonly", "required", "size", "src", "step",
				"type", "value", "width"
			)
		);
	}
}
