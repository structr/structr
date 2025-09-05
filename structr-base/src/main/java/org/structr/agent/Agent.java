/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.structr.agent.ReturnValue.Retry;

/**
 * Abstract base class for all agents.
 */
public abstract class Agent<T> extends Thread implements StatusInfo {

	public static final String AVERAGE_EXECUTION_TIME = "average_execution_time";
	public static final String EXECUTION_STATUS       = "execution_status";
	public static final String MAX_QUEUE_SIZE         = "max_queue_size";
	private static final Logger logger                = LoggerFactory.getLogger(Agent.class.getName());

	private final AtomicBoolean suspended      = new AtomicBoolean(false);
	private final Queue<Task<T>> taskQueue     = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
	private AgentService agentService          = null;
	private long averageExecutionTime          = 0;
	private int maxAgents                      = 10;
	private int maxQueueSize                   = 200;

	/**
	 * This method will be called by the AgentService
	 * @param task
	 */
	public abstract ReturnValue processTask(final Task<T> task) throws Throwable;
	public abstract Class getSupportedTaskType();

	@Override
	public final void run() {

		agentService.notifyAgentStart(this);

		do {

			if (!Services.getInstance().isInitialized()) {

				try { Thread.sleep(100); } catch (InterruptedException i) {}

				// loop until we are stopped
				continue;
			}

			while (suspended.get()) {

				Thread.yield();

			}

			final Task<T> currentTask;

			synchronized (taskQueue) {

				currentTask = taskQueue.poll();

			}

			if (currentTask != null) {

				ReturnValue ret = null;

				// only execute process if Service layer is ready
				// (and not shutting down right now)
				if (Services.getInstance().isInitialized()) {

					if (createEnclosingTransaction()) {

						try (final Tx tx = StructrApp.getInstance().tx()) {

							ret = processTask(currentTask);
							tx.success();

						} catch (FrameworkException fex) {

							// task processing failed..
							logger.error("Processing task {} failed: {}", currentTask.getType(), fex.toString());

						} catch (Throwable t) {

							// task processing failed..
							logger.error("Processing task {} failed: {}", currentTask.getType(), t.getMessage());
						}

					} else {

						try {

							ret = processTask(currentTask);

						} catch (FrameworkException fex) {

							// task processing failed..
							logger.error("Processing task {} failed: {}", currentTask.getType(), fex.toString());

						} catch (Throwable t) {

							// task processing failed..
							logger.error("Processing task {} failed: {}", currentTask.getType(), t.getMessage());
						}
					}
				}

				if (ret != null && Retry.equals(ret) && currentTask.getRetryCount() < 2) {

					// wait some time
					try { Thread.sleep(2000); } catch (InterruptedException ex) {}

					synchronized (taskQueue) {

						currentTask.incrementRetryCount();
						taskQueue.add(currentTask);
					}
				}

				long endTime = System.nanoTime();

				// calc. average execution time
				averageExecutionTime += endTime;
				averageExecutionTime /= 2;

			} else {

				// queue is empty, quit.
				acceptingTasks.set(false);
			}

		} while (acceptingTasks.get());

		// call beforeShutdown to allow agents to clean up
		beforeShutdown();
		agentService.notifyAgentStop(this);
	}

	public final boolean assignTask(final Task<T> task) {

		if (canHandleMore() && acceptingTasks.get()) {

			synchronized (taskQueue) {

				taskQueue.add(task);
			}

			return true;
		}

		return false;
	}

	public final void killAgent() {

		// stop accepting tasks
		acceptingTasks.set(false);

		// clear queue
		taskQueue.clear();

		// interrupt running process..
		// not sure if this works... see Thread.interrupt()'s description!
		// may not work if the processTask method itself catches the interrupt..
		this.interrupt();
	}

	public final void suspendAgent() {

		acceptingTasks.set(false);
		suspended.set(true);
	}

	public final void resumeAgent() {

		acceptingTasks.set(true);
		suspended.set(false);
	}

	protected void beforeShutdown() {

		// override me
	}

	protected boolean canHandleMore() {

		int size = 0;

		synchronized (taskQueue) {

			// FIXME: size may not be a constant time operation! slow?
			size = taskQueue.size();
		}

		// queue is empty, assume new agent
		if (size == 0) {

			return true;

		}

		return size < maxQueueSize;
	}

	public boolean createEnclosingTransaction() {
		return true;
	}

	public final int getMaxQueueSize() {
		return maxQueueSize;
	}

	public final long getAverageExecutionTime() {
		return averageExecutionTime;
	}

	public final void setAgentService(AgentService service) {

		this.setDaemon(true);

		this.agentService = service;
	}

	public int getMaxAgents() {
		return maxAgents;
	}

	// ----- interface StatusInfo -----
	@Override
	public Object getStatusProperty(String key) {

		if (key.equals(AVERAGE_EXECUTION_TIME)) {

			return (getAverageExecutionTime());

		} else if (key.equals(MAX_QUEUE_SIZE)) {

			return (getMaxQueueSize());

		} else if (key.equals(EXECUTION_STATUS)) {

			// TODO.
		}

		return null;
	}

	protected AgentService getBlackboardService() {
		return agentService;
	}

	public final boolean isSuspended() {
		return suspended.get();
	}

	public final boolean isAcceptingTasks() {
		return acceptingTasks.get();
	}
}
