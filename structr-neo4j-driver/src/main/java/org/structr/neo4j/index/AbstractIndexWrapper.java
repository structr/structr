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
package org.structr.neo4j.index;

import org.structr.api.graph.PropertyContainer;
import org.structr.api.QueryResult;
import org.structr.api.index.Index;
import org.structr.neo4j.Neo4jDatabaseService;
import org.structr.neo4j.wrapper.NodeWrapper;
import org.structr.neo4j.wrapper.RelationshipWrapper;

/**
 *
 */
public abstract class AbstractIndexWrapper<S extends org.neo4j.graphdb.PropertyContainer, T extends PropertyContainer> implements Index<T> {

	protected Neo4jDatabaseService graphDb           = null;
	protected org.neo4j.graphdb.index.Index<S> index = null;

	protected abstract Object convertForIndexing(final Object source, final Class typeHint);
	protected abstract Object convertForQuerying(final Object source, final Class typeHint);

	public AbstractIndexWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.index.Index<S> index) {
		this.graphDb = graphDb;
		this.index   = index;
	}

	@Override
	public void add(final T t, final String key, final Object value, final Class typeHint) {
		index.add(unwrap(t), key, convertForIndexing(value, typeHint));
	}

	@Override
	public void remove(final T t) {
		index.remove(unwrap(t));
	}

	@Override
	public void remove(final T t, final String key) {
		index.remove(unwrap(t), key);
	}

	@Override
	public QueryResult<T> query(final String key, final Object value, final Class typeHint) {
		return new IndexHitsWrapper<>(graphDb, index.query(key, convertForQuerying(value, typeHint)));
	}

	@Override
	public QueryResult<T> get(final String key, final Object value, final Class typeHint) {
		return new IndexHitsWrapper<>(graphDb, index.get(key, convertForQuerying(value, typeHint)));
	}

	// ----- protected methods -----
	protected S unwrap(final T propertyContainer) {

		if (propertyContainer instanceof NodeWrapper) {
			return (S)((NodeWrapper)propertyContainer).unwrap();
		}

		if (propertyContainer instanceof RelationshipWrapper) {
			return (S)((RelationshipWrapper)propertyContainer).unwrap();
		}

		return null;
	}

}
