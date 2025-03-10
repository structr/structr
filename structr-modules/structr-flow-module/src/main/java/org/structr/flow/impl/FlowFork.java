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
import org.structr.flow.api.DataSource;
import org.structr.flow.api.Fork;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowFork extends FlowNode implements Fork, DataSource, DeployableEntity, ThrowingElement {

	public FlowFork(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public FlowNode getForkBody() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("loopBody"));
		if (node != null) {

			return node.as(FlowNode.class);
		}

		return null;
	}

	public FlowExceptionHandler getExceptionHandler() {

		final NodeInterface exceptionHandler = wrappedObject.getProperty(traits.key("exceptionHandler"));
		if (exceptionHandler != null) {

			return exceptionHandler.as(FlowExceptionHandler.class);
		}

		return null;
	}

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return getExceptionHandler();
	}

	@Override
	public void handle(final Context context) throws FlowException {

		// Call get while handling the fork process to clear local data and cache given data from dataSource
		context.setData(getUuid(), null);

		FlowDataSource _ds = getDataSource();
		if (_ds != null) {

			context.setData(getUuid(), _ds.get(context));
		}

	}

	@Override
	public Object get(Context context) throws FlowException {

		Object data = context.getData(getUuid());
		if (data == null) {

			FlowDataSource _ds = getDataSource();
			if (_ds != null) {

				data = _ds.get(context);
				context.setData(getUuid(), data);
			}

		}

		return data;

	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
