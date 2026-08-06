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
package org.structr.embedded;

import org.structr.api.graph.Node;

import java.util.Map;
import java.util.function.Function;

/**
 *
 */
class PrefetchNodeMapper implements Function<Map<String, Object>, Node<String>> {

	private EmbeddedDatabaseService db = null;

	public PrefetchNodeMapper(final EmbeddedDatabaseService db) {

		this.db = db;
	}

	@Override
	public Node<String> apply(final Map<String, Object> record) {

		final EmbeddedTransaction tx   = db.getCurrentTransaction();
		final Object              node = record.get("n");
		final NodeWrapper wrapper   = tx.getNodeWrapper((org.neo4j.graphdb.Node) node);

		// Create NodeWrapper instances if a prefetch query returns additional nodes.
		final Object nodesValue = record.get("nodes");
		if (nodesValue instanceof Iterable iterable) {

			for (final Object o : iterable) {

				tx.getNodeWrapper((org.neo4j.graphdb.Node) o);
			}
		}

		// Create RelationshipWrapper instances if a prefetch query returns additional relationship
		// and store them in the cache of our newly created NodeWrapper. This is the actual prefetch
		// operation.
		final Object relsValue  = record.get("rels");
		if (relsValue instanceof Iterable iterable) {

			for (final Object r : iterable) {

				tx.getRelationshipWrapper((org.neo4j.graphdb.Relationship) r);
			}
		}

		return wrapper;
	}
}
