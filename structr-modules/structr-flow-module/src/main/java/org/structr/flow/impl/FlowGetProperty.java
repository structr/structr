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
package org.structr.flow.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowGetProperty extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowGetProperty.class);

	public FlowGetProperty(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public FlowDataSource getNodeSource() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("nodeSource"));
		if (node != null) {

			return node.as(FlowDataSource.class);
		}

		return null;
	}

	public FlowDataSource getPropertyNameSource() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("propertyNameSource"));
		if (node != null) {

			return node.as(FlowDataSource.class);
		}

		return null;
	}

	public String getPropertyName() {
		return wrappedObject.getProperty(traits.key("propertyName"));
	}

	@Override
	public Object get(final Context context) throws FlowException {

		final FlowDataSource _nodeSource = getNodeSource();
		final FlowDataSource _nameSource = getPropertyNameSource();
		final String _propertyName       = getPropertyName();

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

								logger.warn("Name source of {} returned unknown property key {}", getUuid(), mapKey);
							}
						}

					} else {

						logger.warn("Name source of {} returned null", getUuid());
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

						logger.warn("Name source of {} returned null", getUuid());
					}

				} else {

					logger.warn("Node data source of {} returned invalid object of type {}", getUuid(), input.getClass().getName());
				}

			} else {

				logger.warn("Node data source of {} returned null", getUuid());
			}

		} else {

			logger.warn("Unable to evaluate FlowDataSource {}, missing at least one source.", getUuid());
		}

		return null;
	}


	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("propertyName",                getPropertyName());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
