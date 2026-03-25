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

import org.structr.api.service.LicenseManager;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.module.StructrModule;
import org.structr.process.function.ExportBPMNFunction;
import org.structr.process.function.ImportBPMNFunction;
import org.structr.process.traits.definitions.*;
import org.structr.process.traits.rels.*;

import java.util.Set;

/**
 * Structr Process Engine module. Registers BPMN-related traits and node/relationship types.
 */
public class ProcessModule implements StructrModule {

	@Override
	public void onLoad() {

		// Register relationship traits
		StructrTraits.registerTrait(new BpmnDefinitionsHasElement());
		StructrTraits.registerTrait(new BpmnDefinitionsHasSequenceFlow());
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
		StructrTraits.registerTrait(new ProcessInstanceOfDefinition());
		StructrTraits.registerTrait(new ProcessInstanceHasToken());
		StructrTraits.registerTrait(new ProcessTokenAtElement());
		StructrTraits.registerTrait(new TaskInstanceOfProcess());
		StructrTraits.registerTrait(new TaskInstanceDefinedBy());
		StructrTraits.registerTrait(new BpmnElementHasParameter());
		StructrTraits.registerTrait(new ProcessInstanceHasParameterValue());
		StructrTraits.registerTrait(new ProcessParameterValueOfParameter());
		StructrTraits.registerTrait(new ProcessParameterValueSetByElement());

		// Register relationship types
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DEFINITIONS_HAS_ELEMENT,       StructrTraits.BPMN_DEFINITIONS_HAS_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DEFINITIONS_HAS_SEQUENCE_FLOW, StructrTraits.BPMN_DEFINITIONS_HAS_SEQUENCE_FLOW);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DEFINITIONS_HAS_DIAGRAM,       StructrTraits.BPMN_DEFINITIONS_HAS_DIAGRAM);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_SEQUENCE_FLOW_FROM,            StructrTraits.BPMN_SEQUENCE_FLOW_FROM);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_SEQUENCE_FLOW_TO,              StructrTraits.BPMN_SEQUENCE_FLOW_TO);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DI_DIAGRAM_HAS_SHAPE,          StructrTraits.BPMN_DI_DIAGRAM_HAS_SHAPE);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DI_DIAGRAM_HAS_EDGE,           StructrTraits.BPMN_DI_DIAGRAM_HAS_EDGE);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT,   StructrTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DI_EDGE_REFERENCES_FLOW,       StructrTraits.BPMN_DI_EDGE_REFERENCES_FLOW);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT,      StructrTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_ELEMENT_HAS_CHILD_FLOW,         StructrTraits.BPMN_ELEMENT_HAS_CHILD_FLOW);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION, StructrTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_INSTANCE_OF_DEFINITION,          StructrTraits.PROCESS_INSTANCE_OF_DEFINITION);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_INSTANCE_HAS_TOKEN,              StructrTraits.PROCESS_INSTANCE_HAS_TOKEN);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_TOKEN_AT_ELEMENT,                StructrTraits.PROCESS_TOKEN_AT_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.TASK_INSTANCE_OF_PROCESS,                StructrTraits.TASK_INSTANCE_OF_PROCESS);
		StructrTraits.registerRelationshipType(StructrTraits.TASK_INSTANCE_DEFINED_BY,                StructrTraits.TASK_INSTANCE_DEFINED_BY);
		StructrTraits.registerRelationshipType(StructrTraits.BPMN_ELEMENT_HAS_PARAMETER,              StructrTraits.BPMN_ELEMENT_HAS_PARAMETER);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE,    StructrTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_PARAMETER_VALUE_OF_PARAMETER,    StructrTraits.PROCESS_PARAMETER_VALUE_OF_PARAMETER);
		StructrTraits.registerRelationshipType(StructrTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT,  StructrTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT);

		// Register node traits
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
		StructrTraits.registerTrait(new ProcessParameterTraitDefinition());
		StructrTraits.registerTrait(new ProcessParameterValueTraitDefinition());

		// Register node types (type name, then all traits that compose the type)
		StructrTraits.registerNodeType(StructrTraits.BPMN_DEFINITIONS,   StructrTraits.BPMN_DEFINITIONS);
		StructrTraits.registerNodeType(StructrTraits.BPMN_ELEMENT,       StructrTraits.BPMN_ELEMENT);
		StructrTraits.registerNodeType(StructrTraits.BPMN_SEQUENCE_FLOW, StructrTraits.BPMN_SEQUENCE_FLOW);
		StructrTraits.registerNodeType(StructrTraits.BPMN_DI_DIAGRAM,    StructrTraits.BPMN_DI_DIAGRAM);
		StructrTraits.registerNodeType(StructrTraits.BPMN_DI_SHAPE,      StructrTraits.BPMN_DI_SHAPE);
		StructrTraits.registerNodeType(StructrTraits.BPMN_DI_EDGE,       StructrTraits.BPMN_DI_EDGE);
		StructrTraits.registerNodeType(StructrTraits.BPMN_GLOBAL_DEFINITION, StructrTraits.BPMN_GLOBAL_DEFINITION);
		StructrTraits.registerNodeType(StructrTraits.PROCESS_INSTANCE,       StructrTraits.PROCESS_INSTANCE);
		StructrTraits.registerNodeType(StructrTraits.PROCESS_TOKEN,          StructrTraits.PROCESS_TOKEN);
		StructrTraits.registerNodeType(StructrTraits.TASK_INSTANCE,          StructrTraits.TASK_INSTANCE);
		StructrTraits.registerNodeType(StructrTraits.PROCESS_PARAMETER,       StructrTraits.PROCESS_PARAMETER);
		StructrTraits.registerNodeType(StructrTraits.PROCESS_PARAMETER_VALUE, StructrTraits.PROCESS_PARAMETER_VALUE);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new ImportBPMNFunction());
		Functions.put(licenseManager, new ExportBPMNFunction());
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
