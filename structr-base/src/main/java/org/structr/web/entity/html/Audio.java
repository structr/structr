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
package org.structr.web.entity.html;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 */
public class Audio extends DOMElement {

	public static final Property<String> htmlSrcProperty         = new StringProperty("_html_src");
	public static final Property<String> htmlCrossOriginProperty = new StringProperty("_html_crossorigin");
	public static final Property<String> htmlPreloadProperty     = new StringProperty("_html_preload");
	public static final Property<String> htmlAutoplayProperty    = new StringProperty("_html_autoplay");
	public static final Property<String> htmlLoopProperty        = new StringProperty("_html_loop");
	public static final Property<String> htmlMutedProperty       = new StringProperty("_html_muted");
	public static final Property<String> htmlControlsProperty    = new StringProperty("_html_controls");

	public static final View htmlView = new View(Audio.class, PropertyView.Html,
		htmlSrcProperty, htmlCrossOriginProperty, htmlPreloadProperty, htmlAutoplayProperty, htmlLoopProperty, htmlMutedProperty, htmlControlsProperty
	);
}
