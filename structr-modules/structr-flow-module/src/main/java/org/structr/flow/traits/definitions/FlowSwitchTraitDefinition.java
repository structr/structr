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
package org.structr.flow.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.impl.FlowSwitch;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowSwitchTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DEFAULT_PROPERTY = "default";
	public static final String CASES_PROPERTY   = "cases";

	public FlowSwitchTraitDefinition() {
		super(StructrTraits.FLOW_SWITCH);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(FlowNode flowNode) {
					return FlowType.Switch;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowSwitch.class, (traits, node) -> new FlowSwitch(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> switchDefault          = new EndNode(traitsInstance, DEFAULT_PROPERTY, StructrTraits.FLOW_NODES);
		final Property<Iterable<NodeInterface>> cases        = new EndNodes(traitsInstance, CASES_PROPERTY, StructrTraits.FLOW_SWITCH_CASES);

		return newSet(
			switchDefault,
			cases
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					FlowNodeTraitDefinition.PREV_PROPERTY, DEFAULT_PROPERTY, CASES_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					FlowNodeTraitDefinition.PREV_PROPERTY, DEFAULT_PROPERTY, CASES_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
