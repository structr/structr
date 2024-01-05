/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.memgraph;

import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;

import java.util.function.Function;

/**
 *
 */
class MixedResultWrapper<S, T> implements Function<S, T> {

	protected MemgraphDatabaseService db = null;

	public MixedResultWrapper(final MemgraphDatabaseService db) {
		this.db = db;
	}

	@Override
	public T apply(final S from) throws RuntimeException {

		if (from instanceof Node) {

			return (T)NodeWrapper.newInstance(db, (Node)from);
		}

		if (from instanceof Relationship) {

			return (T)RelationshipWrapper.newInstance(db, (Relationship)from);
		}

		if (from instanceof Path) {

			return (T)new PathWrapper(db, (Path)from);
		}

		return (T)from;
	}

}
