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
import java.util.List;

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
 * The same member lookup is what exposes {@code async} on a function that allows it -- see
 * {@link AsyncFunctionWrapper}. That variant lives here rather than in the function registry precisely
 * because this wrapper is only reachable from a polyglot language, so StructrScript's own resolution
 * cannot find it.
 */
public class FunctionWrapper<T,R> implements ProxyExecutable, ProxyObject {

	private static final String ASYNC = "async";

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

	@Override
	public Object getMember(final String key) {

		final Function<Object, Object> namespaced = Functions.get(func.getName() + "." + key);
		if (namespaced != null) {

			return new FunctionWrapper(actionContext, entity, namespaced);
		}

		if (isAsyncMember(key)) {

			return new AsyncFunctionWrapper(actionContext, entity, func);
		}

		return null;
	}

	@Override
	public boolean hasMember(final String key) {

		// GraalJS asks this before it reads: answering false here makes $.GET.async undefined, with no
		// error anywhere, so this has to stay in step with getMember
		return Functions.get(func.getName() + "." + key) != null || isAsyncMember(key);
	}

	@Override
	public Object getMemberKeys() {

		final String prefix       = func.getName() + ".";
		final List<String> members = new ArrayList<>(
			Functions.getNames().stream().filter(name -> name.startsWith(prefix)).map(name -> name.substring(prefix.length())).toList());

		if (func.isAsyncCapable()) {

			members.add(ASYNC);
		}

		return members;
	}

	/**
	 * Whether "async" names this function's asynchronous variant.
	 *
	 * Checked after the namespaced lookup above, so a function that really is registered under
	 * {@code <name>.async} would keep that meaning; nothing is today.
	 *
	 * This is why the variant is reachable from JavaScript and not from StructrScript: StructrScript
	 * resolves a dotted name through its own parser, against the function registry, and never asks a
	 * wrapper for a member. Nothing is added to that registry here.
	 */
	private boolean isAsyncMember(final String key) {

		return ASYNC.equals(key) && func.isAsyncCapable();
	}

	@Override
	public void putMember(final String key, final Value value) {

		throw new UnsupportedOperationException("Cannot add members to the built-in function " + func.getName() + ".");
	}
}
