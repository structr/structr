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
import org.structr.flow.api.Aggregation;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.ThrowingElement;
import org.structr.module.api.DeployableEntity;

public interface FlowAggregate extends FlowNode, Aggregation, DataSource, DeployableEntity, ThrowingElement {

	void setScript(final String script) throws FrameworkException;

	/*

	public static final Property<DataSource> dataSource                 = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget     = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<DataSource> startValueSource           = new StartNode<>("startValue", FlowAggregateStartValue.class);
	public static final Property<FlowExceptionHandler> exceptionHandler = new EndNode<>("exceptionHandler", FlowExceptionHandlerNodes.class);
	public static final Property<String> script                         = new StringProperty("script");

	public static final View defaultView 									= new View(FlowAction.class, PropertyView.Public, script, startValueSource, dataSource, dataTarget, exceptionHandler, isStartNodeOfContainer);
	public static final View uiView      									= new View(FlowAction.class, PropertyView.Ui,     script, startValueSource, dataSource, dataTarget, exceptionHandler, isStartNodeOfContainer);

	@Override
	public void aggregate(Context context) throws FlowException {

		try {

			String _script = getProperty(script);
			DataSource ds = getProperty(dataSource);
			DataSource startValue = getProperty(startValueSource);

			if (_script != null && startValue != null && ds != null) {

				if (context.getData(getUuid()) == null) {
					context.setData(getUuid(), startValue.get(context));
				}

				context.setAggregation(getUuid(), ds.get(context));

				Object result = Scripting.evaluate(context.getActionContext(securityContext, this), this, "${" + _script.trim() + "}", "FlowAggregate(" + getUuid() + ")");

				context.setData(getUuid(), result);

			}

		} catch (FrameworkException ex) {

			throw new FlowException(ex, this);
		}

	}

	@Override
	public Object get(Context context) throws FlowException {

		if (context.getData(getUuid()) == null) {

			DataSource startValue = getProperty(startValueSource);

			if (startValue != null) {

				context.setData(getUuid(), startValue.get(context));
			}

		}

		return context.getData(getUuid());
	}

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return getProperty(exceptionHandler);
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("script", this.getProperty(script));

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}
	*/
}
