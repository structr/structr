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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowGetProperty;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Map;
import java.util.Set;

public class FlowGetPropertyTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String NODE_SOURCE_PROPERTY          = "nodeSource";
	public static final String PROPERTY_NAME_SOURCE_PROPERTY = "propertyNameSource";
	public static final String PROPERTY_NAME_PROPERTY        = "propertyName";

	private static final Logger logger = LoggerFactory.getLogger(FlowGetPropertyTraitDefinition.class);

	public FlowGetPropertyTraitDefinition() {
		super(StructrTraits.FLOW_GET_PROPERTY);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowGetProperty getProperty = node.as(FlowGetProperty.class);
					final FlowDataSource _nodeSource  = getProperty.getNodeSource();
					final FlowDataSource _nameSource  = getProperty.getPropertyNameSource();
					final String _propertyName        = getProperty.getPropertyName();
					final String uuid                 = node.getUuid();

					if (_nodeSource != null && (_nameSource != null || _propertyName != null) ) {

						final Object input = _nodeSource.get(context);
						if (input != null) {

							if (input instanceof GraphObject graphObject) {

								Object mapKey;

								if (_nameSource != null) {
									mapKey = _nameSource.get(context);
								} else {
									mapKey = _propertyName;
								}

								if (mapKey != null) {

									if (mapKey instanceof String stringKey) {

										final Traits traits = graphObject.getTraits();
										if (traits.hasKey(stringKey)) {

											final PropertyKey key = graphObject.getTraits().key(stringKey);
											if (key != null) {

												return graphObject.getProperty(key);
											}

										} else {

											logger.warn("Name source of {} returned unknown property key {}", uuid, mapKey);
										}
									}

								} else {

									logger.warn("Name source of {} returned null", uuid);
								}

							} else if (input instanceof Map) {

								Object key;

								if (_nameSource != null) {

									key = _nameSource.get(context);

								} else {

									key = _propertyName;
								}

								if (key != null) {

									return ((Map<?, ?>) input).get(key);

								} else {

									logger.warn("Name source of {} returned null", uuid);
								}

							} else {

								logger.warn("Node data source of {} returned invalid object of type {}", uuid, input.getClass().getName());
							}

						} else {

							logger.warn("Node data source of {} returned null", uuid);
						}

					} else {

						logger.warn("Unable to evaluate FlowDataSource {}, missing at least one source.", uuid);
					}

					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowGetProperty.class, (traits, node) -> new FlowGetProperty(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> nodeSource         = new StartNode(NODE_SOURCE_PROPERTY, StructrTraits.FLOW_NODE_DATA_SOURCE);
		final Property<NodeInterface> propertyNameSource = new StartNode(PROPERTY_NAME_SOURCE_PROPERTY, StructrTraits.FLOW_NAME_DATA_SOURCE);
		final Property<String> propertyName              = new StringProperty(PROPERTY_NAME_PROPERTY);

		return newSet(
			nodeSource,
			propertyNameSource,
			propertyName
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				NODE_SOURCE_PROPERTY, PROPERTY_NAME_SOURCE_PROPERTY, PROPERTY_NAME_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				NODE_SOURCE_PROPERTY, PROPERTY_NAME_SOURCE_PROPERTY, PROPERTY_NAME_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

}
