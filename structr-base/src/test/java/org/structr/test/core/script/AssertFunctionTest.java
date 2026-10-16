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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * assert() is an access check in many applications, so whatever goes wrong with its arguments has to
 * abort the request. Only the Boolean true may let it through.
 */
public class AssertFunctionTest extends StructrTest {

	@Test
	public void testBooleanConditions() {

		assertPasses("true passes", securityContext, "${{ $.assert(true, 422, 'never') }}");
		assertAbortsWith("false aborts with its status", securityContext, "${{ $.assert(false, 418, 'teapot') }}", 418);
	}

	@Test
	public void testNullConditionOfAnAnonymousUserAborts() {

		final SecurityContext anonymous = SecurityContext.getInstance(null, AccessMode.Frontend);
		final List<String> passed       = new ArrayList<>();

		// case 2: the signed-in check written without a comparison
		assertAborts(passed, "assert($.me) for an anonymous user", anonymous, "${{ $.assert($.me, 401, 'Please sign in') }}");
		assertAborts(passed, "assert(null) in JavaScript", securityContext, "${{ $.assert(null, 401, 'null condition') }}");
		assertAborts(passed, "assert(null) in StructrScript", securityContext, "${assert(null, 401, 'null condition')}");

		assertNothingPassed(passed);
	}

	@Test
	public void testUnsetBooleanPropertyAborts() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("AssertTarget");

			type.addBooleanProperty("flag");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.create("AssertTarget", "unflagged");

			tx.success();
		}

		final List<String> passed = new ArrayList<>();

		// case 3: a Boolean property that was never set
		assertAborts(passed, "assert() on an unset Boolean property", securityContext, "${{ $.assert($.first($.find('AssertTarget')).flag, 403, 'not flagged') }}");

		assertNothingPassed(passed);
	}

	@Test
	public void testMissingOrNullArgumentsAbortEvenWithAFalseCondition() {

		final List<String> passed = new ArrayList<>();

		// case 4: message missing
		assertAborts(passed, "assert(false, 401) without a message", securityContext, "${{ $.assert(false, 401) }}");

		// case 5: status or message null
		assertAborts(passed, "assert(false, null, message)", securityContext, "${{ $.assert(false, null, 'null status') }}");
		assertAborts(passed, "assert(false, status, null)", securityContext, "${{ $.assert(false, 401, null) }}");

		assertNothingPassed(passed);
	}

	@Test
	public void testStatusCodesOfBrokenCalls() {

		assertAbortsWith("a null condition keeps the status", securityContext, "${{ $.assert(null, 401, 'sign in') }}", 401);
		assertAbortsWith("a missing status is 500", securityContext, "${{ $.assert(false) }}", 500);
		assertAbortsWith("a null status is 500", securityContext, "${{ $.assert(false, null, 'x') }}", 500);
		assertAbortsWith("a status that is not a number is 500", securityContext, "${{ $.assert(false, 'four-oh-one', 'x') }}", 500);

		// the check itself passed, a missing message cannot make it fail
		assertPasses("true with a null message passes", securityContext, "${{ $.assert(true, 401, null) }}");
	}

	@Test
	public void testWarningsNameWhatWasWrong() {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(Functions.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			assertAbortsWith("false", securityContext, "${{ $.assert(false, 422, 'plain false') }}", 422);

			assertEquals("a Boolean false is a normal abort and must not warn", 0, warnings(appender).size());

			assertAborts(new ArrayList<>(), "a string", securityContext, "${{ $.assert('true', 422, 'x') }}");
			assertAborts(new ArrayList<>(), "a number", securityContext, "${{ $.assert(1, 422, 'x') }}");
			assertAborts(new ArrayList<>(), "a list", securityContext, "${{ $.assert([1], 422, 'x') }}");
			assertAborts(new ArrayList<>(), "null", securityContext, "${{ $.assert(null, 422, 'x') }}");
			assertAborts(new ArrayList<>(), "missing message", securityContext, "${{ $.assert(false, 422) }}");

			final List<String> warnings = warnings(appender);

			assertTrue(warnings.toString(), warnings.stream().anyMatch(w -> w.contains("the condition is a string, not a boolean")));
			assertTrue(warnings.toString(), warnings.stream().anyMatch(w -> w.contains("the condition is a number, not a boolean")));
			assertTrue(warnings.toString(), warnings.stream().anyMatch(w -> w.contains("the condition is a list, not a boolean")));
			assertTrue(warnings.toString(), warnings.stream().anyMatch(w -> w.contains("the condition is null, not a boolean")));
			assertTrue(warnings.toString(), warnings.stream().anyMatch(w -> w.contains("called with 2 arguments")));

		} finally {

			logger.detachAppender(appender);
		}
	}

	// ----- private methods -----
	private List<String> warnings(final ListAppender<ILoggingEvent> appender) {

		return appender.list.stream().filter(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().startsWith("assert")).map(ILoggingEvent::getFormattedMessage).toList();
	}

	private void assertPasses(final String description, final SecurityContext context, final String script) {

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(context), null, script, "test");

			tx.success();

		} catch (Throwable t) {

			fail(description + ": unexpected " + t);
		}
	}

	private void assertAborts(final List<String> passed, final String description, final SecurityContext context, final String script) {

		try {

			assertAbortsWith(description, context, script, null);

		} catch (AssertionError e) {

			passed.add(e.getMessage());
		}
	}

	private void assertNothingPassed(final List<String> passed) {

		if (!passed.isEmpty()) {

			fail(passed.size() + " call(s) did not abort:\n" + String.join("\n", passed));
		}
	}

	private void assertAbortsWith(final String description, final SecurityContext context, final String script, final Integer status) {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(context), null, script, "test");

			fail(description + ": assert() did not abort, the script went on and returned " + result);

		} catch (AssertException ex) {

			if (status != null) {

				assertEquals(description, (int) status, ex.getStatus());
			}

		} catch (FrameworkException ex) {

			if (status != null) {

				assertEquals(description, (int) status, ex.getStatus());
			}
		}
	}
}
