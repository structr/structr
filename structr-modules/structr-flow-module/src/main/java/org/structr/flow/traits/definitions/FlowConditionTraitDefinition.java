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
import org.structr.core.property.EndNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.flow.impl.FlowCondition;

import java.util.Map;
import java.util.Set;

public class FlowConditionTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CONDITIONS_PROPERTY = "conditions";
	public static final String RESULT_PROPERTY     = "result";


	public FlowConditionTraitDefinition() {
		super(StructrTraits.FLOW_CONDITION);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowCondition.class, (traits, node) -> new FlowCondition(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<Iterable<NodeInterface>> conditions = new StartNodes(CONDITIONS_PROPERTY, StructrTraits.FLOW_CONDITION_CONDITION);
		final PropertyKey<NodeInterface> result               = new EndNode(RESULT_PROPERTY, StructrTraits.FLOW_CONDITION_CONDITION);

		return newSet(
			conditions,
			result
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				CONDITIONS_PROPERTY, RESULT_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				CONDITIONS_PROPERTY, RESULT_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
