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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.web.entity.dom.VisibilityMapping;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Drives a single ProcessInstance through a multi-type process and asserts that each step's
 * VisibilityMapping predicate ({@code evaluate}) is true ONLY while the token is actually at that
 * step. This is the coverage that was missing: the only prior {@code evaluate()} call passed a null
 * context (the no-instance case), so the live "which step's div shows" logic was untested.
 *
 * <p>The process ({@code visibility-multi-type.bpmn}) mixes six element types: startEvent, userTask,
 * serviceTask (pass-through), manualTask (pass-through), intermediate message catchEvent, endEvent.
 * The generator wires their divs to: startEvent -&gt; {@code no-instance}; userTask -&gt;
 * {@code task-available} / {@code task-reserved-by-me}; manualTask + catchEvent -&gt;
 * {@code token-waiting-here}.</p>
 *
 * <p>The catch-event div uses the step-scoped {@code token-waiting-here} predicate, so it shows
 * only while the token is actually parked at that step (Stage C) -- not, as the old instance-level
 * {@code process-awaiting-action} did, for every manual/catch div whenever the instance was merely
 * between actionable tasks. The Stage-C assertions ("catch div visible, manual div hidden") are the
 * regression guard for that fix.</p>
 */
public class BpmnVisibilityMappingEvaluationTest extends AbstractProcessEngineTest {

	@Test
	public void testVisibilityIsStepScopedAcrossStepTypes() throws FrameworkException {

		final NodeInterface worker = createUser("worker");

		final String procId;
		final String vmStart, vmFillAvail, vmFillMine, vmManual, vmWait, vmReviewAvail, vmReviewMine;

		// ----- setup: import, auto-assign user tasks to the initiator, create one mapping per step -----
		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new org.structr.process.bpmn.BpmnImporter(securityContext).importBpmn(loadResource("/visibility-multi-type.bpmn"));
			final NodeInterface proc    = firstProcess(defNode);
			procId = proc.getUuid();

			// so both user tasks reserve to the initiator on activation (task-reserved-by-me)
			proc.setProperty(proc.getTraits().key(BpmnProcessTraitDefinition.DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY), true);

			vmStart       = createMapping(proc, "Start_1",     VisibilityMappingTraitDefinition.STATE_NO_INSTANCE,             false);
			vmFillAvail   = createMapping(proc, "Task_Fill",   VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,          true);
			vmFillMine    = createMapping(proc, "Task_Fill",   VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME,     true);
			vmManual      = createMapping(proc, "Task_Manual", VisibilityMappingTraitDefinition.STATE_TOKEN_WAITING_HERE,      true);
			vmWait        = createMapping(proc, "Event_Wait",  VisibilityMappingTraitDefinition.STATE_TOKEN_WAITING_HERE,      true);
			vmReviewAvail = createMapping(proc, "Task_Review", VisibilityMappingTraitDefinition.STATE_TASK_AVAILABLE,          true);
			vmReviewMine  = createMapping(proc, "Task_Review", VisibilityMappingTraitDefinition.STATE_TASK_RESERVED_BY_ME,     true);

			tx.success();
		}

		final SecurityContext ctx = userContext(worker);

		// ===== Stage A: no instance yet -> only the start (no-instance) div is visible =====
		try (final Tx tx = app.tx()) {
			assertTrue("start div visible when there is no instance in context", visible(vmStart, ctx, null));
			tx.success();
		}

		// ===== Stage B: start -> token waits at Task_Fill =====
		final String instId;
		try (final Tx tx = app.tx()) {
			instId = engineAs(worker).startProcess(app.getNodeById(procId), null).getUuid();
			tx.success();
		}
		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.getNodeById(instId);
			assertEquals("token waits at the first user task", List.of("Task_Fill"), waitingTokenElementIds(instance));

			assertTrue ("Task_Fill div visible at Task_Fill", userTaskVisible(vmFillAvail, vmFillMine, ctx, instance));
			assertFalse("start div hidden once an instance is in context",           visible(vmStart,  ctx, instance));
			assertFalse("Review div hidden while token is at Task_Fill",             userTaskVisible(vmReviewAvail, vmReviewMine, ctx, instance));
			assertFalse("manual div hidden while I have an active task (Task_Fill)",  visible(vmManual, ctx, instance));
			assertFalse("catch div hidden while I have an active task (Task_Fill)",   visible(vmWait,   ctx, instance));

			tx.success();
		}

		// ===== Stage C: complete Task_Fill -> passes serviceTask + manualTask -> waits at the catch event =====
		try (final Tx tx = app.tx()) {
			engineAs(worker).completeTask(openTaskAt(app.getNodeById(instId), "Task_Fill"), Map.of());
			tx.success();
		}
		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.getNodeById(instId);
			assertEquals("token passes the pass-through steps and waits at the catch event",
				List.of("Event_Wait"), waitingTokenElementIds(instance));

			assertTrue ("catch-event div visible while the token waits there", visible(vmWait,   ctx, instance));
			assertFalse("Task_Fill div hidden once completed",                 userTaskVisible(vmFillAvail, vmFillMine, ctx, instance));
			assertFalse("Review div hidden before the token reaches it",       userTaskVisible(vmReviewAvail, vmReviewMine, ctx, instance));

			// Regression guard for the step-scoping fix: the token is at Event_Wait, NOT at
			// Task_Manual, so the manualTask div must be hidden. With the old instance-level
			// process-awaiting-action this was wrongly visible; token-waiting-here is step-scoped
			// (reads boundStepBpmnId), so only the div whose step the token rests at lights up.
			assertFalse("manualTask div must be hidden when the token is at the catch event", visible(vmManual, ctx, instance));

			tx.success();
		}

		// ===== Stage D: deliver the message -> token advances to Task_Review =====
		try (final Tx tx = app.tx()) {
			engineAs(worker).signalEvent(app.getNodeById(instId), "Event_Wait", Map.of());
			tx.success();
		}
		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.getNodeById(instId);
			assertEquals("token advances to the second user task", List.of("Task_Review"), waitingTokenElementIds(instance));

			assertTrue ("Review div visible at Task_Review",                 userTaskVisible(vmReviewAvail, vmReviewMine, ctx, instance));
			assertFalse("catch div hidden once past the catch event",        visible(vmWait,   ctx, instance));
			assertFalse("manual div hidden at Task_Review",                  visible(vmManual, ctx, instance));

			tx.success();
		}
	}

	// ----- helpers -----

	/** Create a VisibilityMapping bound to the step (and optionally the process) with the given state. */
	private String createMapping(final NodeInterface process, final String stepBpmnId, final String state, final boolean bindProcess) throws FrameworkException {

		final Traits t          = Traits.of(ProcessTraits.VISIBILITY_MAPPING);
		final NodeInterface step = elementByBpmnId(process, stepBpmnId);

		final NodeInterface vm = bindProcess
			? app.create(ProcessTraits.VISIBILITY_MAPPING,
				new NodeAttribute<>(t.key(VisibilityMappingTraitDefinition.VISIBLE_WHEN_PROPERTY),   state),
				new NodeAttribute<>(t.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY),      step),
				new NodeAttribute<>(t.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY),   process))
			: app.create(ProcessTraits.VISIBILITY_MAPPING,
				new NodeAttribute<>(t.key(VisibilityMappingTraitDefinition.VISIBLE_WHEN_PROPERTY),   state),
				new NodeAttribute<>(t.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY),      step));

		return vm.getUuid();
	}

	private boolean visible(final String vmUuid, final SecurityContext ctx, final NodeInterface instance) throws FrameworkException {
		return app.getNodeById(vmUuid).as(VisibilityMapping.class).evaluate(ctx, instance);
	}

	/** A user-task div is visible when its available OR its reserved-by-me mapping matches (the generator OR-combines them). */
	private boolean userTaskVisible(final String vmAvail, final String vmMine, final SecurityContext ctx, final NodeInterface instance) throws FrameworkException {
		return visible(vmAvail, ctx, instance) || visible(vmMine, ctx, instance);
	}
}
