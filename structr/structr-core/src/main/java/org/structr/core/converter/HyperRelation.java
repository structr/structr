/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

package org.structr.core.converter;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class HyperRelation implements Value<HyperRelation> {
	
	private PropertyKey parentIdKey		= null;
	private PropertyKey childIdKey		= null;
	private PropertyKey refIdKey		= null;
	private Direction direction		= null;
	private RelationshipType relType	= null;
	private Notion notion			= null;
	private Class entity			= null;
	
	public HyperRelation(Class entity, RelationshipType relType, Direction direction, Notion notion) {
		
		this.entity = entity;
		this.relType = relType;
		this.direction = direction;
		this.notion = notion;
	}

	public PropertyKey getParentIdKey() {
		return parentIdKey;
	}

	public PropertyKey getChildIdKey() {
		return childIdKey;
	}

	public PropertyKey getRefIdKey() {
		return refIdKey;
	}

	// ----- interface value -----
	@Override
	public void set(SecurityContext securityContext, HyperRelation value) {
	}

	@Override
	public HyperRelation get(SecurityContext securityContext) {
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public Class getEntity() {
		return entity;
	}

	public Notion getNotion() {
		return notion;
	}
}
