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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.LinkSource;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Img extends LinkSource {

	public static final Property<String> _alt         = new HtmlProperty("alt");
	public static final Property<String> _src         = new HtmlProperty("src");
	public static final Property<String> _crossorigin = new HtmlProperty("crossorigin");
	public static final Property<String> _usemap      = new HtmlProperty("usemap");
	public static final Property<String> _ismap       = new HtmlProperty("ismap");
	public static final Property<String> _width       = new HtmlProperty("width");
	public static final Property<String> _height      = new HtmlProperty("height");
	
//	public static final EndNodes<Div> divs = new EndNodes<Div>("divs", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<P>   ps   = new EndNodes<P>("ps", P.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<A>   as   = new EndNodes<A>("as", A.class, RelType.CONTAINS, Direction.INCOMING, false);
	
	public static final View htmlView = new View(Img.class, PropertyView.Html,
	    _alt, _src, _crossorigin, _usemap, _ismap, _width, _height
	);

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
