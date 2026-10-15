/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import java.util.Set;

/**
 *
 */
public interface Node<T> extends PropertyContainer<T> {

	Relationship<T> createRelationshipTo(final Node<T> endNode, final RelationshipType relationshipType);
	Relationship<T> createRelationshipTo(final Node<T> endNode, final RelationshipType relationshipType, final Map<String, Object> properties);

	void addLabels(final Set<String> label);
	void removeLabel(final String label);

	Iterable<String> getLabels();

	boolean hasRelationshipTo(final RelationshipType relationshipType, final Node<T> targetNode);
	Relationship<T> getRelationshipTo(final RelationshipType relationshipType, final Node<T> targetNode);

	Iterable<Relationship<T>> getRelationships();
	Iterable<Relationship<T>> getRelationships(final Direction direction);
	Iterable<Relationship<T>> getRelationships(final Direction direction, final RelationshipType relationshipType);

	Map<String, Long> getDegree();

	/**
	 * Locks this node exclusively until the current transaction ends. A transaction that checks the relationships
	 * of a node before it changes them must lock the node first, otherwise a concurrent transaction can change the
	 * relationships between the check and the change. Relationships of this node read in the current transaction
	 * before the lock are read again afterwards.
	 */
	void acquireWriteLock();
	//Map<String, Object> getCache();
}
