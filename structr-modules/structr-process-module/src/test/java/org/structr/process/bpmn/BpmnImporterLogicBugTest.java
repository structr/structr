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
package org.structr.process.bpmn;

import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static org.structr.process.bpmn.BpmnImporter.camundaListenerBody;
import static org.structr.process.bpmn.BpmnImporter.csvToFunctionExpression;
import static org.structr.process.bpmn.BpmnImporter.sanitizeMethodName;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * FAILING reproduction for pure-logic findings in {@link BpmnImporter}. Asserts
 * intended behaviour and currently FAILS. No graph / database needed.
 */
public class BpmnImporterLogicBugTest {

	private static final Pattern VALID = Pattern.compile("[a-z_][a-zA-Z0-9_]*");

	/**
	 * A Camunda listener's {@code expression}/{@code class}/{@code delegateExpression}
	 * payload is not a valid SchemaMethod name; importing such a document used to fail
	 * with a "must_match" validation error. {@code sanitizeMethodName} must always
	 * produce a valid identifier while leaving genuine method names untouched.
	 */
	@Test
	public void testSanitizeMethodNameProducesValidIdentifiers() {

		// Already-valid Structr method names pass through unchanged.
		assertEquals("notify",   sanitizeMethodName("notify"));
		assertEquals("onCreate", sanitizeMethodName("onCreate"));
		assertEquals("_private",  sanitizeMethodName("_private"));

		// Camunda expression -> last invoked method name.
		assertEquals("notifyReviewer", sanitizeMethodName("${notificationService.notifyReviewer(task)}"));
		assertEquals("setVariable",    sanitizeMethodName("${execution.setVariable('x', 1)}"));

		// FQCN / bean reference -> last dotted segment, decapitalised.
		assertEquals("notifyDelegate", sanitizeMethodName("com.example.NotifyDelegate"));
		assertEquals("myListenerBean", sanitizeMethodName("${myListenerBean}"));

		// Degenerate inputs still yield a valid identifier.
		assertEquals("listener", sanitizeMethodName("${}"));
		assertTrue("a digit-leading payload must be prefixed", VALID.matcher(sanitizeMethodName("123abc")).matches());

		assertNull(sanitizeMethodName(null));
		assertNull(sanitizeMethodName("   "));
	}

	/**
	 * A call nested in an argument list describes the argument, not the action, so the
	 * name must come from the OUTER call. Taking the textually last call turned
	 * {@code ${execution.setVariable('startedAt', now())}} into a handler named
	 * {@code now} -- which, having no schemaNode, landed in the user-defined function
	 * namespace and shadowed the builtin {@code now()}.
	 */
	@Test
	public void testSanitizeMethodNamePrefersTheOuterCall() {

		// nested call in an argument: the outer call wins
		assertEquals("setVariable", sanitizeMethodName("${execution.setVariable('startedAt', now())}"));
		assertEquals("setVariable", sanitizeMethodName("${execution.setVariable('score', calc(a, b(c)))}"));

		// chained calls are all at the outer level, so the final one still wins
		assertEquals("doThing", sanitizeMethodName("${svc.get().doThing()}"));

		// a paren inside a string literal must not shift the nesting level
		assertEquals("setVariable", sanitizeMethodName("${execution.setVariable('a(b', now())}"));

		// multi-statement script: the last outer-level call wins
		assertEquals("setVariable", sanitizeMethodName("execution.setVariable(\"approved\", true); execution.setVariable(\"role\", role(x));"));

		// unchanged: a single call, and a payload with no call at all
		assertEquals("notifyReviewer", sanitizeMethodName("${notificationService.notifyReviewer(task)}"));
		assertEquals("notifyDelegate", sanitizeMethodName("com.example.NotifyDelegate"));
	}

	/**
	 * A Camunda listener payload must become a runnable Structr method body: an
	 * expression is transpiled and wrapped as a JS body ({@code { ... }}) so it runs
	 * as JavaScript with $.process (not StructrScript, which chokes on the ${ }); a
	 * bare class/bean reference becomes an inert JS body.
	 */
	@Test
	public void testCamundaListenerBodyProducesRunnableJs() {

		// execution.setVariable -> $.process write, wrapped as a JS method body.
		final String setVar = camundaListenerBody("${execution.setVariable('startedAt', 5)}");
		assertTrue("must be a JS method body ({...}); got:\n" + setVar, setVar.startsWith("{") && setVar.endsWith("}"));
		assertTrue("execution.setVariable must transpile to a $.process write; got:\n" + setVar,
			setVar.contains("$.process.startedAt = 5"));

		// A bean/service call is rewritten to a Structr service-class call: the receiver
		// becomes a capitalized $.<Type> and the method is preserved. The importer scaffolds
		// NotificationService with a static notifyReviewer stub so this runs.
		final String call = camundaListenerBody("${notificationService.notifyReviewer(task)}");
		assertTrue(call.startsWith("{") && call.endsWith("}"));
		assertTrue("bean call should bind to a service class; got:\n" + call,
			call.contains("$.NotificationService.notifyReviewer(task)"));

		// A class / delegate reference has no Structr equivalent -> inert body.
		final String clazz = camundaListenerBody("com.acme.NotifyDelegate");
		assertTrue("class ref must be an inert JS body; got:\n" + clazz, clazz.startsWith("{") && clazz.endsWith("}"));
		assertTrue(clazz.contains("no Structr equivalent"));
	}

	/** Every non-blank payload must map to something the SchemaMethod name validator accepts. */
	@Test
	public void testSanitizeMethodNameAlwaysMatchesPattern() {

		for (final String payload : new String[] {
			"${notificationService.notifyReviewer(task)}",
			"com.acme.listeners.Audit$Inner",
			"${bean}",
			"do-a-thing!!!",
			"42",
			"a.b.c.d"
		}) {
			final String name = sanitizeMethodName(payload);
			assertTrue("sanitized '" + payload + "' -> '" + name + "' must be a valid method name",
				VALID.matcher(name).matches());
		}
	}

	/**
	 * IMP-6: {@code csvToFunctionExpression} splits on <em>every</em> comma, so a
	 * single {@code ${...}} expression that itself contains a comma is torn apart
	 * (the second half is then wrapped as {@code user(b)}}), corrupting the
	 * resulting performer expression.
	 */
	@Test
	public void testCommaBearingExpressionIsNotSplit() {

		// One candidate expression, a function call with two args -- must stay intact.
		final String result = csvToFunctionExpression("${fn(a,b)}", "user");

		assertEquals("A single ${...} expression containing a comma must not be split", "${fn(a,b)}", result);
	}
}
