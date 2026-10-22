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
package org.structr.test.core.scheduler;

import org.structr.common.error.FrameworkException;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class JobQueueManagerTest extends StructrTest {

	/**
	 * A job that reports the end of its run the way the file import jobs do: shouldAbort() first, then a
	 * finally block that reports the completion.
	 */
	private static class TestJob extends ScheduledJob {

		private final CountDownLatch started = new CountDownLatch(1);
		private final CountDownLatch release = new CountDownLatch(1);

		TestJob(final String name) {

			this(name, JobQueueManager.DEFAULT_QUEUE_NAME);
		}

		TestJob(final String name, final String queueName) {

			super(name, null, Collections.emptyMap(), securityContext.getContextStore(), queueName);
		}

		@Override
		public Runnable getRunnable() {

			return () -> {

				started.countDown();

				try {

					release.await(10, TimeUnit.SECONDS);

					if (shouldAbort()) {

						return;
					}

				} catch (InterruptedException ignore) {

				} finally {

					jobFinished();
				}
			};
		}

		boolean awaitStart(final long millis) {

			try { return started.await(millis, TimeUnit.MILLISECONDS); } catch (InterruptedException ignore) { return false; }
		}

		void finish() { release.countDown(); }

		@Override public boolean runInitialChecks() { return true; }
		@Override public String getJobType() { return "TEST"; }
		@Override public String getJobStatusType() { return "TEST_JOB_STATUS"; }
		@Override public String getJobExceptionMessageType() { return "TEST_JOB_EXCEPTION"; }
		@Override public void reportException(final Exception ex) { }
		@Override public Map<String, Object> getStatusData(final JobStatusMessageSubtype subtype) { return new LinkedHashMap<>(); }

		@Override
		public Map<String, Object> getJobInfo() {

			final Map<String, Object> info = new LinkedHashMap<>();

			info.put("jobId", jobId());
			info.put("queue", getQueueName());

			return info;
		}
	}

	@Test
	public void testJobsInDifferentQueuesRunAtTheSameTime() {

		final TestJob first  = new TestJob("first", "one");
		final TestJob second = new TestJob("second", "two");

		try {

			JobQueueManager.getInstance().addJob(first);
			JobQueueManager.getInstance().addJob(second);

			assertTrue("a job in its own queue must start", first.awaitStart(5_000));
			assertTrue("a job in another queue must not wait for the first", second.awaitStart(5_000));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			first.finish();
			second.finish();
		}
	}

	@Test
	public void testJobsInOneQueueRunOneAtATime() {

		final TestJob first  = new TestJob("first", "shared");
		final TestJob second = new TestJob("second", "shared");

		try {

			JobQueueManager.getInstance().addJob(first);
			JobQueueManager.getInstance().addJob(second);

			assertTrue("the first job must start", first.awaitStart(5_000));
			assertFalse("a second job in the same queue must wait", second.awaitStart(1_000));

			first.finish();

			assertTrue("the second job must start once the first is done", second.awaitStart(5_000));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			first.finish();
			second.finish();
		}
	}

	@Test
	public void testJobWithoutAQueueRunsInTheDefaultQueue() {

		final TestJob job = new TestJob("unnamed");

		try {

			JobQueueManager.getInstance().addJob(job);

			assertTrue("the job must start", job.awaitStart(5_000));
			assertEquals("a job that names no queue runs in the default one", JobQueueManager.DEFAULT_QUEUE_NAME, JobQueueManager.getInstance().jobInfo(job.jobId()).get("queue"));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			job.finish();
		}
	}

	@Test
	public void testAbortedJobFreesExactlyOneSlot() {

		final TestJob running = new TestJob("running", "abort");
		final TestJob next    = new TestJob("next", "abort");
		final TestJob last    = new TestJob("last", "abort");

		try {

			final JobQueueManager manager = JobQueueManager.getInstance();

			manager.addJob(running);
			assertTrue("the first job must start", running.awaitStart(5_000));

			manager.addJob(next);
			manager.addJob(last);

			// aborting reports the abort, and the return then runs the finally that reports the completion
			manager.abortActiveJob(running.jobId());
			running.finish();

			assertTrue("the next job must start once the aborted one has freed its slot", next.awaitStart(5_000));
			assertFalse("the second report of the same job ending must not let a third job past", last.awaitStart(2_000));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			running.finish();
			next.finish();
			last.finish();
		}
	}

	@Test
	public void testCancelAllQueuedJobsAfterUsesTheQueueOfTheGivenJob() {

		final TestJob running = new TestJob("running", "named");
		final TestJob dropped = new TestJob("dropped", "named");
		final TestJob inDefault = new TestJob("inDefault");

		try {

			final JobQueueManager manager = JobQueueManager.getInstance();

			manager.addJob(running);
			assertTrue("the first job must start", running.awaitStart(5_000));

			manager.addJob(dropped);

			manager.addJob(inDefault);
			assertTrue("the job in the default queue must start", inDefault.awaitStart(5_000));

			// no queue named, and the id belongs to "named" rather than to the default queue
			manager.cancelAllQueuedJobsAfter(running.jobId(), null);

			assertEquals("a job in another queue must be untouched", null, manager.jobInfo(dropped.jobId()));

			running.finish();

			assertFalse("the cancelled job must never start", dropped.awaitStart(1_000));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			running.finish();
			dropped.finish();
			inDefault.finish();
		}
	}

	@Test
	public void testCancelAllQueuedJobsAfterLeavesOtherQueuesAlone() {

		final TestJob running = new TestJob("running", "cancel");
		final TestJob dropped = new TestJob("dropped", "cancel");
		final TestJob other   = new TestJob("other", "untouched");

		try {

			final JobQueueManager manager = JobQueueManager.getInstance();

			manager.addJob(running);
			assertTrue("the first job must start", running.awaitStart(5_000));

			manager.addJob(dropped);

			// "other" is already running in its own queue, so only the waiting job can be dropped
			manager.addJob(other);
			assertTrue("the job in the other queue must start", other.awaitStart(5_000));

			manager.cancelAllQueuedJobsAfter(running.jobId(), "cancel");

			running.finish();

			assertFalse("a cancelled job must never start", dropped.awaitStart(1_000));
			assertEquals("a cancelled job must no longer be reported", null, manager.jobInfo(dropped.jobId()));

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

		} finally {

			running.finish();
			dropped.finish();
			other.finish();
		}
	}
}
