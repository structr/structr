/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.neo4j.graphdb.Direction;

import org.structr.web.common.RelType;
import org.structr.core.property.Endpoints;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Body extends DOMElement {

	public static final Endpoints<Html>       htmls       = new Endpoints<Html>("htmls", Html.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final Endpoints<Header>     headers     = new Endpoints<Header>("headers", Header.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Footer>     footers     = new Endpoints<Footer>("footers", Footer.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Article>    articles    = new Endpoints<Article>("articles", Article.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Blockquote> blockquotes = new Endpoints<Blockquote>("blockquotes", Blockquote.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Table>      tables      = new Endpoints<Table>("tables", Table.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Div>        divs        = new Endpoints<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Span>       spans       = new Endpoints<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<B>          bs          = new Endpoints<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<P>          ps          = new Endpoints<P>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Label>      labels      = new Endpoints<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Textarea>   textareas   = new Endpoints<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Input>      inputs      = new Endpoints<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Address>    addresss    = new Endpoints<Address>("addresss", Address.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Br>         brs         = new Endpoints<Br>("brs", Br.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Hr>         hrs         = new Endpoints<Hr>("hrs", Hr.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<A>          as          = new Endpoints<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H1>         h1s         = new Endpoints<H1>("h1s", H1.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H2>         h2s         = new Endpoints<H2>("h2s", H2.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H3>         h3s         = new Endpoints<H3>("h3s", H3.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H4>         h4s         = new Endpoints<H4>("h4s", H4.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H5>         h5s         = new Endpoints<H5>("h5s", H5.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<H6>         h6s         = new Endpoints<H6>("h6s", H6.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Img>        imgs        = new Endpoints<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Ul>         uls         = new Endpoints<Ul>("uls", Ul.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Ol>         ols         = new Endpoints<Ol>("ols", Ol.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Form>       forms       = new Endpoints<Form>("forms", Form.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Script>     scripts     = new Endpoints<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
