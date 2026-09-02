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
import org.structr.schema.action.AsyncFunctionExecutor;
import org.structr.schema.action.Function;

import java.util.Arrays;

/**
 * The "async" member of a built-in function that allows it, as in {@code $.GET.async(url)}.
 *
 * Calling it starts the function on a worker thread and hands JavaScript a thenable straight away, so
 * the calls of several such invocations are in flight at once and {@code await} costs the slowest of
 * them rather than their sum. The plain call is untouched and still runs on the calling thread.
 *
 * <p>Two boundaries are load-bearing:</p>
 *
 * <ul>
 * <li><b>Arguments are converted before the work is submitted, and the result is converted after the
 * join</b>, both on the JavaScript thread. A {@link Value} cannot be separated from its context, so
 * the worker is handed plain Java values and its plain Java answer is converted back here.</li>
 * <li><b>then() settles during the call, not afterwards.</b> There is no event loop behind a Structr
 * script, so a promise that only settles later never settles at all -- see
 * {@code PolyglotWrapper.unwrapThenable}, which invokes then() synchronously and treats "did not
 * settle" as an error. Joining inside then() is what makes both {@code await} and returning the value
 * from an unwrapped snippet work.</li>
 * </ul>
 *
 * The concurrency comes from the calls started before the first await, not from the join itself.
 */
public class AsyncFunctionWrapper<T, R> implements ProxyExecutable {

	private final ActionContext actionContext;
	private final GraphObject entity;
	private final Function<T, R> func;

	public AsyncFunctionWrapper(final ActionContext actionContext, final GraphObject entity, final Function<T, R> func) {

		this.actionContext = actionContext;
		this.entity        = entity;
		this.func          = func;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object execute(final Value... arguments) {

		// on the JavaScript thread: these Values belong to this context and the worker may not see them
		final T[] args = (T[]) Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();

		return new Thenable(AsyncFunctionExecutor.submit(actionContext, entity, func, args));
	}

	/**
	 * What JavaScript receives: an object with a then member, which is all await and Promise.all need --
	 * both route a foreign value through Promise.resolve(), which accepts any thenable.
	 *
	 * It is deliberately not a full promise. {@code catch} and {@code finally} are absent rather than
	 * half-implemented, because a catch that does not answer something chainable breaks chaining worse
	 * than not having one; {@code Promise.resolve(p).catch(...)} is the chaining form.
	 */
	private class Thenable implements ProxyObject {

		private final AsyncFunctionExecutor.PendingCall<R> pending;

		private Thenable(final AsyncFunctionExecutor.PendingCall<R> pending) {

			this.pending = pending;
		}

		@Override
		public Object getMember(final String key) {

			if ("then".equals(key)) {

				return (ProxyExecutable) arguments -> settle(arguments);
			}

			return null;
		}

		/**
		 * Joins the call and hands the outcome to whichever callback applies.
		 *
		 * The join is done into a local first, so that a fulfil callback which itself throws cannot also
		 * be reported as a rejection -- settling twice is worse than either outcome.
		 *
		 * A failure is passed to the reject callback when there is one and thrown when there is not. Both
		 * paths matter: await always supplies both callbacks, while throwing is what a caller with neither
		 * needs. Throwing unconditionally instead would make a failing call answer null through
		 * unwrapThenable, whose outer handler re-raises only ThenableFailure and swallows the rest.
		 */
		private Object settle(final Value... arguments) {

			Object value             = null;
			RuntimeException failure = null;

			try {

				// wrapped here, on the JavaScript thread, for the same reason the arguments were unwrapped there
				value = PolyglotWrapper.wrap(actionContext, pending.await());

			} catch (final FrameworkException fex) {

				// exactly what the synchronous FunctionWrapper.execute does with one
				failure = new RuntimeException(fex);

			} catch (final RuntimeException rex) {

				failure = rex;
			}

			if (failure != null) {

				if (arguments.length > 1 && arguments[1].canExecute()) {

					arguments[1].execute(failure);

					return null;
				}

				throw failure;
			}

			if (arguments.length > 0 && arguments[0].canExecute()) {

				arguments[0].execute(value);
			}

			return null;
		}

		@Override
		public boolean hasMember(final String key) {

			return "then".equals(key);
		}

		@Override
		public Object getMemberKeys() {

			return new String[] { "then" };
		}

		@Override
		public void putMember(final String key, final Value value) {

			throw new UnsupportedOperationException("Cannot add members to the pending result of " + func.getName() + ".async().");
		}
	}
}
