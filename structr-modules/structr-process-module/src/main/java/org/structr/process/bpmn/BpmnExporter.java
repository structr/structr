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
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.entity.BpmnCollaboration;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.entity.BpmnDiDiagram;
import org.structr.process.entity.BpmnDiEdge;
import org.structr.process.entity.BpmnDiShape;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnGlobalDefinition;
import org.structr.process.entity.BpmnLane;
import org.structr.process.entity.BpmnMessageFlow;
import org.structr.process.entity.BpmnParticipant;
import org.structr.process.entity.BpmnPerformer;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.entity.BpmnProcessListener;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.entity.BpmnTaskListener;
import org.structr.process.traits.definitions.BpmnPerformerTraitDefinition;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
	public String exportBpmn(final BpmnDefinitions defNode) throws FrameworkException {

		try {

			final StringWriter sw = new StringWriter();
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			final XMLStreamWriter w = xof.createXMLStreamWriter(sw);

			w.writeStartDocument("UTF-8", "1.0");
			w.writeCharacters("\n");

			// Reconstruct namespace declarations
			final String nsJson = defNode.getNamespaceDeclarations();
			final Map<String, String> namespaces = safeMap(nsJson);

			ensureNamespace(namespaces, "xmlns:bpmn", BPMN_NS);
			ensureNamespace(namespaces, "xmlns:bpmndi", DI_NS);
			ensureNamespace(namespaces, "xmlns:dc", DC_NS);
			ensureNamespace(namespaces, "xmlns:di", OMGDI_NS);
			ensureNamespace(namespaces, "xmlns:structr", STRUCTR_NS);
			// xsi is required for the xsi:type attributes emitted on conditionExpression
			// and timer event definitions; a natively-authored graph may not declare it.
			ensureNamespace(namespaces, "xmlns:xsi", XSI_NS);

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

			writeAttrIfNotNull(w, "id", defNode.getBpmnId());
			writeAttrIfNotNull(w, "targetNamespace", defNode.getTargetNamespace());
			writeAttrIfNotNull(w, "exporter", defNode.getExporter());
			writeAttrIfNotNull(w, "exporterVersion", defNode.getExporterVersion());

			// Global definitions (message, signal, error, etc.)
			final Iterable<BpmnGlobalDefinition> globalDefs = defNode.getGlobalDefinitions();
			if (globalDefs != null) {

				for (final BpmnGlobalDefinition gdNode : globalDefs) {

					w.writeCharacters("\n\n  ");
					exportGlobalDefinition(w, gdNode);
				}
			}

			// Optional <bpmn:collaboration>: emitted before processes per BPMN
			// spec ordering. Carries participants (which reference processes by
			// processRef) and message flows.
			final BpmnCollaboration collaboration = defNode.getCollaboration();
			if (collaboration != null) {

				w.writeCharacters("\n\n  ");
				exportCollaboration(w, collaboration);
			}

			// One <bpmn:process> per BpmnProcess child of this definitions root.
			final Iterable<BpmnProcess> processes = defNode.getProcesses();
			if (processes != null) {

				for (final BpmnProcess procNode : processes) {

					w.writeCharacters("\n\n  ");
					exportProcess(w, sw, procNode);
				}
			}

			// DI diagrams
			final Iterable<BpmnDiDiagram> diagrams = defNode.getDiagrams();
			if (diagrams != null) {

				for (final BpmnDiDiagram diagramNode : diagrams) {

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
	private void exportElement(final XMLStreamWriter w, final StringWriter sw, final BpmnElement elem, final int indent) throws XMLStreamException {

		final String elemType = elem.getElementTypeName();

		// Collect content flags
		final String documentation = elem.getDocumentation();
		final String scriptContent = elem.getScriptContent();
		final String eventDefType  = elem.getEventDefinitionType();
		final Iterable<BpmnElement> childElements       = elem.getChildElements();
		final Iterable<BpmnSequenceFlow> childFlows     = elem.getChildFlows();
		final Iterable<BpmnPerformer> performersIter    = elem.getPerformers();
		final Iterable<BpmnTaskListener> listenersIter  = elem.getTaskListeners();
		final Iterable<NodeInterface> methodsIter       = elem.getMethods();
		final boolean hasGraphChildren                  = hasAny(childElements) || hasAny(childFlows);
		final boolean hasPerformers                     = hasAny(performersIter);
		final boolean hasListeners                      = hasAny(listenersIter);
		final boolean hasMethods                        = hasAny(methodsIter);

		// Structr-native process/UI contract (round-trips via <structr:subjectContract>). The
		// subject *type* is process-level (emitted by exportProcessExtensionElements); a step only
		// carries which views it shows / may write, plus its instructions.
		final Traits elemTraits                         = elem.getTraits();
		final String subjectFormView                    = elem.getProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY));
		final String subjectWritableView                = elem.getProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_WRITABLE_VIEW_PROPERTY));
		final String subjectInstructions                = elem.getProperty(elemTraits.key(BpmnElementTraitDefinition.INSTRUCTIONS_PROPERTY));
		final boolean hasContract                       = subjectFormView != null || subjectWritableView != null || subjectInstructions != null;
		final boolean hasContent                        = documentation != null || scriptContent != null || eventDefType != null || hasGraphChildren || hasPerformers || hasListeners || hasMethods || hasContract;

		w.writeStartElement(BPMN_NS, elemType);

		writeAttrIfNotNull(w, "id", elem.getBpmnId());
		writeAttrIfNotNull(w, sw, "name", elem.getBpmnName());

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

		if (elem.isType(BpmnElementType.BOUNDARY_EVENT)) {

			final BpmnElement host = elem.getAttachedToElement();
			if (host != null) {

				final String hostBpmnId = host.getBpmnId();
				if (hostBpmnId != null && !hostBpmnId.isEmpty()) {

					w.writeAttribute("attachedToRef", hostBpmnId);
					attachedToRefEmitted = true;
				}
			}
		}

		// Restore additional attributes from JSON
		final String attrsJson = elem.getBpmnAttributes();
		if (attrsJson != null) {

			final Map<String, String> attrs = safeMap(attrsJson);

			for (final Map.Entry<String, String> a : attrs.entrySet()) {

				// Avoid emitting `attachedToRef` twice when both the typed
				// rel and the legacy JSON entry are populated (could
				// happen if a re-import wrote the rel but didn't run the
				// stripping pass).
				if ("attachedToRef".equals(a.getKey()) && attachedToRefEmitted) {

					continue;
				}

				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		// Documentation. Per the BPMN tBaseElement content model, <documentation>
		// (0..n) must be emitted BEFORE <extensionElements> (0..1).
		if (documentation != null) {

			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "documentation");
			w.writeCharacters(documentation);
			w.writeEndElement();
		}

		// extensionElements: task listeners, method refs and the structr: subject contract.
		if (hasListeners || hasMethods || hasContract) {

			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "extensionElements");

			if (hasListeners) {

				for (final BpmnTaskListener listener : listenersIter) {

					w.writeCharacters("\n" + spaces(indent + 4));
					exportTaskListener(w, listener);
				}
			}

			if (hasMethods) {

				for (final NodeInterface method : methodsIter) {

					w.writeCharacters("\n" + spaces(indent + 4));
					exportMethodRef(w, method);
				}
			}

			if (hasContract) {

				w.writeCharacters("\n" + spaces(indent + 4));
				w.writeEmptyElement("structr", "subjectContract", STRUCTR_NS);
				writeAttrIfNotNull(w, "formView",     subjectFormView);
				writeAttrIfNotNull(w, "writableView", subjectWritableView);
				writeAttrIfNotNull(w, "instructions", subjectInstructions);
			}

			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeEndElement();
		}

		// Event definition
		if (eventDefType != null) {

			w.writeCharacters("\n" + spaces(indent + 2));
			exportEventDefinition(w, elem, eventDefType, indent + 2);
		}

		// Performers (humanPerformer / potentialOwner / performer) -- standard BPMN
		if (hasPerformers) {

			for (final BpmnPerformer performer : performersIter) {

				w.writeCharacters("\n" + spaces(indent + 2));
				exportPerformer(w, performer, indent + 2);
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

			if (childElements != null) {

				for (final BpmnElement childElem : childElements) {

					w.writeCharacters("\n\n" + spaces(indent + 2));
					exportElement(w, sw, childElem, indent + 2);
				}
			}

			if (childFlows != null) {

				for (final BpmnSequenceFlow childFlow : childFlows) {

					w.writeCharacters("\n\n" + spaces(indent + 2));
					exportSequenceFlow(w, sw, childFlow, indent + 2);
				}
			}
		}

		if (hasContent) {

			w.writeCharacters("\n" + spaces(indent));
		}

		w.writeEndElement();
	}

	/**
	 * Export a single BpmnPerformer node as a standard BPMN sub-element. Always
	 * emits one of {@code <humanPerformer>}, {@code <potentialOwner>}, or
	 * {@code <performer>} (matching the {@code kind} property), wrapping the
	 * stored expression body in a {@code <resourceAssignmentExpression>} /
	 * {@code <formalExpression>}.
	 */
	private void exportPerformer(final XMLStreamWriter w, final BpmnPerformer performer, final int indent) throws XMLStreamException {

		final String kind     = performer.getKind();
		final String authored = performer.getExpression();
		final String language = performer.getExpressionLanguage();
		final String pName    = performer.getPerformerName();
		final String pId      = performer.getBpmnId();

		// Effective expression: use the authored string when present;
		// otherwise derive one from the typed Principal binding so the
		// performer round-trips through `user(name) / group(name)` syntax
		// (which the importer's Camunda-extension translation already
		// understands). The derived form is `user(<name>)` for User
		// nodes, `group(<name>)` for Group nodes; everything else is
		// dropped from the synthesis with a soft warning.
		String expression = authored;
		if (expression == null || expression.isEmpty()) {

			final Iterable<NodeInterface> linkedPrincipals = performer.getPrincipals();
			if (linkedPrincipals != null) {

				final List<String> tokens = new LinkedList<>();

				for (final NodeInterface principal : linkedPrincipals) {

					if (principal == null) {

						continue;
					}

					final String name = principal.getName();
					if (name == null || name.isEmpty()) {

						continue;
					}

					if (principal.getTraits().contains(StructrTraits.GROUP)) {

						tokens.add("group(" + name + ")");

					} else {

						// Default to user(...) for User and any other Principal-shaped node.
						tokens.add("user(" + name + ")");
					}
				}

				if (!tokens.isEmpty()) {

					expression = String.join(", ", tokens);
				}
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
	private void exportProcess(final XMLStreamWriter w, final StringWriter sw, final BpmnProcess proc) throws XMLStreamException {

		w.writeStartElement(BPMN_NS, "process");

		writeAttrIfNotNull(w, "id", proc.getProcessId());
		writeAttrIfNotNull(w, sw, "name", proc.getProcessName());

		if (proc.isExecutable()) {

			w.writeAttribute("isExecutable", "true");
		}

		// Process-level extensionElements: process listeners and method refs.
		// Emitted as the first child of <bpmn:process> per BPMN spec ordering.
		exportProcessExtensionElements(w, proc);

		// Optional <bpmn:laneSet>: emit before flow elements per spec ordering.
		exportLaneSet(w, proc);

		// Top-level elements
		final Iterable<BpmnElement> elements = proc.getElements();
		if (elements != null) {

			for (final BpmnElement elemNode : elements) {

				w.writeCharacters("\n\n    ");
				exportElement(w, sw, elemNode, 4);
			}
		}

		// Top-level sequence flows
		final Iterable<BpmnSequenceFlow> flows = proc.getSequenceFlows();
		if (flows != null) {

			for (final BpmnSequenceFlow flowNode : flows) {

				w.writeCharacters("\n\n    ");
				exportSequenceFlow(w, sw, flowNode, 4);
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
	private void exportLaneSet(final XMLStreamWriter w, final BpmnProcess proc) throws XMLStreamException {

		final Iterable<BpmnLane> lanesIterable = proc.getLanes();
		if (lanesIterable == null) {

			return;
		}

		final List<BpmnLane> lanes = Iterables.toList(lanesIterable);
		if (lanes.isEmpty()) {

			return;
		}

		w.writeCharacters("\n\n    ");
		w.writeStartElement(BPMN_NS, "laneSet");
		// laneSet id is auto-generated; tools tolerate either presence/absence.
		w.writeAttribute("id", "LaneSet_" + proc.getUuid().substring(0, 8));

		for (final BpmnLane laneNode : lanes) {

			w.writeCharacters("\n      ");
			w.writeStartElement(BPMN_NS, "lane");

			writeAttrIfNotNull(w, "id", laneNode.getBpmnId());
			writeAttrIfNotNull(w, "name", laneNode.getBpmnName());

			final Iterable<BpmnElement> refs = laneNode.getFlowNodeRefs();
			if (refs != null) {

				for (final BpmnElement elemNode : refs) {

					final String elemBpmnId = elemNode.getBpmnId();
					if (elemBpmnId == null || elemBpmnId.isEmpty()) {

						continue;
					}

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
	private void exportCollaboration(final XMLStreamWriter w, final BpmnCollaboration collab) throws XMLStreamException {

		w.writeStartElement(BPMN_NS, "collaboration");
		writeAttrIfNotNull(w, "id", collab.getBpmnId());

		// Participants -- each references a process by its bpmnId (processRef).
		final Iterable<BpmnParticipant> participants = collab.getParticipants();
		if (participants != null) {

			for (final BpmnParticipant part : participants) {

				w.writeCharacters("\n    ");
				w.writeEmptyElement(BPMN_NS, "participant");

				writeAttrIfNotNull(w, "id",   part.getBpmnId());
				writeAttrIfNotNull(w, "name", part.getBpmnName());

				final BpmnProcess proc = part.getProcess();
				if (proc != null) {

					writeAttrIfNotNull(w, "processRef", proc.getBpmnId());
				}
			}
		}

		// Message flows -- cross-process arrows referencing source/target by bpmnId.
		final Iterable<BpmnMessageFlow> messageFlows = collab.getMessageFlows();
		if (messageFlows != null) {

			for (final BpmnMessageFlow mf : messageFlows) {

				w.writeCharacters("\n    ");
				w.writeEmptyElement(BPMN_NS, "messageFlow");

				writeAttrIfNotNull(w, "id",        mf.getBpmnId());
				writeAttrIfNotNull(w, "name",      mf.getBpmnName());
				writeAttrIfNotNull(w, "sourceRef", mf.getSourceRefId());
				writeAttrIfNotNull(w, "targetRef", mf.getTargetRefId());
			}
		}

		w.writeCharacters("\n  ");
		w.writeEndElement(); // collaboration
	}

	/**
	 * Emit process-level {@code <bpmn:extensionElements>} with process listeners
	 * and method refs, if any. Skipped when the process has neither.
	 */
	private void exportProcessExtensionElements(final XMLStreamWriter w, final BpmnProcess proc) throws XMLStreamException {

		final Iterable<BpmnProcessListener> listenersIter = proc.getProcessListeners();
		final Iterable<NodeInterface> methodsIter         = proc.getMethods();
		final boolean hasListeners                        = hasAny(listenersIter);
		final boolean hasMethods                          = hasAny(methodsIter);

		// Structr-native process-level subject type (round-trips via <structr:subject>).
		final String subjectType                          = proc.getProperty(proc.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY));
		final boolean hasSubject                          = subjectType != null && !subjectType.isEmpty();

		if (!hasListeners && !hasMethods && !hasSubject) {

			return;
		}

		w.writeCharacters("\n    ");
		w.writeStartElement(BPMN_NS, "extensionElements");

		if (hasSubject) {

			w.writeCharacters("\n      ");
			w.writeEmptyElement("structr", "subject", STRUCTR_NS);
			w.writeAttribute("type", subjectType);
		}

		if (hasListeners) {

			for (final BpmnProcessListener listener : listenersIter) {

				w.writeCharacters("\n      ");
				exportProcessListener(w, listener);
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
	private void exportProcessListener(final XMLStreamWriter w, final BpmnProcessListener listener) throws XMLStreamException {

		final String event  = listener.getEvent();
		final String phase  = listener.getPhase();
		final String bpmnId = listener.getBpmnId();

		// Method is now a relationship to a SchemaMethod; emit its name.
		String methodName = null;
		final NodeInterface method = listener.getMethod();

		if (method != null) {

			methodName = method.getName();
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
	private void exportTaskListener(final XMLStreamWriter w, final BpmnTaskListener listener) throws XMLStreamException {

		final String event  = listener.getEvent();
		final String phase  = listener.getPhase();
		final String bpmnId = listener.getBpmnId();

		// Method is now a relationship to a SchemaMethod; emit its name.
		String methodName = null;
		final NodeInterface method = listener.getMethod();

		if (method != null) {

			methodName = method.getName();
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

	private void exportEventDefinition(final XMLStreamWriter w, final BpmnElement elem, final String eventDefType, final int indent) throws XMLStreamException {

		final String eventDefId  = elem.getEventDefinitionId();
		final String eventDefRef = elem.getEventDefinitionRef();
		final String timerType   = elem.getTimerType();
		final String timerValue  = elem.getTimerValue();
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

			final String timerExprType = elem.getTimerExpressionType();
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
	private void exportGlobalDefinition(final XMLStreamWriter w, final BpmnGlobalDefinition gdNode) throws XMLStreamException {

		final String defType      = gdNode.getDefinitionType();
		final String errorCode    = gdNode.getErrorCode();
		final String structureRef = gdNode.getStructureRef();

		w.writeEmptyElement(BPMN_NS, defType);

		writeAttrIfNotNull(w, "id", gdNode.getBpmnId());
		writeAttrIfNotNull(w, "name", gdNode.getBpmnName());

		if (errorCode != null && !errorCode.isEmpty()) {

			w.writeAttribute("errorCode", errorCode);
		}

		if (structureRef != null && !structureRef.isEmpty()) {

			w.writeAttribute("structureRef", structureRef);
		}
	}

	private void exportSequenceFlow(final XMLStreamWriter w, final StringWriter sw, final BpmnSequenceFlow flowNode, final int indent) throws XMLStreamException {

		final String condExpr = flowNode.getConditionExpression();
		final boolean hasCondition = (condExpr != null && !condExpr.isEmpty());

		if (!hasCondition) {

			w.writeEmptyElement(BPMN_NS, "sequenceFlow");

		} else {

			w.writeStartElement(BPMN_NS, "sequenceFlow");
		}

		writeAttrIfNotNull(w, "id", flowNode.getBpmnId());
		writeAttrIfNotNull(w, sw, "name", flowNode.getBpmnName());
		writeAttrIfNotNull(w, "sourceRef", flowNode.getSourceRefId());
		writeAttrIfNotNull(w, "targetRef", flowNode.getTargetRefId());

		// Restore additional attributes
		final String attrsJson = flowNode.getBpmnAttributes();
		if (attrsJson != null) {

			final Map<String, String> attrs = safeMap(attrsJson);

			for (final Map.Entry<String, String> a : attrs.entrySet()) {

				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		if (hasCondition) {

			w.writeCharacters("\n" + spaces(indent + 2));
			w.writeStartElement(BPMN_NS, "conditionExpression");

			final String condType = flowNode.getConditionExpressionType();
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

	private void exportDiagram(final XMLStreamWriter w, final BpmnDiDiagram diagramNode) throws XMLStreamException {

		w.writeStartElement(DI_NS, "BPMNDiagram");
		writeAttrIfNotNull(w, "id", diagramNode.getDiagramId());

		w.writeCharacters("\n    ");
		w.writeStartElement(DI_NS, "BPMNPlane");
		writeAttrIfNotNull(w, "id", diagramNode.getPlaneId());
		writeAttrIfNotNull(w, "bpmnElement", diagramNode.getPlaneElement());

		final Iterable<BpmnDiShape> shapes = diagramNode.getShapes();
		if (shapes != null) {

			for (final BpmnDiShape shapeNode : shapes) {

				w.writeCharacters("\n\n      ");
				exportDiShape(w, shapeNode);
			}
		}

		final Iterable<BpmnDiEdge> edges = diagramNode.getEdges();
		if (edges != null) {

			for (final BpmnDiEdge edgeNode : edges) {

				w.writeCharacters("\n\n      ");
				exportDiEdge(w, edgeNode);
			}
		}

		w.writeCharacters("\n\n    ");
		w.writeEndElement(); // BPMNPlane
		w.writeCharacters("\n  ");
		w.writeEndElement(); // BPMNDiagram
	}

	private void exportDiShape(final XMLStreamWriter w, final BpmnDiShape shapeNode) throws XMLStreamException {

		w.writeStartElement(DI_NS, "BPMNShape");
		writeAttrIfNotNull(w, "id", shapeNode.getShapeId());
		writeAttrIfNotNull(w, "bpmnElement", shapeNode.getBpmnElementRef());

		if (shapeNode.isMarkerVisible()) {

			w.writeAttribute("isMarkerVisible", "true");
		}

		if (shapeNode.isExpanded()) {

			w.writeAttribute("isExpanded", "true");
		}

		if (shapeNode.isHorizontal()) {

			w.writeAttribute("isHorizontal", "true");
		}

		final String diAttrsJson = shapeNode.getDiAttributes();
		if (diAttrsJson != null) {

			final Map<String, String> diAttrs = safeMap(diAttrsJson);

			for (final Map.Entry<String, String> a : diAttrs.entrySet()) {

				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		w.writeCharacters("\n        ");
		w.writeEmptyElement(DC_NS, "Bounds");
		writeAttrDouble(w, "x", shapeNode.getBoundsX());
		writeAttrDouble(w, "y", shapeNode.getBoundsY());
		writeAttrDouble(w, "width", shapeNode.getBoundsWidth());
		writeAttrDouble(w, "height", shapeNode.getBoundsHeight());

		final String labelJson = shapeNode.getLabelBounds();
		final boolean hasLabel = shapeNode.hasLabel();

		if (labelJson != null) {

			final Map<String, String> lb = safeMap(labelJson);

			w.writeCharacters("\n        ");
			w.writeStartElement(DI_NS, "BPMNLabel");
			w.writeCharacters("\n          ");
			w.writeEmptyElement(DC_NS, "Bounds");
			writeAttrIfNotNull(w, "x", lb.get("x"));
			writeAttrIfNotNull(w, "y", lb.get("y"));
			writeAttrIfNotNull(w, "width", lb.get("width"));
			writeAttrIfNotNull(w, "height", lb.get("height"));
			w.writeCharacters("\n        ");
			w.writeEndElement();

		} else if (hasLabel) {

			w.writeCharacters("\n        ");
			w.writeEmptyElement(DI_NS, "BPMNLabel");
		}

		w.writeCharacters("\n      ");
		w.writeEndElement();
	}

	private void exportDiEdge(final XMLStreamWriter w, final BpmnDiEdge edgeNode) throws XMLStreamException {

		w.writeStartElement(DI_NS, "BPMNEdge");
		writeAttrIfNotNull(w, "id", edgeNode.getEdgeId());
		writeAttrIfNotNull(w, "bpmnElement", edgeNode.getBpmnElementRef());

		final String diAttrsJson = edgeNode.getDiAttributes();
		if (diAttrsJson != null) {

			final Map<String, String> diAttrs = safeMap(diAttrsJson);

			for (final Map.Entry<String, String> a : diAttrs.entrySet()) {

				w.writeAttribute(a.getKey(), a.getValue());
			}
		}

		final String wpJson = edgeNode.getWaypoints();
		if (wpJson != null) {

			final List<Map<String, String>> waypoints = safeList(wpJson);

			for (final Map<String, String> wp : waypoints) {

				w.writeCharacters("\n        ");
				w.writeEmptyElement(OMGDI_NS, "waypoint");
				w.writeAttribute("x", wp.get("x"));
				w.writeAttribute("y", wp.get("y"));
			}
		}

		final String labelJson = edgeNode.getLabelBounds();
		if (labelJson != null) {

			final Map<String, String> lb = safeMap(labelJson);

			w.writeCharacters("\n        ");
			w.writeStartElement(DI_NS, "BPMNLabel");
			w.writeCharacters("\n          ");
			w.writeEmptyElement(DC_NS, "Bounds");
			writeAttrIfNotNull(w, "x", lb.get("x"));
			writeAttrIfNotNull(w, "y", lb.get("y"));
			writeAttrIfNotNull(w, "width", lb.get("width"));
			writeAttrIfNotNull(w, "height", lb.get("height"));
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

			if (uri.equals(entry.getValue())) {

				if (entry.getKey().startsWith("xmlns:")) {

					return entry.getKey().substring(6);
				}

				if ("xmlns".equals(entry.getKey())) {

					// The namespace is declared as the DEFAULT (unprefixed) namespace.
					// Return the empty prefix so the element is written unprefixed under
					// that default namespace; forcing a "bpmn" prefix here would emit an
					// unbound prefix (no matching xmlns:bpmn is written), producing
					// namespace-ill-formed XML.

					return "";
				}
			}
		}

		return "bpmn";
	}

	private String spaces(final int count) {

		return " ".repeat(count);
	}

	/**
	 * Parse a stored JSON object into a mutable map, tolerating null / the literal
	 * {@code "null"} / malformed JSON (returns an empty mutable map) so a single bad
	 * attribute value can never abort the whole export.
	 */
	private Map<String, String> safeMap(final String json) {

		if (json != null) {

			try {

				final Map<String, String> parsed = gson.fromJson(json, MAP_TYPE);
				if (parsed != null) {

					return parsed;
				}

			} catch (final Exception ex) {

				logger.warn("Ignoring malformed JSON attribute value during export: {}", ex.getMessage());
			}
		}

		return new LinkedHashMap<>();
	}

	/** List counterpart of {@link #safeMap}; empty list on null / malformed input. */
	private List<Map<String, String>> safeList(final String json) {

		if (json != null) {

			try {

				final List<Map<String, String>> parsed = gson.fromJson(json, LIST_TYPE);
				if (parsed != null) {

					return parsed;
				}

			} catch (final Exception ex) {

				logger.warn("Ignoring malformed JSON list value during export: {}", ex.getMessage());
			}
		}

		return new LinkedList<>();
	}

	private boolean hasAny(final Iterable<?> iterable) {

		return iterable != null && iterable.iterator().hasNext();
	}
}
