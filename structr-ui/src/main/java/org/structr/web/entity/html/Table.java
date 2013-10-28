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

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Table extends DOMElement {

	public static final EndNodes<Tr>    trs    = new EndNodes<Tr>("trs", Tr.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Thead> theads = new EndNodes<Thead>("theads", Thead.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EndNodes<Tbody> tbodys = new EndNodes<Tbody>("tbodys", Tbody.class, RelType.CONTAINS, Direction.OUTGOING, false);
}
