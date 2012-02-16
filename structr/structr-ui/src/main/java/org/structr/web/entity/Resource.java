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
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelation.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 * Represents an arbitrary element
 *
 * @author axel
 */
public class Resource extends AbstractNode {

	public enum Key implements PropertyKey {
		name, tag, elements
	}

	static {

		EntityContext.registerPropertySet(Resource.class,	PropertyView.All,	Key.values());
		EntityContext.registerPropertySet(Resource.class,	PropertyView.Public,	Key.values());
		EntityContext.registerPropertySet(Resource.class,	"ui",			Key.values());
		
		EntityContext.registerEntityRelation(Resource.class,	Element.class,	RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}
}
