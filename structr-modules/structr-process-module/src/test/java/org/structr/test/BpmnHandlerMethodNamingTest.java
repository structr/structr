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
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.process.bpmn.BpmnHandlerNames;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Handler-method naming: the scheme itself, and the process editor's path into it.
 *
 * <p>Handler graph names are scoped to (process, version, element) so that the same authored
 * name can occur on another element, another process or another version without colliding in
 * the global user-function namespace -- see {@link BpmnHandlerNames}. The editor must not
 * build those names itself, so it goes through {@code ensureHandlerMethod} on
 * BpmnElement/BpmnProcess; these tests drive that entry point directly, since the JavaScript
 * that calls it is not otherwise covered.</p>
 */
public class BpmnHandlerMethodNamingTest extends AbstractProcessEngineTest {

	@Test
	public void testQualifiedNameRoundTripsAndStaysAValidMethodName() {

		final String elementHandler = BpmnHandlerNames.qualify("onCreate", "Process_MC", "2", "UserTask_1");
		final String processHandler = BpmnHandlerNames.qualify("onProcessStarted", "Process_MC", "2", null);

		assertEquals("the authored name must be recoverable without any context", "onCreate", BpmnHandlerNames.authoredOf(elementHandler));
		assertEquals("the authored name must be recoverable without any context", "onProcessStarted", BpmnHandlerNames.authoredOf(processHandler));

		assertTrue("a qualified name must still be a valid Structr method name: " + elementHandler, elementHandler.matches(SchemaMethod.schemaMethodNamePattern));
		assertTrue("a qualified name must still be a valid Structr method name: " + processHandler, processHandler.matches(SchemaMethod.schemaMethodNamePattern));

		assertTrue("the element component must take part in the scope", elementHandler.contains("UserTask_1"));
		assertNotSame("element and process handlers of one process must not collide", elementHandler, processHandler);

		// BPMN ids are NCNames and may carry characters that are illegal in a method name.
		final String sanitized = BpmnHandlerNames.qualify("onCreate", "order-process.v2", "1", "User Task:1");

		assertTrue("scope components must be reduced to name-safe characters: " + sanitized, sanitized.matches(SchemaMethod.schemaMethodNamePattern));
		assertEquals("onCreate", BpmnHandlerNames.authoredOf(sanitized));

		// Anything not carrying a scope is passed through untouched: user-authored global
		// functions and methodRef targets must never be rewritten.
		assertEquals("refMethod", BpmnHandlerNames.authoredOf("refMethod"));
		assertEquals("an unqualified name must be left alone", "refMethod", BpmnHandlerNames.qualify("refMethod", null, null, null));
	}

	/**
	 * The editor bug this closes: adding a listener with the SAME authored name to two
	 * different processes used to fail on commit with already_exists, because the editor
	 * created the SchemaMethod client-side under its bare authored name.
	 */
	@Test
	public void testEditorMayAddTheSameHandlerNameToTwoProcesses() throws Exception {

		final String procA = importProcess("/methodref-other-process.bpmn");
		final String procB = importProcess("/bug-methods-clobber.bpmn");

		final String idA;
		final String idB;

		try (final Tx tx = app.tx()) {

			final NodeInterface taskA = elementByBpmnId(app.getNodeById(procA), "UserTask_2");
			final NodeInterface taskB = elementByBpmnId(app.getNodeById(procB), "UserTask_1");

			assertNotNull("UserTask_2 not imported", taskA);
			assertNotNull("UserTask_1 not imported", taskB);

			idA = (String) invokeMethod(securityContext, taskA, "ensureHandlerMethod", Map.of("name", "editorHandler"), true);
			idB = (String) invokeMethod(securityContext, taskB, "ensureHandlerMethod", Map.of("name", "editorHandler"), true);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("adding the same authored handler name to two processes must work: " + fex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface a = app.getNodeById(idA);
			final NodeInterface b = app.getNodeById(idB);

			assertNotNull(a);
			assertNotNull(b);
			assertNotSame("each process must get its own handler method", idA, idB);

			assertEquals("editorHandler", BpmnHandlerNames.authoredOf(a.getName()));
			assertEquals("editorHandler", BpmnHandlerNames.authoredOf(b.getName()));
			assertTrue("the editor path must produce a scoped graph name, not the bare authored name: " + a.getName(), BpmnHandlerNames.isQualified(a.getName()));
			assertTrue("the two graph names must differ", !a.getName().equals(b.getName()));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * The editor keeps an auto-named handler in sync with its (event, phase). That rename
	 * must be re-scoped by the backend; writing the bare authored name would strip the scope
	 * and re-open the collision.
	 */
	@Test
	public void testEditorRenameKeepsTheScope() throws Exception {

		final String procUuid = importProcess("/bug-methods-clobber.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface task = elementByBpmnId(app.getNodeById(procUuid), "UserTask_1");
			assertNotNull("UserTask_1 not imported", task);

			final String methodId = (String) invokeMethod(securityContext, task, "ensureHandlerMethod", Map.of("name", "taskAfterCreated"), true);
			assertNotNull(methodId);

			// same call with an existing method id renames in place
			final String renamedId = (String) invokeMethod(securityContext, task, "ensureHandlerMethod", Map.of("name", "taskAfterCompleted", "method", methodId), true);

			assertEquals("rename must act on the same method node", methodId, renamedId);

			final NodeInterface method = app.getNodeById(methodId);
			final String graphName     = method.getProperty(method.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			assertEquals("the authored name must be the new one", "taskAfterCompleted", BpmnHandlerNames.authoredOf(graphName));
			assertTrue("the renamed handler must still be scoped: " + graphName, BpmnHandlerNames.isQualified(graphName));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}

	/** Asking twice for the same handler must not duplicate it -- the method is an "ensure". */
	@Test
	public void testEnsureHandlerMethodIsIdempotent() throws Exception {

		final String procUuid = importProcess("/bug-methods-clobber.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface task = elementByBpmnId(app.getNodeById(procUuid), "UserTask_1");
			assertNotNull("UserTask_1 not imported", task);

			final String first  = (String) invokeMethod(securityContext, task, "ensureHandlerMethod", Map.of("name", "idempotentHandler"), true);
			final String second = (String) invokeMethod(securityContext, task, "ensureHandlerMethod", Map.of("name", "idempotentHandler"), true);

			assertEquals("a second ensure must return the existing handler", first, second);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}

	/** The process-level counterpart, used by the editor's process-listener panel. */
	@Test
	public void testEditorMayAddProcessLevelHandler() throws Exception {

		final String procUuid = importProcess("/bug-methods-clobber.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface proc = app.getNodeById(procUuid);
			final String methodId    = (String) invokeMethod(securityContext, proc, "ensureHandlerMethod", Map.of("name", "processStartedHandler"), true);

			assertNotNull(methodId);

			final NodeInterface method = app.getNodeById(methodId);
			final String graphName     = method.getProperty(method.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			assertEquals("processStartedHandler", BpmnHandlerNames.authoredOf(graphName));
			assertTrue("a process handler must be scoped too: " + graphName, BpmnHandlerNames.isQualified(graphName));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}
}
