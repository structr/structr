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
package org.structr.test.process.deployment;

import org.structr.test.web.advanced.DeploymentTestBase;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnHandlerNames;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.bpmn.BpmnPageSkeletonGenerator;
import org.structr.process.deployment.BpmnDeploymentHandler;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnDefinitionsTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;
import org.structr.process.traits.definitions.TaskInstanceTraitDefinition;
import org.structr.process.traits.definitions.VisibilityMappingTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Deployment round trip of an application that contains a BPMN process and its generated page.
 *
 * <p>This is the coverage that was missing entirely: the deployment export set is hard-coded in
 * DeployCommand, so before {@code BpmnDeploymentHandler} an archive held the generated page but
 * no process -- definitions/processes/elements absent, every VisibilityMapping gone, and the
 * three UI bridges ({@code ComponentConfiguration.boundUserTask},
 * {@code ActionMapping.controlsProcess}, {@code ActionMapping.targetsElement}) silently dropped.
 * The page still rendered, which is exactly why nobody noticed: no exception, just a page whose
 * steps are all visible at once and whose buttons do nothing.</p>
 *
 * <p>Lives in {@code org.structr.test.web.advanced} to extend {@link DeploymentTestBase}, the
 * only place the export/import round-trip helper exists (same approach as the flow module's
 * FlowDeploymentTest). It therefore re-implements the two BPMN helpers it needs rather than
 * extending AbstractProcessEngineTest.</p>
 */
public class BpmnDeploymentRoundTripTest extends DeploymentTestBase {

	/** The human-facing steps of the fixture, in flow order -- one VisibilityMapping each. */
	private static final List<String> EXPECTED_STEP_BPMN_IDS = List.of("Start_1", "Task_InitialReview", "Task_ManualAssessment", "Event_WaitForResponse");

	@Test
	public void testProcessAndGeneratedPageSurviveDeploymentRoundtrip() {

		final String processId;
		final String pageName;
		final int expectedElements;
		final int expectedMappings;
		final List<String> bindingsBefore = new ArrayList<>();

		// ----- build an application: a process plus its generated page -----
		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadBpmn("/insurance-claim.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("fixture should have a process", procNode);

			final BpmnPageSkeletonGenerator.Result result = BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, procNode.as(BpmnProcess.class), null);

			pageName  = result.pageName();
			processId = procNode.getProperty(procNode.getTraits().key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));

			assertEquals("one div per human-facing step", EXPECTED_STEP_BPMN_IDS.size(), result.stepCount());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			expectedElements = app.nodeQuery(ProcessTraits.BPMN_ELEMENT).getAsList().size();
			expectedMappings = app.nodeQuery(ProcessTraits.VISIBILITY_MAPPING).getAsList().size();

			// the generator emits more mappings than there are steps (a step div is not the only
			// thing whose visibility is process-driven), and not every mapping binds both a
			// process and a step -- so record what the bindings ACTUALLY are and require that
			// exact set back, rather than asserting a guessed shape
			assertTrue("fixture should have produced elements", expectedElements > 0);
			assertTrue("fixture should have produced visibility mappings", expectedMappings > 0);

			bindingsBefore.addAll(visibilityMappingBindings());

			assertTrue("at least one mapping must bind a real step before the roundtrip: " + bindingsBefore,
				bindingsBefore.stream().anyMatch(b -> EXPECTED_STEP_BPMN_IDS.stream().anyMatch(b::endsWith)));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());

			return;
		}

		// ----- export, wipe, import -----
		doImportExportRoundtrip(true);

		// ----- the process itself must be back -----
		try (final Tx tx = app.tx()) {

			final NodeInterface process = processByProcessId(processId);

			assertNotNull("the BpmnProcess must survive the deployment round trip", process);
			assertNotNull("its BpmnDefinitions must survive", app.nodeQuery(ProcessTraits.BPMN_DEFINITIONS).getFirst());
			assertEquals("all BpmnElements must survive", expectedElements, app.nodeQuery(ProcessTraits.BPMN_ELEMENT).getAsList().size());
			assertTrue("sequence flows must survive", app.nodeQuery(ProcessTraits.BPMN_SEQUENCE_FLOW).getAsList().size() > 0);

			// the generated page survives via the normal page export; the link back to the
			// process is ours to restore
			final NodeInterface page = app.nodeQuery(StructrTraits.PAGE).name(pageName).getFirst();
			assertNotNull("the generated page must survive", page);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure after roundtrip: " + fex.getMessage());
		}

		// ----- the UI bridges must be re-bound, not merely present -----
		try (final Tx tx = app.tx()) {

			assertEquals("every visibility mapping must survive", expectedMappings, app.nodeQuery(ProcessTraits.VISIBILITY_MAPPING).getAsList().size());
			assertEquals("every visibility mapping must come back bound to the same process and step", bindingsBefore, visibilityMappingBindings());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected visibility-mapping failure after roundtrip: " + fex.getMessage());
		}
	}

	/**
	 * One {@code <processId>@<stepBpmnId>} line per VisibilityMapping, sorted -- the binding
	 * state that has to be identical before and after a round trip. Uses BPMN ids rather than
	 * UUIDs so the comparison says something about the application rather than about node
	 * identity, and shows "-" where a mapping binds no process or no step.
	 */
	private List<String> visibilityMappingBindings() throws FrameworkException {

		final Traits vmTraits                           = Traits.of(ProcessTraits.VISIBILITY_MAPPING);
		final PropertyKey<NodeInterface> boundProcessKey = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY);
		final PropertyKey<NodeInterface> boundStepKey    = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY);
		final List<String> bindings                      = new ArrayList<>();

		for (final NodeInterface mapping : app.nodeQuery(ProcessTraits.VISIBILITY_MAPPING).getAsList()) {

			final NodeInterface process = mapping.getProperty(boundProcessKey);
			final NodeInterface step    = mapping.getProperty(boundStepKey);
			final String processId = (process != null)
				? process.getProperty(process.getTraits().key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY)) : "-";
			final String stepId    = (step != null)
				? step.getProperty(step.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)) : "-";

			bindings.add(processId + "@" + stepId);
		}

		bindings.sort(null);

		return bindings;
	}

	/**
	 * The listener handler methods of a process must come back attached to it. They travel as
	 * global schema methods (recreated with NEW uuids), so the module handler reattaches them by
	 * name -- which only works because handler names are scoped, see BpmnHandlerNames.
	 */
	@Test
	public void testHandlerMethodsAreReattachedAfterRoundtrip() {

		final List<String> before = new LinkedList<>();
		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadBpmn("/engine-listeners.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("fixture should have a process", procNode);

			procUuid = procNode.getUuid();

			before.addAll(attachedMethodNames(procNode));

			assertTrue("fixture should attach at least one process-level handler", before.size() > 0);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			// By UUID, not "the first process in the database": StructrUiTest#cleanDatabase skips
			// the FIRST method of a class, so in a full-suite run this class starts on the previous
			// class's graph -- and the process-engine tests use these same fixtures. Picking an
			// arbitrary process then found a leftover one with no handlers attached and failed here
			// while passing in isolation. Identity survives the import by design, so use it.
			final NodeInterface process = app.getNodeById(procUuid);
			assertNotNull("the process must survive the round trip", process);

			final List<String> after = attachedMethodNames(process);

			for (final String name : before) {

				assertTrue("handler '" + BpmnHandlerNames.authoredOf(name) + "' must be reattached to its process after a round trip; found: " + after, after.contains(name));
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure after roundtrip: " + fex.getMessage());
		}
	}

	/**
	 * Deploying onto a LIVE installation must not disturb running processes.
	 *
	 * <p>This is the production case: an app with a BPMN process is already deployed, instances
	 * are running, and a new version of the app is imported on top. If the importer recreated the
	 * design-time nodes, every ProcessInstance would lose its BpmnProcess and every TaskInstance
	 * its BpmnElement -- open tasks silently detached from the process they belong to. The
	 * database is deliberately NOT cleaned between export and import here, which is what makes
	 * this an upsert test rather than a restore test.</p>
	 */
	@Test
	public void testRunningInstancesSurviveDeploymentOntoLiveInstance() {

		final String instanceId;
		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadBpmn("/insurance-claim.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("fixture should have a process", procNode);

			procUuid = procNode.getUuid();

			BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, procNode.as(BpmnProcess.class), null);

			invokeMethod(securityContext, procNode, "startProcess", Map.of(), true);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			// the instance OF THIS process: a leftover instance from an earlier test class would
			// otherwise be picked up here (see the comment in the reattach test)
			final NodeInterface instance = instanceOf(procUuid);

			assertNotNull("starting the process must create an instance", instance);
			assertNotNull("the fresh instance must reference its process", processOf(instance));

			instanceId = instance.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());

			return;
		}

		// export and re-import WITHOUT wiping: a new app version landing on a live installation
		doImportExportRoundtrip(true, null, false);

		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.getNodeById(instanceId);

			assertNotNull("the running instance itself must survive", instance);
			assertNotNull("the running instance must STILL reference its process after a deployment", processOf(instance));

			for (final NodeInterface task : app.nodeQuery(ProcessTraits.TASK_INSTANCE).getAsList()) {

				assertNotNull("an open task must still reference the element that defines it", definedByOf(task));
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure after roundtrip: " + fex.getMessage());
		}
	}

	/**
	 * Exporting an unchanged application twice must produce an identical file.
	 *
	 * <p>Acceptance criterion for "deploying a new version of the app must not alter the BPMN if
	 * the process did not change": a deployment diff can only be trusted if the export is
	 * deterministic. Without the explicit sorting in the handler, map iteration order alone would
	 * make every export differ and every deployment look like a process change.</p>
	 */
	@Test
	public void testExportIsDeterministic() {

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadBpmn("/insurance-claim.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("fixture should have a process", procNode);

			BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, procNode.as(BpmnProcess.class), null);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		Path first  = null;
		Path second = null;

		try {

			first  = doExport();
			second = doExport();

			final String firstJson  = readDeploymentFile(first);
			final String secondJson = readDeploymentFile(second);

			assertNotNull("the export must contain " + BpmnDeploymentHandler.DEPLOYMENT_FILE_NAME, firstJson);
			assertEquals("exporting an unchanged application twice must produce an identical file", firstJson, secondJson);

		} catch (final FrameworkException fex) {

			fail("Unexpected export failure: " + fex.getMessage());

		} finally {

			deleteQuietly(first);
			deleteQuietly(second);
		}
	}

	/**
	 * Importing the same archive twice must be a no-op.
	 *
	 * <p>The other half of the same criterion: re-deploying an unchanged application must not
	 * create a new process version, duplicate nodes, or change any UUID. This is what makes the
	 * importer safe to run repeatedly against a live installation -- and it is the property that
	 * an XML-based import would NOT have, since BpmnImporter appends a new version on every run.</p>
	 */
	@Test
	public void testImportingTheSameArchiveTwiceChangesNothing() {

		Path archive = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadBpmn("/insurance-claim.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("fixture should have a process", procNode);

			BpmnPageSkeletonGenerator.createSkeleton(app, securityContext, procNode.as(BpmnProcess.class), null);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		try {

			archive = doExport();

			doImport(archive);

			final List<String> afterFirst = graphSnapshot();
			final int processesAfterFirst = processCount();

			doImport(archive);

			final List<String> afterSecond = graphSnapshot();

			assertEquals("re-importing an unchanged archive must not add, remove or re-identify a single BPMN node", afterFirst, afterSecond);

			try (final Tx tx = app.tx()) {

				// a delta, not an absolute count: leftovers from an earlier test class may already
				// be in the graph (see the comment in the reattach test), and what matters here is
				// that the second import added nothing
				assertEquals("re-importing must not create a second process version", processesAfterFirst, app.nodeQuery(ProcessTraits.BPMN_PROCESS).getAsList().size());

				tx.success();
			}

		} catch (final FrameworkException fex) {

			fail("Unexpected deployment failure: " + fex.getMessage());

		} finally {

			deleteQuietly(archive);
		}
	}

	// ----- helpers (AbstractProcessEngineTest is not our base class, see class comment) -----

	private String readDeploymentFile(final Path exportPath) {

		final Path file = exportPath.resolve("modules").resolve("process").resolve(BpmnDeploymentHandler.DEPLOYMENT_FILE_NAME);
		if (!Files.exists(file)) {

			return null;
		}

		try {

			return Files.readString(file, StandardCharsets.UTF_8);

		} catch (final IOException ioex) {

			fail("Unable to read " + file + ": " + ioex.getMessage());

			return null;
		}
	}

	/** Sorted {@code type:uuid} of every managed BPMN node, plus the visibility-mapping bindings. */
	private List<String> graphSnapshot() throws FrameworkException {

		final List<String> snapshot = new ArrayList<>();

		try (final Tx tx = app.tx()) {

			for (final String type : List.of(ProcessTraits.BPMN_DEFINITIONS, ProcessTraits.BPMN_PROCESS, ProcessTraits.BPMN_ELEMENT,
				ProcessTraits.BPMN_SEQUENCE_FLOW, ProcessTraits.BPMN_TASK_LISTENER, ProcessTraits.BPMN_PROCESS_LISTENER,
				ProcessTraits.VISIBILITY_MAPPING)) {

				for (final NodeInterface node : app.nodeQuery(type).getAsList()) {

					if (type.equals(node.getType())) {

						snapshot.add(type + ":" + node.getUuid());
					}
				}
			}

			snapshot.addAll(visibilityMappingBindings());

			tx.success();
		}

		snapshot.sort(null);

		return snapshot;
	}

	private void deleteQuietly(final Path path) {

		if (path == null) {

			return;
		}

		try {

			deleteExportAt(path);

		} catch (final IOException ioex) {
		}
	}

	private NodeInterface processOf(final NodeInterface instance) throws FrameworkException {

		return instance.getProperty(instance.getTraits().key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
	}

	private NodeInterface definedByOf(final NodeInterface task) throws FrameworkException {

		return task.getProperty(task.getTraits().key(TaskInstanceTraitDefinition.DEFINED_BY_PROPERTY));
	}

	private String loadBpmn(final String path) {

		try (final InputStream in = getClass().getResourceAsStream(path)) {

			assertNotNull("test resource " + path + " not found", in);

			return IOUtils.toString(in, StandardCharsets.UTF_8);

		} catch (final IOException ioex) {

			fail("Unable to read " + path + ": " + ioex.getMessage());

			return null;
		}
	}

	private NodeInterface firstProcess(final NodeInterface defNode) throws FrameworkException {

		final Iterable<NodeInterface> processes = defNode.getProperty(defNode.getTraits().key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY));
		if (processes != null) {

			for (final NodeInterface process : processes) {

				return process;
			}
		}

		return null;
	}

	/** The ProcessInstance of the process with this uuid, or null. */
	private NodeInterface instanceOf(final String procUuid) throws FrameworkException {

		for (final NodeInterface instance : app.nodeQuery(ProcessTraits.PROCESS_INSTANCE).getAsList()) {

			final NodeInterface process = processOf(instance);
			if (process != null && procUuid.equals(process.getUuid())) {

				return instance;
			}
		}

		return null;
	}

	private int processCount() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery(ProcessTraits.BPMN_PROCESS).getAsList().size();

			tx.success();

			return count;
		}
	}

	private NodeInterface processByProcessId(final String processId) throws FrameworkException {

		final Traits traits                    = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> processIdKey = traits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);

		return app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(processIdKey, processId).getFirst();
	}

	/** Graph names of the SchemaMethods attached to a process via HAS_METHOD. */
	private List<String> attachedMethodNames(final NodeInterface process) throws FrameworkException {

		final List<String> names                  = new ArrayList<>();
		final Traits traits                       = process.getTraits();
		final Iterable<NodeInterface> methods     = process.getProperty(traits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY));

		if (methods != null) {

			for (final NodeInterface method : methods) {

				names.add(method.getProperty(method.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			}
		}

		return names;
	}
}
