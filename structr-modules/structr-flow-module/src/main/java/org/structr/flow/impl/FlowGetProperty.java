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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowNameDataSource;
import org.structr.flow.impl.rels.FlowNodeDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FlowGetProperty extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowGetProperty.class);

	public static final Property<DataSource> nodeSource         = new StartNode<>("nodeSource",         FlowNodeDataSource.class);
	public static final Property<DataSource> propertyNameSource = new StartNode<>("propertyNameSource", FlowNameDataSource.class);
	public static final Property<String> propertyName			= new StringProperty("propertyName");

	public static final View defaultView = new View(FlowGetProperty.class, PropertyView.Public, nodeSource, propertyNameSource, propertyName);
	public static final View uiView      = new View(FlowGetProperty.class, PropertyView.Ui,     nodeSource, propertyNameSource, propertyName);

	public FlowGetProperty(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Object get(final Context context) throws FlowException {

		final DataSource _nodeSource = getProperty(nodeSource);
		final DataSource _nameSource = getProperty(propertyNameSource);
		final String _propertyName   = getProperty(propertyName);

		if (_nodeSource != null && (_nameSource != null || _propertyName != null) ) {

			final Object input = _nodeSource.get(context);
			if (input != null) {

				if (input instanceof GraphObject) {

					Object mapKey;

					if (_nameSource != null) {
						mapKey = _nameSource.get(context);
					} else {
						mapKey = _propertyName;
					}

					if (mapKey != null) {

						if (mapKey instanceof String) {

							final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(input.getClass(), (String) mapKey, false);
							if (key != null) {

								return ((GraphObject) input).getProperty(key);

							} else {

								logger.warn("Name source of {} returned unknown property key {}", getUuid(), key);
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
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("propertyName", this.getProperty(propertyName));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}
}
