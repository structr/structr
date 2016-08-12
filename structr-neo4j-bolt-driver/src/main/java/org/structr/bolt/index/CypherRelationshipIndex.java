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
import java.util.LinkedList;
import java.util.List;
import org.structr.api.QueryResult;
import org.structr.api.graph.Relationship;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.RelationshipWrapper;
import org.structr.bolt.SessionTransaction;

/**
 *
 * @author Christian Morgner
 */
public class CypherRelationshipIndex extends AbstractCypherIndex<Relationship> {

	public CypherRelationshipIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix() {
		return "MATCH ()-[n]-() WHERE ";
	}

	@Override
	public String getQuerySuffix() {
		return " RETURN DISTINCT n";
	}

	@Override
	public QueryResult<Relationship> getResult(final CypherQuery context) {

		final SessionTransaction tx                             = db.getCurrentTransaction();
		final List<org.neo4j.driver.v1.types.Relationship> rels = tx.getRelationshipList(context.getStatement(), context.getParameters());
		final List<Relationship> result                         = new LinkedList<>();

		for (final org.neo4j.driver.v1.types.Relationship rel : rels) {
			result.add(RelationshipWrapper.newInstance(db, rel));
		}

		return new QueryResult<Relationship>() {

			@Override
			public int size() {
				return result.size();
			}

			@Override
			public void close() {
			}

			@Override
			public Iterator<Relationship> iterator() {
				return result.iterator();
			}
		};
	}
}
