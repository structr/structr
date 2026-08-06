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
			assertFalse("task must not be completed", TaskInstanceTraitDefinition.STATUS_COMPLETED.equals(taskStatus(task)));
			tx.success();
		}
	}

	@Test
	public void testStartedFiresBeforeCompletionForAutomaticProcess() throws Exception {

		// A fully-automatic process (start -> serviceTask -> end) runs straight to
		// completion during startProcess. The 'started' listener must still observe a
		// 'running' instance -- it must fire before the token advances to the end event,
		// not after the process has already completed.
		final String procUuid = importProcess("/engine-auto-listener.bpmn");
		setMethodSource("recordStartedStatus", "{ $.create('TestOne', { name: 'started-status-' + $.this.status }); }");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			// The 'started' listener saw status=running (fired before completion) ...
			assertEquals(1, app.nodeQuery("TestOne").name("started-status-running").getAsList().size());
			// ... not status=completed.
			assertTrue(app.nodeQuery("TestOne").name("started-status-completed").getAsList().isEmpty());
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Task lifecycle events (engine-task-events.bpmn)
	// ------------------------------------------------------------------

	@Test
	public void testTaskCreatedAndAvailableEventsFire() throws Exception {

		// A candidate task fires 'created' then 'available' on creation.
		startTaskEventsInstance();

		try (final Tx tx = app.tx()) {

			assertMarker("evt-taskCreated");
			assertMarker("evt-taskAvailable");
			tx.success();
		}
	}

	@Test
	public void testTaskClaimedAndAssignedEventsFire() throws Exception {

		final String instId = startTaskEventsInstance();

		try (final Tx tx = app.tx()) {

			final NodeInterface member = app.nodeQuery(StructrTraits.USER).name("reviewer").getFirst();
			engineAs(member).claimTask(openTaskAt(app.getNodeById(instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-taskClaimed");
			assertMarker("evt-taskAssigned");
			tx.success();
		}
	}

	@Test
	public void testTaskDeclinedEventFires() throws Exception {

		final String instId = startTaskEventsInstance();

		try (final Tx tx = app.tx()) {

			final NodeInterface member = app.nodeQuery(StructrTraits.USER).name("reviewer").getFirst();
			engineAs(member).declineTask(openTaskAt(app.getNodeById(instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-taskDeclined");
			tx.success();
		}
	}

	@Test
	public void testTaskCancelledEventFires() throws Exception {

		final String instId = startTaskEventsInstance();

		try (final Tx tx = app.tx()) {

			engine().cancelTask(openTaskAt(app.getNodeById(instId), "Task_Review"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-taskCancelled");
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// Process lifecycle events (engine-process-events.bpmn)
	// ------------------------------------------------------------------

	@Test
	public void testProcessSubjectAttachedEventFires() throws Exception {

		final String instId = startProcessEventsInstance();
		final NodeInterface subject = createTestSubject("the-subject");

		// Setting the subject relationship to a non-null target fires 'subjectAttached'
		// via the OnModification hook.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			inst.setProperty(inst.getTraits().key(ProcessInstanceTraitDefinition.SUBJECT_PROPERTY), app.getNodeById(subject.getUuid()));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-subjectAttached");
			tx.success();
		}
	}

	@Test
	public void testProcessSuspendedResumedTerminatedEventsFire() throws Exception {

		final String instId = startProcessEventsInstance();

		try (final Tx tx = app.tx()) {

			engine().suspendProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-suspended");
			engine().resumeProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-resumed");
			// the instance is running again -> terminate it
			engine().terminateProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertMarker("evt-terminated");
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	/** Import engine-task-events, wire the listener markers, create the group + member, start. */
	private String startTaskEventsInstance() throws Exception {

		final String procUuid = importProcess("/engine-task-events.bpmn");

		setMethodSource("onTaskCreated",   "{ $.create('TestOne', { name: 'evt-taskCreated' }); }");
		setMethodSource("onTaskAvailable", "{ $.create('TestOne', { name: 'evt-taskAvailable' }); }");
		setMethodSource("onTaskClaimed",   "{ $.create('TestOne', { name: 'evt-taskClaimed' }); }");
		setMethodSource("onTaskAssigned",  "{ $.create('TestOne', { name: 'evt-taskAssigned' }); }");
		setMethodSource("onTaskDeclined",  "{ $.create('TestOne', { name: 'evt-taskDeclined' }); }");
		setMethodSource("onTaskCancelled", "{ $.create('TestOne', { name: 'evt-taskCancelled' }); }");

		final NodeInterface group = createGroup("Reviewers");
		addToGroup(group, createUser("reviewer"));

		try (final Tx tx = app.tx()) {

			final String id = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();

			return id;
		}
	}

	/** Import engine-process-events, wire the listener markers, start (waits at the catch event). */
	private String startProcessEventsInstance() throws Exception {

		final String procUuid = importProcess("/engine-process-events.bpmn");

		setMethodSource("onSubjectAttached", "{ $.create('TestOne', { name: 'evt-subjectAttached' }); }");
		setMethodSource("onSuspended",       "{ $.create('TestOne', { name: 'evt-suspended' }); }");
		setMethodSource("onResumed",         "{ $.create('TestOne', { name: 'evt-resumed' }); }");
		setMethodSource("onTerminated",      "{ $.create('TestOne', { name: 'evt-terminated' }); }");

		try (final Tx tx = app.tx()) {

			final String id = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();

			return id;
		}
	}

	private void setMethodSource(final String methodName, final String source) throws Exception {

		try (final Tx tx = app.tx()) {

			final NodeInterface method = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(methodName).getFirst();
			assertNotNull("importer should have created a SchemaMethod named " + methodName, method);
			method.setProperty(method.getTraits().key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source);
			tx.success();
		}
	}

	private void assertMarker(final String name) throws Exception {

		assertEquals("expected exactly one '" + name + "' marker from a listener", 1, app.nodeQuery("TestOne").name(name).getAsList().size());
	}

	private void assertNoMarker(final String name) throws Exception {

		assertTrue("did not expect a '" + name + "' marker yet", app.nodeQuery("TestOne").name(name).getAsList().isEmpty());
	}
}
