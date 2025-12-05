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
package org.structr.flow.impl;

import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.flow.traits.definitions.FlowParameterDataSourceTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.List;
import java.util.Map;

public class FlowParameterDataSource extends FlowDataSource implements DeployableEntity {

	public FlowParameterDataSource(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getKey() {
		return wrappedObject.getProperty(traits.key(FlowParameterDataSourceTraitDefinition.KEY_PROPERTY));
	}

	public Object resolveParts(Object obj, List<String> parts) {

		if (!parts.isEmpty()) {

			Object resolvedPart = getValue(obj, parts.get(0));
			parts.remove(0);
			return resolveParts(resolvedPart, parts);
		}

		return obj;
	}

	private Object getValue(Object obj, String key) {

		if (obj instanceof GraphObject) {

			return getGraphObjectValue((GraphObject) obj, key);

		} else if (obj instanceof Map) {

			return getMapValue((Map<?,?>) obj, key);
		}

		return null;
	}

	private Object getMapValue(Map<?,?> map, String key) {

		if (map.containsKey(key)) {

			return map.get(key);
		}
		return null;
	}

	private Object getGraphObjectValue(final GraphObject go, final String key) {

		final PropertyKey propertyKey = go.getTraits().key(key);

		return go.getProperty(propertyKey);
	}
}
