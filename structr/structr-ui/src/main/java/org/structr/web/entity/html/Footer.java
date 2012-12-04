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
public class Footer extends HtmlElement {

	public static final CollectionProperty<Content>  Contents  = new CollectionProperty<Content>(Content.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Div>      Divs      = new CollectionProperty<Div>(Div.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Span>     Spans     = new CollectionProperty<Span>(Span.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<B>        Bs        = new CollectionProperty<B>(B.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<P>        Ps        = new CollectionProperty<P>(P.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Label>    Labels    = new CollectionProperty<Label>(Label.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Textarea> Textareas = new CollectionProperty<Textarea>(Textarea.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Input>    Inputs    = new CollectionProperty<Input>(Input.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Address>  Addresss  = new CollectionProperty<Address>(Address.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Br>       Brs       = new CollectionProperty<Br>(Br.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Hr>       Hrs       = new CollectionProperty<Hr>(Hr.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<A>        As        = new CollectionProperty<A>(A.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H1>       H1s       = new CollectionProperty<H1>(H1.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H2>       H2s       = new CollectionProperty<H2>(H2.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H3>       H3s       = new CollectionProperty<H3>(H3.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H4>       H4s       = new CollectionProperty<H4>(H4.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H5>       H5s       = new CollectionProperty<H5>(H5.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H6>       H6s       = new CollectionProperty<H6>(H6.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Img>      Imgs      = new CollectionProperty<Img>(Img.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ul>       Uls       = new CollectionProperty<Ul>(Ul.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ol>       Ols       = new CollectionProperty<Ol>(Ol.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Form>     Forms     = new CollectionProperty<Form>(Form.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Script>   Scripts   = new CollectionProperty<Script>(Script.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Table>    Tables    = new CollectionProperty<Table>(Table.class, RelType.CONTAINS, Direction.OUTGOING);
}
