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
import org.structr.web.common.HtmlProperty;

import java.util.Map;
import java.util.Set;

public class Form extends GenericHtmlElementTraitDefinition {

	public Form() {
		super("Form");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> acceptCharsetProperty = new HtmlProperty("accept-charset");
		final PropertyKey<String> actionProperty = new HtmlProperty("action");
		final PropertyKey<String> autocompleteProperty = new HtmlProperty("autocomplete");
		final PropertyKey<String> enctypeProperty = new HtmlProperty("enctype");
		final PropertyKey<String> methodProperty = new HtmlProperty("method");
		final PropertyKey<String> nameProperty = new HtmlProperty("name");
		final PropertyKey<String> novalidateProperty = new HtmlProperty("novalidate");
		final PropertyKey<String> targetProperty = new HtmlProperty("target");

		return newSet(
			acceptCharsetProperty, actionProperty, autocompleteProperty, enctypeProperty, methodProperty, nameProperty, novalidateProperty, targetProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"accept-charset", "action", "autocomplete", "enctype", "method", "name", "novalidate", "target"
			)
		);
	}
}
