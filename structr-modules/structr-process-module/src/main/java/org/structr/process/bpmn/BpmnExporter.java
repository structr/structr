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
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.traits.definitions.*;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structr-native BPMN 2.0.2 XML exporter. Reads the process definition graph
 * and emits valid BPMN XML including the DI layer.
 *
 * All content is reconstructed from typed graph properties. There is no raw XML
 * pass-through -- the graph is the single source of truth.
 */
public class BpmnExporter {

	private static final Logger logger = LoggerFactory.getLogger(BpmnExporter.class);

	private static final String BPMN_NS  = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS    = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS    = "http://www.omg.org/spec/DD/20100524/DC";
	private static final String OMGDI_NS = "http://www.omg.org/spec/DD/20100524/DI";
	private static final String XSI_NS   = "http://www.w3.org/2001/XMLSchema-instance";

	private static final Type MAP_TYPE  = new TypeToken<Map<String, String>>() {}.getType();
	private static final Type LIST_TYPE = new TypeToken<List<Map<String, String>>>() {}.getType();

	private final Gson gson;

	public BpmnExporter() {
		this.gson = new GsonBuilder().create();
	}

	/**
	 * Export a BpmnDefinitions node to BPMN 2.0.2 XML string.
	 */
	public String exportBpmn(final NodeInterface defNode) throws FrameworkException {

		try {

			final Traits defTraits = defNode.getTraits();
			final StringWriter sw = new StringWriter();
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			final XMLStreamWriter w = xof.createXMLStreamWriter(sw);

			w.writeStartDocument("UTF-8", "1.0");
			w.writeCharacters("\n");

			// Reconstruct namespace declarations
			final String nsJson = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS));
			final Map<String, String> namespaces = nsJson != null ? gson.fromJson(nsJson, MAP_TYPE) : new LinkedHashMap<>();

			ensureNamespace(namespaces, "xmlns:bpmn", BPMN_NS);
			ensureNamespace(namespaces, "xmlns:bpmndi", DI_NS);
			ensureNamespace(namespaces, "xmlns:dc", DC_NS);
			ensureNamespace(namespaces, "xmlns:di", OMGDI_NS);

			final String bpmnPrefix = findPrefixForNamespace(namespaces, BPMN_NS);

			// bpmn:definitions
			w.writeStartElement(bpmnPrefix, "definitions", BPMN_NS);
			for (final Map.Entry<String, String> ns : namespaces.entrySet()) {
				final String key = ns.getKey();
				if (key.startsWith("xmlns:")) {
					w.writeNamespace(key.substring(6), ns.getValue());
				} else if ("xmlns".equals(key)) {
					w.writeDefaultNamespace(ns.getValue());
				}
			}

			writeAttrIfNotNull(w, "id", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.BPMN_ID_PROPERTY)));
			writeAttrIfNotNull(w, "targetNamespace", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY)));
			writeAttrIfNotNull(w, "exporter", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY)));
			writeAttrIfNotNull(w, "exporterVersion", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY)));

			// Global definitions (message, signal, error, etc.)
			final Iterable<NodeInterface> globalDefs = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.GLOBAL_DEFINITIONS_PROPERTY));
			if (globalDefs != null) {
				final Traits gdTraits = Traits.of(StructrTraits.BPMN_GLOBAL_DEFINITION);
				for (final NodeInterface gdNode : globalDefs) {
					w.writeCharacters("\n\n  ");
					exportGlobalDefinition(w, gdNode, gdTraits);
				}
			}

			w.writeCharacters("\n\n  ");

			// bpmn:process
			w.writeStartElement(BPMN_NS, "process");
			writeAttrIfNotNull(w, "id", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_ID_PROPERTY)));
			writeAttrIfNotNull(w, sw, "name", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_NAME_PROPERTY)));

			final Boolean isExec = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESS_IS_EXECUTABLE));
			if (Boolean.TRUE.equals(isExec)) {
				w.writeAttribute("isExecutable", "true");
			}

			// Top-level elements
			final Traits elemTraits = Traits.of(StructrTraits.BPMN_ELEMENT);
			final Iterable<NodeInterface> elements = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.ELEMENTS_PROPERTY));
			if (elements != null) {
				for (final NodeInterface elemNode : elements) {
					w.writeCharacters("\n\n    ");
					exportElement(w, sw, elemNode, elemTraits, 4);
				}
			}

			// Top-level sequence flows
			final Traits flowTraits = Traits.of(StructrTraits.BPMN_SEQUENCE_FLOW);
			final Iterable<NodeInterface> flows = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
			if (flows != null) {
				for (final NodeInterface flowNode : flows) {
					w.writeCharacters("\n\n    ");
					exportSequenceFlow(w, sw, flowNode, flowTraits, 4);
				}
			}

			w.writeCharacters("\n\n  ");
			w.writeEndElement(); // process

			// DI diagrams
			final Iterable<NodeInterface> diagrams = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.DIAGRAMS_PROPERTY));
			if (diagrams != null) {
				for (final NodeInterface diagramNode : diagrams) {
					w.writeCharacters("\n\n  ");
					exportDiagram(w, diagramNode);
				}
			}

			w.writeCharacters("\n\n");
			w.writeEndElement(); // definitions
			w.writeCharacters("\n");
			w.writeEndDocument();
			w.flush();

			return sw.toString();

		} catch (XMLStreamException ex) {
			logger.error("Error exporting BPMN XML", ex);
			throw new FrameworkException(500, "Error exporting BPMN XML: " + ex.getMessage());
		}
	}

	/**
	 * Export a single BPMN element with all content reconstructed from typed properties.
	 */
	private void exportElement(final XMLStreamWriter w, final StringWriter sw, final NodeInterface elemNode,
							   final Traits traits, final int indent) throws XMLStreamException {

		final String elemType = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));

		// Collect content flags
		final String documentation = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.DOCUMENTATION_PROPERTY));
		final String scriptContent = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY));
		final String eventDefType  = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY));

		final Iterable<NodeInterface> childElements = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY));
		final Iterable<NodeInterface> childFlows    = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.CHILD_FLOWS_PROPERTY));
		final boolean hasGraphChildren = hasAny(childElements) || hasAny(childFlows);
		final boolean hasContent = documentation != null || scriptContent != null || eventDefType != null || hasGraphChildren;

		w.writeStartElement(BPMN_NS, elemType);
		writeAttrIfNotNull(w, "id", elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ID_PROPERTY)));
		writeAttrIfNotNull(w, sw, "name", elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY)));

		// Restore additional attributes from JSON
		final String attrsJson = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
		if (attrsJson != null) {
			final Map<String, String> attrs = gson.fromJson(attrsJson, MAP_TYPE);
			for (final Map.Entry<String, String> a : attrs.entrySet()) {
				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		// Documentation
		if (documentation != null) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "documentation");
			w.writeCharacters(documentation);
			w.writeEndElement();
		}

		// Event definition
		if (eventDefType != null) {
			w.writeCharacters("\n" + spaces(indent + 2));
			exportEventDefinition(w, elemNode, traits, eventDefType, indent + 2);
		}

		// Script content (for scriptTask)
		if (scriptContent != null) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "script");
			w.writeCharacters(scriptContent);
			w.writeEndElement();
		}

		// Recursively write graph children (sub-process containment hierarchy)
		if (hasGraphChildren) {
			final Traits childElemTraits = Traits.of(StructrTraits.BPMN_ELEMENT);
			final Traits childFlowTraits = Traits.of(StructrTraits.BPMN_SEQUENCE_FLOW);

			if (childElements != null) {
				for (final NodeInterface childElem : childElements) {
					w.writeCharacters("\n\n" + spaces(indent + 2));
					exportElement(w, sw, childElem, childElemTraits, indent + 2);
				}
			}

			if (childFlows != null) {
				for (final NodeInterface childFlow : childFlows) {
					w.writeCharacters("\n\n" + spaces(indent + 2));
					exportSequenceFlow(w, sw, childFlow, childFlowTraits, indent + 2);
				}
			}
		}

		if (hasContent) {
			w.writeCharacters("\n" + spaces(indent));
		}

		w.writeEndElement();
	}

	/**
	 * Export an event definition element from typed properties.
	 */
	private void exportEventDefinition(final XMLStreamWriter w, final NodeInterface elemNode,
									   final Traits traits, final String eventDefType, final int indent) throws XMLStreamException {

		final String eventDefId  = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_ID_PROPERTY));
		final String eventDefRef = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_REF_PROPERTY));
		final String timerType   = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_TYPE_PROPERTY));
		final String timerValue  = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_VALUE_PROPERTY));

		final boolean hasTimerChild = (timerType != null && timerValue != null);

		if (!hasTimerChild && eventDefRef == null) {
			// Simple empty element (e.g. <terminateEventDefinition id="..." />)
			w.writeEmptyElement(BPMN_NS, eventDefType);
		} else {
			w.writeStartElement(BPMN_NS, eventDefType);
		}

		writeAttrIfNotNull(w, "id", eventDefId);

		// Write ref attribute based on type
		if (eventDefRef != null) {
			if ("errorEventDefinition".equals(eventDefType)) {
				w.writeAttribute("errorRef", eventDefRef);
			} else if ("messageEventDefinition".equals(eventDefType)) {
				w.writeAttribute("messageRef", eventDefRef);
			} else if ("signalEventDefinition".equals(eventDefType)) {
				w.writeAttribute("signalRef", eventDefRef);
			} else if ("escalationEventDefinition".equals(eventDefType)) {
				w.writeAttribute("escalationRef", eventDefRef);
			}
		}

		// Timer child element
		if (hasTimerChild) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, timerType);

			final String timerExprType = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.TIMER_EXPRESSION_TYPE_PROPERTY));
			if (timerExprType != null) {
				w.writeAttribute(XSI_NS, "type", timerExprType);
			}

			w.writeCharacters(timerValue);
			w.writeEndElement();
			w.writeCharacters("\n" + spaces(indent));
		}

		if (hasTimerChild || eventDefRef != null) {
			w.writeEndElement();
		}
	}

	/**
	 * Export a global definition element (message, signal, error, etc.).
	 */
	private void exportGlobalDefinition(final XMLStreamWriter w, final NodeInterface gdNode, final Traits traits) throws XMLStreamException {

		final String defType  = gdNode.getProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.DEFINITION_TYPE_PROPERTY));
		final String errorCode    = gdNode.getProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.ERROR_CODE_PROPERTY));
		final String structureRef = gdNode.getProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.STRUCTURE_REF_PROPERTY));

		w.writeEmptyElement(BPMN_NS, defType);
		writeAttrIfNotNull(w, "id", gdNode.getProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.BPMN_ID_PROPERTY)));
		writeAttrIfNotNull(w, "name", gdNode.getProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.BPMN_NAME_PROPERTY)));

		if (errorCode != null && !errorCode.isEmpty()) {
			w.writeAttribute("errorCode", errorCode);
		}
		if (structureRef != null && !structureRef.isEmpty()) {
			w.writeAttribute("structureRef", structureRef);
		}
	}

	private void exportSequenceFlow(final XMLStreamWriter w, final StringWriter sw, final NodeInterface flowNode,
									final Traits traits, final int indent) throws XMLStreamException {

		final String condExpr = flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_PROPERTY));
		final boolean hasCondition = (condExpr != null && !condExpr.isEmpty());

		if (!hasCondition) {
			w.writeEmptyElement(BPMN_NS, "sequenceFlow");
		} else {
			w.writeStartElement(BPMN_NS, "sequenceFlow");
		}

		writeAttrIfNotNull(w, "id", flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_ID_PROPERTY)));
		writeAttrIfNotNull(w, sw, "name", flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_NAME_PROPERTY)));
		writeAttrIfNotNull(w, "sourceRef", flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.SOURCE_REF_ID_PROPERTY)));
		writeAttrIfNotNull(w, "targetRef", flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY)));

		// Restore additional attributes
		final String attrsJson = flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
		if (attrsJson != null) {
			final Map<String, String> attrs = gson.fromJson(attrsJson, MAP_TYPE);
			for (final Map.Entry<String, String> a : attrs.entrySet()) {
				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		if (hasCondition) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "conditionExpression");

			final String condType = flowNode.getProperty(traits.key(BpmnSequenceFlowTraitDefinition.CONDITION_EXPRESSION_TYPE_PROPERTY));
			if (condType != null && !condType.isEmpty()) {
				w.writeAttribute(XSI_NS, "type", condType);
			}

			w.writeCharacters(condExpr);
			w.writeEndElement();
			w.writeCharacters("\n" + spaces(indent));
			w.writeEndElement();
		}
	}

	// --- DI export methods ---

	private void exportDiagram(final XMLStreamWriter w, final NodeInterface diagramNode) throws XMLStreamException {

		final Traits diagramTraits = diagramNode.getTraits();
		final Traits shapeTraits   = Traits.of(StructrTraits.BPMN_DI_SHAPE);
		final Traits edgeTraits    = Traits.of(StructrTraits.BPMN_DI_EDGE);

		w.writeStartElement(DI_NS, "BPMNDiagram");
		writeAttrIfNotNull(w, "id", diagramNode.getProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.DIAGRAM_ID_PROPERTY)));

		w.writeCharacters("\n    ");
		w.writeStartElement(DI_NS, "BPMNPlane");
		writeAttrIfNotNull(w, "id", diagramNode.getProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.PLANE_ID_PROPERTY)));
		writeAttrIfNotNull(w, "bpmnElement", diagramNode.getProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.PLANE_ELEMENT)));

		final Iterable<NodeInterface> shapes = diagramNode.getProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.SHAPES_PROPERTY));
		if (shapes != null) {
			for (final NodeInterface shapeNode : shapes) {
				w.writeCharacters("\n\n      ");
				exportDiShape(w, shapeNode, shapeTraits);
			}
		}

		final Iterable<NodeInterface> edges = diagramNode.getProperty(diagramTraits.key(BpmnDiDiagramTraitDefinition.EDGES_PROPERTY));
		if (edges != null) {
			for (final NodeInterface edgeNode : edges) {
				w.writeCharacters("\n\n      ");
				exportDiEdge(w, edgeNode, edgeTraits);
			}
		}

		w.writeCharacters("\n\n    ");
		w.writeEndElement(); // BPMNPlane
		w.writeCharacters("\n  ");
		w.writeEndElement(); // BPMNDiagram
	}

	private void exportDiShape(final XMLStreamWriter w, final NodeInterface shapeNode, final Traits traits) throws XMLStreamException {

		w.writeStartElement(DI_NS, "BPMNShape");
		writeAttrIfNotNull(w, "id", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.SHAPE_ID_PROPERTY)));
		writeAttrIfNotNull(w, "bpmnElement", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY)));

		if (Boolean.TRUE.equals(shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_MARKER_VISIBLE)))) {
			w.writeAttribute("isMarkerVisible", "true");
		}
		if (Boolean.TRUE.equals(shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_EXPANDED)))) {
			w.writeAttribute("isExpanded", "true");
		}
		if (Boolean.TRUE.equals(shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.IS_HORIZONTAL)))) {
			w.writeAttribute("isHorizontal", "true");
		}

		final String diAttrsJson = shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.DI_ATTRIBUTES_PROPERTY));
		if (diAttrsJson != null) {
			final Map<String, String> diAttrs = gson.fromJson(diAttrsJson, MAP_TYPE);
			for (final Map.Entry<String, String> a : diAttrs.entrySet()) {
				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		w.writeCharacters("\n        ");
		w.writeEmptyElement(DC_NS, "Bounds");
		writeAttrDouble(w, "x", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_X_PROPERTY)));
		writeAttrDouble(w, "y", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_Y_PROPERTY)));
		writeAttrDouble(w, "width", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_WIDTH_PROPERTY)));
		writeAttrDouble(w, "height", shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_HEIGHT_PROPERTY)));

		final String labelJson = shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.LABEL_BOUNDS_PROPERTY));
		final Boolean hasLabel = shapeNode.getProperty(traits.key(BpmnDiShapeTraitDefinition.HAS_LABEL_PROPERTY));
		if (labelJson != null) {
			final Map<String, String> lb = gson.fromJson(labelJson, MAP_TYPE);
			w.writeCharacters("\n        ");
			w.writeStartElement(DI_NS, "BPMNLabel");
			w.writeCharacters("\n          ");
			w.writeEmptyElement(DC_NS, "Bounds");
			w.writeAttribute("x", lb.get("x"));
			w.writeAttribute("y", lb.get("y"));
			w.writeAttribute("width", lb.get("width"));
			w.writeAttribute("height", lb.get("height"));
			w.writeCharacters("\n        ");
			w.writeEndElement();
		} else if (Boolean.TRUE.equals(hasLabel)) {
			w.writeCharacters("\n        ");
			w.writeEmptyElement(DI_NS, "BPMNLabel");
		}

		w.writeCharacters("\n      ");
		w.writeEndElement();
	}

	private void exportDiEdge(final XMLStreamWriter w, final NodeInterface edgeNode, final Traits traits) throws XMLStreamException {

		w.writeStartElement(DI_NS, "BPMNEdge");
		writeAttrIfNotNull(w, "id", edgeNode.getProperty(traits.key(BpmnDiEdgeTraitDefinition.EDGE_ID_PROPERTY)));
		writeAttrIfNotNull(w, "bpmnElement", edgeNode.getProperty(traits.key(BpmnDiEdgeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY)));

		final String diAttrsJson = edgeNode.getProperty(traits.key(BpmnDiEdgeTraitDefinition.DI_ATTRIBUTES_PROPERTY));
		if (diAttrsJson != null) {
			final Map<String, String> diAttrs = gson.fromJson(diAttrsJson, MAP_TYPE);
			for (final Map.Entry<String, String> a : diAttrs.entrySet()) {
				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		final String wpJson = edgeNode.getProperty(traits.key(BpmnDiEdgeTraitDefinition.WAYPOINTS_PROPERTY));
		if (wpJson != null) {
			final List<Map<String, String>> waypoints = gson.fromJson(wpJson, LIST_TYPE);
			for (final Map<String, String> wp : waypoints) {
				w.writeCharacters("\n        ");
				w.writeEmptyElement(OMGDI_NS, "waypoint");
				w.writeAttribute("x", wp.get("x"));
				w.writeAttribute("y", wp.get("y"));
			}
		}

		final String labelJson = edgeNode.getProperty(traits.key(BpmnDiEdgeTraitDefinition.LABEL_BOUNDS_PROPERTY));
		if (labelJson != null) {
			final Map<String, String> lb = gson.fromJson(labelJson, MAP_TYPE);
			w.writeCharacters("\n        ");
			w.writeStartElement(DI_NS, "BPMNLabel");
			w.writeCharacters("\n          ");
			w.writeEmptyElement(DC_NS, "Bounds");
			w.writeAttribute("x", lb.get("x"));
			w.writeAttribute("y", lb.get("y"));
			w.writeAttribute("width", lb.get("width"));
			w.writeAttribute("height", lb.get("height"));
			w.writeCharacters("\n        ");
			w.writeEndElement();
		}

		w.writeCharacters("\n      ");
		w.writeEndElement();
	}

	// --- Helper methods ---

	private void writeAttrIfNotNull(final XMLStreamWriter w, final StringWriter sw, final String name, final Object value) throws XMLStreamException {
		if (value != null) {
			final String str = value.toString();
			if (str.indexOf('\n') >= 0) {
				w.flush();
				sw.write(" " + name + "=\"" + xmlEscapeAttr(str) + "\"");
			} else {
				w.writeAttribute(name, str);
			}
		}
	}

	private void writeAttrIfNotNull(final XMLStreamWriter w, final String name, final Object value) throws XMLStreamException {
		if (value != null) {
			w.writeAttribute(name, value.toString());
		}
	}

	private String xmlEscapeAttr(final String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("\n", "&#10;").replace("\r", "&#13;");
	}

	private void writeAttrDouble(final XMLStreamWriter w, final String name, final Double value) throws XMLStreamException {
		if (value != null) {
			if (value == Math.floor(value) && !Double.isInfinite(value)) {
				w.writeAttribute(name, String.valueOf((int) Math.floor(value)));
			} else {
				w.writeAttribute(name, String.valueOf(value));
			}
		}
	}

	private void ensureNamespace(final Map<String, String> ns, final String prefix, final String uri) {
		if (!ns.containsKey(prefix)) {
			for (final Map.Entry<String, String> entry : ns.entrySet()) {
				if (uri.equals(entry.getValue())) {
					return;
				}
			}
			ns.put(prefix, uri);
		}
	}

	private String findPrefixForNamespace(final Map<String, String> namespaces, final String uri) {
		for (final Map.Entry<String, String> entry : namespaces.entrySet()) {
			if (uri.equals(entry.getValue()) && entry.getKey().startsWith("xmlns:")) {
				return entry.getKey().substring(6);
			}
		}
		return "bpmn";
	}

	private boolean hasAny(final Iterable<?> iterable) {
		if (iterable == null) {
			return false;
		}
		return iterable.iterator().hasNext();
	}

	private String spaces(final int count) {
		return " ".repeat(count);
	}
}
