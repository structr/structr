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

public class Th extends GenericHtmlElementTraitDefinition {

	public Th() {
		super("Th");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> colspanProperty = new HtmlProperty("colspan");
		final PropertyKey<String> rowspanProperty = new HtmlProperty("rowspan");
		final PropertyKey<String> headersProperty = new HtmlProperty("headers");
		final PropertyKey<String> scopeProperty = new HtmlProperty("scope");
		final PropertyKey<String> abbrProperty = new HtmlProperty("abbr");

		return newSet(
			colspanProperty, rowspanProperty, headersProperty, scopeProperty, abbrProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"_html_colspan", "_html_rowspan", "_html_headers", "_html_scope", "_html_abbr"
			)
		);
	}
}
