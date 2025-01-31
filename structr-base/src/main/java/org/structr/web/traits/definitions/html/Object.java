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

public class Object extends GenericHtmlElementTraitDefinition {

	public Object() {
		super("Object");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> typeProperty = new HtmlProperty("type");
		final PropertyKey<String> typemustmatchProperty = new HtmlProperty("typemustmatch");
		final PropertyKey<String> usemapProperty = new HtmlProperty("usemap");
		final PropertyKey<String> formProperty = new HtmlProperty("form");
		final PropertyKey<String> widthProperty = new HtmlProperty("width");
		final PropertyKey<String> heightProperty = new HtmlProperty("height");

		return newSet(
			typeProperty, typemustmatchProperty, usemapProperty, formProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"type", "typemustmatch", "usemap", "form", "width", "height"
			)
		);
	}
}
