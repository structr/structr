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

import org.structr.common.error.FrameworkException;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The named queues a scheduled job can run in, and the jobs currently known to them.
 *
 * Each queue runs one job at a time; separate queues run in parallel. A job that does not name a queue
 * runs in {@link #DEFAULT_QUEUE_NAME}.
 *
 * There is no lock here. A queue owns the lock that decides what runs in it, and nothing this class
 * does spans two queues atomically: a job belongs to exactly one queue for its whole life, and the job
 * list is a report rather than an invariant.
 */
public class JobQueueManager {

	public static final String DEFAULT_QUEUE_NAME = "default";

	private static final JobQueueManager singletonInstance = new JobQueueManager();

	private final Map<String, JobQueue> queues     = new ConcurrentHashMap<>();
	private final Map<Long, ScheduledJob> jobsById = new ConcurrentHashMap<>();
	private final AtomicLong jobIdCount            = new AtomicLong(0);

	private JobQueueManager() { }

	/*
	 * Public API
	 */
	public static JobQueueManager getInstance() {

		return singletonInstance;
	}

	/**
	 * The id a job is known by, taken when the job is created rather than when it is queued.
	 *
	 * schedule() hands the id back to its caller, and the job only reaches addJob() after the calling
	 * transaction has been processed -- so an id taken at queueing time is assigned after the caller has
	 * already been given it.
	 */
	Long nextJobId() {

		return jobIdCount.incrementAndGet();
	}

	public void addJob(final ScheduledJob job) throws FrameworkException {

		if (job.runInitialChecks()) {

			// known by id before it can run, so that a job reporting its own start is already findable
			jobsById.put(job.jobId(), job);

			queue(job.getQueueName()).submit(job);
		}
	}

	public void pauseRunningJob(final Long jobId) {

		final ScheduledJob job = jobsById.get(jobId);

		if (job != null) {

			job.pauseJob();
		}
	}

	public void resumePausedJob(final Long jobId) {

		final ScheduledJob job = jobsById.get(jobId);

		if (job != null) {

			job.resumeJob();
		}
	}

	public void abortActiveJob(final Long jobId) {

		final ScheduledJob job = jobsById.get(jobId);

		if (job != null) {

			job.abortJob();
		}
	}

	public void cancelQueuedJob(final Long jobId) {

		final ScheduledJob job = jobsById.get(jobId);

		if (job != null && queue(job.getQueueName()).cancelQueued(job)) {

			jobsById.remove(jobId);
		}
	}

	/**
	 * Drops every job still waiting behind the given id, in one queue.
	 *
	 * Ids are handed out globally and a queue holds only some of them, so an id alone no longer says
	 * which line it is standing in -- but the job it names does, and that is the line the caller is
	 * looking at. So an unnamed queue means the one the job itself is in, and naming a queue overrides
	 * that. A job already gone leaves nothing to read a queue from, and the default queue is where a job
	 * that never asked for one runs.
	 */
	public void cancelAllQueuedJobsAfter(final Long jobId, final String queueName) {

		for (final ScheduledJob job : queue(queueToCancelIn(jobId, queueName)).cancelQueuedAfter(jobId)) {

			jobsById.remove(job.jobId());
		}
	}

	private String queueToCancelIn(final Long jobId, final String queueName) {

		if (queueName != null && !queueName.isBlank()) {

			return normalize(queueName);
		}

		final ScheduledJob job = jobsById.get(jobId);

		return (job != null) ? job.getQueueName() : DEFAULT_QUEUE_NAME;
	}

	public List<Map<String, Object>> listJobs() {

		final List<Map<String, Object>> jobInfoList = new LinkedList<>();

		for (final JobQueue jobQueue : sortedQueues()) {

			for (final ScheduledJob job : jobQueue.snapshot()) {

				jobInfoList.add(job.getJobInfo());
			}
		}

		return jobInfoList;
	}

	public Map<String, Object> jobInfo(final Long jobId) {

		final ScheduledJob job = jobsById.get(jobId);

		return (job != null) ? job.getJobInfo() : null;
	}

	/*
	 * Called by a job from its own thread when its run is over.
	 */
	protected void jobFinished(final ScheduledJob job) {

		jobEnded(job);
	}

	protected void jobAborted(final ScheduledJob job) {

		jobEnded(job);
	}

	/*
	 * Private methods
	 */
	private void jobEnded(final ScheduledJob job) {

		jobsById.remove(job.jobId());

		queue(job.getQueueName()).jobEnded(job);
	}

	/**
	 * The queue of that name, created on first use.
	 *
	 * computeIfAbsent and not get-then-putIfAbsent: two threads naming the same queue at the same moment
	 * would each build one, and the thread holding the one that lost would synchronize on a lock nobody
	 * else uses -- two jobs running in what everything else reports as one queue.
	 *
	 * A queue is never removed. It is an object with an empty deque, and reaping one would have to prove
	 * that no thread is between looking it up and submitting to it.
	 */
	private JobQueue queue(final String name) {

		return queues.computeIfAbsent(name, JobQueue::new);
	}

	/** The default queue first, the rest by name, so the job list reads the same way twice running. */
	private List<JobQueue> sortedQueues() {

		final List<JobQueue> sorted = new LinkedList<>(queues.values());

		sorted.sort(Comparator
			.comparing((final JobQueue jobQueue) -> !DEFAULT_QUEUE_NAME.equals(jobQueue.getName()))
			.thenComparing(JobQueue::getName));

		return sorted;
	}

	static String normalize(final String queueName) {

		if (queueName == null || queueName.isBlank()) {

			return DEFAULT_QUEUE_NAME;
		}

		return queueName.trim();
	}
}
