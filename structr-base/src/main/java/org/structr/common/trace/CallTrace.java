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
package org.structr.common.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.LogThrottle;

import java.util.List;

/**
 * The call trace of the current thread: which piece of user logic is running, and who called it.
 *
 * <p>Every evaluation of user code enters a {@link CallFrame} and leaves it again. Leaving is also
 * where errors are settled, which is what makes an error reported exactly once:</p>
 *
 * <ul>
 * <li>An error a frame exits with moves on to the parent frame, unless the frame is a boundary or the
 * root, in which case nothing above can catch it any more and it is reported.</li>
 * <li>An error that was thrown back into the frame's script code, see {@link ScriptError#thrownIntoScript()},
 * and that the frame does not exit with, was caught by the script. It is dropped and never logged.</li>
 * <li>Any other error still pending was swallowed by Java code between the two frames without being
 * reported or handled. It is reported when the frame exits, so that no error is lost.</li>
 * </ul>
 *
 * <p>Code runs in a frame through {@link #call(CallFrame, Body)} or {@link #run(CallFrame, VoidBody)},
 * which learn how the frame was left. Only code that cannot be such a lambda -- one that throws more
 * than one checked exception, or returns from the enclosing method -- enters and exits a {@link Scope}
 * itself, and it must record a failure before the scope exits:</p>
 *
 * <pre>
 * final CallTrace.Scope scope = CallTrace.enter(frame);
 * try {
 *     ...
 * } catch (Throwable t) {
 *     scope.failed(t);
 *     throw t;
 * } finally {
 *     scope.exit();
 * }
 * </pre>
 */
public final class CallTrace {

	private static final Logger logger                  = LoggerFactory.getLogger(CallTrace.class);
	private static final LogThrottle unnamedRootLog     = new LogThrottle("Call flow without entry frame", 1);
	private static final LogThrottle staleFrameLog      = new LogThrottle("Call flow left on a thread", 1);
	private static final ThreadLocal<CallFrame> CURRENT = new ThreadLocal<>();

	private CallTrace() {}

	/**
	 * The frame running on this thread, or null if no user logic is running.
	 */
	public static CallFrame current() {

		return CURRENT.get();
	}

	/**
	 * The trace as it is now, to hand to code that runs on another thread or later. A frame's parent
	 * and identity never change, so this is the current frame itself; the errors pending on a frame
	 * are touched only by the thread it is current on.
	 */
	public static CallFrame capture() {

		return CURRENT.get();
	}

	/**
	 * Makes sure nothing runs on this thread before a flow enters Structr on it. Pooled threads serve
	 * one flow after another, so a frame left behind would make the next flow its child, with the
	 * session of whoever ran the previous one.
	 */
	public static void discardStaleFlow(final String entry) {

		final CallFrame stale = CURRENT.get();
		if (stale != null) {

			if (staleFrameLog.allow(stale.kind())) {

				logger.warn("Call flow {} was not left before {} started on this thread; discarding it", stale.describe(), entry);
			}

			CURRENT.remove();
		}
	}

	/**
	 * Makes the given frame the current one until the returned scope is exited. The frame's parent is
	 * whatever it was built with; a frame resumed on a worker thread names a frame of another thread
	 * as its parent and must be a boundary.
	 */
	public static Scope enter(final CallFrame frame) {

		final CallFrame previous = CURRENT.get();

		// every flow should enter through an entry frame; a script frame that starts one is a way into
		// Structr that does not say what it is
		if (frame.isRoot() && !(frame instanceof EntryFrame) && logger.isDebugEnabled() && unnamedRootLog.allow(frame.kind() + frame.getName())) {

			logger.debug("{} started a call flow without an entry frame", frame.describe(), new Throwable("entered here"));
		}

		CURRENT.set(frame);

		return new Scope(frame, previous);
	}

	/**
	 * Runs the given code in the given frame and returns its result. The frame is left when the code
	 * returns or fails; a failure is passed on unchanged.
	 */
	public static <T, E extends Throwable> T call(final CallFrame frame, final Body<T, E> body) throws E {

		final Scope scope = enter(frame);

		try {

			return body.call();

		} catch (Throwable t) {

			scope.failed(t);
			throw t;

		} finally {

			scope.exit();
		}
	}

	/**
	 * Runs the given code in the given frame, see {@link #call(CallFrame, Body)}.
	 */
	public static <E extends Throwable> void run(final CallFrame frame, final VoidBody<E> body) throws E {

		call(frame, () -> {

			body.run();

			return null;
		});
	}

	/**
	 * The given work, run in the given frame: for work handed to another thread or to later, in a
	 * continuation of the frame that was current when it was handed on.
	 */
	public static Runnable inFrame(final CallFrame frame, final Runnable runnable) {

		return () -> run(frame, runnable::run);
	}

	// ----- nested classes -----
	/**
	 * Code that runs in a frame and returns a result.
	 */
	@FunctionalInterface
	public interface Body<T, E extends Throwable> {

		T call() throws E;
	}

	/**
	 * Code that runs in a frame.
	 */
	@FunctionalInterface
	public interface VoidBody<E extends Throwable> {

		void run() throws E;
	}

	/**
	 * A frame entered by {@link #enter(CallFrame)}, until {@link #exit()}. Deliberately not
	 * {@link AutoCloseable}: try-with-resources closes before its catch block runs, so the scope would
	 * not learn the failure it is left with.
	 */
	public static final class Scope {

		private final CallFrame frame;
		private final CallFrame previous;
		private Throwable failure = null;
		private boolean exited    = false;

		private Scope(final CallFrame frame, final CallFrame previous) {

			this.frame    = frame;
			this.previous = previous;
		}

		/**
		 * Records the exception this frame is being left with.
		 */
		public void failed(final Throwable failure) {

			this.failure = failure;
		}

		/**
		 * Leaves the frame, settling the errors pending on it, and makes the previous frame current again.
		 */
		public void exit() {

			if (exited) {
				return;
			}

			exited = true;

			try {

				settle();

			} finally {

				// request threads are pooled, so the outermost frame leaves nothing behind
				if (previous != null) {

					CURRENT.set(previous);

				} else {

					CURRENT.remove();
				}
			}
		}

		private void settle() {

			final ScriptError exiting      = ScriptError.of(failure);
			final List<ScriptError> errors = frame.drainPending();

			if (errors != null) {

				for (final ScriptError error : errors) {

					if (error == exiting || error.isSettled()) {
						continue;
					}

					if (error.wasThrownIntoScript()) {

						// the script caught it
						error.dropped();

					} else {

						// Java code between this frame and the one the error left swallowed it
						error.report();
					}
				}
			}

			if (exiting != null && !exiting.isSettled()) {

				final CallFrame parent = frame.getParent();

				if (frame.isBoundary() || parent == null || parent != previous) {

					exiting.report();

				} else {

					exiting.movedOn();
					parent.addPending(exiting);
				}
			}
		}
	}
}
