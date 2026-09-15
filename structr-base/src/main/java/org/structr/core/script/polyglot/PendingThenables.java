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
package org.structr.core.script.polyglot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * The settlements of host thenables that a script has registered but that have not been joined yet.
 *
 * <p>Registered by {@link org.structr.core.script.polyglot.wrappers.PendingCallWrapper} when the script
 * calls {@code then()} on it, and joined by {@link PolyglotWrapper#unwrap} while it
 * settles the script's completion value. This is not an event loop: it schedules nothing, it cannot
 * settle a promise that nothing was going to settle, and it never outlives the evaluation that filled
 * it. It chooses the <em>order</em> in which already-running calls are joined -- by completion rather
 * than by registration -- and lets the calls nobody is waiting for any more be dropped.</p>
 *
 * <h3>Why the order matters</h3>
 *
 * <p>{@code Promise.race([a, b])} registers a reaction on every element synchronously, in argument
 * order, before any of them can settle. A {@code then()} that joins its call inline therefore settles
 * the first element before the second one's reaction has been registered, and the race answers the
 * first <em>argument</em> rather than the first call to finish. Deferring the join and taking completed
 * calls first is what makes the outcome depend on completion order.</p>
 *
 * <p>{@code Promise.all} and {@code allSettled} are unaffected: they answer in argument order by
 * specification, whatever order their elements settle in.</p>
 *
 * <h3>The drain runs on the host, not in a guest job</h3>
 *
 * <p>A deferral is only worth joining while the script's completion promise is still pending, and that
 * condition is visible only to the host, which holds that promise's reactions. A drain scheduled as a
 * guest job cannot apply it: {@code js.interop-complete-promises} drains the guest job queue <em>to
 * empty</em> at an interop boundary rather than stopping once the promise it is completing has settled,
 * so a job enqueued before the script resolved still runs, and still blocks on a call whose result is
 * no longer wanted.</p>
 *
 * <h3>Thread confinement</h3>
 *
 * <p>A drain must run on the thread that registered the settlements. The callbacks are guest
 * {@code Value}s, which cannot leave their context; the context lock is held by this thread; and
 * {@code TransactionCommand} is a thread local, so a join performed on another thread would run outside
 * the caller's transaction. A worker thread executing an async call therefore has its own empty
 * registry, and can neither see nor be seen by the caller's.</p>
 *
 * <p>Evaluations nest -- a script calls a function that evaluates another script -- so registrations are
 * held in a stack of {@link #openFrame() frames} and only the innermost is drained. Otherwise an inner
 * evaluation could join an outer script's calls, outside the transaction the outer script is running
 * in.</p>
 */
public final class PendingThenables {

	private static final Logger logger = LoggerFactory.getLogger(PendingThenables.class);

	/**
	 * How long the drain parks between checks while no deferred call has completed.
	 *
	 * The calls being waited on are socket reads on other threads, taking milliseconds to seconds, so a
	 * short park costs a bounded number of wake-ups and needs no coordination with the workers.
	 */
	private static final long PARK_NANOS = 200_000L;

	private static final ThreadLocal<List<Frame>> FRAMES = ThreadLocal.withInitial(ArrayList::new);

	private PendingThenables() {
	}

	/**
	 * One evaluation's worth of deferred settlements.
	 */
	private static final class Frame {

		private final List<Deferred> deferred = new ArrayList<>();
	}

	/**
	 * A registered settlement and the question of whether it can run yet.
	 */
	private record Deferred(Completion completion, Runnable settle) {
	}

	/**
	 * Whether the call behind a deferred settlement has finished.
	 *
	 * Deliberately narrower than {@code Future}: the drain must not be able to retrieve a result itself,
	 * because joining is what applies the settlement's error translation.
	 */
	public interface Completion {

		boolean isDone();
	}

	// ----- frames -----

	/**
	 * Opens a frame for one evaluation. Must be paired with {@link #closeFrame()} in a finally block.
	 */
	public static void openFrame() {

		FRAMES.get().add(new Frame());
	}

	/**
	 * Closes the current frame, discarding anything still registered in it.
	 *
	 * What remains is a settlement the script did not need: the calls a race beat, or the rest of a frame
	 * whose evaluation failed part way through. A call that was started and never awaited is not among
	 * them, since nothing ever calls {@code then()} on one.
	 *
	 * Nothing is joined here. The evaluation is over, so blocking would hold a request thread for a result
	 * that has nowhere to go; the worker finishes on its own and its answer is discarded.
	 */
	public static void closeFrame() {

		final List<Frame> frames = FRAMES.get();
		if (!frames.isEmpty()) {

			final Frame frame = frames.remove(frames.size() - 1);
			if (!frame.deferred.isEmpty()) {

				logger.debug("{} asynchronous settlement(s) were never needed; their results are discarded.", frame.deferred.size());
			}
		}

		if (frames.isEmpty()) {

			// the map entry itself, not just the list: request threads are pooled and reused, so an empty
			// ArrayList left on each of them would accumulate
			FRAMES.remove();
		}
	}

	/**
	 * Whether a drain will happen, and so whether deferring a join is safe.
	 *
	 * A thenable whose {@code then()} is reached with no frame open has nothing that will come back to
	 * drain it, and must settle inline instead. {@code unwrap} is reachable from paths that never went
	 * through a polyglot evaluation, so this is a real case rather than a defensive one.
	 */
	public static boolean hasFrame() {

		return !FRAMES.get().isEmpty();
	}

	/**
	 * Defers one settlement into the current frame.
	 *
	 * @param completion whether the underlying call has finished
	 * @param settle     joins the call and hands the outcome to the script's callbacks; runs on this thread
	 */
	public static void defer(final Completion completion, final Runnable settle) {

		final List<Frame> frames = FRAMES.get();
		if (frames.isEmpty()) {

			// callers are expected to check hasFrame() first; settling inline rather than dropping the
			// deferral keeps a missed check from producing a promise that never settles
			settle.run();

			return;
		}

		frames.get(frames.size() - 1).deferred.add(new Deferred(completion, settle));
	}

	// ----- the drain -----

	/**
	 * Whether the current frame still holds a settlement that could be run.
	 *
	 * Half of the drain loop's stop condition: a pending completion promise is only worth waiting on
	 * while something is still outstanding that could settle it.
	 */
	public static boolean hasDeferred() {

		final List<Frame> frames = FRAMES.get();

		return !frames.isEmpty() && !frames.get(frames.size() - 1).deferred.isEmpty();
	}

	/**
	 * Settles the deferral whose call completed first, parking until one has.
	 *
	 * <p>One per invocation, so that the guest can act on this settlement before the caller decides
	 * whether to join anything further. Settling invokes a guest callback, which resolves a guest promise;
	 * because that invocation is an interop call, the guest job queue is drained when it returns, running
	 * whatever was waiting on that promise. The script may therefore be complete by the time it
	 * returns.</p>
	 */
	public static void settleNextCompleted() {

		final List<Frame> frames = FRAMES.get();
		if (frames.isEmpty()) {

			return;
		}

		final Frame frame = frames.get(frames.size() - 1);
		if (frame.deferred.isEmpty()) {

			return;
		}

		final Deferred next = takeNextCompleted(frame);

		// run after removal from the list, so that a callback which registers another deferral, or throws,
		// cannot disturb the iteration
		next.settle().run();
	}

	/**
	 * Removes and answers the first deferral whose call has completed, parking until one has.
	 *
	 * On interrupt, falls back to the earliest registration rather than parking again, so that a
	 * {@link Completion} which never answers true cannot block teardown indefinitely.
	 */
	private static Deferred takeNextCompleted(final Frame frame) {

		while (true) {

			final Iterator<Deferred> candidates = frame.deferred.iterator();

			while (candidates.hasNext()) {

				final Deferred candidate = candidates.next();
				if (candidate.completion().isDone()) {

					candidates.remove();

					return candidate;
				}
			}

			LockSupport.parkNanos(PARK_NANOS);

			if (Thread.currentThread().isInterrupted()) {

				// the evaluation is being torn down; join in registration order rather than park again

				return frame.deferred.remove(0);
			}
		}
	}
}
