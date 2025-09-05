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

import java.util.TreeMap;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowParameterDataSource;
import org.structr.flow.impl.FlowParameterInput;

import java.util.Map;
import java.util.Set;
import org.structr.flow.traits.operations.GetExportData;

public class FlowParameterInputTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CALL_PROPERTY = "call";
	public static final String KEY_PROPERTY  = "key";

	public FlowParameterInputTraitDefinition() {
		super(StructrTraits.FLOW_PARAMETER_INPUT);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowParameterInput.class, (traits, node) -> new FlowParameterInput(traits, node)
		);
	}


	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(FlowParameterInputTraitDefinition.KEY_PROPERTY,                     flowBaseNode.as(FlowParameterInput.class).getKey());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        flowBaseNode.isVisibleToPublicUsers());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, flowBaseNode.isVisibleToAuthenticatedUsers());

						return result;
					}
				}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> call = new EndNodes(CALL_PROPERTY, StructrTraits.FLOW_CALL_PARAMETER);
		final Property<String> key                   = new StringProperty(KEY_PROPERTY);

		return newSet(
			call,
			key
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				KEY_PROPERTY, CALL_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				KEY_PROPERTY, CALL_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
