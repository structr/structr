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

import org.apache.commons.lang3.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.process.traits.definitions.*;
import org.structr.process.bpmn.interop.BpmnVendorAdapter;
import org.structr.process.bpmn.interop.BpmnVendorAdapters;
import org.structr.process.bpmn.interop.SubjectTypeSynthesizer;
import org.structr.process.bpmn.interop.VendorTaskForm;
import org.structr.process.engine.ProcessEngine;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.ProcessTraits;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

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
	static final String CAMUNDA_NS         = "http://camunda.org/schema/1.0/bpmn";
	private static final String STRUCTR_NS = "http://structr.org/schema/process/1.0";

	private static final Set<String> KNOWN_ELEMENT_TYPES = BpmnElementType.knownTypeNames();

	/**
	 * Direct children of a {@code <process>}/{@code <subProcess>} that are
	 * structural constructs, NOT flow-node elements, and must therefore never be
	 * imported as a generic {@link org.structr.process.ProcessTraits#BPMN_ELEMENT}.
	 * Some are consumed by dedicated passes ({@code laneSet}/{@code lane}/
	 * {@code flowNodeRef} by importLaneSet; {@code extensionElements} by the
	 * process-listener / methodRef passes); {@code incoming}/{@code outgoing} are
	 * per-node flow-id stubs; the rest ({@code documentation}, {@code ioSpecification},
	 * {@code property}, data/loop specs) carry no flow-node semantics. Without this
	 * guard they would fall into the "unknown element" branch and pollute the graph
	 * (and, being id-less, collide on the empty-string key of elementMap).
	 */
	private static final Set<String> NON_ELEMENT_CHILD_NAMES = Set.of(
		"incoming", "outgoing",
		"laneSet", "lane", "flowNodeRef",
		"extensionElements", "documentation",
		"ioSpecification", "property",
		"dataInputAssociation", "dataOutputAssociation",
		"multiInstanceLoopCharacteristics", "standardLoopCharacteristics"
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

	/** A valid SchemaMethod name: starts with a lowercase letter or underscore, then word chars. */
	/** The pattern the SchemaMethod validator enforces on commit -- not a copy of it, so a
	 * change to the validator cannot silently turn into a 422 during BPMN import. */
	private static final Pattern VALID_METHOD_NAME = Pattern.compile(SchemaMethod.schemaMethodNamePattern);

	// compiled once, these run per imported BPMN element
	private static final Pattern WHITESPACE_RUN     = Pattern.compile("\\s+");
	private static final Pattern EXPRESSION_PREFIX  = Pattern.compile("^\\$\\{");
	private static final Pattern EXPRESSION_SUFFIX  = Pattern.compile("\\}$");
	private static final Pattern NON_IDENTIFIER     = Pattern.compile("[^A-Za-z0-9_]");

	/** An {@code identifier(} token -- used to pull the invoked method name out of a JUEL/JS expression. */

	private final SecurityContext securityContext;
	private final Gson gson;

	// "Type#method" pairs already scaffolded during THIS import. Dedups references that
	// occur in more than one place in the BPMN: a same-transaction nodeQuery can't reliably
	// see a just-created sibling method, and creating a second one trips the "no two methods
	// with the same name" validation.
	private final Set<String> scaffoldedServiceMethods = new HashSet<>();

	/**
	 * Version stamp applied to every node created during the current import.
	 * Auto-incremented per processId in computeNextVersion() once we know it,
	 * then read by createBpmnNode() at every creation site.
	 */
	private String currentVersion;

	/**
	 * processId of the <bpmn:process> currently being imported. Together with
	 * currentVersion it scopes the graph names of that process's handler methods
	 * (see BpmnHandlerNames). Set per process, so a multi-process file qualifies
	 * each process's handlers with its own id.
	 */
	private String currentProcessId;

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
			// Harden against XXE: BPMN never uses a DOCTYPE, so disallow it entirely.
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc            = builder.parse(inputStream);

			return importDocument(doc);

		} catch (FrameworkException fe) {

			// Log the reason (message + error tokens) so it is discoverable in the
			// server log, not only in the client. Validation failures such as a
			// duplicate user-defined function left over from a previous import
			// surface here; fe.toString() includes the error tokens.
			logger.warn("BPMN import failed: {}", fe.toString());

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
			// Harden against XXE: BPMN never uses a DOCTYPE, so disallow it entirely.
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc            = builder.parse(new InputSource(new StringReader(xml)));

			return importDocument(doc);

		} catch (FrameworkException fe) {

			// Log the reason (message + error tokens) so it is discoverable in the
			// server log, not only in the client. Validation failures such as a
			// duplicate user-defined function left over from a previous import
			// surface here; fe.toString() includes the error tokens.
			logger.warn("BPMN import failed: {}", fe.toString());

			throw fe;

		} catch (Exception ex) {

			logger.error("Error importing BPMN XML", ex);
			throw new FrameworkException(422, "Error importing BPMN XML: " + ex.getMessage());
		}
	}

	private NodeInterface importDocument(final Document doc) throws FrameworkException {

		final App app      = StructrApp.getInstance(securityContext);
		final Element root = doc.getDocumentElement();

		// Collect namespace declarations from root element
		final Map<String, String> namespaces = collectNamespaces(root);

		// Create BpmnDefinitions (file root) -- holds metadata + container rels
		// for processes, collaboration and DI diagrams. Per-process state lives
		// on each BpmnProcess child.
		final NodeInterface defNode = app.create(ProcessTraits.BPMN_DEFINITIONS);
		final Traits defTraits      = defNode.getTraits();

		defNode.setProperty(defTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),             root.getAttribute("id"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.TARGET_NAMESPACE_PROPERTY), root.getAttribute("targetNamespace"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_PROPERTY),         root.getAttribute("exporter"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.EXPORTER_VERSION_PROPERTY), root.getAttribute("exporterVersion"));
		defNode.setProperty(defTraits.key(BpmnDefinitionsTraitDefinition.NAMESPACE_DECLARATIONS),    gson.toJson(namespaces));

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

		defNode.setProperty(defTraits.key("name"), StringUtils.isNotEmpty(firstProcessName) ? firstProcessName : firstProcessId);

		// Top-level global definitions (message, signal, error, escalation, ...)
		// live on BpmnDefinitions, not on any individual process.
		final NodeList rootChildren = root.getChildNodes();

		for (int i = 0; i < rootChildren.getLength(); i++) {

			final Node child = rootChildren.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {

				final Element childEl = (Element) child;
				final String ln       = childEl.getLocalName();

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

		// Create each <bpmn:process>: its BpmnProcess node and properties, all child
		// elements and sequence flows, lane sets, listeners and method refs. On
		// re-import (previous version present, found via findPreviousProcess) the
		// previous version's methods are cloned onto the new process first.
		for (int i = 0; i < processNodes.getLength(); i++) {

			final Element processEl    = (Element) processNodes.item(i);
			final String processIdAttr = processEl.getAttribute("id");
			final NodeInterface procNode = createBpmnNode(app, ProcessTraits.BPMN_PROCESS);
			final Traits procTraits = procNode.getTraits();

			procNode.setProperty(procTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),              processIdAttr);
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY),            processIdAttr);
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY),          processEl.getAttribute("name"));
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.PROCESS_IS_EXECUTABLE_PROPERTY), "true".equals(processEl.getAttribute("isExecutable")));
			procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.DEFINITION_PROPERTY),            defNode);

			final String procName = processEl.getAttribute("name");

			procNode.setProperty(procTraits.key("name"), StringUtils.isNotEmpty(procName) ? procName : processIdAttr);

			processMap.put(processIdAttr, procNode);

			// Scope for this process's handler-method graph names (see BpmnHandlerNames).
			this.currentProcessId = processIdAttr;

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

			// Structr-native process/UI contract: <structr:subject type="..."/> at the process and
			// <structr:subjectContract formView=".." writableView=".." instructions=".."/> per user
			// task. This is the canonical round-trip form (BpmnExporter emits it); it references
			// existing schema views by name rather than defining fields. Runs BEFORE vendor-form
			// synthesis so an explicit subject type wins and synthesis skips.
			importStructrContract(app, procNode, processEl, elementMap);

			// Foreign-vendor forms (Camunda, ...): translate any vendor form definitions on this
			// process's user tasks into a synthesized Structr subject type + per-step views, so a
			// BPMN authored for another engine still renders. No-op when the process declares its
			// own subjectType, or when no vendor forms are present. See the interop package
			// (BpmnVendorAdapter) for the design pillars.
			importVendorForms(app, procNode, processEl, elementMap, namespaces);

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

			importCollaboration(app, defNode, (Element) collabs.item(0), processMap, elementMap, participantBpmnIds, messageFlowBpmnIds);
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

			importDiagram(app, defNode, (Element) diagramNodes.item(i), elementMap, flowMap, participantBpmnIds, messageFlowBpmnIds, laneBpmnIds);
		}

		return defNode;
	}

	/**
	 * Create the {@code BpmnCollaboration} node with its participants and
	 * message flows. Participants reference {@code BpmnProcess} via
	 * {@code processRef}; message flows hold source/target {@code bpmnId}
	 * strings and (when resolvable) edges to the actual {@code BpmnElement}s.
	 */
	private void importCollaboration(final App app, final NodeInterface defNode, final Element collabEl, final Map<String, NodeInterface> processMap, final Map<String, NodeInterface> elementMap, final Set<String> participantBpmnIds, final Set<String> messageFlowBpmnIds) throws FrameworkException {

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
			if (n.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			final Element child = (Element) n;
			if (!BPMN_NS.equals(child.getNamespaceURI())) {

				continue;
			}

			final String ln = child.getLocalName();
			if ("participant".equals(ln)) {

				final String partBpmnId      = child.getAttribute("id");
				final NodeInterface partNode = createBpmnNode(app, ProcessTraits.BPMN_PARTICIPANT);

				partNode.setProperty(partTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),                partBpmnId);
				partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.BPMN_NAME_PROPERTY),           child.getAttribute("name"));

				final String pname = child.getAttribute("name");

				partNode.setProperty(partTraits.key("name"), StringUtils.isNotEmpty(pname) ? pname : partBpmnId);
				partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.COLLABORATION_PROPERTY),       collabNode);

				final String processRef = child.getAttribute("processRef");
				if (StringUtils.isNotEmpty(processRef)) {

					final NodeInterface procNode = processMap.get(processRef);
					if (procNode != null) {

						partNode.setProperty(partTraits.key(BpmnParticipantTraitDefinition.PROCESS_PROPERTY), procNode);

					} else {

						logger.warn("Participant '{}' references unknown process '{}'", partBpmnId, processRef);
					}
				}

				if (StringUtils.isNotEmpty(partBpmnId)) {

					participantBpmnIds.add(partBpmnId);
				}

			} else if ("messageFlow".equals(ln)) {

				final String mfBpmnId      = child.getAttribute("id");
				final NodeInterface mfNode = createBpmnNode(app, ProcessTraits.BPMN_MESSAGE_FLOW);

				mfNode.setProperty(mfTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY),               mfBpmnId);
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.BPMN_NAME_PROPERTY),          child.getAttribute("name"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.SOURCE_REF_ID_PROPERTY),      child.getAttribute("sourceRef"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.TARGET_REF_ID_PROPERTY),      child.getAttribute("targetRef"));
				mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.COLLABORATION_PROPERTY),      collabNode);

				final String mfName = child.getAttribute("name");

				mfNode.setProperty(mfTraits.key("name"), StringUtils.isNotEmpty(mfName) ? mfName : mfBpmnId);

				// Resolve source / target if both elements were imported.
				final NodeInterface src = elementMap.get(child.getAttribute("sourceRef"));
				final NodeInterface tgt = elementMap.get(child.getAttribute("targetRef"));

				if (src != null) {

					mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.SOURCE_ELEMENT_PROPERTY), src);
				}

				if (tgt != null) {

					mfNode.setProperty(mfTraits.key(BpmnMessageFlowTraitDefinition.TARGET_ELEMENT_PROPERTY), tgt);
				}

				if (StringUtils.isNotEmpty(mfBpmnId)) {

					messageFlowBpmnIds.add(mfBpmnId);
				}
			}
		}
	}

	/**
	 * Recursively import child elements of a process or sub-process container.
	 */
	private void importProcessChildren(final App app, final NodeInterface procNode, final NodeInterface parentElement, final Element containerEl, final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap) throws FrameworkException {

		final NodeList children = containerEl.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			final Element el       = (Element) child;
			final String localName = el.getLocalName();

			// Dispatch mixes enum-based and string-literal checks on purpose:
			// BpmnElementType models only flow-node / artifact *element types*
			// (the things that become a BpmnElement). Names checked as enum
			// constants / KNOWN_ELEMENT_TYPES below are exactly those types.
			// The bare "sequenceFlow" literal is an edge, routed through flowMap
			// via importSequenceFlow -- there is no enum member for it. Everything
			// not a known element type and not in NON_ELEMENT_CHILD_NAMES (the
			// structural constructs handled elsewhere or intentionally ignored)
			// is imported as a generic BpmnElement.
			if ("sequenceFlow".equals(localName)) {

				final NodeInterface flowNode = importSequenceFlow(app, procNode, parentElement, el);
				flowMap.put(el.getAttribute("id"), flowNode);

			} else if (BpmnElementType.SUB_PROCESS.matches(localName) || BpmnElementType.TRANSACTION.matches(localName) || BpmnElementType.AD_HOC_SUB_PROCESS.matches(localName)) {

				final NodeInterface subProcNode = importElement(app, procNode, parentElement, el, localName);

				elementMap.put(el.getAttribute("id"), subProcNode);
				importProcessChildren(app, procNode, subProcNode, el, elementMap, flowMap);

			} else if (KNOWN_ELEMENT_TYPES.contains(localName)) {

				final NodeInterface elemNode = importElement(app, procNode, parentElement, el, localName);

				elementMap.put(el.getAttribute("id"), elemNode);

			} else if (!NON_ELEMENT_CHILD_NAMES.contains(localName)) {

				// Unknown flow-node element -- import as generic BpmnElement.
				// Structural children (extensionElements, documentation, lane
				// constructs, incoming/outgoing stubs, ...) are excluded via
				// NON_ELEMENT_CHILD_NAMES and handled by their dedicated passes.
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
	private void importLaneSet(final App app, final NodeInterface procNode, final Element processEl, final Map<String, NodeInterface> elementMap, final Set<String> laneBpmnIds) throws FrameworkException {

		// Only DIRECT-child <laneSet>/<lane> elements belong to this process; a
		// laneSet nested inside a <subProcess> must not be flattened up here.
		for (final Element laneSetEl : getChildrenByLocalName(processEl, "laneSet")) {

			for (final Element laneEl : getChildrenByLocalName(laneSetEl, "lane")) {

				final NodeInterface laneNode = createBpmnNode(app, ProcessTraits.BPMN_LANE);
				final Traits laneTraits      = laneNode.getTraits();
				final String laneBpmnId      = laneEl.getAttribute("id");
				final String laneName        = laneEl.getAttribute("name");

				laneNode.setProperty(laneTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), laneBpmnId);
				laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY),  laneName);
				laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.PROCESS_PROPERTY),    procNode);
				laneNode.setProperty(laneTraits.key("name"), (StringUtils.isNotEmpty(laneName)) ? laneName : laneBpmnId);

				if (StringUtils.isNotEmpty(laneBpmnId)) {

					laneBpmnIds.add(laneBpmnId);
				}

				// Resolve <bpmn:flowNodeRef> entries -> BpmnElement nodes.
				final List<NodeInterface> resolved = new LinkedList<>();

				for (final Element ref : getChildrenByLocalName(laneEl, "flowNodeRef")) {

					final String refId = ref.getTextContent();
					if (refId == null) {

						continue;
					}

					final NodeInterface elemNode = elementMap.get(refId.trim());
					if (elemNode != null) {

						resolved.add(elemNode);
					}
				}

				if (!resolved.isEmpty()) {

					laneNode.setProperty(laneTraits.key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY), resolved);
				}
			}
		}
	}

	private void resolveFlowReferences(final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap) throws FrameworkException {

		for (final Map.Entry<String, NodeInterface> entry : flowMap.entrySet()) {

			final NodeInterface flowNode = entry.getValue();
			final Traits flowTraits      = flowNode.getTraits();
			final BpmnSequenceFlow flow  = flowNode.as(BpmnSequenceFlow.class);
			final String srcRef          = flow.getSourceRefId();
			final String tgtRef          = flow.getTargetRefId();

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

			final Traits traits    = elemNode.getTraits();
			final BpmnElement elem = elemNode.as(BpmnElement.class);

			if (!elem.isType(BpmnElementType.BOUNDARY_EVENT)) {

				continue;
			}

			final String json = elem.getBpmnAttributes();
			if (StringUtils.isEmpty(json)) {

				continue;
			}

			Map<String, Object> attrs;

			try {

				attrs = gson.fromJson(json, Map.class);

			} catch (Exception ex) {

				logger.warn("Boundary attachedToRef: malformed bpmnAttributes JSON on {}: {}", elemNode.getUuid(), ex.getMessage());
				continue;
			}

			if (attrs == null) {

				continue;
			}

			final Object refObj = attrs.get("attachedToRef");
			if (!(refObj instanceof String)) {

				continue;
			}

			final String hostBpmnId = ((String) refObj).trim();
			if (hostBpmnId.isEmpty()) {

				continue;
			}

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
	private NodeInterface importElement(final App app, final NodeInterface procNode, final NodeInterface parentElement, final Element el, final String elementType) throws FrameworkException {

		final NodeInterface elemNode = createBpmnNode(app, ProcessTraits.BPMN_ELEMENT);
		final Traits traits          = elemNode.getTraits();

		elemNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		elemNode.setProperty(traits.key(BpmnElementTraitDefinition.BPMN_ELEMENT_TYPE_PROPERTY), elementType);

		final String name = el.getAttribute("name");
		if (StringUtils.isNotEmpty(name)) {

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
		final List<String> consumedAttributeKeys = new LinkedList<>();
		importPerformers(app, elemNode, el, consumedAttributeKeys);

		// Task listeners: parse <bpmn:extensionElements>'s <structr:taskListener>
		// children and auto-translate <camunda:taskListener> on import. Both flavors
		// collapse to BpmnTaskListener nodes; exports always emit the structr form.
		importTaskListeners(app, elemNode, el);

		// Camunda <camunda:inputOutput> variable mappings (input/output parameters).
		importIoMappings(elemNode, el);

		// Store extra attributes as JSON (everything except id, name, and consumed camunda:* keys)
		final Map<String, String> attrs = collectAttributes(el);
		attrs.remove("id");
		attrs.remove("name");

		for (final String consumed : consumedAttributeKeys) {

			attrs.remove(consumed);
		}

		// Camunda service-task implementation -> executable Structr script.
		// A camunda:expression is runnable (transpiled as foreign JS, so
		// execution.getVariable/setVariable map onto $.process). class /
		// delegateExpression / external (type+topic) have no Structr runtime
		// equivalent and are left inert (kept in bpmnAttributes) with a warning.
		final String camExpr = nullIfEmpty(el.getAttributeNS(CAMUNDA_NS, "expression"));
		if (camExpr != null) {

			String body = camExpr.trim();
			if (body.startsWith("${") && body.endsWith("}")) {

				body = body.substring(2, body.length() - 1).trim();
			}

			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY), bpmnSourceComment(procNode, el, elementType) + stripBpmnSourceComment(body));
			attrs.put("scriptFormat", "javascript"); // run via the foreign-JS path at execution time
			scaffoldServiceClasses(app, body); // create service-class stubs for any bean/service calls

		} else {

			final String camClass = nullIfEmpty(el.getAttributeNS(CAMUNDA_NS, "class"));
			final String camDeleg = nullIfEmpty(el.getAttributeNS(CAMUNDA_NS, "delegateExpression"));

			if (camClass != null || camDeleg != null) {

				logger.warn("Element '{}' has a Camunda service-task implementation ({}) with no Structr runtime equivalent; "
					+ "imported but inert -- supply a scriptContent body to execute it.",
					el.getAttribute("id"), camClass != null ? "class=" + camClass : "delegateExpression=" + camDeleg);
			}
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

			final String scriptBody = stripBpmnSourceComment(scriptEl.getTextContent());
			elemNode.setProperty(traits.key(BpmnElementTraitDefinition.SCRIPT_CONTENT_PROPERTY), bpmnSourceComment(procNode, el, elementType) + scriptBody);
			scaffoldServiceClasses(app, scriptBody); // create service-class stubs for any bean/service calls
		}

		// Event definitions (timer, error, message, signal, terminate, etc.)
		for (final String evtDefType : EVENT_DEFINITION_TYPES) {

			final Element evtDef = getFirstChildByLocalName(el, evtDefType);
			if (evtDef != null) {

				// Store the definition type (e.g. "timerEventDefinition")
				elemNode.setProperty(traits.key(BpmnElementTraitDefinition.EVENT_DEF_TYPE_PROPERTY), evtDefType);

				// Store the id attribute
				final String evtDefId = evtDef.getAttribute("id");
				if (StringUtils.isNotEmpty(evtDefId)) {

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
							if (StringUtils.isNotEmpty(xsiType)) {

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
	private NodeInterface importSequenceFlow(final App app, final NodeInterface procNode, final NodeInterface parentElement, final Element el) throws FrameworkException {

		final NodeInterface flowNode = createBpmnNode(app, ProcessTraits.BPMN_SEQUENCE_FLOW);
		final Traits traits = flowNode.getTraits();

		flowNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.SOURCE_REF_ID_PROPERTY), el.getAttribute("sourceRef"));
		flowNode.setProperty(traits.key(BpmnSequenceFlowTraitDefinition.TARGET_REF_ID_PROPERTY), el.getAttribute("targetRef"));

		final String name = el.getAttribute("name");
		if (StringUtils.isNotEmpty(name)) {

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
			if (StringUtils.isNotEmpty(xsiType)) {

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

	// --- DI import methods ---

	private void importDiagram(final App app, final NodeInterface defNode, final Element diagramEl, final Map<String, NodeInterface> elementMap, final Map<String, NodeInterface> flowMap, final Set<String> participantBpmnIds, final Set<String> messageFlowBpmnIds, final Set<String> laneBpmnIds) throws FrameworkException {

		final NodeInterface diagramNode = createBpmnNode(app, ProcessTraits.BPMN_DI_DIAGRAM);
		final Traits diagramTraits      = diagramNode.getTraits();

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
				if (StringUtils.isEmpty(ref) || (!elementMap.containsKey(ref) && !participantBpmnIds.contains(ref) && !laneBpmnIds.contains(ref))) {

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
				if (StringUtils.isEmpty(ref) || (!flowMap.containsKey(ref) && !messageFlowBpmnIds.contains(ref))) {

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

	private void importDiShape(final App app, final NodeInterface diagramNode, final Element shapeEl, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final NodeInterface shapeNode = createBpmnNode(app, ProcessTraits.BPMN_DI_SHAPE);
		final Traits traits           = shapeNode.getTraits();

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

			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_X_PROPERTY), parseDoubleOrNull(b.getAttribute("x")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_Y_PROPERTY), parseDoubleOrNull(b.getAttribute("y")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_WIDTH_PROPERTY), parseDoubleOrNull(b.getAttribute("width")));
			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.BOUNDS_HEIGHT_PROPERTY), parseDoubleOrNull(b.getAttribute("height")));
		}

		final NodeList labels = shapeEl.getElementsByTagNameNS(DI_NS, "BPMNLabel");
		if (labels.getLength() > 0) {

			shapeNode.setProperty(traits.key(BpmnDiShapeTraitDefinition.HAS_LABEL_PROPERTY), true);

			final Element labelEl = (Element) labels.item(0);
			final NodeList labelBounds = labelEl.getElementsByTagNameNS(DC_NS, "Bounds");

			if (labelBounds.getLength() > 0) {

				final Element lb                = (Element) labelBounds.item(0);
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

	private void importDiEdge(final App app, final NodeInterface diagramNode, final Element edgeEl, final Map<String, NodeInterface> flowMap) throws FrameworkException {

		final NodeInterface edgeNode = createBpmnNode(app, ProcessTraits.BPMN_DI_EDGE);
		final Traits traits          = edgeNode.getTraits();

		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.EDGE_ID_PROPERTY), edgeEl.getAttribute("id"));
		edgeNode.setProperty(traits.key(BpmnDiEdgeTraitDefinition.BPMN_ELEMENT_REF_PROPERTY), edgeEl.getAttribute("bpmnElement"));
		edgeNode.setProperty(traits.key("name"), edgeEl.getAttribute("bpmnElement"));

		final NodeList wpNodes                    = edgeEl.getElementsByTagNameNS(OMGDI_NS, "waypoint");
		final List<Map<String, String>> waypoints = new LinkedList<>();

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

			final Element labelEl      = (Element) labels.item(0);
			final NodeList labelBounds = labelEl.getElementsByTagNameNS(DC_NS, "Bounds");

			if (labelBounds.getLength() > 0) {

				final Element lb                = (Element) labelBounds.item(0);
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
	private void importGlobalDefinition(final App app, final NodeInterface defNode, final Element el, final String definitionType) throws FrameworkException {

		final NodeInterface gdNode = createBpmnNode(app, ProcessTraits.BPMN_GLOBAL_DEFINITION);
		final Traits traits        = gdNode.getTraits();

		gdNode.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), el.getAttribute("id"));
		gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.DEFINITION_TYPE_PROPERTY), definitionType);

		final String name = el.getAttribute("name");
		if (StringUtils.isNotEmpty(name)) {

			gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.BPMN_NAME_PROPERTY), name);
			gdNode.setProperty(traits.key("name"), name);

		} else {

			gdNode.setProperty(traits.key("name"), definitionType + " " + el.getAttribute("id"));
		}

		// Error-specific: errorCode
		final String errorCode = el.getAttribute("errorCode");
		if (StringUtils.isNotEmpty(errorCode)) {

			gdNode.setProperty(traits.key(BpmnGlobalDefinitionTraitDefinition.ERROR_CODE_PROPERTY), errorCode);
		}

		// structureRef (used by message and error)
		final String structureRef = el.getAttribute("structureRef");
		if (StringUtils.isNotEmpty(structureRef)) {

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
	private void importPerformers(final App app, final NodeInterface elemNode, final Element el, final List<String> consumedAttributeKeys) throws FrameworkException {

		// Standard BPMN sub-elements (multiple of each kind allowed by spec)
		importPerformerSubElements(app, elemNode, el, "humanPerformer",  BpmnPerformerTraitDefinition.KIND_HUMAN_PERFORMER);
		importPerformerSubElements(app, elemNode, el, "potentialOwner",  BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER);
		importPerformerSubElements(app, elemNode, el, "performer",       BpmnPerformerTraitDefinition.KIND_PERFORMER);

		// Camunda interop on input only (we never emit these on export).
		// camunda:assignee="demo"           -> humanPerformer body "user(demo)"
		// camunda:assignee="${initiator}"   -> humanPerformer body "${initiator}" (expression preserved)
		final String camundaAssignee = el.getAttributeNS(CAMUNDA_NS, "assignee");
		if (StringUtils.isNotEmpty(camundaAssignee)) {

			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_HUMAN_PERFORMER, wrapAsUserIfBare(camundaAssignee), null, null, null);
			consumedAttributeKeys.add("camunda:assignee");
		}

		// camunda:candidateUsers="alice, bob" -> potentialOwner body "user(alice), user(bob)"
		final String candidateUsers = el.getAttributeNS(CAMUNDA_NS, "candidateUsers");
		if (StringUtils.isNotEmpty(candidateUsers)) {

			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER, csvToFunctionExpression(candidateUsers, "user"), null, null, null);
			consumedAttributeKeys.add("camunda:candidateUsers");
		}

		// camunda:candidateGroups="managers, hr" -> potentialOwner body "group(managers), group(hr)"
		final String candidateGroups = el.getAttributeNS(CAMUNDA_NS, "candidateGroups");
		if (StringUtils.isNotEmpty(candidateGroups)) {

			createPerformerNode(app, elemNode, BpmnPerformerTraitDefinition.KIND_POTENTIAL_OWNER, csvToFunctionExpression(candidateGroups, "group"), null, null, null);
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
	private void importPerformerSubElements(final App app, final NodeInterface elemNode, final Element parent, final String localName, final String kind) throws FrameworkException {

		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE || !localName.equals(child.getLocalName())) {

				continue;
			}

			final Element performerEl  = (Element) child;
			final Element rae          = getFirstChildByLocalName(performerEl, "resourceAssignmentExpression");
			final Element formal       = (rae != null) ? getFirstChildByLocalName(rae, "formalExpression") : null;
			final String body          = (formal != null) ? formal.getTextContent().trim() : "";
			final String lang          = (formal != null) ? nullIfEmpty(formal.getAttribute("language")) : null;
			final String performerName = nullIfEmpty(performerEl.getAttribute("name"));
			final String performerId   = nullIfEmpty(performerEl.getAttribute("id"));

			if (body.isEmpty()) {

				// No expression we could extract. Identify likely authoring mistakes.
				final String parentId = parent.getAttribute("id");

				if (rae == null && hasNonWhitespaceText(performerEl)) {

					logger.warn("BPMN <{}> on element '{}' contains text directly inside the element ('{}'); the standard requires it to be wrapped in <resourceAssignmentExpression><formalExpression>...</...></...>. Performer skipped.", localName, parentId, collapseWhitespace(performerEl.getTextContent()));

				} else if (rae != null && formal == null && hasNonWhitespaceText(rae)) {

					logger.warn("BPMN <{}> on element '{}' has a <resourceAssignmentExpression> with text but no nested <formalExpression> ('{}'). Performer skipped.", localName, parentId, collapseWhitespace(rae.getTextContent()));

				} else {

					logger.warn("BPMN <{}> on element '{}' declares no resolvable expression body. Performer skipped.", localName, parentId);
				}

				continue;
			}

			createPerformerNode(app, elemNode, kind, body, lang, performerName, performerId);
		}
	}

	private boolean hasNonWhitespaceText(final Element el) {

		final String t = el.getTextContent();

		return StringUtils.isNotBlank(t);
	}

	private String collapseWhitespace(final String s) {

		if (s == null) {

			return "";
		}

		return WHITESPACE_RUN.matcher(s).replaceAll(" ").trim();
	}

	private void createPerformerNode(final App app, final NodeInterface elemNode, final String kind, final String expression, final String language, final String performerName, final String bpmnId) throws FrameworkException {

		final NodeInterface node = createBpmnNode(app, ProcessTraits.BPMN_PERFORMER);
		final Traits traits      = node.getTraits();

		node.setProperty(traits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY),       kind);
		node.setProperty(traits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY), expression);

		if (language != null) {

			node.setProperty(traits.key(BpmnPerformerTraitDefinition.EXPRESSION_LANGUAGE_PROPERTY), language);
		}

		if (performerName != null) {

			node.setProperty(traits.key(BpmnPerformerTraitDefinition.PERFORMER_NAME_PROPERTY), performerName);
		}

		if (bpmnId != null) {

			node.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}

		node.setProperty(traits.key(BpmnPerformerTraitDefinition.ELEMENT_PROPERTY), elemNode);
	}

	/** Split a CSV on commas that are NOT nested inside {@code (...)} or {@code {...}} (e.g. a ${...} expression). */
	private static List<String> splitTopLevelCommas(final String s) {

		final List<String> parts = new LinkedList<>();
		int depth                = 0;
		int start                = 0;

		for (int i = 0; i < s.length(); i++) {

			final char c = s.charAt(i);
			if (c == '(' || c == '{') {

				depth++;

			} else if (c == ')' || c == '}') {

				if (depth > 0) {

					depth--;
				}

			} else if (c == ',' && depth == 0) {

				parts.add(s.substring(start, i));
				start = i + 1;
			}
		}

		parts.add(s.substring(start));

		return parts;
	}

	/**
	 * Translate a camunda CSV (e.g. "alice, bob") to a Structr expression body
	 * using the given function name, e.g. "user(alice), user(bob)". Entries that
	 * are already expressions ({@code ${...}}) are preserved verbatim.
	 */
	static String csvToFunctionExpression(final String csv, final String fnName) {

		final StringBuilder out = new StringBuilder();
		boolean first           = true;

		// Split on top-level commas only, so a single ${...} expression containing
		// commas (e.g. ${fn(a,b)}) is kept intact rather than torn apart.
		for (final String raw : splitTopLevelCommas(csv)) {

			final String name = raw.trim();
			if (name.isEmpty()) {

				continue;
			}

			if (!first) {

				out.append(", ");
			}

			if (name.startsWith("${")) {

				out.append(name);

			} else {

				out.append(fnName).append('(').append(name).append(')');
			}

			first = false;
		}

		return out.toString();
	}

	private void appendMethods(final NodeInterface node, final PropertyKey<Iterable<NodeInterface>> methodsKey, final List<NodeInterface> toAdd) throws FrameworkException {

		final List<NodeInterface> merged = new LinkedList<>();
		final Set<String> seen           = new LinkedHashSet<>();
		final Iterable<NodeInterface> existing = node.getProperty(methodsKey);

		if (existing != null) {

			for (final NodeInterface m : existing) {

				if (m != null && seen.add(m.getUuid())) {

					merged.add(m);
				}
			}
		}

		for (final NodeInterface m : toAdd) {

			if (m != null && seen.add(m.getUuid())) {

				merged.add(m);
			}
		}

		node.setProperty(methodsKey, merged);
	}

	private Double parseDoubleOrNull(final String s) {

		if (StringUtils.isBlank(s)) {

			return null;
		}

		try {

			return Double.parseDouble(s.trim());

		} catch (final NumberFormatException ex) {

			return null;
		}
	}

	private String nullIfEmpty(final String s) {

		return (StringUtils.isEmpty(s)) ? null : s;
	}

	private static final String BPMN_SRC_HEADER_START = "// ---- generated from BPMN ----";
	private static final String BPMN_SRC_HEADER_END   = "// ------------------------------";

	/**
	 * A provenance header prepended to JavaScript generated from a BPMN element (script tasks and
	 * {@code camunda:expression} service tasks), so the origin of imported code is visible when
	 * editing it: the source process (name + id, and version when set) and the source element.
	 * Deterministic (no timestamp) so re-imports don't churn the body, and delimited so
	 * {@link #stripBpmnSourceComment} can remove a prior header before a fresh one is prepended
	 * (the exporter writes scriptContent -- header included -- back into {@code <bpmn:script>}, so a
	 * re-import would otherwise stack headers). Never throws.
	 */
	private String bpmnSourceComment(final NodeInterface procNode, final Element el, final String elementType) {

		try {

			final Traits pt      = procNode.getTraits();
			final String name    = procNode.getProperty(pt.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY));
			final String procId  = procNode.getProperty(pt.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));
			final Object version = pt.hasKey(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY)
				? procNode.getProperty(pt.key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY)) : null;

			final StringBuilder sb = new StringBuilder();
			sb.append(BPMN_SRC_HEADER_START).append('\n');
			sb.append("// process: ").append(StringUtils.defaultIfBlank(name, procId));

			if (StringUtils.isNotBlank(procId)) {

				sb.append(" (id: ").append(procId).append(')');
			}

			if (version != null) {

				sb.append(", version: ").append(version);
			}

			sb.append('\n');
			sb.append("// element: ").append(nullIfEmpty(el.getAttribute("id"))).append(" (").append(elementType).append(")\n");
			sb.append(BPMN_SRC_HEADER_END).append('\n');

			return sb.toString();

		} catch (final Exception e) {

			return "";
		}
	}

	/**
	 * Remove a previously prepended {@link #bpmnSourceComment} header (the delimited block from
	 * {@value #BPMN_SRC_HEADER_START} to {@value #BPMN_SRC_HEADER_END}) so re-imports don't stack
	 * duplicate headers. Leaves any other content untouched.
	 */
	private String stripBpmnSourceComment(final String body) {

		if (body == null || !body.startsWith(BPMN_SRC_HEADER_START)) {

			return body;
		}

		final int end = body.indexOf(BPMN_SRC_HEADER_END);
		if (end < 0) {

			return body;
		}

		final int nl = body.indexOf('\n', end);

		return nl < 0 ? "" : body.substring(nl + 1);
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
			if (child.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			if (!"taskListener".equals(child.getLocalName())) {

				continue;
			}

			final Element listenerEl = (Element) child;
			final String  ns         = listenerEl.getNamespaceURI();
			final String rawEvent    = listenerEl.getAttribute("event");

			if (StringUtils.isEmpty(rawEvent)) {

				logger.warn("Task listener on element '{}' has no 'event' attribute -- skipped", el.getAttribute("id"));
				continue;
			}

			final String eventName  = translateListenerEvent(rawEvent, ns);
			final String methodName = extractListenerMethod(listenerEl, ns);

			if (StringUtils.isEmpty(methodName)) {

				logger.warn("Task listener on element '{}' (event='{}') has no method/class/expression payload -- skipped", el.getAttribute("id"), rawEvent);
				continue;
			}

			// Phase: 'on' (pre-commit, veto) or 'after' (post-commit, default).
			// Legacy interop: a Camunda/old-structr sync="true" maps to 'on'.
			String phase = nullIfEmpty(listenerEl.getAttribute("phase"));
			if (phase == null) {

				phase = "true".equalsIgnoreCase(nullIfEmpty(listenerEl.getAttribute("sync")))
					? BpmnTaskListenerTraitDefinition.PHASE_ON
					: BpmnTaskListenerTraitDefinition.PHASE_AFTER;
			}

			final String bpmnId = nullIfEmpty(listenerEl.getAttribute("id"));

			createTaskListenerNode(app, elemNode, eventName, methodName, phase, bpmnId);
		}
	}

	static String translateListenerEvent(final String rawEvent, final String ns) {

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
		if (method != null) {

			return method;
		}

		// Camunda fallbacks
		final String clazz = nullIfEmpty(listenerEl.getAttribute("class"));
		if (clazz != null) {

			return clazz;
		}

		final String dExpr = nullIfEmpty(listenerEl.getAttribute("delegateExpression"));
		if (dExpr != null) {

			return dExpr;
		}

		final String expr = nullIfEmpty(listenerEl.getAttribute("expression"));
		if (expr != null) {

			return expr;
		}

		// Inline <camunda:script> child
		final Element scriptEl = getFirstChildByLocalName(listenerEl, "script");
		if (scriptEl != null) {

			final String scriptText = scriptEl.getTextContent();
			if (StringUtils.isNotBlank(scriptText)) {

				return scriptText.trim();
			}
		}

		return null;
	}

	private void createTaskListenerNode(final App app, final NodeInterface elemNode, final String event, final String methodName, final String phase, final String bpmnId) throws FrameworkException {

		// Ensure the handler method exists on the element, then point the
		// listener directly at it (the engine dispatches via this rel, no
		// name resolution).
		final NodeInterface method = ensureElementMethod(app, elemNode, methodName);
		final NodeInterface node   = createBpmnNode(app, ProcessTraits.BPMN_TASK_LISTENER);
		final Traits traits        = node.getTraits();

		node.setProperty(traits.key(BpmnTaskListenerTraitDefinition.EVENT_PROPERTY), event);
		node.setProperty(traits.key(BpmnTaskListenerTraitDefinition.PHASE_PROPERTY), phase);
		node.setProperty(traits.key(BpmnTaskListenerTraitDefinition.METHOD_PROPERTY), method);

		if (bpmnId != null) {

			node.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}

		node.setProperty(traits.key(BpmnTaskListenerTraitDefinition.ELEMENT_PROPERTY), elemNode);
	}

	/**
	 * Scaffold a Structr service class for every Camunda bean/service call in {@code script}
	 * (e.g. {@code notificationService.notifyReviewer(task)}). For each distinct receiver a
	 * service-class SchemaNode ({@code isServiceClass=true}) is created if absent, and for each
	 * referenced method a STATIC stub SchemaMethod is added if absent. Idempotent across
	 * re-imports: existing types/methods are reused and never overwritten, so a body the user
	 * has already implemented survives. The transpiler rewrites the call sites to
	 * {@code $.<Type>.<method>(...)}, so the process runs against these stubs immediately.
	 */
	private void scaffoldServiceClasses(final App app, final String script) throws FrameworkException {

		final Map<String, Map<String, Integer>> services = ProcessEngine.detectServiceCalls(script);
		if (!services.isEmpty()) {

			logger.info("BPMN import: detected service calls {}", services);
		}

		for (final Map.Entry<String, Map<String, Integer>> service : services.entrySet()) {

			final NodeInterface type = ensureServiceClass(app, service.getKey());

			for (final Map.Entry<String, Integer> method : service.getValue().entrySet()) {

				ensureServiceMethod(app, type, service.getKey(), method.getKey(), method.getValue());
			}
		}
	}

	/** Return the service-class SchemaNode named {@code typeName}, creating it (isServiceClass=true) if absent. */
	private NodeInterface ensureServiceClass(final App app, final String typeName) throws FrameworkException {

		final Traits schemaNodeTraits     = Traits.of(StructrTraits.SCHEMA_NODE);
		final PropertyKey<String> nameKey = schemaNodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final NodeInterface existing = app.nodeQuery(StructrTraits.SCHEMA_NODE).key(nameKey, typeName).getFirst();

		if (existing != null) {

			return existing;
		}

		final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE);

		node.setProperty(nameKey, typeName);
		node.setProperty(schemaNodeTraits.key(AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY), true);

		logger.info("BPMN import: scaffolded service class '{}' for an imported service reference", typeName);

		return node;
	}

	/**
	 * Add a stub SchemaMethod {@code methodName} to service class {@code schemaNode} if it
	 * doesn't already exist. Bound via the {@code schemaNode} RELATIONSHIP so the method's
	 * name-uniqueness level is the service class itself. This is essential: the uniqueness
	 * validator (SchemaMethodTraitDefinition) groups methods by their schemaNode and treats
	 * ALL schemaNode==null methods as one "user-defined functions" level. Binding by
	 * staticSchemaNodeName leaves schemaNode==null, which collides with the global listener
	 * function the importer derives from the same payload (e.g. a listener
	 * "${notificationService.notifyReviewer(task)}" yields a global "notifyReviewer"), failing
	 * the whole commit. With the relationship set, the stub sits under its type -- a different
	 * level -- so there is no collision. Service-class methods are forced static by the backend.
	 */
	private void ensureServiceMethod(final App app, final NodeInterface schemaNode, final String typeName, final String methodName, final int argCount) throws FrameworkException {

		final Traits methodTraits                      = Traits.of(StructrTraits.SCHEMA_METHOD);
		final PropertyKey<String> nameKey              = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<NodeInterface> schemaNodeKey = methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);
		final PropertyKey<String> staticNameKey        = methodTraits.key(SchemaMethodTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY);
		final String schemaNodeId                      = schemaNode.getUuid();

		// Within this import: skip a (type, method) pair we've already scaffolded. A
		// same-transaction query can't reliably see a just-created sibling, so this
		// in-memory guard prevents duplicates for a service referenced in more than one place.
		if (!scaffoldedServiceMethods.add(typeName + "#" + methodName)) {

			return;
		}

		// Across imports: skip if a method of this name already belongs to this service class,
		// bound via the schemaNode relationship (this version) OR staticSchemaNodeName (older
		// versions / leftovers). Never overwrite a body the user may have implemented.
		for (final NodeInterface m : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(nameKey, methodName).getResultStream()) {

			final NodeInterface owner = m.getProperty(schemaNodeKey);
			if (owner != null && schemaNodeId.equals(owner.getUuid())) {

				logger.info("BPMN import: service method {}.{} already exists, skipping", typeName, methodName);

				return;
			}

			if (typeName.equalsIgnoreCase(m.getProperty(staticNameKey))) {

				logger.info("BPMN import: service method {}.{} already exists (legacy binding), skipping", typeName, methodName);

				return;
			}
		}

		final String stub = "{\n"
			+ "\t// TODO: implement " + typeName + "." + methodName + " (" + argCount + " arg(s)) -- scaffolded from an imported BPMN service reference.\n"
			+ "\t$.log('Service stub called (not implemented): " + typeName + "." + methodName + "');\n"
			+ "\treturn null;\n"
			+ "}";

		app.create(StructrTraits.SCHEMA_METHOD,
			new NodeAttribute<>(nameKey, methodName),
			new NodeAttribute<>(schemaNodeKey, schemaNode),
			new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), stub)
		);
		logger.info("BPMN import: created service method {}.{}", typeName, methodName);
	}

	/**
	 * Return the SchemaMethod named {@code methodName} attached to {@code elemNode}
	 * (via HAS_METHOD), creating and attaching it if absent. The method is the
	 * handler body a task listener invokes; it lives on the element so it shows
	 * up under that element in the Code module.
	 */
	private NodeInterface ensureElementMethod(final App app, final NodeInterface elemNode, final String methodName) throws FrameworkException {

		final Traits methodTraits                             = Traits.of(StructrTraits.SCHEMA_METHOD);
		final Traits elemTraits                               = elemNode.getTraits();
		final PropertyKey<Iterable<NodeInterface>> methodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);
		final PropertyKey<String> nameKey                     = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		// The incoming value may be a raw Camunda payload (FQCN, ${...} expression,
		// delegateExpression or inline script) that is NOT a valid SchemaMethod name.
		// Derive a valid identifier; when it had to be rewritten, keep the original
		// payload as the method body so it is not lost.
		final String safeName                                 = sanitizeMethodName(methodName);
		scaffoldServiceClasses(app, methodName); // service-class stubs for bean/service calls in listener bodies

		// The graph name is scoped to (process, version, element) so that the same authored
		// handler name may occur in another element, another process or another version of
		// this process without colliding in the global user-function namespace. The authored
		// name is what the BPMN file carries; see BpmnHandlerNames for the full rationale.
		final String elementBpmnId                            = elemNode.getProperty(elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
		final String graphName                                = BpmnHandlerNames.qualify(safeName, currentProcessId, currentVersion, elementBpmnId);
		final List<NodeInterface> existing                    = new LinkedList<>();
		final Iterable<NodeInterface> current                 = elemNode.getProperty(methodsKey);

		if (current != null) {

			for (final NodeInterface m : current) {

				if (graphName.equals(m.getProperty(nameKey))) {

					return m;
				}

				existing.add(m);
			}
		}

		final NodeInterface method = app.create(StructrTraits.SCHEMA_METHOD);

		method.setProperty(nameKey, graphName);

		if (!safeName.equals(methodName)) {

			method.setProperty(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), camundaListenerBody(methodName));
		}

		existing.add(method);
		elemNode.setProperty(methodsKey, existing);

		return method;
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
	/**
	 * Import the Structr-native process/UI contract from {@code structr:} extension elements -- the
	 * canonical round-trip form written by {@link BpmnExporter}. Unlike the vendor adapters this
	 * references existing schema views by name (no synthesis): {@code <structr:subject type="..."/>}
	 * at the process sets {@code subjectType}; {@code <structr:subjectContract formView=".."
	 * writableView=".." instructions=".."/>} on a user task sets that step's contract properties.
	 */
	private void importStructrContract(final App app, final NodeInterface procNode, final Element processEl, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits procTraits = procNode.getTraits();

		// Process-level subject type.
		final Element procExt = getFirstChildByLocalName(processEl, "extensionElements");
		if (procExt != null) {

			final Element subjectEl = firstStructrChild(procExt, "subject");
			if (subjectEl != null) {

				final String type = nullIfEmpty(subjectEl.getAttribute("type"));
				if (type != null) {

					procNode.setProperty(procTraits.key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY), type);
				}
			}
		}

		// Per user-task contract.
		final NodeList userTasks = processEl.getElementsByTagNameNS(BPMN_NS, "userTask");

		for (int i = 0; i < userTasks.getLength(); i++) {

			final Element userTaskEl = (Element) userTasks.item(i);
			final String  taskId     = nullIfEmpty(userTaskEl.getAttribute("id"));

			if (taskId == null) {

				continue;
			}

			final NodeInterface element = elementMap.get(taskId);
			if (element == null) {

				continue;
			}

			final Element ext = getFirstChildByLocalName(userTaskEl, "extensionElements");
			if (ext == null) {

				continue;
			}

			final Element contractEl = firstStructrChild(ext, "subjectContract");
			if (contractEl == null) {

				continue;
			}

			final Traits elemTraits    = element.getTraits();
			final String formView      = nullIfEmpty(contractEl.getAttribute("formView"));
			final String writableView  = nullIfEmpty(contractEl.getAttribute("writableView"));
			final String instructions  = nullIfEmpty(contractEl.getAttribute("instructions"));

			if (formView != null) {

				element.setProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY), formView);
			}

			if (writableView != null) {

				element.setProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_WRITABLE_VIEW_PROPERTY), writableView);
			}

			if (instructions != null) {

				element.setProperty(elemTraits.key(BpmnElementTraitDefinition.INSTRUCTIONS_PROPERTY), instructions);
			}
		}
	}

	/** First direct child of {@code parent} in the Structr namespace with the given local name. */
	private Element firstStructrChild(final Element parent, final String localName) {

		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && STRUCTR_NS.equals(child.getNamespaceURI()) && localName.equals(child.getLocalName())) {

				return (Element) child;
			}
		}

		return null;
	}

	/**
	 * Run every applicable vendor adapter over this process, collect the translated user-task
	 * forms, and hand them to {@link SubjectTypeSynthesizer} to manufacture a subject type + views.
	 * Detection is by declared namespace; a file mixing dialects is handled by several adapters at
	 * once. No graph work happens here -- adapters only read XML; synthesis owns the mutation.
	 */
	private void importVendorForms(final App app, final NodeInterface procNode, final Element processEl,
	                               final Map<String, NodeInterface> elementMap, final Map<String, String> namespaces) throws FrameworkException {

		final Set<String> namespaceUris          = new HashSet<>(namespaces.values());
		final List<BpmnVendorAdapter> adapters   = BpmnVendorAdapters.applicableTo(namespaceUris);

		if (adapters.isEmpty()) {

			return;
		}

		final List<VendorTaskForm> forms = new ArrayList<>();

		for (final BpmnVendorAdapter adapter : adapters) {

			forms.addAll(adapter.extractForms(processEl));
		}

		if (!forms.isEmpty()) {

			SubjectTypeSynthesizer.synthesize(app, procNode, forms, elementMap);
		}
	}

	private void importProcessListeners(final App app, final NodeInterface procNode, final Element processEl) throws FrameworkException {

		final Element extEl = getFirstChildByLocalName(processEl, "extensionElements");
		if (extEl == null) {

			return;
		}

		final NodeList children = extEl.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			final Element listenerEl = (Element) child;
			final String  ns         = listenerEl.getNamespaceURI();
			final String  localName  = listenerEl.getLocalName();

			// Recognise structr:processListener and camunda:executionListener.
			final boolean isStructr  = STRUCTR_NS.equals(ns)  && "processListener".equals(localName);
			final boolean isCamunda  = CAMUNDA_NS.equals(ns)  && "executionListener".equals(localName);

			if (!isStructr && !isCamunda) {

				continue;
			}

			final String rawEvent = listenerEl.getAttribute("event");
			if (StringUtils.isEmpty(rawEvent)) {

				logger.warn("Process listener on process element '{}' has no 'event' attribute; skipped", processEl.getAttribute("id"));
				continue;
			}

			final String eventName  = translateProcessListenerEvent(rawEvent, ns);
			final String methodName = extractListenerMethod(listenerEl, ns);

			if (StringUtils.isEmpty(methodName)) {

				logger.warn("Process listener on process element '{}' (event='{}') has no method/class/expression payload; skipped", processEl.getAttribute("id"), rawEvent);
				continue;
			}

			// Phase: 'on' (pre-commit, veto) or 'after' (post-commit, default).
			// Legacy interop: a Camunda/old-structr sync="true" maps to 'on'.
			String phase = nullIfEmpty(listenerEl.getAttribute("phase"));
			if (phase == null) {

				phase = "true".equalsIgnoreCase(nullIfEmpty(listenerEl.getAttribute("sync")))
					? BpmnProcessListenerTraitDefinition.PHASE_ON
					: BpmnProcessListenerTraitDefinition.PHASE_AFTER;
			}

			final String bpmnId = nullIfEmpty(listenerEl.getAttribute("id"));

			createProcessListenerNode(app, procNode, eventName, methodName, phase, bpmnId);
		}
	}

	static String translateProcessListenerEvent(final String rawEvent, final String ns) {

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

	private void createProcessListenerNode(final App app, final NodeInterface procNode, final String event, final String methodName, final String phase, final String bpmnId) throws FrameworkException {

		// Ensure the handler method exists on the process, then point the
		// listener directly at it (the engine dispatches via this rel).
		final NodeInterface method = ensureProcessMethod(app, procNode, methodName);
		final NodeInterface node   = createBpmnNode(app, ProcessTraits.BPMN_PROCESS_LISTENER);
		final Traits traits        = node.getTraits();

		node.setProperty(traits.key(BpmnProcessListenerTraitDefinition.EVENT_PROPERTY),  event);
		node.setProperty(traits.key(BpmnProcessListenerTraitDefinition.PHASE_PROPERTY),  phase);
		node.setProperty(traits.key(BpmnProcessListenerTraitDefinition.METHOD_PROPERTY), method);

		if (bpmnId != null) {

			node.setProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY), bpmnId);
		}

		node.setProperty(traits.key(BpmnProcessListenerTraitDefinition.PROCESS_PROPERTY), procNode);
	}

	/**
	 * Coerce a listener's method payload into a valid {@code SchemaMethod} name
	 * ({@code [a-z_][a-zA-Z0-9_]*}).
	 *
	 * <p>A {@code structr:taskListener method="notify"} value is already a valid
	 * identifier and passes through unchanged. A {@code camunda:*} listener instead
	 * carries a fully-qualified class name, a {@code ${...}} expression, a
	 * {@code delegateExpression}, or inline script -- none of which are valid method
	 * names. From those we derive a sensible identifier: the last method invoked at the
	 * OUTER level of an expression (e.g. {@code ${svc.notifyReviewer(t)}} ->
	 * {@code notifyReviewer}), otherwise the last dotted segment of a class / bean
	 * reference ({@code com.acme.NotifyDelegate} -> {@code notifyDelegate}), stripped to
	 * identifier characters and given a valid first character. Package-private and
	 * static for unit testing.</p>
	 *
	 * <p>"Outer level" matters as soon as a call appears in an argument:
	 * {@code ${execution.setVariable('startedAt', now())}} describes a setVariable call,
	 * so the name is {@code setVariable}, not the nested {@code now} -- which would also
	 * have shadowed the builtin {@code now()} function, since handler methods have no
	 * schemaNode and therefore live in the user-defined function namespace. Chained calls
	 * stay at the outer level, so {@code ${svc.get().doThing()}} still yields
	 * {@code doThing}.</p>
	 */
	static String sanitizeMethodName(final String raw) {

		if (raw == null) {

			return null;
		}

		final String s = raw.trim();
		if (s.isEmpty()) {

			return null;
		}

		// Common case: already a valid Structr method name (structr:taskListener method="...").
		if (VALID_METHOD_NAME.matcher(s).matches()) {

			return s;
		}

		// Prefer the LAST invoked method name at the outer level of an expression /
		// script; a call nested in an argument list describes the argument, not the
		// action. lastCallName falls back to the last call at any depth when the input
		// is unbalanced enough to have no depth-0 call at all.
		String candidate = lastCallName(s);

		// Otherwise the last dotted segment of a FQCN / bean reference (minus ${ }).
		if (candidate == null) {

			String ref = EXPRESSION_SUFFIX.matcher(EXPRESSION_PREFIX.matcher(s).replaceAll("")).replaceAll("").trim();
			final int dot = ref.lastIndexOf('.');

			if (dot >= 0 && dot < ref.length() - 1) {

				ref = ref.substring(dot + 1);
			}

			candidate = ref;
		}

		// Keep only identifier characters.
		candidate = NON_IDENTIFIER.matcher(candidate).replaceAll("");

		if (candidate.isEmpty()) {

			candidate = "listener";
		}

		// Ensure a valid first character (lowercase letter or underscore).
		final char c0 = candidate.charAt(0);
		if (c0 >= 'A' && c0 <= 'Z') {

			candidate = Character.toLowerCase(c0) + candidate.substring(1);

		} else if (!((c0 >= 'a' && c0 <= 'z') || c0 == '_')) {

			candidate = "_" + candidate;
		}

		return candidate;
	}

	/**
	 * Return the name of the last method call at paren depth 0 in {@code s}, or null when
	 * there is none (a bean reference, or a grouping paren with no callee). Parentheses
	 * inside single- or double-quoted literals don't count, so an argument such as
	 * {@code 'a(b'} cannot skew the nesting level.
	 */
	private static String lastCallName(final String s) {

		String outer  = null;
		String anyDepth = null;
		int depth     = 0;
		char quote    = 0;

		for (int i = 0; i < s.length(); i++) {

			final char c = s.charAt(i);

			if (quote != 0) {

				if (c == '\\') {

					i++;

				} else if (c == quote) {

					quote = 0;
				}

				continue;
			}

			if (c == '\'' || c == '"') {

				quote = c;

			} else if (c == '(') {

				final String name = identifierEndingAt(s, i);
				if (name != null) {

					anyDepth = name;

					if (depth == 0) {

						outer = name;
					}
				}

				depth++;

			} else if (c == ')' && depth > 0) {

				depth--;
			}
		}

		return outer != null ? outer : anyDepth;
	}

	/**
	 * Return the identifier ending at {@code parenIndex} (ignoring whitespace before the
	 * paren), or null if what precedes the paren isn't an identifier.
	 */
	private static String identifierEndingAt(final String s, final int parenIndex) {

		int end = parenIndex;

		while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {

			end--;
		}

		int start = end;

		while (start > 0) {

			final char c = s.charAt(start - 1);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {

				start--;

			} else {

				break;
			}
		}

		if (start == end) {

			return null;
		}

		// the loop above accepted [A-Za-z0-9_], so a digit is the only way the run can
		// fail to be an identifier
		if (Character.isDigit(s.charAt(start))) {

			return null;
		}

		return s.substring(start, end);
	}

	/**
	 * Turn a Camunda listener payload into a runnable Structr method body.
	 *
	 * <p>An expression / inline script is stripped of its {@code ${ }} wrapper,
	 * transpiled ({@code execution.getVariable/setVariable} -> {@code $.process}) and
	 * wrapped as a JavaScript method body ({@code { ... }}) so it runs as JS with
	 * {@code $.process} available (the engine installs the process context when a
	 * listener fires). A bare class / delegate-bean reference has no Structr runtime
	 * equivalent, so it becomes an inert JS body carrying the original as a comment
	 * for manual porting. (Camunda built-ins like {@code now()} and beans still need
	 * porting -- this only fixes the syntax/engine and the execution.* mapping.)</p>
	 */
	static String camundaListenerBody(final String payload) {

		String inner = payload.trim();
		if (inner.startsWith("${") && inner.endsWith("}")) {

			inner = inner.substring(2, inner.length() - 1).trim();
		}

		// Executable when it references execution.* or is a call; otherwise it's a
		// class/bean reference we can't run.
		final boolean executable = inner.contains("execution.") || inner.contains("(");
		if (executable) {

			return "{\n" + ProcessEngine.transpileForeignScript(inner) + "\n}";
		}

		final String safeComment = payload.replace("*/", "* /");

		return "{ /* imported from Camunda listener: " + safeComment + " -- no Structr equivalent, port manually */ }";
	}

	private NodeInterface ensureProcessMethod(final App app, final NodeInterface procNode, final String methodName) throws FrameworkException {

		final Traits methodTraits                             = Traits.of(StructrTraits.SCHEMA_METHOD);
		final Traits procTraits                               = procNode.getTraits();
		final PropertyKey<Iterable<NodeInterface>> methodsKey = procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY);
		final PropertyKey<String> nameKey                     = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		// See ensureElementMethod: coerce a raw Camunda payload into a valid method
		// name and keep the original as the body when it had to be rewritten.
		final String safeName                                 = sanitizeMethodName(methodName);
		scaffoldServiceClasses(app, methodName); // service-class stubs for bean/service calls in listener bodies

		// Scoped to (process, version) -- no element component for a process-level handler.
		final String graphName                                = BpmnHandlerNames.qualify(safeName, currentProcessId, currentVersion, null);
		final List<NodeInterface> existing                    = new LinkedList<>();
		final Iterable<NodeInterface> current                 = procNode.getProperty(methodsKey);

		if (current != null) {

			for (final NodeInterface m : current) {

				if (graphName.equals(m.getProperty(nameKey))) {

					return m;
				}

				existing.add(m);
			}
		}

		final NodeInterface method = app.create(StructrTraits.SCHEMA_METHOD);

		method.setProperty(nameKey, graphName);

		if (!safeName.equals(methodName)) {

			method.setProperty(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), camundaListenerBody(methodName));
		}

		existing.add(method);
		procNode.setProperty(methodsKey, existing);

		return method;
	}

	// --- methodRef parsing ---

	/**
	 * Parse {@code <structr:methodRef name="..."/>} children of the process-level
	 * {@code <bpmn:extensionElements>} and attach the resolved SchemaMethods to
	 * the BpmnDefinitions via HAS_METHOD. Unresolved names are logged and skipped.
	 */
	private void importProcessMethodRefs(final App app, final NodeInterface procNode, final Element processEl) throws FrameworkException {

		final List<NodeInterface> resolved = collectMethodRefs(app, processEl, procNode, processEl.getAttribute("id"));
		if (resolved.isEmpty()) {

			return;
		}

		final Traits procTraits = Traits.of(ProcessTraits.BPMN_PROCESS);

		appendMethods(procNode, procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY), resolved);
	}

	/**
	 * For each XML element whose {@code id} matches an entry in {@code elementMap},
	 * parse {@code <structr:methodRef>} children of its {@code <bpmn:extensionElements>}
	 * and attach the resolved SchemaMethods to the corresponding BpmnElement node
	 * via HAS_METHOD. Walks the element subtree to handle nested sub-processes.
	 */
	private void importElementMethodRefs(final App app, final Element processEl, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits elemTraits                                   = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final PropertyKey<Iterable<NodeInterface>> elemMethodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);

		// Iterate all descendant Elements (including nested subProcesses).
		final NodeList all = processEl.getElementsByTagNameNS("*", "*");

		for (int i = 0; i < all.getLength(); i++) {

			final Node n = all.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			final Element el = (Element) n;
			final String bpmnId = el.getAttribute("id");

			if (StringUtils.isEmpty(bpmnId)) {

				continue;
			}

			final NodeInterface elemNode = elementMap.get(bpmnId);
			if (elemNode == null) {

				continue;
			}

			final List<NodeInterface> resolved = collectMethodRefs(app, el, elemNode, bpmnId);
			if (!resolved.isEmpty()) {

				appendMethods(elemNode, elemMethodsKey, resolved);
			}
		}
	}

	/**
	 * Read {@code <structr:methodRef name="..."/>} children of {@code parent}'s
	 * direct {@code <bpmn:extensionElements>} and resolve each name to a
	 * SchemaMethod. {@code owner} is the BpmnProcess / BpmnElement the resolved
	 * methods will be attached to (see {@link #resolveMethodRef}); the
	 * {@code ownerLabel} is used only in warning messages.
	 */
	private List<NodeInterface> collectMethodRefs(final App app, final Element parent, final NodeInterface owner, final String ownerLabel) throws FrameworkException {

		final Element extEl = getFirstChildByLocalName(parent, "extensionElements");
		if (extEl == null) {

			return List.of();
		}

		final List<NodeInterface> resolved = new LinkedList<>();
		final NodeList children = extEl.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {

				continue;
			}

			final Element refEl = (Element) child;
			if (!STRUCTR_NS.equals(refEl.getNamespaceURI())) {

				continue;
			}

			if (!"methodRef".equals(refEl.getLocalName())) {

				continue;
			}

			final String name = nullIfEmpty(refEl.getAttribute("name"));
			if (name == null) {

				logger.warn("methodRef on '{}' has no 'name' attribute -- skipped", ownerLabel);
				continue;
			}

			final NodeInterface method = resolveMethodRef(app, name, owner, ownerLabel);
			if (method == null) {

				continue;
			}

			resolved.add(method);
		}

		return resolved;
	}

	/**
	 * Resolve a {@code <structr:methodRef name="..."/>} to exactly one SchemaMethod, or to
	 * nothing at all. The reference carries only a name (see BpmnExporter#exportMethodRef),
	 * so resolution has to be strict: HAS_BPMN_METHOD is One-to-Many, so attaching a method that
	 * already belongs to another process/element would silently STEAL it -- ensureCardinality
	 * drops the previous owner's relationship. The rules are therefore:
	 *
	 * <ul>
	 * <li>only methods without a {@code schemaNode} are candidates -- a method owned by a
	 *     type is class code, never a process handler;</li>
	 * <li>a candidate already attached to a different node in the same slot ({@code bpmnProcess}
	 *     for a process owner, {@code bpmnElement} for an element owner) is rejected, so an
	 *     import can never take a method away from an existing process;</li>
	 * <li>the match must be unique -- an ambiguous name resolves to nothing rather than to an
	 *     arbitrary "first" hit.</li>
	 * </ul>
	 *
	 * Anything unresolved is logged and skipped. The {@code methodRef} stays in the XML, so a
	 * re-import after creating (or deployment-importing) the intended method links it up. Note
	 * that re-importing a process that already exists doesn't take this path at all: methods
	 * are cloned from the previous version by processId, which is unambiguous.
	 */
	private NodeInterface resolveMethodRef(final App app, final String name, final NodeInterface owner, final String ownerLabel) throws FrameworkException {

		final Traits methodTraits                      = Traits.of(StructrTraits.SCHEMA_METHOD);
		final PropertyKey<String> nameKey              = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<NodeInterface> schemaNodeKey = methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);
		final String slotKey                           = owner != null && owner.is(ProcessTraits.BPMN_PROCESS) ? "bpmnProcess" : "bpmnElement";
		final PropertyKey<NodeInterface> slotKeyProp   = methodTraits.hasKey(slotKey) ? methodTraits.key(slotKey) : null;
		final String ownerId                           = owner != null ? owner.getUuid() : null;
		final List<NodeInterface> candidates           = new ArrayList<>();
		int rejectedAsTyped                            = 0;
		int rejectedAsForeign                          = 0;

		for (final NodeInterface m : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(nameKey, name).getResultStream()) {

			if (m.getProperty(schemaNodeKey) != null) {

				rejectedAsTyped++;
				continue;
			}

			final NodeInterface currentOwner = slotKeyProp != null ? m.getProperty(slotKeyProp) : null;
			if (currentOwner != null && !currentOwner.getUuid().equals(ownerId)) {

				rejectedAsForeign++;
				continue;
			}

			candidates.add(m);
		}

		if (candidates.size() == 1) {

			return candidates.get(0);
		}

		if (candidates.size() > 1) {

			logger.warn("methodRef '{}' on '{}' is ambiguous -- {} unattached SchemaMethods share that name, skipped (attach the intended one manually)", name, ownerLabel, candidates.size());

		} else if (rejectedAsTyped > 0 || rejectedAsForeign > 0) {

			logger.warn("methodRef '{}' on '{}' could not be resolved -- skipped ({} method(s) of that name belong to a type, {} to another process element)", name, ownerLabel, rejectedAsTyped, rejectedAsForeign);

		} else {

			logger.warn("methodRef '{}' on '{}' could not be resolved -- skipped (no SchemaMethod of that name exists in this database)", name, ownerLabel);
		}

		return null;
	}

	// --- Helper methods ---

	/**
	 * Parse a {@code <camunda:inputOutput>} block (inside {@code <extensionElements>})
	 * into the element's {@code ioMappings} JSON. Each {@code <camunda:inputParameter>}
	 * / {@code <camunda:outputParameter name="x">source</...>} becomes an entry; the
	 * source is the parameter's text (a literal or a {@code ${...}} expression). The
	 * engine applies inputs before, and outputs after, automatic-task execution.
	 */
	private void importIoMappings(final NodeInterface elemNode, final Element el) throws FrameworkException {

		final Element ext = getFirstChildByLocalName(el, "extensionElements");
		if (ext == null) {

			return;
		}

		final Element io = getFirstChildByLocalName(ext, "inputOutput");
		if (io == null) {

			return;
		}

		final List<Map<String, String>> inputs  = collectIoParams(io, "inputParameter");
		final List<Map<String, String>> outputs = collectIoParams(io, "outputParameter");

		if (inputs.isEmpty() && outputs.isEmpty()) {

			return;
		}

		final Map<String, Object> mappings = new LinkedHashMap<>();
		mappings.put("inputs", inputs);
		mappings.put("outputs", outputs);

		elemNode.setProperty(elemNode.getTraits().key(BpmnElementTraitDefinition.IO_MAPPINGS_PROPERTY), gson.toJson(mappings));
	}

	private List<Map<String, String>> collectIoParams(final Element io, final String localName) {

		final List<Map<String, String>> out = new LinkedList<>();

		for (final Element p : getChildrenByLocalName(io, localName)) {

			final String name = p.getAttribute("name");
			if (StringUtils.isEmpty(name)) {

				continue;
			}

			// Simple text/expression source. Nested complex sources (camunda:map /
			// list / script) collapse to their text content (best effort).
			final String source = p.getTextContent() != null ? p.getTextContent().trim() : "";
			final Map<String, String> entry = new LinkedHashMap<>();

			entry.put("name", name);
			entry.put("source", source);
			out.add(entry);
		}

		return out;
	}

	private List<Element> getChildrenByLocalName(final Element parent, final String localName) {

		final List<Element> result = new LinkedList<>();
		final NodeList children    = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {

				result.add((Element) child);
			}
		}

		return result;
	}

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
			if (StringUtils.isNotEmpty(val)) {

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
			if (!"xmlns".equals(attr.getPrefix()) && !"xmlns".equals(attr.getName()) && !CAMUNDA_NS.equals(attr.getNamespaceURI())) {

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

		final NodeInterface node = app.create(type);

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

		if (StringUtils.isEmpty(processId)) {

			return;
		}

		// Collect every BpmnProcess version of this processId. Split into:
		//   * the NEW one (parent == newDefNode): becomes the rewire target
		//     for both ActionMappings and VisibilityMappings (both rels now
		//     point at a specific BpmnProcess after the multi-process refactor).
		//   * the OLD ones (different parent): anchors for the rel-based
		//     rewire pass.
		final Traits procTraits                    = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> procProcessIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<NodeInterface> procDefKey = procTraits.key(BpmnProcessTraitDefinition.DEFINITION_PROPERTY);
		final List<NodeInterface> oldProcs          = new LinkedList<>();
		NodeInterface newProcessNode                = null;

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

		final Traits vmTraits                               = Traits.of(ProcessTraits.VISIBILITY_MAPPING);
		final PropertyKey<NodeInterface> boundProcessKey    = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_PROCESS_PROPERTY);
		final PropertyKey<NodeInterface> boundStepKey       = vmTraits.key(VisibilityMappingTraitDefinition.BOUND_STEP_PROPERTY);
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
		String stepBpmnId           = null;
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

				logger.warn("VisibilityMapping {} bound step bpmnId='{}' has no match in re-imported definition; boundStep cleared", vm.getUuid(), stepBpmnId);
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

		final Traits amTraits                                    = Traits.of(StructrTraits.ACTION_MAPPING);
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

	private void rewireAm(final NodeInterface am, final NodeInterface newProcessNode, final Map<String, NodeInterface> elementMap, final PropertyKey<NodeInterface> controlsProcessKey, final PropertyKey<NodeInterface> targetsElementKey, final PropertyKey<String> controlsProcessIdKey, final PropertyKey<String> targetsElementBpmnIdKey, final String processId) throws FrameworkException {

		am.setProperty(controlsProcessKey, newProcessNode);
		am.setProperty(controlsProcessIdKey, processId);

		final NodeInterface oldElem = am.getProperty(targetsElementKey);
		String elemBpmnId           = null;

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

				logger.warn("ActionMapping {} targets element bpmnId='{}' has no match in re-imported definition; targetsElement cleared", am.getUuid(), elemBpmnId);
			}
		}
	}

	/**
	 * Find the highest-versioned BpmnProcess with the given processId,
	 * excluding the just-created one. Returns null when no previous version exists.
	 */
	private NodeInterface findPreviousProcess(final App app, final String processId, final String excludeUuid) throws FrameworkException {

		if (StringUtils.isEmpty(processId)) {

			return null;
		}

		final Traits procTraits                = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> processIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<String> versionKey   = procTraits.key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY);
		NodeInterface previousProc = null;
		int highest = -1;

		for (final NodeInterface candidate : app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(processIdKey, processId).getResultStream()) {

			if (candidate.getUuid().equals(excludeUuid)) {

				continue;
			}

			final String v = candidate.getProperty(versionKey);
			if (v == null) {

				continue;
			}

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
	 * <p>v1 scope: clone the scalar properties (name, source, summary, description,
	 * codeType, httpVerb, isPrivate, isStatic, returnRawResult). Method parameters
	 * (SchemaMethodParameter relationships) are NOT cloned for now -- BPMN-bound
	 * lifecycle methods take {@code this = TaskInstance} and don't need named
	 * parameters in practice. Extend later if a use case requires it.</p>
	 */
	private void cloneProcessMethods(final App app, final NodeInterface newProcNode, final NodeInterface previousProc) throws FrameworkException {

		final Traits procTraits                               = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<Iterable<NodeInterface>> methodsKey = procTraits.key(BpmnProcessTraitDefinition.METHODS_PROPERTY);
		final Iterable<NodeInterface> previousMethods         = previousProc.getProperty(methodsKey);

		if (previousMethods == null) {

			return;
		}

		// Each clone is renamed into the NEW version's scope: the old and new names differ
		// only by their version component, so the previous version keeps its own methods and
		// nothing collides in the global user-function namespace (see BpmnHandlerNames).
		// The process listeners are wired afterwards and will find these clones by name.
		final PropertyKey<String> methodNameKey = Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final List<NodeInterface> clonedMethods = new LinkedList<>();

		for (final NodeInterface oldMethod : previousMethods) {

			final String authored = BpmnHandlerNames.authoredOf(oldMethod.getProperty(methodNameKey));
			final String newName  = BpmnHandlerNames.qualify(authored, currentProcessId, currentVersion, null);

			clonedMethods.add(cloneSchemaMethod(app, oldMethod, newName));
		}

		appendMethods(newProcNode, methodsKey, clonedMethods);
	}

	/**
	 * Clone per-element methods from the previous version. Each old element is
	 * matched to a new one by bpmnId (taken from the elementMap built during
	 * importProcessChildren). Old elements that no longer exist in the new
	 * process simply drop their methods; new elements added in this version
	 * have nothing to clone in.
	 */
	private void cloneElementMethods(final App app, final NodeInterface previousProc, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits procTraits                                = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<Iterable<NodeInterface>> elementsKey = procTraits.key(BpmnProcessTraitDefinition.ELEMENTS_PROPERTY);
		final Iterable<NodeInterface> oldElements              = previousProc.getProperty(elementsKey);

		if (oldElements == null) {

			return;
		}

		final Traits elemTraits                                   = Traits.of(ProcessTraits.BPMN_ELEMENT);
		final Traits methodTraits                                 = Traits.of(StructrTraits.SCHEMA_METHOD);
		final PropertyKey<String> bpmnIdKey                       = elemTraits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY);
		final PropertyKey<Iterable<NodeInterface>> elemMethodsKey = elemTraits.key(BpmnElementTraitDefinition.METHODS_PROPERTY);
		final PropertyKey<String> methodNameKey                   = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		for (final NodeInterface oldElem : oldElements) {

			final String oldBpmnId = oldElem.getProperty(bpmnIdKey);
			if (oldBpmnId == null) {

				continue;
			}

			final NodeInterface newElem = elementMap.get(oldBpmnId);
			if (newElem == null) {

				continue;
			}

			final Iterable<NodeInterface> oldElemMethods = oldElem.getProperty(elemMethodsKey);
			if (oldElemMethods == null) {

				continue;
			}

			// The element's task listeners were already wired (during importElement)
			// to freshly-created, still-empty handler methods on newElem. For each
			// previous-version method, restore its body INTO that same-named stub so
			// the listener binding is preserved; only genuinely new methods are
			// cloned in as new nodes. Appending a fresh clone instead would leave the
			// listener pointing at the empty stub and accumulate duplicate methods on
			// every re-import.
			// Match old to new by AUTHORED name: the graph names differ by version scope, so
			// comparing them literally would never match and would clone a duplicate on every
			// re-import (see BpmnHandlerNames).
			final List<NodeInterface> clonedMethods = new LinkedList<>();

			for (final NodeInterface oldMethod : oldElemMethods) {

				final String authored        = BpmnHandlerNames.authoredOf(oldMethod.getProperty(methodNameKey));
				final String newName         = BpmnHandlerNames.qualify(authored, currentProcessId, currentVersion, oldBpmnId);
				final NodeInterface existing = (newName != null) ? findMethodByName(newElem, elemMethodsKey, methodNameKey, newName) : null;

				if (existing != null) {

					copyMethodScalars(oldMethod, existing, methodTraits);

				} else {

					clonedMethods.add(cloneSchemaMethod(app, oldMethod, newName));
				}
			}

			if (!clonedMethods.isEmpty()) {

				appendMethods(newElem, elemMethodsKey, clonedMethods);
			}
		}
	}

	/** The SchemaMethod named {@code name} already attached to {@code element}, or null. */
	private NodeInterface findMethodByName(final NodeInterface element, final PropertyKey<Iterable<NodeInterface>> methodsKey,
										   final PropertyKey<String> nameKey, final String name) throws FrameworkException {

		final Iterable<NodeInterface> methods = element.getProperty(methodsKey);
		if (methods != null) {

			for (final NodeInterface m : methods) {

				if (name.equals(m.getProperty(nameKey))) {

					return m;
				}
			}
		}

		return null;
	}

	/**
	 * Create a new SchemaMethod node and copy the scalar properties from the
	 * source. Used by both per-process and per-element cloning paths.
	 */
	/**
	 * Clone a SchemaMethod under an explicitly given name. The name is never copied from the
	 * source: a handler's graph name is scoped to its process version (see BpmnHandlerNames),
	 * so a clone made for a new version must be renamed into that version's scope, and the
	 * caller is the only one that knows the target scope.
	 */
	private NodeInterface cloneSchemaMethod(final App app, final NodeInterface source, final String name) throws FrameworkException {

		final Traits methodTraits  = Traits.of(StructrTraits.SCHEMA_METHOD);
		final NodeInterface cloned = app.create(StructrTraits.SCHEMA_METHOD);

		copyMethodScalars(source, cloned, methodTraits);
		cloned.setProperty(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

		return cloned;
	}

	/** Copy the scalar SchemaMethod properties (name, source, flags, ...) from source to target. */
	/**
	 * Copy a method's authored content. NAME is deliberately NOT copied: it carries the
	 * process-version scope (see BpmnHandlerNames), so the target keeps the name belonging
	 * to ITS scope while receiving the source's body and metadata.
	 */
	private void copyMethodScalars(final NodeInterface source, final NodeInterface target, final Traits methodTraits) throws FrameworkException {

		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.SOURCE_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.SUMMARY_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.CODE_TYPE_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.IS_STATIC_PROPERTY);
		copyProp(source, target, methodTraits, SchemaMethodTraitDefinition.RETURN_RAW_RESULT_PROPERTY);
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

		if (StringUtils.isEmpty(processId)) {

			return "1";
		}

		final Traits procTraits                = Traits.of(ProcessTraits.BPMN_PROCESS);
		final PropertyKey<String> processIdKey = procTraits.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY);
		final PropertyKey<String> versionKey   = procTraits.key(BpmnBaseNodeTraitDefinition.VERSION_PROPERTY);
		int max = 0;

		for (final NodeInterface existing : app.nodeQuery(ProcessTraits.BPMN_PROCESS).key(processIdKey, processId).getResultStream()) {

			final String v = existing.getProperty(versionKey);
			if (v != null) {

				try {

					final int parsed = Integer.parseInt(v);
					if (parsed > max) {

						max = parsed;
					}

				} catch (NumberFormatException ignore) {

					// ignore non-integer versions; they don't participate in auto-increment
				}
			}
		}

		return String.valueOf(max + 1);
	}
}
