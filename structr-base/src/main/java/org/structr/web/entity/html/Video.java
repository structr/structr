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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

public class Video extends DOMElement {

	public static final Property<String> htmlSrcProperty            = new StringProperty("_html_src").partOfBuiltInSchema();
	public static final Property<String> htmlCrossoriginProperty    = new StringProperty("_html_crossorigin").partOfBuiltInSchema();
	public static final Property<String> htmlPosterProperty         = new StringProperty("_html_poster").partOfBuiltInSchema();
	public static final Property<String> htmlPreloadProperty        = new StringProperty("_html_preload").partOfBuiltInSchema();
	public static final Property<String> htmlAutoplayProperty       = new StringProperty("_html_autoplay").partOfBuiltInSchema();
	public static final Property<String> htmlPlaysInlineProperty    = new StringProperty("_html_playsinline").partOfBuiltInSchema();
	public static final Property<String> htmlLoopProperty           = new StringProperty("_html_loop").partOfBuiltInSchema();
	public static final Property<String> htmlMutedProperty          = new StringProperty("_html_muted").partOfBuiltInSchema();
	public static final Property<String> htmlControlsProperty       = new StringProperty("_html_controls").partOfBuiltInSchema();
	public static final Property<String> htmlWidthProperty          = new StringProperty("_html_width").partOfBuiltInSchema();
	public static final Property<String> htmlHeightProperty         = new StringProperty("_html_height").partOfBuiltInSchema();

	public static final View htmlView = new View(Video.class, PropertyView.Html,
		htmlSrcProperty, htmlCrossoriginProperty, htmlPosterProperty, htmlPreloadProperty, htmlAutoplayProperty, htmlPlaysInlineProperty, htmlLoopProperty,
		htmlMutedProperty, htmlControlsProperty, htmlWidthProperty, htmlHeightProperty
	);

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}
}
