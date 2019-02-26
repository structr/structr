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

import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;
import org.structr.bolt.mapper.RelationshipRelationshipMapper;

/**
 *
 */
public class CypherRelationshipIndex extends AbstractCypherIndex<Relationship> {

	public CypherRelationshipIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix(final String typeLabel, final String sourceTypeLabel, final String targetTypeLabel) {

		final StringBuilder buf       = new StringBuilder();
		final String tenantIdentifier = db.getTenantIdentifier();

		buf.append("MATCH (");

		if (tenantIdentifier != null) {
			buf.append(":");
			buf.append(tenantIdentifier);
		}

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
	public Iterable<Relationship> getResult(final AdvancedCypherQuery query) {

		final SessionTransaction tx = db.getCurrentTransaction();

		tx.setIsPing(query.getQueryContext().isPing());

		return Iterables.map(new RelationshipRelationshipMapper(db), tx.getRelationships(query.getStatement(), query.getParameters()));
	}
}
