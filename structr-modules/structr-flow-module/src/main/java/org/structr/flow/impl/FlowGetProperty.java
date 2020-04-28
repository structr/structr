/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowNodeDataSource;
import org.structr.flow.impl.rels.FlowNameDataSource;

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

	@Override
	public Object get(final Context context) throws FlowException {

		final DataSource _nodeSource = getProperty(nodeSource);
		final DataSource _nameSource = getProperty(propertyNameSource);
		final String _propertyName   = getProperty(propertyName);

		if (_nodeSource != null && (_nameSource != null || _propertyName != null) ) {

			final Object node = _nodeSource.get(context);
			if (node != null) {

				if (node instanceof GraphObject) {

					Object name;

					if (_nameSource != null) {
						name = _nameSource.get(context);
					} else {
						name = _propertyName;
					}

					if (name != null) {

						if (name instanceof String) {

							final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(node.getClass(), (String) name, false);
							if (key != null) {

								return ((GraphObject) node).getProperty(key);

							} else {

								logger.warn("Name source of {} returned unknown property key {}", getUuid(), name);
							}
						}

					} else {

						logger.warn("Name source of {} returned null", getUuid());
					}

				} else if (node instanceof NativeObject) {

					Object name;

					if (_nameSource != null) {
						name = _nameSource.get(context);
					} else {
						name = _propertyName;
					}

					if (name != null) {

						if (name instanceof String) {

							return ((NativeObject)node).get(name);
						}

					} else {

						logger.warn("Name source of {} returned null", getUuid());
					}
				} else {

					logger.warn("Node data source of {} returned invalid object of type {}", getUuid(), node.getClass().getName());
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
