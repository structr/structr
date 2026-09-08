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
import org.structr.core.script.polyglot.PendingThenables;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.AsyncFunctionExecutor;
import org.structr.schema.action.Function;

/**
 * A call that was started on a worker thread, as JavaScript sees it: an object with a then member.
 *
 * A caller asks for this by passing the {@code async} option to a function that allows it -- see
 * {@code Function#isAsyncCapable()} -- and receives it immediately, so several such calls are in
 * flight at once and {@code await} costs the slowest of them rather than their sum. A call without
 * the option runs on the calling thread and answers its result directly.
 *
 * A then member is all await and Promise.all need: both route a foreign value through
 * Promise.resolve(), which accepts any thenable. Deliberately not a full promise -- {@code catch} and
 * {@code finally} are absent rather than half-implemented, since a catch that does not answer
 * something chainable is worse than no catch at all. {@code Promise.resolve(p).catch(...)} is the
 * chaining form.
 *
 * <p>Two boundaries are load-bearing:</p>
 *
 * <ul>
 * <li><b>Arguments are converted before the work is submitted, and the result after the join</b>, both
 * on the JavaScript thread. A {@link Value} cannot be separated from its context, so the worker is
 * handed plain Java values and its plain Java answer is converted back here. {@code FunctionWrapper}
 * has already converted the arguments by the time it starts a call.</li>
 * <li><b>then() defers the join rather than performing it.</b> Joining inline settles each call in the
 * order its reaction was registered, which makes {@code Promise.race} answer its first argument rather
 * than the first call to finish. The settlement is handed to
 * {@link org.structr.core.script.polyglot.PendingThenables} and run by a host-driven drain that takes
 * completed calls first and stops once the script's own promise has settled, so a race costs its
 * winner rather than its slowest element. The drain still happens within the evaluation: there is no
 * event loop behind a Structr script, and a promise that nothing was going to settle never
 * settles.</li>
 * </ul>
 *
 * The concurrency comes from the calls started before the first await, not from the join.
 */
public class PendingCallWrapper<T, R> implements ProxyObject {

	private final ActionContext actionContext;
	private final Function<T, R> func;
	private final AsyncFunctionExecutor.PendingCall<R> pending;

	/**
	 * Starts the call and answers the thenable for it.
	 *
	 * @param args the call's arguments, already converted to plain Java values by the caller
	 */
	public PendingCallWrapper(final ActionContext actionContext, final GraphObject entity, final Function<T, R> func, final T[] args) {

		this.actionContext = actionContext;
		this.func          = func;
		this.pending       = AsyncFunctionExecutor.submit(actionContext, entity, func, args);
	}

	@Override
	public Object getMember(final String key) {

		if ("then".equals(key)) {

			return (ProxyExecutable) arguments -> {

				// Joining here would settle this call before the script has finished registering its
				// reactions on the others, which makes Promise.race answer its first argument rather than
				// the first call to finish. Deferring hands the choice of order to PendingThenables,
				// which takes completed calls first.
				//
				// Nothing is scheduled from here: the drain is driven by the host, from
				// PolyglotWrapper.unwrapThenable, while the script's completion promise is still pending.
				// Only the host can see that stop condition, and a drain scheduled as a guest job would
				// run regardless because the interop boundary drains that queue to empty.
				if (!PendingThenables.hasFrame()) {

					return settle(arguments);
				}

				PendingThenables.defer(pending::isDone, () -> settle(arguments));

				return null;
			};
		}

		return null;
	}

	/**
	 * Joins the call and hands the outcome to whichever callback applies.
	 *
	 * The join result goes into a local first, so that a fulfil callback which itself throws is not
	 * then also reported as a rejection.
	 *
	 * A failure is passed to the reject callback when there is one and thrown when there is not:
	 * {@code await} always supplies both callbacks, while a caller with neither needs the throw.
	 * Throwing unconditionally would make a failing call answer null through
	 * {@code PolyglotWrapper.unwrapThenable}, whose outer handler re-raises only
	 * {@code ThenableFailure} and swallows anything else.
	 */
	private Object settle(final Value... arguments) {

		Object value             = null;
		RuntimeException failure = null;

		try {

			// wrapped here, on the JavaScript thread, for the same reason the arguments were unwrapped there
			value = PolyglotWrapper.wrap(actionContext, pending.await());

		} catch (final FrameworkException fex) {

			// the shape the synchronous FunctionWrapper.execute produces, so the two are indistinguishable
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

		throw new UnsupportedOperationException("Cannot add members to the pending result of " + func.getName() + "().");
	}
}
