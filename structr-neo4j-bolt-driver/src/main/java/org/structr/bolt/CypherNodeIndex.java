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
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;

/**
 *
 */
class CypherNodeIndex extends AbstractCypherIndex<Node> {

	public CypherNodeIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix(final String typeLabel, final AdvancedCypherQuery query) {

		final StringBuilder buf = new StringBuilder();

		if (query.getHasOptionalParts()) {

			buf.append("OPTIONAL ");
		}

		buf.append("MATCH (n");

		// Only add :NodeInterface label when query has predicates, single label queries are much faster.
		if (query.hasPredicates()) {
			buf.append(":NodeInterface");
		}

		final String tenantId = db.getTenantIdentifier();

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

		buf.append(" RETURN DISTINCT n");

		final SortOrder sortOrder = query.getSortOrder();
		if (sortOrder != null) {

			int sortSpecIndex = 0;

			for (final SortSpec spec : sortOrder.getSortElements()) {

				final String sortKey = spec.getSortKey();
				if (sortKey != null) {

					buf.append(", n.`");
					buf.append(sortKey);
					buf.append("` AS sortKey");
					buf.append(sortSpecIndex);
				}

				sortSpecIndex++;
			}
		}

		return buf.toString();
	}

	@Override
	public Iterable<Node> getResult(final CypherQuery query) {
		return db.getCurrentTransaction().getCachedResult(query);
	}
}