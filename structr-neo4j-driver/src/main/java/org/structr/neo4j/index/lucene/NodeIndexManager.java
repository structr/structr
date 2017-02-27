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
package org.structr.neo4j.index.lucene;

import java.util.HashMap;
import java.util.Map;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.structr.api.graph.Node;
import org.structr.api.index.Index;
import org.structr.api.index.IndexManager;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class NodeIndexManager implements IndexManager<Node> {

	private LuceneIndexWrapper<org.neo4j.graphdb.Node, Node> fulltextIndex = null;
	private LuceneIndexWrapper<org.neo4j.graphdb.Node, Node> keywordIndex  = null;
	private SpatialIndexWrapper<org.neo4j.graphdb.Node, Node> spatialIndex = null;
	private Neo4jDatabaseService graphDb                                   = null;

	public NodeIndexManager(final Neo4jDatabaseService graphDb) {
		this.graphDb = graphDb;
	}

	@Override
	public Index<Node> fulltext() {

		if (fulltextIndex == null) {

			fulltextIndex = new LuceneIndexWrapper<>(graphDb, graphDb.getGraphDb().index().forNodes("fulltextAllNodes", LuceneIndexImplementation.FULLTEXT_CONFIG));
		}

		return fulltextIndex;
	}

	@Override
	public Index<Node> exact() {

		if (keywordIndex == null) {

			keywordIndex = new LuceneIndexWrapper<>(graphDb, graphDb.getGraphDb().index().forNodes("keywordAllNodes", LuceneIndexImplementation.EXACT_CONFIG));
		}

		return keywordIndex;
	}

	@Override
	public Index<Node> spatial() {

		if (spatialIndex == null) {

			final Map<String, String> spatialConfig = new HashMap<>();

			spatialConfig.put(LayerNodeIndex.LAT_PROPERTY_KEY, "latitude");
			spatialConfig.put(LayerNodeIndex.LON_PROPERTY_KEY, "longitude");
			spatialConfig.put(SpatialIndexProvider.GEOMETRY_TYPE, LayerNodeIndex.POINT_PARAMETER);

			spatialIndex = new SpatialIndexWrapper<>(graphDb, new LayerNodeIndex("layerIndex", graphDb.getGraphDb(), spatialConfig));
		}

		return spatialIndex;
	}
}
