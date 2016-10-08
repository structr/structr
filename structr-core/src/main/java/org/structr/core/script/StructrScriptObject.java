/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

public class StructrScriptObject {

	private GraphObject entity          = null;
	private ActionContext actionContext = null;
	private Object[] parameters         = null;
	
	public StructrScriptObject(final ActionContext actionContext, final GraphObject entity, final Object[] parameters) {

		this.actionContext = actionContext;
		this.entity        = entity;
		this.parameters    = parameters;
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

//		final Function<Object, Object> function = Functions.functions.get(name);
//		
//		if (function != null) {
//			
//				return function.apply(actionContext, entity, parameters);
//		}
		
		return null;
	}
}
