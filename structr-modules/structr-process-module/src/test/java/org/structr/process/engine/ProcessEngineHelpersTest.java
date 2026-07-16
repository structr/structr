/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.engine;

import com.google.gson.Gson;
import org.structr.process.traits.definitions.ProcessParameterValueTraitDefinition;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Pure unit tests for the self-contained (static) helpers of {@link ProcessEngine}
 * that need no graph / database: the bpmnAttributes JSON extractor, the BPMN timer
 * expression parser, parameter-type inference/conversion and the foreign-script
 * transpiler.
 *
 * <p>These run without {@code StructrUiTest} — no Structr services are started.</p>
 */
public class ProcessEngineHelpersTest {

	private static final Gson GSON = new Gson();

	// ==================================================================
	// getJsonAttributeValue -- the bpmnAttributes extractor
	// ==================================================================

	@Test
	public void testSimpleStringValue() {
		assertEquals("Flow_no", ProcessEngine.getJsonAttributeValue("{\"default\":\"Flow_no\"}", "default"));
	}

	@Test
	public void testPicksCorrectKeyAmongMany() {
		final String json = "{\"a\":\"1\",\"default\":\"Flow_no\",\"z\":\"9\"}";
		assertEquals("1",       ProcessEngine.getJsonAttributeValue(json, "a"));
		assertEquals("Flow_no", ProcessEngine.getJsonAttributeValue(json, "default"));
		assertEquals("9",       ProcessEngine.getJsonAttributeValue(json, "z"));
	}

	@Test
	public void testCancelActivityFalseAsString() {
		// The importer stores cancelActivity as a string; the engine compares it to "false".
		assertEquals("false", ProcessEngine.getJsonAttributeValue("{\"cancelActivity\":\"false\"}", "cancelActivity"));
	}

	@Test
	public void testValueWithSpaces() {
		assertEquals("hello world", ProcessEngine.getJsonAttributeValue("{\"name\":\"hello world\"}", "name"));
	}

	@Test
	public void testValueContainingColon() {
		assertEquals("a:b:c", ProcessEngine.getJsonAttributeValue("{\"ref\":\"a:b:c\"}", "ref"));
	}

	@Test
	public void testValueContainingBraces() {
		assertEquals("{x:1}", ProcessEngine.getJsonAttributeValue("{\"expr\":\"{x:1}\"}", "expr"));
	}

	@Test
	public void testValueWithEscapedQuotes() {
		// Built with Gson exactly as the importer would: value contains double quotes.
		// The previous indexOf-based scanner truncated this at the first escaped quote
		// (returning `he said \`); the JSON parser returns the full, unescaped value.
		final String json = GSON.toJson(Map.of("note", "he said \"hi\""));
		assertEquals("he said \"hi\"", ProcessEngine.getJsonAttributeValue(json, "note"));
	}

	@Test
	public void testAbsentKeyReturnsNull() {
		assertNull(ProcessEngine.getJsonAttributeValue("{\"a\":\"1\"}", "default"));
	}

	@Test
	public void testKeySubstringDoesNotFalseMatch() {
		// "default" must NOT match the longer key "defaultFlow".
		assertNull(ProcessEngine.getJsonAttributeValue("{\"defaultFlow\":\"x\"}", "default"));
	}

	@Test
	public void testKeyNameOccurringInsideAValueIsIgnored() {
		// The word "default" appears inside another attribute's value; only the real
		// "default" key must be returned.
		final String json = GSON.toJson(new LinkedHashMap<>(Map.of(
			"documentation", "take the default branch",
			"default",       "Flow_yes"
		)));
		assertEquals("Flow_yes", ProcessEngine.getJsonAttributeValue(json, "default"));
	}

	@Test
	public void testNonStringPrimitivesAreReturnedAsStrings() {
		// Robustness: even though the importer only writes strings, a numeric or boolean
		// value must be read correctly (the old scanner returned null for these).
		assertEquals("42",   ProcessEngine.getJsonAttributeValue("{\"count\":42}", "count"));
		assertEquals("true", ProcessEngine.getJsonAttributeValue("{\"flag\":true}", "flag"));
	}

	@Test
	public void testNullBlankAndMalformedJsonReturnNull() {
		assertNull(ProcessEngine.getJsonAttributeValue(null,        "x"));
		assertNull(ProcessEngine.getJsonAttributeValue("",          "x"));
		assertNull(ProcessEngine.getJsonAttributeValue("   ",       "x"));
		assertNull(ProcessEngine.getJsonAttributeValue("{not json", "x"));
		assertNull(ProcessEngine.getJsonAttributeValue("[\"a\"]",  "x")); // not an object
		assertNull(ProcessEngine.getJsonAttributeValue("{\"x\":\"y\"}", null));
	}

	@Test
	public void testJsonNullValueReturnsNull() {
		assertNull(ProcessEngine.getJsonAttributeValue("{\"x\":null}", "x"));
	}

	@Test
	public void testSurroundingWhitespaceIsTolerated() {
		assertEquals("F", ProcessEngine.getJsonAttributeValue("   {\"default\":\"F\"}   ", "default"));
	}

	// ==================================================================
	// computeFireAt -- BPMN timer expressions
	// ==================================================================

	@Test
	public void testTimerDurationHours() {
		final long before = System.currentTimeMillis();
		final Date fireAt = ProcessEngine.computeFireAt("timeDuration", "PT1H");
		assertNotNull(fireAt);
		final long delta = fireAt.getTime() - before;
		assertTrue("PT1H should fire ~1h out, was " + delta + "ms",
			delta >= 59 * 60_000L && delta <= 61 * 60_000L);
	}

	@Test
	public void testTimerDurationDays() {
		final long before = System.currentTimeMillis();
		final Date fireAt = ProcessEngine.computeFireAt("timeDuration", "P1D");
		assertNotNull(fireAt);
		final long delta = fireAt.getTime() - before;
		assertTrue("P1D should fire ~24h out", delta >= 23 * 3_600_000L && delta <= 25 * 3_600_000L);
	}

	@Test
	public void testTimerDate() {
		final String iso = "2999-01-01T00:00:00Z";
		assertEquals(Date.from(Instant.parse(iso)), ProcessEngine.computeFireAt("timeDate", iso));
	}

	@Test
	public void testTimerCycleUnsupportedReturnsNull() {
		assertNull(ProcessEngine.computeFireAt("timeCycle", "R3/PT10M"));
	}

	@Test
	public void testTimerInvalidOrEmptyReturnsNull() {
		assertNull(ProcessEngine.computeFireAt("timeDuration", "not-a-duration"));
		assertNull(ProcessEngine.computeFireAt("timeDate",     "not-a-date"));
		assertNull(ProcessEngine.computeFireAt("timeDuration", ""));
		assertNull(ProcessEngine.computeFireAt("timeDuration", null));
		assertNull(ProcessEngine.computeFireAt("unknownType",  "PT1H"));
	}

	@Test
	public void testParseIso8601DurationMillis() {
		assertEquals(86_400_000L, ProcessEngine.parseIso8601DurationMillis("P1D"));
		assertEquals(3_600_000L,  ProcessEngine.parseIso8601DurationMillis("PT1H"));
		assertEquals(86_400_000L + 2 * 3_600_000L, ProcessEngine.parseIso8601DurationMillis("P1DT2H"));
		assertEquals(7 * 86_400_000L, ProcessEngine.parseIso8601DurationMillis("P1W"));
		assertEquals(-1L, ProcessEngine.parseIso8601DurationMillis("X"));   // does not start with P
		assertEquals(-1L, ProcessEngine.parseIso8601DurationMillis(null));
	}

	// ==================================================================
	// inferParameterType / convertParameterValue
	// ==================================================================

	@Test
	public void testInferParameterType() {
		assertEquals(ProcessParameterValueTraitDefinition.TYPE_BOOLEAN, ProcessEngine.inferParameterType(Boolean.TRUE));
		assertEquals(ProcessParameterValueTraitDefinition.TYPE_INTEGER, ProcessEngine.inferParameterType(42));
		assertEquals(ProcessParameterValueTraitDefinition.TYPE_INTEGER, ProcessEngine.inferParameterType(42L));
		assertEquals(ProcessParameterValueTraitDefinition.TYPE_DOUBLE,  ProcessEngine.inferParameterType(3.14));
		assertEquals(ProcessParameterValueTraitDefinition.TYPE_DATE,    ProcessEngine.inferParameterType(new Date()));
		// Strings (the common form-post case) and null stay untyped.
		assertNull(ProcessEngine.inferParameterType("hello"));
		assertNull(ProcessEngine.inferParameterType(null));
	}

	@Test
	public void testConvertParameterValue() {
		assertNull(ProcessEngine.convertParameterValue(null, ProcessParameterValueTraitDefinition.TYPE_STRING));
		assertEquals("hello", ProcessEngine.convertParameterValue("hello", null));
		assertEquals("hello", ProcessEngine.convertParameterValue("hello", ProcessParameterValueTraitDefinition.TYPE_STRING));
		assertEquals(Boolean.TRUE, ProcessEngine.convertParameterValue("true", ProcessParameterValueTraitDefinition.TYPE_BOOLEAN));
		assertEquals(42, ProcessEngine.convertParameterValue("42", ProcessParameterValueTraitDefinition.TYPE_INTEGER));
		assertEquals(3.14, ProcessEngine.convertParameterValue("3.14", ProcessParameterValueTraitDefinition.TYPE_DOUBLE));
		// Unparseable number falls back to the raw string rather than throwing.
		assertEquals("notanumber", ProcessEngine.convertParameterValue("notanumber", ProcessParameterValueTraitDefinition.TYPE_INTEGER));
	}

	// ==================================================================
	// transpileForeignScript
	// ==================================================================

	// ==================================================================
	// rewriteConditionExpression -- JUEL/bare-variable -> $.process rewriting
	// ==================================================================

	@Test
	public void testRewriteSingleVariable() {
		assertEquals("$.process.approved == true",
			ProcessEngine.rewriteConditionExpression("approved == true", Set.of("approved")));
	}

	@Test
	public void testRewriteKeepsLiteralsAndOperators() {
		// Only the known variable 'delivery' is rewritten; the string literal 'express' is not.
		assertEquals("$.process.delivery == 'express'",
			ProcessEngine.rewriteConditionExpression("delivery == 'express'", Set.of("delivery")));
	}

	@Test
	public void testRewriteMultipleVariables() {
		final String out = ProcessEngine.rewriteConditionExpression(
			"approved == true && amount > 100", Set.of("approved", "amount"));
		assertEquals("$.process.approved == true && $.process.amount > 100", out);
	}

	@Test
	public void testRewriteDoesNotDoubleRewriteAlreadyQualified() {
		// A reference already written as $.process.x must not be rewritten again.
		assertEquals("$.process.approved == true",
			ProcessEngine.rewriteConditionExpression("$.process.approved == true", Set.of("approved")));
	}

	@Test
	public void testRewriteRespectsWordBoundaries() {
		// 'amount' must not match inside 'totalamount'.
		assertEquals("totalamount > 1",
			ProcessEngine.rewriteConditionExpression("totalamount > 1", Set.of("amount")));
	}

	@Test
	public void testRewriteNoVariablesOrNullIsIdentity() {
		assertEquals("approved == true",
			ProcessEngine.rewriteConditionExpression("approved == true", Set.of()));
		assertNull(ProcessEngine.rewriteConditionExpression(null, Set.of("approved")));
	}

	// ==================================================================
	// detectScriptLanguage -- scriptFormat classification
	// ==================================================================

	@Test
	public void testDetectForeignJavaScript() {
		assertEquals(ProcessEngine.ScriptLanguage.FOREIGN_JAVASCRIPT, ProcessEngine.detectScriptLanguage("javascript"));
		assertEquals(ProcessEngine.ScriptLanguage.FOREIGN_JAVASCRIPT, ProcessEngine.detectScriptLanguage("JS"));
	}

	@Test
	public void testDetectStructrJavaScript() {
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_JAVASCRIPT, ProcessEngine.detectScriptLanguage("structr-javascript"));
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_JAVASCRIPT, ProcessEngine.detectScriptLanguage("structr-js"));
	}

	@Test
	public void testDetectStructrScriptDefault() {
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_SCRIPT, ProcessEngine.detectScriptLanguage(null));
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_SCRIPT, ProcessEngine.detectScriptLanguage(""));
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_SCRIPT, ProcessEngine.detectScriptLanguage("structrscript"));
	}

	@Test
	public void testDetectScriptLanguageKnownGaps() {
		// Documents the (deliberately narrow, arguably too narrow) matching: a non-JS
		// foreign format and a real JavaScript MIME type both fall through to StructrScript.
		// These are the cases flagged as questionable and are worth revisiting.
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_SCRIPT, ProcessEngine.detectScriptLanguage("groovy"));
		assertEquals(ProcessEngine.ScriptLanguage.STRUCTR_SCRIPT, ProcessEngine.detectScriptLanguage("text/javascript"));
	}

	// ==================================================================
	// transpileForeignScript
	// ==================================================================

	@Test
	public void testTranspileForeignScript() {
		final String out = ProcessEngine.transpileForeignScript(
			"var x = execution.getVariable(\"amount\");\nexecution.setVariable(\"approved\", true);");
		// getVariable is rewritten to the $.process accessor ...
		assertTrue("getVariable should be rewritten to $.process.amount", out.contains("$.process.amount"));
		// ... the original source is preserved as a block comment ...
		assertTrue("original source should be preserved as a comment", out.contains("/*"));
		// ... and the live setVariable statement is dropped from the transpiled body.
		final String transpiledBody = out.substring(out.indexOf("*/") + 2);
		assertFalse("setVariable call should be dropped from the transpiled body",
			transpiledBody.contains("execution.setVariable"));
	}
}
