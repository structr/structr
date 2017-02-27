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
package org.structr.neo4j.index;

import java.util.Iterator;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.QueryResult;
import org.structr.api.util.Iterables;
import org.structr.neo4j.Neo4jDatabaseService;
import org.structr.neo4j.wrapper.MixedResultWrapper;

/**
 *
 */
public class IndexHitsWrapper<S extends org.neo4j.graphdb.PropertyContainer, T extends PropertyContainer> implements QueryResult<T> {

	private org.neo4j.graphdb.index.IndexHits<S> indexHits = null;
	private Neo4jDatabaseService graphDb                   = null;

	public IndexHitsWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.index.IndexHits<S> indexHits) {
		this.graphDb   = graphDb;
		this.indexHits = indexHits;
	}

	@Override
	public void close() {
		indexHits.close();
	}

	@Override
	public Iterator<T> iterator() {
		return Iterables.map(new MixedResultWrapper<>(graphDb), (Iterator<S>)indexHits);
	}
}
