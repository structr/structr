/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JdbcFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "jdbc";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("jdbcUrl, sqlQuery[, username, password]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final List<Map<String, Object>> data = new LinkedList<>();
			final String url                     = (String)sources[0];
			final String sql                     = (String)sources[1];

			String username = null;
			String password = null;

			switch (sources.length) {

				case 4: password = sources[3].toString();
				case 3: username = sources[2].toString();
					break;
			}

			try {

				try (final Connection connection = getConnection(url, username, password)) {

					final Statement statement = connection.createStatement();

					if (statement.execute(sql)) {

						try (final ResultSet resultSet = statement.getResultSet()) {

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
						}
					}
				}

			} catch (Throwable t) {

				// gets thrown as a basic SQLException
				if (t.getMessage().contains("No suitable driver found")) {

					//logger.warn("No suitable JDBC driver not found. Ensure that the appropriate driver JAR file is located in the lib directory.");
					throw new FrameworkException(422, "No suitable JDBC driver not found. Ensure that the appropriate driver JAR file is located in the lib directory.");

				} else {

					//logException(t, t.getMessage(), sources);
					throw new FrameworkException(422, t.getMessage());
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.jdbc(url, query[, username, password ]); }}. Example: ${{ $.jdbc('jdbc:mysql://localhost:3306', 'SELECT * from Test', 'user', 'p4ssw0rd'); }}"),
			Usage.structrScript("Usage: ${jdbc(url, query[, username, password ])}. Example: ${jdbc('jdbc:mysql://localhost:3306', 'SELECT * from Test', 'user', 'p4ssw0rd')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Fetches data from a JDBC source.";
	}

	@Override
	public String getLongDescription() {
		return """
		Make sure the driver specific to your SQL server is available in a JAR file in Structr's lib directory (`/usr/lib/structr/lib` in Debian installations).
		
		Other JAR sources are available for Oracle (https://www.oracle.com/technetwork/database/application-development/jdbc/downloads/index.html) or MSSQL (https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server).
		
		For other SQL Servers, please consult the documentation of that server.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "JDBC url to connect to the server"),
			Parameter.mandatory("query", "query to execute"),
			Parameter.optional("username", "username to use to connect"),
			Parameter.optional("password", "password to used to connect")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			{
			    let rows = $.jdbc('jdbc:oracle:thin:test/test@localhost:1521:orcl', 'SELECT * FROM test.emails');

			    rows.forEach(function(row) {

				$.log('Fetched row from Oracle database: ', row);

				let fromPerson = $.getOrCreate('Person', 'name', row.fromAddress);
				let toPerson   = $.getOrCreate('Person', 'name', row.toAddress);

				let message = $.getOrCreate('EMailMessage',
				    'content',   row.emailBody,
				    'sender',    fromPerson,
				    'recipient', toPerson,
				    'sentDate',  $.parseDate(row.emailDate, 'yyyy-MM-dd')
				);

				$.log('Found existing or created new EMailMessage node: ', message);
			    });
			}
			""", "Fetch data from an Oracle database")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Username and password can also be included in the JDBC connection string."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}

	private Connection getConnection(final String url, final String username, final String password) throws SQLException {

		if (username == null && password == null) {

			return DriverManager.getConnection(url);

		} else {

			return DriverManager.getConnection(url, username, password);
		}
	}
}
