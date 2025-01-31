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
import org.structr.web.traits.operations.AvoidWhitespace;

import java.util.Map;
import java.util.Set;

public class Textarea extends GenericHtmlElementTraitDefinition {

	public Textarea() {
		super("Textarea");
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
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> nameProperty = new HtmlProperty("name");
		final PropertyKey<String> disabledProperty = new HtmlProperty("disabled");
		final PropertyKey<String> formProperty = new HtmlProperty("form");
		final PropertyKey<String> readonlyProperty = new HtmlProperty("readonly");
		final PropertyKey<String> maxlengthProperty = new HtmlProperty("maxlength");
		final PropertyKey<String> autofocusProperty = new HtmlProperty("autofocus");
		final PropertyKey<String> requiredProperty = new HtmlProperty("required");
		final PropertyKey<String> placeholderProperty = new HtmlProperty("placeholder");
		final PropertyKey<String> dirnameProperty = new HtmlProperty("dirname");
		final PropertyKey<String> rowsProperty = new HtmlProperty("rows");
		final PropertyKey<String> wrapProperty = new HtmlProperty("wrap");
		final PropertyKey<String> colsProperty = new HtmlProperty("cols");

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
				"name", "disabled", "form", "readonly", "maxlength", "autofocus",
				"required", "placeholder", "dirname", "rows", "wrap", "cols"
			)
		);
	}
}
