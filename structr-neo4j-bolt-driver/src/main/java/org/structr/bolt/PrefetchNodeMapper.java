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
package org.structr.bolt;

import org.structr.api.graph.Node;

import java.util.function.Function;
import org.neo4j.driver.Value;

/**
 *
 */
class PrefetchNodeMapper implements Function<org.neo4j.driver.Record, Node> {

	private BoltDatabaseService db = null;

	public PrefetchNodeMapper(final BoltDatabaseService db) {
		this.db = db;
	}

	@Override
	public Node apply(final org.neo4j.driver.Record record) {

		final SessionTransaction tx            = db.getCurrentTransaction();
		final org.neo4j.driver.types.Node node = record.get("n").asNode();
		final NodeWrapper wrapper              = tx.getNodeWrapper(node);

		// Create NodeWrapper instances if a prefetch query returns additional nodes.
		final Value nodesValue = record.get("nodes");
		if (!nodesValue.isNull()) {

			nodesValue.asList(r -> r.asNode()).stream().forEach(n -> tx.getNodeWrapper(n));
		}

		// Create RelationshipWrapper instances if a prefetch query returns additional relationship
		// and store them in the cache of our newly created NodeWrapper. This is the actual prefetch
		// operation.
		final Value relsValue  = record.get("rels");
		if (!relsValue.isNull()) {

			relsValue.asList(r -> r.asRelationship()).stream().forEach(r -> {

				wrapper.storeRelationship(tx.getRelationshipWrapper(r), false);
			});
		}

		return wrapper;
	}
}
