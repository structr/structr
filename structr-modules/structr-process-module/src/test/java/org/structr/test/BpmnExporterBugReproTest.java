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
import org.structr.core.traits.Traits;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.traits.definitions.BpmnDefinitionsTraitDefinition;
import org.structr.process.traits.definitions.BpmnDiShapeTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnTaskListenerTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * FAILING reproductions for the BpmnExporter findings. Each test asserts the
 * intended behaviour and currently FAILS because the bug is present. Once the
 * corresponding bug is fixed, the test should pass and become a regression guard.
 */
public class BpmnExporterBugReproTest extends StructrUiTest {

	private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

	/**
	 * EXP-1: {@code xmlns:xsi} is never ensured, but conditional flows (and timer
	 * event definitions) emit {@code xsi:type} attributes. A graph whose stored
	 * {@code namespaceDeclarations} does not declare {@code xmlns:xsi} (e.g. one
	 * authored natively rather than imported) throws
	 * {@code XMLStreamException: ... has not been bound to any prefix} on export.
	 */
	@Test
	public void testConditionalFlowExportsWithoutXsiInNamespaceDeclarations() {

		final String defUuid = importDef("/simple-approval.bpmn");
		Throwable thrown = null;
		String exported  = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = app.getNodeById(defUuid);
			final Traits t              = defNode.getTraits();

			// Simulate a natively-authored definition: namespace map WITHOUT xmlns:xsi.
			defNode.setProperty(t.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS), "{\"xmlns:bpmn\":\"http://www.omg.org/spec/BPMN/20100524/MODEL\"}");

			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();

		} catch (final Throwable t) {

			thrown = t;
		}

		assertNull("EXP-1: export of a conditional flow must not fail when xmlns:xsi is absent "
			+ "from namespaceDeclarations (xsi should be auto-ensured). Got: "
			+ (thrown != null ? thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : ""), thrown);
		assertTrue("EXP-1: export should still emit an xsi:type attribute", exported != null && exported.contains("xsi:type"));
	}

	/**
	 * EXP-2: {@code <documentation>} is emitted AFTER {@code <extensionElements>}.
	 * The BPMN {@code tBaseElement} content model requires {@code documentation}
	 * (0..n) to precede {@code extensionElements} (0..1); strict schema validators
	 * reject the current output.
	 */
	@Test
	public void testDocumentationPrecedesExtensionElements() throws Exception {

		final String defUuid = importDef("/simple-approval.bpmn");

		final String exported;

		try (final Tx tx = app.tx()) {

			// Give the userTask BOTH documentation AND a task listener so
			// extensionElements is emitted.
			final NodeInterface userTask = firstElementOfBpmnType("userTask");
			assertNotNull("fixture should contain a userTask", userTask);
			final Traits et = userTask.getTraits();

			userTask.setProperty(et.key(BpmnElementTraitDefinition.DOCUMENTATION_PROPERTY), "Please review the request carefully.");

			final NodeInterface listener = app.create(ProcessTraits.BPMN_TASK_LISTENER, (String) null);
			listener.setProperty(listener.getTraits().key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY), "created");
			userTask.setProperty(et.key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY), List.of(listener));

			final NodeInterface defNode = app.getNodeById(defUuid);
			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();
		}

		final Document doc     = parseXml(exported);
		final Element userTask = (Element) doc.getElementsByTagNameNS(BPMN_NS, "userTask").item(0);

		assertNotNull("exported XML should contain a userTask", userTask);

		final int docIdx = firstChildIndex(userTask, "documentation");
		final int extIdx = firstChildIndex(userTask, "extensionElements");

		assertTrue("EXP-2: userTask must contain both <documentation> and <extensionElements> (doc=" + docIdx + ", ext=" + extIdx + ")", docIdx >= 0 && extIdx >= 0);
		assertTrue("EXP-2: <documentation> must appear before <extensionElements> per BPMN tBaseElement (doc=" + docIdx + ", ext=" + extIdx + ")", docIdx < extIdx);
	}

	/**
	 * EXP-3: the {@code bpmnAttributes} JSON is only null-guarded, then handed to
	 * {@code gson.fromJson}. A literal {@code "null"} string parses to {@code null}
	 * and the subsequent {@code entrySet()} iteration NPEs; malformed JSON throws
	 * an uncaught {@code JsonSyntaxException}. Either escapes the exporter's
	 * {@code XMLStreamException}-only catch and aborts the whole export.
	 */
	@Test
	public void testMalformedBpmnAttributesDoesNotAbortExport() {

		final String defUuid = importDef("/simple-approval.bpmn");
		Throwable thrown = null;
		String exported  = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface element = firstElementOfBpmnType("userTask");
			assertNotNull(element);
			element.setProperty(element.getTraits().key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY), "null");

			final NodeInterface defNode = app.getNodeById(defUuid);
			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();

		} catch (final Throwable t) {

			thrown = t;
		}

		assertNull("EXP-3: a literal \"null\"/malformed bpmnAttributes value must not crash the export "
			+ "(should be tolerated or raise a clean FrameworkException). Got: "
			+ (thrown != null ? thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : ""), thrown);
		assertNotNull(exported);
	}

	/**
	 * EXP-4: DI label bounds attributes are written via {@code writeAttribute("x",
	 * lb.get("x"))} with no null guard (unlike the shape's own Bounds, which use
	 * the null-safe {@code writeAttrDouble}). A {@code labelBounds} JSON missing
	 * {@code width}/{@code height} yields {@code writeAttribute(name, null)} → NPE.
	 */
	@Test
	public void testDiLabelBoundsMissingDimensionsDoesNotCrash() {

		final String defUuid = importDef("/simple-approval.bpmn");
		Throwable thrown = null;
		String exported  = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface shape = app.nodeQuery(ProcessTraits.BPMN_DI_SHAPE).getFirst();
			assertNotNull("fixture should contain a BPMNShape", shape);
			final Traits st = shape.getTraits();

			// label bounds JSON missing width/height
			shape.setProperty(st.key(BpmnDiShapeTraitDefinition.LABEL_BOUNDS_PROPERTY), "{\"x\":\"10\",\"y\":\"20\"}");
			shape.setProperty(st.key(BpmnDiShapeTraitDefinition.HAS_LABEL_PROPERTY), true);

			final NodeInterface defNode = app.getNodeById(defUuid);
			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();

		} catch (final Throwable t) {

			thrown = t;
		}

		assertNull("EXP-4: incomplete DI label bounds (missing width/height) must not NPE the export. Got: "
			+ (thrown != null ? thrown.getClass().getSimpleName() + ": " + thrown.getMessage() : ""), thrown);
		assertNotNull(exported);
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private String importDef(final String resource) {

		final String xml = loadResource(resource);
		String defUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode = new BpmnImporter(securityContext).importBpmn(xml);
			assertNotNull("import returned null for " + resource, defNode);
			defUuid = defNode.getUuid();

			tx.success();

		} catch (final Exception ex) {

			throw new RuntimeException("import failed for " + resource, ex);
		}

		return defUuid;
	}

	private NodeInterface firstElementOfBpmnType(final String bpmnType) throws Exception {

		for (final NodeInterface e : app.nodeQuery(ProcessTraits.BPMN_ELEMENT).getResultStream()) {

			if (bpmnType.equals(e.getProperty(e.getTraits().key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY)))) {

				return e;
			}
		}

		return null;
	}

	/** Index of the first direct child of {@code parent} with the given BPMN local name, or -1. */
	private int firstChildIndex(final Element parent, final String localName) {

		final NodeList children = parent.getChildNodes();
		int elementIndex = 0;

		for (int i = 0; i < children.getLength(); i++) {

			final org.w3c.dom.Node child = children.item(i);
			if (child.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {

				continue;
			}

			final Element el = (Element) child;
			if (BPMN_NS.equals(el.getNamespaceURI()) && localName.equals(el.getLocalName())) {

				return elementIndex;
			}

			elementIndex++;
		}

		return -1;
	}

	private String loadResource(final String path) {

		try (final InputStream is = getClass().getResourceAsStream(path)) {

			assertNotNull("resource not found: " + path, is);

			return new String(is.readAllBytes(), StandardCharsets.UTF_8);

		} catch (final Exception ex) {

			throw new RuntimeException("could not load " + path, ex);
		}
	}

	private Document parseXml(final String xml) throws Exception {

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		final DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}
}
