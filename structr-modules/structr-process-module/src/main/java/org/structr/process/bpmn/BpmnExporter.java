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
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.process.traits.definitions.*;
import org.structr.process.ProcessTraits;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Iterator;
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

	private static final String BPMN_NS    = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS      = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS      = "http://www.omg.org/spec/DD/20100524/DC";
	private static final String OMGDI_NS   = "http://www.omg.org/spec/DD/20100524/DI";
	private static final String XSI_NS     = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String STRUCTR_NS = "http://structr.org/schema/process/1.0";

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
			ensureNamespace(namespaces, "xmlns:structr", STRUCTR_NS);

			// Register prefix -> namespace bindings on the writer BEFORE the root
			// element is written. writeNamespace() below emits the xmlns:* attributes
			// but does NOT register the prefix for subsequent writeStartElement(uri,
			// localName) lookups on the JDK default XMLStreamWriter; setPrefix /
			// setDefaultNamespace do. Without these calls, deep elements like
			// <structr:taskListener> fail with "NamespaceURI ... has not been bound
			// to any prefix" because the writer doesn't know the structr URI is
			// xmlns:structr (it only saw an attribute, not a binding).
			for (final Map.Entry<String, String> ns : namespaces.entrySet()) {
				final String key = ns.getKey();
				if (key.startsWith("xmlns:")) {
					w.setPrefix(key.substring(6), ns.getValue());
				} else if ("xmlns".equals(key)) {
					w.setDefaultNamespace(ns.getValue());
				}
			}

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

			writeAttrIfNotNull(w, "id", defNode.getProperty(defTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
			writeAttrIfNotNull(w, "targetNamespace", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY)));
			writeAttrIfNotNull(w, "exporter", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY)));
			writeAttrIfNotNull(w, "exporterVersion", defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY)));

			// Global definitions (message, signal, error, etc.)
			final Iterable<NodeInterface> globalDefs = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.GLOBAL_DEFINITIONS_PROPERTY));
			if (globalDefs != null) {
				final Traits gdTraits = Traits.of(ProcessTraits.BPMN_GLOBAL_DEFINITION);
				for (final NodeInterface gdNode : globalDefs) {
					w.writeCharacters("\n\n  ");
					exportGlobalDefinition(w, gdNode, gdTraits);
				}
			}

			// Optional <bpmn:collaboration>: emitted before processes per BPMN
			// spec ordering. Carries participants (which reference processes by
			// processRef) and message flows.
			final NodeInterface collaboration = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.COLLABORATION_PROPERTY));
			if (collaboration != null) {
				w.writeCharacters("\n\n  ");
				exportCollaboration(w, collaboration);
			}

			// One <bpmn:process> per BpmnProcess child of this definitions root.
			final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
			final Iterable<NodeInterface> processes = defNode.getProperty(defTraits.key(BpmnDefinitionsTraitDefinition.PROCESSES_PROPERTY));
			if (processes != null) {
				for (final NodeInterface procNode : processes) {
					w.writeCharacters("\n\n  ");
					exportProcess(w, sw, procNode, procTraits);
				}
			}

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

		final Iterable<NodeInterface> childElements   = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.CHILD_ELEMENTS_PROPERTY));
		final Iterable<NodeInterface> childFlows      = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.CHILD_FLOWS_PROPERTY));
		final Iterable<NodeInterface> performersIter  = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.PERFORMERS_PROPERTY));
		final Iterable<NodeInterface> listenersIter   = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.TASK_LISTENERS_PROPERTY));
		final Iterable<NodeInterface> methodsIter     = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.METHODS_PROPERTY));
		final boolean hasGraphChildren = hasAny(childElements) || hasAny(childFlows);
		final boolean hasPerformers    = hasAny(performersIter);
		final boolean hasListeners     = hasAny(listenersIter);
		final boolean hasMethods       = hasAny(methodsIter);
		final boolean hasContent = documentation != null || scriptContent != null || eventDefType != null || hasGraphChildren || hasPerformers || hasListeners || hasMethods;

		w.writeStartElement(BPMN_NS, elemType);
		writeAttrIfNotNull(w, "id", elemNode.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
		writeAttrIfNotNull(w, sw, "name", elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY)));

		// Boundary events: emit `attachedToRef` from the typed
		// relationship. The importer (resolveBoundaryAttachments) lifts
		// this attribute out of bpmnAttributes JSON into a typed
		// `attachedTo` rel so the graph is the single source of truth.
		//
		// Fall back to the JSON pass-through when the typed rel is
		// null: covers data imported before resolveBoundaryAttachments
		// existed, plus any other path that wrote a raw attribute
		// without going through the typed rel. A `attachedToRefEmitted`
		// flag is tracked so the JSON loop below skips `attachedToRef`
		// only when we already emitted it from the rel (avoids the
		// "attribute emitted twice" XMLStreamWriter error).
		boolean attachedToRefEmitted = false;
		if (BpmnElementType.BOUNDARY_EVENT.matches(elemType)) {
			final NodeInterface host = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.ATTACHED_TO_PROPERTY));
			if (host != null) {
				final String hostBpmnId = host.getProperty(host.getTraits().key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
				if (hostBpmnId != null && !hostBpmnId.isEmpty()) {
					w.writeAttribute("attachedToRef", hostBpmnId);
					attachedToRefEmitted = true;
				}
			}
		}

		// Restore additional attributes from JSON
		final String attrsJson = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
		if (attrsJson != null) {
			final Map<String, String> attrs = gson.fromJson(attrsJson, MAP_TYPE);
			for (final Map.Entry<String, String> a : attrs.entrySet()) {
				// Avoid emitting `attachedToRef` twice when both the typed
				// rel and the legacy JSON entry are populated (could
				// happen if a re-import wrote the rel but didn't run the
				// stripping pass).
				if ("attachedToRef".equals(a.getKey()) && attachedToRefEmitted) continue;
				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		// extensionElements: task listeners and method refs. BPMN spec places
		// it as one of the first child elements of an activity.
		if (hasListeners || hasMethods) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "extensionElements");
			if (hasListeners) {
				final Traits listenerTraits = Traits.of(ProcessTraits.BPMN_TASK_LISTENER);
				for (final NodeInterface listener : listenersIter) {
					w.writeCharacters("\n" + spaces(indent + 4));
					exportTaskListener(w, listener, listenerTraits);
				}
			}
			if (hasMethods) {
				for (final NodeInterface method : methodsIter) {
					w.writeCharacters("\n" + spaces(indent + 4));
					exportMethodRef(w, method);
				}
			}
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeEndElement();
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

		// Performers (humanPerformer / potentialOwner / performer) -- standard BPMN
		if (hasPerformers) {
			final Traits perfTraits = Traits.of(ProcessTraits.BPMN_PERFORMER);
			for (final NodeInterface performer : performersIter) {
				w.writeCharacters("\n" + spaces(indent + 2));
				exportPerformer(w, performer, perfTraits, indent + 2);
			}
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
			final Traits childElemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
			final Traits childFlowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);

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
	/**
	 * Export a single BpmnPerformer node as a standard BPMN sub-element. Always
	 * emits one of {@code <humanPerformer>}, {@code <potentialOwner>}, or
	 * {@code <performer>} (matching the {@code kind} property), wrapping the
	 * stored expression body in a {@code <resourceAssignmentExpression>} /
	 * {@code <formalExpression>}.
	 */
	private void exportPerformer(final XMLStreamWriter w, final NodeInterface performer,
								 final Traits perfTraits, final int indent) throws XMLStreamException {

		final String kind        = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY));
		final String authored    = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY));
		final String language    = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.EXPRESSION_LANGUAGE_PROPERTY));
		final String pName       = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.PERFORMER_NAME_PROPERTY));
		final String pId         = performer.getProperty(perfTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

		// Effective expression: use the authored string when present;
		// otherwise derive one from the typed Principal binding so the
		// performer round-trips through `user(name) / group(name)` syntax
		// (which the importer's Camunda-extension translation already
		// understands). The derived form is `user(<name>)` for User
		// nodes, `group(<name>)` for Group nodes; everything else is
		// dropped from the synthesis with a soft warning.
		String expression = authored;
		if (expression == null || expression.isEmpty()) {
			final Iterable<NodeInterface> linkedPrincipals = performer.getProperty(perfTraits.key(BpmnPerformerTraitDefinition.PRINCIPALS_PROPERTY));
			if (linkedPrincipals != null) {
				final java.util.List<String> tokens = new java.util.ArrayList<>();
				for (final NodeInterface principal : linkedPrincipals) {
					if (principal == null) continue;
					final String name = principal.getProperty(principal.getTraits().key("name"));
					if (name == null || name.isEmpty()) continue;
					if (principal.getTraits().contains(StructrTraits.GROUP)) {
						tokens.add("group(" + name + ")");
					} else {
						// Default to user(...) for User and any other Principal-shaped node.
						tokens.add("user(" + name + ")");
					}
				}
				if (!tokens.isEmpty()) expression = String.join(", ", tokens);
			}
		}

		final String localName = (kind != null) ? kind : BpmnPerformerTraitDefinition.KIND_PERFORMER;

		w.writeStartElement(BPMN_NS, localName);
		writeAttrIfNotNull(w, "id",   pId);
		writeAttrIfNotNull(w, "name", pName);

		if (expression != null && !expression.isEmpty()) {
			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "resourceAssignmentExpression");

			w.writeCharacters("\n" + spaces(indent + 4));
			w.writeStartElement(BPMN_NS, "formalExpression");
			if (language != null && !language.isEmpty()) {
				w.writeAttribute("language", language);
			}
			w.writeCharacters(expression);
			w.writeEndElement(); // formalExpression

			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeEndElement(); // resourceAssignmentExpression

			w.writeCharacters("\n" + spaces(indent));
		}

		w.writeEndElement(); // humanPerformer / potentialOwner / performer
	}

	/**
	 * Emit a single {@code <bpmn:process>} element with its content.
	 */
	private void exportProcess(final XMLStreamWriter w, final StringWriter sw, final NodeInterface procNode, final Traits procTraits) throws XMLStreamException {

		w.writeStartElement(BPMN_NS, "process");
		writeAttrIfNotNull(w, "id", procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY)));
		writeAttrIfNotNull(w, sw, "name", procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY)));

		final Boolean isExec = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_IS_EXECUTABLE_PROPERTY));
		if (Boolean.TRUE.equals(isExec)) {
			w.writeAttribute("isExecutable", "true");
		}

		// Process-level extensionElements: process listeners and method refs.
		// Emitted as the first child of <bpmn:process> per BPMN spec ordering.
		exportProcessExtensionElements(w, procNode, procTraits);

		// Optional <bpmn:laneSet>: emit before flow elements per spec ordering.
		exportLaneSet(w, procNode, procTraits);

		// Top-level elements
		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final Iterable<NodeInterface> elements = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY));
		if (elements != null) {
			for (final NodeInterface elemNode : elements) {
				w.writeCharacters("\n\n    ");
				exportElement(w, sw, elemNode, elemTraits, 4);
			}
		}

		// Top-level sequence flows
		final Traits flowTraits = Traits.of(ProcessTraits.BPMN_SEQUENCE_FLOW);
		final Iterable<NodeInterface> flows = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.SEQUENCE_FLOWS_PROPERTY));
		if (flows != null) {
			for (final NodeInterface flowNode : flows) {
				w.writeCharacters("\n\n    ");
				exportSequenceFlow(w, sw, flowNode, flowTraits, 4);
			}
		}

		w.writeCharacters("\n\n  ");
		w.writeEndElement(); // process
	}

	/**
	 * Emit {@code <bpmn:laneSet>} containing one {@code <bpmn:lane>} per
	 * BpmnLane attached to the process, with each lane's flowNodeRefs as
	 * {@code <bpmn:flowNodeRef>} children. No-op when the process has no
	 * lanes (single-lane / unbanded layouts).
	 */
	private void exportLaneSet(final XMLStreamWriter w, final NodeInterface procNode, final Traits procTraits) throws XMLStreamException {

		final Iterable<NodeInterface> lanes = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.LANES_PROPERTY));
		if (lanes == null) return;
		final Iterator<NodeInterface> it = lanes.iterator();
		if (!it.hasNext()) return;

		w.writeCharacters("\n\n    ");
		w.writeStartElement(BPMN_NS, "laneSet");
		// laneSet id is auto-generated; tools tolerate either presence/absence.
		w.writeAttribute("id", "LaneSet_" + procNode.getUuid().substring(0, 8));

		final Traits laneTraits  = Traits.of(ProcessTraits.BPMN_LANE);
		final Traits elemTraits  = Traits.of(ProcessTraits.BPMN_ELEMENT);

		// Re-iterate from scratch since we consumed `it` to test non-emptiness.
		for (final NodeInterface laneNode : procNode.<Iterable<NodeInterface>>getProperty(procTraits.key(BpmnProcessTraitDefinition.LANES_PROPERTY))) {
			w.writeCharacters("\n      ");
			w.writeStartElement(BPMN_NS, "lane");
			writeAttrIfNotNull(w, "id", laneNode.getProperty(laneTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
			writeAttrIfNotNull(w, "name", laneNode.getProperty(laneTraits.key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY)));

			final Iterable<NodeInterface> refs = laneNode.getProperty(laneTraits.key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY));
			if (refs != null) {
				for (final NodeInterface elemNode : refs) {
					final String elemBpmnId = elemNode.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
					if (elemBpmnId == null || elemBpmnId.isEmpty()) continue;
					w.writeCharacters("\n        ");
					w.writeStartElement(BPMN_NS, "flowNodeRef");
					w.writeCharacters(elemBpmnId);
					w.writeEndElement();
				}
				w.writeCharacters("\n      ");
			}
			w.writeEndElement(); // lane
		}
		w.writeCharacters("\n    ");
		w.writeEndElement(); // laneSet
	}

	/**
	 * Emit {@code <bpmn:collaboration>} with its participants and message flows.
	 */
	private void exportCollaboration(final XMLStreamWriter w, final NodeInterface collabNode) throws XMLStreamException {

		final Traits collabTraits = collabNode.getTraits();
		w.writeStartElement(BPMN_NS, "collaboration");
		writeAttrIfNotNull(w, "id", collabNode.getProperty(collabTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));

		// Participants -- each references a process by its bpmnId (processRef).
		final Iterable<NodeInterface> participants = collabNode.getProperty(collabTraits.key(BpmnCollaborationTraitDefinition.PARTICIPANTS_PROPERTY));
		if (participants != null) {
			final Traits partTraits = Traits.of(ProcessTraits.BPMN_PARTICIPANT);
			final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
			for (final NodeInterface part : participants) {
				w.writeCharacters("\n    ");
				w.writeEmptyElement(BPMN_NS, "participant");
				writeAttrIfNotNull(w, "id",   part.getProperty(partTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				writeAttrIfNotNull(w, "name", part.getProperty(partTraits.key(BpmnParticipantTraitDefinition.BPMN_NAME_PROPERTY)));
				final NodeInterface proc = part.getProperty(partTraits.key(BpmnParticipantTraitDefinition.PROCESS_PROPERTY));
				if (proc != null) {
					writeAttrIfNotNull(w, "processRef", proc.getProperty(procTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				}
			}
		}

		// Message flows -- cross-process arrows referencing source/target by bpmnId.
		final Iterable<NodeInterface> messageFlows = collabNode.getProperty(collabTraits.key(BpmnCollaborationTraitDefinition.MESSAGE_FLOWS_PROPERTY));
		if (messageFlows != null) {
			final Traits mfTraits = Traits.of(ProcessTraits.BPMN_MESSAGE_FLOW);
			for (final NodeInterface mf : messageFlows) {
				w.writeCharacters("\n    ");
				w.writeEmptyElement(BPMN_NS, "messageFlow");
				writeAttrIfNotNull(w, "id",        mf.getProperty(mfTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
				writeAttrIfNotNull(w, "name",      mf.getProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.BPMN_NAME_PROPERTY)));
				writeAttrIfNotNull(w, "sourceRef", mf.getProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.SOURCE_REF_ID_PROPERTY)));
				writeAttrIfNotNull(w, "targetRef", mf.getProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.TARGET_REF_ID_PROPERTY)));
			}
		}

		w.writeCharacters("\n  ");
		w.writeEndElement(); // collaboration
	}

	/**
	 * Emit process-level {@code <bpmn:extensionElements>} with process listeners
	 * and method refs, if any. Skipped when the process has neither.
	 */
	private void exportProcessExtensionElements(final XMLStreamWriter w, final NodeInterface procNode, final Traits procTraits) throws XMLStreamException {

		final Iterable<NodeInterface> listenersIter = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_LISTENERS_PROPERTY));
		final Iterable<NodeInterface> methodsIter   = procNode.getProperty(procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY));
		final boolean hasListeners = hasAny(listenersIter);
		final boolean hasMethods   = hasAny(methodsIter);
		if (!hasListeners && !hasMethods) {
			return;
		}

		w.writeCharacters("\n    ");
		w.writeStartElement(BPMN_NS, "extensionElements");

		if (hasListeners) {
			final Traits plTraits = Traits.of(ProcessTraits.BPMN_PROCESS_LISTENER);
			for (final NodeInterface listener : listenersIter) {
				w.writeCharacters("\n      ");
				exportProcessListener(w, listener, plTraits);
			}
		}
		if (hasMethods) {
			for (final NodeInterface method : methodsIter) {
				w.writeCharacters("\n      ");
				exportMethodRef(w, method);
			}
		}

		w.writeCharacters("\n    ");
		w.writeEndElement();
	}

	/**
	 * Emit a process-level lifecycle listener as
	 * {@code <structr:processListener>}. Mirrors {@link #exportTaskListener} for
	 * the per-element case.
	 */
	private void exportProcessListener(final XMLStreamWriter w, final NodeInterface listener,
									   final Traits plTraits) throws XMLStreamException {

		final String event   = listener.getProperty(plTraits.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY));
		final String phase   = listener.getProperty(plTraits.key(BpmnProcessListenerTraitDefinition.PHASE_PROPERTY));
		final String bpmnId  = listener.getProperty(plTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

		// Method is now a relationship to a SchemaMethod; emit its name.
		String methodName = null;
		final NodeInterface method = listener.getProperty(plTraits.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY));
		if (method != null) {
			methodName = method.getProperty(method.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
		}

		w.writeEmptyElement("structr", "processListener", STRUCTR_NS);
		writeAttrIfNotNull(w, "id",     bpmnId);
		writeAttrIfNotNull(w, "event",  event);
		writeAttrIfNotNull(w, "phase",  phase);
		writeAttrIfNotNull(w, "method", methodName);
	}

	/**
	 * Emit a HAS_METHOD attachment as {@code <structr:methodRef name="..."/>}.
	 * Reference is by SchemaMethod name; the body is not embedded. Cross-system
	 * migration of method bodies goes through Structr's deployment export/import.
	 */
	private void exportMethodRef(final XMLStreamWriter w, final NodeInterface method) throws XMLStreamException {

		final String name = method.getName();
		if (name == null || name.isEmpty()) {
			return;
		}
		w.writeEmptyElement("structr", "methodRef", STRUCTR_NS);
		w.writeAttribute("name", name);
	}

	/**
	 * Emit a single task listener as {@code <structr:taskListener>}. Always
	 * uses the canonical structr namespace -- never re-emits Camunda forms,
	 * even if the source BPMN had them (interop is import-only).
	 */
	private void exportTaskListener(final XMLStreamWriter w, final NodeInterface listener,
									final Traits listenerTraits) throws XMLStreamException {

		final String event   = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY));
		final String phase   = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.PHASE_PROPERTY));
		final String bpmnId  = listener.getProperty(listenerTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));

		// Method is now a relationship to a SchemaMethod; emit its name.
		String methodName = null;
		final NodeInterface method = listener.getProperty(listenerTraits.key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY));
		if (method != null) {
			methodName = method.getProperty(method.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
		}

		// Pass the prefix explicitly. The 2-arg writeEmptyElement(uri, localName)
		// looks up the prefix in the writer's namespace context, which is unreliable
		// across JDK XMLStreamWriter implementations. The 3-arg form is unambiguous.
		w.writeEmptyElement("structr", "taskListener", STRUCTR_NS);
		writeAttrIfNotNull(w, "id",     bpmnId);
		writeAttrIfNotNull(w, "event",  event);
		writeAttrIfNotNull(w, "phase",  phase);
		writeAttrIfNotNull(w, "method", methodName);
	}

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
		writeAttrIfNotNull(w, "id", gdNode.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
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

		writeAttrIfNotNull(w, "id", flowNode.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY)));
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
		final Traits shapeTraits   = Traits.of(ProcessTraits.BPMN_DI_SHAPE);
		final Traits edgeTraits    = Traits.of(ProcessTraits.BPMN_DI_EDGE);

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
