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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowKeyValue;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowKeyValueTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String KEY_PROPERTY         = "key";
	public static final String DATA_TARGET_PROPERTY = "dataTarget";

	private static final Logger logger = LoggerFactory.getLogger(FlowKeyValueTraitDefinition.class);

	public FlowKeyValueTraitDefinition() {
		super(StructrTraits.FLOW_KEY_VALUE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource node) throws FlowException {

						final FlowKeyValue keyValue = node.as(FlowKeyValue.class);
						final String _key           = keyValue.getKey();
						final FlowDataSource _ds    = keyValue.getDataSource();
						final String uuid           = keyValue.getUuid();

						if (_key != null && _ds != null) {

							final Object data = _ds.get(context);
							if (_key.length() > 0) {

								return new KeyValue(_key, data);

							} else {

								logger.warn("Unable to evaluate FlowKeyValue {}, key was empty", uuid);
							}

						} else {

							logger.warn("Unable to evaluate FlowKeyValue {}, missing at least one source.", uuid);
						}

						return null;
					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(FlowKeyValueTraitDefinition.KEY_PROPERTY,                           flowBaseNode.as(FlowKeyValue.class).getKey());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        flowBaseNode.isVisibleToPublicUsers());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, flowBaseNode.isVisibleToAuthenticatedUsers());

						return result;
					}
				}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowKeyValue.class, (traits, node) -> new FlowKeyValue(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> key                         = new StringProperty(KEY_PROPERTY);
		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(traitsInstance, DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);

		return newSet(
			key,
			dataTarget
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				KEY_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				KEY_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
