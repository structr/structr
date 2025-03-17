/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.impl.FlowSwitch;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowSwitchTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowSwitchTraitDefinition() {
		super("FlowSwitch");
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
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> prev         = new StartNodes("prev", "FlowNodes");
		final Property<NodeInterface> switchDefault          = new EndNode("default", "FlowNodes");
		final Property<Iterable<NodeInterface>> cases        = new EndNodes("cases", "FlowSwitchCases");
		final Property<NodeInterface> dataSource             = new StartNode("dataSource", "FlowDataInput");
		final Property<NodeInterface> isStartNodeOfContainer = new StartNode("isStartNodeOfContainer", "FlowContainerFlowNode");


		return newSet(
			prev,
			switchDefault,
			cases,
			dataSource,
			isStartNodeOfContainer
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"prev", "default", "cases", "dataSource"
			),
			PropertyView.Ui,
			newSet(
				"prev", "default", "cases", "dataSource"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
