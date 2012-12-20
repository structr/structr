/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity.html;

import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Li extends HtmlElement {

	public static final Property<String> _value = new HtmlProperty("value");

	public static final CollectionProperty<Content> contents = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<A>       as       = new CollectionProperty<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Span>    spans    = new CollectionProperty<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Div>     divs     = new CollectionProperty<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Img>     imgs     = new CollectionProperty<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ul>      uls      = new CollectionProperty<Ul>("uls", Ul.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ol>      ols      = new CollectionProperty<Ol>("ols", Ol.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
	public static final View htmlView = new View(Li.class, PropertyView.Html,
		_value
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
