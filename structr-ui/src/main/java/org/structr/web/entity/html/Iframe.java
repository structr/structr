/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 */
public class Iframe extends DOMElement {
	
	public static final Property<String> _src             = new HtmlProperty("src");
	public static final Property<String> _srcdoc          = new HtmlProperty("srcdoc");
	public static final Property<String> _sandbox         = new HtmlProperty("sandbox");
	public static final Property<String> _seamless        = new HtmlProperty("seamless");
	public static final Property<String> _allowfullscreen = new HtmlProperty("allowfullscreen");
	public static final Property<String> _width           = new HtmlProperty("width");
	public static final Property<String> _height          = new HtmlProperty("height");

	public static final View htmlView = new View(Img.class, PropertyView.Html,
	    _src, _srcdoc, _sandbox, _seamless, _allowfullscreen, _width, _height
	);
	
}
