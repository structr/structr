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
package org.structr.core.script;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;
import org.structr.api.RetryException;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.trace.CallFrame;
import org.structr.common.trace.CallTrace;
import org.structr.common.trace.GuestStackEntry;
import org.structr.common.trace.ScriptError;
import org.structr.common.trace.ScriptFrame;
import org.structr.common.trace.ScriptIdentity;
import org.structr.common.trace.SourcePosition;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Turns failures of user code into {@link ScriptError}s: once, where the failure first leaves a
 * script, with its location taken from what the script engine reports.
 *
 * <p>A failure that already carries a script error keeps it. A failure caused by a deadlock is never
 * attributed: it is not a script error, and the outermost transaction is repeated. A failed assertion
 * is attributed like any other failure, and marked as one.</p>
 */
public final class ScriptFailures {

	private ScriptFailures() {}

	/**
	 * Attributes a failure that left the evaluation of the given snippet to the frame running it,
	 * unless it is attributed already or is a deadlock, and attaches the script error to the given
	 * exception, which is the one that carries the failure on.
	 */
	static <T extends Throwable> T attributeEvaluation(final PolyglotException ex, final Snippet snippet, final T carrier) {

		final Throwable failure = ex.isHostException() ? ex.asHostException() : ex;

		attributeOnce(failure, carrier, () -> {

			final CallFrame current = CallTrace.current();
			final ScriptError error = fromPolyglot(evaluationFrame(current, snippet), ex);

			// with no frame running, no frame exit will settle it
			if (current == null) {

				error.report();
			}

			return error;
		});

		return carrier;
	}

	/**
	 * Reports a syntax error in a StructrScript expression, at the row and column the parser stopped.
	 */
	static void reportSyntaxError(final CallFrame frame, final String message, final int row, final int column) {

		ScriptError.attribute(frame, message, new SourcePosition(row, column), false).report();
	}

	/**
	 * The script error of a failure that left guest code somewhere other than an evaluation, such as a
	 * function a script handed to doInNewTransaction() or schedule(): the one it carries already, null
	 * for a deadlock, or a new one attributed to the given frame.
	 */
	public static ScriptError attribute(final CallFrame frame, final Throwable failure) {

		return attributeOnce(failure, () -> failure instanceof PolyglotException pex

			? fromPolyglot(frame, pex)
			: ScriptError.attribute(frame, String.valueOf(failure), isAssertion(failure))
		);
	}

	/**
	 * Attributes a failure that left guest code to the given frame, unless it is attributed already,
	 * and reports it. Says whether it was a script error; a deadlock is not.
	 */
	public static boolean report(final CallFrame frame, final Throwable failure) {

		final ScriptError error = attribute(frame, failure);
		if (error != null) {

			error.report();

			return true;
		}

		return false;
	}

	/**
	 * The script error of a failure that left a StructrScript function: the one it carries already,
	 * null for a deadlock, or a new one at the row and column of the function, which the parser records
	 * before each call.
	 */
	public static ScriptError attributeStructrScript(final ActionContext actionContext, final Throwable failure) {

		final String message = failure instanceof FrameworkException fex ? fex.toString() : String.valueOf(failure.getMessage());

		final SourcePosition at = new SourcePosition(actionContext.getScriptRow(), actionContext.getScriptColumn());

		return attributeOnce(failure, () -> ScriptError.attribute(CallTrace.current(), message, at, isAssertion(failure)));
	}

	/**
	 * Attributes the failure a rejected promise was turned into, at the JavaScript error it was rejected
	 * with if there is one, since only that knows where it was thrown.
	 */
	public static void attributeRejection(final Exception failure, final PolyglotException guestError) {

		if (ScriptError.of(failure) == null) {

			final ScriptError error = attribute(CallTrace.current(), guestError != null ? guestError : failure);
			if (error != null) {

				error.attachTo(failure);
			}
		}
	}

	/**
	 * Marks the script error the given failure carries, if any, as going back into the calling
	 * script, where a try/catch may catch it.
	 */
	public static <T extends Throwable> T thrownIntoScript(final T failure) {

		final ScriptError error = ScriptError.of(failure);
		if (error != null) {

			error.thrownIntoScript();
		}

		return failure;
	}

	// ----- private methods -----
	private static ScriptError attributeOnce(final Throwable failure, final Supplier<ScriptError> attribution) {

		return attributeOnce(failure, failure, attribution);
	}

	/**
	 * The script error the given failure carries already, null for a deadlock, or a new one, attached to
	 * the exception that carries the failure on.
	 */
	private static ScriptError attributeOnce(final Throwable failure, final Throwable carrier, final Supplier<ScriptError> attribution) {

		final ScriptError existing = ScriptError.of(failure);
		if (existing != null) {

			// a failure that is passed on in another exception keeps its script error there
			if (carrier != failure) {

				existing.attachTo(carrier);
			}

			return existing;
		}

		// a deadlock is not a script error: the request is repeated
		if (RetryException.find(failure) != null) {

			return null;
		}

		final ScriptError error = attribution.get();

		error.attachTo(carrier);

		return error;
	}

	/**
	 * The frame a failing evaluation ran in. An evaluation that did not come through evaluate() has no
	 * frame of its own; it gets one for the report, as a child of whatever is running.
	 */
	private static CallFrame evaluationFrame(final CallFrame current, final Snippet snippet) {

		if (current instanceof ScriptFrame script && Objects.equals(script.getSourceName(), snippet.getName())) {

			return current;
		}

		final ScriptIdentity identity = ScriptIdentity.builder(snippet.getName())
			.codeSource(snippet.getCodeSource(), null)
			.startRow(snippet.getStartRow())
			.build();

		return new ScriptFrame(new ScriptFrame.Spec(snippet.getName(), identity, null));
	}

	private static boolean isAssertion(final Throwable failure) {

		if (failure instanceof PolyglotException pex && pex.isHostException()) {

			return pex.asHostException() instanceof AssertException;
		}

		return failure instanceof AssertException;
	}

	private static ScriptError fromPolyglot(final CallFrame origin, final PolyglotException ex) {

		final SourceSection location = ex.isGuestException() ? ex.getSourceLocation() : null;
		final boolean hasLocation    = location != null && location.isAvailable();
		final SourcePosition start   = hasLocation ? new SourcePosition(location.getStartLine(), location.getStartColumn()) : null;
		final SourcePosition end     = hasLocation ? new SourcePosition(location.getEndLine(), location.getEndColumn()) : null;

		return ScriptError.attribute(origin, ex.getMessage(), start, end, guestStack(ex), isAssertion(ex));
	}

	/**
	 * Where each evaluation running on the thread is. GraalVM's stack trace of a failure holds every
	 * guest frame on the thread, the callers' included. Consecutive guest frames belong to one
	 * evaluation, and every nested evaluation is entered through host code, so a host frame separates
	 * them; the first frame of each run of guest frames is where that evaluation currently is.
	 */
	private static List<GuestStackEntry> guestStack(final PolyglotException ex) {

		final List<GuestStackEntry> stack = new ArrayList<>();
		boolean inRun                     = false;

		for (final PolyglotException.StackFrame frame : ex.getPolyglotStackTrace()) {

			if (frame.isGuestFrame()) {

				if (!inRun) {

					final SourceSection location = frame.getSourceLocation();
					if (location != null && location.isAvailable()) {

						stack.add(new GuestStackEntry(location.getSource().getName(), new SourcePosition(location.getStartLine(), location.getStartColumn())));

					} else {

						stack.add(new GuestStackEntry(null, null));
					}

					inRun = true;
				}

			} else {

				inRun = false;
			}
		}

		return stack;
	}
}
