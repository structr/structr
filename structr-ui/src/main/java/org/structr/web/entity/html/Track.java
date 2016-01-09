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
public class Track extends DOMElement {

	public static final Property<String> _kind		= new HtmlProperty("kind");
	public static final Property<String> _src		= new HtmlProperty("src");
	public static final Property<String> _srclang		= new HtmlProperty("srclang");
	public static final Property<String> _label		= new HtmlProperty("label");
	public static final Property<String> _default		= new HtmlProperty("default");
	
	public static final org.structr.common.View htmlView	= new org.structr.common.View(Track.class, PropertyView.Html,
		_kind, _src, _srclang, _label, _default
	);
	
	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}
}
