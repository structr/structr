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
public class Body extends HtmlElement {

	public static final CollectionProperty<Html>       htmls       = new CollectionProperty<Html>(Html.class, RelType.CONTAINS, Direction.INCOMING);
	public static final CollectionProperty<Header>     headers     = new CollectionProperty<Header>(Header.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Footer>     footers     = new CollectionProperty<Footer>(Footer.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Article>    articles    = new CollectionProperty<Article>(Article.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Blockquote> blockquotes = new CollectionProperty<Blockquote>(Blockquote.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Table>      tables      = new CollectionProperty<Table>(Table.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Div>        divs        = new CollectionProperty<Div>(Div.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Span>       spans       = new CollectionProperty<Span>(Span.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<B>          bs          = new CollectionProperty<B>(B.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<P>          ps          = new CollectionProperty<P>(P.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Label>      labels      = new CollectionProperty<Label>(Label.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Textarea>   textareas   = new CollectionProperty<Textarea>(Textarea.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Input>      inputs      = new CollectionProperty<Input>(Input.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Address>    addresss    = new CollectionProperty<Address>(Address.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Br>         brs         = new CollectionProperty<Br>(Br.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Hr>         hrs         = new CollectionProperty<Hr>(Hr.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<A>          as          = new CollectionProperty<A>(A.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H1>         h1s         = new CollectionProperty<H1>(H1.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H2>         h2s         = new CollectionProperty<H2>(H2.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H3>         h3s         = new CollectionProperty<H3>(H3.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H4>         h4s         = new CollectionProperty<H4>(H4.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H5>         h5s         = new CollectionProperty<H5>(H5.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<H6>         h6s         = new CollectionProperty<H6>(H6.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Img>        imgs        = new CollectionProperty<Img>(Img.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ul>         uls         = new CollectionProperty<Ul>(Ul.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Ol>         ols         = new CollectionProperty<Ol>(Ol.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Form>       forms       = new CollectionProperty<Form>(Form.class, RelType.CONTAINS, Direction.OUTGOING);
	public static final CollectionProperty<Script>     scripts     = new CollectionProperty<Script>(Script.class, RelType.CONTAINS, Direction.OUTGOING);
}
