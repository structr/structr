/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.converter;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;

/**
 * Returns the "type" property of the start node when evaluated.
 *
 *
 */
public class RelationshipStartNodeTypeConverter extends PropertyConverter {

	public RelationshipStartNodeTypeConverter(SecurityContext securityContext, GraphObject entity) {
		super(securityContext, entity);
	}

	@Override
	public Object revert(Object source) {

		if(currentObject instanceof AbstractRelationship) {

			final AbstractRelationship rel = (AbstractRelationship)currentObject;
			final NodeInterface startNode = rel.getSourceNode();

			if(startNode != null) {

				return startNode.getType();
			}
		}

		return null;
	}

	@Override
	public Object convert(Object source) {
		return null;
	}
}
