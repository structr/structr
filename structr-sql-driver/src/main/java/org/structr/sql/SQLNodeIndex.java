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
package org.structr.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;

/**
 */
public class SQLNodeIndex extends AbstractSQLIndex<Node> {

	public SQLNodeIndex(final SQLDatabaseService db) {
		super(db);
	}

	@Override
	public String getQueryPrefix(final String mainType, final String sourceTypeLabel, final String targetTypeLabel) {
		return null;
	}

	@Override
	public String getQuerySuffix(final SQLQuery query) {
		return "";
	}

	@Override
	public Iterable<Node> getResult(final SQLQuery query) {

		try {
			final SQLTransaction tx      = db.getCurrentTransaction();
			final List<Object> params    = query.getParameters();
			final String sql             = query.getStatement();
			final PreparedStatement stm  = tx.prepareStatement(sql);
			int index                    = 1;

			for (final Object value : params) {

				stm.setObject(index++, value);
			}

			System.out.println(sql + ": " + params);

			return Iterables.map(r -> SQLNode.newInstance(db, r), new NodeResultStream(db, stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}
}
