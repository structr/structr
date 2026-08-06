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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnDefinitionsTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * FAILING reproductions for the {@code BpmnImporter} findings. Each test asserts
 * the CORRECT/intended behaviour and currently FAILS because the bug is present,
 * pinning the finding so a fix makes it pass.
 */
public class BpmnImporterBugReproTest extends StructrUiTest {

	/**
	 * IMP-1: an element carrying BOTH a task listener (whose handler method is
	 * attached via {@code ensureElementMethod}) AND a {@code <structr:methodRef>}
	 * loses the listener's method: {@code importElementMethodRefs} does
	 * {@code setProperty(methods, resolved)} which REPLACES the whole collection
	 * instead of appending. The listener method vanishes from the element's
	 * {@code methods} collection (orphan/duplicate methods accumulate on re-import).
	 */
	@Test
	public void testTaskListenerMethodSurvivesMethodRefImport() {

		try (final Tx tx = app.tx()) {

			// The <structr:methodRef name="refMethod"/> resolves to a pre-existing
			// orphan SchemaMethod (findSchemaMethodByName prefers orphans).
			final NodeInterface refMethod = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
			refMethod.setProperty(refMethod.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "refMethod");

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = findElementByBpmnId(procNode, "UserTask_1");

			assertNotNull("UserTask_1 not imported", userTask);

			final List<String> names = methodNames(userTask);

			assertTrue("Element methods should still contain the task-listener method 'onCreate' after methodRef import; found: " + names, names.contains("onCreate"));
			assertTrue("Element methods should contain the methodRef target 'refMethod'; found: " + names, names.contains("refMethod"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	/**
	 * IMP-2: a {@code <bpmn:transaction>} (a legal sub-process variant) is imported
	 * as a childless leaf because {@code importProcessChildren} recurses only for
	 * {@code "subProcess"}. Its inner tasks/flows are never created.
	 */
	@Test
	public void testTransactionChildrenAreImported() {

		try (final Tx tx = app.tx()) {

			new BpmnImporter(securityContext).importBpmn(loadResource("/bug-transaction.bpmn"));

			final NodeInterface inner = app.nodeQuery(ProcessTraits.BPMN_ELEMENT)
				.key(Traits.of(ProcessTraits.BPMN_ELEMENT).key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), "Inner_Task")
				.getFirst();

			assertNotNull("Inner task of <bpmn:transaction> should be imported (transaction children are currently dropped)", inner);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	/**
	 * IMP-3: a single {@code <dc:Bounds>} missing the {@code width} attribute makes
	 * {@code Double.parseDouble("")} throw, aborting import of the ENTIRE file. An
	 * otherwise-valid diagram should import (the incomplete shape may be skipped),
	 * not reject the whole process.
	 */
	@Test
	public void testMissingDiBoundsDoesNotAbortImport() {

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-di-missing-bounds.bpmn"));
			assertNotNull("Import should succeed despite an incomplete <dc:Bounds>", defNode);

			tx.success();

		} catch (final FrameworkException | RuntimeException ex) {

			fail("Import aborted by an incomplete <dc:Bounds> (missing width): " + ex);
		}
	}

	/**
	 * IMP-4: the Camunda extension attribute is read namespace-correctly
	 * ({@code getAttributeNS(CAMUNDA_NS, ...)}) but stripped from the stored
	 * {@code bpmnAttributes} by the LITERAL key {@code "camunda:assignee"}. When the
	 * Camunda namespace is bound to a different prefix (here {@code c:}), the raw
	 * {@code c:assignee} attribute leaks into {@code bpmnAttributes} and is
	 * re-exported alongside the generated performer.
	 */
	@Test
	public void testCamundaAttributeStrippedRegardlessOfPrefix() {

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-camunda-prefix.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = findElementByBpmnId(procNode, "UserTask_1");

			assertNotNull("UserTask_1 not imported", userTask);

			// The attribute WAS read (a performer was generated) -- proving the
			// namespace-based read works; only the string-based stripping is broken.
			final List<NodeInterface> performers = app.nodeQuery(ProcessTraits.BPMN_PERFORMER).getAsList();
			assertFalse("Expected a BpmnPerformer generated from c:assignee", performers.isEmpty());

			final String bpmnAttrs = userTask.getProperty(userTask.getTraits().key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
			assertFalse("The Camunda assignee attribute leaked into bpmnAttributes (not stripped for prefix 'c'): " + bpmnAttrs, bpmnAttrs != null && bpmnAttrs.contains("assignee"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// helpers (mirrors BpmnRoundTripTest idioms; raw getProperty navigation)
	// ------------------------------------------------------------------

	private String loadResource(final String path) {

		try (final InputStream is = getClass().getResourceAsStream(path)) {

			assertNotNull("Resource not found: " + path, is);

			return new String(is.readAllBytes(), StandardCharsets.UTF_8);

		} catch (final Exception ex) {

			throw new RuntimeException("Could not load resource " + path, ex);
		}
	}

	private NodeInterface firstProcess(final NodeInterface defNode) {

		final Traits defTraits = defNode.getTraits();
		final Iterable<NodeInterface> processes = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY));

		if (processes == null) {

			return null;
		}

		for (final NodeInterface p : processes) {

			return p;
		}

		return null;
	}

	private NodeInterface findElementByBpmnId(final NodeInterface procNode, final String bpmnId) {

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));

		if (elements == null) {

			return null;
		}

		for (final NodeInterface e : elements) {

			final String id = e.getProperty(e.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
			if (bpmnId.equals(id)) {

				return e;
			}
		}

		return null;
	}

	private List<String> methodNames(final NodeInterface element) {

		final List<String> names = new ArrayList<>();
		final Iterable<NodeInterface> methods = element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY));

		if (methods != null) {

			for (final NodeInterface m : methods) {

				names.add(m.getProperty(m.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			}
		}

		return names;
	}

	private List<String> laneBpmnIds(final NodeInterface procNode) {

		final List<String> ids = new ArrayList<>();
		final Traits pt = Traits.of(ProcessTraits.BPMN_PROCESS);
		final Iterable<NodeInterface> lanes = procNode.getProperty(pt.key(BpmnProcessTraitDefinition.LANES_PROPERTY));

		if (lanes != null) {

			for (final NodeInterface l : lanes) {

				ids.add(l.getProperty(l.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
			}
		}

		return ids;
	}

	// ==================================================================
	// Additional (complex) reproductions
	// ==================================================================

	/**
	 * IMP-5: {@code importLaneSet} uses recursive {@code getElementsByTagNameNS} on
	 * the &lt;process&gt; element, so a &lt;laneSet&gt; nested inside a
	 * &lt;subProcess&gt; is pulled up and its lanes are wired to the TOP-level
	 * process, collapsing the containment hierarchy.
	 */
	@Test
	public void testNestedLaneSetIsNotFlattenedOntoTopProcess() {

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-nested-lanes.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);

			assertNotNull("process not imported", procNode);

			final List<String> topLaneIds = laneBpmnIds(procNode);

			assertFalse("the sub-process's lane 'Lane_Sub' must NOT be attached to the top-level process "
				+ "(nested laneSet was flattened up); top-process lanes = " + topLaneIds, topLaneIds.contains("Lane_Sub"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	/**
	 * IMP-7: the importer's {@code DocumentBuilderFactory} does not disable DOCTYPE
	 * / external entities, so a malicious BPMN can use an external entity (XXE) to
	 * read local files. The secret contents of a local file must NOT be expanded
	 * into the imported graph. (If a hardened parser instead rejects the DOCTYPE,
	 * that is the secure outcome and the test passes.)
	 */
	@Test
	public void testExternalEntityIsNotExpanded() throws Exception {

		final String secret = "XXE-SECRET-" + java.util.UUID.randomUUID();
		final java.nio.file.Path secretFile = java.nio.file.Files.createTempFile("xxe-secret", ".txt");

		java.nio.file.Files.writeString(secretFile, secret);
		secretFile.toFile().deleteOnExit();

		final String xml =
			  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE bpmn:definitions [ <!ENTITY xxe SYSTEM \"" + secretFile.toUri() + "\"> ]>\n"
			+ "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
			+ "id=\"Definitions_XXE\" targetNamespace=\"http://test/xxe\">\n"
			+ "  <bpmn:process id=\"Process_XXE\" isExecutable=\"true\">\n"
			+ "    <bpmn:userTask id=\"UserTask_1\">\n"
			+ "      <bpmn:documentation>&xxe;</bpmn:documentation>\n"
			+ "    </bpmn:userTask>\n"
			+ "  </bpmn:process>\n"
			+ "</bpmn:definitions>\n";

		try (final Tx tx = app.tx()) {

			NodeInterface defNode;

			try {

				defNode = new BpmnImporter(securityContext).importBpmn(xml);

			} catch (final Exception ex) {

				// A hardened parser rejecting DOCTYPE / external entities is the SECURE outcome.
				tx.success();

				return;
			}

			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = procNode != null ? findElementByBpmnId(procNode, "UserTask_1") : null;
			final String documentation   = userTask != null
				? userTask.getProperty(userTask.getTraits().key(BpmnElementTraitDefinition.DOCUMENTATION_PROPERTY))
				: null;

			assertFalse("XXE: the external entity was expanded and leaked local file contents into the graph: " + documentation, documentation != null && documentation.contains(secret));

			tx.success();
		}
	}
}
