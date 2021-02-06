/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.core.function;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class JdbcFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${jdbc(url, query[, driver])}. Example: ${jdbc(\"jdbc:mysql://localhost:3306\", \"SELECT * from Test\")}";

	@Override
	public String getName() {
		return "jdbc";
	}

	@Override
	public String getSignature() {
		return "jdbcUrl, sqlQuery [, driverClass ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final List<Map<String, Object>> data = new LinkedList<>();
			final String url                     = (String)sources[0];
			final String sql                     = (String)sources[1];

			String driverClass = "com.mysql.jdbc.Driver";

			if (sources.length == 3) {
				driverClass = (String)sources[2];
			}

			try {

				Class.forName(driverClass).newInstance();

				final Connection connection      = DriverManager.getConnection(url);
				final Statement statement        = connection.createStatement();
				final ResultSet resultSet        = statement.executeQuery(sql);
				final ResultSetMetaData metaData = resultSet.getMetaData();
				final int count                  = metaData.getColumnCount();

				while (resultSet.next()) {

					final Map<String, Object> row = new LinkedHashMap<>();

					for (int i=1; i<=count; i++) {

						final String key   = metaData.getColumnName(i);
						final Object value = resultSet.getObject(i);

						row.put(key, value);
					}

					data.add(row);
				}

			} catch (Throwable t) {
				if (t instanceof ClassNotFoundException) {
					logException((ClassNotFoundException) t, "{}: Driver class \"{}\" not found. Make sure the jar containing the class is located in the lib directory.", new Object[] { getReplacement(), driverClass, caller, getParametersAsString(sources) });
				} else {
					logException(t, t.getMessage(), sources);
				}
			}

			return data;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Fetches data from a JDBC source";
	}
}
