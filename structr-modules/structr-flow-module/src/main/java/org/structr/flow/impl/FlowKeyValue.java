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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowKeyValue extends FlowBaseNode implements DataSource, DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowKeyValue.class);

	public FlowKeyValue(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getKey() {
		return wrappedObject.getProperty(traits.key("key"));
	}

	@Override
	public Object get(final Context context) throws FlowException {

		final String _key    = getKey();
		final DataSource _ds = getDataSource();

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

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("key",                         getKey());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
