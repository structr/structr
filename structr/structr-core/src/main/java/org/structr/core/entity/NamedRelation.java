/*
 *  Copyright (C) 2012 Axel Morgner
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class NamedRelation {

	private RelationshipType relType = null;
	private Class sourceType = null;
	private Class destType = null;
	private String name = null;

	public NamedRelation(String name, Class sourceType, Class destType, RelationshipType relType) {
		this.sourceType = sourceType;
		this.destType = destType;
		this.relType = relType;
		this.name = name;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public void setRelType(RelationshipType relType) {
		this.relType = relType;
	}

	public Class getSourceType() {
		return sourceType;
	}

	public void setSourceType(Class sourceType) {
		this.sourceType = sourceType;
	}

	public Class getDestType() {
		return destType;
	}

	public void setDestType(Class destType) {
		this.destType = destType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<? extends GraphObject> getRelationships(GraphObject obj) throws FrameworkException {
		return obj.getRelationships(relType, getDirectionForType(obj.getClass()));
	}

	// ----- private methods -----
	private Direction getDirectionForType(Class type) {

		if(type.equals(sourceType)) {
			return Direction.OUTGOING;
		}

		if(type.equals(destType)) {
			return Direction.INCOMING;
		}

		return null;
	}
}
