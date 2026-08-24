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

import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.function.LogLevelFunction;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * The per-level logging functions: log.warn(), log.error(), log.info() and log.debug().
 *
 * What is asserted here is that each name resolves and runs in BOTH scripting languages, and that the
 * line they build has the shape log() uses. Whether the line reached the log file is not asserted -
 * that would need a log appender to prove one statement, while the rendered text is the part that can
 * actually be wrong.
 */
public class LogLevelFunctionTest extends StructrTest {

	@Test
	public void testAllLevelsAreRegistered() {

		for (final String name : new String[] { "log.warn", "log.error", "log.info", "log.debug" }) {

			assertNotNull("function '" + name + "' is not registered", Functions.get(name));
		}

		// slf4j spells it warn, so that is the name, but warning has to work too
		assertNotNull("alias 'log.warning' is not registered", Functions.get("log.warning"));
		assertEquals("alias 'log.warning' does not point at log.warn", "log.warn", Functions.get("log.warning").getName());

		// and the plain log() is untouched
		assertNotNull("log() is no longer registered", Functions.get("log"));
	}

	@Test
	public void testStructrScript() {

		final ActionContext ctx = new ActionContext(securityContext);

		for (final String script : new String[] {
			"${log.warn('warned')}", "${log.warning('warned via the alias')}",
			"${log.error('failed')}", "${log.info('noted')}", "${log.debug('detail')}",
			"${log('plain log still works')}"
		}) {

			try {
				Scripting.evaluate(ctx, null, script, "test");

			} catch (FrameworkException fex) {

				fail("StructrScript could not evaluate " + script + ": " + fex.getMessage());
			}
		}
	}

	@Test
	public void testJavaScript() {

		final ActionContext ctx = new ActionContext(securityContext);

		// $.log is executable AND carries the levels as members
		for (final String script : new String[] {
			"${{ $.log.warn('warned'); }}", "${{ $.log.warning('warned via the alias'); }}",
			"${{ $.log.error('failed'); }}", "${{ $.log.info('noted'); }}", "${{ $.log.debug('detail'); }}",
			"${{ $.log('plain log still works'); }}"
		}) {

			try {
				Scripting.evaluate(ctx, null, script, "test");

			} catch (FrameworkException fex) {

				fail("JavaScript could not evaluate " + script + ": " + fex.getMessage());
			}
		}
	}

	@Test
	public void testTheScriptLocationIsRecorded() {

		final ActionContext ctx = new ActionContext(securityContext);

		assertNull("a location was reported before any function ran", ctx.getScriptLocation());

		try {

			// the location is set immediately before each call and not restored afterwards, so after
			// evaluating, the context holds the location of the last function that ran
			Scripting.evaluate(ctx, null, "${log.warn('single line')}", "test");
			assertEquals("wrong location for a call at the start of a one-line script", "1:1", ctx.getScriptLocation());

			// the point of the exercise: a call further down reports the line it is actually on
			final ActionContext multiLine = new ActionContext(securityContext);
			Scripting.evaluate(multiLine, null, "${\n\n   log.warn('third line')\n}", "test");
			assertEquals("wrong location for a call on the third line, indented by three", "3:4", multiLine.getScriptLocation());

			// JavaScript records nothing: GraalJS would have to be asked for a stack trace. The line
			// then renders exactly as log() has always rendered it, with no location in front.
			final ActionContext javaScript = new ActionContext(securityContext);
			Scripting.evaluate(javaScript, null, "${{ $.log.warn('js'); }}", "test");
			assertNull("JavaScript reported a location, which it has no way of knowing", javaScript.getScriptLocation());

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testTheRenderedLine() {

		// no location: a JavaScript call renders exactly as log() always has
		assertEquals("one two", LogLevelFunction.buildMessage(null, null, new Object[] { "one ", "two" }));
		assertEquals("nulls are skipped", LogLevelFunction.buildMessage(null, null, new Object[] { "nulls ", null, "are skipped" }));
		assertEquals("Caller: Sync - failed", LogLevelFunction.buildMessage(null, "Sync", new Object[] { "failed" }));
		assertEquals("", LogLevelFunction.buildMessage(null, null, new Object[] {}));

		// with the location StructrScript provides
		assertEquals("(12:5) failed", LogLevelFunction.buildMessage("12:5", null, new Object[] { "failed" }));
		assertEquals("(12:5) Caller: Sync - failed", LogLevelFunction.buildMessage("12:5", "Sync", new Object[] { "failed" }));
	}
}
