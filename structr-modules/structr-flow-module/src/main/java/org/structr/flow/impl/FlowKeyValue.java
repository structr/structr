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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FlowKeyValue extends FlowBaseNode implements DataSource, DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowKeyValue.class);

	public static final Property<DataSource> dataSource 		= new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget = new EndNodes<>("dataTarget", FlowDataInput.class);

	public static final Property<String> key             		= new StringProperty("key");

	public static final View defaultView = new View(FlowKeyValue.class, PropertyView.Public, key, dataSource, dataTarget);
	public static final View uiView      = new View(FlowKeyValue.class, PropertyView.Ui, key, dataSource, dataTarget);

	@Override
	public Object get(final Context context) throws FlowException {

		final String _key   = getProperty(key);
		final DataSource _ds = getProperty(dataSource);

		if (_key != null && _ds != null) {

			final Object data = _ds.get(context);
			if (_key.length() > 0) {

				return new KeyValue(_key, data);
			} else {

				logger.warn("Unable to evaluate FlowKeyValue {}, key was empty", getUuid());
			}

		} else {

			logger.warn("Unable to evaluate FlowKeyValue {}, missing at least one source.", getUuid());
		}

		return null;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("key", this.getProperty(key));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}
}
