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
public class Source extends DOMElement {

	public static final Property<String> _src		= new HtmlProperty("src");
	public static final Property<String> _type		= new HtmlProperty("type");
	public static final Property<String> _media		= new HtmlProperty("media");
	
	public static final org.structr.common.View htmlView	= new org.structr.common.View(Source.class, PropertyView.Html,
		_src, _type, _media
	);

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}
}
