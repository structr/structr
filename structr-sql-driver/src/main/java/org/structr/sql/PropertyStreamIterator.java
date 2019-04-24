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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
class PropertyStreamIterator implements Iterator<NodeResult> {

	private NodeResult previous = null;
	private NodeResult current  = null;
	private ResultSet resultSet        = null;
	private long currentObjectId       = -1;
	private boolean firstCall          = true;

	public PropertyStreamIterator(final ResultSet resultSet) {

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
	public NodeResult next() {

		firstCall = false;

		try {
			// fetch data for next result..
			while (resultSet.next()) {

				final long thisObjectId = resultSet.getLong(2);

				if (currentObjectId != thisObjectId) {

					// current object has changed..
					currentObjectId = thisObjectId;
					previous        = current;
					current         = new NodeResult(SQLIdentity.forId(thisObjectId));

					current.visit(resultSet);

					// return previous object
					if (previous != null) {
						return previous;
					}

				} else {

					current.visit(resultSet);
				}
			}

		} catch (SQLException ex) {
			Logger.getLogger(PropertyStreamIterator.class.getName()).log(Level.SEVERE, null, ex);
		}

		// return last result
		return current;
	}
}
