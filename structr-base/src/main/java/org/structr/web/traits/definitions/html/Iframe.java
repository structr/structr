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

public class Iframe extends GenericHtmlElementTraitDefinition {

	public static final String NAME_PROPERTY            = getPrefixedHTMLAttributeName("name");
	public static final String SRC_PROPERTY             = getPrefixedHTMLAttributeName("src");
	public static final String SRCDOC_PROPERTY          = getPrefixedHTMLAttributeName("srcdoc");
	public static final String SANDBOX_PROPERTY         = getPrefixedHTMLAttributeName("sandbox");
	public static final String SEAMLESS_PROPERTY        = getPrefixedHTMLAttributeName("seamless");
	public static final String ALLOWFULLSCREEN_PROPERTY = getPrefixedHTMLAttributeName("allowfullscreen");
	public static final String WIDTH_PROPERTY           = getPrefixedHTMLAttributeName("width");
	public static final String HEIGHT_PROPERTY          = getPrefixedHTMLAttributeName("height");

	public Iframe() {
		super(StructrTraits.IFRAME);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> nameProperty            = new StringProperty(NAME_PROPERTY);
		final PropertyKey<String> srcProperty             = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> srcdocProperty          = new StringProperty(SRCDOC_PROPERTY);
		final PropertyKey<String> sandboxProperty         = new StringProperty(SANDBOX_PROPERTY);
		final PropertyKey<String> seamlessProperty        = new StringProperty(SEAMLESS_PROPERTY);
		final PropertyKey<String> allowfullscreenProperty = new StringProperty(ALLOWFULLSCREEN_PROPERTY);
		final PropertyKey<String> widthProperty           = new StringProperty(WIDTH_PROPERTY);
		final PropertyKey<String> heightProperty          = new StringProperty(HEIGHT_PROPERTY);

		return newSet(
			nameProperty, srcProperty, srcdocProperty, sandboxProperty, seamlessProperty, allowfullscreenProperty, widthProperty, heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					NAME_PROPERTY, SRC_PROPERTY, SRCDOC_PROPERTY, SANDBOX_PROPERTY, SEAMLESS_PROPERTY, ALLOWFULLSCREEN_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			)
		);
	}
}
