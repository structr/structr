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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.flow.impl.rels.FlowExceptionHandlerNodes;
import org.structr.flow.impl.rels.FlowScriptConditionSource;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowScriptCondition extends FlowCondition implements 	DeployableEntity, ThrowingElement {

	public static final Property<DataSource> scriptSource               = new StartNode<>("scriptSource", FlowScriptConditionSource.class);
	public static final Property<DataSource> dataSource                 = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget     = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<FlowExceptionHandler> exceptionHandler = new EndNode<>("exceptionHandler", FlowExceptionHandlerNodes.class);
	public static final Property<String> script                         = new StringProperty("script");

	public static final View defaultView    = new View(FlowScriptCondition.class, PropertyView.Public, script, scriptSource, dataSource, dataTarget, exceptionHandler);
	public static final View uiView         = new View(FlowScriptCondition.class, PropertyView.Ui,     script, scriptSource, dataSource, dataTarget, exceptionHandler);

	public FlowScriptCondition(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Object get(final Context context) throws FlowException {

		try {

			final DataSource _ds = getProperty(dataSource);
			final DataSource _sc = getProperty(scriptSource);
			final String _script = getProperty(script);
			final String _dynamicScript = _sc != null ? (String)_sc.get(context) : null;


			if (_script != null || _dynamicScript != null) {

					if (_ds != null) {
						context.setData(getUuid(), _ds.get(context));
					}

					final String finalScript = _dynamicScript != null ? _dynamicScript : _script;

					Object result =  Scripting.evaluate(context.getActionContext(securityContext, this), context.getThisObject(), "${" + finalScript.trim() + "}", "FlowScriptCondition(" + getUuid() + ")");
					context.setData(getUuid(), result);
					return result;
			}

		} catch (FrameworkException fex) {

			throw new FlowException(fex, this);
		}

		return null;
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
}
