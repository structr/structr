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
import org.structr.core.property.EndNodes;
import org.structr.web.entity.dom.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Span extends DOMElement {

	public static final EndNodes<Content> contents = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Span>    spans    = new EndNodes<Span>("spans", Span.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<B>       bs       = new EndNodes<B>("bs", B.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<P>       ps       = new EndNodes<P>("ps", P.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Label>   labels   = new EndNodes<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Input>   inputs   = new EndNodes<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Address> addresss = new EndNodes<Address>("addresss", Address.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Footer>  footers  = new EndNodes<Footer>("footers", Footer.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<A>       as       = new EndNodes<A>("as", A.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Img>     imgs     = new EndNodes<Img>("imgs", Img.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Script>  scripts  = new EndNodes<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}
}
