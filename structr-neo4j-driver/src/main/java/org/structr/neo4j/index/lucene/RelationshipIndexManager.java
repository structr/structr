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
package org.structr.neo4j.index.lucene;

import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.index.IndexManager;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class RelationshipIndexManager implements IndexManager<Relationship> {

	private LuceneIndexWrapper<org.neo4j.graphdb.Relationship, Relationship> fulltextIndex = null;
	private LuceneIndexWrapper<org.neo4j.graphdb.Relationship, Relationship> keywordIndex  = null;
	private Neo4jDatabaseService graphDb                                                   = null;

	public RelationshipIndexManager(final Neo4jDatabaseService graphDb) {
		this.graphDb = graphDb;
	}

	@Override
	public Index<Relationship> fulltext() {

		if (fulltextIndex == null) {

			fulltextIndex = new LuceneIndexWrapper<>(graphDb, graphDb.getGraphDb().index().forRelationships("fulltextAllRelationships", LuceneIndexImplementation.FULLTEXT_CONFIG));
		}

		return fulltextIndex;
	}

	@Override
	public Index<Relationship> exact() {

		if (keywordIndex == null) {

			keywordIndex = new LuceneIndexWrapper(graphDb, graphDb.getGraphDb().index().forRelationships("keywordAllRelationships", LuceneIndexImplementation.EXACT_CONFIG));
		}

		return keywordIndex;
	}

	@Override
	public Index<Relationship> spatial() {
		return null;
	}
}
