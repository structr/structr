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


//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.neo4j.graphdb.Relationship;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.node.NodeService.RelationshipIndex;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class GenericRelationship extends AbstractRelationship {

	private static final Logger logger = Logger.getLogger(GenericRelationship.class.getName());


	static {

		EntityContext.registerPropertySet(GenericRelationship.class, PropertyView.All, Key.values());

		EntityContext.registerSearchablePropertySet(GenericRelationship.class, RelationshipIndex.rel_uuid.name(), Key.uuid);
	}


	//~--- constructors ---------------------------------------------------

	static {
		EntityContext.registerPropertySet(GenericRelationship.class, PropertyView.All,		Key.values());

	}

	public GenericRelationship() {}

	public GenericRelationship(SecurityContext securityContext, Relationship dbRelationship) {
		init(securityContext, dbRelationship);
	}

	@Override
	public PropertyKey getStartNodeIdKey() {
		return null;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return null;
	}
}
