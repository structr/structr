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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.traits.definitions.FlowReturnTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowReturn extends FlowNode implements DeployableEntity, ThrowingElement {

	public FlowReturn(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getResult() {
		return wrappedObject.getProperty(traits.key(FlowReturnTraitDefinition.RESULT_PROPERTY));
	}

	public void setResult(final String result) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowReturnTraitDefinition.RESULT_PROPERTY), result);
	}

	public FlowExceptionHandler getExceptionHandler() {

		final NodeInterface exceptionHandler = wrappedObject.getProperty(traits.key(FlowReturnTraitDefinition.EXCEPTION_HANDLER_PROPERTY));
		if (exceptionHandler != null) {

			return exceptionHandler.as(FlowExceptionHandler.class);
		}

		return null;
	}

	public Object getResult(final Context context) throws FlowException {

		final FlowDataSource ds = getDataSource();
		final String _script    = getResult();

		String script = _script;
		if (script == null || script.equals("")) {
			script = "data";
		}

		if (ds != null) {
			context.setData(getUuid(), ds.get(context));
		}

		try {
			return Scripting.evaluate(context.getActionContext(getSecurityContext(), this), context.getThisObject(), "${" + script.trim() + "}", "FlowReturn(" + getUuid() + ")");

		} catch (FrameworkException fex) {

			throw new FlowException(fex, this);
		}

	}

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return getExceptionHandler();
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           getType());
		result.put(FlowReturnTraitDefinition.RESULT_PROPERTY,                          getResult());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, isVisibleToAuthenticatedUsers());

		return result;
	}
}
