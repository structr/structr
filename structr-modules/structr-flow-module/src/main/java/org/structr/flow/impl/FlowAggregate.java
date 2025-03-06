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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.flow.api.Aggregation;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowAggregate extends FlowNode implements Aggregation, DataSource, DeployableEntity, ThrowingElement {

	public FlowAggregate(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getScript() {
		return wrappedObject.getProperty(traits.key("script"));
	}

	public DataSource getStartValueSource() {

		final NodeInterface startValueSource = wrappedObject.getProperty(traits.key("startValueSource"));
		if (startValueSource != null) {

			return startValueSource.as(DataSource.class);
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
	public void aggregate(final Context context) throws FlowException {

		try {

			final String _script        = getScript();
			final DataSource ds         = getDataSource();
			final DataSource startValue = getStartValueSource();

			if (_script != null && startValue != null && ds != null) {

				if (context.getData(getUuid()) == null) {
					context.setData(getUuid(), startValue.get(context));
				}

				context.setAggregation(getUuid(), ds.get(context));

				Object result = Scripting.evaluate(context.getActionContext(getSecurityContext(), this), this, "${" + _script.trim() + "}", "FlowAggregate(" + getUuid() + ")");

				context.setData(getUuid(), result);

			}

		} catch (FrameworkException ex) {

			throw new FlowException(ex, this);
		}

	}

	@Override
	public Object get(final Context context) throws FlowException {

		if (context.getData(getUuid()) == null) {

			DataSource startValue = getStartValueSource();

			if (startValue != null) {

				context.setData(getUuid(), startValue.get(context));
			}

		}

		return context.getData(getUuid());
	}

	@Override
	public FlowExceptionHandler getExceptionHandler(final Context context) {
		return getExceptionHandler();
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("script",                      getScript());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
