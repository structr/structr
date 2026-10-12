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
package org.structr.core.scheduler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * One lane of the job queue: the jobs waiting in it, the one job running, and the lock that keeps those
 * two in step.
 *
 * A queue runs one job at a time. Queues run in parallel with each other, so a queue is the unit a
 * developer reasons about when two scheduled scripts must not touch the same data at once.
 *
 * The lock guards the slot and the waiting jobs together, because the invariant is about both: a job
 * leaves the deque and takes the slot in one step, and nothing can observe a state where it is in
 * neither or in both. Everything that reaches outside this class -- starting a job, announcing one --
 * happens after the lock is released, so a thread never holds this lock while holding another.
 */
class JobQueue {

	private final Deque<ScheduledJob> queued = new ArrayDeque<>();
	private final Object lock                = new Object();
	private final String name;

	private ScheduledJob active = null;

	JobQueue(final String name) {

		this.name = name;
	}

	String getName() {

		return name;
	}

	/**
	 * Takes the job, and starts it if the queue is idle.
	 *
	 * @return whether the job was started rather than queued
	 */
	boolean submit(final ScheduledJob job) {

		final boolean start;

		synchronized (lock) {

			if (active == null && queued.isEmpty()) {

				active = job;
				start  = true;

			} else {

				queued.addLast(job);
				start = false;
			}
		}

		if (start) {

			job.startJob();

		} else {

			job.reportQueued();
		}

		return start;
	}

	/**
	 * Releases the slot the job held and starts the next one.
	 *
	 * A job announces the end of its own run, and an aborted run announces it twice -- once for the
	 * abort and once from the finally block that follows it. The identity check makes the second
	 * announcement a no-op: only the job that actually holds the slot can free it, so a job that has
	 * already been released cannot hand out a second one.
	 */
	void jobEnded(final ScheduledJob job) {

		final ScheduledJob next;

		synchronized (lock) {

			if (active != job) {

				return;
			}

			active = queued.poll();
			next   = active;
		}

		if (next != null) {

			next.startJob();
		}
	}

	boolean cancelQueued(final ScheduledJob job) {

		synchronized (lock) {

			return queued.remove(job);
		}
	}

	/**
	 * Drops every waiting job queued after the given one. Ids are handed out in submission order, so
	 * within one queue a higher id is always further back in the line.
	 */
	List<ScheduledJob> cancelQueuedAfter(final long jobId) {

		final List<ScheduledJob> cancelled = new ArrayList<>();

		synchronized (lock) {

			queued.removeIf(job -> {

				if (job.jobId() > jobId) {

					cancelled.add(job);

					return true;
				}

				return false;
			});
		}

		return cancelled;
	}

	/** The running job first, then the waiting ones in the order they will run. */
	List<ScheduledJob> snapshot() {

		final List<ScheduledJob> jobs = new ArrayList<>();

		synchronized (lock) {

			if (active != null) {

				jobs.add(active);
			}

			jobs.addAll(queued);
		}

		return jobs;
	}
}
