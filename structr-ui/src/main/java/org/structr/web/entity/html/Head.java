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
public class Head extends DOMElement {

	public static final Endpoints<Html>   htmls   = new Endpoints<Html>("htmls", Html.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final Endpoints<Title>  titles  = new Endpoints<Title>("titles", Title.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Style>  styles  = new Endpoints<Style>("styles", Style.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Script> scripts = new Endpoints<Script>("scripts", Script.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Link>   links   = new Endpoints<Link>("links", Link.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Meta>   metas   = new Endpoints<Meta>("metas", Meta.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final Endpoints<Base>   bases   = new Endpoints<Base>("bases", Base.class, RelType.CONTAINS, Direction.OUTGOING, false);
	
}
