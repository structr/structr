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

import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.structr.api.UnknownClientException;
import org.structr.api.graph.Node;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
	public Map<Node, Double> fulltextQuery(final String indexName, final String searchString) {

		final String tenantIdentifier        = db.getTenantIdentifier();
		final Map<String, Object> parameters = new LinkedHashMap<>();
		final SessionTransaction tx          = db.getCurrentTransaction();
		final String statement;

		// check if index exists first
		if (Iterables.toList(tx.run(new SimpleCypherQuery("SHOW FULLTEXT INDEXES WHERE name = $indexName", Map.of("indexName", indexName)))).isEmpty()) {

			throw new UnknownClientException(null, null, "Index \"" + indexName + "\" does not exist.");
		}

		parameters.put("indexName", indexName);
		parameters.put("searchValue", searchString);

		if (StringUtils.isNotBlank(tenantIdentifier)) {

			statement = "CALL db.index.fulltext.queryNodes($indexName, $searchValue) YIELD node, score WHERE ANY (l in labels(node) WHERE l = $tenantIdentifier) RETURN node, score";

			parameters.put("tenantIdentifier", tenantIdentifier);

		} else {

			statement = "CALL db.index.fulltext.queryNodes($indexName, $searchValue) YIELD node, score RETURN node, score";
		}

		final SimpleCypherQuery query              = new SimpleCypherQuery(statement, parameters);
		final Iterable<Map<String, Object>> result = tx.run(query);
		final Map<Node, Double> nodes              = new LinkedHashMap<>();

		for (final Map<String, Object> entry : result) {

			final Node node    = (Node) entry.get("node");
			final Double score = (Double) entry.get("score");

			nodes.put(node, score);
		}

		return nodes;
	}

	@Override
	public Iterable<Node> getResult(final CypherQuery query) {

		try {

			return db.getCurrentTransaction().getCachedResult(query);

		} catch (ClientException e) {
			ReactiveSessionTransaction.translateClientException(e);
		} catch (DatabaseException d) {
			ReactiveSessionTransaction.translateDatabaseException(d);
		}

		return Collections.EMPTY_LIST;
	}
}



























