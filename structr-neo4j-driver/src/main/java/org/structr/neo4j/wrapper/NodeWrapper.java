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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.gis.spatial.rtree.RTreeRelationshipTypes;
import org.structr.api.graph.Direction;
import org.structr.api.util.Iterables;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.neo4j.Neo4jDatabaseService;
import org.structr.neo4j.mapper.LabelMapper;
import org.structr.neo4j.mapper.RelationshipMapper;

/**
 *
 */
public class NodeWrapper extends EntityWrapper<org.neo4j.graphdb.Node> implements Node {

	private final Map<String, Set<Relationship>> relationshipCache = new HashMap<>();

	private NodeWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Node node) {
		super(graphDb, node);
	}

	@Override
	public int hashCode() {
		return entity.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Node) {
			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public Relationship createRelationshipTo(final Node endNode, final RelationshipType relationshipType) {

		try {

			TransactionWrapper.getCurrentTransaction().registerModified(this);

			// clear caches of start and end node
			((NodeWrapper)endNode).relationshipCache.clear();
			relationshipCache.clear();

			return RelationshipWrapper.getWrapper(graphDb, entity.createRelationshipTo(unwrap(endNode), unwrap(relationshipType)));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public void addLabel(final Label label) {

		try {
			TransactionWrapper.getCurrentTransaction().registerModified(this);
			entity.addLabel(unwrap(label));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public void removeLabel(final Label label) {

		try {
			TransactionWrapper.getCurrentTransaction().registerModified(this);
			entity.removeLabel(unwrap(label));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public Iterable<Label> getLabels() {

		try {
			return Iterables.map(new LabelMapper(), entity.getLabels());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public Iterable<Relationship> getRelationships() {

		Set<Relationship> allRelationships = relationshipCache.get("*");
		if (allRelationships == null) {

			try {

				allRelationships = Iterables.toSet(Iterables.map(new RelationshipMapper(graphDb), entity.getRelationships()));
				relationshipCache.put("*", allRelationships);

			} catch (org.neo4j.graphdb.NotInTransactionException t) {

				throw new NotInTransactionException(t);
			}
		}

		return allRelationships;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction) {

		Set<Relationship> relationships = relationshipCache.get(direction.name());
		if (relationships == null) {

			try {

				relationships = Iterables.toSet(Iterables.map(new RelationshipMapper(graphDb), entity.getRelationships(unwrap(direction))));
				relationshipCache.put(direction.name(), relationships);

			} catch (org.neo4j.graphdb.NotInTransactionException t) {

				throw new NotInTransactionException(t);
			}
		}

		return relationships;
	}

	@Override
	public Iterable<Relationship> getRelationships(Direction direction, RelationshipType relationshipType) {

		Set<Relationship> relationships = relationshipCache.get(direction.name() + relationshipType.name());
		if (relationships == null) {

			try {

				relationships = Iterables.toSet(Iterables.map(new RelationshipMapper(graphDb), entity.getRelationships(unwrap(direction), unwrap(relationshipType))));
				relationshipCache.put(direction.name() + relationshipType.name(), relationships);

			} catch (org.neo4j.graphdb.NotInTransactionException t) {

				throw new NotInTransactionException(t);
			}
		}

		return relationships;
	}

	@Override
	public long getId() {
		return entity.getId();
	}

	@Override
	public void delete() throws NotInTransactionException {

		try {

			TransactionWrapper.getCurrentTransaction().registerModified(this);
			entity.delete();
			deleted = true;

			graphDb.removeNodeFromCache(getId());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public boolean isSpatialEntity() {
		return entity.hasRelationship(RTreeRelationshipTypes.values());
	}

	// ----- helper methods -----
	public org.neo4j.graphdb.Node unwrap() {
		return entity;
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		relationshipCache.clear();
	}

	// ----- public static methods -----
	public static NodeWrapper getWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Node node) {

		NodeWrapper wrapper = graphDb.getNodeFromCache(node.getId());
		if (wrapper == null) {

			wrapper = new NodeWrapper(graphDb, node);
			graphDb.storeNodeInCache(wrapper);
		}

		return wrapper;
	}
}
