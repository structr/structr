/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.DirectedRelationship.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Group extends Principal {

	static {

		EntityContext.registerPropertySet(Group.class, PropertyView.All, Key.values());
		//EntityContext.registerPropertyRelation(Group.class, Key.users,	User.class, RelType.HAS_CHILD, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Group.class, User.class, RelType.HAS_CHILD, Direction.OUTGOING, Cardinality.ManyToMany);
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ users }

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/group.png";
	}
}
