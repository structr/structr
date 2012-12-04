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
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Span extends HtmlElement {

	public static final CollectionProperty<Content> contents = new CollectionProperty<Content>(Content.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Span>    spans    = new CollectionProperty<Span>(Span.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<B>       bs       = new CollectionProperty<B>(B.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<P>       ps       = new CollectionProperty<P>(P.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Label>   labels   = new CollectionProperty<Label>(Label.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Input>   inputs   = new CollectionProperty<Input>(Input.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Address> addresss = new CollectionProperty<Address>(Address.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Footer>  footers  = new CollectionProperty<Footer>(Footer.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<A>       as       = new CollectionProperty<A>(A.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Img>     imgs     = new CollectionProperty<Img>(Img.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Script>  scripts  = new CollectionProperty<Script>(Script.class, RelType.CONTAINS, Direction.OUTGOING);

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}

}
