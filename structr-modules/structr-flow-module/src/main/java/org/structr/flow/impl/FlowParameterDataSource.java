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

import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FlowParameterDataSource extends FlowBaseNode implements DataSource, DeployableEntity {

	public FlowParameterDataSource(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getKey() {
		return wrappedObject.getProperty(traits.key("key"));
	}

	@Override
	public Object get(Context context) throws FlowException {

		final String _key = getKey();
		if (_key != null) {

			if (_key.contains(".")) {

				final List<String> parts = Arrays.stream(_key.split("\\.")).collect(Collectors.toList());
				if (!parts.isEmpty()) {

					Object entity = context.getParameter(parts.get(0));
					parts.remove(0);

					return resolveParts(entity, parts);

				} else {

					return null;
				}
			}
			return context.getParameter(_key);
		}

		return null;
	}

	private Object resolveParts(Object obj, List<String> parts) {

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

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("key",                         getKey());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
