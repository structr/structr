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
 * Returns the "type" property of the end node when evaluated.
 *
 *
 */
public class RelationshipEndNodeTypeConverter extends PropertyConverter<Object, String> {

	public RelationshipEndNodeTypeConverter(SecurityContext securityContext, GraphObject entity) {
		super(securityContext, entity);
	}

	@Override
	public Object revert(String source) {

		if (currentObject instanceof AbstractRelationship) {

			final AbstractRelationship rel = (AbstractRelationship) currentObject;
			final NodeInterface endNode    = rel.getTargetNode();

			if (endNode != null) {

				return endNode.getType();
			}
		}

		return null;
	}

	@Override
	public String convert(Object source) {
		return source != null ? source.toString() : null;
	}
}
