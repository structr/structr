/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.script;

import org.structr.common.CaseHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.awt.*;

public class StructrScriptObject {

	private GraphObject entity          = null;
	private ActionContext actionContext = null;
	
	public StructrScriptObject(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext = actionContext;
		this.entity        = entity;
	}

	public void clear() {
		actionContext.clear();
	}
	
	public Object get(final String name) throws FrameworkException {
		
		if ("this".equals(name)) {
			return entity;
		}

		if ("me".equals(name)) {
			return actionContext.getSecurityContext().getUser(false);
		}

		if (actionContext.getConstant(name) != null) {
			return actionContext.getConstant(name);
		}

		if (actionContext.getAllVariables().containsKey(name)) {
			return actionContext.getAllVariables().get(name);
		}

		return null;
	}

	public Object call(final String name, Object... parameters) throws FrameworkException {

		final Function<Object, Object> function = Functions.get(CaseHelper.toUnderscore(name, false));
		if (function != null) {

			return function.apply(actionContext, entity, parameters);
		}

		return null;
	}
}
