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
 * <p>This is the whole of the "bounded drain" that lets a race answer the fastest call rather than the
 * first one written. Structr has no event loop, and this is not one: it does not schedule anything, it
 * cannot make a promise settle that nothing was going to settle, and it never outlives the evaluation
 * that filled it. All it does is choose the <em>order</em> in which already-running calls are joined --
 * by completion instead of by registration.</p>
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
		private boolean drainScheduled        = false;
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
	 * What is left here is a settlement that was registered and never reached: a race's losers, or the
	 * remainder of a frame whose evaluation failed part way through. Note that a call which was started
	 * and never awaited is not among them -- nothing ever calls then() on one, so it is never deferred.
	 *
	 * Nothing is joined here. The evaluation is over, so blocking now would hold a request thread for a
	 * result that no longer has anywhere to go.
	 */
	public static void closeFrame() {

		final List<Frame> frames = FRAMES.get();

		if (!frames.isEmpty()) {

			final Frame frame = frames.remove(frames.size() - 1);

			if (!frame.deferred.isEmpty()) {

				logger.debug("{} asynchronous settlement(s) were never reached; their results are discarded.", frame.deferred.size());
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

	/**
	 * Runs {@code schedule} once per frame, for the caller that needs a drain to be arranged.
	 *
	 * The scheduling is language-specific -- it means enqueueing a job on the guest's own queue -- so it
	 * is passed in rather than done here. Once per frame is enough because the scheduled job chains the
	 * next one itself for as long as anything is still deferred; the gate reopens when the chain runs out.
	 */
	public static void scheduleDrainOnce(final Runnable schedule) {

		final List<Frame> frames = FRAMES.get();

		if (frames.isEmpty()) {

			return;
		}

		final Frame frame = frames.get(frames.size() - 1);

		if (!frame.drainScheduled) {

			frame.drainScheduled = true;

			schedule.run();
		}
	}

	/**
	 * Settles the one deferral whose call finished first, and answers whether any remain.
	 *
	 * <p>One at a time, so that the decision to join anything further is taken after the guest has had a
	 * chance to act on this settlement. Settling hands a value to a guest callback, which resolves a guest
	 * promise and runs whatever was waiting on it -- and that may be all the script needed.</p>
	 *
	 * <p><b>It does not currently save a race the cost of its losers</b>, and the reason is worth knowing
	 * before trying to make it. The scheduler in {@code AsyncFunctionWrapper} enqueues the next drain as a
	 * guest job while anything is still deferred, and GraalJS drains its job queue <em>to empty</em> at the
	 * interop boundary rather than stopping once the promise it is completing has settled. So the job
	 * enqueued before the race resolved still runs, and it parks on the loser: the race answers the call
	 * that finished first, but the script ends up costing the slowest one. Stopping earlier needs the guest
	 * to say that its promise has settled, because only the guest knows -- see gotchas.md in the
	 * structr-refactor workspace under the scripting section.</p>
	 *
	 * @return true if the frame still holds deferrals after this one was settled
	 */
	public static boolean drainOne() {

		final List<Frame> frames = FRAMES.get();

		if (frames.isEmpty()) {

			return false;
		}

		final Frame frame = frames.get(frames.size() - 1);

		if (frame.deferred.isEmpty()) {

			// the chain of drain jobs has run out; a later burst of registrations starts a new one
			frame.drainScheduled = false;

			return false;
		}

		final Deferred next = takeNextCompleted(frame);

		// run outside the list it was taken from, so that a callback which registers another deferral --
		// or throws -- cannot corrupt the iteration that is draining it
		next.settle().run();

		if (frame.deferred.isEmpty()) {

			frame.drainScheduled = false;

			return false;
		}

		return true;
	}

	/**
	 * Settles the current frame completely, completed calls first.
	 *
	 * For the host-driven path in {@code PolyglotWrapper.unwrapThenable}, which invokes then() itself and
	 * has no guest job queue to lean on for the next round. It is settling a single returned thenable
	 * there, so there is no race whose losers this could make it wait for.
	 */
	public static void drainAll() {

		while (drainOne()) {
			// drainOne() answers whether anything is left
		}
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
