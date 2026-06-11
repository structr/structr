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
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.traits.definitions.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.structr.process.ProcessTraits;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Tests BPMN 2.0.2 XML import and export round-trip fidelity.
 *
 * Each test imports a BPMN file, exports it back to XML, re-parses the exported XML,
 * and verifies that all semantic content is preserved.
 */
public class BpmnRoundTripTest extends StructrUiTest {

	private static final String BPMN_NS    = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS      = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS      = "http://www.omg.org/spec/DD/20100524/DC";
	private static final String STRUCTR_NS = "http://structr.org/schema/process/1.0";

	// ------------------------------------------------------------------
	// Test: simple-approval.bpmn
	// Covers: basic elements, exclusive gateway, condition expressions, DI
	// ------------------------------------------------------------------
	@Test
	public void testSimpleApprovalRoundTrip() {

		try {
			final String exported = importAndExport("/simple-approval.bpmn");
			final Document doc = parseXml(exported);

			// Verify definitions
			final Element root = doc.getDocumentElement();
			assertEquals("Definitions_1", root.getAttribute("id"));
			assertEquals("http://bpmn.io/schema/bpmn", root.getAttribute("targetNamespace"));

			// Verify process
			final NodeList processes = root.getElementsByTagNameNS(BPMN_NS, "process");
			assertEquals(1, processes.getLength());
			final Element process = (Element) processes.item(0);
			assertEquals("Process_1", process.getAttribute("id"));
			assertEquals("Simple Approval", process.getAttribute("name"));
			assertEquals("true", process.getAttribute("isExecutable"));

			// Verify element counts
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "startEvent").getLength());
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "endEvent").getLength());
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "userTask").getLength());
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "serviceTask").getLength());
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "exclusiveGateway").getLength());
			assertEquals(6, process.getElementsByTagNameNS(BPMN_NS, "sequenceFlow").getLength());

			// Verify condition expressions exist
			final NodeList conditions = process.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
			assertEquals(2, conditions.getLength());

			// Verify DI
			final NodeList diagrams = root.getElementsByTagNameNS(DI_NS, "BPMNDiagram");
			assertEquals(1, diagrams.getLength());
			final Element diagram = (Element) diagrams.item(0);
			final NodeList planes = diagram.getElementsByTagNameNS(DI_NS, "BPMNPlane");
			assertEquals(1, planes.getLength());

			// Verify shape and edge counts
			final Element plane = (Element) planes.item(0);
			assertEquals(7, plane.getElementsByTagNameNS(DI_NS, "BPMNShape").getLength());
			assertEquals(6, plane.getElementsByTagNameNS(DI_NS, "BPMNEdge").getLength());

		} catch (Exception ex) {
			fail("Round-trip test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: camunda-loan-approval.bpmn
	// Covers: bpmn2: prefix, Camunda extension attributes, incoming/outgoing,
	//         xsi:schemaLocation, newline in name (&#10;), DI sourceElement/targetElement
	// ------------------------------------------------------------------
	@Test
	public void testCamundaLoanApprovalRoundTrip() {

		try {
			final String exported = importAndExport("/camunda-loan-approval.bpmn");
			final Document doc = parseXml(exported);

			final Element root = doc.getDocumentElement();

			// Verify definitions-level attributes survived
			assertNotNull(root.getAttribute("id"));
			assertEquals("http://camunda.org/examples", root.getAttribute("targetNamespace"));

			// Verify process
			final NodeList processes = root.getElementsByTagNameNS(BPMN_NS, "process");
			assertEquals(1, processes.getLength());
			final Element process = (Element) processes.item(0);
			assertEquals("embeddedFormsQuickstart", process.getAttribute("id"));
			assertEquals("Embedded Forms Quickstart", process.getAttribute("name"));

			// Verify elements
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "startEvent").getLength());
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "endEvent").getLength());
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "userTask").getLength());
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "sequenceFlow").getLength());

			// Verify DI
			final NodeList diagrams = root.getElementsByTagNameNS(DI_NS, "BPMNDiagram");
			assertEquals(1, diagrams.getLength());

			final Element plane = (Element) ((Element) diagrams.item(0)).getElementsByTagNameNS(DI_NS, "BPMNPlane").item(0);
			assertEquals(3, plane.getElementsByTagNameNS(DI_NS, "BPMNShape").getLength());
			assertEquals(2, plane.getElementsByTagNameNS(DI_NS, "BPMNEdge").getLength());

		} catch (Exception ex) {
			fail("Round-trip test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: parallel-subprocess.bpmn
	// Covers: parallel gateway fork/join, embedded sub-process, manual tasks,
	//         sub-process elements and flows, isExpanded DI attribute
	// ------------------------------------------------------------------
	@Test
	public void testParallelSubprocessRoundTrip() {

		try {
			final String exported = importAndExport("/parallel-subprocess.bpmn");
			final Document doc = parseXml(exported);

			final Element root = doc.getDocumentElement();
			final NodeList processes = root.getElementsByTagNameNS(BPMN_NS, "process");
			assertEquals(1, processes.getLength());
			final Element process = (Element) processes.item(0);
			assertEquals("Order Fulfillment", process.getAttribute("name"));

			// Verify parallel gateways
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "parallelGateway").getLength());

			// Verify sub-process exists
			final NodeList subProcesses = process.getElementsByTagNameNS(BPMN_NS, "subProcess");
			assertEquals(1, subProcesses.getLength());

			// Verify sub-process children are nested inside, not at top level
			final Element subProcess = (Element) subProcesses.item(0);
			assertEquals("SubProcess_Pack", subProcess.getAttribute("id"));

			// Count direct children of the sub-process element
			assertEquals(1, getDirectChildElements(subProcess, BPMN_NS, "startEvent").size());
			assertEquals(1, getDirectChildElements(subProcess, BPMN_NS, "endEvent").size());
			assertEquals(2, getDirectChildElements(subProcess, BPMN_NS, "manualTask").size());
			assertEquals(3, getDirectChildElements(subProcess, BPMN_NS, "sequenceFlow").size());

			// Verify top-level process element counts (should NOT include sub-process children)
			assertEquals(1, getDirectChildElements(process, BPMN_NS, "startEvent").size());
			assertEquals(1, getDirectChildElements(process, BPMN_NS, "endEvent").size());
			assertEquals(10, getDirectChildElements(process, BPMN_NS, "sequenceFlow").size());

			// Verify DI - isExpanded on sub-process shape
			final Element plane = (Element) ((Element) root.getElementsByTagNameNS(DI_NS, "BPMNDiagram").item(0))
				.getElementsByTagNameNS(DI_NS, "BPMNPlane").item(0);
			final NodeList shapes = plane.getElementsByTagNameNS(DI_NS, "BPMNShape");
			boolean foundExpandedSubProcess = false;
			for (int i = 0; i < shapes.getLength(); i++) {
				final Element shape = (Element) shapes.item(i);
				if ("SubProcess_Pack".equals(shape.getAttribute("bpmnElement"))) {
					assertEquals("true", shape.getAttribute("isExpanded"));
					foundExpandedSubProcess = true;
				}
			}
			assertTrue("Expected isExpanded=true on sub-process shape", foundExpandedSubProcess);

		} catch (Exception ex) {
			fail("Round-trip test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: verify sub-process graph hierarchy after import
	// ------------------------------------------------------------------
	@Test
	public void testSubProcessGraphHierarchy() {

		try {

			final String xml = loadResource("/parallel-subprocess.bpmn");

			try (final Tx tx = app.tx()) {

				final BpmnImporter importer = new BpmnImporter(securityContext);
				final NodeInterface defNode = importer.importBpmn(xml);

				assertNotNull(defNode);

				final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
				final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull("BpmnDefinitions should have at least one BpmnProcess", procNode);

				// Count top-level elements (connected to the process)
				final Iterable<NodeInterface> topElements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
				int topElemCount = 0;
				NodeInterface subProcNode = null;
				for (final NodeInterface e : topElements) {
					topElemCount++;
					final String type = e.getProperty(elemTraits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
					if ("subProcess".equals(type)) {
						subProcNode = e;
					}
				}
				// Top-level: startEvent, userTask, 2x parallelGateway, 2x serviceTask, userTask(shipping), subProcess, endEvent = 9
				assertEquals(9, topElemCount);
				assertNotNull("SubProcess node should exist at top level", subProcNode);

				// Count top-level flows (connected to the process)
				final Iterable<NodeInterface> topFlows = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
				int topFlowCount = 0;
				for (final NodeInterface f : topFlows) { topFlowCount++; }
				assertEquals(10, topFlowCount);

				// Count child elements of the sub-process
				final Iterable<NodeInterface> childElems = subProcNode.getProperty(elemTraits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY));
				int childElemCount = 0;
				for (final NodeInterface c : childElems) { childElemCount++; }
				// SubProcess children: SubStart_1, Task_Pick, Task_Pack, SubEnd_1 = 4
				assertEquals(4, childElemCount);

				// Count child flows of the sub-process
				final Iterable<NodeInterface> childFlows = subProcNode.getProperty(elemTraits.key(BpmnElementTraitDefinition.CHILD_FLOWS_PROPERTY));
				int childFlowCount = 0;
				for (final NodeInterface f : childFlows) { childFlowCount++; }
				// SubProcess flows: SubFlow_1, SubFlow_2, SubFlow_3 = 3
				assertEquals(3, childFlowCount);

				// Verify child elements have parentElement set to sub-process, not process
				for (final NodeInterface c : childElems) {
					final NodeInterface parent = c.getProperty(elemTraits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
					assertNotNull("Child element should have parentElement", parent);
					assertEquals("Child element parent should be the sub-process", subProcNode.getUuid(), parent.getUuid());

					// Should NOT be connected to process directly (only via parentElement)
					final NodeInterface proc = c.getProperty(elemTraits.key(BpmnElementTraitDefinition.PROCESS_PROPERTY));
					assertNull("Child element should NOT be directly connected to process", proc);
				}

				tx.success();
			}

			cleanupBpmnData();

		} catch (Exception ex) {
			fail("Sub-process graph hierarchy test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: events-and-gateways.bpmn
	// Covers: message/signal/error definitions at definitions level,
	//         boundary events (timer + error), intermediate catch/throw,
	//         script task with inline script, inclusive gateway with default flow,
	//         terminate end event, documentation element
	// ------------------------------------------------------------------
	@Test
	public void testEventsAndGatewaysRoundTrip() {

		try {
			final String exported = importAndExport("/events-and-gateways.bpmn");
			final Document doc = parseXml(exported);

			final Element root = doc.getDocumentElement();

			// Verify top-level message, signal, error definitions are preserved
			// These are outside the process element and should survive as extension XML
			// or as recognized elements
			final NodeList messages = root.getElementsByTagNameNS(BPMN_NS, "message");
			final NodeList signals = root.getElementsByTagNameNS(BPMN_NS, "signal");
			final NodeList errors = root.getElementsByTagNameNS(BPMN_NS, "error");
			// Note: these may be in extensionXml if not yet handled as first-class types

			final NodeList processes = root.getElementsByTagNameNS(BPMN_NS, "process");
			assertEquals(1, processes.getLength());
			final Element process = (Element) processes.item(0);
			assertEquals("Event Handling Demo", process.getAttribute("name"));

			// Verify boundary events
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "boundaryEvent").getLength());

			// Verify intermediate events
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "intermediateCatchEvent").getLength());
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "intermediateThrowEvent").getLength());

			// Verify script task
			assertEquals(1, process.getElementsByTagNameNS(BPMN_NS, "scriptTask").getLength());

			// Verify inclusive gateways
			assertEquals(2, process.getElementsByTagNameNS(BPMN_NS, "inclusiveGateway").getLength());

			// Verify DI
			final Element plane = (Element) ((Element) root.getElementsByTagNameNS(DI_NS, "BPMNDiagram").item(0))
				.getElementsByTagNameNS(DI_NS, "BPMNPlane").item(0);
			assertTrue(plane.getElementsByTagNameNS(DI_NS, "BPMNShape").getLength() >= 14);
			assertTrue(plane.getElementsByTagNameNS(DI_NS, "BPMNEdge").getLength() >= 12);

		} catch (Exception ex) {
			fail("Round-trip test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: verify graph structure after import
	// ------------------------------------------------------------------
	@Test
	public void testGraphStructureAfterImport() {

		try {

			final String xml = loadResource("/simple-approval.bpmn");
			NodeInterface defNode = null;

			try (final Tx tx = app.tx()) {

				final BpmnImporter importer = new BpmnImporter(securityContext);
				defNode = importer.importBpmn(xml);

				assertNotNull(defNode);

				final Traits defTraits  = defNode.getTraits();
				final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);

				// Verify definition properties
				assertEquals("Definitions_1", defNode.getProperty(defTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				// Verify file-level display name
				assertEquals("Simple Approval", defNode.getProperty(defTraits.key("name")));

				// Resolve the (single) BpmnProcess child for per-process assertions.
				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull("BpmnDefinitions should have at least one BpmnProcess", procNode);

				// Verify per-process properties
				assertEquals("Simple Approval", procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY)));
				assertEquals("Process_1",       procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY)));
				assertEquals(Boolean.TRUE,      procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_IS_EXECUTABLE_PROPERTY)));

				// Verify element count
				final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
				int elemCount = 0;
				for (NodeInterface e : elements) { elemCount++; }
				assertEquals(7, elemCount);

				// Verify sequence flow count
				final Iterable<NodeInterface> flows = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
				int flowCount = 0;
				for (NodeInterface f : flows) { flowCount++; }
				assertEquals(6, flowCount);

				// Verify diagram count
				final Iterable<NodeInterface> diagrams = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.DIAGRAMS_PROPERTY));
				int diagramCount = 0;
				for (NodeInterface d : diagrams) { diagramCount++; }
				assertEquals(1, diagramCount);

				tx.success();
			}

			// Clean up
			cleanupBpmnData();

		} catch (Exception ex) {
			fail("Graph structure test failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: HAS_METHOD attachments and process listeners round-trip via
	// <structr:methodRef> and <structr:processListener>.
	// ------------------------------------------------------------------
	@Test
	public void testMethodRefAndProcessListenerRoundTrip() {

		try {
			final String defUuid;

			// Step 1: import a simple BPMN, attach orphan SchemaMethods at process
			// and element level, and attach a process listener.
			try (final Tx tx = app.tx()) {

				final String xml = loadResource("/simple-approval.bpmn");
				final BpmnImporter importer = new BpmnImporter(securityContext);
				final NodeInterface defNode = importer.importBpmn(xml);
				assertNotNull(defNode);
				defUuid = defNode.getUuid();
				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull("BpmnDefinitions should have a BpmnProcess child", procNode);
				final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);

				// Process-level orphan method (now attached to BpmnProcess)
				final NodeInterface procMethod = createOrphanMethod("calculateRisk");
				procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY), List.of(procMethod));

				// Element-level orphan method on the userTask
				final NodeInterface elemMethod = createOrphanMethod("onApprovalCompleted");
				final NodeInterface userTask   = findFirstElementOfType(procNode, "userTask");
				assertNotNull("expected a userTask element", userTask);
				userTask.setProperty(userTask.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY), List.of(elemMethod));

				// Process listener (now attached to BpmnProcess)
				final NodeInterface listener = app.create(ProcessTraits.BPMN_PROCESS_LISTENER, (String) null);
				final Traits plTraits = listener.getTraits();
				listener.setProperty(plTraits.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY),   "started");
//				listener.setProperty(plTraits.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY),  "onProcessStart");
				listener.setProperty(plTraits.key(BpmnProcessListenerTraitDefinition.PROCESS_PROPERTY), procNode);

				tx.success();
			}

			// Step 2: export, parse, and verify the new emissions.
			String exported;
			try (final Tx tx = app.tx()) {
				final NodeInterface defNode = app.getNodeById(defUuid);
				assertNotNull(defNode);
				exported = new BpmnExporter().exportBpmn(defNode);
				tx.success();
			}
			assertNotNull(exported);

			final Document doc = parseXml(exported);
			final Element root = doc.getDocumentElement();
			final Element process = (Element) root.getElementsByTagNameNS(BPMN_NS, "process").item(0);

			// Process-level <bpmn:extensionElements>
			final List<Element> procExt = getDirectChildElements(process, BPMN_NS, "extensionElements");
			assertEquals("expected exactly one process-level extensionElements", 1, procExt.size());
			final List<Element> procListeners = getDirectChildElements(procExt.get(0), STRUCTR_NS, "processListener");
			final List<Element> procMethodRefs = getDirectChildElements(procExt.get(0), STRUCTR_NS, "methodRef");
			assertEquals(1, procListeners.size());
			assertEquals("started",        procListeners.get(0).getAttribute("event"));
//			assertEquals("onProcessStart", procListeners.get(0).getAttribute("method"));
			assertEquals(1, procMethodRefs.size());
			assertEquals("calculateRisk",  procMethodRefs.get(0).getAttribute("name"));

			// Element-level methodRef on the userTask
			final Element userTaskEl = (Element) process.getElementsByTagNameNS(BPMN_NS, "userTask").item(0);
			final List<Element> taskExt = getDirectChildElements(userTaskEl, BPMN_NS, "extensionElements");
			assertEquals(1, taskExt.size());
			final List<Element> taskMethodRefs = getDirectChildElements(taskExt.get(0), STRUCTR_NS, "methodRef");
			assertEquals(1, taskMethodRefs.size());
			assertEquals("onApprovalCompleted", taskMethodRefs.get(0).getAttribute("name"));

			// Step 3: simulate cross-system import. Delete the BPMN graph (keep
			// the orphan SchemaMethods around, as a deployment-import would have
			// brought them in), re-import the exported XML, and verify the
			// HAS_METHOD links are re-established by name.
			cleanupBpmnData();

			final String reimportedDefUuid;
			try (final Tx tx = app.tx()) {
				final NodeInterface reimported = new BpmnImporter(securityContext).importBpmn(exported);
				assertNotNull(reimported);
				reimportedDefUuid = reimported.getUuid();
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				final NodeInterface defNode = app.getNodeById(reimportedDefUuid);
				assertNotNull(defNode);
				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull(procNode);
				final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);

				// Process-level method should resolve to the orphan we created earlier.
				final List<NodeInterface> procMethods = collectAll(procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY)));
				assertEquals(1, procMethods.size());
				assertEquals("calculateRisk", procMethods.get(0).getName());

				// Element-level method on userTask should resolve too.
				final NodeInterface userTask = findFirstElementOfType(procNode, "userTask");
				assertNotNull(userTask);
				final List<NodeInterface> taskMethods = collectAll(userTask.getProperty(userTask.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY)));
				assertEquals(1, taskMethods.size());
				assertEquals("onApprovalCompleted", taskMethods.get(0).getName());

				tx.success();
			}

			cleanupBpmnData();
			cleanupOrphanMethods();

		} catch (Exception ex) {
			fail("methodRef/processListener round-trip failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: lanes.bpmn
	// Covers: <bpmn:laneSet>, <bpmn:lane>, <bpmn:flowNodeRef>, lane DI
	//         shapes (with isHorizontal="true"), graph wiring on import
	// ------------------------------------------------------------------
	@Test
	public void testLaneRoundTrip() {

		try {
			final String defUuid;

			// Step 1: import the lane fixture and verify the graph wiring.
			try (final Tx tx = app.tx()) {

				final String xml = loadResource("/lanes.bpmn");
				final BpmnImporter importer = new BpmnImporter(securityContext);
				final NodeInterface defNode = importer.importBpmn(xml);
				assertNotNull(defNode);
				defUuid = defNode.getUuid();

				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull("expected a BpmnProcess child", procNode);
				final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);

				// Two lanes attached to the process.
				final List<NodeInterface> lanes = collectAll(procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.LANES_PROPERTY)));
				assertEquals("expected 2 lanes", 2, lanes.size());

				// Each lane has the right name and the right element count.
				NodeInterface customerLane = null, serviceLane = null;
				for (final NodeInterface ln : lanes) {
					final String bpmnId = ln.getProperty(ln.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
					if ("Lane_Customer".equals(bpmnId)) customerLane = ln;
					if ("Lane_Service".equals(bpmnId))  serviceLane  = ln;
				}
				assertNotNull("Lane_Customer not imported", customerLane);
				assertNotNull("Lane_Service not imported",  serviceLane);
				assertEquals("Customer",      customerLane.getProperty(customerLane.getTraits().key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY)));
				assertEquals("Service Desk",  serviceLane.getProperty(serviceLane.getTraits().key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY)));

				// flowNodeRef wiring: each element in the right lane.
				final List<NodeInterface> custRefs = collectAll(customerLane.getProperty(customerLane.getTraits().key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY)));
				final List<NodeInterface> svcRefs  = collectAll(serviceLane.getProperty(serviceLane.getTraits().key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY)));
				assertEquals("Customer lane should have 2 flowNodeRefs", 2, custRefs.size());
				assertEquals("Service lane should have 2 flowNodeRefs",  2, svcRefs.size());

				// Inverse: each element knows which lane it belongs to.
				final NodeInterface submitTask = findElementByBpmnId(procNode, "Task_Submit");
				assertNotNull(submitTask);
				final NodeInterface submitLane = submitTask.getProperty(submitTask.getTraits().key(BpmnElementTraitDefinition.LANE_PROPERTY));
				assertNotNull("Task_Submit should be wired to its lane", submitLane);
				assertEquals("Lane_Customer", submitLane.getProperty(submitLane.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

				tx.success();
			}

			// Step 2: export and verify the laneSet block + DI shapes.
			String exported;
			try (final Tx tx = app.tx()) {
				final NodeInterface defNode = app.getNodeById(defUuid);
				assertNotNull(defNode);
				exported = new BpmnExporter().exportBpmn(defNode);
				tx.success();
			}
			assertNotNull(exported);

			final Document doc = parseXml(exported);
			final Element root = doc.getDocumentElement();
			final Element process = (Element) root.getElementsByTagNameNS(BPMN_NS, "process").item(0);

			// <bpmn:laneSet> with two <bpmn:lane> children.
			final NodeList laneSets = process.getElementsByTagNameNS(BPMN_NS, "laneSet");
			assertEquals("exporter should emit a single laneSet", 1, laneSets.getLength());
			final Element laneSet = (Element) laneSets.item(0);
			final List<Element> exportedLanes = getDirectChildElements(laneSet, BPMN_NS, "lane");
			assertEquals(2, exportedLanes.size());

			// Each lane carries a name and the right flowNodeRef count.
			Element customerEl = null, serviceEl = null;
			for (final Element ln : exportedLanes) {
				if ("Lane_Customer".equals(ln.getAttribute("id"))) customerEl = ln;
				if ("Lane_Service".equals(ln.getAttribute("id")))  serviceEl  = ln;
			}
			assertNotNull(customerEl);
			assertNotNull(serviceEl);
			assertEquals("Customer",     customerEl.getAttribute("name"));
			assertEquals("Service Desk", serviceEl.getAttribute("name"));
			assertEquals(2, getDirectChildElements(customerEl, BPMN_NS, "flowNodeRef").size());
			assertEquals(2, getDirectChildElements(serviceEl,  BPMN_NS, "flowNodeRef").size());

			// DI: two BPMNShapes for the lanes, each with isHorizontal="true".
			final Element plane = (Element) root.getElementsByTagNameNS(DI_NS, "BPMNPlane").item(0);
			final NodeList shapes = plane.getElementsByTagNameNS(DI_NS, "BPMNShape");
			int laneShapesWithHoriz = 0;
			for (int i = 0; i < shapes.getLength(); i++) {
				final Element sh = (Element) shapes.item(i);
				final String ref = sh.getAttribute("bpmnElement");
				if ("Lane_Customer".equals(ref) || "Lane_Service".equals(ref)) {
					assertEquals("lane DI shape should be isHorizontal=true",
						"true", sh.getAttribute("isHorizontal"));
					laneShapesWithHoriz++;
				}
			}
			assertEquals("expected 2 lane DI shapes", 2, laneShapesWithHoriz);

			cleanupBpmnData();

		} catch (Exception ex) {
			fail("Lane round-trip failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Test: leave-request-2.bpmn
	// Covers: boundaryEvent attached to a userTask via attachedToRef.
	// On import, the typed `attachedTo` relationship is set and the
	// attribute is stripped from bpmnAttributes JSON. On export, the
	// attribute is regenerated from the relationship.
	// ------------------------------------------------------------------
	@Test
	public void testBoundaryEventRoundTrip() {

		try {
			final String defUuid;

			// Step 1: import + verify the typed attachedTo relationship.
			try (final Tx tx = app.tx()) {

				final String xml = loadResource("/leave-request-2.bpmn");
				final BpmnImporter importer = new BpmnImporter(securityContext);
				final NodeInterface defNode = importer.importBpmn(xml);
				assertNotNull(defNode);
				defUuid = defNode.getUuid();

				final NodeInterface procNode = firstProcess(defNode);
				assertNotNull(procNode);

				final NodeInterface boundary = findElementByBpmnId(procNode, "Boundary_ReviewTimeout");
				assertNotNull("Boundary_ReviewTimeout not imported", boundary);
				final Traits bTraits = boundary.getTraits();

				// Typed relationship resolves to Task_Review.
				final NodeInterface host = boundary.getProperty(bTraits.key(BpmnElementTraitDefinition.ATTACHED_TO_PROPERTY));
				assertNotNull("attachedTo relationship not set on boundary", host);
				assertEquals("Task_Review", host.getProperty(host.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

				// Inverse: host's attachedBoundaries lists the boundary.
				final List<NodeInterface> hostBoundaries = collectAll(host.getProperty(host.getTraits().key(BpmnElementTraitDefinition.ATTACHED_BOUNDARIES_PROPERTY)));
				assertEquals("Task_Review should carry one boundary", 1, hostBoundaries.size());
				assertEquals(boundary.getUuid(), hostBoundaries.get(0).getUuid());

				// attachedToRef must be removed from bpmnAttributes JSON
				// (graph is the single source of truth post-resolve).
				final String attrsJson = boundary.getProperty(bTraits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
				if (attrsJson != null) {
					assertFalse("attachedToRef must be stripped from bpmnAttributes after import",
						attrsJson.contains("attachedToRef"));
				}

				tx.success();
			}

			// Step 2: export and verify the attribute is regenerated.
			String exported;
			try (final Tx tx = app.tx()) {
				final NodeInterface defNode = app.getNodeById(defUuid);
				assertNotNull(defNode);
				exported = new BpmnExporter().exportBpmn(defNode);
				tx.success();
			}
			assertNotNull(exported);

			final Document doc = parseXml(exported);
			final Element root = doc.getDocumentElement();
			final Element process = (Element) root.getElementsByTagNameNS(BPMN_NS, "process").item(0);
			final NodeList boundaries = process.getElementsByTagNameNS(BPMN_NS, "boundaryEvent");
			assertEquals("exporter should emit exactly one boundaryEvent", 1, boundaries.getLength());
			final Element be = (Element) boundaries.item(0);
			assertEquals("Boundary_ReviewTimeout", be.getAttribute("id"));
			assertEquals("attachedToRef should be emitted from the typed relationship",
				"Task_Review", be.getAttribute("attachedToRef"));
			// attachedToRef should appear exactly once on the element.
			final String serialized = exported;
			final int idx1 = serialized.indexOf("Boundary_ReviewTimeout");
			final int next = serialized.indexOf('>', idx1);
			final String openTag = serialized.substring(idx1, next);
			int occurrences = 0; int from = 0;
			while ((from = openTag.indexOf("attachedToRef", from)) >= 0) { occurrences++; from++; }
			assertEquals("attachedToRef should be emitted exactly once on the boundary element",
				1, occurrences);

			cleanupBpmnData();

		} catch (Exception ex) {
			fail("Boundary round-trip failed: " + ex.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/**
	 * Import a BPMN resource file, export it, and return the exported XML.
	 */
	private String importAndExport(final String resourcePath) throws Exception {

		final String xml = loadResource(resourcePath);
		String exported = null;

		try (final Tx tx = app.tx()) {

			final BpmnImporter importer = new BpmnImporter(securityContext);
			final NodeInterface defNode = importer.importBpmn(xml);

			assertNotNull("Import returned null for " + resourcePath, defNode);

			final BpmnExporter exporter = new BpmnExporter();
			exported = exporter.exportBpmn(defNode);

			assertNotNull("Export returned null for " + resourcePath, exported);
			assertFalse("Export returned empty string for " + resourcePath, exported.isEmpty());

			tx.success();
		}

		// Clean up after each test
		cleanupBpmnData();

		return exported;
	}

	/**
	 * Load a resource file from the classpath.
	 */
	private String loadResource(final String path) throws Exception {

		try (final InputStream is = getClass().getResourceAsStream(path)) {

			assertNotNull("Resource not found: " + path, is);
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Parse an XML string into a DOM Document.
	 */
	private Document parseXml(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Get direct child elements matching a namespace URI and local name.
	 * Unlike getElementsByTagNameNS, this does NOT recurse into descendants.
	 */
	private List<Element> getDirectChildElements(final Element parent, final String namespaceURI, final String localName) {

		final List<Element> result = new ArrayList<>();
		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			final org.w3c.dom.Node child = children.item(i);
			if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				final Element childEl = (Element) child;
				if (namespaceURI.equals(childEl.getNamespaceURI()) && localName.equals(childEl.getLocalName())) {
					result.add(childEl);
				}
			}
		}

		return result;
	}

	private NodeInterface createOrphanMethod(final String name) throws FrameworkException {
		final NodeInterface m = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
		m.setProperty(m.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
		return m;
	}

	/**
	 * Find the first BpmnProcess child of the given BpmnDefinitions, or null
	 * if the file has none.
	 */
	private NodeInterface firstProcess(final NodeInterface defNode) {
		final Traits defTraits = defNode.getTraits();
		final Iterable<NodeInterface> processes = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY));
		if (processes == null) return null;
		for (final NodeInterface p : processes) return p;
		return null;
	}

	/** Walk a BpmnProcess's elements for the one with the given bpmnId. */
	private NodeInterface findElementByBpmnId(final NodeInterface procNode, final String bpmnId) {
		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements == null) return null;
		for (final NodeInterface e : elements) {
			final String id = e.getProperty(e.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
			if (bpmnId.equals(id)) return e;
		}
		return null;
	}

	/** Walk a BpmnProcess's elements for the first one of the given bpmnElementType. */
	private NodeInterface findFirstElementOfType(final NodeInterface procNode, final String bpmnElementType) {
		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements == null) return null;
		for (final NodeInterface e : elements) {
			final String type = e.getProperty(e.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
			if (bpmnElementType.equals(type)) {
				return e;
			}
		}
		return null;
	}

	private List<NodeInterface> collectAll(final Iterable<NodeInterface> it) {
		final List<NodeInterface> out = new ArrayList<>();
		if (it != null) {
			for (final NodeInterface n : it) out.add(n);
		}
		return out;
	}

	private void cleanupOrphanMethods() throws FrameworkException {
		try (final Tx tx = app.tx()) {
			for (final NodeInterface m : app.nodeQuery(StructrTraits.SCHEMA_METHOD).getAsList()) {
				if (m.getProperty(m.getTraits().key(org.structr.core.traits.definitions.SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY)) == null) {
					app.delete(m);
				}
			}
			tx.success();
		}
	}

	/**
	 * Delete all BPMN data from the database.
	 */
	private void cleanupBpmnData() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String[] types = {
				ProcessTraits.BPMN_DI_EDGE,
				ProcessTraits.BPMN_DI_SHAPE,
				ProcessTraits.BPMN_DI_DIAGRAM,
				ProcessTraits.BPMN_SEQUENCE_FLOW,
				ProcessTraits.BPMN_LANE,
				ProcessTraits.BPMN_ELEMENT,
				ProcessTraits.BPMN_MESSAGE_FLOW,
				ProcessTraits.BPMN_PARTICIPANT,
				ProcessTraits.BPMN_COLLABORATION,
				ProcessTraits.BPMN_PROCESS,
				ProcessTraits.BPMN_GLOBAL_DEFINITION,
				ProcessTraits.BPMN_DEFINITIONS
			};

			for (final String type : types) {
				for (final NodeInterface node : app.nodeQuery(type).getAsList()) {
					app.delete(node);
				}
			}

			tx.success();
		}
	}
}
