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
 * The settlements of host thenables that have been registered by a script but not yet joined.
 *
 * <p>This is the whole of the "bounded drain" that lets a race answer -- and cost -- the fastest call
 * rather than the first one written. Structr has no event loop, and this is not one: it does not
 * schedule anything, it cannot make a promise settle that nothing was going to settle, and it never
 * outlives the evaluation that filled it. All it does is choose the <em>order</em> in which
 * already-running calls are joined -- by completion instead of by registration -- and stop as soon as
 * the script's own promise has settled.</p>
 *
 * <h3>Why the order was wrong without it</h3>
 *
 * <p>{@code Promise.race([a, b])} registers a reaction on every element synchronously, in argument
 * order, before any of them can settle. When {@code then()} joins its call inline -- which is what
 * {@link org.structr.core.script.polyglot.wrappers.AsyncFunctionWrapper}'s thenable used to do -- the
 * first registration settles the race before the second one is even made, so the race answers the
 * first <em>argument</em>. Deferring the join and draining afterwards is what fixes that, and it is
 * also why {@code Promise.all} is unaffected: {@code all} answers in argument order by specification,
 * whatever order its elements settle in.</p>
 *
 * <h3>Who drains, and why it is the host</h3>
 *
 * <p>The drain is driven from {@link org.structr.core.script.polyglot.PolyglotWrapper}, one settlement
 * at a time, for exactly as long as the script's completion promise is still pending. That is what
 * makes a race cheap as well as correct: once the winner has resolved the script, the loop stops and
 * the losers are never joined at all -- {@link #closeFrame()} discards them.</p>
 *
 * <p>It has to be the host, because the stop condition is only visible there. A drain scheduled as a
 * guest job cannot stop early: {@code js.interop-complete-promises} drains the guest job queue
 * <em>to empty</em> at an interop boundary rather than stopping once the promise being completed has
 * settled, so a job enqueued before the race resolved still runs and still parks on its loser. The
 * host, holding the completion promise's own reactions, knows when there is nothing left to wait for.
 * See gotchas.md in the structr-refactor workspace.</p>
 *
 * <h3>Why a thread local, and why frames</h3>
 *
 * <p>A drain must happen on the thread that registered the settlements: the callbacks are guest
 * {@code Value}s that cannot leave their context, the context lock is held by this thread, and
 * {@code TransactionCommand} is itself a thread local, so a join performed anywhere else would be
 * outside the caller's transaction. A worker thread running an async call therefore has its own, empty
 * registry and can never see -- or be seen by -- the caller's.</p>
 *
 * <p>Evaluations nest: a script calls a function that evaluates another script. Each evaluation opens
 * a {@link #openFrame() frame} and drains only its own, so an inner evaluation cannot settle an outer
 * script's calls early, which would join them outside the transaction the outer script is running in.</p>
 */
public final class PendingThenables {

	private static final Logger logger = LoggerFactory.getLogger(PendingThenables.class);

	/**
	 * How long the drain parks when nothing has completed yet.
	 *
	 * The drain is waiting on a socket read on another thread, so the wait is milliseconds at best and
	 * whole seconds at worst; parking briefly costs a bounded number of wake-ups over that period and
	 * needs no coordination with the worker. Spinning instead would burn the request thread, and having
	 * the workers signal a shared monitor would mean {@code PendingCall} handing out a completion hook
	 * for the sole benefit of this loop.
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
	 * Narrower than a {@code Future} on purpose: the drain must not be able to join anything itself, or
	 * the settlement's own error translation would be bypassed.
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
	 * What is left here is a settlement the script turned out not to need: a race's losers, or the
	 * remainder of a frame whose evaluation failed part way through. Discarding them is the point of the
	 * whole mechanism rather than an oversight -- a race that has answered must not pay for the calls it
	 * beat. Note that a call which was started and never awaited is not among them: nothing ever calls
	 * then() on one, so it is never deferred.
	 *
	 * Nothing is joined here. The evaluation is over, so blocking now would hold a request thread for a
	 * result that no longer has anywhere to go. The worker finishes on its own and its answer is dropped.
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

			// the map entry itself, not just the list -- a request thread is pooled and reused, and an
			// empty ArrayList left behind on every one of them is a leak that never shows up as a bug
			FRAMES.remove();
		}
	}

	/**
	 * Whether a drain will happen, and so whether deferring a join is safe.
	 *
	 * A thenable whose {@code then()} is reached with no frame open has nothing that would come back for
	 * it, so it has to settle inline the way it always did. That is not a hypothetical: {@code unwrap}
	 * is reached from paths that never went through a polyglot evaluation at all.
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

			// hasFrame() is the guard callers are expected to use; running it here rather than dropping it
			// keeps a missed guard from turning into a promise that silently never settles
			settle.run();

			return;
		}

		frames.get(frames.size() - 1).deferred.add(new Deferred(completion, settle));
	}

	// ----- the drain -----

	/**
	 * Whether the current frame still holds a settlement that could be run.
	 *
	 * The drain loop's other half of the stop condition: the script's promise being pending is only
	 * worth waiting on while something is still outstanding that could settle it.
	 */
	public static boolean hasDeferred() {

		final List<Frame> frames = FRAMES.get();

		return !frames.isEmpty() && !frames.get(frames.size() - 1).deferred.isEmpty();
	}

	/**
	 * Settles the one deferral whose call finished first, parking until one has.
	 *
	 * <p>One at a time, so the decision to join anything further is taken after the guest has had a
	 * chance to act on this settlement. Settling hands a value to a guest callback, which resolves a
	 * guest promise and -- because that callback is an interop call whose return drains the guest job
	 * queue -- runs whatever was waiting on it. That may be all the script needed, in which case the
	 * caller stops and the remaining calls are never joined.</p>
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

		// run outside the list it was taken from, so that a callback which registers another deferral --
		// or throws -- cannot corrupt the iteration that is draining it
		next.settle().run();
	}

	/**
	 * Removes and answers the first deferral whose call has completed, parking until one has.
	 *
	 * Falls back to the earliest registration once nothing is outstanding but undecided, so that a
	 * {@link Completion} which never answers true cannot spin here forever.
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
