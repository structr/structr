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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.BpmnSequenceFlow;
import org.structr.process.traits.wrappers.BpmnSequenceFlowTraitWrapper;
import java.util.Set;

/**
 * Trait definition for BpmnSequenceFlow -- a directed connection between two BPMN elements.
 * Maps to &lt;bpmn:sequenceFlow&gt; in BPMN 2.0.2 XML.
 */
public class BpmnSequenceFlowTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String BPMN_NAME_PROPERTY                   = "bpmnName";
	public static final String SOURCE_REF_ID_PROPERTY               = "sourceRefId";
	public static final String TARGET_REF_ID_PROPERTY               = "targetRefId";
	public static final String CONDITION_EXPRESSION_PROPERTY        = "conditionExpression";
	public static final String CONDITION_EXPRESSION_TYPE_PROPERTY   = "conditionExpressionType";
	public static final String IS_DEFAULT_PROPERTY                  = "isDefault";
	public static final String BPMN_ATTRIBUTES_PROPERTY             = "bpmnAttributes";
	public static final String PROCESS_PROPERTY                     = "process";
	public static final String PARENT_ELEMENT_PROPERTY              = "parentElement";
	public static final String SOURCE_ELEMENT_PROPERTY              = "sourceElement";
	public static final String TARGET_ELEMENT_PROPERTY              = "targetElement";
	public static final String DI_EDGE_PROPERTY                     = "diEdge";

	public BpmnSequenceFlowTraitDefinition() {
		super(ProcessTraits.BPMN_SEQUENCE_FLOW);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			BpmnSequenceFlow.class, (traits, node) -> new BpmnSequenceFlowTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> bpmnName                = new StringProperty(BPMN_NAME_PROPERTY);
		final Property<String> sourceRefId             = new StringProperty(SOURCE_REF_ID_PROPERTY);
		final Property<String> targetRefId             = new StringProperty(TARGET_REF_ID_PROPERTY);
		final Property<String> conditionExpression     = new StringProperty(CONDITION_EXPRESSION_PROPERTY);
		final Property<String> conditionExpressionType = new StringProperty(CONDITION_EXPRESSION_TYPE_PROPERTY);
		final Property<Boolean> isDefault              = new BooleanProperty(IS_DEFAULT_PROPERTY);
		final Property<String> bpmnAttributes          = new StringProperty(BPMN_ATTRIBUTES_PROPERTY);
		final Property<NodeInterface> process          = new StartNode(traitsInstance, PROCESS_PROPERTY, ProcessTraits.BPMN_PROCESS_HAS_SEQUENCE_FLOW);
		final Property<NodeInterface> parentElement    = new StartNode(traitsInstance, PARENT_ELEMENT_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_CHILD_FLOW);
		final Property<NodeInterface> sourceElement    = new EndNode(traitsInstance, SOURCE_ELEMENT_PROPERTY, ProcessTraits.BPMN_SEQUENCE_FLOW_FROM);
		final Property<NodeInterface> targetElement    = new EndNode(traitsInstance, TARGET_ELEMENT_PROPERTY, ProcessTraits.BPMN_SEQUENCE_FLOW_TO);
		final Property<NodeInterface> diEdge           = new StartNode(traitsInstance, DI_EDGE_PROPERTY, ProcessTraits.BPMN_DI_EDGE_REFERENCES_FLOW);

		return newSet(bpmnName, sourceRefId, targetRefId, conditionExpression, conditionExpressionType, isDefault, bpmnAttributes, process, parentElement, sourceElement, targetElement, diEdge);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, BPMN_NAME_PROPERTY, SOURCE_REF_ID_PROPERTY, TARGET_REF_ID_PROPERTY, SOURCE_ELEMENT_PROPERTY, TARGET_ELEMENT_PROPERTY),
			PropertyView.Ui, newSet(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, BPMN_NAME_PROPERTY, SOURCE_REF_ID_PROPERTY, TARGET_REF_ID_PROPERTY, CONDITION_EXPRESSION_PROPERTY, CONDITION_EXPRESSION_TYPE_PROPERTY, IS_DEFAULT_PROPERTY, BPMN_ATTRIBUTES_PROPERTY, PROCESS_PROPERTY, PARENT_ELEMENT_PROPERTY, SOURCE_ELEMENT_PROPERTY, TARGET_ELEMENT_PROPERTY, DI_EDGE_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
