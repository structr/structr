/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Iterator;
import org.structr.api.QueryResult;
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
	public String getQueryPrefix(final String typeLabel) {

		if (typeLabel != null) {

			return "MATCH ()-[n: " + typeLabel + "]-()";

		} else {

			return "MATCH ()-[n]-()";

		}
	}

	@Override
	public String getQuerySuffix() {
		return " RETURN DISTINCT n";
	}

	@Override
	public QueryResult<Relationship> getResult(final CypherQuery context) {

		final SessionTransaction tx                 = db.getCurrentTransaction();
		final RelationshipRelationshipMapper mapper = new RelationshipRelationshipMapper(db);
		final Iterable<Relationship> mapped         = Iterables.map(mapper, tx.getRelationships(context.getStatement(), context.getParameters()));

		return new QueryResult<Relationship>() {

			@Override
			public void close() {
			}

			@Override
			public Iterator<Relationship> iterator() {
				return mapped.iterator();
			}
		};
	}
}
