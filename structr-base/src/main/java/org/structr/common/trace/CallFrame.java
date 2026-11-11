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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One step of a call flow: where it entered Structr, a script evaluation, or the continuation of
 * a flow on another thread or after its transaction.
 *
 * <p>Every frame points at the frame that led to it, so a frame together with its parents is the
 * whole trace at that moment, and handing a trace to another thread is handing over one reference
 * (see {@link CallTrace#capture()}). The identifying state of a frame is immutable.</p>
 *
 * <p>A flow has exactly one root. A frame attaches itself to whatever runs on the thread when it is
 * created, so it only becomes a root when nothing runs yet; there is no way to start a second root
 * inside a running flow. Only a {@link ContinuationFrame}, or a script frame continuing captured code,
 * names its parent explicitly: the captured frame, which keeps the flow's original root.</p>
 *
 * <p>Subclasses say what the step is. Modules add their own, which is why this class is open.</p>
 *
 * <p>The errors pending on a frame are mutable and only ever touched by the thread that executes it.
 * A frame continuing another thread's frame is a boundary, so errors never travel back into the frames
 * of the thread that captured them.</p>
 */
public abstract class CallFrame {

	private final CallFrame parent;
	private final String name;
	private final String sessionId;
	private final boolean boundary;
	private final int depth;

	private List<ScriptError> pending = null;

	/**
	 * A frame called by whatever runs on this thread now, or the root of a new flow if nothing does.
	 * The session is the one errors are shown to; a frame keeps the session of the flow it belongs to
	 * and takes the given one only if the flow has none.
	 */
	protected CallFrame(final String name, final String sessionId, final boolean boundary) {

		this(CallTrace.current(), name, sessionId, boundary);
	}

	/**
	 * A frame continuing the given frame, which may belong to another thread or a finished
	 * transaction. Only continuations use this.
	 */
	protected CallFrame(final CallFrame continued, final String name, final String sessionId, final boolean boundary) {

		this.parent    = continued;
		this.name      = name;
		this.boundary  = boundary;
		this.depth     = continued != null ? continued.depth + 1 : 0;
		this.sessionId = continued != null && continued.sessionId != null ? continued.sessionId : sessionId;
	}

	/**
	 * What this step is, as a stable identifier: {@code METHOD}, {@code HTTP}, {@code JOB}, ...
	 */
	public abstract String kind();

	/**
	 * How this frame is named in a log line.
	 */
	public String describe() {
		return name;
	}

	/**
	 * Like {@link #describe()}, with the type of the node holding the code if the frame does not know it.
	 */
	public String describe(final String knownCodeSourceType) {
		return describe();
	}

	/**
	 * How the caller handed its work to this frame, if not by a direct call, for example
	 * "doInNewTransaction()". Printed on the caller's line of a trace.
	 */
	public String continuationLabel() {
		return null;
	}

	/**
	 * Whether this frame names where code runs well enough for the caller prefix of a log() line, for
	 * code that is not a method, such as a flow node.
	 */
	public boolean isLogContext() {
		return false;
	}

	public CallFrame getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public String getSessionId() {
		return sessionId;
	}

	public boolean isBoundary() {
		return boundary;
	}

	public boolean isRoot() {
		return parent == null;
	}

	/**
	 * This frame and every frame above it, innermost first.
	 */
	public List<CallFrame> chain() {

		final List<CallFrame> chain = new ArrayList<>(depth + 1);

		for (CallFrame frame = this; frame != null; frame = frame.parent) {

			chain.add(frame);
		}

		return chain;
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("kind", kind());
		map.put("name", name);

		return map;
	}

	@Override
	public String toString() {
		return describe();
	}

	// ----- package-private methods -----
	void addPending(final ScriptError error) {

		if (pending == null) {

			pending = new ArrayList<>(2);
		}

		if (!pending.contains(error)) {

			pending.add(error);
		}
	}

	List<ScriptError> drainPending() {

		final List<ScriptError> drained = pending;

		pending = null;

		return drained;
	}
}
