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

public class Dialog extends GenericHtmlElementTraitDefinition {

	public static final String OPEN_PROPERTY = getPrefixedHTMLAttributeName("open");

	public Dialog() {
		super(StructrTraits.DIALOG);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> openProperty = new StringProperty(OPEN_PROPERTY);

		return newSet(
			openProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					OPEN_PROPERTY
			)
		);
	}
}
