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
import org.structr.process.bpmn.BpmnElementType;
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
		final String json = GSON.toJson(new LinkedHashMap<>(Map.of("documentation", "take the default branch", "default",       "Flow_yes")));
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
		assertTrue("PT1H should fire ~1h out, was " + delta + "ms", delta >= 59 * 60_000L && delta <= 61 * 60_000L);
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

		assertEquals("$.process.approved == true", ProcessEngine.rewriteConditionExpression("approved == true", Set.of("approved")));
	}

	@Test
	public void testRewriteKeepsLiteralsAndOperators() {

		// Only the known variable 'delivery' is rewritten; the string literal 'express' is not.
		assertEquals("$.process.delivery == 'express'", ProcessEngine.rewriteConditionExpression("delivery == 'express'", Set.of("delivery")));
	}

	@Test
	public void testRewriteMultipleVariables() {

		final String out = ProcessEngine.rewriteConditionExpression("approved == true && amount > 100", Set.of("approved", "amount"));
		assertEquals("$.process.approved == true && $.process.amount > 100", out);
	}

	@Test
	public void testRewriteDoesNotDoubleRewriteAlreadyQualified() {

		// A reference already written as $.process.x must not be rewritten again.
		assertEquals("$.process.approved == true", ProcessEngine.rewriteConditionExpression("$.process.approved == true", Set.of("approved")));
	}

	@Test
	public void testRewriteRespectsWordBoundaries() {

		// 'amount' must not match inside 'totalamount'.
		assertEquals("totalamount > 1", ProcessEngine.rewriteConditionExpression("totalamount > 1", Set.of("amount")));
	}

	@Test
	public void testRewriteNoVariablesOrNullIsIdentity() {

		assertEquals("approved == true", ProcessEngine.rewriteConditionExpression("approved == true", Set.of()));
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
	// BpmnElementType enum
	// ==================================================================

	@Test
	public void testElementTypeFromBpmnName() {

		assertEquals(BpmnElementType.USER_TASK, BpmnElementType.fromBpmnName("userTask"));
		assertEquals(BpmnElementType.SUB_PROCESS, BpmnElementType.fromBpmnName("subProcess"));
		assertEquals(BpmnElementType.UNKNOWN, BpmnElementType.fromBpmnName("notARealType"));
		assertEquals(BpmnElementType.UNKNOWN, BpmnElementType.fromBpmnName(null));
	}

	@Test
	public void testElementTypeMatches() {

		assertTrue(BpmnElementType.USER_TASK.matches("userTask"));
		assertFalse(BpmnElementType.USER_TASK.matches("serviceTask"));
		// UNKNOWN has no bpmn name and matches nothing (including null).
		assertFalse(BpmnElementType.UNKNOWN.matches(null));
		assertFalse(BpmnElementType.UNKNOWN.matches("userTask"));
	}

	@Test
	public void testElementTypeKnownTypeNames() {

		assertTrue(BpmnElementType.isKnown("boundaryEvent"));
		assertFalse(BpmnElementType.isKnown("frobnicate"));
		assertTrue(BpmnElementType.knownTypeNames().contains("startEvent"));
		assertTrue(BpmnElementType.knownTypeNames().contains("userTask"));
		// UNKNOWN's null name must never leak into the known-name set: every named
		// constant except UNKNOWN contributes exactly one non-null name.
		assertEquals(BpmnElementType.values().length - 1, BpmnElementType.knownTypeNames().size());
	}

	// ==================================================================
	// transpileForeignScript
	// ==================================================================

	@Test
	public void testTranspileForeignScript() {

		final String out = ProcessEngine.transpileForeignScript("var x = execution.getVariable(\"amount\");\nexecution.setVariable(\"approved\", true);");
		// getVariable is rewritten to the $.process accessor ...
		assertTrue("getVariable should be rewritten to $.process.amount", out.contains("$.process.amount"));
		// ... the original source is preserved as a block comment ...
		assertTrue("original source should be preserved as a comment", out.contains("/*"));
		// ... and the live setVariable statement is dropped from the transpiled body.
		final String transpiledBody = out.substring(out.indexOf("*/") + 2);
		assertFalse("setVariable call should be dropped from the transpiled body", transpiledBody.contains("execution.setVariable"));
	}

	@Test
	public void testTranspileSetVariableWithoutTrailingSemicolon() {

		// A Camunda camunda:expression body ("${execution.setVariable(...)}", stripped
		// of the ${ } wrapper) has NO trailing semicolon. It must still transpile to a
		// $.process write, otherwise the service-task variable is never set.
		final String out  = ProcessEngine.transpileForeignScript("execution.setVariable('approved', true)");
		final String body = out.substring(out.indexOf("*/") + 2);

		assertTrue("no-semicolon setVariable must transpile to a $.process write; got:\n" + out, body.contains("$.process.approved = true"));
		assertFalse("the original execution.setVariable call must not survive in the transpiled body", body.contains("execution.setVariable"));
	}

	@Test
	public void testTranspileSetVariableWithNestedFunctionCall() {

		// Regression: a value containing parentheses (a function call) must not be
		// truncated at the first ')'. The old non-greedy regex captured "now(" and
		// produced the corrupted "$.process.startedAt = now(;)".
		final String out  = ProcessEngine.transpileForeignScript("execution.setVariable('startedAt', now())");
		final String body = out.substring(out.indexOf("*/") + 2);

		assertTrue("nested-call value must survive and be prefixed; got:\n" + out, body.contains("$.process.startedAt = $.now();"));
		assertFalse("must not emit the corrupted 'now(;)' form; got:\n" + out, body.contains("now(;)"));
	}

	@Test
	public void testTranspilePrefixesBareStructrFunctions() {

		final String out  = ProcessEngine.transpileForeignScript("var d = now();");
		final String body = out.substring(out.indexOf("*/") + 2);

		assertTrue("bare now() should become $.now(); got:\n" + out, body.contains("$.now()"));
	}

	@Test
	public void testTranspileLeavesMemberAndKeywordCallsAlone() {

		// Member calls (obj.foo()), JS control keywords (if/for/...), built-ins
		// (parseInt) and already-prefixed calls must NOT be turned into $. calls.
		assertFalse("member call must not be prefixed", ProcessEngine.prefixStructrFunctions("obj.foo(x)").contains("$.foo"));
		assertEquals("keyword must not be prefixed", "if (x) {", ProcessEngine.prefixStructrFunctions("if (x) {"));
		assertFalse("built-in parseInt must not be prefixed", ProcessEngine.prefixStructrFunctions("parseInt(x)").contains("$.parseInt"));
		assertEquals("already-prefixed call must be left alone", "$.now()", ProcessEngine.prefixStructrFunctions("$.now()"));
	}

	@Test
	public void testRewriteServiceCallsBindsBeansToServiceClasses() {

		// receiver.method(...) -> $.Receiver.method(...); nested/arg calls preserved.
		assertEquals("$.NotificationService.notifyReviewer(task)", ProcessEngine.rewriteServiceCalls("notificationService.notifyReviewer(task)"));
		// script-context / built-in receivers are NOT treated as services.
		assertEquals("$.process.x = 5;", ProcessEngine.rewriteServiceCalls("$.process.x = 5;"));
		assertEquals("Math.floor(x)", ProcessEngine.rewriteServiceCalls("Math.floor(x)"));
		// idempotent: an already-bound call is left alone on a second pass.
		assertEquals("$.NotificationService.notifyReviewer(task)", ProcessEngine.rewriteServiceCalls("$.NotificationService.notifyReviewer(task)"));
	}

	@Test
	public void testDetectServiceCallsCollectsReceiversMethodsAndArgCounts() {

		final Map<String, Map<String, Integer>> svc = ProcessEngine.detectServiceCalls("paymentGateway.charge(amount, currency); notificationService.notify()");
		assertEquals("two distinct services detected", 2, svc.size());
		assertEquals("charge takes 2 args", Integer.valueOf(2), svc.get("PaymentGateway").get("charge"));
		assertEquals("notify takes 0 args", Integer.valueOf(0), svc.get("NotificationService").get("notify"));
		// built-in / context receivers are excluded from detection
		assertTrue("no built-in receivers", ProcessEngine.detectServiceCalls("Math.floor(x); $.process.foo()").isEmpty());
	}

	@Test
	public void testServiceHeuristicIgnoresPlainVariables() {

		// Only service-convention names (…Service/…Delegate/…Gateway/…) are treated as
		// services; ordinary variables and the `task` context object are left untouched.
		assertEquals("plain variable call must not become a service class", "task.complete()", ProcessEngine.rewriteServiceCalls("task.complete()"));
		assertEquals("plain variable call must not become a service class", "order.total()", ProcessEngine.rewriteServiceCalls("order.total()"));
		assertTrue("no service detected for plain variables", ProcessEngine.detectServiceCalls("task.complete(); order.total()").isEmpty());
		// but convention-named beans still bind
		assertEquals("$.PaymentGateway.charge(x)", ProcessEngine.rewriteServiceCalls("paymentGateway.charge(x)"));
	}

	@Test
	public void testMatchingParenIndexHandlesNestingAndQuotes() {

		// ')' inside a quoted string must not close the call.
		final String s = "f('a)b', g())";
		assertEquals(s.length() - 1, ProcessEngine.matchingParenIndex(s, 1));
		assertEquals("unbalanced input returns -1", -1, ProcessEngine.matchingParenIndex("f((", 1));
	}
	// ------------------------------------------------------------------
	// ProcessEngine.stringValueOf: the persisted string form of a value
	// ------------------------------------------------------------------

	/**
	 * JavaScript has no integer type, so a service task doing {@code $.process.amount = 25000}
	 * hands the engine a Double. Persisting that with toString() stored "25000.0", which is
	 * what REST clients, the UI and string comparisons then saw.
	 */
	@Test
	public void testWholeValuedNumbersArePersistedWithoutFraction() {

		assertEquals("25000", ProcessEngine.stringValueOf(25000.0d));
		assertEquals("25000", ProcessEngine.stringValueOf(25000.0f));
		assertEquals("0",     ProcessEngine.stringValueOf(0.0d));
		assertEquals("-7",    ProcessEngine.stringValueOf(-7.0d));
	}

	/** A real fractional value keeps every digit it had. */
	@Test
	public void testFractionalValuesAreUntouched() {

		assertEquals("0.5",      ProcessEngine.stringValueOf(0.5d));
		assertEquals("25000.25", ProcessEngine.stringValueOf(25000.25d));
	}

	/** Non-numeric and already-integral types pass straight through. */
	@Test
	public void testOtherTypesPassThrough() {

		assertEquals("25000", ProcessEngine.stringValueOf(25000));
		assertEquals("25000", ProcessEngine.stringValueOf(25000L));
		assertEquals("true",  ProcessEngine.stringValueOf(Boolean.TRUE));
		assertEquals("text",  ProcessEngine.stringValueOf("text"));
		assertNull(ProcessEngine.stringValueOf(null));
	}

	/**
	 * Beyond 2^53 a double no longer represents every integer, and NaN / infinity have no
	 * integral form at all -- those keep their own representation rather than being silently
	 * rounded into a long.
	 */
	@Test
	public void testValuesOutsideTheExactIntegerRangeAreUntouched() {

		assertEquals("1.0E17", ProcessEngine.stringValueOf(1.0E17d));
		assertEquals("NaN",    ProcessEngine.stringValueOf(Double.NaN));
		assertEquals("Infinity", ProcessEngine.stringValueOf(Double.POSITIVE_INFINITY));
	}

}
