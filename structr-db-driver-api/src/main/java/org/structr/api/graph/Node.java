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
package org.structr.api.graph;

import java.util.Map;

/**
 *
 */
public interface Node extends PropertyContainer {

	Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType);
	Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties);

	void addLabel(final String label);
	void removeLabel(final String label);

	Iterable<String> getLabels();

	boolean hasRelationshipTo(final RelationshipType relationshipType, final Node targetNode);
	Relationship getRelationshipTo(final RelationshipType relationshipType, final Node targetNode);

	Iterable<Relationship> getRelationships();
	Iterable<Relationship> getRelationships(final Direction direction);
	Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType relationshipType);
}
