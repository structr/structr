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
package org.structr.schema.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * Runs a built-in function off the calling thread, for the functions that allow it.
 *
 * This is the whole of the async mechanism that is not about a particular language, and it knows
 * nothing about any particular function: what makes a call eligible is the function's own answer to
 * {@link Function#isAsyncCapable()}, and everything else here applies to all of them the same way.
 *
 * The point is not to move work off the request thread -- it is that several calls can be in flight
 * at once, so awaiting them costs the slowest rather than their sum. A caller starts each call, then
 * joins them; the join happens on the calling thread, in {@link PendingCall#await()}.
 *
 * Nothing polyglot may reach a worker. The caller is responsible for handing over plain Java values
 * and for converting the answer back, because a guest value cannot be separated from its context.
 */
public final class AsyncFunctionExecutor {

	private static final Logger logger = LoggerFactory.getLogger(AsyncFunctionExecutor.class);

	/**
	 * One virtual thread per call.
	 *
	 * A virtual thread parks instead of blocking while the call waits on a socket, so N calls in flight
	 * cost N sockets rather than N platform threads, and there is no pool size to get wrong. It also
	 * removes the failure mode a shared fixed pool would introduce: one script's slow calls would queue
	 * every other request's async calls behind them, which can make async slower than the synchronous
	 * calls it replaced. Virtual threads are always daemon, so there is nothing to shut down.
	 *
	 * The name matters in a stack dump and in a support ticket -- an unnamed virtual thread reports
	 * itself as VirtualThread[#123]/runnable@ForkJoinPool-1-worker-3, which says nothing.
	 */
	private static final ExecutorService EXECUTOR = Executors.newThreadPerTaskExecutor(
		Thread.ofVirtual().name("structr-async-", 0).factory());

	/**
	 * The bound on calls in flight for the whole instance.
	 *
	 * Acquired inside the task rather than at submit time, so that saturation delays only the join. A
	 * caller-runs policy would instead run the call inline at the point it was started, which changes
	 * when the request actually goes out -- visible in a log and to any test that asserts on ordering.
	 * No deadlock is possible: a worker never submits here, because a function that may run on one is
	 * not allowed to run a script.
	 */
	private static final Semaphore SLOTS = new Semaphore(Math.max(1, Settings.ScriptingAsyncConcurrency.getValue()), true);

	private AsyncFunctionExecutor() {
	}

	/**
	 * Starts the function on a worker thread and answers a handle to join on.
	 *
	 * The caller must already have converted {@code sources} to plain Java values, on its own thread.
	 *
	 * @param actionContext the caller's context; the worker gets {@link ActionContext#detached()} of it
	 * @param entity        the entity the call runs against
	 * @param func          the function to run, which must answer true to {@link Function#isAsyncCapable()}
	 * @param sources       the already-converted arguments
	 * @return a handle whose {@link PendingCall#await()} answers the call's result
	 */
	public static <S, T> PendingCall<T> submit(final ActionContext actionContext, final GraphObject entity, final Function<S, T> func, final S[] sources) {

		// both on the calling thread: the snapshot has to be taken before the script can change anything,
		// and the MDC is thread-local, so the copy has to be made here to be carried over at all
		final ActionContext detached      = actionContext.detached();
		final Map<String, String> mdc     = MDC.getCopyOfContextMap();
		final String name                 = func.getName();

		final Future<T> future = EXECUTOR.submit(() -> {

			if (mdc != null) {

				// without this a warning logged from inside the call -- HttpHelper logs one for relaxed TLS --
				// loses the request correlation that makes it traceable
				MDC.setContextMap(mdc);
			}

			try {

				SLOTS.acquire();

				try {

					return func.apply(detached, entity, sources);

				} finally {

					SLOTS.release();
				}

			} catch (final Throwable t) {

				// a call whose result is never awaited would otherwise fail entirely silently, because
				// nothing ever reads its Future
				logger.debug("Asynchronous call to {}() failed.", name, t);

				throw t;

			} finally {

				MDC.clear();
			}
		});

		return new PendingCall<>(name, future);
	}

	/**
	 * A call that has been started and not yet joined.
	 *
	 * Deliberately not a Future: the only thing a caller may do with it is wait for the answer, and
	 * cancellation is not offered because it could not be honoured -- a call blocked in a socket read
	 * ends when the socket timeout says so.
	 */
	public static final class PendingCall<T> {

		private final String functionName;
		private final Future<T> future;

		private PendingCall(final String functionName, final Future<T> future) {

			this.functionName = functionName;
			this.future       = future;
		}

		/**
		 * The worker's answer, or its failure raised here as the synchronous call would have raised it.
		 *
		 * The translation mirrors the synchronous path exactly, so that what a script catches does not
		 * depend on whether it called the function or its async variant.
		 */
		public T await() throws FrameworkException {

			try {

				return future.get();

			} catch (final ExecutionException ex) {

				final Throwable cause = ex.getCause();

				if (cause instanceof FrameworkException fex) {
					throw fex;
				}

				if (cause instanceof RuntimeException rex) {
					throw rex;
				}

				if (cause instanceof Error err) {
					throw err;
				}

				throw new RuntimeException(cause);

			} catch (final InterruptedException ex) {

				Thread.currentThread().interrupt();

				throw new FrameworkException(422, "Interrupted while waiting for " + functionName + ".async() to complete.");
			}
		}

		public boolean isDone() {

			return future.isDone();
		}

		public String getFunctionName() {

			return functionName;
		}
	}
}
