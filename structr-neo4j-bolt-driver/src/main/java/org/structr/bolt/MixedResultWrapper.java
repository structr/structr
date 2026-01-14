/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.bolt;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

import java.util.function.Function;

/**
 *
 */
class MixedResultWrapper<S, T> implements Function<S, T> {

	protected BoltDatabaseService db = null;

	public MixedResultWrapper(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public T apply(final S from) throws RuntimeException {

		if (from instanceof Node node) {

			return (T)db.getCurrentTransaction().getNodeWrapper(node);
		}

		if (from instanceof Relationship rel) {

			return (T)db.getCurrentTransaction().getRelationshipWrapper(rel);
		}

		if (from instanceof Path path) {

			return (T)new PathWrapper(db, path);
		}

		return (T)from;
	}

}
