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
import java.sql.Types;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;

/**
 */
public abstract class SQLEntity implements PropertyContainer {

	protected Map<String, Object> data = new LinkedHashMap<>();
	protected SQLDatabaseService db    = null;
	protected SQLIdentity id           = null;
	protected boolean stale            = false;

	SQLEntity(final SQLDatabaseService db, final SQLIdentity identity, final Map<String, Object> data) {

		this.db   = db;
		this.id   = identity;

		if (data != null) {
			this.data.putAll(data);
		}
	}

	SQLEntity(final SQLIdentity id) {
		this.id = id;
	}

	SQLIdentity getIdentity() {
		return id;
	}

	@Override
	public Identity getId() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {
		return data.containsKey(name);
	}

	@Override
	public Object getProperty(final String name) {
		return data.get(name);
	}

	@Override
	public Object getProperty(final String name, final Object defaultValue) {

		if (data.containsKey(name)) {

			return data.get(name);
		}

		return defaultValue;
	}

	@Override
	public void setProperty(final String name, final Object value) {

		if (value != null) {

			try {

				final SQLTransaction tx     = db.getCurrentTransaction();
				final PreparedStatement stm = tx.prepareStatement("UPDATE " + id.getType() + " SET `" + name + "` = ? WHERE id = ?");

				stm.setObject(1, value);
				stm.setString(2, id.getId());

				stm.executeUpdate();

				data.remove(name);

			} catch (SQLException ex) {
				ex.printStackTrace();
			}

		} else {

			removeProperty(name);
		}
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		try {

			final SQLTransaction tx  = db.getCurrentTransaction();
			final StringBuilder buf  = new StringBuilder("UPDATE " + id.getType() + " SET ");
			final List<Object> data  = new LinkedList<>();

			for (final Iterator<Entry<String, Object>> iterator = values.entrySet().iterator(); iterator.hasNext();) {

				final Entry<String, Object> entry = iterator.next();
				final String name                 = entry.getKey();
				final Object value                = entry.getValue();

				if (value != null) {

					buf.append("`");
					buf.append(name);
					buf.append("` = ?");

					data.add(value);

				} else {

					buf.append(" = NULL");
				}

				if (iterator.hasNext()) {
					buf.append(", ");
				}
			}

			buf.append(" WHERE id = ?");

			final PreparedStatement stm = tx.prepareStatement(buf.toString());
			int index                   = 1;

			for (final Object value : data) {
				stm.setObject(index++, value);
			}

			// set ID for WHERE clause
			stm.setString(index++, id.getId());


			stm.executeUpdate();

			for (final String name : values.keySet()) {
				data.remove(name);
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void removeProperty(final String name) {

		try {

			final SQLTransaction tx     = db.getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("UPDATE " + id.getType() + " SET `" + name + "` = NULL WHERE id = ?");

			stm.setString(1, id.getId());

			stm.executeUpdate();

			data.remove(name);

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return data.keySet();
	}

	// ----- public static methods -----
	public static int getInsertTypeForValue(final Object value) {

		if (value != null) {

			if (value.getClass().isEnum()) {
				return 5;
			}

			if (value instanceof String) {
				return 5;
			}

			if (value instanceof Integer) {
				return 6;
			}

			if (value instanceof Boolean) {
				return 7;
			}

			if (value instanceof Long) {
				return 8;
			}
		}

		return -1;
	}

	public static void configureStatement(final PreparedStatement stm, final long id, final String key, final Object value) throws SQLException {

		stm.setLong(1, id);
		stm.setString(2, key);
		stm.setInt(3, SQLEntity.getInsertTypeForValue(value));

		// reset values before re-use
		stm.setNull(4, Types.VARCHAR);
		stm.setNull(5, Types.INTEGER);
		stm.setNull(6, Types.BOOLEAN);
		stm.setNull(7, Types.BIGINT);

		if (value != null) {

			if (value.getClass().isEnum()) {
				stm.setString(4, ((Enum)value).name());
				return;
			}

			if (value instanceof String) {
				stm.setString(4, (String)value);
				return;
			}

			if (value instanceof Integer) {
				stm.setInt(5, (Integer)value);
				return;
			}

			if (value instanceof Boolean) {
				stm.setBoolean(6, (Boolean)value);
				return;
			}

			if (value instanceof Long) {
				stm.setLong(7, (Long)value);
				return;
			}
		}
	}
}
