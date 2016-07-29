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

import java.util.Iterator;
import java.util.Map;
import org.structr.api.NativeResult;
import org.structr.api.util.Iterables;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class Neo4jResultWrapper<T> implements NativeResult<T> {

	private Neo4jDatabaseService graphDb    = null;
	private org.neo4j.graphdb.Result result = null;

	public Neo4jResultWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Result result) {
		this.graphDb = graphDb;
		this.result  = result;
	}

	@Override
	public Iterator<T> columnAs(String name) {
		return Iterables.map(new MixedResultWrapper<>(graphDb), result.columnAs(name));
	}

	@Override
	public boolean hasNext() {
		return result.hasNext();
	}

	@Override
	public Map<String, Object> next() {
		return new MapResultWrapper(graphDb, result.next());
	}

	@Override
	public void close() {
		result.close();
	}
}
