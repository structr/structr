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

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowGetProperty extends FlowDataSource {

	public FlowGetProperty(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final FlowDataSource getNodeSource() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("nodeSource"));
		if (node != null) {

			return node.as(FlowDataSource.class);
		}

		return null;
	}

	public final FlowDataSource getPropertyNameSource() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("propertyNameSource"));
		if (node != null) {

			return node.as(FlowDataSource.class);
		}

		return null;
	}

	public final String getPropertyName() {
		return wrappedObject.getProperty(traits.key("propertyName"));
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
