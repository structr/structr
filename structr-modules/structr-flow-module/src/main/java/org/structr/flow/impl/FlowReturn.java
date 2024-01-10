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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.Return;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.flow.impl.rels.FlowExceptionHandlerNodes;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FlowReturn extends FlowNode implements Return, DeployableEntity, ThrowingElement {

	public static final Property<DataSource> dataSource = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<FlowExceptionHandler> exceptionHandler 	= new EndNode<>("exceptionHandler", FlowExceptionHandlerNodes.class);
	public static final Property<String> result = new StringProperty("result");

	public static final View defaultView = new View(FlowReturn.class, PropertyView.Public, result, dataSource, exceptionHandler, isStartNodeOfContainer);
	public static final View uiView      = new View(FlowReturn.class, PropertyView.Ui,     result, dataSource, exceptionHandler, isStartNodeOfContainer);

	@Override
	public Object getResult(final Context context) throws FlowException {

		final DataSource ds = getProperty(dataSource);
		final String _script = getProperty(result);

		String script = _script;
		if (script == null || script.equals("")) {
			script = "data";
		}

		if (ds != null) {
			context.setData(getUuid(), ds.get(context));
		}

		try {
			return Scripting.evaluate(context.getActionContext(securityContext, this), context.getThisObject(), "${" + script.trim() + "}", "FlowReturn(" + getUuid() + ")");

		} catch (FrameworkException fex) {

			throw new FlowException(fex, this);
		}

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
		result.put("result", this.getProperty(FlowReturn.result));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}
}
