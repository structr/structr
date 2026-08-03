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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.engine.ProcessEngine;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.BpmnProcess;
import org.structr.process.traits.wrappers.BpmnProcessTraitWrapper;
import java.util.Set;

/**
 * Trait definition for BpmnProcess -- the execution unit declared by a single
 * {@code <bpmn:process>} element. Multiple processes can live under one
 * BpmnDefinitions (collaboration / file root); each runs independently in
 * the engine.
 *
 * <p>Holds what was previously on BpmnDefinitions: processId, processName,
 * isExecutable, defaultAssigneeFromInitiator, plus relationships to
 * elements, sequence flows, methods, and process listeners. BpmnDefinitions
 * keeps file-level metadata only.</p>
 */
public class BpmnProcessTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DEFINITION_PROPERTY                       = "definition";
	public static final String PROCESS_ID_PROPERTY                       = "processId";
	public static final String PROCESS_NAME_PROPERTY                     = "processName";
	public static final String PROCESS_IS_EXECUTABLE_PROPERTY            = "processIsExecutable";
	public static final String DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY  = "defaultAssigneeFromInitiator";
	public static final String ELEMENTS_PROPERTY                         = "elements";
	public static final String SEQUENCE_FLOWS_PROPERTY                   = "sequenceFlows";
	public static final String METHODS_PROPERTY                          = "methods";
	public static final String PROCESS_LISTENERS_PROPERTY                = "processListeners";
	public static final String LANES_PROPERTY                            = "lanes";
	public static final String PARTICIPANT_PROPERTY                      = "participant";
	// Explicit binding to the Page that renders one of this process's
	// instances. When set, the engine builds the start-process URL from
	// the page's name; when unset, the convention falls back to the
	// slugified processName.
	public static final String INSTANCE_PAGE_PROPERTY                    = "instancePage";
	public static final String CONTROL_ACTIONS_PROPERTY                  = "controlActions";
	public static final String VISIBILITY_MAPPINGS_PROPERTY              = "visibilityMappings";
	// Process / UI contract: the SchemaNode type of the single domain object
	// (the "subject") this process operates on. A ProcessInstance has at most
	// one subject, so its type is a process-level fact -- it cannot legitimately
	// differ between steps. Individual UserTask elements narrow *which fields*
	// of this type they expose via subjectFormView / subjectWritableView, but
	// the type itself lives here. Read at render time by process-bound widgets
	// as their expected data type; their data source is the `current` channel
	// (the process instance's single subject), not "node:<subjectType>".
	public static final String SUBJECT_TYPE_PROPERTY                     = "subjectType";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnProcess.class, (traits, node) -> new BpmnProcessTraitWrapper(traits, node)
		);
	}

	public BpmnProcessTraitDefinition() {
		super(ProcessTraits.BPMN_PROCESS);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String>  processId                          = new StringProperty(PROCESS_ID_PROPERTY).indexed();
		final Property<String>  processName                        = new StringProperty(PROCESS_NAME_PROPERTY).indexed();
		final Property<Boolean> processIsExecutable                = new BooleanProperty(PROCESS_IS_EXECUTABLE_PROPERTY);
		final Property<Boolean> defaultAssigneeFromInitiator       = new BooleanProperty(DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY);
		final Property<String>  subjectType                        = new StringProperty(SUBJECT_TYPE_PROPERTY).indexed();

		// Parent reference: which BpmnDefinitions hosts this process.
		final Property<NodeInterface>           definition         = new StartNode(traitsInstance, DEFINITION_PROPERTY,        ProcessTraits.BPMN_DEFINITIONS_HAS_PROCESS);
		// Owned collections moved here from BpmnDefinitions.
		final Property<Iterable<NodeInterface>> elements           = new EndNodes(traitsInstance,  ELEMENTS_PROPERTY,          ProcessTraits.BPMN_PROCESS_HAS_ELEMENT);
		final Property<Iterable<NodeInterface>> sequenceFlows      = new EndNodes(traitsInstance,  SEQUENCE_FLOWS_PROPERTY,    ProcessTraits.BPMN_PROCESS_HAS_SEQUENCE_FLOW);
		final Property<Iterable<NodeInterface>> methods            = new EndNodes(traitsInstance,  METHODS_PROPERTY,           ProcessTraits.BPMN_PROCESS_HAS_METHOD);
		final Property<Iterable<NodeInterface>> processListeners   = new EndNodes(traitsInstance,  PROCESS_LISTENERS_PROPERTY, ProcessTraits.BPMN_PROCESS_HAS_PROCESS_LISTENER);
		final Property<Iterable<NodeInterface>> lanes              = new EndNodes(traitsInstance,  LANES_PROPERTY,             ProcessTraits.BPMN_PROCESS_HAS_LANE);
		// Inverse: the BpmnParticipant (if any) that wraps this process in a
		// collaboration. Optional -- single-process imports have no participant.
		final Property<NodeInterface>           participant        = new StartNode(traitsInstance, PARTICIPANT_PROPERTY,       ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS);
		// Bound instance page: which Page renders an instance of this
		// process. Many-to-One -- multiple processes may share a single
		// generic instance page, but each process binds at most one.
		final Property<NodeInterface>           instancePage       = new EndNode(traitsInstance,   INSTANCE_PAGE_PROPERTY,     ProcessTraits.BPMN_PROCESS_HAS_INSTANCE_PAGE);
		// Inverse for ActionMapping CONTROLS BpmnProcess rels. Required so
		// OneToMany.ensureCardinality can resolve the source side when an
		// ActionMapping's controlsProcess is reassigned (e.g. via the EAM
		// editor): without this property, am.setProperty(controlsProcess,
		// newProcess) throws "missing StartNode(s) property".
		final Property<Iterable<NodeInterface>> controlActions     = new StartNodes(traitsInstance, CONTROL_ACTIONS_PROPERTY,   StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS);
		// Inverse for VisibilityMapping FOR BpmnProcess rels. Same rationale as
		// controlActions above: needed so OneToMany.ensureCardinality can
		// resolve the source side when a VM's boundProcess is reassigned.
		final Property<Iterable<NodeInterface>> visibilityMappings = new StartNodes(traitsInstance, VISIBILITY_MAPPINGS_PROPERTY, ProcessTraits.VISIBILITY_MAPPING_FOR_BPMN_PROCESS);

		return newSet(processId, processName, processIsExecutable, defaultAssigneeFromInitiator, subjectType,
			definition, elements, sequenceFlows, methods, processListeners, lanes, participant, instancePage, controlActions, visibilityMappings);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				PROCESS_ID_PROPERTY, PROCESS_NAME_PROPERTY, PROCESS_IS_EXECUTABLE_PROPERTY,
				DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY, SUBJECT_TYPE_PROPERTY,
				ELEMENTS_PROPERTY, SEQUENCE_FLOWS_PROPERTY, METHODS_PROPERTY, PROCESS_LISTENERS_PROPERTY,
				LANES_PROPERTY),
			PropertyView.Ui, newSet(
				BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY,
				PROCESS_ID_PROPERTY, PROCESS_NAME_PROPERTY, PROCESS_IS_EXECUTABLE_PROPERTY,
				DEFAULT_ASSIGNEE_FROM_INITIATOR_PROPERTY, SUBJECT_TYPE_PROPERTY,
				DEFINITION_PROPERTY, ELEMENTS_PROPERTY, SEQUENCE_FLOWS_PROPERTY, METHODS_PROPERTY,
				PROCESS_LISTENERS_PROPERTY, LANES_PROPERTY, PARTICIPANT_PROPERTY, INSTANCE_PAGE_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("startProcess", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final SecurityContext securityContext    = actionContext.getSecurityContext();
					final ProcessEngine engine               = new ProcessEngine(securityContext);
					final java.util.Map<String, Object> args = arguments.toMap();
					final NodeInterface subject              = BpmnDefinitionsTraitDefinition.resolveSubject(actionContext, args.get("subject"));

					// Forward the remaining EAM parameters as initial process
					// parameters: subject-matching fields populate the subject,
					// the rest become ProcessParameterValues. Mirrors complete().
					final java.util.Map<String, Object> params = new java.util.LinkedHashMap<>(args);

					params.remove("subject");

					return engine.startProcess((NodeInterface) entity, subject, params.isEmpty() ? null : params);
				}

				@Override
				public String getDescription() {
					return "Starts a new process instance from this BpmnProcess. Pass an optional 'subject' parameter (node UUID or node object) to attach the domain object this instance operates on. Any other parameters are stored as initial process parameters (subject-matching fields populate the subject; the rest become ProcessParameterValues). Returns the created ProcessInstance node.";
				}
			},

			new JavaMethod("liveTokenCounts", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					// Super-user query so the overlay reflects ALL instances, not just
					// those the current user is permitted to read.
					final App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());
					return ProcessEngine.computeLiveTokenCounts(app, (NodeInterface) entity);
				}

				@Override
				public String getDescription() {
					return "Returns a map of element bpmnId -> number of non-completed tokens currently sitting at that element, aggregated across all instances of this process. Powers the editor's live instance-count overlay.";
				}
			},

			new JavaMethod("completedTokenCounts", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					// Super-user query so the overlay reflects ALL instances, not just
					// those the current user is permitted to read.
					final App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());
					return ProcessEngine.computeCompletedTokenCounts(app, (NodeInterface) entity);
				}

				@Override
				public String getDescription() {
					return "Returns a map of element bpmnId -> number of completed tokens that finished at that element, aggregated across all instances of this process. Powers the editor's finished-instance-count overlay (shown alongside the live/active badge).";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
