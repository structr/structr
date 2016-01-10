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
import org.structr.core.property.Property;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 */
public class Object extends DOMElement {

	//public static final Property<String> _data		= new HtmlProperty("data");
	public static final Property<String> _type		= new HtmlProperty("type");
	public static final Property<String> _typemustmatch	= new HtmlProperty("typemustmatch");
	public static final Property<String> _usemap		= new HtmlProperty("usemap");
	public static final Property<String> _form		= new HtmlProperty("form");
	public static final Property<String> _width		= new HtmlProperty("width");
	public static final Property<String> _height		= new HtmlProperty("height");

	public static final org.structr.common.View htmlView	= new org.structr.common.View(Object.class, PropertyView.Html,
		_data, _type, _typemustmatch, _usemap, _form, _width, _height
	);
	

}
