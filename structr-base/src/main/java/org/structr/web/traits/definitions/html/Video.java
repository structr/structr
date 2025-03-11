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
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;

import java.util.Map;
import java.util.Set;

public class Video extends GenericHtmlElementTraitDefinition {

	public static final String SRC_PROPERTY         = getPrefixedHTMLAttributeName("src");
	public static final String CROSSORIGIN_PROPERTY = getPrefixedHTMLAttributeName("crossorigin");
	public static final String POSTER_PROPERTY      = getPrefixedHTMLAttributeName("poster");
	public static final String PRELOAD_PROPERTY     = getPrefixedHTMLAttributeName("preload");
	public static final String AUTOPLAY_PROPERTY    = getPrefixedHTMLAttributeName("autoplay");
	public static final String PLAYSINLINE_PROPERTY = getPrefixedHTMLAttributeName("playsinline");
	public static final String LOOP_PROPERTY        = getPrefixedHTMLAttributeName("loop");
	public static final String MUTED_PROPERTY       = getPrefixedHTMLAttributeName("muted");
	public static final String CONTROLS_PROPERTY    = getPrefixedHTMLAttributeName("controls");
	public static final String WIDTH_PROPERTY       = getPrefixedHTMLAttributeName("width");
	public static final String HEIGHT_PROPERTY      = getPrefixedHTMLAttributeName("height");

	public Video() {
		super(StructrTraits.VIDEO);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> srcProperty         = new StringProperty(SRC_PROPERTY);
		final PropertyKey<String> crossoriginProperty = new StringProperty(CROSSORIGIN_PROPERTY);
		final PropertyKey<String> posterProperty      = new StringProperty(POSTER_PROPERTY);
		final PropertyKey<String> preloadProperty     = new StringProperty(PRELOAD_PROPERTY);
		final PropertyKey<String> autoplayProperty    = new StringProperty(AUTOPLAY_PROPERTY);
		final PropertyKey<String> playsinlineProperty = new StringProperty(PLAYSINLINE_PROPERTY);
		final PropertyKey<String> loopProperty        = new StringProperty(LOOP_PROPERTY);
		final PropertyKey<String> mutedProperty       = new StringProperty(MUTED_PROPERTY);
		final PropertyKey<String> controlsProperty    = new StringProperty(CONTROLS_PROPERTY);
		final PropertyKey<String> widthProperty       = new StringProperty(WIDTH_PROPERTY);
		final PropertyKey<String> heightProperty      = new StringProperty(HEIGHT_PROPERTY);

		return newSet(
			srcProperty,
			crossoriginProperty,
			posterProperty,
			preloadProperty,
			autoplayProperty,
			playsinlineProperty,
			loopProperty,
			mutedProperty,
			controlsProperty,
			widthProperty,
			heightProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
					SRC_PROPERTY, CROSSORIGIN_PROPERTY, POSTER_PROPERTY, PRELOAD_PROPERTY, AUTOPLAY_PROPERTY,
					PLAYSINLINE_PROPERTY, LOOP_PROPERTY, MUTED_PROPERTY, CONTROLS_PROPERTY, WIDTH_PROPERTY, HEIGHT_PROPERTY
			)
		);
	}
}
