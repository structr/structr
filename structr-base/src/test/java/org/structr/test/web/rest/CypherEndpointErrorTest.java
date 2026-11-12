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
package org.structr.test.web.rest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.restassured.RestAssured;
import org.slf4j.LoggerFactory;
import org.structr.api.DataFormatException;
import org.structr.api.SyntaxErrorException;
import org.structr.api.UnknownClientException;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A query the database rejects is the caller's error: 422 with the database's message, one log line, no stack trace.
 */
public class CypherEndpointErrorTest extends StructrUiTest {

	@Test
	public void testTypeErrorWhileReadingTheResultIs422() {

		createTarget();

		final List<ILoggingEvent> events = capture(() -> post("MATCH (n:CypherErrorTarget) RETURN toString(n.tags) AS s", "toString"));

		assertLoggedOnce(events);
	}

	@Test
	public void testTypeErrorInTheCypherFunctionIs422() {

		createTarget();

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.cypher('MATCH (n:CypherErrorTarget) RETURN toString(n.tags) AS s') }}", "test");

			fail("expected the query to be rejected");

		} catch (FrameworkException fex) {

			assertEquals(fex.toString(), 422, fex.getStatus());
		}
	}

	@Test
	public void testOnlyInvalidRequestsAreTheCallersError() {

		assertTrue(JsonRestServlet.isCallerError(new SyntaxErrorException(null, "Neo.ClientError.Statement.SyntaxError", "x")));
		assertTrue(JsonRestServlet.isCallerError(new DataFormatException(null, "Neo.ClientError.Statement.TypeError", "x")));
		assertTrue(JsonRestServlet.isCallerError(new UnknownClientException(null, "Neo.ClientError.Statement.ArgumentError", "x")));

		// operational errors stay server errors
		assertFalse(JsonRestServlet.isCallerError(new UnknownClientException(null, "Neo.ClientError.Transaction.TransactionTimedOut", "x")));
		assertFalse(JsonRestServlet.isCallerError(new UnknownClientException(null, "Neo.ClientError.General.ForbiddenOnReadOnlyDatabase", "x")));
		assertFalse(JsonRestServlet.isCallerError(new UnknownClientException(null, null, "x")));
		assertFalse(JsonRestServlet.isCallerError(new IllegalStateException("x")));
	}

	@Test
	public void testSyntaxErrorIs422() {

		final List<ILoggingEvent> events = capture(() -> post("MATCH (n RETURN n", "Invalid input"));

		assertLoggedOnce(events);
	}

	@Test
	public void testSyntaxErrorInGetIs422() {

		final List<ILoggingEvent> events = capture(() -> {

			RestAssured.basePath = "/structr/rest";

			RestAssured
				.given()
					.header(X_USER_HEADER, Settings.SuperUserName.getValue())
					.header(X_PASSWORD_HEADER, Settings.SuperUserPassword.getValue())
					.queryParam("query", "MATCH (n RETURN n")
				.expect()
					.statusCode(422)
				.when()
					.get("/cypher");
		});

		assertLoggedOnce(events);
	}

	// ----- private methods -----
	private void createTarget() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("CypherErrorTarget").addStringArrayProperty("tags");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			app.create("CypherErrorTarget", new NodeAttribute<>(Traits.of("CypherErrorTarget").key("tags"), new String[] { "a", "b" }));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private void post(final String query, final String expectedMessage) {

		RestAssured.basePath = "/structr/rest";

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, Settings.SuperUserName.getValue())
				.header(X_PASSWORD_HEADER, Settings.SuperUserPassword.getValue())
				.body(Map.of("query", query))
			.expect()
				.statusCode(422)
				.body("message", containsString(expectedMessage))
			.when()
				.post("/cypher");
	}

	private void assertLoggedOnce(final List<ILoggingEvent> events) {

		final List<ILoggingEvent> warnings = events.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN)).toList();
		final List<ILoggingEvent> rejected = events.stream().filter(e -> e.getFormattedMessage().contains("the database rejected the request")).toList();

		assertEquals("a caller's query error must not be logged as a warning: " + warnings.stream().map(ILoggingEvent::getFormattedMessage).toList(), 0, warnings.size());
		assertEquals(1, rejected.size());
		assertEquals("one line, no stack trace", null, rejected.get(0).getThrowableProxy());
		assertTrue(rejected.get(0).getFormattedMessage(), rejected.get(0).getFormattedMessage().contains("/cypher"));
	}

	private List<ILoggingEvent> capture(final Runnable code) {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(JsonRestServlet.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			code.run();

		} finally {

			logger.detachAppender(appender);
		}

		return List.copyOf(appender.list);
	}
}
