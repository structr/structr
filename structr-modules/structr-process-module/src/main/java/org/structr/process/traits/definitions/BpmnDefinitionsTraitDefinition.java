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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import org.structr.process.engine.ProcessEngine;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;

/**
 * Trait definition for BpmnDefinitions -- the top-level container for a BPMN process definition.
 * Maps to the &lt;bpmn:definitions&gt; element in BPMN 2.0.2 XML.
 *
 * Exposes a startProcess method callable via REST:
 *   POST /structr/rest/BpmnDefinitions/{id}/startProcess
 */
public class BpmnDefinitionsTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_ID_PROPERTY             = "bpmnId";
	public static final String TARGET_NAMESPACE_PROPERTY     = "targetNamespace";
	public static final String EXPORTER_PROPERTY             = "exporter";
	public static final String EXPORTER_VERSION_PROPERTY     = "exporterVersion";
	public static final String PROCESS_ID_PROPERTY           = "processId";
	public static final String PROCESS_NAME_PROPERTY         = "processName";
	public static final String PROCESS_IS_EXECUTABLE         = "processIsExecutable";
	public static final String NAMESPACE_DECLARATIONS        = "namespaceDeclarations";
	public static final String GLOBAL_DEFINITIONS_PROPERTY   = "globalDefinitions";
	public static final String PROCESS_INSTANCES_PROPERTY    = "processInstances";
	public static final String ELEMENTS_PROPERTY             = "elements";
	public static final String SEQUENCE_FLOWS_PROPERTY       = "sequenceFlows";
	public static final String DIAGRAMS_PROPERTY             = "diagrams";

	public BpmnDefinitionsTraitDefinition() {
		super(ProcessTraits.BPMN_DEFINITIONS);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> bpmnId                             = new StringProperty(BPMN_ID_PROPERTY).indexed();
		final Property<String> targetNamespace                    = new StringProperty(TARGET_NAMESPACE_PROPERTY);
		final Property<String> exporter                           = new StringProperty(EXPORTER_PROPERTY);
		final Property<String> exporterVersion                    = new StringProperty(EXPORTER_VERSION_PROPERTY);
		final Property<String> processId                          = new StringProperty(PROCESS_ID_PROPERTY).indexed();
		final Property<String> processName                        = new StringProperty(PROCESS_NAME_PROPERTY).indexed();
		final Property<Boolean> processIsExecutable               = new BooleanProperty(PROCESS_IS_EXECUTABLE);
		final Property<String> namespaceDeclarations              = new StringProperty(NAMESPACE_DECLARATIONS);
		final Property<Iterable<NodeInterface>> globalDefinitions = new EndNodes(traitsInstance, GLOBAL_DEFINITIONS_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_GLOBAL_DEFINITION);
		final Property<Iterable<NodeInterface>> processInstances  = new StartNodes(traitsInstance, PROCESS_INSTANCES_PROPERTY, ProcessTraits.PROCESS_INSTANCE_OF_DEFINITION);
		final Property<Iterable<NodeInterface>> elements          = new EndNodes(traitsInstance, ELEMENTS_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_ELEMENT);
		final Property<Iterable<NodeInterface>> sequenceFlows     = new EndNodes(traitsInstance, SEQUENCE_FLOWS_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_SEQUENCE_FLOW);
		final Property<Iterable<NodeInterface>> diagrams          = new EndNodes(traitsInstance, DIAGRAMS_PROPERTY, ProcessTraits.BPMN_DEFINITIONS_HAS_DIAGRAM);

		return newSet(bpmnId, targetNamespace, exporter, exporterVersion, processId, processName, processIsExecutable, namespaceDeclarations, globalDefinitions, processInstances, elements, sequenceFlows, diagrams);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(BPMN_ID_PROPERTY, PROCESS_ID_PROPERTY, PROCESS_NAME_PROPERTY, GLOBAL_DEFINITIONS_PROPERTY, ELEMENTS_PROPERTY, SEQUENCE_FLOWS_PROPERTY, DIAGRAMS_PROPERTY),
			PropertyView.Ui, newSet(BPMN_ID_PROPERTY, TARGET_NAMESPACE_PROPERTY, EXPORTER_PROPERTY, EXPORTER_VERSION_PROPERTY, PROCESS_ID_PROPERTY, PROCESS_NAME_PROPERTY, PROCESS_IS_EXECUTABLE, NAMESPACE_DECLARATIONS, GLOBAL_DEFINITIONS_PROPERTY, ELEMENTS_PROPERTY, SEQUENCE_FLOWS_PROPERTY, DIAGRAMS_PROPERTY)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("startProcess", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					final ProcessEngine engine = new ProcessEngine(securityContext);
					return engine.startProcess((NodeInterface) entity);
				}

				@Override
				public String getDescription() {
					return "Starts a new process instance from this process definition. Returns the created ProcessInstance node.";
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
