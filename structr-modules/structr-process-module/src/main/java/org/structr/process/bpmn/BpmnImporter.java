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
package org.structr.process.bpmn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.traits.definitions.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

/**
 * Structr-native BPMN 2.0.2 XML importer. Parses BPMN XML using standard Java DOM
 * parsing and creates graph nodes with appropriate traits. No third-party BPMN libraries.
 *
 * All BPMN content is stored as typed graph properties. There is no raw XML storage
 * (no bpmnChildXml). The graph is the single source of truth.
 *
 * Sub-process children are stored as proper graph nodes connected via HAS_CHILD_ELEMENT
 * and HAS_CHILD_FLOW relationships, forming a true containment hierarchy in the graph.
 */
public class BpmnImporter {

	private static final Logger logger = LoggerFactory.getLogger(BpmnImporter.class);

	private static final String BPMN_NS  = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS    = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS    = "http://www.omg.org/spec/DD/20100524/DC";
	private static final String OMGDI_NS = "http://www.omg.org/spec/DD/20100524/DI";
	private static final String XSI_NS   = "http://www.w3.org/2001/XMLSchema-instance";

	private static final Set<String> KNOWN_ELEMENT_TYPES = Set.of(
		"startEvent", "endEvent", "intermediateThrowEvent", "intermediateCatchEvent",
		"userTask", "serviceTask", "scriptTask", "manualTask", "task",
		"exclusiveGateway", "parallelGateway", "inclusiveGateway", "eventBasedGateway",
		"subProcess", "callActivity",
		"boundaryEvent",
		"dataObjectReference", "dataStoreReference", "dataObject",
		"association"
	);

	/** Event definition local names that we extract into typed properties. */
	private static final Set<String> EVENT_DEFINITION_TYPES = Set.of(
		"timerEventDefinition", "errorEventDefinition", "messageEventDefinition",
		"signalEventDefinition", "terminateEventDefinition", "conditionalEventDefinition",
		"compensateEventDefinition", "escalationEventDefinition", "linkEventDefinition",
		"cancelEventDefinition"
	);

	/** Timer sub-element types. */
	private static final Set<String> TIMER_SUB_TYPES = Set.of("timeDuration", "timeCycle", "timeDate");

	private final SecurityContext securityContext;
	private final Gson gson;

	public BpmnImporter(final SecurityContext securityContext) {
		this.securityContext = securityContext;
		this.gson = new GsonBuilder().create();
	}

	/**
	 * Import a BPMN 2.0.2 XML document from an InputStream and create graph nodes.
	 */
	public NodeInterface importBpmn(final InputStream inputStream) throws FrameworkException {

		try {

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(inputStream);

			return importDocument(doc);

		} catch (FrameworkException fe) {
			throw fe;
		} catch (Exception ex) {
			logger.error("Error importing BPMN XML", ex);
			throw new FrameworkException(422, "Error importing BPMN XML: " + ex.getMessage());
		}
	}

	/**
	 * Import from a BPMN XML string.
	 */
	public NodeInterface importBpmn(final String xml) throws FrameworkException {

		try {

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(new InputSource(new StringReader(xml)));

			return importDocument(doc);

		} catch (FrameworkException fe) {
			throw fe;
		} catch (Exception ex) {
			logger.error("Error importing BPMN XML", ex);
			throw new FrameworkException(422, "Error importing BPMN XML: " + ex.getMessage());
		}
	}

	private NodeInterface importDocument(final Document doc) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final Element root = doc.getDocumentElement();

		// Collect namespace declarations from root element
		final Map<String, String> namespaces = collectNamespaces(root);

		// Create BpmnDefinitions node
		final NodeInterface defNode = app.create(StructrTraits.BPMN_DEFINITIONS, (String) null);
		final Traits defTraits = defNode.getTraits();

		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.BPMN_ID_PROPERTY), root.getAttribute("id"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY), root.getAttribute("targetNamespace"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY), root.getAttribute("exporter"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY), root.getAttribute("exporterVersion"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS), gson.toJson(namespaces));

		// Import top-level global definitions (message, signal, error, etc.)
		final NodeList rootChildren = root.getChildNodes();
		for (int i = 0; i < rootChildren.getLength(); i++) {
			final Node child = rootChildren.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				final Element childEl = (Element) child;
				final String ln = childEl.getLocalName();
				if (!"process".equals(ln) && !"BPMNDiagram".equals(ln)) {
					importGlobalDefinition(app, defNode, childEl, ln);
				}
			}
		}

		// Find the bpmn:process element
		final NodeList processNodes = root.getElementsByTagNameNS(BPMN_NS, "process");
		if (processNodes.getLength() == 0) {
			throw new FrameworkException(422, "No bpmn:process element found in BPMN XML");
		}

		final Element processEl = (Element) processNodes.item(0);
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_ID_PROPERTY), processEl.getAttribute("id"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_NAME_PROPERTY), processEl.getAttribute("name"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_IS_EXECUTABLE), "true".equals(processEl.getAttribute("isExecutable")));

		final String processName = processEl.getAttribute("name");
		defNode.setProperty(defTraits.key("name"), processName != null && !processName.isEmpty() ? processName : processEl.getAttribute("id"));

		// Map of bpmnId -> NodeInterface for resolving references
		final Map<String, NodeInterface> elementMap = new LinkedHashMap<>();
		final Map<String, NodeInterface> flowMap    = new LinkedHashMap<>();

		// Import process child elements (recursively handles sub-processes)
		importProcessChildren(app, defNode, null, processEl, elementMap, flowMap);

		// Resolve sequence flow source/target references
		resolveFlowReferences(elementMap, flowMap);

		// Import DI data
		final NodeList diagramNodes = root.getElementsByTagNameNS(DI_NS, "BPMNDiagram");
		for (int i = 0; i < diagramNodes.getLength(); i++) {
			importDiagram(app, defNode, (Element) diagramNodes.item(i), elementMap, flowMap);
		}

		return defNode;
	}

	/**
	 * Recursively import child elements of a process or sub-process container.
	 */
	private void importProcessChildren(final App app, final NodeInterface defNode, final NodeInterface parentElement,
										final Element containerEl,
										final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap) throws FrameworkException {

		final NodeList children = containerEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			final Element el = (Element) child;
			final String localName = el.getLocalName();

			if ("sequenceFlow".equals(localName)) {
				final NodeInterface flowNode = importSequenceFlow(app, defNode, parentElement, el);
				flowMap.put(el.getAttribute("id"), flowNode);
			} else if ("subProcess".equals(localName)) {
				final NodeInterface subProcNode = importElement(app, defNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), subProcNode);
				importProcessChildren(app, defNode, subProcNode, el, elementMap, flowMap);
			} else if (KNOWN_ELEMENT_TYPES.contains(localName)) {
				final NodeInterface elemNode = importElement(app, defNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), elemNode);
			} else if (!"incoming".equals(localName) && !"outgoing".equals(localName)) {
				// Unknown element -- import as generic BpmnElement (skip incoming/outgoing refs)
				final NodeInterface elemNode = importElement(app, defNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), elemNode);
			}
		}
	}

	private void resolveFlowReferences(final Map<String, NodeInterface> elementMap,
									   final Map<String, NodeInterface> flowMap) throws FrameworkException {

		for (final Map.Entry<String, NodeInterface> entry : flowMap.entrySet()) {

			final NodeInterface flowNode = entry.getValue();
			final Traits flowTraits = flowNode.getTraits();
			final String srcRef = flowNode.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.SOURCE_REF_ID_PROPERTY));
			final String tgtRef = flowNode.getProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY));

			if (srcRef != null && elementMap.containsKey(srcRef)) {
				flowNode.setProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.SOURCE_ELEMENT_PROPERTY), elementMap.get(srcRef));
			}
			if (tgtRef != null && elementMap.containsKey(tgtRef)) {
				flowNode.setProperty(flowTraits.key(BpmnSequenceFlowTraitDefinition.TARGET_ELEMENT_PROPERTY), elementMap.get(tgtRef));
			}
		}
	}

	/**
	 * Import a single BPMN element with all its content extracted into typed properties.
	 */
	private NodeInterface importElement(final App app, final NodeInterface defNode, final NodeInterface parentElement,
										final Element el, final String elementType) throws FrameworkException {

		final NodeInterface elemNode = app.create(StructrTraits.BPMN_ELEMENT, (String) null);
		final Traits traits = elemNode.getTraits();

		elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY), elementType);

		final String name = el.getAttribute("name");
		if (name != null && !name.isEmpty()) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY), name);
			elemNode.setProperty(traits.key("name"), name);
		} else {
			elemNode.setProperty(traits.key("name"), elementType + " " + el.getAttribute("id"));
		}

		// Store extra attributes as JSON (everything except id and name)
		final Map<String, String> attrs = collectAttributes(el);
		attrs.remove("id");
		attrs.remove("name");
		if (!attrs.isEmpty()) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY), gson.toJson(attrs));
		}

		// --- Extract child elements into typed properties ---

		// Documentation
		final Element docEl = getFirstChildByLocalName(el, "documentation");
		if (docEl != null) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.DOCUMENTATION_PROPERTY), docEl.getTextContent());
		}

		// Script content (for scriptTask)
		final Element scriptEl = getFirstChildByLocalName(el, "script");
		if (scriptEl != null) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY), scriptEl.getTextContent());
		}

		// Event definitions (timer, error, message, signal, terminate, etc.)
		for (final String evtDefType : EVENT_DEFINITION_TYPES) {
			final Element evtDef = getFirstChildByLocalName(el, evtDefType);
			if (evtDef != null) {
				// Store the definition type (e.g. "timerEventDefinition")
				elemNode.setProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY), evtDefType);

				// Store the id attribute
				final String evtDefId = evtDef.getAttribute("id");
				if (evtDefId != null && !evtDefId.isEmpty()) {
					elemNode.setProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_ID_PROPERTY), evtDefId);
				}

				// Store ref attribute (errorRef, messageRef, signalRef, etc.)
				final String ref = getEventDefinitionRef(evtDef);
				if (ref != null) {
					elemNode.setProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_REF_PROPERTY), ref);
				}

				// Timer-specific: extract sub-element (timeDuration, timeCycle, timeDate)
				if ("timerEventDefinition".equals(evtDefType)) {
					for (final String timerSubType : TIMER_SUB_TYPES) {
						final Element timerEl = getFirstChildByLocalName(evtDef, timerSubType);
						if (timerEl != null) {
							elemNode.setProperty(traits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY), timerSubType);
							elemNode.setProperty(traits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY), timerEl.getTextContent().trim());

							final String xsiType = timerEl.getAttributeNS(XSI_NS, "type");
							if (xsiType != null && !xsiType.isEmpty()) {
								elemNode.setProperty(traits.key(BpmnElementTraitDefinition.TIMER_EXPRESSION_TYPE_PROPERTY), xsiType);
							}
							break;
						}
					}
				}

				break; // Only one event definition per element
			}
		}

		// Connect to definition (top-level) or parent element (sub-process child)
		if (parentElement == null) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.DEFINITION_PROPERTY), defNode);
		} else {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY), parentElement);
		}

		return elemNode;
	}

	/**
	 * Import a sequence flow.
	 */
	private NodeInterface importSequenceFlow(final App app, final NodeInterface defNode, final NodeInterface parentElement,
											 final Element el) throws FrameworkException {

		final NodeInterface flowNode = app.create(StructrTraits.BPMN_SEQUENCE_FLOW, (String) null);
		final Traits traits = flowNode.getTraits();

		flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.SOURCE_REF_ID_PROPERTY), el.getAttribute("sourceRef"));
		flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY), el.getAttribute("targetRef"));

		final String name = el.getAttribute("name");
		if (name != null && !name.isEmpty()) {
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_NAME_PROPERTY), name);
			flowNode.setProperty(traits.key("name"), name);
		} else {
			flowNode.setProperty(traits.key("name"), el.getAttribute("sourceRef") + " -> " + el.getAttribute("targetRef"));
		}

		// Condition expression
		final NodeList condExprs = el.getElementsByTagNameNS(BPMN_NS, "conditionExpression");
		if (condExprs.getLength() > 0) {
			final Element condEl = (Element) condExprs.item(0);
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_PROPERTY), condEl.getTextContent().trim());

			final String xsiType = condEl.getAttributeNS(XSI_NS, "type");
			if (xsiType != null && !xsiType.isEmpty()) {
				flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_TYPE_PROPERTY), xsiType);
			}
		}

		// Store remaining attributes
		final Map<String, String> attrs = collectAttributes(el);
		attrs.remove("id");
		attrs.remove("name");
		attrs.remove("sourceRef");
		attrs.remove("targetRef");
		if (!attrs.isEmpty()) {
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_ATTRIBUTES_PROPERTY), gson.toJson(attrs));
		}

		// Connect to definition (top-level) or parent element (sub-process child)
		if (parentElement == null) {
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.DEFINITION_PROPERTY), defNode);
		} else {
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.PARENT_ELEMENT_PROPERTY), parentElement);
		}

		return flowNode;
	}

	// --- DI import methods (unchanged) ---

	private void importDiagram(final App app, final NodeInterface defNode, final Element diagramEl,
							   final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap) throws FrameworkException {

		final NodeInterface diagramNode = app.create(StructrTraits.BPMN_DI_DIAGRAM, (String) null);
		final Traits diagramTraits = diagramNode.getTraits();

		diagramNode.setProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.DIAGRAM_ID_PROPERTY), diagramEl.getAttribute("id"));
		diagramNode.setProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.DEFINITION_PROPERTY), defNode);
		diagramNode.setProperty(diagramTraits.key("name"), diagramEl.getAttribute("id"));

		final NodeList planes = diagramEl.getElementsByTagNameNS(DI_NS, "BPMNPlane");
		if (planes.getLength() > 0) {
			final Element planeEl = (Element) planes.item(0);
			diagramNode.setProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.PLANE_ID_PROPERTY), planeEl.getAttribute("id"));
			diagramNode.setProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.PLANE_ELEMENT), planeEl.getAttribute("bpmnElement"));

			final NodeList shapes = planeEl.getElementsByTagNameNS(DI_NS, "BPMNShape");
			for (int i = 0; i < shapes.getLength(); i++) {
				importDiShape(app, diagramNode, (Element) shapes.item(i), elementMap);
			}

			final NodeList edges = planeEl.getElementsByTagNameNS(DI_NS, "BPMNEdge");
			for (int i = 0; i < edges.getLength(); i++) {
				importDiEdge(app, diagramNode, (Element) edges.item(i), flowMap);
			}
		}
	}

	private void importDiShape(final App app, final NodeInterface diagramNode, final Element shapeEl,
							   final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final NodeInterface shapeNode = app.create(StructrTraits.BPMN_DI_SHAPE, (String) null);
		final Traits traits = shapeNode.getTraits();

		shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.SHAPE_ID_PROPERTY), shapeEl.getAttribute("id"));
		shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY), shapeEl.getAttribute("bpmnElement"));
		shapeNode.setProperty(traits.key("name"), shapeEl.getAttribute("bpmnElement"));

		if ("true".equals(shapeEl.getAttribute("isMarkerVisible"))) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.IS_MARKER_VISIBLE), true);
		}
		if ("true".equals(shapeEl.getAttribute("isExpanded"))) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.IS_EXPANDED), true);
		}
		if ("true".equals(shapeEl.getAttribute("isHorizontal"))) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.IS_HORIZONTAL), true);
		}

		final NodeList boundsList = shapeEl.getElementsByTagNameNS(DC_NS, "Bounds");
		if (boundsList.getLength() > 0) {
			final Element b = (Element) boundsList.item(0);
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_X_PROPERTY), Double.parseDouble(b.getAttribute("x")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_Y_PROPERTY), Double.parseDouble(b.getAttribute("y")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_WIDTH_PROPERTY), Double.parseDouble(b.getAttribute("width")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_HEIGHT_PROPERTY), Double.parseDouble(b.getAttribute("height")));
		}

		final NodeList labels = shapeEl.getElementsByTagNameNS(DI_NS, "BPMNLabel");
		if (labels.getLength() > 0) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.HAS_LABEL_PROPERTY), true);
			final Element labelEl = (Element) labels.item(0);
			final NodeList labelBounds = labelEl.getElementsByTagNameNS(DC_NS, "Bounds");
			if (labelBounds.getLength() > 0) {
				final Element lb = (Element) labelBounds.item(0);
				final Map<String, String> lbMap = new LinkedHashMap<>();
				lbMap.put("x", lb.getAttribute("x"));
				lbMap.put("y", lb.getAttribute("y"));
				lbMap.put("width", lb.getAttribute("width"));
				lbMap.put("height", lb.getAttribute("height"));
				shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.LABEL_BOUNDS_PROPERTY), gson.toJson(lbMap));
			}
		}

		final Map<String, String> diAttrs = collectAttributes(shapeEl);
		diAttrs.remove("id");
		diAttrs.remove("bpmnElement");
		diAttrs.remove("isMarkerVisible");
		diAttrs.remove("isExpanded");
		diAttrs.remove("isHorizontal");
		if (!diAttrs.isEmpty()) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.DI_ATTRIBUTES_PROPERTY), gson.toJson(diAttrs));
		}

		shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.DIAGRAM_PROPERTY), diagramNode);

		final String ref = shapeEl.getAttribute("bpmnElement");
		if (ref != null && elementMap.containsKey(ref)) {
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.REFERENCES_ELEMENT), elementMap.get(ref));
		}
	}

	private void importDiEdge(final App app, final NodeInterface diagramNode, final Element edgeEl,
							  final Map<String, NodeInterface> flowMap) throws FrameworkException {

		final NodeInterface edgeNode = app.create(StructrTraits.BPMN_DI_EDGE, (String) null);
		final Traits traits = edgeNode.getTraits();

		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.EDGE_ID_PROPERTY), edgeEl.getAttribute("id"));
		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY), edgeEl.getAttribute("bpmnElement"));
		edgeNode.setProperty(traits.key("name"), edgeEl.getAttribute("bpmnElement"));

		final NodeList wpNodes = edgeEl.getElementsByTagNameNS(OMGDI_NS, "waypoint");
		final List<Map<String, String>> waypoints = new ArrayList<>();
		for (int i = 0; i < wpNodes.getLength(); i++) {
			final Element wp = (Element) wpNodes.item(i);
			final Map<String, String> m = new LinkedHashMap<>();
			m.put("x", wp.getAttribute("x"));
			m.put("y", wp.getAttribute("y"));
			waypoints.add(m);
		}
		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.WAYPOINTS_PROPERTY), gson.toJson(waypoints));

		final NodeList labels = edgeEl.getElementsByTagNameNS(DI_NS, "BPMNLabel");
		if (labels.getLength() > 0) {
			final Element labelEl = (Element) labels.item(0);
			final NodeList labelBounds = labelEl.getElementsByTagNameNS(DC_NS, "Bounds");
			if (labelBounds.getLength() > 0) {
				final Element lb = (Element) labelBounds.item(0);
				final Map<String, String> lbMap = new LinkedHashMap<>();
				lbMap.put("x", lb.getAttribute("x"));
				lbMap.put("y", lb.getAttribute("y"));
				lbMap.put("width", lb.getAttribute("width"));
				lbMap.put("height", lb.getAttribute("height"));
				edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.LABEL_BOUNDS_PROPERTY), gson.toJson(lbMap));
			}
		}

		final Map<String, String> diAttrs = collectAttributes(edgeEl);
		diAttrs.remove("id");
		diAttrs.remove("bpmnElement");
		if (!diAttrs.isEmpty()) {
			edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.DI_ATTRIBUTES_PROPERTY), gson.toJson(diAttrs));
		}

		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.DIAGRAM_PROPERTY), diagramNode);

		final String ref = edgeEl.getAttribute("bpmnElement");
		if (ref != null && flowMap.containsKey(ref)) {
			edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.REFERENCES_FLOW_PROPERTY), flowMap.get(ref));
		}
	}

	/**
	 * Import a top-level global definition element (message, signal, error, etc.).
	 */
	private void importGlobalDefinition(final App app, final NodeInterface defNode,
										final Element el, final String definitionType) throws FrameworkException {

		final NodeInterface gdNode = app.create(StructrTraits.BPMN_GLOBAL_DEFINITION, (String) null);
		final Traits traits = gdNode.getTraits();

		gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.DEFINITION_TYPE_PROPERTY), definitionType);

		final String name = el.getAttribute("name");
		if (name != null && !name.isEmpty()) {
			gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.BPMN_NAME_PROPERTY), name);
			gdNode.setProperty(traits.key("name"), name);
		} else {
			gdNode.setProperty(traits.key("name"), definitionType + " " + el.getAttribute("id"));
		}

		// Error-specific: errorCode
		final String errorCode = el.getAttribute("errorCode");
		if (errorCode != null && !errorCode.isEmpty()) {
			gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.ERROR_CODE_PROPERTY), errorCode);
		}

		// structureRef (used by message and error)
		final String structureRef = el.getAttribute("structureRef");
		if (structureRef != null && !structureRef.isEmpty()) {
			gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.STRUCTURE_REF_PROPERTY), structureRef);
		}

		// Connect to definition
		gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.DEFINITION_PROPERTY), defNode);
	}

	// --- Helper methods ---

	private Element getFirstChildByLocalName(final Element parent, final String localName) {
		final NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
				return (Element) child;
			}
		}
		return null;
	}

	private String getEventDefinitionRef(final Element evtDef) {
		for (final String attr : new String[]{"errorRef", "messageRef", "signalRef", "escalationRef", "linkName"}) {
			final String val = evtDef.getAttribute(attr);
			if (val != null && !val.isEmpty()) {
				return val;
			}
		}
		return null;
	}

	private Map<String, String> collectNamespaces(final Element root) {
		final Map<String, String> ns = new LinkedHashMap<>();
		final NamedNodeMap attrs = root.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			final Attr attr = (Attr) attrs.item(i);
			if ("xmlns".equals(attr.getPrefix()) || "xmlns".equals(attr.getName())) {
				ns.put(attr.getName(), attr.getValue());
			}
		}
		return ns;
	}

	private Map<String, String> collectAttributes(final Element el) {
		final Map<String, String> attrs = new LinkedHashMap<>();
		final NamedNodeMap nodeAttrs = el.getAttributes();
		for (int i = 0; i < nodeAttrs.getLength(); i++) {
			final Attr attr = (Attr) nodeAttrs.item(i);
			if (!"xmlns".equals(attr.getPrefix()) && !"xmlns".equals(attr.getName())) {
				attrs.put(attr.getName(), attr.getValue());
			}
		}
		return attrs;
	}

}
