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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 */
class IdentityStreamIterator implements Iterator<SQLIdentity> {

	private ResultSet resultSet = null;
	private boolean firstCall   = true;

	public IdentityStreamIterator(final ResultSet resultSet) {

		this.resultSet  = resultSet;
	}

	@Override
	public boolean hasNext() {

		try {

			if (firstCall) {

				return resultSet.isBeforeFirst();
			}

			return !resultSet.isAfterLast();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return false;
	}

	@Override
	public SQLIdentity next() {

		firstCall = false;

		try {
			// fetch data for next result..
			if (resultSet.next()) {

				return SQLIdentity.forId(resultSet.getLong(1));
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		// return last result
		return null;
	}
}
