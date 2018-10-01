/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowNodeDataSource;
import org.structr.flow.impl.rels.FlowNameDataSource;

/**
 *
 */
public class FlowGetProperty extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowGetProperty.class);

	public static final Property<DataSource> nodeSource         = new StartNode<>("nodeSource",         FlowNodeDataSource.class);
	public static final Property<DataSource> propertyNameSource = new StartNode<>("propertyNameSource", FlowNameDataSource.class);
	public static final Property<String> propertyName			= new StringProperty("propertyName");

	public static final View defaultView = new View(FlowGetProperty.class, PropertyView.Public, nodeSource, propertyNameSource, propertyName, currentDataSource);
	public static final View uiView      = new View(FlowGetProperty.class, PropertyView.Ui,     nodeSource, propertyNameSource, propertyName, currentDataSource);

	@Override
	public Object get(final Context context, FlowBaseNode requestingEntity) throws FlowException {

		DataSource _nodeSource = getProperty(nodeSource);
		final DataSource _nameSource = getProperty(propertyNameSource);
		final String _propertyName   = getProperty(propertyName);

		if (_nodeSource == null) {
			_nodeSource = getProperty(currentDataSource);
		}

		if (_nodeSource != null && (_nameSource != null || _propertyName != null) ) {

			final Object node = _nodeSource.get(context, this);
			if (node != null) {

				if (node instanceof GraphObject) {

					Object name;

					if (_nameSource != null) {
						name = _nameSource.get(context, this);
					} else {
						name = _propertyName;
					}

					if (name != null) {

						if (name instanceof String) {

							final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(node.getClass(), (String)name, false);
							if (key != null) {

								return ((GraphObject)node).getProperty(key);

							} else {

								logger.warn("Name source of {} returned unknown property key {}", getUuid(), name);
							}
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
}
