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

package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

/**
 * A relationship with a direction.
 *
 * @author Christian Morgner
 */
public class DirectedRelationship {

	private RelationshipType relType =  null;
	private Direction direction = null;

	public DirectedRelationship(RelationshipType relType, Direction direction) {

		this.direction = direction;
		this.relType = relType;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

}
