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

import java.util.function.Function;
import org.structr.neo4j.Neo4jDatabaseService;

/**
 *
 */
public class MixedResultWrapper<S, T> implements Function<S, T> {

	protected Neo4jDatabaseService graphDb = null;

	public MixedResultWrapper(final Neo4jDatabaseService graphDb) {
		this.graphDb = graphDb;
	}

	@Override
	public T apply(final S from) throws RuntimeException {

		if (from instanceof org.neo4j.graphdb.Node) {

			return (T)NodeWrapper.getWrapper(graphDb, (org.neo4j.graphdb.Node)from);
		}

		if (from instanceof org.neo4j.graphdb.Relationship) {

			return (T)RelationshipWrapper.getWrapper(graphDb, (org.neo4j.graphdb.Relationship)from);
		}

		if (from instanceof org.neo4j.graphdb.Path) {

			return (T)new PathWrapper(graphDb, (org.neo4j.graphdb.Path)from);
		}

		return (T)from;
	}

}
