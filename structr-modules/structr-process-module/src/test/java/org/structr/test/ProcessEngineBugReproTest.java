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
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;
import org.structr.process.traits.definitions.ProcessTokenTraitDefinition;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * FAILING reproductions for the ProcessEngine execution findings from the
 * code review. Each test drives a small BPMN fixture and asserts the CORRECT
 * (intended) behaviour; every test currently FAILS because the bug is present,
 * pinning the defect so that a fix turns the test green.
 *
 * <p>All branches used to trigger the depth-first-spawning bugs (PE-1, PE-2,
 * PE-5) are AUTOMATIC (manualTask pass-through), so the whole reproduction runs
 * inside a single {@code startProcess} call.</p>
 */
public class ProcessEngineBugReproTest extends AbstractProcessEngineTest {

	/**
	 * PE-1: Inclusive-gateway join misbehaves with automatic branches. Tokens are
	 * spawned/advanced depth-first, so branch A reaches the join before branch B
	 * exists -> the join fires with only A, and B is then stranded waiting at the
	 * join. The join must instead synchronise both branches into exactly ONE
	 * continuing token (at Task_After), with nothing left at Join_1.
	 */
	@Test
	public void testInclusiveJoinSynchronisesAutomaticBranches() throws Exception {

		final String procUuid = importProcess("/engine-bug-inclusive-auto.bpmn");

		final String instId;
		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertFalse("inclusive join stranded a token at the gateway (fired before both branches arrived)",
				waitingTokenElementIds(inst).contains("Join_1"));
			assertEquals("inclusive join must produce exactly one continuing token",
				1, tokenCount(inst, ProcessTokenTraitDefinition.STATUS_WAITING));
			assertNotNull("the single continuing token should wait at Task_After", openTaskAt(inst, "Task_After"));

			tx.success();
		}
	}

	/**
	 * PE-2: A parallel fork whose first (automatic) branch reaches a top-level end
	 * event completes the whole instance prematurely -- because the sibling branch
	 * has not been spawned yet when checkProcessCompletion runs. The instance must
	 * stay RUNNING while branch B's user task is still open.
	 */
	@Test
	public void testParallelBranchToEndEventDoesNotCompleteInstance() throws Exception {

		final String procUuid = importProcess("/engine-bug-parallel-endevent.bpmn");

		final String instId;
		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertNotNull("branch B's user task must be open", openTaskAt(inst, "Task_B"));
			assertEquals("instance must stay RUNNING until all parallel branches finish, not complete on the first end event",
				ProcessInstanceTraitDefinition.STATUS_RUNNING, instanceStatus(inst));

			tx.success();
		}
	}

	/**
	 * PE-4: Suspending an instance must stop advancement, but completeTask never
	 * checks instance status -- so completing a task on a suspended instance
	 * advances the token anyway. After suspend + complete, the token must NOT have
	 * moved from Task_A to Task_B.
	 */
	@Test
	public void testSuspendedInstanceDoesNotAdvanceOnTaskCompletion() throws Exception {

		final String procUuid = importProcess("/engine-bug-two-usertasks.bpmn");

		final String instId;
		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			engine().suspendProcess(app.getNodeById(instId));
			tx.success();
		}

		FrameworkException rejected = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_A"), Map.of());
			tx.success();

		} catch (final FrameworkException fe) {

			rejected = fe;
		}

		assertNotNull("completing a task on a suspended instance must be rejected", rejected);

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertNull("a suspended instance must not open the next task", anyTaskAt(inst, "Task_B"));
			assertFalse("a suspended instance must not advance the token to Task_B",
				waitingTokenElementIds(inst).contains("Task_B"));

			tx.success();
		}
	}

	/**
	 * PE-5: A parallel gateway that both joins (2 incoming) AND forks (2 outgoing)
	 * only continues via outgoingFlows.get(0), silently dropping the other outgoing
	 * path. Both Task_X and Task_Y must open.
	 */
	@Test
	public void testMixedGatewayForksToAllOutgoing() throws Exception {

		final String procUuid = importProcess("/engine-bug-mixed-gateway.bpmn");

		final String instId;
		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertNotNull("mixed gateway must continue to its first outgoing path", openTaskAt(inst, "Task_X"));
			assertNotNull("mixed gateway must ALSO continue to its second outgoing path (currently dropped)",
				openTaskAt(inst, "Task_Y"));

			tx.success();
		}
	}

	/**
	 * PE-3: {@code signalEvent} resolves the catch element with a GLOBAL
	 * {@code nodeQuery(...).getFirst()} instead of scoping to the instance's own
	 * definition version. When two versions of a definition share the catch
	 * event's bpmnId, the signal is routed to whichever element the index returns
	 * first; if that is not the instance's version, its waiting token is never
	 * found and the signal cannot be delivered.
	 */
	@Test
	public void testSignalEventScopedToInstanceVersion() throws Exception {

		// Import the SAME definition twice -> two versions, both with element bpmnId "Catch_1".
		final String proc1 = importProcess("/engine-catch-signal.bpmn");
		final String proc2 = importProcess("/engine-catch-signal.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			// Which "Catch_1" does the engine's global lookup return?
			final NodeInterface globalCatch = app.nodeQuery(ProcessTraits.BPMN_ELEMENT)
				.key(Traits.of(ProcessTraits.BPMN_ELEMENT).key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), "Catch_1")
				.getFirst();
			assertNotNull(globalCatch);

			// Start the instance on the OTHER version, so its token waits at a
			// Catch_1 the global lookup will NOT return.
			final NodeInterface catch1 = elementByBpmnId(app.getNodeById(proc1), "Catch_1");
			final NodeInterface target = globalCatch.getUuid().equals(catch1.getUuid())
				? app.getNodeById(proc2) : app.getNodeById(proc1);

			instId = engine().startProcess(target, null).getUuid();
			tx.success();
		}

		FrameworkException thrown = null;

		try (final Tx tx = app.tx()) {

			engine().signalEvent(app.getNodeById(instId), "Catch_1", null);
			tx.success();

		} catch (final FrameworkException fe) {

			thrown = fe;
		}

		assertNull("signalEvent must resolve the catch event within the instance's own version, not globally. Got: "
			+ (thrown != null ? thrown.getMessage() : ""), thrown);

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertFalse("the signal must be delivered: the token must have left Catch_1",
				waitingTokenElementIds(inst).contains("Catch_1"));
			tx.success();
		}
	}

	/**
	 * PE-6: {@code advanceToken} recurses (advanceToken -&gt; completeTokenAndMoveToNext
	 * -&gt; moveTokenToElement -&gt; advanceToken) with no depth/cycle guard, so a long
	 * chain of automatic pass-through elements recurses once per element and
	 * overflows the stack. Execution should be iterative / bounded.
	 */
	@Test
	public void testDeepAutomaticChainDoesNotOverflowStack() throws Exception {

		final int chainLength = 5000;

		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(generateAutomaticChain(chainLength));
			assertNotNull(defNode);

			final NodeInterface proc = app.nodeQuery(ProcessTraits.BPMN_PROCESS).getFirst();
			assertNotNull(proc);
			procUuid = proc.getUuid();

			tx.success();
		}

		Throwable overflow = null;

		try (final Tx tx = app.tx()) {

			engine().startProcess(app.getNodeById(procUuid), null);
			tx.success();

		} catch (final StackOverflowError soe) {

			overflow = soe;

		} catch (final Throwable other) {

			// Any non-overflow outcome (e.g. clean completion) is acceptable here.
		}

		assertNull("PE-6: a deep automatic chain (" + chainLength + " elements) overflowed the stack; "
			+ "advanceToken must not recurse unbounded", overflow);
	}

	/**
	 * PE-10: the parallel join fires on {@code countTokensAtElement >= incomingFlows.size()},
	 * i.e. it counts the TOTAL tokens waiting at the gateway instead of requiring one
	 * per distinct incoming edge. An upstream parallel fork sends TWO tokens to the
	 * join via the SAME edge (P); those two satisfy the count before the other edge
	 * (Q, a user task) has delivered, so the join fires prematurely. It must instead
	 * wait for every incoming edge.
	 */
	@Test
	public void testParallelJoinWaitsPerIncomingEdgeNotTotalCount() throws Exception {

		final String procUuid = importProcess("/engine-bug-parallel-join-overcount.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			// Q's branch reached a user task and has NOT delivered to the join.
			assertNotNull("UserTask_Q should be open (its branch parked at a user task)", anyTaskAt(inst, "UserTask_Q"));

			// Therefore the join must NOT have fired yet: Task_After must not exist.
			assertNull("parallel join fired on two tokens from the SAME incoming edge before the other edge (Q) delivered; "
				+ "it must wait for every incoming edge, not just a total count", anyTaskAt(inst, "Task_After"));

			tx.success();
		}
	}

	/** Build a BPMN definition: Start -&gt; N manualTasks -&gt; End, all automatic pass-through. */
	private String generateAutomaticChain(final int n) {

		final StringBuilder b = new StringBuilder();

		b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		 .append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ")
		 .append("id=\"Definitions_DeepChain\" targetNamespace=\"http://test/deep\">\n")
		 .append("  <bpmn:process id=\"Process_DeepChain\" isExecutable=\"true\">\n")
		 .append("    <bpmn:startEvent id=\"Start_1\"/>\n");

		for (int i = 0; i < n; i++) {
			b.append("    <bpmn:manualTask id=\"T").append(i).append("\"/>\n");
		}

		b.append("    <bpmn:endEvent id=\"End_1\"/>\n")
		 .append("    <bpmn:sequenceFlow id=\"F_start\" sourceRef=\"Start_1\" targetRef=\"T0\"/>\n");

		for (int i = 0; i < n - 1; i++) {
			b.append("    <bpmn:sequenceFlow id=\"F").append(i).append("\" sourceRef=\"T").append(i)
			 .append("\" targetRef=\"T").append(i + 1).append("\"/>\n");
		}

		b.append("    <bpmn:sequenceFlow id=\"F_end\" sourceRef=\"T").append(n - 1).append("\" targetRef=\"End_1\"/>\n")
		 .append("  </bpmn:process>\n</bpmn:definitions>\n");

		return b.toString();
	}
}
