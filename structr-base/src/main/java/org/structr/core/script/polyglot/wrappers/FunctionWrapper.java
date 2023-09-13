/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Arrays;

public class FunctionWrapper<T,R> implements ProxyExecutable {

	private final ActionContext actionContext;
	private final GraphObject entity;
	private final Function<T,R> func;

	public FunctionWrapper(final ActionContext actionContext, final GraphObject entity, final Function<T, R> func) {

		this.actionContext = actionContext;
		this.entity        = entity;
		this.func          = func;
	}

	@Override
	@SuppressWarnings("unchecked")
	public R execute(Value... arguments) {

		try {
			T[] args = (T[]) Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

			return (R) PolyglotWrapper.wrap(actionContext, func.apply(actionContext, entity, args));

		} catch (FrameworkException ex) {

			throw new RuntimeException(ex);
		}
	}
}
