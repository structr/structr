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
package org.structr.test.core.script;

import org.structr.core.function.HMACFunction;
import org.structr.core.function.JdbcFunction;
import org.structr.core.function.MongoDBFunction;
import org.structr.schema.action.Function;
import org.structr.web.function.AddHeaderFunction;
import org.structr.web.function.CreateZipFunction;
import org.structr.web.function.HttpGetFunction;
import org.structr.web.function.HttpOptions;
import org.structr.web.function.HttpPostFunction;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/** Functions that take credentials must not write them into the server log when a wrong call is logged. */
public class CredentialParameterLoggingTest {

	private static final String SECRET = "do-not-log-this-secret-4711";

	@Test
	public void testCredentialFunctionsLogNoArgumentValues() throws Exception {

		for (final Function function : List.<Function>of(new HMACFunction(), new JdbcFunction(), new MongoDBFunction(), new AddHeaderFunction(), new CreateZipFunction())) {

			final String log = parametersForLog(function, new Object[] { "jdbc:mysql://user:" + SECRET + "@host/db", SECRET });

			assertFalse(function.getName() + ": " + log, log.contains(SECRET));
			assertTrue(function.getName() + ": " + log, log.contains("not logged because they are credentials"));
		}
	}

	@Test
	public void testHttpFunctionsMaskOnlyCredentials() throws Exception {

		final Object[] sources = {
			"https://user:" + SECRET + "@example.com/api",
			"{ \"name\": \"value\" }",
			Map.of("username", "user", "password", SECRET, "headers", Map.of("Authorization", "Bearer " + SECRET, "X-Api-Key", SECRET, "Accept", "application/json"))
		};

		final String expected = HttpOptions.parametersForLog(sources);

		assertFalse(expected, expected.contains(SECRET));
		assertTrue(expected, expected.contains("https://user:***@example.com/api"));
		assertTrue(expected, expected.contains("username=user"));
		assertTrue(expected, expected.contains("Accept=application/json"));
		assertTrue(expected, expected.contains("{ \"name\": \"value\" }"));

		for (final Function function : List.<Function>of(new HttpGetFunction(), new HttpPostFunction())) {

			assertEquals(function.getName(), expected, parametersForLog(function, sources));
		}
	}

	// ----- private methods -----
	private String parametersForLog(final Function function, final Object[] parameters) throws Exception {

		final Method method = Function.class.getDeclaredMethod("parametersForLog", Object[].class);

		method.setAccessible(true);

		return (String) method.invoke(function, (Object) parameters);
	}
}
