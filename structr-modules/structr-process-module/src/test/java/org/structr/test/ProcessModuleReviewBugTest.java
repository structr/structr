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
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.engine.ProcessEngine;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnDefinitionsTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.BpmnTaskListenerTraitDefinition;
import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.structr.process.websocket.BpmnDiagramBatchCommand;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Regression guards for the HIGH and MEDIUM findings of the process-module code
 * review (July 2026). Each test drives a small fixture and asserts the CORRECT
 * (intended) behaviour. They started as failing reproductions; the corresponding
 * fixes have since been applied, so they now serve as guards:
 * <ul>
 *   <li>RV-1 sub-process completion, RV-2 element-listener re-import, RV-3
 *       structural children, RV-4 inclusive-join reachability, RV-6 exporter
 *       default namespace -- each pins a fixed bug.</li>
 *   <li>RV-5 is a BEHAVIOUR-PINNING test (see its javadoc): the flagged
 *       condition-error "swallowing" turned out to be a deliberate tolerance, so
 *       the test pins that tradeoff rather than a fix.</li>
 *   <li>RV-4b batch-command diagram-node guard and RV-5b VisibilityMapping
 *       rel-over-string read-through pin the corresponding MEDIUM fixes. (The
 *       batch create-type allowlist is covered by the runnable, DB-free
 *       {@code BpmnDiagramBatchCommandLogicTest}.)</li>
 * </ul>
 *
 * <p>These are distinct from the earlier {@code PE-*} / {@code IMP-*} / {@code EXP-*}
 * reproductions: those cover related-but-different defects on the same code paths.</p>
 *
 * <p>Findings that are NOT deterministically reproducible in a single-threaded
 * unit test are intentionally omitted and documented at the bottom of this file.</p>
 */
public class ProcessModuleReviewBugTest extends AbstractProcessEngineTest {

	private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

	// ==================================================================
	// RV-1 (HIGH) -- sub-process completes on the first internal end event
	// ==================================================================

	/**
	 * A sub-process forks internally (parallel) into branch A (automatic, ends
	 * immediately) and branch B (a user task). When branch A reaches an internal
	 * end event, the engine resumes the parent token straight away instead of
	 * waiting for branch B, so the outer flow advances to Task_After while the
	 * sub-process still has a live token. The sub-process must complete only when
	 * ALL its internal tokens are consumed.
	 */
	@Test
	public void testSubProcessDoesNotCompleteWhileInternalBranchStillOpen() throws Exception {

		final String procUuid = importProcess("/review-subprocess-parallel-end.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertNotNull("the sub-process's inner branch-B user task must still be open", openTaskAt(inst, "Sub_TaskB"));
			assertNull("the parent must NOT advance past the sub-process (Task_After opened) while an internal "
				+ "branch is still live; the sub-process resumed the parent on the first internal end event", anyTaskAt(inst, "Task_After"));

			tx.success();
		}
	}

	// ==================================================================
	// RV-2 (HIGH) -- re-import binds the element task-listener to an EMPTY
	// method and duplicates the authored one
	// ==================================================================

	/**
	 * On re-import (a new version of the same processId), element-level task
	 * listeners are wired to a freshly-created empty handler method BEFORE the
	 * previous version's authored method is cloned in. Because {@code appendMethods}
	 * dedups by UUID (not name), the cloned authored method is appended as a SECOND
	 * same-named method, and the listener keeps pointing at the empty stub. After a
	 * re-import the element must have exactly ONE {@code onTaskCreated} method and
	 * the "created" listener must resolve to the authored body.
	 */
	@Test
	public void testReimportElementListenerKeepsSingleAuthoredMethod() throws Exception {

		final String body = "// authored body RV-2\n";

		// Version 1.
		final String proc1 = importProcess("/engine-listeners.bpmn");

		// Author a body on version 1's onTaskCreated element method.
		try (final Tx tx = app.tx()) {

			final NodeInterface task = elementByBpmnId(app.getNodeById(proc1), "Task_1");
			assertNotNull("Task_1 not imported in v1", task);

			final NodeInterface created = methodByName(task, "onTaskCreated");
			assertNotNull("onTaskCreated method not created by the v1 task listener", created);
			created.setProperty(created.getTraits().key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), body);

			tx.success();
		}

		// Version 2: re-import the SAME definition.
		final String proc2 = importProcess("/engine-listeners.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface task = elementByBpmnId(app.getNodeById(proc2), "Task_1");
			assertNotNull("Task_1 not imported in v2", task);

			final List<String> names = methodNames(task);
			assertEquals("re-import must not duplicate the element handler method; methods = " + names, 1, names.stream().filter("onTaskCreated"::equals).count());

			final NodeInterface listenerMethod = listenerMethodForEvent(task, BpmnTaskListenerTraitDefinition.EVENT_CREATED);
			assertNotNull("the 'created' task listener must resolve to a method", listenerMethod);
			assertEquals("the 'created' listener must bind to the authored method (body), not an empty stub",
				body, listenerMethod.getProperty(listenerMethod.getTraits().key(SchemaMethodTraitDefinition.SOURCE_PROPERTY)));

			tx.success();
		}
	}

	// ==================================================================
	// RV-3 (HIGH) -- structural children imported as generic BpmnElement nodes
	// ==================================================================

	/**
	 * A process-level {@code <bpmn:extensionElements>} (present whenever the
	 * process declares a {@code structr:processListener}) is a structural child,
	 * not a flow node. {@code importProcessChildren} only skips
	 * incoming/outgoing/laneSet/lane/flowNodeRef, so extensionElements falls into
	 * the "unknown -> generic BpmnElement" branch and a bogus element of type
	 * {@code extensionElements} is created (and, being id-less, collides on the
	 * empty-string key of elementMap). No such element must exist.
	 */
	@Test
	public void testStructuralExtensionElementsNotImportedAsElement() throws Exception {

		importProcess("/engine-listeners.bpmn");

		try (final Tx tx = app.tx()) {

			final Traits t = Traits.of(ProcessTraits.BPMN_ELEMENT);
			final List<NodeInterface> bogus = app.nodeQuery(ProcessTraits.BPMN_ELEMENT)
				.key(t.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY), "extensionElements")
				.getAsList();

			assertTrue("a structural <bpmn:extensionElements> was imported as a generic BpmnElement (type=extensionElements); "
				+ "found " + bogus.size(), bogus.isEmpty());

			tx.success();
		}
	}

	// ==================================================================
	// RV-4 (MEDIUM) -- inclusive join counts unrelated instance tokens
	// ==================================================================

	/**
	 * The inclusive-gateway join fires only when there are zero active tokens
	 * anywhere else in the instance. An unrelated parallel branch (Task_B) that
	 * will never reach the join keeps that global count above zero, so the join
	 * deadlocks even after both of its own branches (A1, A2) have arrived. The
	 * join must synchronise its own branches and open Task_A_After regardless of
	 * the unrelated Task_B.
	 */
	@Test
	public void testInclusiveJoinIgnoresUnrelatedParallelBranch() throws Exception {

		final String procUuid = importProcess("/review-inclusive-unrelated.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Sanity: all three user tasks are open after the two forks.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull("inclusive branch A1 task must be open", openTaskAt(inst, "Task_A1"));
			assertNotNull("inclusive branch A2 task must be open", openTaskAt(inst, "Task_A2"));
			assertNotNull("unrelated branch B task must be open",  openTaskAt(inst, "Task_B"));
			tx.success();
		}

		// Complete both inclusive branches; leave the unrelated Task_B open.
		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_A1"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			engine().completeTask(openTaskAt(inst, "Task_A2"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertFalse("no token may be stranded at the inclusive join once both its branches arrived", waitingTokenElementIds(inst).contains("Inc_Join"));
			assertNotNull("the inclusive join must fire once A1 and A2 arrived and open Task_A_After, "
				+ "even though the unrelated Task_B is still open (currently deadlocks)", openTaskAt(inst, "Task_A_After"));

			tx.success();
		}
	}

	// ==================================================================
	// RV-5 (MEDIUM, reclassified) -- gateway condition errors evaluate to false
	// ==================================================================

	/**
	 * BEHAVIOUR-PINNING test (NOT a bug reproduction).
	 *
	 * <p>The review flagged that {@code evaluateCondition} swallows every
	 * evaluation error and returns false, so a broken condition is
	 * indistinguishable from a legitimately-false one and the gateway silently
	 * takes its default branch. On closer inspection this is a DELIBERATE
	 * tolerance, not a fixable bug: BPMN condition expressions are JavaScript, and
	 * a reference to an <em>unset</em> process variable and a call to an
	 * <em>undefined</em> function both raise the same {@code ReferenceError}. The
	 * engine relies on "missing data ⇒ false" -- e.g.
	 * {@code ProcessEngineExecutionTest} starts a process whose {@code approved}
	 * variable is never set and expects the condition to be false (no path matched
	 * ⇒ 422), not a thrown error. Making evaluation errors propagate would break
	 * that tolerated semantics.</p>
	 *
	 * <p>This test therefore pins the intended behaviour: a throwing condition is
	 * treated as false and the gateway routes to its declared default branch,
	 * without raising. If someone later changes evaluateCondition to throw, this
	 * fails on purpose so the tradeoff is revisited consciously.</p>
	 */
	@Test
	public void testThrowingGatewayConditionFallsThroughToDefault() throws Exception {

		final String procUuid = importProcess("/review-gateway-condition-error.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertNotNull("a throwing condition is treated as false, so the gateway must take its default branch", openTaskAt(inst, "Task_Default"));
			assertNull("the intended (throwing-condition) branch must not be taken", anyTaskAt(inst, "Task_Intended"));
			tx.success();
		}
	}

	// ==================================================================
	// RV-6 (MEDIUM) -- exporter emits an unbound bpmn: prefix for a
	// default-namespace definition
	// ==================================================================

	/**
	 * When the BPMN namespace is stored as the DEFAULT namespace ({@code xmlns},
	 * no prefix), {@code findPrefixForNamespace} still falls back to the literal
	 * prefix {@code "bpmn"} while only {@code writeDefaultNamespace} is emitted. The
	 * root is written as {@code <bpmn:definitions xmlns="...">} with the {@code bpmn}
	 * prefix never bound, producing namespace-ill-formed XML that fails to re-parse.
	 */
	@Test
	public void testExportOfDefaultNamespaceDefinitionIsWellFormed() throws Exception {

		final String defUuid = importDefinition("/simple-approval.bpmn");
		String exported     = null;
		Throwable exportErr  = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = app.getNodeById(defUuid);

			// Store the BPMN namespace as the DEFAULT (unprefixed) namespace.
			defNode.setProperty(defNode.getTraits().key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS), "{\"xmlns\":\"" + BPMN_NS + "\"}");

			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();

		} catch (final Throwable t) {

			exportErr = t;
		}

		assertNull("export of a default-namespace definition must not fail. Got: "
			+ (exportErr != null ? exportErr.getClass().getSimpleName() + ": " + exportErr.getMessage() : ""), exportErr);
		assertNotNull(exported);

		// The exported XML must be namespace-well-formed (re-parseable).
		Throwable parseErr = null;

		try {

			parseXmlNamespaceAware(exported);

		} catch (final Throwable t) {

			parseErr = t;
		}

		assertNull("exported XML uses an unbound 'bpmn:' prefix for a default-namespace definition and does not "
			+ "re-parse. Got: " + (parseErr != null ? parseErr.getClass().getSimpleName() + ": " + parseErr.getMessage() : "")
			+ "\n---\n" + exported, parseErr);
	}

	// ==================================================================
	// RV-4b (MEDIUM) -- batch command refuses non-diagram nodes (update/delete guard)
	// ==================================================================

	/**
	 * {@code BpmnDiagramBatchCommand} must only update/delete BPMN diagram-domain
	 * nodes. A BpmnProcess is a diagram node; an unrelated node (a User) is not and
	 * must be rejected, closing the "delete/update any node by id" abuse. (The
	 * create-type allowlist is covered by the pure-logic BpmnDiagramBatchCommandLogicTest.)
	 */
	@Test
	public void testBatchGuardAcceptsDiagramNodesRejectsOthers() throws Exception {

		final String procUuid    = importProcess("/engine-listeners.bpmn");
		final NodeInterface user = createUser("rv4b-victim");

		try (final Tx tx = app.tx()) {

			assertTrue("a BpmnProcess must be recognised as a diagram node", BpmnDiagramBatchCommand.isDiagramNode(app.getNodeById(procUuid)));
			assertFalse("a User must NOT be treatable by a diagram batch (update/delete guard)", BpmnDiagramBatchCommand.isDiagramNode(app.getNodeById(user.getUuid())));

			tx.success();
		}
	}

	// ==================================================================
	// RV-5b (MEDIUM) -- VisibilityMapping prefers the authoritative rel over a stale string
	// ==================================================================

	/**
	 * {@code evaluate} must derive the bound process / step from the authoritative
	 * {@code boundProcess}/{@code boundStep} relationships when present, using the
	 * denormalized {@code *Id} strings only as a fallback. This pins the id-resolution
	 * seam: a present rel wins over a stale backup string; a null rel falls back.
	 */
	@Test
	public void testVisibilityMappingPrefersRelOverStaleBackupString() throws Exception {

		final String procUuid = importProcess("/engine-listeners.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface proc = app.getNodeById(procUuid);
			final String realProcessId = proc.getProperty(proc.getTraits().key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));
			final NodeInterface step   = elementByBpmnId(proc, "Task_1");

			assertNotNull("Task_1 should be imported", step);

			// rel present -> derive id from the rel, ignoring the stale backup string
			assertEquals("boundProcess rel must win over a stale boundProcessId string", realProcessId, VisibilityMappingTraitDefinition.preferredBoundProcessId(proc, "STALE-PROCESS-ID"));
			assertEquals("boundStep rel must win over a stale boundStepBpmnId string", "Task_1", VisibilityMappingTraitDefinition.preferredBoundStepBpmnId(step, "STALE-STEP"));

			// rel absent -> fall back to the denormalized string
			assertEquals("FALLBACK-PROCESS", VisibilityMappingTraitDefinition.preferredBoundProcessId(null, "FALLBACK-PROCESS"));
			assertEquals("FALLBACK-STEP", VisibilityMappingTraitDefinition.preferredBoundStepBpmnId(null, "FALLBACK-STEP"));

			tx.success();
		}
	}

	// ==================================================================
	// Service-task execution + process-variable write-back (Camunda compat #1/#2)
	// ==================================================================

	/**
	 * A Camunda service task's {@code camunda:expression} calling
	 * {@code execution.setVariable('approved', true)} must, on execution, persist the
	 * variable (via transpile -> $.process write -> ProcessContext sink), so the
	 * following exclusive gateway routes to the approved branch.
	 */
	@Test
	public void testServiceTaskCamundaExpressionSetsVariableAndRoutes() throws Exception {

		final String procUuid = importProcess("/review-camunda-servicetask.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertEquals("service task's execution.setVariable must persist approved=true", "true", parameterValues(inst).get("approved"));
			assertNotNull("the gateway must route to the approved branch (variable was set by the service task)", openTaskAt(inst, "Task_Yes"));
			assertNull("the default/reject branch must not be taken", anyTaskAt(inst, "Task_No"));

			tx.success();
		}
	}

	/**
	 * A Structr-native service task whose inline structr-javascript body does
	 * {@code $.process.amount = 25000} must persist that write (ProcessContext write
	 * sink), so the amount gateway routes to the medium bracket.
	 */
	@Test
	public void testServiceTaskStructrScriptSetsVariableAndRoutes() throws Exception {

		final String procUuid = importProcess("/review-native-servicetask.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertEquals("$.process.amount write from the service task must persist", "25000", parameterValues(inst).get("amount"));
			assertNotNull("the amount gateway must route to the medium bracket", openTaskAt(inst, "Task_Medium"));
			assertNull("the small/default bracket must not be taken", anyTaskAt(inst, "Task_Small"));

			tx.success();
		}
	}

	// ==================================================================
	// Camunda inputOutput -- activity-local variable scope (#3, option C)
	// ==================================================================

	/**
	 * Input parameters are activity-LOCAL: they shadow a same-named process variable
	 * during the activity without overwriting it, and they do not leak to process
	 * scope afterwards. An output can read the local shadow and promote it.
	 *
	 * <p>Instance starts with x=1; the service task has inputs x=5, tmp=99 and output
	 * seen=${x}. After it runs: process x is still 1 (not clobbered), tmp is absent
	 * (no leak), seen is 5 (output saw the local shadow, not the process value).</p>
	 */
	@Test
	public void testIoInputsAreLocalAndDoNotLeakOrClobber() throws Exception {

		final String procUuid = importProcess("/review-io-scope.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null, Map.of("x", 1)).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst              = app.getNodeById(instId);
			final Map<String, String> parameters = parameterValues(inst);

			assertEquals("a local input must NOT clobber the same-named process variable", "1", parameters.get("x"));
			assertNull("a local input must NOT leak into process scope", parameters.get("tmp"));
			assertEquals("an output must read the local shadow (5), not the process value (1)", "5", parameters.get("seen"));

			tx.success();
		}
	}

	/**
	 * An output mapping promotes a locally-computed value to process scope, where a
	 * downstream gateway routes on it: amount=${25000} -> the medium bracket.
	 */
	@Test
	public void testIoOutputPersistsAndRoutesGateway() throws Exception {

		final String procUuid = importProcess("/review-io-output-routes.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);

			assertEquals("output mapping must promote amount to process scope", "25000", parameterValues(inst).get("amount"));
			assertNotNull("the gateway must route to the medium bracket on the promoted amount", openTaskAt(inst, "Task_Medium"));
			assertNull("the small/default bracket must not be taken", anyTaskAt(inst, "Task_Small"));

			tx.success();
		}
	}

	// ==================================================================
	// Listener execution -- Camunda expression listeners run as JS with $.process
	// ==================================================================

	/**
	 * A Camunda execution listener whose expression is
	 * {@code ${execution.setVariable('startedFlag', true)}} must, on the process
	 * 'started' event, run as JavaScript (not StructrScript) with $.process installed,
	 * so the transpiled write persists the variable.
	 */
	@Test
	public void testCamundaExecutionListenerSetsVariable() throws Exception {

		final String procUuid = importProcess("/review-camunda-listener.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface inst = app.getNodeById(instId);
			assertEquals("a Camunda execution listener's execution.setVariable must persist the variable", "true", parameterValues(inst).get("startedFlag"));
			tx.success();
		}
	}

	// ==================================================================
	// Live instance-count overlay (editor badge data)
	// ==================================================================

	/**
	 * {@code computeLiveTokenCounts} must aggregate non-completed tokens per element
	 * across all instances of a process, and track them as tokens advance.
	 */
	@Test
	public void testLiveTokenCountsAggregateAcrossInstances() throws Exception {

		final String procUuid = importProcess("/engine-bug-two-usertasks.bpmn");

		final String inst1;

		try (final Tx tx = app.tx()) {

			inst1 = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			engine().startProcess(app.getNodeById(procUuid), null);
			tx.success();
		}

		// Both instances park at Task_A.
		try (final Tx tx = app.tx()) {

			final Map<String, Integer> counts = ProcessEngine.computeLiveTokenCounts(app, app.getNodeById(procUuid));
			assertEquals("two instances sit at Task_A", Integer.valueOf(2), counts.get("Task_A"));
			assertNull("nothing is at Task_B yet", counts.get("Task_B"));
			tx.success();
		}

		// Advance one instance Task_A -> Task_B.
		try (final Tx tx = app.tx()) {

			engine().completeTask(openTaskAt(app.getNodeById(inst1), "Task_A"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Integer> counts = ProcessEngine.computeLiveTokenCounts(app, app.getNodeById(procUuid));
			assertEquals("one instance still at Task_A", Integer.valueOf(1), counts.get("Task_A"));
			assertEquals("one instance advanced to Task_B", Integer.valueOf(1), counts.get("Task_B"));
			tx.success();
		}
	}

	// ==================================================================
	// currentStepElements -- where an instance is right now
	// ==================================================================

	/**
	 * {@code currentStepElements} returns the BpmnElement(s) an instance is currently
	 * at (its non-completed tokens' elements), and follows the token as it advances.
	 */
	@Test
	public void testCurrentStepElementsFollowTheInstance() throws Exception {

		final String procUuid = importProcess("/engine-bug-two-usertasks.bpmn");

		final String instId;

		try (final Tx tx = app.tx()) {

			instId = engine().startProcess(app.getNodeById(procUuid), null).getUuid();
			tx.success();
		}

		// Parked at Task_A.
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> steps = ProcessEngine.currentStepElements(app.getNodeById(instId));
			assertEquals("exactly one current step", 1, steps.size());
			assertEquals("instance is at Task_A", "Task_A", steps.get(0).getProperty(steps.get(0).getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
			tx.success();
		}

		// Advance Task_A -> Task_B.
		try (final Tx tx = app.tx()) {

			engine().completeTask(openTaskAt(app.getNodeById(instId), "Task_A"), Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> steps = ProcessEngine.currentStepElements(app.getNodeById(instId));
			assertEquals(1, steps.size());
			assertEquals("instance advanced to Task_B", "Task_B", steps.get(0).getProperty(steps.get(0).getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
			tx.success();
		}
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private String importDefinition(final String resource) throws FrameworkException {

		final String xml = loadResource(resource);
		String defUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(xml);
			assertNotNull("import returned null for " + resource, defNode);
			defUuid = defNode.getUuid();

			tx.success();
		}

		return defUuid;
	}

	private NodeInterface methodByName(final NodeInterface element, final String name) throws FrameworkException {

		final Iterable<NodeInterface> methods = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY));
		if (methods != null) {

			for (final NodeInterface m : methods) {

				if (name.equals(m.getProperty(m.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)))) {

					return m;
				}
			}
		}

		return null;
	}

	private List<String> methodNames(final NodeInterface element) throws FrameworkException {

		final List<String> names = new ArrayList<>();
		final Iterable<NodeInterface> methods = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY));

		if (methods != null) {

			for (final NodeInterface m : methods) {

				names.add(m.getProperty(m.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			}
		}

		return names;
	}

	private NodeInterface listenerMethodForEvent(final NodeInterface element, final String event) throws FrameworkException {

		final Iterable<NodeInterface> listeners = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY));
		if (listeners != null) {

			for (final NodeInterface l : listeners) {

				if (event.equals(l.getProperty(l.getTraits().key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY)))) {

					return l.getProperty(l.getTraits().key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY));
				}
			}
		}

		return null;
	}

	private Document parseXmlNamespaceAware(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	// ==================================================================
	// MEDIUM findings without an automated test here (status notes)
	// ==================================================================
	//
	//  * No per-instance locking on concurrent branch completion
	//    (ProcessEngine parallel/inclusive join): ATTEMPTED via a DB write-lock
	//    (bumping a `revision` counter on the instance at the start of each
	//    mutating op) and then REVERTED. Writing any property to the instance
	//    BEFORE its status change displaces `status` out of the modification
	//    queue's `modifiedProperties` map (GraphObjectModificationState.modify
	//    records only the first-modified key there), so
	//    ProcessInstance.onModification's isPropertyModified(STATUS) stops
	//    detecting the transition and the completed/suspended/terminated lifecycle
	//    events silently stop firing (observed via ProcessEngineListenerTest). A
	//    correct fix needs a locking mechanism that does not write a tracked
	//    property on the instance (or a change to the event-detection), validated
	//    under real concurrent load -- neither reproducible in this single-thread,
	//    DB-less sandbox.
	//
	//  * ProcessTimerService.doRun not volatile: FIXED (made volatile). A
	//    Java-Memory-Model visibility bug that cannot be deterministically
	//    reproduced in a test.
	//
	//  * Clustered duplicate timer fire (ProcessTimerService / fireTimer): FIXED by
	//    claiming the timer (writing FIRED) at the START of the fire transaction, so
	//    a racing cluster node conflicts on the timer node and rolls back. Requires a
	//    multi-node cluster to verify; not reproducible in a single-instance test.
	//
	//  * PrincipalExpressionResolver user(name)/group(name): FIXED (now warns on an
	//    ambiguous name). The "correct" pick is undefined when two principals share a
	//    name, so there is no single value a functional test could assert; the fix is
	//    an observability/warning improvement.
	//
	//  * BpmnDiagramBatchCommand unscoped client type/id: FIXED (type allowlist +
	//    diagram-node guard). Covered by BpmnDiagramBatchCommandLogicTest (create
	//    allowlist, DB-free) and testBatchGuardAcceptsDiagramNodesRejectsOthers
	//    (update/delete guard, DB-backed) above.
	//
	//  * VisibilityMappingTraitDefinition.evaluate read-through: FIXED (prefers the
	//    boundProcess/boundStep rels over the denormalized strings). Covered by
	//    testVisibilityMappingPrefersRelOverStaleBackupString above.
}
