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

import org.graalvm.polyglot.PolyglotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One failure of user code, attributed to the frame it happened in.
 *
 * <p>Created once, where the failure first leaves a script evaluation, and carried on the exception
 * from then on ({@link ScriptErrorCarrier}).
 * An exception that already carries one is not attributed again, which is what keeps a failure in a
 * deeply nested call from being reported at every level it passes.</p>
 *
 * <p>Whether and where it is reported is decided by the frames it leaves, see {@link CallTrace}.
 * Reporting itself is delegated to the {@link Reporter} installed by the scripting layer.</p>
 */
public final class ScriptError {

	/**
	 * Writes the log line, the runtime event and the UI notification for an error, and looks up the
	 * node holding failing code, which only the layers above the trace can do.
	 */
	public interface Reporter {

		void report(final ScriptError error);

		/**
		 * The node with the given uuid, or null if there is none.
		 */
		CodeSourceInfo resolve(final String codeSourceId);
	}

	/**
	 * Exceptions that are not a {@link ScriptErrorCarrier} cannot carry a script error, so it is
	 * remembered for them here, weakly and by identity: Throwable does not override equals().
	 */
	private static final Map<Throwable, ScriptError> foreign = Collections.synchronizedMap(new WeakHashMap<>());

	private static final Logger logger        = LoggerFactory.getLogger(ScriptError.class);
	private static volatile Reporter reporter = null;

	private final AtomicReference<State> state    = new AtomicReference<>(State.PENDING);
	private final Map<CallFrame, SourcePosition> callSites;
	private final CallFrame origin;
	private final String message;
	private final SourcePosition start;
	private final SourcePosition end;
	private final boolean assertion;
	private volatile CodeSourceInfo codeSource    = null;
	private volatile boolean codeSourceResolved   = false;
	private boolean thrownIntoScript              = false;

	private enum State {
		PENDING, REPORTED, HANDLED, DROPPED
	}

	private ScriptError(final CallFrame origin, final String message, final SourcePosition start, final SourcePosition end, final Map<CallFrame, SourcePosition> callSites, final boolean assertion) {

		this.origin    = origin;
		this.message   = message;
		this.start     = known(start);
		this.end       = this.start != null && known(end) != null ? end : this.start;
		this.callSites = callSites;
		this.assertion = assertion;
	}

	/**
	 * Attributes a failure that left guest code. Its location is where it happened: the guest
	 * exception's own location if it has one, otherwise the position of the origin's evaluation on the
	 * guest stack, which is where a host exception (a failing built-in function, or an error from a
	 * nested call) entered the script. The same stack gives each calling frame the line it made its
	 * call from, as far as those callers are scripts.
	 *
	 * @param start     the start of the guest exception's own location, or null if it has none
	 * @param end       the end of that location, or null
	 * @param stack     where each evaluation running on the thread is, innermost first
	 * @param assertion whether the failure is a failed assertion, which is the script's answer rather than a fault
	 */
	public static ScriptError attribute(final CallFrame origin, final String message, final SourcePosition start, final SourcePosition end, final List<GuestStackEntry> stack, final boolean assertion) {

		final CallFrame failed                         = originOrDefault(origin);
		final Map<CallFrame, SourcePosition> callSites = callSites(failed, stack);

		// the origin's own position on the stack is where the failure entered it, not a call site
		final SourcePosition entered = callSites.remove(failed);

		if (start != null) {

			return new ScriptError(failed, message, start, end, callSites, assertion);
		}

		return new ScriptError(failed, message, entered, null, callSites, assertion);
	}

	/**
	 * Attributes a failure that has no location, such as one that left Java code a script handed work to.
	 */
	public static ScriptError attribute(final CallFrame origin, final String message, final boolean assertion) {

		return attribute(origin, message, null, assertion);
	}

	/**
	 * Attributes a failure whose location is known without a stack trace, such as a StructrScript
	 * syntax error.
	 *
	 * @param at where the failure happened, or null if unknown
	 */
	public static ScriptError attribute(final CallFrame origin, final String message, final SourcePosition at, final boolean assertion) {

		return new ScriptError(originOrDefault(origin), message, at, null, Map.of(), assertion);
	}

	/**
	 * The script error carried by the given exception or one of its causes, or null.
	 */
	public static ScriptError of(final Throwable throwable) {

		Throwable current = throwable;

		for (int i = 0; current != null && i < 16; i++) {

			if (current instanceof ScriptErrorCarrier carrier && carrier.getScriptError() != null) {

				return carrier.getScriptError();
			}

			final ScriptError remembered = foreign.get(current);
			if (remembered != null) {

				return remembered;
			}

			if (current instanceof PolyglotException pex && pex.isHostException()) {

				final ScriptError fromHost = of(pex.asHostException());
				if (fromHost != null) {

					return fromHost;
				}
			}

			if (current.getCause() == current) {
				break;
			}

			current = current.getCause();
		}

		return null;
	}

	/**
	 * Reports the script error the given failure carries, if it carries one, and says whether it did.
	 * For code that catches a failure and logs it: a failure of user code is reported here, once, with
	 * where it happened and the calls that led there, and needs no log line of its own.
	 *
	 * <pre>
	 * if (!ScriptError.reportIfPresent(t)) {
	 *     logger.warn("...", t);
	 * }
	 * </pre>
	 */
	public static boolean reportIfPresent(final Throwable failure) {

		final ScriptError error = of(failure);
		if (error != null) {

			error.report();

			return true;
		}

		return false;
	}

	/**
	 * Marks the script error the given failure carries, if it carries one, as handled, and says whether
	 * it did. For code to which the failure is the answer rather than a fault, such as an error handler
	 * or a probe: the failure is then never reported.
	 */
	public static boolean handleIfPresent(final Throwable failure) {

		final ScriptError error = of(failure);
		if (error != null) {

			error.handled();

			return true;
		}

		return false;
	}

	/**
	 * Carries the script error of one exception over to another that replaces it, so that wrapping a
	 * failure does not make it look new.
	 */
	public static <T extends Throwable> T carry(final Throwable from, final T to) {

		final ScriptError error = of(from);
		if (error != null) {

			error.attachTo(to);
		}

		return to;
	}

	/**
	 * Attaches this error to the exception that carries it on.
	 */
	public <T extends Throwable> T attachTo(final T throwable) {

		if (throwable instanceof ScriptErrorCarrier carrier) {

			if (carrier.getScriptError() == null) {

				carrier.setScriptError(this);
			}

		} else if (throwable != null) {

			foreign.putIfAbsent(throwable, this);
		}

		return throwable;
	}

	public static void setReporter(final Reporter reporter) {

		ScriptError.reporter = reporter;
	}

	// ----- state transitions -----
	/**
	 * Reports this error unless it has been settled already. Code that logs a failure of user code
	 * itself calls this instead, so that it is not reported twice.
	 */
	public void report() {

		// the exception carrying it may be caught on another thread than the one it failed on
		if (!state.compareAndSet(State.PENDING, State.REPORTED)) {
			return;
		}

		final Reporter current = reporter;
		if (current != null) {

			current.report(this);

		} else {

			logger.error(format());
		}
	}

	/**
	 * Marks this error as handled by code that treats the failure as an answer rather than a fault, so
	 * that it is never reported.
	 */
	public void handled() {

		state.compareAndSet(State.PENDING, State.HANDLED);
	}

	/**
	 * Records that this error is being thrown into the script code of the current frame, where a
	 * try/catch may catch it.
	 */
	public void thrownIntoScript() {

		this.thrownIntoScript = true;
	}

	public boolean isSettled() {

		return state.get() != State.PENDING;
	}

	public boolean isReported() {

		return state.get() == State.REPORTED;
	}

	boolean wasThrownIntoScript() {

		return thrownIntoScript;
	}

	void dropped() {

		state.compareAndSet(State.PENDING, State.DROPPED);
	}

	void movedOn() {

		// the frame it moves to has not seen it in its script code yet
		this.thrownIntoScript = false;
	}

	// ----- public methods -----
	public CallFrame getOrigin() {
		return origin;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * Whether the failure is a failed assertion ({@code $.assert}), which is the script's answer rather
	 * than a fault.
	 */
	public boolean isAssertion() {
		return assertion;
	}

	/**
	 * Where the failure starts in the code source, counting the start row of a template expression, or
	 * null if unknown.
	 */
	public SourcePosition getStart() {
		return inCodeSource(start);
	}

	/**
	 * Where the failure ends in the code source, see {@link #getStart()}.
	 */
	public SourcePosition getEnd() {
		return inCodeSource(end);
	}

	/**
	 * The line in the code source, counting the start row of a template expression, or 0 if unknown.
	 */
	public int getLine() {
		return start != null ? getStart().line() : 0;
	}

	public int getColumn() {
		return start != null ? start.column() : 0;
	}

	public int getEndLine() {
		return end != null ? getEnd().line() : 0;
	}

	public int getEndColumn() {
		return end != null ? end.column() : 0;
	}

	/**
	 * The line and column the given calling frame made its call from, counting the frame's start
	 * row, or null if not known.
	 */
	public SourcePosition getCallSite(final CallFrame frame) {

		final SourcePosition site = callSites.get(frame);
		if (site != null) {

			return site.plusRows(startRow(frame));
		}

		return null;
	}

	/**
	 * The node holding the failing code, or null if the origin names none or it does not exist. Looked
	 * up the first time it is asked for, which is when the error is reported or formatted: most errors
	 * a script catches never need it.
	 */
	public CodeSourceInfo getCodeSource() {

		if (!codeSourceResolved) {

			final String id         = origin instanceof ScriptFrame script ? script.getCodeSourceId() : null;
			final Reporter resolver = reporter;

			codeSource         = id != null && resolver != null ? resolver.resolve(id) : null;
			codeSourceResolved = true;
		}

		return codeSource;
	}

	/**
	 * The origin and every frame that called it, each with its location, innermost first.
	 */
	public List<Map<String, Object>> getTrace() {

		final List<Map<String, Object>> trace = new ArrayList<>();

		for (final CallFrame frame : origin.chain()) {

			final Map<String, Object> map = frame.toMap();
			final SourcePosition site     = frame == origin ? getStart() : getCallSite(frame);

			map.put("description", frame.describe());

			if (site != null) {

				map.put("row",    site.line());
				map.put("column", site.column());
			}

			trace.add(map);
		}

		return trace;
	}

	/**
	 * The error as it is logged: the origin with its location and the message, then one line for each
	 * calling frame.
	 */
	public String format() {

		final StringBuilder buf = new StringBuilder();

		final CodeSourceInfo codeSource = getCodeSource();

		buf.append(origin.describe(codeSource != null ? codeSource.type() : null));

		final SourcePosition at = getStart();
		if (at != null) {

			buf.append(":").append(at.line()).append(":").append(at.column());
		}

		buf.append(": ").append(message);

		for (final String caller : getCallers()) {

			buf.append(System.lineSeparator()).append("\tcalled from ").append(caller);
		}

		return buf.toString();
	}

	/**
	 * The frames that called the origin, innermost first, each as it is logged: its name, the line it
	 * made its call from, and how it handed the work on if that was not a direct call.
	 */
	public List<String> getCallers() {

		final List<String> callers = new ArrayList<>();
		CallFrame callee           = origin;

		for (CallFrame frame = origin.getParent(); frame != null; frame = frame.getParent()) {

			final StringBuilder buf   = new StringBuilder(frame.describe());
			final SourcePosition site = getCallSite(frame);

			if (site != null) {

				buf.append(":").append(site.line()).append(":").append(site.column());
			}

			// the callee continued this frame's code on another thread or later
			if (callee.continuationLabel() != null) {

				buf.append(" via ").append(callee.continuationLabel());
			}

			callers.add(buf.toString());

			callee = frame;
		}

		return callers;
	}

	@Override
	public String toString() {
		return format();
	}

	// ----- private methods -----
	private static int startRow(final CallFrame frame) {

		return frame instanceof ScriptFrame script ? script.getStartRow() : 0;
	}

	private static CallFrame originOrDefault(final CallFrame origin) {

		return origin != null ? origin : new ScriptFrame(ScriptFrame.Spec.named("script"));
	}

	/**
	 * The given position, or null if it names no line: script engines and the StructrScript parser
	 * report an unknown position as line 0.
	 */
	private static SourcePosition known(final SourcePosition position) {

		return position != null && position.line() > 0 ? position : null;
	}

	private SourcePosition inCodeSource(final SourcePosition position) {

		return position != null ? position.plusRows(startRow(origin)) : null;
	}

	/**
	 * Matches the evaluations on the guest stack to the frames of the trace by source name, innermost
	 * first, skipping entries that belong to no frame (the async wrapper trampoline) and frames that
	 * have no entry (StructrScript, entry points).
	 */
	private static Map<CallFrame, SourcePosition> callSites(final CallFrame origin, final List<GuestStackEntry> stack) {

		final Map<CallFrame, SourcePosition> callSites = new IdentityHashMap<>();
		int next                                       = 0;

		for (CallFrame frame = origin; frame != null && next < stack.size(); frame = frame.getParent()) {

			// only script frames have guest frames of their own
			final String sourceName = frame instanceof ScriptFrame script ? script.getSourceName() : null;
			if (sourceName == null) {
				continue;
			}

			for (int i = next; i < stack.size(); i++) {

				final GuestStackEntry entry = stack.get(i);

				if (sourceName.equals(entry.sourceName()) && entry.position() != null) {

					callSites.put(frame, entry.position());
					next = i + 1;
					break;
				}
			}
		}

		return callSites;
	}
}
