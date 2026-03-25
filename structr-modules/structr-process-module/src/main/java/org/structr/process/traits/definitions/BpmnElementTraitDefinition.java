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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

/**
 * Trait definition for BpmnElement -- any BPMN element inside a process definition.
 * The bpmnElementType property identifies the BPMN element kind (startEvent, userTask,
 * exclusiveGateway, etc.).
 *
 * All BPMN content is stored as typed graph properties -- there is no raw XML storage.
 * Unknown/extra XML attributes are preserved in bpmnAttributes (JSON) for round-trip fidelity.
 */
public class BpmnElementTraitDefinition extends AbstractNodeTraitDefinition {

	// --- Core identity ---
	public static final String BPMN_ID_PROPERTY             = "bpmnId";
	public static final String BPMN_ELEMENT_TYPE_PROPERTY   = "bpmnElementType";
	public static final String BPMN_NAME_PROPERTY           = "bpmnName";
	public static final String BPMN_ATTRIBUTES_PROPERTY     = "bpmnAttributes";

	// --- Content properties (promoted from XML child elements) ---
	public static final String DOCUMENTATION_PROPERTY       = "documentation";
	public static final String SCRIPT_CONTENT_PROPERTY      = "scriptContent";

	// --- Event definition properties ---
	public static final String EVENT_DEF_TYPE_PROPERTY      = "eventDefinitionType";
	public static final String EVENT_DEF_ID_PROPERTY        = "eventDefinitionId";
	public static final String EVENT_DEF_REF_PROPERTY       = "eventDefinitionRef";
	public static final String TIMER_TYPE_PROPERTY          = "timerType";
	public static final String TIMER_EXPRESSION_TYPE_PROPERTY = "timerExpressionType";
	public static final String TIMER_VALUE_PROPERTY         = "timerValue";

	// --- Relationship properties ---
	public static final String DEFINITION_PROPERTY          = "definition";
	public static final String PARENT_ELEMENT_PROPERTY      = "parentElement";
	public static final String CHILD_ELEMENTS_PROPERTY      = "childElements";
	public static final String CHILD_FLOWS_PROPERTY         = "childFlows";
	public static final String OUTGOING_FLOWS_PROPERTY      = "outgoingFlows";
	public static final String INCOMING_FLOWS_PROPERTY      = "incomingFlows";
	public static final String PARAMETERS_PROPERTY          = "parameters";
	public static final String DI_SHAPE_PROPERTY            = "diShape";

	public BpmnElementTraitDefinition() {
		super(StructrTraits.BPMN_ELEMENT);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		// Core identity
		final Property<String> bpmnId          = new StringProperty(BPMN_ID_PROPERTY).indexed().unique();
		final Property<String> bpmnElementType = new StringProperty(BPMN_ELEMENT_TYPE_PROPERTY).indexed();
		final Property<String> bpmnName        = new StringProperty(BPMN_NAME_PROPERTY).indexed();
		final Property<String> bpmnAttributes  = new StringProperty(BPMN_ATTRIBUTES_PROPERTY);

		// Content
		final Property<String> documentation   = new StringProperty(DOCUMENTATION_PROPERTY);
		final Property<String> scriptContent   = new StringProperty(SCRIPT_CONTENT_PROPERTY);

		// Event definitions
		final Property<String> eventDefType         = new StringProperty(EVENT_DEF_TYPE_PROPERTY);
		final Property<String> eventDefId           = new StringProperty(EVENT_DEF_ID_PROPERTY);
		final Property<String> eventDefRef          = new StringProperty(EVENT_DEF_REF_PROPERTY);
		final Property<String> timerType            = new StringProperty(TIMER_TYPE_PROPERTY);
		final Property<String> timerExpressionType  = new StringProperty(TIMER_EXPRESSION_TYPE_PROPERTY);
		final Property<String> timerValue           = new StringProperty(TIMER_VALUE_PROPERTY);

		// Relationships
		final Property<NodeInterface> def                        = new StartNode(traitsInstance, DEFINITION_PROPERTY, StructrTraits.BPMN_DEFINITIONS_HAS_ELEMENT);
		final Property<NodeInterface> parentElement              = new StartNode(traitsInstance, PARENT_ELEMENT_PROPERTY, StructrTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		final Property<Iterable<NodeInterface>> childElements    = new EndNodes(traitsInstance, CHILD_ELEMENTS_PROPERTY, StructrTraits.BPMN_ELEMENT_HAS_CHILD_ELEMENT);
		final Property<Iterable<NodeInterface>> childFlows       = new EndNodes(traitsInstance, CHILD_FLOWS_PROPERTY, StructrTraits.BPMN_ELEMENT_HAS_CHILD_FLOW);
		final Property<Iterable<NodeInterface>> outgoingFlows    = new StartNodes(traitsInstance, OUTGOING_FLOWS_PROPERTY, StructrTraits.BPMN_SEQUENCE_FLOW_FROM);
		final Property<Iterable<NodeInterface>> incomingFlows    = new StartNodes(traitsInstance, INCOMING_FLOWS_PROPERTY, StructrTraits.BPMN_SEQUENCE_FLOW_TO);
		final Property<Iterable<NodeInterface>> parameters       = new EndNodes(traitsInstance, PARAMETERS_PROPERTY, StructrTraits.BPMN_ELEMENT_HAS_PARAMETER);
		final Property<NodeInterface> diShape                    = new StartNode(traitsInstance, DI_SHAPE_PROPERTY, StructrTraits.BPMN_DI_SHAPE_REFERENCES_ELEMENT);

		return newSet(bpmnId, bpmnElementType, bpmnName, bpmnAttributes,
			documentation, scriptContent,
			eventDefType, eventDefId, eventDefRef, timerType, timerExpressionType, timerValue,
			def, parentElement, childElements, childFlows, outgoingFlows, incomingFlows, parameters, diShape);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(BPMN_ID_PROPERTY, BPMN_ELEMENT_TYPE_PROPERTY, BPMN_NAME_PROPERTY,
				DOCUMENTATION_PROPERTY, SCRIPT_CONTENT_PROPERTY, EVENT_DEF_TYPE_PROPERTY),
			PropertyView.Ui, newSet(BPMN_ID_PROPERTY, BPMN_ELEMENT_TYPE_PROPERTY, BPMN_NAME_PROPERTY, BPMN_ATTRIBUTES_PROPERTY,
				DOCUMENTATION_PROPERTY, SCRIPT_CONTENT_PROPERTY,
				EVENT_DEF_TYPE_PROPERTY, EVENT_DEF_ID_PROPERTY, EVENT_DEF_REF_PROPERTY,
				TIMER_TYPE_PROPERTY, TIMER_EXPRESSION_TYPE_PROPERTY, TIMER_VALUE_PROPERTY,
				DEFINITION_PROPERTY, PARENT_ELEMENT_PROPERTY, CHILD_ELEMENTS_PROPERTY, CHILD_FLOWS_PROPERTY,
				OUTGOING_FLOWS_PROPERTY, INCOMING_FLOWS_PROPERTY, DI_SHAPE_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
