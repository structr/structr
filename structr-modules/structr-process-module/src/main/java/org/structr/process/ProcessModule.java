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
package org.structr.process;

import org.structr.api.service.LicenseManager;
import org.structr.core.function.Functions;
import org.structr.core.property.EndNode;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsManager;
import org.structr.module.StructrModule;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;
import org.structr.process.function.ExportBPMNFunction;
import org.structr.process.function.ImportBPMNFunction;
import org.structr.process.function.NotifyFunction;
import org.structr.process.function.ProcessInstanceUrlFunction;
import org.structr.process.function.ProcessTokenFunction;
import org.structr.process.function.ValidateProcessTokenFunction;
import org.structr.process.traits.definitions.*;
import org.structr.process.traits.rels.*;
import org.structr.process.websocket.BpmnDiagramBatchCommand;
import org.structr.websocket.StructrWebSocket;

import java.util.Set;

/**
 * Structr Process Engine module. Registers BPMN-related traits and node/relationship types.
 */
public class ProcessModule implements StructrModule {

	@Override
	public void onLoad() {

		// websocket command (registered explicitly since the JPMS migration removed the class-path scan)
		StructrWebSocket.addCommand(BpmnDiagramBatchCommand.class);

		// Register relationship traits
		StructrTraits.registerTrait(new BpmnDefinitionsHasDiagram());
		StructrTraits.registerTrait(new BpmnSequenceFlowFrom());
		StructrTraits.registerTrait(new BpmnSequenceFlowTo());
		StructrTraits.registerTrait(new BpmnDiDiagramHasShape());
		StructrTraits.registerTrait(new BpmnDiDiagramHasEdge());
		StructrTraits.registerTrait(new BpmnDiShapeReferencesElement());
		StructrTraits.registerTrait(new BpmnDiEdgeReferencesFlow());
		StructrTraits.registerTrait(new BpmnElementHasChildElement());
		StructrTraits.registerTrait(new BpmnElementHasChildFlow());
		StructrTraits.registerTrait(new BpmnDefinitionsHasGlobalDefinition());
		StructrTraits.registerTrait(new ProcessInstanceOfProcess());
		StructrTraits.registerTrait(new ProcessInstanceInitiatedBy());
		StructrTraits.registerTrait(new ProcessInstanceHasSubject());
		StructrTraits.registerTrait(new ProcessInstanceHasToken());
		StructrTraits.registerTrait(new ProcessTokenAtElement());
		StructrTraits.registerTrait(new TaskInstanceOfProcess());
		StructrTraits.registerTrait(new TaskInstanceDefinedBy());
		StructrTraits.registerTrait(new TaskInstanceAssignedTo());
		StructrTraits.registerTrait(new TaskInstanceHasCandidateAssignee());
		StructrTraits.registerTrait(new TaskInstanceDeclinedBy());
		StructrTraits.registerTrait(new ProcessTimerOfInstance());
		StructrTraits.registerTrait(new ProcessTimerForToken());
		StructrTraits.registerTrait(new ProcessTimerAtElement());
		StructrTraits.registerTrait(new BpmnElementHasPerformer());
		StructrTraits.registerTrait(new BpmnElementHasTaskListener());
		StructrTraits.registerTrait(new BpmnElementHasMethod());
		StructrTraits.registerTrait(new BpmnTaskListenerCallsMethod());
		StructrTraits.registerTrait(new BpmnProcessListenerCallsMethod());
		StructrTraits.registerTrait(new BpmnDefinitionsHasProcess());
		StructrTraits.registerTrait(new BpmnDefinitionsHasCollaboration());
		StructrTraits.registerTrait(new BpmnProcessHasElement());
		StructrTraits.registerTrait(new BpmnProcessHasSequenceFlow());
		StructrTraits.registerTrait(new BpmnProcessHasMethod());
		StructrTraits.registerTrait(new BpmnProcessHasProcessListener());
		StructrTraits.registerTrait(new BpmnCollaborationHasParticipant());
		StructrTraits.registerTrait(new BpmnParticipantOfProcess());
		StructrTraits.registerTrait(new BpmnCollaborationHasMessageFlow());
		StructrTraits.registerTrait(new BpmnMessageFlowFrom());
		StructrTraits.registerTrait(new BpmnMessageFlowTo());
		StructrTraits.registerTrait(new BpmnProcessHasLane());
		StructrTraits.registerTrait(new BpmnLaneHasFlowNode());
		StructrTraits.registerTrait(new BpmnElementAttachedTo());
		StructrTraits.registerTrait(new BpmnProcessHasInstancePage());
		StructrTraits.registerTrait(new BpmnPerformerHasPrincipal());
		StructrTraits.registerTrait(new ActionMappingCONTROLSBpmnProcess());
		StructrTraits.registerTrait(new ActionMappingTARGETSBpmnElement());
		StructrTraits.registerTrait(new ComponentConfigurationBOUNDBpmnElement());
		StructrTraits.registerTrait(new VisibilityMappingFORBpmnProcess());
		StructrTraits.registerTrait(new VisibilityMappingATBpmnElement());
		StructrTraits.registerTrait(new ProcessInstanceHasParameterValue());
		StructrTraits.registerTrait(new ProcessParameterValueSetByElement());
		StructrTraits.registerTrait(new ProcessTokenAccessTokenPrincipal());

		// Register relationship types
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM,       ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_SEQUENCE_FLOW_FROM,            ProcessTraits.BPMN_SEQUENCE_FLOW_FROM);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_SEQUENCE_FLOW_TO,              ProcessTraits.BPMN_SEQUENCE_FLOW_TO);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DI_DIAGRAM_HAS_SHAPE,          ProcessTraits.BPMN_DI_DIAGRAM_HAS_SHAPE);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DI_DIAGRAM_HAS_EDGE,           ProcessTraits.BPMN_DI_DIAGRAM_HAS_EDGE);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT,   ProcessTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DI_EDGE_REFERENCES_FLOW,       ProcessTraits.BPMN_DI_EDGE_REFERENCES_FLOW);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT,     ProcessTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_HAS_CHILD_FLOW,        ProcessTraits.BPMN_ELEMENT_HAS_CHILD_FLOW);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION, ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_INSTANCE_OF_PROCESS,        ProcessTraits.PROCESS_INSTANCE_OF_PROCESS);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_INSTANCE_INITIATED_BY,      ProcessTraits.PROCESS_INSTANCE_INITIATED_BY);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_INSTANCE_HAS_SUBJECT,       ProcessTraits.PROCESS_INSTANCE_HAS_SUBJECT);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_INSTANCE_HAS_TOKEN,         ProcessTraits.PROCESS_INSTANCE_HAS_TOKEN);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_TOKEN_AT_ELEMENT,           ProcessTraits.PROCESS_TOKEN_AT_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.TASK_INSTANCE_OF_PROCESS,           ProcessTraits.TASK_INSTANCE_OF_PROCESS);
		StructrTraits.registerRelationshipType(ProcessTraits.TASK_INSTANCE_DEFINED_BY,           ProcessTraits.TASK_INSTANCE_DEFINED_BY);
		StructrTraits.registerRelationshipType(ProcessTraits.TASK_INSTANCE_ASSIGNED_TO,          ProcessTraits.TASK_INSTANCE_ASSIGNED_TO);
		StructrTraits.registerRelationshipType(ProcessTraits.TASK_INSTANCE_HAS_CANDIDATE_ASSIGNEE, ProcessTraits.TASK_INSTANCE_HAS_CANDIDATE_ASSIGNEE);
		StructrTraits.registerRelationshipType(ProcessTraits.TASK_INSTANCE_DECLINED_BY,          ProcessTraits.TASK_INSTANCE_DECLINED_BY);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_TIMER_OF_INSTANCE,          ProcessTraits.PROCESS_TIMER_OF_INSTANCE);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_TIMER_FOR_TOKEN,            ProcessTraits.PROCESS_TIMER_FOR_TOKEN);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_TIMER_AT_ELEMENT,           ProcessTraits.PROCESS_TIMER_AT_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_HAS_PERFORMER,         ProcessTraits.BPMN_ELEMENT_HAS_PERFORMER);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_HAS_TASK_LISTENER,     ProcessTraits.BPMN_ELEMENT_HAS_TASK_LISTENER);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_HAS_METHOD,             ProcessTraits.BPMN_ELEMENT_HAS_METHOD);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_TASK_LISTENER_CALLS_METHOD,     ProcessTraits.BPMN_TASK_LISTENER_CALLS_METHOD);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD,  ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DEFINITIONS_HAS_PROCESS,         ProcessTraits.BPMN_DEFINITIONS_HAS_PROCESS);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_DEFINITIONS_HAS_COLLABORATION,   ProcessTraits.BPMN_DEFINITIONS_HAS_COLLABORATION);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_ELEMENT,             ProcessTraits.BPMN_PROCESS_HAS_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_SEQUENCE_FLOW,       ProcessTraits.BPMN_PROCESS_HAS_SEQUENCE_FLOW);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_METHOD,              ProcessTraits.BPMN_PROCESS_HAS_METHOD);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_PROCESS_LISTENER,    ProcessTraits.BPMN_PROCESS_HAS_PROCESS_LISTENER);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_COLLABORATION_HAS_PARTICIPANT,   ProcessTraits.BPMN_COLLABORATION_HAS_PARTICIPANT);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS,          ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_COLLABORATION_HAS_MESSAGE_FLOW,  ProcessTraits.BPMN_COLLABORATION_HAS_MESSAGE_FLOW);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_MESSAGE_FLOW_FROM,               ProcessTraits.BPMN_MESSAGE_FLOW_FROM);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_MESSAGE_FLOW_TO,                 ProcessTraits.BPMN_MESSAGE_FLOW_TO);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_LANE,                ProcessTraits.BPMN_PROCESS_HAS_LANE);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_LANE_HAS_FLOW_NODE,              ProcessTraits.BPMN_LANE_HAS_FLOW_NODE);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_ELEMENT_ATTACHED_TO,             ProcessTraits.BPMN_ELEMENT_ATTACHED_TO);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PROCESS_HAS_INSTANCE_PAGE,       ProcessTraits.BPMN_PROCESS_HAS_INSTANCE_PAGE);
		StructrTraits.registerRelationshipType(ProcessTraits.BPMN_PERFORMER_HAS_PRINCIPAL,         ProcessTraits.BPMN_PERFORMER_HAS_PRINCIPAL);
		StructrTraits.registerRelationshipType(StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS, StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS);
		StructrTraits.registerRelationshipType(StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT,      StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT, StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.VISIBILITY_MAPPING_FOR_BPMN_PROCESS,  ProcessTraits.VISIBILITY_MAPPING_FOR_BPMN_PROCESS);
		StructrTraits.registerRelationshipType(ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT,       ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE, ProcessTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT, ProcessTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT);
		StructrTraits.registerRelationshipType(ProcessTraits.PROCESS_TOKEN_ACCESS_TOKEN_PRINCIPAL, ProcessTraits.PROCESS_TOKEN_ACCESS_TOKEN_PRINCIPAL);

		// Register node traits
		StructrTraits.registerTrait(new BpmnBaseNodeTraitDefinition());
		StructrTraits.registerTrait(new BpmnDefinitionsTraitDefinition());
		StructrTraits.registerTrait(new BpmnElementTraitDefinition());
		StructrTraits.registerTrait(new BpmnSequenceFlowTraitDefinition());
		StructrTraits.registerTrait(new BpmnDiDiagramTraitDefinition());
		StructrTraits.registerTrait(new BpmnDiShapeTraitDefinition());
		StructrTraits.registerTrait(new BpmnDiEdgeTraitDefinition());
		StructrTraits.registerTrait(new BpmnGlobalDefinitionTraitDefinition());
		StructrTraits.registerTrait(new ProcessInstanceTraitDefinition());
		StructrTraits.registerTrait(new ProcessTokenTraitDefinition());
		StructrTraits.registerTrait(new TaskInstanceTraitDefinition());
		StructrTraits.registerTrait(new ProcessParameterValueTraitDefinition());
		StructrTraits.registerTrait(new BpmnPerformerTraitDefinition());
		StructrTraits.registerTrait(new BpmnTaskListenerTraitDefinition());
		StructrTraits.registerTrait(new BpmnProcessListenerTraitDefinition());
		StructrTraits.registerTrait(new BpmnProcessTraitDefinition());
		StructrTraits.registerTrait(new BpmnCollaborationTraitDefinition());
		StructrTraits.registerTrait(new BpmnParticipantTraitDefinition());
		StructrTraits.registerTrait(new BpmnMessageFlowTraitDefinition());
		StructrTraits.registerTrait(new BpmnLaneTraitDefinition());
		StructrTraits.registerTrait(new VisibilityMappingTraitDefinition());
		StructrTraits.registerTrait(new ProcessTimerTraitDefinition());

		// Register node types. BPMN_BASE_NODE is composed into every BPMN-sourced
		// type so they share bpmnId + version (factored out of individual trait defs).
		StructrTraits.registerNodeType(ProcessTraits.BPMN_DEFINITIONS,        ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_DEFINITIONS);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_ELEMENT,            ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_ELEMENT);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_SEQUENCE_FLOW,      ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_SEQUENCE_FLOW);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_DI_DIAGRAM,         ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_DI_DIAGRAM);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_DI_SHAPE,           ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_DI_SHAPE);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_DI_EDGE,            ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_DI_EDGE);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_GLOBAL_DEFINITION,  ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_GLOBAL_DEFINITION);
		StructrTraits.registerNodeType(ProcessTraits.PROCESS_INSTANCE,        ProcessTraits.PROCESS_INSTANCE);
		StructrTraits.registerNodeType(ProcessTraits.PROCESS_TOKEN,           ProcessTraits.PROCESS_TOKEN);
		StructrTraits.registerNodeType(ProcessTraits.TASK_INSTANCE,           ProcessTraits.TASK_INSTANCE);
		StructrTraits.registerNodeType(ProcessTraits.PROCESS_PARAMETER_VALUE, ProcessTraits.PROCESS_PARAMETER_VALUE);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_PERFORMER,          ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_PERFORMER);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_TASK_LISTENER,      ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_TASK_LISTENER);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_PROCESS_LISTENER,   ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_PROCESS_LISTENER);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_PROCESS,            ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_PROCESS);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_COLLABORATION,      ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_COLLABORATION);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_PARTICIPANT,        ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_PARTICIPANT);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_MESSAGE_FLOW,       ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_MESSAGE_FLOW);
		StructrTraits.registerNodeType(ProcessTraits.BPMN_LANE,               ProcessTraits.BPMN_BASE_NODE, ProcessTraits.BPMN_LANE);
		StructrTraits.registerNodeType(ProcessTraits.VISIBILITY_MAPPING,      ProcessTraits.VISIBILITY_MAPPING);

		// Attach process-control properties directly to the existing ACTION_MAPPING
		// trait. They live here rather than on ActionMappingTraitDefinition in
		// structr-base because the EndNode constructor eagerly resolves the rel
		// type, and ACTION_MAPPING_CONTROLS_BPMN_PROCESS / ACTION_MAPPING_TARGETS_BPMN_ELEMENT
		// are registered above by the process module. The StringProperty entries
		// could technically live in structr-base, but they belong with the EndNode
		// declarations as one logical group.
		{
			final Trait actionMapping = Traits.getTrait(StructrTraits.ACTION_MAPPING);
			actionMapping.registerPropertyKey(new EndNode(TraitsManager.getRootInstance(),
				ActionMappingTraitDefinition.CONTROLS_PROCESS_PROPERTY,
				StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS));
			actionMapping.registerPropertyKey(new EndNode(TraitsManager.getRootInstance(),
				ActionMappingTraitDefinition.TARGETS_ELEMENT_PROPERTY,
				StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT));
			actionMapping.registerPropertyKey(new StringProperty(ActionMappingTraitDefinition.PROCESS_OPERATION_PROPERTY)
				.description("Process operation: start | claim | release | decline | delegate | complete | cancel | makeAvailable | assignTask | signal | terminate | suspend | resume"));
			actionMapping.registerPropertyKey(new StringProperty(ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_PROPERTY).indexed());
			actionMapping.registerPropertyKey(new StringProperty(ActionMappingTraitDefinition.TARGETS_ELEMENT_BPMN_ID_PROPERTY).indexed());
			actionMapping.registerPropertyKey(new StringProperty(ActionMappingTraitDefinition.CONTROLS_PROCESS_ID_EXPRESSION_PROPERTY)
				.description("Structr scripting expression that resolves to a BpmnDefinitions UUID at render time. Alternative to selecting a Process manually; useful on process-catalog pages where the target definition is data-driven (e.g. ${current.id}). Only honoured for the 'start' operation."));
		}

		// Attach inverse StartNode properties for BpmnProcess HAS_METHOD SchemaMethod
		// and BpmnElement HAS_METHOD SchemaMethod directly to the existing SCHEMA_METHOD
		// trait. OneToMany.ensureCardinality() resolves a relationship's source via the
		// target type's traits, so without these properties the cardinality check fails
		// with "Invalid schema setup: missing StartNode(s) property" the first time
		// element.methods or process.methods is assigned. The two rel types must already
		// be registered above (BPMN_PROCESS_HAS_METHOD and BPMN_ELEMENT_HAS_METHOD), since
		// the StartNode constructor eagerly resolves the rel type.
		Traits.getTrait(StructrTraits.SCHEMA_METHOD).registerPropertyKey(
			new StartNode(TraitsManager.getRootInstance(), "bpmnProcess", ProcessTraits.BPMN_PROCESS_HAS_METHOD));
		Traits.getTrait(StructrTraits.SCHEMA_METHOD).registerPropertyKey(
			new StartNode(TraitsManager.getRootInstance(), "bpmnElement", ProcessTraits.BPMN_ELEMENT_HAS_METHOD));
		// Inverse for BpmnTaskListener -[CALLS]-> SchemaMethod (Many-to-One), so
		// ensureCardinality can resolve the source side when listener.method is set.
		Traits.getTrait(StructrTraits.SCHEMA_METHOD).registerPropertyKey(
			new StartNodes(TraitsManager.getRootInstance(), "callingTaskListeners", ProcessTraits.BPMN_TASK_LISTENER_CALLS_METHOD));
		Traits.getTrait(StructrTraits.SCHEMA_METHOD).registerPropertyKey(
			new StartNodes(TraitsManager.getRootInstance(), "callingProcessListeners", ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD));

		// Attach the `boundUserTask` EndNode property to the existing
		// COMPONENT_CONFIGURATION trait so the render path (in structr-base)
		// can walk to a UserTask in process-bound mode. Lives here for the
		// same reason as the rels above: the target type (BPMN_ELEMENT) is
		// only registered by the process module, so the property declaration
		// has to follow.
		Traits.getTrait(StructrTraits.COMPONENT_CONFIGURATION).registerPropertyKey(
			new EndNode(TraitsManager.getRootInstance(),
				ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY,
				StructrTraits.COMPONENT_CONFIGURATION_BOUND_BPMN_ELEMENT));

		StructrTraits.registerNodeType(ProcessTraits.PROCESS_TIMER,           ProcessTraits.PROCESS_TIMER);

		// Touch the WebSocket command class so its static initializer runs and
		// registers the command with StructrWebSocket. Without this reference,
		// the class is never loaded and the BPMN_DIAGRAM_BATCH command is
		// unknown to the dispatcher.
		BpmnDiagramBatchCommand.class.getName();
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new ImportBPMNFunction());
		Functions.put(licenseManager, new ExportBPMNFunction());
		Functions.put(licenseManager, new NotifyFunction());
		Functions.put(licenseManager, new ProcessInstanceUrlFunction());
		Functions.put(licenseManager, new ProcessTokenFunction());
		Functions.put(licenseManager, new ValidateProcessTokenFunction());
	}

	@Override
	public String getName() {
		return "process";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
