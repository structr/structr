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

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.common.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Set;

public class StructrPolyglotBinding implements ProxyObject {

	private GraphObject entity          = null;
	private ActionContext actionContext = null;

	public StructrPolyglotBinding(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext = actionContext;
		this.entity        = entity;
	}

	@Override
	public Object getMember(String name) {

		if ("this".equals(name)) {
			return StructrPolyglotWrapper.wrap(entity);
		}

		if ("me".equals(name)) {
			return StructrPolyglotWrapper.wrap(actionContext.getSecurityContext().getUser(false));
		}

		if (actionContext.getConstant(name) != null) {
			return StructrPolyglotWrapper.wrap(actionContext.getConstant(name));
		}

		if (actionContext.getAllVariables().containsKey(name)) {
			return StructrPolyglotWrapper.wrap(actionContext.getAllVariables().get(name));
		}

		Function<Object, Object> func = Functions.get(CaseHelper.toUnderscore(name, false));
		if (func != null) {

			return new StructrPolyglotFunctionWrapper(actionContext, entity, func);
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		Set<String> keys = actionContext.getAllVariables().keySet();
		keys.add("this");
		keys.add("me");
		return keys;
	}

	@Override
	public boolean hasMember(String key) {
		return getMember(key) != null;
	}

	@Override
	public void putMember(String key, Value value) {

	}
}
