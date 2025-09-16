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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.flow.impl.FlowNode;

import java.util.Map;
import java.util.Set;

public class FlowNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String IS_START_NODE_OF_CONTAINER_PROPERTY       = "isStartNodeOfContainer";
	public static final String PREV_PROPERTY                             = "prev";
	public static final String NEXT_PROPERTY                             = "next";
	public static final String PREV_FOR_EACH_PROPERTY                    = "prevForEach";		// FIXME: is this ever used?


	public FlowNodeTraitDefinition() {
		super(StructrTraits.FLOW_NODE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowNode.class, (traits, node) -> new FlowNode(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> isStartNodeOfContainer = new StartNode(traitsInstance, IS_START_NODE_OF_CONTAINER_PROPERTY, StructrTraits.FLOW_CONTAINER_FLOW_NODE);
		final Property<Iterable<NodeInterface>> prev         = new StartNodes(traitsInstance, PREV_PROPERTY, StructrTraits.FLOW_NODES);
		final Property<NodeInterface> next                   = new EndNode(traitsInstance, NEXT_PROPERTY, StructrTraits.FLOW_NODES);
		final Property<NodeInterface> prevForEach            = new StartNode(traitsInstance, PREV_FOR_EACH_PROPERTY, StructrTraits.FLOW_FOR_EACH_BODY);

		return newSet(
			isStartNodeOfContainer,
			prev,
			next,
			prevForEach
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				PREV_PROPERTY, NEXT_PROPERTY, IS_START_NODE_OF_CONTAINER_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				PREV_PROPERTY, NEXT_PROPERTY, IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
