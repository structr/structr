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
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.process.ProcessTraits;
import org.structr.process.traits.definitions.*;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Execution-semantics tests for the {@link org.structr.process.engine.ProcessEngine}:
 * token flow, all three gateway kinds, sub-processes, intermediate catch/signal,
 * timers (intermediate + boundary), script tasks, parameter/subject routing and
 * the process lifecycle (suspend / resume / terminate / complete).
 *
 * Human-task assignment and the task lifecycle live in {@link ProcessEngineTaskTest};
 * listeners live in {@link ProcessEngineListenerTest}.
 */
public class ProcessEngineExecutionTest extends AbstractProcessEngineTest {

	// ==================================================================
	// Basic flow + exclusive gateway (simple-approval.bpmn)
	// ==================================================================

	@Test
	public void testStartCreatesInstanceTokenAndUserTask() throws Exception {

		final String procUuid = importProcess("/simple-approval.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			// Instance is running, waiting at the user task.
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			assertEquals("token should wait at UserTask_1", List.of("UserTask_1"), waitingTokenElementIds(inst));

			// Exactly one task instance was created for the user task.
			assertEquals(1, tasks(inst).size());
			final NodeInterface task = openTaskAt(inst, "UserTask_1");
			assertNotNull(task);
			// No performers declared, no initiator-fallback flag -> status "created".
			assertEquals(TaskInstanceTraitDefinition.STATUS_CREATED, taskStatus(task));

			tx.success();
		}
	}

	@Test
	public void testExclusiveGatewayApprovedPath() throws Exception {

		final String procUuid = importProcess("/simple-approval.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Complete the review task with approved=true -> approved branch -> end.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "UserTask_1"), Map.of("approved", true));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			// 'approved' had no matching subject (none attached) -> stored as a parameter value.
			assertEquals("true", parameterValues(inst).get("approved"));
			// No tokens remain active/waiting.
			assertEquals(0, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_ACTIVE));
			assertEquals(0, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_WAITING));
			tx.success();
		}
	}

	@Test
	public void testExclusiveGatewayRejectedPath() throws Exception {

		final String procUuid = importProcess("/simple-approval.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "UserTask_1"), Map.of("approved", false));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			assertEquals("false", parameterValues(inst).get("approved"));
			tx.success();
		}
	}

	@Test
	public void testExclusiveGatewayNoMatchingFlowFails() throws Exception {

		final String procUuid = importProcess("/simple-approval.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Completing without 'approved' leaves both conditions false and there is no
		// default flow -> the exclusive gateway raises a 422.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			try {

				engine().completeTask(openTaskAt(inst, "UserTask_1"), Map.of());
				fail("expected FrameworkException when no outgoing path matches");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}

	@Test
	public void testCompleteAlreadyCompletedTaskFails() throws Exception {

		final String procUuid = importProcess("/simple-approval.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		final String taskId;

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			final NodeInterface task = openTaskAt(inst, "UserTask_1");

			taskId = task.getUuid();
			engine().completeTask(task, Map.of("approved", true));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				engine().completeTask(app.getNodeById(taskId), Map.of("approved", true));
				fail("expected FrameworkException completing an already-completed task");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}

	// ==================================================================
	// Exclusive gateway: default flow + implicit XOR (engine-exclusive-default / -implicit-xor)
	// ==================================================================

	@Test
	public void testExclusiveGatewayDefaultFlowTaken() throws Exception {

		final String procUuid = importProcess("/engine-exclusive-default.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// approved=false -> the single conditional (approved==true) fails -> the
		// gateway's declared default flow is taken -> reject branch.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Review"), Map.of("approved", false));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull("default flow should route to the reject task", openTaskAt(inst, "Task_Reject"));
			assertNull(anyTaskAt(inst, "Task_Approve"));
			tx.success();
		}
	}

	@Test
	public void testExclusiveGatewayConditionalWinsOverDefault() throws Exception {

		final String procUuid = importProcess("/engine-exclusive-default.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// approved=true -> the conditional flow matches and is preferred over the default.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Review"), Map.of("approved", true));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull(openTaskAt(inst, "Task_Approve"));
			assertNull(anyTaskAt(inst, "Task_Reject"));
			tx.success();
		}
	}

	@Test
	public void testImplicitExclusiveGatewayRouting() throws Exception {

		// A user task with two conditional outgoing flows and no gateway: the engine
		// routes it as an implicit exclusive gateway.
		final String procUuid = importProcess("/engine-implicit-xor.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Decide"), Map.of("route", "a"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull(openTaskAt(inst, "Task_A"));
			assertNull(anyTaskAt(inst, "Task_B"));
			tx.success();
		}
	}

	// ==================================================================
	// Parallel gateway fork / join (engine-parallel.bpmn)
	// ==================================================================

	@Test
	public void testParallelForkAndJoin() throws Exception {

		final String procUuid = importProcess("/engine-parallel.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// After the fork: two parallel user tasks are open, instance still running.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			assertNotNull(openTaskAt(inst, "Task_A"));
			assertNotNull(openTaskAt(inst, "Task_B"));
			assertEquals(2, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_WAITING));
			tx.success();
		}

		// Completing the first branch leaves the join waiting.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_A"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			tx.success();
		}

		// Completing the second branch satisfies the join -> process completes.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_B"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			assertEquals(0, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_WAITING));
			tx.success();
		}
	}

	// ==================================================================
	// Inclusive gateway (engine-inclusive.bpmn)
	// ==================================================================

	@Test
	public void testInclusiveGatewayBothBranches() throws Exception {

		final String procUuid = importProcess("/engine-inclusive.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null, Map.of("branchA", true, "branchB", true)).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull("branch A taken", openTaskAt(inst, "Task_A"));
			assertNotNull("branch B taken", openTaskAt(inst, "Task_B"));
			// The default service branch must NOT have run.
			assertNull(anyTaskAt(inst, "Service_C"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_A"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			// join still waiting for branch B
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			engine().completeTask(openTaskAt(inst, "Task_B"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	@Test
	public void testInclusiveGatewaySingleBranch() throws Exception {

		final String procUuid = importProcess("/engine-inclusive.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null, Map.of("branchA", true, "branchB", false)).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull(openTaskAt(inst, "Task_A"));
			assertNull(openTaskAt(inst, "Task_B"));
			// only one in-flight token -> completing A satisfies the inclusive join
			engine().completeTask(openTaskAt(inst, "Task_A"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	@Test
	public void testInclusiveGatewayDefaultBranch() throws Exception {

		final String procUuid = importProcess("/engine-inclusive.bpmn");
		final String instId;

		// No parameters -> neither condition matches -> the default (service) branch
		// runs and the process completes synchronously.
		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			assertNull(anyTaskAt(inst, "Task_A"));
			assertNull(anyTaskAt(inst, "Task_B"));
			tx.success();
		}
	}

	// ==================================================================
	// Sub-process (engine-subprocess.bpmn)
	// ==================================================================

	@Test
	public void testSubProcessWaitsAndResumesParent() throws Exception {

		final String procUuid = importProcess("/engine-subprocess.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Inside the sub-process: the inner user task is open, the parent token
		// waits on the sub-process element.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			assertNotNull(openTaskAt(inst, "Sub_Task"));
			assertTrue("parent token should wait on the sub-process", waitingTokenElementIds(inst).contains("Sub_1"));
			tx.success();
		}

		// Completing the inner task ends the sub-process, resumes the parent, runs the
		// following service task and reaches the end event.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Sub_Task"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	// ==================================================================
	// Intermediate catch event + signal (engine-catch-signal.bpmn)
	// ==================================================================

	@Test
	public void testIntermediateCatchEventWaitsForSignal() throws Exception {

		final String procUuid = importProcess("/engine-catch-signal.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			assertEquals(List.of("Catch_1"), waitingTokenElementIds(inst));
			assertTrue("no task for a catch event", tasks(inst).isEmpty());
			tx.success();
		}

		// Signalling the catch event resumes the token through to completion.
		try (final Tx tx = app.tx()) {

			engine().signalEvent(app.getNodeById(instId), "Catch_1", null);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	@Test
	public void testSignalUnknownElementFails() throws Exception {

		final String procUuid = importProcess("/engine-catch-signal.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				engine().signalEvent(app.getNodeById(instId), "does_not_exist", null);
				fail("expected FrameworkException for unknown catch event id");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}

	// ==================================================================
	// Intermediate timer (engine-timer-intermediate.bpmn)
	// ==================================================================

	@Test
	public void testIntermediateTimerSchedulesAndFires() throws Exception {

		final String procUuid = importProcess("/engine-timer-intermediate.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		final String timerId;

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(List.of("Timer_1"), waitingTokenElementIds(inst));

			final List<NodeInterface> pending = pendingTimers();
			assertEquals("one pending intermediate timer expected", 1, pending.size());
			final NodeInterface timer = pending.get(0);
			assertEquals(ProcessTimerTraitDefinition.TIMER_INTERMEDIATE, timer.getProperty(timer.getTraits().key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY)));
			timerId = timer.getUuid();
			tx.success();
		}

		// Fire the timer directly (as the ProcessTimerService would).
		try (final Tx tx = app.tx()) {

			engine().fireTimer(app.getNodeById(timerId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			assertEquals(ProcessTimerTraitDefinition.STATUS_FIRED, timerStatus(app.getNodeById(timerId)));
			tx.success();
		}
	}

	// ==================================================================
	// Boundary timer (engine-boundary-timer[-reminder].bpmn)
	// ==================================================================

	@Test
	public void testInterruptingBoundaryTimerCancelsTaskAndEscalates() throws Exception {

		final String procUuid = importProcess("/engine-boundary-timer.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		final String timerId;

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull(openTaskAt(inst, "Task_Work"));
			final List<NodeInterface> pending = pendingTimers();
			assertEquals(1, pending.size());
			assertEquals(ProcessTimerTraitDefinition.TIMER_BOUNDARY, pending.get(0).getProperty(pending.get(0).getTraits().key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY)));
			timerId = pending.get(0).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().fireTimer(app.getNodeById(timerId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			// interrupting: the task was cancelled and the process took the escalation path.
			final NodeInterface task = anyTaskAt(inst, "Task_Work");
			assertEquals(TaskInstanceTraitDefinition.STATUS_CANCELLED, taskStatus(task));
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			tx.success();
		}
	}

	@Test
	public void testCompletingTaskCancelsBoundaryTimer() throws Exception {

		final String procUuid = importProcess("/engine-boundary-timer.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Work"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));
			// The boundary timer must have been cancelled when the task completed normally.
			assertTrue("no pending timers should remain", pendingTimers().isEmpty());
			assertEquals(1, allTimers().size());
			assertEquals(ProcessTimerTraitDefinition.STATUS_CANCELLED, timerStatus(allTimers().get(0)));
			tx.success();
		}
	}

	@Test
	public void testNonInterruptingBoundaryTimerKeepsTaskRunning() throws Exception {

		final String procUuid = importProcess("/engine-boundary-timer-reminder.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		final String timerId;

		try (final Tx tx = app.tx()) {

			timerId = pendingTimers().get(0).getUuid();
			tx.success();
		}

		// Fire the reminder: the escalation branch runs but the task stays open.
		try (final Tx tx = app.tx()) {

			engine().fireTimer(app.getNodeById(timerId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals("task must survive a non-interrupting timer", TaskInstanceTraitDefinition.STATUS_CREATED, taskStatus(anyTaskAt(inst, "Task_Work")));
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			tx.success();
		}

		// Completing the task now finishes the process (the reminder branch already ended).
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Work"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	@Test
	public void testCancelBoundaryTimerByBpmnId() throws Exception {

		final String procUuid = importProcess("/engine-boundary-timer.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Explicitly cancel the armed boundary timer by its bpmnId (e.g. what a
		// 'claimed' listener does to stop an escalation timer).
		try (final Tx tx = app.tx()) {

			final NodeInterface task = openTaskAt(app.getNodeById(instId), "Task_Work");
			assertEquals(1, pendingTimers().size());

			final int cancelled = engine().cancelBoundaryTimerByBpmnId(task, "Boundary_Timeout");
			assertEquals(1, cancelled);
			// A non-matching id cancels nothing.
			assertEquals(0, engine().cancelBoundaryTimerByBpmnId(task, "Does_Not_Exist"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertTrue("timer must be cancelled", pendingTimers().isEmpty());
			assertEquals(ProcessTimerTraitDefinition.STATUS_CANCELLED, timerStatus(allTimers().get(0)));
			tx.success();
		}
	}

	@Test
	public void testFireTimerUnsupportedTypeGoesToErrorAndStops() throws Exception {

		// A timer whose type the engine does not implement must transition to a
		// terminal 'error' status (with a message) -- NOT stay pending and re-fire.
		final String timerId;

		try (final Tx tx = app.tx()) {

			final NodeInterface timer = app.create(ProcessTraits.PROCESS_TIMER);
			final var t = timer.getTraits();

			timer.setProperty(t.key(ProcessTimerTraitDefinition.STATUS_PROPERTY),     ProcessTimerTraitDefinition.STATUS_PENDING);
			timer.setProperty(t.key(ProcessTimerTraitDefinition.TIMER_TYPE_PROPERTY), ProcessTimerTraitDefinition.TIMER_START);
			timer.setProperty(t.key(ProcessTimerTraitDefinition.FIRE_AT_PROPERTY),    new Date());
			timerId = timer.getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().fireTimer(app.getNodeById(timerId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface timer = app.getNodeById(timerId);
			assertEquals(ProcessTimerTraitDefinition.STATUS_ERROR, timerStatus(timer));
			assertNotNull("an error message should be recorded", timer.getProperty(timer.getTraits().key(ProcessTimerTraitDefinition.ERROR_MESSAGE_PROPERTY)));
			// Terminal status means pollAndFire (which only selects 'pending') never re-fires it.
			assertTrue(pendingTimers().isEmpty());
			tx.success();
		}
	}

	// ==================================================================
	// Script tasks (engine-script[-error].bpmn)
	// ==================================================================

	@Test
	public void testScriptTaskExecutes() throws Exception {

		final String procUuid = importProcess("/engine-script.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			final List<NodeInterface> markers = app.nodeQuery("TestOne").name("script-marker").getAsList();
			assertEquals("script task should have created exactly one marker node", 1, markers.size());
			tx.success();
		}
	}

	@Test
	public void testScriptTaskErrorIsNonFatal() throws Exception {

		final String procUuid = importProcess("/engine-script-error.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			// A throwing script must not abort the process.
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	// ==================================================================
	// Parameter / subject routing (engine-human-task.bpmn + TestOne subject)
	// ==================================================================

	@Test
	public void testParameterRoutingSplitsSubjectAndParameterValues() throws Exception {

		final String procUuid = importProcess("/engine-human-task.bpmn");
		final NodeInterface initiator = createUser("initiator");
		final NodeInterface subject   = createTestSubject("the-subject");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engineAs(initiator).startProcess(app.getNodeById(procUuid), app.getNodeById(subject.getUuid())).getUuid();
			tx.success();
		}

		// Complete the task with one subject field (aString) and one non-subject field (note).
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_Fill"), Map.of("aString", "hello world", "note", "process only"));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(inst));

			// 'aString' matched a property of TestOne -> written to the subject.
			final NodeInterface subj = subjectOf(inst);
			assertNotNull(subj);
			assertEquals("hello world", subj.getProperty(subj.getTraits().key("aString")));

			// 'note' had no matching property -> stored as a parameter value.
			assertEquals("process only", parameterValues(inst).get("note"));
			assertFalse("subject field must not also become a parameter value", parameterValues(inst).containsKey("aString"));
			tx.success();
		}
	}

	// ==================================================================
	// Process lifecycle: suspend / resume / terminate
	// ==================================================================

	@Test
	public void testSuspendBlocksSignalThenResumeAllowsIt() throws Exception {

		final String procUuid = importProcess("/engine-catch-signal.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().suspendProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_SUSPENDED, instanceStatus(inst));
			// Signalling a suspended instance is rejected.
			try {

				engine().signalEvent(inst, "Catch_1", null);
				fail("expected FrameworkException signalling a suspended instance");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().resumeProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));
			engine().signalEvent(inst, "Catch_1", null);
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertEquals(ProcessInstanceTraitDefinition.STATUS_COMPLETED, instanceStatus(app.getNodeById(instId)));
			tx.success();
		}
	}

	@Test
	public void testTerminateConsumesWaitingTokens() throws Exception {

		final String procUuid = importProcess("/engine-catch-signal.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().terminateProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals(ProcessInstanceTraitDefinition.STATUS_TERMINATED, instanceStatus(inst));
			// The previously-waiting token was consumed (marked completed) without advancing.
			assertEquals(0, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_WAITING));
			assertEquals(0, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_ACTIVE));
			tx.success();
		}
	}

	@Test
	public void testInvalidLifecycleTransitionsAreRejected() throws Exception {

		final String procUuid = importProcess("/engine-catch-signal.bpmn");
		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// resume while running -> 422
		try (final Tx tx = app.tx()) {

			try {

				engine().resumeProcess(app.getNodeById(instId));
				fail("expected FrameworkException resuming a running instance");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}

		// terminate, then terminating again / suspending are rejected
		try (final Tx tx = app.tx()) {

			engine().terminateProcess(app.getNodeById(instId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			try {

				engine().terminateProcess(inst);
				fail("expected FrameworkException terminating a terminated instance");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			try {

				engine().suspendProcess(inst);
				fail("expected FrameworkException suspending a non-running instance");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}

	@Test
	public void testStartProcessWithoutStartEventFails() throws Exception {

		// A process definition with no start event cannot be started.
		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface def  = app.create(ProcessTraits.BPMN_DEFINITIONS, "empty-def");
			final NodeInterface proc = app.create(ProcessTraits.BPMN_PROCESS, "empty-proc");

			proc.setProperty(proc.getTraits().key(BpmnProcessTraitDefinition.DEFINITION_PROPERTY), def);
			procUuid = proc.getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			try {

				engine().startProcess(app.getNodeById(procUuid), null);
				fail("expected FrameworkException starting a process without a start event");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();
		}
	}
}
