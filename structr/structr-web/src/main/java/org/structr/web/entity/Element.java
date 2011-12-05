/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship.Cardinality;
import org.structr.web.common.RelType;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a web resource which is addressable by a URI
 *
 * @author axel
 */
public class Element extends AbstractNode {

	public enum Key implements PropertyKey {
		name, tag, contents, elements
	}

	static {

		EntityContext.registerPropertySet(Element.class,	PropertyView.All,	Key.values());
		EntityContext.registerPropertySet(Element.class,	PropertyView.Public,	Key.values());

		EntityContext.registerRelation(Element.class,	Key.elements,	Resource.class,	RelType.CONTAINS,	Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerRelation(Element.class,	Key.contents,	Element.class,	RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerRelation(Element.class,	Key.elements,	Content.class,	RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}
}
