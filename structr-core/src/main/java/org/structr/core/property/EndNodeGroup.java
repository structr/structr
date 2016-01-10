/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 * A property group that returns grouped properties from the end node of a relationship.
 *
 *
 */
public class EndNodeGroup extends GroupProperty {
	
	public EndNodeGroup(String name, Class<? extends GraphObject> entityClass, PropertyKey... properties) {
		super(name, entityClass, properties);
	}

	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		if(source instanceof RelationshipInterface) {

			RelationshipInterface rel = (RelationshipInterface)source;
			NodeInterface end         = rel.getTargetNode();

			return super.getGroupedProperties(securityContext, end);
		}

		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
