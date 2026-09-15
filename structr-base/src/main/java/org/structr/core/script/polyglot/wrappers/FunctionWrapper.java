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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.core.function.Functions;
import org.structr.schema.action.Function;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A built-in function as seen from JavaScript.
 *
 * It is executable AND an object, because some functions have a dotted namespace below them:
 * {@code log.warn}, {@code find.equals} and so on are registered under those names. Being both lets
 * {@code $.log('x')} and {@code $.log.warn('x')} work at the same time - the first executes this
 * wrapper, the second asks it for the member "warn" and gets a wrapper for {@code log.warn}.
 *
 * StructrScript needs none of this: its parser resolves a dotted name directly.
 *
 * It is also where a call that asked to run asynchronously is started rather than made -- see
 * {@link PendingCallWrapper}. That decision lives here rather than in the function itself because it is
 * about which thread makes the call, not about what the call does, and because this wrapper is the
 * boundary at which the arguments have been converted and a thenable can be handed back. A call from
 * StructrScript never arrives here at all, so it cannot start one: the function refuses the option
 * instead.
 */
public class FunctionWrapper<T,R> implements ProxyExecutable, ProxyObject {

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

			// on the calling thread, and before anything else: these Values belong to this context, so a
			// worker may not see them, and the function is asked about plain Java values
			T[] args = (T[]) Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

			// isAsyncCapable() first: it is the function's statement about what apply() touches, and asking
			// a function that made no such statement whether these arguments want async is meaningless. A
			// call that asks anyway falls through to the synchronous path, where apply() reports the option
			// as one it does not accept.
			if (func.isAsyncCapable() && func.isAsyncRequested(args)) {

				return (R) new PendingCallWrapper<>(actionContext, entity, func, args);
			}

			return (R) PolyglotWrapper.wrap(actionContext, func.apply(actionContext, entity, args));

		} catch (FrameworkException ex) {

			throw new RuntimeException(ex);
		}
	}

	@Override
	public Object getMember(final String key) {

		final Function<Object, Object> namespaced = Functions.get(func.getName() + "." + key);
		if (namespaced != null) {

			return new FunctionWrapper(actionContext, entity, namespaced);
		}

		return null;
	}

	@Override
	public boolean hasMember(final String key) {

		// GraalJS asks this before it reads, so a member this answers false for is undefined with no error
		// anywhere: it has to stay in step with getMember

		return Functions.get(func.getName() + "." + key) != null;
	}

	@Override
	public Object getMemberKeys() {

		final String prefix = func.getName() + ".";

		return new ArrayList<>(Functions.getNames().stream().filter(name -> name.startsWith(prefix)).map(name -> name.substring(prefix.length())).toList());
	}

	@Override
	public void putMember(final String key, final Value value) {

		throw new UnsupportedOperationException("Cannot add members to the built-in function " + func.getName() + ".");
	}
}
