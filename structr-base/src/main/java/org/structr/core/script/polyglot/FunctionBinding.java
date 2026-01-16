/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.script.polyglot.wrappers.FunctionWrapper;
import org.structr.schema.action.ActionContext;

public class FunctionBinding implements ProxyObject {

	private GraphObject entity                   = null;
	private ActionContext actionContext          = null;

	public FunctionBinding(final ActionContext actionContext, final GraphObject entity) {

		this.entity = entity;
		this.actionContext = actionContext;
	}

	@Override
	public Object getMember(final String name) {

		if (hasMember(name)) {
			return new FunctionWrapper(actionContext, entity, Functions.get(name));
		}

		return null;
	}

	@Override
	public Object getMemberKeys() {
		return Functions.getNames();
	}

	@Override
	public boolean hasMember(final String key) {
		return (Functions.get(key) != null);
	}

	@Override
	public void putMember(final String key, final Value value) {
	}
}