/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.api.graph.Relationship;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;

/**
 *
 */
class CypherRelationshipIndex extends AbstractCypherIndex<Relationship> {

	public CypherRelationshipIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix(final String typeLabel, final AdvancedCypherQuery query) {

		final StringBuilder buf       = new StringBuilder();
		final String tenantIdentifier = db.getTenantIdentifier();

		buf.append("MATCH (");

		if (tenantIdentifier != null) {
			buf.append(":");
			buf.append(tenantIdentifier);
		}

		final String sourceTypeLabel = query.getSourceType();
		if (sourceTypeLabel != null) {
			buf.append(":");
			buf.append(sourceTypeLabel);
		}

		buf.append(")-[n");

		if (typeLabel != null) {
			buf.append(":");
			buf.append(typeLabel);
		}

		buf.append("]->(");

		if (tenantIdentifier != null) {
			buf.append(":");
			buf.append(tenantIdentifier);
		}

		final String targetTypeLabel = query.getTargetType();
		if (targetTypeLabel != null) {
			buf.append(":");
			buf.append(targetTypeLabel);
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
	public Iterable<Relationship> getResult(final CypherQuery query) {
		return Iterables.map(new RelationshipRelationshipMapper(db), Iterables.map(new RecordRelationshipMapper(db), new LazyRecordIterable(db, query)));
	}
}
