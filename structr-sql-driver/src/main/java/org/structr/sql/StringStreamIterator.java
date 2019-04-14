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
class StringStreamIterator implements Iterator<String> {

	private ResultSet resultSet = null;
	private boolean firstCall   = true;
	private int columnIndex     = -1;

	public StringStreamIterator(final ResultSet resultSet, final int columnIndex) {

		this.columnIndex = columnIndex;
		this.resultSet   = resultSet;
	}

	@Override
	public boolean hasNext() {

		try {

			if (firstCall) {

				return resultSet.isBeforeFirst();
			}

			return !resultSet.isLast();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return true;
	}

	@Override
	public String next() {

		firstCall = false;

		try {

			if (resultSet.next()) {

				return resultSet.getString(columnIndex);
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}
}
