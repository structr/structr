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

import java.util.Map;
import java.util.Set;

public class Td extends GenericHtmlElementTraitDefinition {

	public static final String COLSPAN_PROPERTY = getPrefixedHTMLAttributeName("colspan");
	public static final String ROWSPAN_PROPERTY = getPrefixedHTMLAttributeName("rowspan");
	public static final String HEADERS_PROPERTY = getPrefixedHTMLAttributeName("headers");

	public Td() {
		super(StructrTraits.TD);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> colspanProperty = new StringProperty(COLSPAN_PROPERTY);
		final PropertyKey<String> rowspanProperty = new StringProperty(ROWSPAN_PROPERTY);
		final PropertyKey<String> headersProperty = new StringProperty(HEADERS_PROPERTY);

		return newSet(
			colspanProperty, rowspanProperty, headersProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					COLSPAN_PROPERTY, ROWSPAN_PROPERTY, HEADERS_PROPERTY
			)
		);
	}
}
