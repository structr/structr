/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.neo4j.wrapper;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.structr.api.graph.RelationshipType;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class RelationshipTypeWrapper implements RelationshipType {

	private org.neo4j.graphdb.RelationshipType relType = null;

	public RelationshipTypeWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.RelationshipType relType) {
		this.relType = relType;
	}

	@Override
	public int hashCode() {
		return relType.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof RelationshipType) {
			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public String name() {
		return relType.name();
	}

	// ----- helper methods -----
	public org.neo4j.graphdb.RelationshipType unwrap() {
		return relType;
	}

	public org.neo4j.graphdb.RelationshipType unwrap(final RelationshipType relationshipType) {

		if (relationshipType instanceof RelationshipTypeWrapper) {

			return ((RelationshipTypeWrapper)relationshipType).unwrap();
		}

		return DynamicRelationshipType.withName(relationshipType.name());
	}
}
