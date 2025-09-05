/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.flow.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.impl.FlowDecision;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowDecisionTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CONDITION_PROPERTY     = "condition";
	public static final String TRUE_ELEMENT_PROPERTY  = "trueElement";
	public static final String FALSE_ELEMENT_PROPERTY = "falseElement";

	public FlowDecisionTraitDefinition() {
		super(StructrTraits.FLOW_DECISION);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(FlowNode flowNode) {
					return FlowType.Decision;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowDecision.class, (traits, node) -> new FlowDecision(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> condition    = new StartNode(CONDITION_PROPERTY, StructrTraits.FLOW_DECISION_CONDITION);
		final Property<NodeInterface> trueElement  = new EndNode(TRUE_ELEMENT_PROPERTY, StructrTraits.FLOW_DECISION_TRUE);
		final Property<NodeInterface> falseElement = new EndNode(FALSE_ELEMENT_PROPERTY, StructrTraits.FLOW_DECISION_FALSE);

		return newSet(
			condition,
			trueElement,
			falseElement
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				CONDITION_PROPERTY, TRUE_ELEMENT_PROPERTY, FALSE_ELEMENT_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				CONDITION_PROPERTY, TRUE_ELEMENT_PROPERTY, FALSE_ELEMENT_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
