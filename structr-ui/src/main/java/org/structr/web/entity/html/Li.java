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

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang3.ArrayUtils;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Li extends DOMElement {

	public static final Property<String> _value = new HtmlProperty("value");

//	public static final EndNodes<Content> contents = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<A>       as       = new EndNodes<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Span>    spans    = new EndNodes<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Div>     divs     = new EndNodes<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Img>     imgs     = new EndNodes<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Ul>      uls      = new EndNodes<Ul>("uls", Ul.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Ol>      ols      = new EndNodes<Ol>("ols", Ol.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final View htmlView = new View(Li.class, PropertyView.Html,
		_value
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
