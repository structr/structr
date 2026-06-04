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
package org.structr.process;

/**
 * Constants for the Structr Process Engine module -- node types and relationship types.
 * These were previously in StructrTraits and are now module-local.
 */
public class ProcessTraits {

	// Shared base trait composed into every BPMN node type.
	public static final String BPMN_BASE_NODE          = "BpmnBaseNode";

	// BPMN Process Engine node types
	public static final String BPMN_DEFINITIONS        = "BpmnDefinitions";
	public static final String BPMN_PROCESS            = "BpmnProcess";
	public static final String BPMN_COLLABORATION      = "BpmnCollaboration";
	public static final String BPMN_PARTICIPANT        = "BpmnParticipant";
	public static final String BPMN_MESSAGE_FLOW       = "BpmnMessageFlow";
	public static final String BPMN_LANE               = "BpmnLane";
	public static final String BPMN_ELEMENT            = "BpmnElement";
	public static final String BPMN_SEQUENCE_FLOW      = "BpmnSequenceFlow";
	public static final String BPMN_DI_DIAGRAM         = "BpmnDiDiagram";
	public static final String BPMN_DI_SHAPE           = "BpmnDiShape";
	public static final String BPMN_DI_EDGE            = "BpmnDiEdge";
	public static final String BPMN_GLOBAL_DEFINITION  = "BpmnGlobalDefinition";
	public static final String BPMN_PERFORMER          = "BpmnPerformer";
	public static final String BPMN_TASK_LISTENER      = "BpmnTaskListener";
	public static final String BPMN_PROCESS_LISTENER   = "BpmnProcessListener";
	public static final String VISIBILITY_MAPPING      = "VisibilityMapping";
	public static final String PROCESS_TIMER           = "ProcessTimer";

	// Process Engine runtime node types
	public static final String PROCESS_INSTANCE        = "ProcessInstance";
	public static final String PROCESS_TOKEN           = "ProcessToken";
	public static final String TASK_INSTANCE           = "TaskInstance";

	// Process data node types
	public static final String PROCESS_PARAMETER_VALUE = "ProcessParameterValue";

	// BPMN Process Engine relationship types
	public static final String BPMN_DEFINITIONS_HAS_DIAGRAM            = "BpmnDefinitionsHAS_DIAGRAMBpmnDiDiagram";
	public static final String BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION  = "BpmnDefinitionsHAS_GLOBAL_DEFINITIONBpmnGlobalDefinition";
	public static final String BPMN_DEFINITIONS_HAS_PROCESS            = "BpmnDefinitionsHAS_PROCESSBpmnProcess";
	public static final String BPMN_DEFINITIONS_HAS_COLLABORATION      = "BpmnDefinitionsHAS_COLLABORATIONBpmnCollaboration";
	public static final String BPMN_PROCESS_HAS_ELEMENT                = "BpmnProcessHAS_ELEMENTBpmnElement";
	public static final String BPMN_PROCESS_HAS_SEQUENCE_FLOW          = "BpmnProcessHAS_SEQUENCE_FLOWBpmnSequenceFlow";
	public static final String BPMN_PROCESS_HAS_METHOD                 = "BpmnProcessHAS_METHODSchemaMethod";
	public static final String BPMN_PROCESS_HAS_PROCESS_LISTENER       = "BpmnProcessHAS_PROCESS_LISTENERBpmnProcessListener";
	public static final String BPMN_PROCESS_HAS_LANE                   = "BpmnProcessHAS_LANEBpmnLane";
	public static final String BPMN_PROCESS_HAS_INSTANCE_PAGE          = "BpmnProcessHAS_INSTANCE_PAGEPage";
	public static final String BPMN_PERFORMER_HAS_PRINCIPAL            = "BpmnPerformerHAS_PRINCIPALPrincipal";
	public static final String BPMN_LANE_HAS_FLOW_NODE                 = "BpmnLaneHAS_FLOW_NODEBpmnElement";
	public static final String BPMN_COLLABORATION_HAS_PARTICIPANT      = "BpmnCollaborationHAS_PARTICIPANTBpmnParticipant";
	public static final String BPMN_PARTICIPANT_OF_PROCESS             = "BpmnParticipantOF_PROCESSBpmnProcess";
	public static final String BPMN_COLLABORATION_HAS_MESSAGE_FLOW     = "BpmnCollaborationHAS_MESSAGE_FLOWBpmnMessageFlow";
	public static final String BPMN_MESSAGE_FLOW_FROM                  = "BpmnMessageFlowFROMBpmnElement";
	public static final String BPMN_MESSAGE_FLOW_TO                    = "BpmnMessageFlowTOBpmnElement";
	public static final String BPMN_SEQUENCE_FLOW_FROM                 = "BpmnSequenceFlowFROMBpmnElement";
	public static final String BPMN_SEQUENCE_FLOW_TO                   = "BpmnSequenceFlowTOBpmnElement";
	public static final String BPMN_DI_DIAGRAM_HAS_SHAPE               = "BpmnDiDiagramHAS_SHAPEBpmnDiShape";
	public static final String BPMN_DI_DIAGRAM_HAS_EDGE                = "BpmnDiDiagramHAS_EDGEBpmnDiEdge";
	public static final String BPMN_DI_SHAPE_REFERENCES_ELEMENT        = "BpmnDiShapeREFERENCESBpmnElement";
	public static final String BPMN_DI_EDGE_REFERENCES_FLOW            = "BpmnDiEdgeREFERENCESBpmnSequenceFlow";
	public static final String BPMN_ELEMENT_HAS_CHILD_ELEMENT          = "BpmnElementHAS_CHILD_ELEMENTBpmnElement";
	public static final String BPMN_ELEMENT_ATTACHED_TO                = "BpmnElementATTACHED_TOBpmnElement";
	public static final String BPMN_ELEMENT_HAS_CHILD_FLOW             = "BpmnElementHAS_CHILD_FLOWBpmnSequenceFlow";
	public static final String BPMN_ELEMENT_HAS_PERFORMER              = "BpmnElementHAS_PERFORMERBpmnPerformer";
	public static final String BPMN_ELEMENT_HAS_TASK_LISTENER          = "BpmnElementHAS_TASK_LISTENERBpmnTaskListener";
	public static final String BPMN_ELEMENT_HAS_METHOD                 = "BpmnElementHAS_METHODSchemaMethod";
	public static final String BPMN_TASK_LISTENER_CALLS_METHOD         = "BpmnTaskListenerCALLSSchemaMethod";
	public static final String BPMN_PROCESS_LISTENER_CALLS_METHOD      = "BpmnProcessListenerCALLSSchemaMethod";
	public static final String VISIBILITY_MAPPING_FOR_BPMN_PROCESS     = "VisibilityMappingFORBpmnProcess";
	public static final String VISIBILITY_MAPPING_AT_BPMN_ELEMENT      = "VisibilityMappingATBpmnElement";

	// Process Engine runtime relationship types
	public static final String PROCESS_INSTANCE_OF_PROCESS             = "ProcessInstanceINSTANCE_OFBpmnProcess";
	public static final String PROCESS_INSTANCE_INITIATED_BY           = "ProcessInstanceINITIATED_BYPrincipal";
	public static final String PROCESS_INSTANCE_HAS_SUBJECT            = "ProcessInstanceHAS_SUBJECTNodeInterface";
	public static final String PROCESS_INSTANCE_HAS_TOKEN              = "ProcessInstanceHAS_TOKENProcessToken";
	public static final String PROCESS_TOKEN_AT_ELEMENT                = "ProcessTokenAT_ELEMENTBpmnElement";
	public static final String TASK_INSTANCE_OF_PROCESS                = "TaskInstanceTASK_OFProcessInstance";
	public static final String TASK_INSTANCE_DEFINED_BY                = "TaskInstanceDEFINED_BYBpmnElement";
	public static final String TASK_INSTANCE_ASSIGNED_TO               = "TaskInstanceASSIGNED_TOPrincipal";
	public static final String TASK_INSTANCE_HAS_CANDIDATE_ASSIGNEE    = "TaskInstanceHAS_CANDIDATE_ASSIGNEEPrincipal";
	public static final String TASK_INSTANCE_DECLINED_BY               = "TaskInstanceDECLINED_BYPrincipal";

	// Timer relationship types
	public static final String PROCESS_TIMER_OF_INSTANCE               = "ProcessTimerOF_INSTANCEProcessInstance";
	public static final String PROCESS_TIMER_FOR_TOKEN                 = "ProcessTimerFOR_TOKENProcessToken";
	public static final String PROCESS_TIMER_AT_ELEMENT                = "ProcessTimerAT_ELEMENTBpmnElement";

	// Process data relationship types
	public static final String PROCESS_INSTANCE_HAS_PARAMETER_VALUE    = "ProcessInstanceHAS_PARAMETER_VALUEProcessParameterValue";
	public static final String PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT  = "ProcessParameterValueSET_BY_ELEMENTBpmnElement";

	// Access token relationship type
	public static final String PROCESS_TOKEN_ACCESS_TOKEN_PRINCIPAL    = "ProcessTokenACCESS_TOKEN_PRINCIPALPrincipal";
}
