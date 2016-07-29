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
import org.neo4j.function.Function;
import org.neo4j.helpers.collection.Iterables;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class PathWrapper implements Path {

	protected Neo4jDatabaseService graphDb = null;
	private org.neo4j.graphdb.Path source  = null;

	public PathWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.Path source) {
		this.graphDb = graphDb;
		this.source  = source;
	}


	@Override
	public Iterator<PropertyContainer> iterator() {

		final MixedResultWrapper<org.neo4j.graphdb.PropertyContainer, PropertyContainer> wrapper = new MixedResultWrapper<>(graphDb);

		return Iterables.map(new Function<org.neo4j.graphdb.PropertyContainer, PropertyContainer>() {

			@Override
			public PropertyContainer apply(final org.neo4j.graphdb.PropertyContainer from) throws RuntimeException {
			
				return wrapper.apply(from);
			}

		}, source.iterator());
	}
}
