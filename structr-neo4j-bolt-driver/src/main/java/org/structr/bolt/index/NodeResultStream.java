/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Map;
import org.neo4j.driver.v1.types.Node;
import org.structr.api.QueryResult;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;

/**
 */
public class NodeResultStream extends AbstractResultStream<Node> {

	public NodeResultStream(final BoltDatabaseService db, final PageableQuery query) {
		super(db, query);
	}

	@Override
	protected QueryResult<Node> fetchData(final BoltDatabaseService db, final String statement, final Map<String, Object> data) {

		final SessionTransaction tx = db.getCurrentTransaction();
		tx.setIsPing(getQuery().getQueryContext().isPing());
		return tx.getNodes(statement, data);
	}
}
