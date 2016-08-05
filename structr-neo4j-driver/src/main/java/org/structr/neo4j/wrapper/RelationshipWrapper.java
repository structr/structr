/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.gis.spatial.SpatialRelationshipTypes;
import org.neo4j.gis.spatial.rtree.RTreeRelationshipTypes;
import org.structr.api.graph.Node;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class RelationshipWrapper extends EntityWrapper<org.neo4j.graphdb.Relationship> implements Relationship {

	private static final Set<String> SpatialRelationshipTypeNames = new HashSet<>(

		Arrays.asList(new String[] {

			RTreeRelationshipTypes.RTREE_CHILD.name(),
			RTreeRelationshipTypes.RTREE_METADATA.name(),
			RTreeRelationshipTypes.RTREE_REFERENCE.name(),
			RTreeRelationshipTypes.RTREE_ROOT.name(),
			
			SpatialRelationshipTypes.DATASET.name(),
			SpatialRelationshipTypes.DATASETS.name(),
			SpatialRelationshipTypes.LAYER.name(),
			SpatialRelationshipTypes.LAYERS.name(),
			SpatialRelationshipTypes.LAYER_CONFIG.name(),
			SpatialRelationshipTypes.NETWORK.name(),
			SpatialRelationshipTypes.PROPERTY_MAPPING.name(),
			SpatialRelationshipTypes.SPATIAL.name()
			
		})
	);

	private RelationshipWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Relationship relationship) {
		super(graphDb, relationship);
	}

	@Override
	public String toString() {
		return "RelationshipWrapper(" + entity.getId() + ", " + entity.getType().name() + ")";
	}

	@Override
	public int hashCode() {
		return entity.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof Relationship) {
			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public Node getStartNode() {

		try {

			return NodeWrapper.getWrapper(graphDb, entity.getStartNode());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public Node getEndNode() {

		try {

			return NodeWrapper.getWrapper(graphDb, entity.getEndNode());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public Node getOtherNode(final Node node) {

		try {
			return NodeWrapper.getWrapper(graphDb, entity.getOtherNode(unwrap(node)));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public RelationshipType getType() {

		try {

			return new RelationshipTypeWrapper(graphDb, entity.getType());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public long getId() {
		return entity.getId();
	}

	@Override
	public void delete() throws NotInTransactionException {

		try {


			final TransactionWrapper tx = TransactionWrapper.getCurrentTransaction();
			final NodeWrapper startNode = NodeWrapper.getWrapper(graphDb, entity.getStartNode());
			final NodeWrapper endNode   = NodeWrapper.getWrapper(graphDb, entity.getEndNode());

			tx.registerModified(startNode);
			tx.registerModified(endNode);
			tx.registerModified(this);

			entity.delete();
			deleted = true;

			// remove deleted relationship from cache
			graphDb.removeRelationshipFromCache(getId());
			startNode.clearCaches();
			endNode.clearCaches();

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public boolean isSpatialEntity() {
		return SpatialRelationshipTypeNames.contains(entity.getType().name());
	}

	// ----- helper methods -----
	public org.neo4j.graphdb.Relationship unwrap() {
		return entity;
	}

	// ----- public static methods -----
	public static RelationshipWrapper getWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Relationship relationship) {

		RelationshipWrapper wrapper = graphDb.getRelationshipFromCache(relationship.getId());
		if (wrapper == null) {

			wrapper = new RelationshipWrapper(graphDb, relationship);
			graphDb.storeRelationshipInCache(wrapper);
		}

		return wrapper;
	}
}
