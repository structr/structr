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

import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;
import org.structr.process.traits.definitions.TaskInstanceTraitDefinition;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Tests the process- and task-listener dispatch of the
 * {@link org.structr.process.engine.ProcessEngine}: listeners bound to lifecycle
 * events run their SchemaMethod, and an {@code on} (pre-commit) listener that
 * throws vetoes the transition and rolls it back.
 *
 * <p>The fixtures declare {@code <structr:taskListener>} / {@code <structr:processListener>}
 * elements; the importer creates empty SchemaMethods for them by name, and each test
 * fills in an observable body (creating a marker node).</p>
 */
public class ProcessEngineListenerTest extends AbstractProcessEngineTest {

	@Test
	public void testProcessAndTaskListenersFire() throws Exception {

		final String procUuid = importProcess("/engine-listeners.bpmn");

		// Give the importer-created listener methods an observable body.
		setMethodSource("onProcessStarted",   "{ $.create('TestOne', { name: 'evt-processStarted' }); }");
		setMethodSource("onProcessCompleted", "{ $.create('TestOne', { name: 'evt-processCompleted' }); }");
		setMethodSource("onTaskCreated",      "{ $.create('TestOne', { name: 'evt-taskCreated' }); }");
		setMethodSource("onTaskCompleted",    "{ $.create('TestOne', { name: 'evt-taskCompleted' }); }");

		final String instId;
		try (final Tx tx = app.tx()) {
			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// 'created' (task) and 'started' (process) fired during start.
		try (final Tx tx = app.tx()) {
			assertMarker("evt-taskCreated");
			assertMarker("evt-processStarted");
			assertNoMarker("evt-taskCompleted");
			assertNoMarker("evt-processCompleted");
			tx.success();
		}

		try (final Tx tx = app.tx()) {
			engine().completeTask(openTaskAt(app.getNodeById(instId), "Task_1"), Map.of());
			tx.success();
		}

		// 'completed' (task) and 'completed' (process, via the OnModification hook) fired.
		try (final Tx tx = app.tx()) {
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			assertMarker("evt-taskCompleted");
			assertMarker("evt-processCompleted");
			tx.success();
		}
	}

	@Test
	public void testOnPhaseListenerVetoRollsBackCompletion() throws Exception {

		final String procUuid = importProcess("/engine-listener-veto.bpmn");
		setMethodSource("vetoComplete", "{ throw new Error('veto: not allowed'); }");

		final String instId;
		try (final Tx tx = app.tx()) {
			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// The pre-commit listener throws -> the whole completion transaction rolls back.
		try (final Tx tx = app.tx()) {
			try {
				engine().completeTask(openTaskAt(app.getNodeById(instId), "Task_1"), Map.of());
				fail("expected the vetoing listener to abort completion");
			} catch (final Exception expected) {
				// veto propagated as expected
			}
			// intentionally NOT calling tx.success() -> rollback
		}

		// Nothing advanced: the task is still open and the instance still running.
		try (final Tx tx = app.tx()) {
			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			final NodeInterface task = openTaskAt(inst, "Task_1");
			assertNotNull("the task must remain open after a vetoed completion", task);
			assertFalse("task must not be completed",
				TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(taskStatus(task)));
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private void setMethodSource(final String methodName, final String source) throws Exception {
		try (final Tx tx = app.tx()) {
			final NodeInterface method = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).getFirst();
			assertNotNull("importer should have created a SchemaMethod named " + methodName, method);
			method.setProperty(method.getTraits().key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source);
			tx.success();
		}
	}

	private void assertMarker(final String name) throws Exception {
		assertEquals("expected exactly one '" + name + "' marker from a listener",
			1, app.nodeQuery("TestOne").name(name).getAsList().size());
	}

	private void assertNoMarker(final String name) throws Exception {
		assertTrue("did not expect a '" + name + "' marker yet",
			app.nodeQuery("TestOne").name(name).getAsList().isEmpty());
	}
}
