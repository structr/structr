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
import org.structr.core.property.CollectionProperty;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Td extends DOMElement {

	public static final CollectionProperty<Content>  contents  = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Div>      divs      = new CollectionProperty<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Span>     spans     = new CollectionProperty<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<B>        bs        = new CollectionProperty<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<P>        ps        = new CollectionProperty<P>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Label>    labels    = new CollectionProperty<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Textarea> textareas = new CollectionProperty<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Input>    inputs    = new CollectionProperty<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Address>  addresss  = new CollectionProperty<Address>("addresss", Address.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Footer>   footers   = new CollectionProperty<Footer>("footers", Footer.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Br>       brs       = new CollectionProperty<Br>("brs", Br.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Hr>       hrs       = new CollectionProperty<Hr>("hrs", Hr.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<A>        as        = new CollectionProperty<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H1>       h1s       = new CollectionProperty<H1>("h1s", H1.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H2>       h2s       = new CollectionProperty<H2>("h2s", H2.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H3>       h3s       = new CollectionProperty<H3>("h3s", H3.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H4>       h4s       = new CollectionProperty<H4>("h4s", H4.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H5>       h5s       = new CollectionProperty<H5>("h5s", H5.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<H6>       h6s       = new CollectionProperty<H6>("h6s", H6.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Img>      imgs      = new CollectionProperty<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ul>       uls       = new CollectionProperty<Ul>("uls", Ul.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Ol>       ols       = new CollectionProperty<Ol>("ols", Ol.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Form>     forms     = new CollectionProperty<Form>("forms", Form.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Script>   scripts   = new CollectionProperty<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Pre>      pres      = new CollectionProperty<Pre>("pres", Pre.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
