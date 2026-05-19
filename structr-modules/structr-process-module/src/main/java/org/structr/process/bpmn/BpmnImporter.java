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
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.process.traits.definitions.*;
import org.structr.process.ProcessTraits;
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

	private static final String BPMN_NS    = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	private static final String DI_NS      = "http://www.omg.org/spec/BPMN/20100524/DI";
	private static final String DC_NS      = "http://www.omg.org/spec/DD/20100524/DC";
	private static final String OMGDI_NS   = "http://www.omg.org/spec/DD/20100524/DI";
	private static final String XSI_NS     = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String CAMUNDA_NS = "http://camunda.org/schema/1.0/bpmn";
	private static final String STRUCTR_NS = "http://structr.org/schema/process/1.0";

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

	/**
	 * Version stamp applied to every node created during the current import.
	 * Auto-incremented per processId in computeNextVersion() once we know it,
	 * then read by createBpmnNode() at every creation site.
	 */
	private String currentVersion;

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

		// Create BpmnDefinitions (file root) -- holds metadata + container rels
		// for processes, collaboration and DI diagrams. Per-process state lives
		// on each BpmnProcess child.
		final NodeInterface defNode = app.create(ProcessTraits.BPMN_DEFINITIONS, (String) null);
		final Traits defTraits = defNode.getTraits();

		defNode.setProperty(defTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),                    root.getAttribute("id"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY),       root.getAttribute("targetNamespace"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY),               root.getAttribute("exporter"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY),       root.getAttribute("exporterVersion"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS),          gson.toJson(namespaces));

		final NodeList processNodes = root.getElementsByTagNameNS(BPMN_NS, "process");
		if (processNodes.getLength() == 0) {
			throw new FrameworkException(422, "No bpmn:process element found in BPMN XML");
		}

		// Use the first process's id for versioning (single chain across the
		// file even if multi-process). All BpmnProcess + child nodes created
		// during this import get this version stamp.
		final Element firstProcessEl = (Element) processNodes.item(0);
		final String firstProcessId = firstProcessEl.getAttribute("id");
		this.currentVersion = computeNextVersion(app, firstProcessId);
		stampVersion(defNode);

		// File-level display name: prefer the first process's name attribute.
		final String firstProcessName = firstProcessEl.getAttribute("name");
		defNode.setProperty(defTraits.key("name"), firstProcessName != null && !firstProcessName.isEmpty() ? firstProcessName : firstProcessId);

		// Top-level global definitions (message, signal, error, escalation, ...)
		// live on BpmnDefinitions, not on any individual process.
		final NodeList rootChildren = root.getChildNodes();
		for (int i = 0; i < rootChildren.getLength(); i++) {
			final Node child = rootChildren.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				final Element childEl = (Element) child;
				final String ln = childEl.getLocalName();
				if (!"process".equals(ln) && !"BPMNDiagram".equals(ln) && !"collaboration".equals(ln)) {
					importGlobalDefinition(app, defNode, childEl, ln);
				}
			}
		}

		// Map of bpmnId -> NodeInterface for resolving references across all
		// processes in this file. Sequence flows, DI shapes/edges, and message
		// flows look up source/target through this map.
		final Map<String, NodeInterface> elementMap = new LinkedHashMap<>();
		final Map<String, NodeInterface> flowMap    = new LinkedHashMap<>();

		// Map of processBpmnId -> BpmnProcess node, used by the collaboration
		// import to wire participants to their referenced processes.
		final Map<String, NodeInterface> processMap = new LinkedHashMap<>();

		// BpmnIds of imported lanes so the DI filter keeps their pool-style
		// BPMNShape entries. Populated by importLaneSet during process walk.
		final Set<String> laneBpmnIds = new HashSet<>();

		// Find previous BpmnProcess by processId (used for method cloning).
		// A multi-process file with N processes does N independent lookups.
		for (int i = 0; i < processNodes.getLength(); i++) {
			final Element processEl = (Element) processNodes.item(i);
			final String processIdAttr = processEl.getAttribute("id");

			final NodeInterface procNode = createBpmnNode(app, ProcessTraits.BPMN_PROCESS);
			final Traits procTraits = procNode.getTraits();
			procNode.setProperty(procTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),                  processIdAttr);
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY),                processIdAttr);
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY),              processEl.getAttribute("name"));
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_IS_EXECUTABLE_PROPERTY),     "true".equals(processEl.getAttribute("isExecutable")));
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.DEFINITION_PROPERTY),                defNode);
			final String procName = processEl.getAttribute("name");
			procNode.setProperty(procTraits.key("name"), procName != null && !procName.isEmpty() ? procName : processIdAttr);

			processMap.put(processIdAttr, procNode);

			// Resolve previous version of THIS process (by processId).
			final NodeInterface previousProc = findPreviousProcess(app, processIdAttr, procNode.getUuid());

			if (previousProc != null) {
				cloneProcessMethods(app, procNode, previousProc);
			}

			// Import process child elements (recursively handles sub-processes)
			importProcessChildren(app, procNode, null, processEl, elementMap, flowMap);

			if (previousProc != null) {
				cloneElementMethods(app, previousProc, elementMap);
			}

			// Optional <bpmn:laneSet>: each lane carries a name and a list of
			// flowNodeRefs pointing back into elementMap. Lanes are pure
			// layout; the engine ignores them but we keep them so the editor
			// can paint pool subdivisions.
			importLaneSet(app, procNode, processEl, elementMap, laneBpmnIds);

			// Process-level lifecycle listeners.
			importProcessListeners(app, procNode, processEl);

			// methodRef extension elements when no previous version exists.
			if (previousProc == null) {
				importProcessMethodRefs(app, procNode, processEl);
				importElementMethodRefs(app, processEl, elementMap);
			}
		}

		// Optional <bpmn:collaboration>: create BpmnCollaboration with its
		// participants and message flows. Participant.process is wired via
		// processMap; messageFlow source/target are resolved against
		// elementMap (cross-process bpmnId match). The bpmnIds of the
		// imported participants and messageFlows go into separate sets so
		// the DI filter below knows their shapes/edges are legitimate
		// (otherwise they'd be dropped as "orphan" references that don't
		// resolve to a BpmnElement / BpmnSequenceFlow).
		final Set<String> participantBpmnIds = new HashSet<>();
		final Set<String> messageFlowBpmnIds = new HashSet<>();
		final NodeList collabs = root.getElementsByTagNameNS(BPMN_NS, "collaboration");
		if (collabs.getLength() > 0) {
			importCollaboration(app, defNode, (Element) collabs.item(0), processMap, elementMap,
				participantBpmnIds, messageFlowBpmnIds);
		}

		// Rewire incoming references for any process whose id we just imported.
		// VisibilityMappings and ActionMappings hold references by processId;
		// repoint them at the new BpmnDefinitions and (per processId) the
		// element map. The new file root holds all the rewired bindings.
		for (final String processIdAttr : processMap.keySet()) {
			rewireExternalReferences(app, processIdAttr, defNode, elementMap);
		}

		// Resolve sequence flow source/target references across all processes.
		resolveFlowReferences(elementMap, flowMap);

		// Resolve boundary-event `attachedToRef` -> host activity. Sets
		// the typed `attachedTo` relationship and strips the now-redundant
		// attribute out of bpmnAttributes so the graph is the single
		// source of truth for the attachment.
		resolveBoundaryAttachments(elementMap);

		// Import DI data (lives on BpmnDefinitions; spans all processes).
		// Pass the participant + messageFlow + lane bpmnId sets so the DI
		// filter keeps shapes that visualise the collaboration / lane set.
		final NodeList diagramNodes = root.getElementsByTagNameNS(DI_NS, "BPMNDiagram");
		for (int i = 0; i < diagramNodes.getLength(); i++) {
			importDiagram(app, defNode, (Element) diagramNodes.item(i), elementMap, flowMap,
				participantBpmnIds, messageFlowBpmnIds, laneBpmnIds);
		}

		return defNode;
	}

	/**
	 * Create the {@code BpmnCollaboration} node with its participants and
	 * message flows. Participants reference {@code BpmnProcess} via
	 * {@code processRef}; message flows hold source/target {@code bpmnId}
	 * strings and (when resolvable) edges to the actual {@code BpmnElement}s.
	 */
	private void importCollaboration(final App app, final NodeInterface defNode, final Element collabEl,
									 final Map<String, NodeInterface> processMap,
									 final Map<String, NodeInterface> elementMap,
									 final Set<String> participantBpmnIds,
									 final Set<String> messageFlowBpmnIds) throws FrameworkException {

		final NodeInterface collabNode = createBpmnNode(app, ProcessTraits.BPMN_COLLABORATION);
		final Traits collabTraits = collabNode.getTraits();
		collabNode.setProperty(collabTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),       collabEl.getAttribute("id"));
		collabNode.setProperty(collabTraits.key(BpmnCollaborationTraitDefinition.DEFINITION_PROPERTY), defNode);
		collabNode.setProperty(collabTraits.key("name"), collabEl.getAttribute("id"));

		final NodeList children = collabEl.getChildNodes();
		final Traits partTraits = Traits.of(ProcessTraits.BPMN_PARTICIPANT);
		final Traits mfTraits   = Traits.of(ProcessTraits.BPMN_MESSAGE_FLOW);

		for (int i = 0; i < children.getLength(); i++) {
			final Node n = children.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) continue;
			final Element child = (Element) n;
			if (!BPMN_NS.equals(child.getNamespaceURI())) continue;
			final String ln = child.getLocalName();

			if ("participant".equals(ln)) {
				final String partBpmnId = child.getAttribute("id");
				final NodeInterface partNode = createBpmnNode(app, ProcessTraits.BPMN_PARTICIPANT);
				partNode.setProperty(partTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),                partBpmnId);
				partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.BPMN_NAME_PROPERTY),           child.getAttribute("name"));
				final String pname = child.getAttribute("name");
				partNode.setProperty(partTraits.key("name"), pname != null && !pname.isEmpty() ? pname : partBpmnId);
				partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.COLLABORATION_PROPERTY),       collabNode);
				final String processRef = child.getAttribute("processRef");
				if (processRef != null && !processRef.isEmpty()) {
					final NodeInterface procNode = processMap.get(processRef);
					if (procNode != null) {
						partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.PROCESS_PROPERTY), procNode);
					} else {
						logger.warn("Participant '{}' references unknown process '{}'", partBpmnId, processRef);
					}
				}
				if (partBpmnId != null && !partBpmnId.isEmpty()) participantBpmnIds.add(partBpmnId);
			} else if ("messageFlow".equals(ln)) {
				final String mfBpmnId = child.getAttribute("id");
				final NodeInterface mfNode = createBpmnNode(app, ProcessTraits.BPMN_MESSAGE_FLOW);
				mfNode.setProperty(mfTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),               mfBpmnId);
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.BPMN_NAME_PROPERTY),          child.getAttribute("name"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.SOURCE_REF_ID_PROPERTY),      child.getAttribute("sourceRef"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.TARGET_REF_ID_PROPERTY),      child.getAttribute("targetRef"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.COLLABORATION_PROPERTY),      collabNode);
				final String mfName = child.getAttribute("name");
				mfNode.setProperty(mfTraits.key("name"), mfName != null && !mfName.isEmpty() ? mfName : mfBpmnId);
				// Resolve source / target if both elements were imported.
				final NodeInterface src = elementMap.get(child.getAttribute("sourceRef"));
				final NodeInterface tgt = elementMap.get(child.getAttribute("targetRef"));
				if (src != null) mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.SOURCE_ELEMENT_PROPERTY), src);
				if (tgt != null) mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.TARGET_ELEMENT_PROPERTY), tgt);
				if (mfBpmnId != null && !mfBpmnId.isEmpty()) messageFlowBpmnIds.add(mfBpmnId);
			}
		}
	}

	/**
	 * Recursively import child elements of a process or sub-process container.
	 */
	private void importProcessChildren(final App app, final NodeInterface procNode, final NodeInterface parentElement,
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
				final NodeInterface flowNode = importSequenceFlow(app, procNode, parentElement, el);
				flowMap.put(el.getAttribute("id"), flowNode);
			} else if ("subProcess".equals(localName)) {
				final NodeInterface subProcNode = importElement(app, procNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), subProcNode);
				importProcessChildren(app, procNode, subProcNode, el, elementMap, flowMap);
			} else if (KNOWN_ELEMENT_TYPES.contains(localName)) {
				final NodeInterface elemNode = importElement(app, procNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), elemNode);
			} else if (!"incoming".equals(localName)
					&& !"outgoing".equals(localName)
					&& !"laneSet".equals(localName)
					&& !"lane".equals(localName)
					&& !"flowNodeRef".equals(localName)) {
				// Unknown element -- import as generic BpmnElement (skip incoming/outgoing
				// flow ID stubs and laneSet/lane/flowNodeRef which are handled by importLaneSet).
				final NodeInterface elemNode = importElement(app, procNode, parentElement, el, localName);
				elementMap.put(el.getAttribute("id"), elemNode);
			}
		}
	}

	/**
	 * Import the {@code <bpmn:laneSet>} children of a process element. Each
	 * {@code <bpmn:lane>} becomes a BpmnLane node wired back to the process
	 * via BPMN_PROCESS_HAS_LANE; {@code <bpmn:flowNodeRef>} entries are
	 * resolved against {@code elementMap} and attached via
	 * BPMN_LANE_HAS_FLOW_NODE. Lanes whose bpmnIds are added to
	 * {@code laneBpmnIds} so the DI filter keeps their pool-style shapes.
	 *
	 * <p>Multiple {@code <bpmn:laneSet>} elements per process are flattened
	 * into a single in-memory collection -- the spec allows them but in
	 * practice tools emit at most one.</p>
	 */
	private void importLaneSet(final App app, final NodeInterface procNode, final Element processEl,
							   final Map<String, NodeInterface> elementMap,
							   final Set<String> laneBpmnIds) throws FrameworkException {

		final NodeList laneSets = processEl.getElementsByTagNameNS(BPMN_NS, "laneSet");
		for (int i = 0; i < laneSets.getLength(); i++) {
			final Element laneSetEl = (Element) laneSets.item(i);
			final NodeList lanes = laneSetEl.getElementsByTagNameNS(BPMN_NS, "lane");
			for (int j = 0; j < lanes.getLength(); j++) {
				final Element laneEl = (Element) lanes.item(j);

				final NodeInterface laneNode = createBpmnNode(app, ProcessTraits.BPMN_LANE);
				final Traits laneTraits = laneNode.getTraits();

				final String laneBpmnId = laneEl.getAttribute("id");
				final String laneName   = laneEl.getAttribute("name");

				laneNode.setProperty(laneTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), laneBpmnId);
				laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY),  laneName);
				laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.PROCESS_PROPERTY),    procNode);
				laneNode.setProperty(laneTraits.key("name"),
					(laneName != null && !laneName.isEmpty()) ? laneName : laneBpmnId);

				if (laneBpmnId != null && !laneBpmnId.isEmpty()) {
					laneBpmnIds.add(laneBpmnId);
				}

				// Resolve <bpmn:flowNodeRef> entries -> BpmnElement nodes.
				final NodeList refs = laneEl.getElementsByTagNameNS(BPMN_NS, "flowNodeRef");
				final List<NodeInterface> resolved = new ArrayList<>();
				for (int k = 0; k < refs.getLength(); k++) {
					final String refId = refs.item(k).getTextContent();
					if (refId == null) continue;
					final NodeInterface elemNode = elementMap.get(refId.trim());
					if (elemNode != null) resolved.add(elemNode);
				}
				if (!resolved.isEmpty()) {
					laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY), resolved);
				}
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
	 * Walk every imported boundary-event element, read its `attachedToRef`
	 * out of bpmnAttributes, resolve against the element map, and set the
	 * typed {@code attachedTo} relationship. The ref is then removed from
	 * bpmnAttributes so the graph is the only source of truth (export
	 * regenerates the attribute from the relationship).
	 *
	 * cancelActivity is left in bpmnAttributes: it's a single boolean
	 * attribute on the boundary itself, doesn't need typing, and the
	 * exporter handles it via the generic attribute pass-through.
	 */
	@SuppressWarnings("unchecked")
	private void resolveBoundaryAttachments(final Map<String, NodeInterface> elementMap) throws FrameworkException {

		for (final NodeInterface elemNode : elementMap.values()) {

			final Traits traits   = elemNode.getTraits();
			final String elemType = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY));
			if (!"boundaryEvent".equals(elemType)) continue;

			final String json = elemNode.getProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY));
			if (json == null || json.isEmpty()) continue;

			Map<String, Object> attrs;
			try {
				attrs = gson.fromJson(json, Map.class);
			} catch (Exception ex) {
				logger.warn("Boundary attachedToRef: malformed bpmnAttributes JSON on {}: {}", elemNode.getUuid(), ex.getMessage());
				continue;
			}
			if (attrs == null) continue;

			final Object refObj = attrs.get("attachedToRef");
			if (!(refObj instanceof String)) continue;
			final String hostBpmnId = ((String) refObj).trim();
			if (hostBpmnId.isEmpty()) continue;

			final NodeInterface host = elementMap.get(hostBpmnId);
			if (host == null) {
				logger.warn("Boundary attachedToRef '{}' on element '{}' does not resolve to any imported element", hostBpmnId, elemNode.getUuid());
				continue;
			}
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.ATTACHED_TO_PROPERTY), host);

			// Strip attachedToRef from bpmnAttributes; rewrite or clear.
			attrs.remove("attachedToRef");
			final String newJson = attrs.isEmpty() ? null : gson.toJson(attrs);
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ATTRIBUTES_PROPERTY), newJson);
		}
	}

	/**
	 * Import a single BPMN element with all its content extracted into typed properties.
	 */
	private NodeInterface importElement(final App app, final NodeInterface procNode, final NodeInterface parentElement,
										final Element el, final String elementType) throws FrameworkException {

		final NodeInterface elemNode = createBpmnNode(app, ProcessTraits.BPMN_ELEMENT);
		final Traits traits = elemNode.getTraits();

		elemNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY), elementType);

		final String name = el.getAttribute("name");
		if (name != null && !name.isEmpty()) {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_NAME_PROPERTY), name);
			elemNode.setProperty(traits.key("name"), name);
		} else {
			elemNode.setProperty(traits.key("name"), elementType + " " + el.getAttribute("id"));
		}

		// Performers: parse standard <bpmn:humanPerformer> / <bpmn:potentialOwner> /
		// <bpmn:performer> sub-elements, and auto-translate Camunda extension
		// attributes (camunda:assignee / camunda:candidateUsers / camunda:candidateGroups)
		// into equivalent BpmnPerformer nodes for interop on import. Once translated,
		// the camunda:* attributes are stripped from the bpmnAttributes JSON so the
		// graph is the single source of truth (and exports are standards-clean).
		final List<String> consumedAttributeKeys = new ArrayList<>();
		importPerformers(app, elemNode, el, consumedAttributeKeys);

		// Task listeners: parse <bpmn:extensionElements>'s <structr:taskListener>
		// children and auto-translate <camunda:taskListener> on import. Both flavors
		// collapse to BpmnTaskListener nodes; exports always emit the structr form.
		importTaskListeners(app, elemNode, el);

		// Store extra attributes as JSON (everything except id, name, and consumed camunda:* keys)
		final Map<String, String> attrs = collectAttributes(el);
		attrs.remove("id");
		attrs.remove("name");
		for (final String consumed : consumedAttributeKeys) {
			attrs.remove(consumed);
		}
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
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.PROCESS_PROPERTY), procNode);
		} else {
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.PARENT_ELEMENT_PROPERTY), parentElement);
		}

		return elemNode;
	}

	/**
	 * Import a sequence flow.
	 */
	private NodeInterface importSequenceFlow(final App app, final NodeInterface procNode, final NodeInterface parentElement,
											 final Element el) throws FrameworkException {

		final NodeInterface flowNode = createBpmnNode(app, ProcessTraits.BPMN_SEQUENCE_FLOW);
		final Traits traits = flowNode.getTraits();

		flowNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
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
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.PROCESS_PROPERTY), procNode);
		} else {
			flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.PARENT_ELEMENT_PROPERTY), parentElement);
		}

		return flowNode;
	}

	// --- DI import methods (unchanged) ---

	private void importDiagram(final App app, final NodeInterface defNode, final Element diagramEl,
							   final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap,
							   final Set<String> participantBpmnIds, final Set<String> messageFlowBpmnIds,
							   final Set<String> laneBpmnIds) throws FrameworkException {

		final NodeInterface diagramNode = createBpmnNode(app, ProcessTraits.BPMN_DI_DIAGRAM);
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
			int skippedShapes = 0;
			for (int i = 0; i < shapes.getLength(); i++) {
				final Element sh = (Element) shapes.item(i);
				final String ref = sh.getAttribute("bpmnElement");
				// Keep shapes that visualise either an imported BpmnElement,
				// an imported BpmnParticipant (collaboration pool), or an
				// imported BpmnLane (pool subdivision). Drop shapes
				// referencing entities we didn't import.
				if (ref == null || ref.isEmpty()
						|| (!elementMap.containsKey(ref)
							&& !participantBpmnIds.contains(ref)
							&& !laneBpmnIds.contains(ref))) {
					skippedShapes++;
					continue;
				}
				importDiShape(app, diagramNode, sh, elementMap);
			}
			if (skippedShapes > 0) {
				logger.warn("Skipped {} BPMNShape(s) referencing entities that were not imported.", skippedShapes);
			}

			final NodeList edges = planeEl.getElementsByTagNameNS(DI_NS, "BPMNEdge");
			int skippedEdges = 0;
			for (int i = 0; i < edges.getLength(); i++) {
				final Element ed = (Element) edges.item(i);
				final String ref = ed.getAttribute("bpmnElement");
				// Keep edges for sequence flows AND message flows.
				if (ref == null || ref.isEmpty()
						|| (!flowMap.containsKey(ref) && !messageFlowBpmnIds.contains(ref))) {
					skippedEdges++;
					continue;
				}
				importDiEdge(app, diagramNode, ed, flowMap);
			}
			if (skippedEdges > 0) {
				logger.warn("Skipped {} BPMNEdge(s) referencing entities that were not imported.", skippedEdges);
			}
		}
	}

	private void importDiShape(final App app, final NodeInterface diagramNode, final Element shapeEl,
							   final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final NodeInterface shapeNode = createBpmnNode(app, ProcessTraits.BPMN_DI_SHAPE);
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

		final NodeInterface edgeNode = createBpmnNode(app, ProcessTraits.BPMN_DI_EDGE);
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

		final NodeInterface gdNode = createBpmnNode(app, ProcessTraits.BPMN_GLOBAL_DEFINITION);
		final Traits traits = gdNode.getTraits();

		gdNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
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

	// --- Performer parsing ---

	/**
	 * Parse all performer sub-elements ({@code <humanPerformer>}, {@code <potentialOwner>},
	 * {@code <performer>}) and synthesize equivalents from Camunda extension attributes
	 * ({@code camunda:assignee}, {@code camunda:candidateUsers}, {@code camunda:candidateGroups}).
	 *
	 * Each consumed Camunda attribute key is added to {@code consumedAttributeKeys} so the
	 * caller can strip it from the generic-attributes JSON.
	 */
	private void importPerformers(final App app, final NodeInterface elemNode, final Element el,
								  final List<String> consumedAttributeKeys) throws FrameworkException {

		// Standard BPMN sub-elements (multiple of each kind allowed by spec)
		importPerformerSubElements(app, elemNode, el, "humanPerformer",  BpmnPerformerTraitDefinition.KIND_HUMAN_PERFORMER);
		importPerformerSubElements(app, elemNode, el, "potentialOwner",  BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER);
		importPerformerSubElements(app, elemNode, el, "performer",       BpmnPerformerTraitDefinition.KIND_PERFORMER);

		// Camunda interop on input only (we never emit these on export).
		// camunda:assignee="demo"           -> humanPerformer body "user(demo)"
		// camunda:assignee="${initiator}"   -> humanPerformer body "${initiator}" (expression preserved)
		final String camundaAssignee = el.getAttributeNS(CAMUNDA_NS, "assignee");
		if (camundaAssignee != null && !camundaAssignee.isEmpty()) {
			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_HUMAN_PERFORMER,
				wrapAsUserIfBare(camundaAssignee), null, null, null);
			consumedAttributeKeys.add("camunda:assignee");
		}

		// camunda:candidateUsers="alice, bob" -> potentialOwner body "user(alice), user(bob)"
		final String candidateUsers = el.getAttributeNS(CAMUNDA_NS, "candidateUsers");
		if (candidateUsers != null && !candidateUsers.isEmpty()) {
			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER,
				csvToFunctionExpression(candidateUsers, "user"), null, null, null);
			consumedAttributeKeys.add("camunda:candidateUsers");
		}

		// camunda:candidateGroups="managers, hr" -> potentialOwner body "group(managers), group(hr)"
		final String candidateGroups = el.getAttributeNS(CAMUNDA_NS, "candidateGroups");
		if (candidateGroups != null && !candidateGroups.isEmpty()) {
			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER,
				csvToFunctionExpression(candidateGroups, "group"), null, null, null);
			consumedAttributeKeys.add("camunda:candidateGroups");
		}
	}

	/**
	 * If {@code raw} is already an expression (starts with {@code ${}), return it
	 * verbatim. Otherwise treat it as a bare username and wrap as {@code user(raw)}.
	 */
	private String wrapAsUserIfBare(final String raw) {
		final String trimmed = raw.trim();
		if (trimmed.startsWith("${")) {
			return trimmed;
		}
		return "user(" + trimmed + ")";
	}

	/**
	 * Parse all standard BPMN performer sub-elements of the given kind under {@code parent}.
	 *
	 * Diagnoses the two common authoring mistakes (text directly inside
	 * {@code <performer>}, or a {@code <resourceAssignmentExpression>} without a
	 * nested {@code <formalExpression>}) and logs a clear warning instead of
	 * silently producing an empty performer node.
	 */
	private void importPerformerSubElements(final App app, final NodeInterface elemNode, final Element parent,
											final String localName, final String kind) throws FrameworkException {

		final NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE || !localName.equals(child.getLocalName())) {
				continue;
			}
			final Element performerEl = (Element) child;

			final Element rae    = getFirstChildByLocalName(performerEl, "resourceAssignmentExpression");
			final Element formal = (rae != null) ? getFirstChildByLocalName(rae, "formalExpression") : null;
			final String body    = (formal != null) ? formal.getTextContent().trim() : "";
			final String lang    = (formal != null) ? nullIfEmpty(formal.getAttribute("language")) : null;

			final String performerName = nullIfEmpty(performerEl.getAttribute("name"));
			final String performerId   = nullIfEmpty(performerEl.getAttribute("id"));

			if (body.isEmpty()) {
				// No expression we could extract. Identify likely authoring mistakes.
				final String parentId = parent.getAttribute("id");
				if (rae == null && hasNonWhitespaceText(performerEl)) {
					logger.warn("BPMN <{}> on element '{}' contains text directly inside the element ('{}'); the standard requires it to be wrapped in <resourceAssignmentExpression><formalExpression>...</...></...>. Performer skipped.",
						localName, parentId, collapseWhitespace(performerEl.getTextContent()));
				} else if (rae != null && formal == null && hasNonWhitespaceText(rae)) {
					logger.warn("BPMN <{}> on element '{}' has a <resourceAssignmentExpression> with text but no nested <formalExpression> ('{}'). Performer skipped.",
						localName, parentId, collapseWhitespace(rae.getTextContent()));
				} else {
					logger.warn("BPMN <{}> on element '{}' declares no resolvable expression body. Performer skipped.",
						localName, parentId);
				}
				continue;
			}

			createPerformerNode(app, elemNode, kind, body, lang, performerName, performerId);
		}
	}

	private boolean hasNonWhitespaceText(final Element el) {
		final String t = el.getTextContent();
		return t != null && !t.trim().isEmpty();
	}

	private String collapseWhitespace(final String s) {
		if (s == null) return "";
		return s.replaceAll("\\s+", " ").trim();
	}

	private void createPerformerNode(final App app, final NodeInterface elemNode, final String kind,
									 final String expression, final String language,
									 final String performerName, final String bpmnId) throws FrameworkException {

		final NodeInterface node = createBpmnNode(app, ProcessTraits.BPMN_PERFORMER);
		final Traits t = node.getTraits();
		node.setProperty(t.key(BpmnPerformerTraitDefinition.KIND_PROPERTY),       kind);
		node.setProperty(t.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY), expression);
		if (language != null) {
			node.setProperty(t.key(BpmnPerformerTraitDefinition.EXPRESSION_LANGUAGE_PROPERTY), language);
		}
		if (performerName != null) {
			node.setProperty(t.key(BpmnPerformerTraitDefinition.PERFORMER_NAME_PROPERTY), performerName);
		}
		if (bpmnId != null) {
			node.setProperty(t.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}
		node.setProperty(t.key(BpmnPerformerTraitDefinition.ELEMENT_PROPERTY), elemNode);
	}

	/**
	 * Translate a camunda CSV (e.g. "alice, bob") to a Structr expression body
	 * using the given function name, e.g. "user(alice), user(bob)". Entries that
	 * are already expressions ({@code ${...}}) are preserved verbatim.
	 */
	private String csvToFunctionExpression(final String csv, final String fnName) {
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (final String raw : csv.split(",")) {
			final String name = raw.trim();
			if (name.isEmpty()) {
				continue;
			}
			if (!first) out.append(", ");
			if (name.startsWith("${")) {
				out.append(name);
			} else {
				out.append(fnName).append('(').append(name).append(')');
			}
			first = false;
		}
		return out.toString();
	}

	private String nullIfEmpty(final String s) {
		return (s == null || s.isEmpty()) ? null : s;
	}

	// --- Task listener parsing ---

	/**
	 * Parse task listeners declared in {@code <bpmn:extensionElements>} on the
	 * given element. Recognises:
	 *
	 * <ul>
	 *   <li>{@code <structr:taskListener event="..." method="..." sync="..."/>}
	 *       (canonical, what we emit on export)</li>
	 *   <li>{@code <camunda:taskListener event="..." class="..." />} or with
	 *       {@code expression}, {@code delegateExpression}, {@code script} -- auto-translated
	 *       on input only. Camunda's {@code class}/{@code delegateExpression}/{@code expression}
	 *       payloads are passed through to the {@code method} field; users who imported
	 *       Camunda BPMN should review these and replace with Structr schema-method names.</li>
	 * </ul>
	 *
	 * Camunda event names are translated to ours: {@code create -> created},
	 * {@code assignment -> assigned}, {@code complete -> completed},
	 * {@code delete -> cancelled}. Other Camunda events are imported with their
	 * raw name (logged so the user knows).
	 */
	private void importTaskListeners(final App app, final NodeInterface elemNode, final Element el) throws FrameworkException {

		final Element extEl = getFirstChildByLocalName(el, "extensionElements");
		if (extEl == null) {
			return;
		}

		final NodeList children = extEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			if (!"taskListener".equals(child.getLocalName())) continue;

			final Element listenerEl = (Element) child;
			final String  ns         = listenerEl.getNamespaceURI();

			final String rawEvent = listenerEl.getAttribute("event");
			if (rawEvent == null || rawEvent.isEmpty()) {
				logger.warn("Task listener on element '{}' has no 'event' attribute -- skipped",
					el.getAttribute("id"));
				continue;
			}

			final String eventName  = translateListenerEvent(rawEvent, ns);
			final String methodName = extractListenerMethod(listenerEl, ns);
			if (methodName == null || methodName.isEmpty()) {
				logger.warn("Task listener on element '{}' (event='{}') has no method/class/expression payload -- skipped",
					el.getAttribute("id"), rawEvent);
				continue;
			}

			final boolean sync   = "true".equalsIgnoreCase(nullIfEmpty(listenerEl.getAttribute("sync")));
			final String  bpmnId = nullIfEmpty(listenerEl.getAttribute("id"));

			createTaskListenerNode(app, elemNode, eventName, methodName, sync, bpmnId);
		}
	}

	private String translateListenerEvent(final String rawEvent, final String ns) {
		if (CAMUNDA_NS.equals(ns)) {
			switch (rawEvent) {
				case "create":     return BpmnTaskListenerTraitDefinition.EVENT_CREATED;
				case "assignment": return BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED;
				case "complete":   return BpmnTaskListenerTraitDefinition.EVENT_COMPLETED;
				case "delete":     return BpmnTaskListenerTraitDefinition.EVENT_CANCELLED;
				default:
					logger.warn("Camunda task listener event '{}' has no Structr equivalent; importing as-is", rawEvent);
					return rawEvent;
			}
		}
		// structr:taskListener and any other namespace: pass through as-is
		return rawEvent;
	}

	/**
	 * Extract the listener's method-name payload. For {@code structr:taskListener}
	 * we use the {@code method} attribute. For {@code camunda:taskListener} we
	 * accept {@code class}, {@code delegateExpression}, or {@code expression}
	 * (in that order); only the string is preserved -- the user must adapt the
	 * referenced code to a Structr schema method themselves.
	 */
	private String extractListenerMethod(final Element listenerEl, final String ns) {
		final String method = nullIfEmpty(listenerEl.getAttribute("method"));
		if (method != null) return method;

		// Camunda fallbacks
		final String clazz   = nullIfEmpty(listenerEl.getAttribute("class"));
		if (clazz != null)   return clazz;
		final String dExpr   = nullIfEmpty(listenerEl.getAttribute("delegateExpression"));
		if (dExpr != null)   return dExpr;
		final String expr    = nullIfEmpty(listenerEl.getAttribute("expression"));
		if (expr != null)    return expr;

		// Inline <camunda:script> child
		final Element scriptEl = getFirstChildByLocalName(listenerEl, "script");
		if (scriptEl != null) {
			final String scriptText = scriptEl.getTextContent();
			if (scriptText != null && !scriptText.trim().isEmpty()) {
				return scriptText.trim();
			}
		}
		return null;
	}

	private void createTaskListenerNode(final App app, final NodeInterface elemNode,
										final String event, final String method, final boolean sync,
										final String bpmnId) throws FrameworkException {

		final NodeInterface node = createBpmnNode(app, ProcessTraits.BPMN_TASK_LISTENER);
		final Traits t = node.getTraits();
		node.setProperty(t.key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY),  event);
		node.setProperty(t.key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY), method);
		if (sync) {
			node.setProperty(t.key(BpmnTaskListenerTraitDefinition.SYNC_PROPERTY), Boolean.TRUE);
		}
		if (bpmnId != null) {
			node.setProperty(t.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}
		node.setProperty(t.key(BpmnTaskListenerTraitDefinition.ELEMENT_PROPERTY), elemNode);
	}

	/**
	 * Parse process-level lifecycle listeners declared in {@code <bpmn:extensionElements>}
	 * on the {@code <bpmn:process>} element. Recognises:
	 *
	 * <ul>
	 *   <li>{@code <structr:processListener event="..." method="..." sync="..."/>}
	 *       (canonical, what we emit on export)</li>
	 *   <li>{@code <camunda:executionListener event="start"|"end" class="..." />} or with
	 *       {@code expression}, {@code delegateExpression}, or inline {@code <camunda:script>}.
	 *       Auto-translated on input only. Camunda's payload is preserved verbatim in the
	 *       {@code method} field; users must adapt to a Structr schema-method name.</li>
	 * </ul>
	 *
	 * <p>Camunda event names translate to ours: {@code start -> started},
	 * {@code end -> completed}. Camunda has no native equivalent for
	 * {@code created} / {@code subjectAttached} / {@code terminated} /
	 * {@code suspended} / {@code resumed}; if such an event name is encountered,
	 * it is imported as-is with a warning.</p>
	 */
	private void importProcessListeners(final App app, final NodeInterface procNode, final Element processEl) throws FrameworkException {

		final Element extEl = getFirstChildByLocalName(processEl, "extensionElements");
		if (extEl == null) {
			return;
		}

		final NodeList children = extEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;

			final Element listenerEl = (Element) child;
			final String  ns         = listenerEl.getNamespaceURI();
			final String  localName  = listenerEl.getLocalName();

			// Recognise structr:processListener and camunda:executionListener.
			final boolean isStructr  = STRUCTR_NS.equals(ns)  && "processListener".equals(localName);
			final boolean isCamunda  = CAMUNDA_NS.equals(ns)  && "executionListener".equals(localName);
			if (!isStructr && !isCamunda) continue;

			final String rawEvent = listenerEl.getAttribute("event");
			if (rawEvent == null || rawEvent.isEmpty()) {
				logger.warn("Process listener on process element '{}' has no 'event' attribute; skipped", processEl.getAttribute("id"));
				continue;
			}

			final String eventName  = translateProcessListenerEvent(rawEvent, ns);
			final String methodName = extractListenerMethod(listenerEl, ns);
			if (methodName == null || methodName.isEmpty()) {
				logger.warn("Process listener on process element '{}' (event='{}') has no method/class/expression payload; skipped",
					processEl.getAttribute("id"), rawEvent);
				continue;
			}

			final boolean sync   = "true".equalsIgnoreCase(nullIfEmpty(listenerEl.getAttribute("sync")));
			final String  bpmnId = nullIfEmpty(listenerEl.getAttribute("id"));

			createProcessListenerNode(app, procNode, eventName, methodName, sync, bpmnId);
		}
	}

	private String translateProcessListenerEvent(final String rawEvent, final String ns) {
		if (CAMUNDA_NS.equals(ns)) {
			switch (rawEvent) {
				case "start": return BpmnProcessListenerTraitDefinition.EVENT_STARTED;
				case "end":   return BpmnProcessListenerTraitDefinition.EVENT_COMPLETED;
				// Camunda's executionListener has no equivalent for our broader event set.
				default:
					logger.warn("Camunda execution listener event '{}' has no Structr equivalent; importing as-is", rawEvent);
					return rawEvent;
			}
		}
		// structr:processListener: pass through verbatim (the EnumProperty validates).
		return rawEvent;
	}

	private void createProcessListenerNode(final App app, final NodeInterface procNode,
										   final String event, final String method, final boolean sync,
										   final String bpmnId) throws FrameworkException {

		final NodeInterface node = createBpmnNode(app, ProcessTraits.BPMN_PROCESS_LISTENER);
		final Traits t = node.getTraits();
		node.setProperty(t.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY),  event);
		node.setProperty(t.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY), method);
		if (sync) {
			node.setProperty(t.key(BpmnProcessListenerTraitDefinition.SYNC_PROPERTY), Boolean.TRUE);
		}
		if (bpmnId != null) {
			node.setProperty(t.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}
		node.setProperty(t.key(BpmnProcessListenerTraitDefinition.PROCESS_PROPERTY), procNode);
	}

	// --- methodRef parsing ---

	/**
	 * Parse {@code <structr:methodRef name="..."/>} children of the process-level
	 * {@code <bpmn:extensionElements>} and attach the resolved SchemaMethods to
	 * the BpmnDefinitions via HAS_METHOD. Unresolved names are logged and skipped.
	 */
	private void importProcessMethodRefs(final App app, final NodeInterface procNode, final Element processEl) throws FrameworkException {

		final List<NodeInterface> resolved = collectMethodRefs(app, processEl, processEl.getAttribute("id"));
		if (resolved.isEmpty()) {
			return;
		}
		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY), resolved);
	}

	/**
	 * For each XML element whose {@code id} matches an entry in {@code elementMap},
	 * parse {@code <structr:methodRef>} children of its {@code <bpmn:extensionElements>}
	 * and attach the resolved SchemaMethods to the corresponding BpmnElement node
	 * via HAS_METHOD. Walks the element subtree to handle nested sub-processes.
	 */
	private void importElementMethodRefs(final App app, final Element processEl, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final PropertyKey<Iterable<NodeInterface>> elemMethodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);

		// Iterate all descendant Elements (including nested subProcesses).
		final NodeList all = processEl.getElementsByTagNameNS("*", "*");
		for (int i = 0; i < all.getLength(); i++) {
			final Node n = all.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) continue;
			final Element el = (Element) n;
			final String bpmnId = el.getAttribute("id");
			if (bpmnId == null || bpmnId.isEmpty()) continue;
			final NodeInterface elemNode = elementMap.get(bpmnId);
			if (elemNode == null) continue;

			final List<NodeInterface> resolved = collectMethodRefs(app, el, bpmnId);
			if (!resolved.isEmpty()) {
				elemNode.setProperty(elemMethodsKey, resolved);
			}
		}
	}

	/**
	 * Read {@code <structr:methodRef name="..."/>} children of {@code parent}'s
	 * direct {@code <bpmn:extensionElements>} and resolve each name to a
	 * SchemaMethod. The {@code ownerLabel} is used only in warning messages.
	 */
	private List<NodeInterface> collectMethodRefs(final App app, final Element parent, final String ownerLabel) throws FrameworkException {

		final Element extEl = getFirstChildByLocalName(parent, "extensionElements");
		if (extEl == null) {
			return List.of();
		}
		final List<NodeInterface> resolved = new ArrayList<>();
		final NodeList children = extEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			final Element refEl = (Element) child;
			if (!STRUCTR_NS.equals(refEl.getNamespaceURI())) continue;
			if (!"methodRef".equals(refEl.getLocalName())) continue;

			final String name = nullIfEmpty(refEl.getAttribute("name"));
			if (name == null) {
				logger.warn("methodRef on '{}' has no 'name' attribute -- skipped", ownerLabel);
				continue;
			}
			final NodeInterface method = findSchemaMethodByName(app, name);
			if (method == null) {
				logger.warn("methodRef '{}' on '{}' could not be resolved -- skipped (no matching SchemaMethod in this database)", name, ownerLabel);
				continue;
			}
			resolved.add(method);
		}
		return resolved;
	}

	/**
	 * Look up a SchemaMethod by name. Prefers orphan methods (no schemaNode),
	 * since BPMN-attached methods are conventionally orphans (cloned per
	 * definition version). Falls back to a typed match if no orphan exists,
	 * with a warning. Returns null if no SchemaMethod matches.
	 */
	private NodeInterface findSchemaMethodByName(final App app, final String name) throws FrameworkException {

		final Traits methodTraits = Traits.of(StructrTraits.SCHEMA_METHOD);
		final PropertyKey<String> nameKey               = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<NodeInterface> schemaNodeKey  = methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

		final List<NodeInterface> orphans = new ArrayList<>();
		final List<NodeInterface> typed   = new ArrayList<>();
		for (final NodeInterface m : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(nameKey, name).getResultStream()) {
			if (m.getProperty(schemaNodeKey) == null) {
				orphans.add(m);
			} else {
				typed.add(m);
			}
		}
		if (!orphans.isEmpty()) {
			if (orphans.size() > 1) {
				logger.warn("Multiple orphan SchemaMethods named '{}' found; picking the first", name);
			}
			return orphans.get(0);
		}
		if (!typed.isEmpty()) {
			logger.warn("No orphan SchemaMethod named '{}' found; falling back to a typed match", name);
			return typed.get(0);
		}
		return null;
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

	private int countDirectChildElementsNS(final Element parent, final String namespaceURI, final String localName) {
		if (parent == null) return 0;
		final NodeList children = parent.getChildNodes();
		int count = 0;
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			if (!localName.equals(child.getLocalName())) continue;
			if (namespaceURI != null && !namespaceURI.equals(child.getNamespaceURI())) continue;
			count++;
		}
		return count;
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

	/**
	 * Create a BPMN-sourced node and stamp it with the current import version.
	 * All node-creation sites in the importer go through this so the version
	 * stays consistent across the entire imported sub-graph.
	 */
	private NodeInterface createBpmnNode(final App app, final String type) throws FrameworkException {
		final NodeInterface node = app.create(type, (String) null);
		stampVersion(node);
		return node;
	}

	/**
	 * Apply {@link #currentVersion} to the given node's {@code version} property.
	 * No-op if no version has been computed yet (the BpmnDefinitions itself is
	 * created before we know the processId).
	 */
	private void stampVersion(final NodeInterface node) throws FrameworkException {
		if (currentVersion != null) {
			node.setProperty(node.getTraits().key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY), currentVersion);
		}
	}

	/**
	 * After a re-import, repoint external references that were bound to any old
	 * version of this {@code processId} so they target the new BpmnProcess
	 * and the new BpmnElements (matched by bpmnId via {@code elementMap}).
	 *
	 * <p>Two consumer types are rewired:
	 * <ul>
	 *   <li>{@link org.structr.process.traits.definitions.VisibilityMappingTraitDefinition VisibilityMapping}
	 *       (boundProcess + boundStep + denormalized name backups)</li>
	 *   <li>{@link org.structr.web.traits.definitions.ActionMappingTraitDefinition ActionMapping}
	 *       (controlsProcess + targetsElement + denormalized name backups)</li>
	 * </ul>
	 *
	 * <p>Uses the source-side EndNode property as the query key (Structr's
	 * query layer follows relationship-target keys to filter source nodes).</p>
	 */
	private void rewireExternalReferences(final App app, final String processId, final NodeInterface newDefNode, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		if (processId == null || processId.isEmpty()) return;

		// Collect every BpmnProcess version of this processId. Split into:
		//   * the NEW one (parent == newDefNode): becomes the rewire target
		//     for both ActionMappings and VisibilityMappings (both rels now
		//     point at a specific BpmnProcess after the multi-process refactor).
		//   * the OLD ones (different parent): anchors for the rel-based
		//     rewire pass.
		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> procProcessIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<NodeInterface> procDefKey = procTraits.key(BpmnProcessTraitDefinition.DEFINITION_PROPERTY);
		final List<NodeInterface> oldProcs = new ArrayList<>();
		NodeInterface newProcessNode = null;
		for (final NodeInterface proc : app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(procProcessIdKey, processId).getResultStream()) {
			final NodeInterface def = proc.getProperty(procDefKey);
			if (def != null && def.getUuid().equals(newDefNode.getUuid())) {
				newProcessNode = proc;
				continue;
			}
			oldProcs.add(proc);
		}

		// The rewire runs in two passes so it covers both:
		//   (a) VMs/AMs whose rel still points at an old anchor (typical
		//       re-import path -- old defs/processes still exist).
		//   (b) VMs/AMs whose rel has already been broken because the old
		//       anchor was deleted; they kept their denormalized
		//       boundProcessId / controlsProcessId backup, and we use that
		//       string to identify them and reattach to the new anchor.
		// UUID dedup avoids redoing the same node twice if it appears in both
		// passes.
		final Set<String> processedVms = new HashSet<>();
		final Set<String> processedAms = new HashSet<>();
		rewireVisibilityMappings(app, oldProcs, processId, newProcessNode, elementMap, processedVms);
		rewireActionMappings   (app, oldProcs, processId, newProcessNode, elementMap, processedAms);
	}

	private void rewireVisibilityMappings(final App app, final List<NodeInterface> oldProcs, final String processId, final NodeInterface newProcessNode, final Map<String, NodeInterface> elementMap, final Set<String> processedVms) throws FrameworkException {

		// VisibilityMapping.boundProcess now targets BpmnProcess (post the
		// multi-process refactor). If the re-imported file does not contain
		// a process with this id, there is nothing to repoint to; warn and
		// leave existing rels alone for manual fixup.
		if (newProcessNode == null) {
			logger.warn("rewireVisibilityMappings: no new BpmnProcess found for processId='{}' in the re-imported file; skipping VisibilityMapping rewire for this id.", processId);
			return;
		}

		final Traits vmTraits = Traits.of(ProcessTraits.VISIBILITY_MAPPING);
		final PropertyKey<NodeInterface> boundProcessKey   = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY);
		final PropertyKey<NodeInterface> boundStepKey      = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY);
		final PropertyKey<String>        boundProcessIdKey  = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_ID_PROPERTY);
		final PropertyKey<String>        boundStepBpmnIdKey = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_BPMN_ID_PROPERTY);

		// Pass (a): rel-based -- VMs whose boundProcess still points at any old BpmnProcess.
		for (final NodeInterface oldProc : oldProcs) {
			for (final NodeInterface vm : app.nodeQuery(ProcessTraits.VISIBILITY_MAPPING).key(boundProcessKey, oldProc).getResultStream()) {
				if (processedVms.add(vm.getUuid())) {
					rewireVm(vm, newProcessNode, elementMap, boundProcessKey, boundStepKey, boundProcessIdKey, boundStepBpmnIdKey, processId);
				}
			}
		}

		// Pass (b): backup-string -- VMs whose backup matches our processId
		// regardless of rel state. Catches the "old processes deleted" case.
		for (final NodeInterface vm : app.nodeQuery(ProcessTraits.VISIBILITY_MAPPING).key(boundProcessIdKey, processId).getResultStream()) {
			if (processedVms.add(vm.getUuid())) {
				rewireVm(vm, newProcessNode, elementMap, boundProcessKey, boundStepKey, boundProcessIdKey, boundStepBpmnIdKey, processId);
			}
		}
	}

	private void rewireVm(final NodeInterface vm, final NodeInterface newProcessNode, final Map<String, NodeInterface> elementMap,
						  final PropertyKey<NodeInterface> boundProcessKey, final PropertyKey<NodeInterface> boundStepKey,
						  final PropertyKey<String> boundProcessIdKey, final PropertyKey<String> boundStepBpmnIdKey,
						  final String processId) throws FrameworkException {

		vm.setProperty(boundProcessKey, newProcessNode);
		vm.setProperty(boundProcessIdKey, processId);

		// Determine the step bpmnId from whichever source is available: the
		// existing boundStep rel (most reliable, since the rel still points at
		// the old element with its bpmnId), then fall back to the denormalized
		// backup (used when the old element is gone).
		String stepBpmnId = null;
		final NodeInterface oldStep = vm.getProperty(boundStepKey);
		if (oldStep != null) {
			stepBpmnId = oldStep.getProperty(Traits.of(ProcessTraits.BPMN_ELEMENT).key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		}
		if (stepBpmnId == null) {
			stepBpmnId = vm.getProperty(boundStepBpmnIdKey);
		}

		if (stepBpmnId != null) {
			final NodeInterface newStep = elementMap.get(stepBpmnId);
			vm.setProperty(boundStepKey, newStep);
			vm.setProperty(boundStepBpmnIdKey, stepBpmnId);
			if (newStep == null) {
				logger.warn("VisibilityMapping {} bound step bpmnId='{}' has no match in re-imported definition; boundStep cleared",
					vm.getUuid(), stepBpmnId);
			}
		}
	}

	private void rewireActionMappings(final App app, final List<NodeInterface> oldProcs, final String processId, final NodeInterface newProcessNode, final Map<String, NodeInterface> elementMap, final Set<String> processedAms) throws FrameworkException {

		// ActionMapping.controlsProcess now targets BpmnProcess (post the
		// multi-process refactor). If the re-imported file does not contain
		// a process with this id, there is nothing to repoint to; warn and
		// leave existing rels alone for manual fixup.
		if (newProcessNode == null) {
			logger.warn("rewireActionMappings: no new BpmnProcess found for processId='{}' in the re-imported file; skipping ActionMapping rewire for this id.", processId);
			return;
		}

		final Traits amTraits = Traits.of(StructrTraits.ACTION_MAPPING);
		final PropertyKey<NodeInterface> controlsProcessKey      = amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY);
		final PropertyKey<NodeInterface> targetsElementKey       = amTraits.key(ActionMappingTraitDefinition.TARGETS_ELEMENT_PROPERTY);
		final PropertyKey<String>        controlsProcessIdKey    = amTraits.key(ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY);
		final PropertyKey<String>        targetsElementBpmnIdKey = amTraits.key(ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY);

		// Pass (a): rel-based -- AMs whose controlsProcess still points at any old BpmnProcess.
		for (final NodeInterface oldProc : oldProcs) {
			for (final NodeInterface am : app.nodeQuery(StructrTraits.ACTION_MAPPING).key(controlsProcessKey, oldProc).getResultStream()) {
				if (processedAms.add(am.getUuid())) {
					rewireAm(am, newProcessNode, elementMap, controlsProcessKey, targetsElementKey, controlsProcessIdKey, targetsElementBpmnIdKey, processId);
				}
			}
		}

		// Pass (b): backup-string -- AMs whose controlsProcessId matches our processId
		// regardless of rel state.
		for (final NodeInterface am : app.nodeQuery(StructrTraits.ACTION_MAPPING).key(controlsProcessIdKey, processId).getResultStream()) {
			if (processedAms.add(am.getUuid())) {
				rewireAm(am, newProcessNode, elementMap, controlsProcessKey, targetsElementKey, controlsProcessIdKey, targetsElementBpmnIdKey, processId);
			}
		}
	}

	private void rewireAm(final NodeInterface am, final NodeInterface newProcessNode, final Map<String, NodeInterface> elementMap,
						  final PropertyKey<NodeInterface> controlsProcessKey, final PropertyKey<NodeInterface> targetsElementKey,
						  final PropertyKey<String> controlsProcessIdKey, final PropertyKey<String> targetsElementBpmnIdKey,
						  final String processId) throws FrameworkException {

		am.setProperty(controlsProcessKey, newProcessNode);
		am.setProperty(controlsProcessIdKey, processId);

		String elemBpmnId = null;
		final NodeInterface oldElem = am.getProperty(targetsElementKey);
		if (oldElem != null) {
			elemBpmnId = oldElem.getProperty(Traits.of(ProcessTraits.BPMN_ELEMENT).key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		}
		if (elemBpmnId == null) {
			elemBpmnId = am.getProperty(targetsElementBpmnIdKey);
		}

		if (elemBpmnId != null) {
			final NodeInterface newElem = elementMap.get(elemBpmnId);
			am.setProperty(targetsElementKey, newElem);
			am.setProperty(targetsElementBpmnIdKey, elemBpmnId);
			if (newElem == null) {
				logger.warn("ActionMapping {} targets element bpmnId='{}' has no match in re-imported definition; targetsElement cleared",
					am.getUuid(), elemBpmnId);
			}
		}
	}

	/**
	 * Find the highest-versioned BpmnProcess with the given processId,
	 * excluding the just-created one. Returns null when no previous version exists.
	 */
	private NodeInterface findPreviousProcess(final App app, final String processId, final String excludeUuid) throws FrameworkException {

		if (processId == null || processId.isEmpty()) return null;

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> processIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<String> versionKey   = procTraits.key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY);

		NodeInterface previousProc = null;
		int highest = -1;
		for (final NodeInterface candidate : app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(processIdKey, processId).getResultStream()) {
			if (candidate.getUuid().equals(excludeUuid)) continue;
			final String v = candidate.getProperty(versionKey);
			if (v == null) continue;
			try {
				final int parsed = Integer.parseInt(v);
				if (parsed > highest) {
					highest = parsed;
					previousProc = candidate;
				}
			} catch (NumberFormatException ignore) {
				// non-integer versions don't participate in the auto-increment chain
			}
		}
		return previousProc;
	}

	/**
	 * Deep-copy each SchemaMethod attached to the previous BpmnProcess
	 * (per-process namespace) onto the new process. Old methods stay attached
	 * to the old process.
	 *
	 * <p>v1 scope: clone the scalar properties (name, source, summary, codeType,
	 * httpVerb, isPrivate, isStatic, returnRawResult). Method parameters
	 * (SchemaMethodParameter relationships) are NOT cloned for now -- BPMN-bound
	 * lifecycle methods take {@code this = TaskInstance} and don't need named
	 * parameters in practice. Extend later if a use case requires it.</p>
	 */
	private void cloneProcessMethods(final App app, final NodeInterface newProcNode, final NodeInterface previousProc) throws FrameworkException {

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<Iterable<NodeInterface>> methodsKey = procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY);

		final Iterable<NodeInterface> previousMethods = previousProc.getProperty(methodsKey);
		if (previousMethods == null) return;

		final List<NodeInterface> clonedMethods = new ArrayList<>();
		for (final NodeInterface oldMethod : previousMethods) {
			clonedMethods.add(cloneSchemaMethod(app, oldMethod));
		}
		newProcNode.setProperty(methodsKey, clonedMethods);
	}

	/**
	 * Clone per-element methods from the previous version. Each old element is
	 * matched to a new one by bpmnId (taken from the elementMap built during
	 * importProcessChildren). Old elements that no longer exist in the new
	 * process simply drop their methods; new elements added in this version
	 * have nothing to clone in.
	 */
	private void cloneElementMethods(final App app, final NodeInterface previousProc, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<Iterable<NodeInterface>> elementsKey = procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY);

		final Iterable<NodeInterface> oldElements = previousProc.getProperty(elementsKey);
		if (oldElements == null) return;

		final Traits elemTraits = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final PropertyKey<String> bpmnIdKey                       = elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> elemMethodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);

		for (final NodeInterface oldElem : oldElements) {
			final String oldBpmnId = oldElem.getProperty(bpmnIdKey);
			if (oldBpmnId == null) continue;
			final NodeInterface newElem = elementMap.get(oldBpmnId);
			if (newElem == null) continue;

			final Iterable<NodeInterface> oldElemMethods = oldElem.getProperty(elemMethodsKey);
			if (oldElemMethods == null) continue;

			final List<NodeInterface> clonedMethods = new ArrayList<>();
			for (final NodeInterface oldMethod : oldElemMethods) {
				clonedMethods.add(cloneSchemaMethod(app, oldMethod));
			}
			if (!clonedMethods.isEmpty()) {
				newElem.setProperty(elemMethodsKey, clonedMethods);
			}
		}
	}

	/**
	 * Create a new SchemaMethod node and copy the scalar properties from the
	 * source. Used by both per-process and per-element cloning paths.
	 */
	private NodeInterface cloneSchemaMethod(final App app, final NodeInterface source) throws FrameworkException {
		final NodeInterface cloned = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
		final Traits methodTraits = Traits.of(StructrTraits.SCHEMA_METHOD);
		copyProp(source, cloned, methodTraits, NodeInterfaceTraitDefinition.NAME_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.SOURCE_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.SUMMARY_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.CODE_TYPE_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.IS_STATIC_PROPERTY);
		copyProp(source, cloned, methodTraits, SchemaMethodTraitDefinition.RETURN_RAW_RESULT_PROPERTY);
		return cloned;
	}

	/**
	 * Copy a property value from {@code source} to {@code target} using the
	 * given traits' typed key. No-op when the source value is null.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void copyProp(final NodeInterface source, final NodeInterface target, final Traits traits, final String propertyName) throws FrameworkException {
		final PropertyKey key = traits.key(propertyName);
		final Object value = source.getProperty(key);
		if (value != null) {
			target.setProperty(key, value);
		}
	}

	/**
	 * Compute the next integer-string version for a given BPMN {@code processId}.
	 * Returns {@code "1"} on first import; thereafter, max(existing version) + 1.
	 * Non-integer existing values are ignored (treated as 0) so a manual edit to
	 * a version string can't permanently break the auto-increment.
	 */
	private String computeNextVersion(final App app, final String processId) throws FrameworkException {
		if (processId == null || processId.isEmpty()) {
			return "1";
		}
		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> processIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<String> versionKey   = procTraits.key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY);

		int max = 0;
		for (final NodeInterface existing : app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(processIdKey, processId).getResultStream()) {
			final String v = existing.getProperty(versionKey);
			if (v != null) {
				try {
					final int parsed = Integer.parseInt(v);
					if (parsed > max) max = parsed;
				} catch (NumberFormatException ignore) {
					// ignore non-integer versions; they don't participate in auto-increment
				}
			}
		}
		return String.valueOf(max + 1);
	}

}
