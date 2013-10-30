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
package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 * A property group that returns grouped properties from the start node of a relationship.
 * 
 * @author Christian Morgner
 */
public class StartNodeGroup extends GroupProperty {
	
	public StartNodeGroup(String name, Class<? extends GraphObject> entityClass, PropertyKey... properties) {
		super(name, entityClass, properties);
	}

	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		if(source instanceof AbstractRelationship) {

			RelationshipInterface rel = (RelationshipInterface)source;
			NodeInterface startNode   = rel.getSourceNode();

			return super.getGroupedProperties(securityContext, startNode);
		}

		return null;
	}
	
	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public Integer getSortType() {
		return null;
	}
}
