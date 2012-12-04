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

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.core.property.CollectionProperty;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Head extends HtmlElement {

	public static final CollectionProperty<Html>   htmls   = new CollectionProperty<Html>(Html.class, RelType.CONTAINS, Direction.INCOMING);
	public static final CollectionProperty<Title>  titles  = new CollectionProperty<Title>(Title.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Style>  styles  = new CollectionProperty<Style>(Style.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Script> scripts = new CollectionProperty<Script>(Script.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Link>   links   = new CollectionProperty<Link>(Link.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Meta>   metas   = new CollectionProperty<Meta>(Meta.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Base>   bases   = new CollectionProperty<Base>(Base.class, RelType.CONTAINS, Direction.OUTGOING);
}
