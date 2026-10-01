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
package org.structr.embedded;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 * The rows of a query that ran in a transaction other than the calling thread's current one.
 *
 * Neo4j entities are bound to the transaction that produced them, and a result is only computed while
 * its transaction is open. So the rows are read completely on construction, with every node and
 * relationship reduced to its element id, and the entities are looked up again in the thread's current
 * transaction when the rows are iterated. By then the producing transaction has usually committed, so
 * its writes are visible. A row referring to an entity that no longer exists, because the query
 * deleted it, is skipped.
 */
class DetachedResult implements Iterable<Map<String, Object>> {

	private final List<Map<String, Object>> rows = new LinkedList<>();
	private final EmbeddedDatabaseService db;

	public DetachedResult(final EmbeddedDatabaseService db, final Result result) {

		this.db = db;

		try (result) {

			while (result.hasNext()) {

				rows.add(detach(result.next()));
			}
		}
	}

	@Override
	public Iterator<Map<String, Object>> iterator() {

		return Iterables.filter(Objects::nonNull, Iterables.map(this::attachRow, rows)).iterator();
	}

	// ----- private methods -----
	private Map<String, Object> detach(final Map<String, Object> map) {

		final Map<String, Object> detached = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {

			detached.put(entry.getKey(), detach(entry.getValue()));
		}

		return detached;
	}

	private Object detach(final Object value) {

		if (value instanceof Node node) {

			return new NodeReference(node.getElementId());
		}

		if (value instanceof Relationship relationship) {

			return new RelationshipReference(relationship.getElementId());
		}

		if (value instanceof org.neo4j.graphdb.Path path) {

			final List<Object> elements = new ArrayList<>();

			for (final Entity entity : path) {

				elements.add(detach(entity));
			}

			return new PathReference(elements);
		}

		if (value instanceof Map map) {

			return detach((Map<String, Object>) map);
		}

		if (value instanceof Collection<?> collection) {

			final List<Object> list = new ArrayList<>(collection.size());

			for (final Object element : collection) {

				list.add(detach(element));
			}

			return list;
		}

		return value;
	}

	private Map<String, Object> attachRow(final Map<String, Object> row) {

		try {

			return attach(row);

		} catch (NotFoundException nfe) {

			return null;
		}
	}

	private Map<String, Object> attach(final Map<String, Object> map) {

		final Map<String, Object> attached = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {

			attached.put(entry.getKey(), attach(entry.getValue()));
		}

		return attached;
	}

	private Object attach(final Object value) {

		// existence is checked against the transaction, not its wrapper cache, which can still hold an
		// entity that the producing transaction has deleted
		if (value instanceof NodeReference(final String id)) {

			final EmbeddedTransaction tx = db.getCurrentTransaction();

			if (!tx.nodeExists(id)) {

				throw new NotFoundException("Node with ID " + id + " not found.");
			}

			return tx.getNodeWrapper(id);
		}

		if (value instanceof RelationshipReference(final String id)) {

			final EmbeddedTransaction tx = db.getCurrentTransaction();

			if (!tx.relationshipExists(id)) {

				throw new NotFoundException("Relationship with ID " + id + " not found.");
			}

			return tx.getRelationshipWrapper(id);
		}

		if (value instanceof PathReference(final List<Object> elements)) {

			final List<PropertyContainer> attached = new ArrayList<>(elements.size());

			for (final Object element : elements) {

				attached.add((PropertyContainer) attach(element));
			}

			return (Path) attached::iterator;
		}

		if (value instanceof Map map) {

			return attach((Map<String, Object>) map);
		}

		if (value instanceof List<?> list) {

			final List<Object> attached = new ArrayList<>(list.size());

			for (final Object element : list) {

				attached.add(attach(element));
			}

			return attached;
		}

		return value;
	}

	// ----- nested classes -----
	private record NodeReference(String id) {}
	private record RelationshipReference(String id) {}
	private record PathReference(List<Object> elements) {}
}
