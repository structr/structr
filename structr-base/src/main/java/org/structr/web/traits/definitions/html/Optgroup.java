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

import java.util.Map;
import java.util.Set;

public class Optgroup extends GenericHtmlElementTraitDefinition {

	public static final String DISABLED_PROPERTY = getPrefixedHTMLAttributeName("disabled");
	public static final String LABEL_PROPERTY    = getPrefixedHTMLAttributeName("label");

	public Optgroup() {
		super(StructrTraits.OPTGROUP);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> disabledProperty = new StringProperty(DISABLED_PROPERTY);
		final PropertyKey<String> labelProperty    = new StringProperty(LABEL_PROPERTY);

		return newSet(
			disabledProperty, labelProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					DISABLED_PROPERTY, LABEL_PROPERTY
			)
		);
	}
}
