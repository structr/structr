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

public class Iframe extends GenericHtmlElementTraitDefinition {

	public Iframe() {
		super("Iframe");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> nameProperty = new HtmlProperty("name");
		final PropertyKey<String> srcProperty = new HtmlProperty("src");
		final PropertyKey<String> srcdocProperty = new HtmlProperty("srcdoc");
		final PropertyKey<String> sandboxProperty = new HtmlProperty("sandbox");
		final PropertyKey<String> seamlessProperty = new HtmlProperty("seamless");
		final PropertyKey<String> allowfullscreenProperty = new HtmlProperty("allowfullscreen");
		final PropertyKey<String> widthProperty = new HtmlProperty("width");
		final PropertyKey<String> heightProperty = new HtmlProperty("height");

		return newSet(
			nameProperty, srcProperty, srcdocProperty, sandboxProperty, seamlessProperty, allowfullscreenProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"_html_name", "_html_src", "_html_srcdoc", "_html_sandbox", "_html_seamless", "_html_allowfullscreen", "_html_width", "_html_height"
			)
		);
	}
}
