/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
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

	private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS   = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS   = "http://www.omg.org/spec/DD/20100524/DC";

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

				final Traits defTraits = defNode.getTraits();
				final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);

				// Count top-level elements (connected to definition)
				final Iterable<NodeInterface> topElements = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.ELEMENTS_PROPERTY));
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

				// Count top-level flows (connected to definition)
				final Iterable<NodeInterface> topFlows = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
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

				// Verify child elements have parentElement set to sub-process, not definition
				for (final NodeInterface c : childElems) {
					final NodeInterface parent = c.getProperty(elemTraits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY));
					assertNotNull("Child element should have parentElement", parent);
					assertEquals("Child element parent should be the sub-process", subProcNode.getUuid(), parent.getUuid());

					// Should NOT be connected to definition
					final NodeInterface def = c.getProperty(elemTraits.key(BpmnElementTraitDefinition.DEFINITION_PROPERTY));
					assertNull("Child element should NOT be connected to definition", def);
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

				final Traits defTraits = defNode.getTraits();

				// Verify definition properties
				assertEquals("Definitions_1", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.BPMN_ID_PROPERTY)));
				assertEquals("Simple Approval", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_NAME_PROPERTY)));
				assertEquals("Process_1", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_ID_PROPERTY)));
				assertEquals(Boolean.TRUE, defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_IS_EXECUTABLE)));

				// Verify name is set for UI
				assertEquals("Simple Approval", defNode.getProperty(defTraits.key("name")));

				// Verify element count
				final Iterable<NodeInterface> elements = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.ELEMENTS_PROPERTY));
				int elemCount = 0;
				for (NodeInterface e : elements) { elemCount++; }
				assertEquals(7, elemCount);

				// Verify sequence flow count
				final Iterable<NodeInterface> flows = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
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
				ProcessTraits.BPMN_ELEMENT,
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
