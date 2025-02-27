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

public class Video extends GenericHtmlElementTraitDefinition {

	public Video() {
		super("Video");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> srcProperty         = new HtmlProperty("src");
		final PropertyKey<String> crossoriginProperty = new HtmlProperty("crossorigin");
		final PropertyKey<String> posterProperty      = new HtmlProperty("poster");
		final PropertyKey<String> preloadProperty     = new HtmlProperty("preload");
		final PropertyKey<String> autoplayProperty    = new HtmlProperty("autoplay");
		final PropertyKey<String> playsinlineProperty = new HtmlProperty("playsinline");
		final PropertyKey<String> loopProperty        = new HtmlProperty("loop");
		final PropertyKey<String> mutedProperty       = new HtmlProperty("muted");
		final PropertyKey<String> controlsProperty    = new HtmlProperty("controls");
		final PropertyKey<String> widthProperty       = new HtmlProperty("width");
		final PropertyKey<String> heightProperty      = new HtmlProperty("height");

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
				"_html_src", "_html_crossorigin", "_html_poster", "_html_preload", "_html_autoplay",
				"_html_playsinline", "_html_loop", "_html_muted", "_html_controls", "_html_width", "_html_height"
			)
		);
	}
}
