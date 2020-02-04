/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.bolt.index;

import org.structr.api.graph.Node;
import org.structr.api.search.QueryContext;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.mapper.NodeNodeMapper;
import org.structr.bolt.mapper.RecordNodeMapper;

/**
 *
 */
public class CypherNodeIndex extends AbstractCypherIndex<Node> {

	public CypherNodeIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix(final String typeLabel, final String sourceTypeLabel, final String targetTypeLabel, final boolean hasPredicates) {

		final StringBuilder buf = new StringBuilder("MATCH (n");

		// Only add :NodeInterface label when query has predicates, single label queries are much faster.
		if (hasPredicates) {
			buf.append(":NodeInterface");
		}

		final String tenantId   = db.getTenantIdentifier();

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		if (typeLabel != null) {

			buf.append(":");
			buf.append(typeLabel);
		}

		buf.append(")");

		return buf.toString();
	}

	@Override
	public String getQuerySuffix(final AdvancedCypherQuery query) {

		final StringBuilder buf = new StringBuilder();
		final String sortKey    = query.getSortKey();

		buf.append(" RETURN DISTINCT n");

		if (sortKey != null) {

			buf.append(", n.`");
			buf.append(sortKey);
			buf.append("` AS sortKey");
		}

		return buf.toString();
	}

	@Override
	public Iterable<Node> getResult(final AdvancedCypherQuery query) {

		final IterableQueueingRecordConsumer consumer = new IterableQueueingRecordConsumer(db, query);
		final QueryContext context                    = query.getQueryContext();

		if (context != null && !context.isDeferred()) {
			consumer.start();
		}

		// return mapped result
		return Iterables.map(new NodeNodeMapper(db), Iterables.map(new RecordNodeMapper(), consumer));
	}
}
