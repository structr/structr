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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.DatePropertyGenerator;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.AssertJUnit.*;

/**
 * Locks the behaviour of the {@code now} keyword across both scripting engines.
 *
 * In StructrScript (${now}):
 *   ActionContext returns DatePropertyGenerator.format(new Date(), Settings.DefaultDateFormat)
 *   — a String using the configured date format pattern.
 *
 * In JavaScript (${{ $.now }}):
 *   StructrBinding.get("now") returns wrap(actionContext, new Date())
 *   — a JS Date object; typeof is "object", instanceof Date is true.
 *   When embedded in a replaceVariables template the Date is serialised via
 *   Scripting.formatToDefaultDateOrString, which uses the same DefaultDateFormat.
 */
public class NowKeywordTest extends StructrTest {

	// The default format configured in Settings.DefaultDateFormat.
	private static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	// Window used when asserting that a resolved timestamp is "recent".
	private static final long RECENCY_WINDOW_MS = 10_000L;

	// -----------------------------------------------------------------------
	// StructrScript — ${now}
	// -----------------------------------------------------------------------

	@Test
	public void testStructrScriptNowReturnsNonNullString() {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final Object result = Scripting.evaluate(ctx, null, "${now}", "test");

			assertNotNull("${now} must not return null", result);
			assertTrue("${now} must return a String in StructrScript context", result instanceof String);
			assertFalse("${now} must not return an empty string", ((String) result).isEmpty());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testStructrScriptNowMatchesDefaultDateFormat() {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final String result = (String) Scripting.evaluate(ctx, null, "${now}", "test");

			assertNotNull("${now} must not return null", result);

			// The result must be parseable with the default format.
			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
			sdf.setLenient(false);

			try {

				sdf.parse(result);

			} catch (ParseException ex) {

				fail("${now} result \"" + result + "\" does not match default format " + DEFAULT_FORMAT);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testStructrScriptNowIsRecent() {

		try (final Tx tx = app.tx()) {

			final long before = System.currentTimeMillis();
			final ActionContext ctx = new ActionContext(securityContext);
			final String result = (String) Scripting.evaluate(ctx, null, "${now}", "test");
			final long after  = System.currentTimeMillis();

			assertNotNull("${now} must not return null", result);

			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
			final Date parsed;

			try {

				parsed = sdf.parse(result);

			} catch (ParseException ex) {

				fail("${now} result \"" + result + "\" is not parseable: " + ex.getMessage());

				return;
			}

			final long millis = parsed.getTime();
			assertTrue("${now} must not be earlier than the test start", millis >= before - RECENCY_WINDOW_MS);
			assertTrue("${now} must not be later than the test end",     millis <= after  + RECENCY_WINDOW_MS);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testStructrScriptNowReplaceVariablesReturnsString() {

		// replaceVariables embeds the result into a surrounding template string, so
		// the returned type is always String regardless of what the keyword resolves to.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final String result = Scripting.replaceVariables(ctx, null, "${now}");

			assertNotNull("replaceVariables ${now} must not return null", result);
			assertFalse("replaceVariables ${now} must not return an empty string", result.isEmpty());

			// Confirm the result is parseable as a date using the default format.
			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);

			try {

				sdf.parse(result);

			} catch (ParseException ex) {

				fail("replaceVariables ${now} result \"" + result + "\" does not match default format " + DEFAULT_FORMAT);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testStructrScriptNowEmbeddedInTemplate() {

		// Surrounding text must be preserved; only the ${now} token is replaced.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final String result = Scripting.replaceVariables(ctx, null, "prefix-${now}-suffix");

			assertNotNull("Template result must not be null", result);
			assertTrue("Template result must start with 'prefix-'",  result.startsWith("prefix-"));
			assertTrue("Template result must end with '-suffix'",    result.endsWith("-suffix"));

			// The middle part must be a valid date.
			final String middle = result.substring("prefix-".length(), result.length() - "-suffix".length());
			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);

			try {

				sdf.parse(middle);

			} catch (ParseException ex) {

				fail("Embedded ${now} in template yielded non-date middle part: \"" + middle + "\"");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testStructrScriptNowUsesConfiguredFormat() {

		// Settings.DefaultDateFormat controls the output pattern.
		// Changing it must change what ${now} returns.
		final String originalFormat = Settings.DefaultDateFormat.getValue();

		try (final Tx tx = app.tx()) {

			Settings.DefaultDateFormat.setValue("dd.MM.yyyy");

			final ActionContext ctx = new ActionContext(securityContext);
			final String result = (String) Scripting.evaluate(ctx, null, "${now}", "test");

			assertNotNull("${now} with custom format must not return null", result);

			// Must match dd.MM.yyyy (e.g. "19.06.2026"), not the default ISO pattern.
			assertTrue("${now} must use the configured format dd.MM.yyyy; got: " + result, result.matches("\\d{2}\\.\\d{2}\\.\\d{4}"));

			// Must NOT match the default ISO format.
			final SimpleDateFormat iso = new SimpleDateFormat(DEFAULT_FORMAT);
			iso.setLenient(false);

			try {

				iso.parse(result);
				fail("With custom format dd.MM.yyyy, ${now} should not parse as ISO format");

			} catch (ParseException expected) {

				// correct
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();

		} finally {

			Settings.DefaultDateFormat.setValue(originalFormat);
		}
	}

	@Test
	public void testStructrScriptNowIsAfterKnownPastDate() {

		// Functional check: now must be greater than a fixed date in the past.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			// ${gte(now, "2000-01-01T00:00:00+0000")} must be "true" — now is after 2000.
			final String gte = Scripting.replaceVariables(ctx, null, "${gte(now, \"2000-01-01T00:00:00+0000\")}");
			assertEquals("now must be >= 2000-01-01", "true", gte);

			// ${lt(now, "2000-01-01T00:00:00+0000")} must be "false".
			final String lt  = Scripting.replaceVariables(ctx, null, "${lt(now, \"2000-01-01T00:00:00+0000\")}");
			assertEquals("now must not be < 2000-01-01", "false", lt);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	// -----------------------------------------------------------------------
	// JavaScript — ${{ $.now }}
	// -----------------------------------------------------------------------

	@Test
	public void testJavaScriptNowTypeofIsObject() {

		// In the JS engine, $.now is a Date object: typeof must be "object".
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final Object result = Scripting.evaluate(ctx, null, "${{ typeof $.now; }}", "test");

			assertEquals("typeof $.now must be 'object' in JS context", "object", result);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testJavaScriptNowIsDateInstance() {

		// $.now instanceof Date must be true in JS.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final Object result = Scripting.evaluate(ctx, null, "${{ $.now instanceof Date; }}", "test");

			assertEquals("$.now instanceof Date must be true in JS context", Boolean.TRUE, result);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testJavaScriptNowToIsoStringIsRecent() {

		// $.now.toISOString() must return a non-null ISO-8601 UTC string that is recent.
		try (final Tx tx = app.tx()) {

			final long before = System.currentTimeMillis();
			final ActionContext ctx = new ActionContext(securityContext);
			final Object result = Scripting.evaluate(ctx, null, "${{ $.now.toISOString(); }}", "test");
			final long after  = System.currentTimeMillis();

			assertNotNull("$.now.toISOString() must not return null", result);
			assertTrue("$.now.toISOString() must return a String", result instanceof String);

			final String iso = (String) result;

			// JS toISOString always produces "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" in UTC.
			assertTrue("$.now.toISOString() must match ISO-8601 UTC pattern; got: " + iso, iso.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));

			// The represented time must fall within the test execution window.
			final Date parsed;

			try {

				parsed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(iso);

			} catch (ParseException ex) {

				fail("Could not parse $.now.toISOString() result: " + iso);

				return;
			}

			assertTrue("$.now.toISOString() must not be earlier than test start", parsed.getTime() >= before - RECENCY_WINDOW_MS);
			assertTrue("$.now.toISOString() must not be later than test end",     parsed.getTime() <= after  + RECENCY_WINDOW_MS);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	@Test
	public void testJavaScriptNowEmbeddedInTemplateIsFormattedDate() {

		// When ${{ $.now }} is embedded in a replaceVariables template, the Date object
		// is serialised via Scripting.formatToDefaultDateOrString — same DefaultDateFormat
		// as StructrScript ${now}.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final String result = Scripting.replaceVariables(ctx, null, "x=${{ $.now; }}");

			assertNotNull("Template with ${{ $.now }} must not return null", result);
			assertTrue("Result must start with 'x='", result.startsWith("x="));

			final String dateStr = result.substring("x=".length());
			assertFalse("Serialised date must not be empty", dateStr.isEmpty());

			// Must be parseable with the default format — not raw "object" or [object Date].
			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);

			try {

				sdf.parse(dateStr);

			} catch (ParseException ex) {

				fail("JS $.now embedded in template must serialise to default date format; got: \"" + dateStr + "\"");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}

	// -----------------------------------------------------------------------
	// Cross-engine consistency
	// -----------------------------------------------------------------------

	@Test
	public void testStructrScriptAndJavaScriptNowAreConsistent() {

		// Both engines must return dates within a short window of each other.
		// The StructrScript path returns a formatted String; the JS path returns
		// an object that serialises to the same format when embedded in a template.
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final String structrNow = Scripting.replaceVariables(ctx, null, "${now}");
			final String jsNow      = Scripting.replaceVariables(ctx, null, "${{ $.now; }}");

			assertNotNull("StructrScript ${now} must not be null", structrNow);
			assertNotNull("JavaScript $.now must not be null",     jsNow);

			final SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
			final Date structrDate;
			final Date jsDate;

			try {

				structrDate = sdf.parse(structrNow);
				jsDate      = sdf.parse(jsNow);

			} catch (ParseException ex) {

				fail("Could not parse now values for cross-engine comparison: structr=\"" + structrNow + "\", js=\"" + jsNow + "\"");

				return;
			}

			final long diff = Math.abs(structrDate.getTime() - jsDate.getTime());
			assertTrue("StructrScript and JS now values must be within " + RECENCY_WINDOW_MS + "ms of each other; diff=" + diff + "ms", diff <= RECENCY_WINDOW_MS);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
			fex.printStackTrace();
		}
	}
}
