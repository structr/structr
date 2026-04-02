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

	// BPMN Process Engine node types
	public static final String BPMN_DEFINITIONS        = "BpmnDefinitions";
	public static final String BPMN_ELEMENT            = "BpmnElement";
	public static final String BPMN_SEQUENCE_FLOW      = "BpmnSequenceFlow";
	public static final String BPMN_DI_DIAGRAM         = "BpmnDiDiagram";
	public static final String BPMN_DI_SHAPE           = "BpmnDiShape";
	public static final String BPMN_DI_EDGE            = "BpmnDiEdge";
	public static final String BPMN_GLOBAL_DEFINITION  = "BpmnGlobalDefinition";

	// Process Engine runtime node types
	public static final String PROCESS_INSTANCE        = "ProcessInstance";
	public static final String PROCESS_TOKEN           = "ProcessToken";
	public static final String TASK_INSTANCE           = "TaskInstance";

	// Process data node types
	public static final String PROCESS_PARAMETER       = "ProcessParameter";
	public static final String PROCESS_PARAMETER_VALUE = "ProcessParameterValue";

	// BPMN Process Engine relationship types
	public static final String BPMN_DEFINITIONS_HAS_ELEMENT            = "BpmnDefinitionsHAS_ELEMENTBpmnElement";
	public static final String BPMN_DEFINITIONS_HAS_SEQUENCE_FLOW      = "BpmnDefinitionsHAS_SEQUENCE_FLOWBpmnSequenceFlow";
	public static final String BPMN_DEFINITIONS_HAS_DIAGRAM            = "BpmnDefinitionsHAS_DIAGRAMBpmnDiDiagram";
	public static final String BPMN_SEQUENCE_FLOW_FROM                 = "BpmnSequenceFlowFROMBpmnElement";
	public static final String BPMN_SEQUENCE_FLOW_TO                   = "BpmnSequenceFlowTOBpmnElement";
	public static final String BPMN_DI_DIAGRAM_HAS_SHAPE               = "BpmnDiDiagramHAS_SHAPEBpmnDiShape";
	public static final String BPMN_DI_DIAGRAM_HAS_EDGE                = "BpmnDiDiagramHAS_EDGEBpmnDiEdge";
	public static final String BPMN_DI_SHAPE_REFERENCES_ELEMENT        = "BpmnDiShapeREFERENCESBpmnElement";
	public static final String BPMN_DI_EDGE_REFERENCES_FLOW            = "BpmnDiEdgeREFERENCESBpmnSequenceFlow";
	public static final String BPMN_ELEMENT_HAS_CHILD_ELEMENT          = "BpmnElementHAS_CHILD_ELEMENTBpmnElement";
	public static final String BPMN_ELEMENT_HAS_CHILD_FLOW             = "BpmnElementHAS_CHILD_FLOWBpmnSequenceFlow";
	public static final String BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION  = "BpmnDefinitionsHAS_GLOBAL_DEFINITIONBpmnGlobalDefinition";

	// Process Engine runtime relationship types
	public static final String PROCESS_INSTANCE_OF_DEFINITION          = "ProcessInstanceINSTANCE_OFBpmnDefinitions";
	public static final String PROCESS_INSTANCE_HAS_TOKEN              = "ProcessInstanceHAS_TOKENProcessToken";
	public static final String PROCESS_TOKEN_AT_ELEMENT                = "ProcessTokenAT_ELEMENTBpmnElement";
	public static final String TASK_INSTANCE_OF_PROCESS                = "TaskInstanceTASK_OFProcessInstance";
	public static final String TASK_INSTANCE_DEFINED_BY                = "TaskInstanceDEFINED_BYBpmnElement";

	// Process data relationship types
	public static final String BPMN_ELEMENT_HAS_PARAMETER              = "BpmnElementHAS_PARAMETERProcessParameter";
	public static final String PROCESS_INSTANCE_HAS_PARAMETER_VALUE    = "ProcessInstanceHAS_PARAMETER_VALUEProcessParameterValue";
	public static final String PROCESS_PARAMETER_VALUE_OF_PARAMETER    = "ProcessParameterValueOF_PARAMETERProcessParameter";
	public static final String PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT  = "ProcessParameterValueSET_BY_ELEMENTBpmnElement";
}
