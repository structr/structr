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

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowDataSource extends FlowNode implements DeployableEntity, ThrowingElement {

	public FlowDataSource(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getQuery() {
		return wrappedObject.getProperty(traits.key("query"));
	}

	public void setQuery(final String query) throws FrameworkException {
		wrappedObject.setProperty(traits.key("query"), query);
	}

	public final FlowExceptionHandler getExceptionHandler() {

		final NodeInterface exceptionHandler = wrappedObject.getProperty(traits.key("exceptionHandler"));
		if (exceptionHandler != null) {

			return exceptionHandler.as(FlowExceptionHandler.class);
		}

		return null;
	}

	public void setDataTarget(final Iterable<FlowBaseNode> nodes) throws FrameworkException {
		wrappedObject.setProperty(traits.key("dataTarget"), nodes);
	}

	public final Iterable<FlowBaseNode> getDataTarget() {
		final Iterable<NodeInterface> dataTargets = wrappedObject.getProperty(traits.key("dataTarget"));
		return Iterables.map(n -> n.as(FlowBaseNode.class), dataTargets);
	}

	public final Object get(final Context context) throws FlowException {
		return traits.getMethod(DataSourceOperations.class).get(context, this);
	}

	@Override
	public final FlowExceptionHandler getExceptionHandler(final Context context) {
		return getExceptionHandler();
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("query",                       getQuery());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
