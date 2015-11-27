/**
 * Copyright (C) 2010-2015 Structr GmbH
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


//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Abstract base class for all agents.
 *
 *
 */
public abstract class Agent<T extends NodeInterface> extends Thread implements StatusInfo {

	public static final String AVERAGE_EXECUTION_TIME = "average_execution_time";
	public static final String EXECUTION_STATUS       = "execution_status";
	public static final String MAX_QUEUE_SIZE         = "max_queue_size";
	private static final Logger logger                = Logger.getLogger(Agent.class.getName());

	//~--- fields ---------------------------------------------------------

	private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
	private AgentService agentService          = null;
	private long averageExecutionTime          = 0;
	private Task currentTask                   = null;
	private long lastStartTime                 = 0;
	private int maxAgents                      = 4;
	private int maxQueueSize                   = 10;
	private final AtomicBoolean suspended      = new AtomicBoolean(false);
	private final Queue<Task<T>> taskQueue     = new ConcurrentLinkedQueue<>();

	//~--- methods --------------------------------------------------------

	@Override
	public final void run() {

		agentService.notifyAgentStart(this);

		do {

			while (suspended.get()) {

				Thread.yield();

			}

			synchronized (taskQueue) {

				currentTask = taskQueue.poll();

			}

			if (currentTask != null) {

				lastStartTime = System.nanoTime();

				ReturnValue ret = null;

				// only execute process if Service layer is ready
				// (and not shutting down right now)
				if (Services.getInstance().isInitialized()) {

					if (createEnclosingTransaction()) {

						try (final Tx tx = StructrApp.getInstance().tx()) {

							ret = processTask(currentTask);
							tx.success();

						} catch (Throwable t) {

							// someone killed us or the task processing failed..
							// Log this!!
							logger.log(Level.SEVERE, "Processing task {0} failed. Maybe someone killed us?", currentTask.getType());
							t.printStackTrace();
						}

					} else {

						try {

							ret = processTask(currentTask);

						} catch (Throwable t) {

							// someone killed us or the task processing failed..
							// Log this!!
							logger.log(Level.SEVERE, "Processing task {0} failed. Maybe someone killed us?", currentTask.getType());
							t.printStackTrace();
						}
					}
				}

				if (ret != null) {

					// handle return value
					switch (ret) {

						case Success :
						case Abort :

							// task finished, nothing to do in these cases
							break;

						case Retry :

							// TODO: schedule task for re-execution
							break;

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

		// TODO: do type check here
		if (canHandleMore() && acceptingTasks.get()) {

			synchronized (taskQueue) {

				taskQueue.add(task);

			}

			return (true);

		}

		return (false);
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

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private boolean canHandleMore() {

//              return (taskQueue.isEmpty());
		int size = 0;

		synchronized (taskQueue) {

			// FIXME: size may not be a constant time operation! slow?
			size = taskQueue.size();
		}

		// queue is empty, assume new agent
		if (size == 0) {

			return (true);

		}

		long actualExecutionTime = System.nanoTime() - lastStartTime;

		// calculate thresholds for queue size adaption
		long upperThreshold = averageExecutionTime + (averageExecutionTime / 2);
		long lowerThreshold = averageExecutionTime - (averageExecutionTime / 2);

		if ((actualExecutionTime > upperThreshold) && (maxQueueSize > 2)) {

			// FIXME
			maxQueueSize -= 2;

			// do not take the next task
			return (false);
		} else if ((actualExecutionTime < lowerThreshold) && (maxQueueSize < 200)) {

			// FIXME
			maxQueueSize += 2;

			// can take the next task
			return (true);
		}

		return (size < maxQueueSize);
	}

	// </editor-fold>

	/**
	 * This method will be called by the AgentService
	 * @param task
	 */
	public abstract ReturnValue processTask(final Task<T> task) throws Throwable;

	public boolean createEnclosingTransaction() {
		return true;
	}

	public final Task<T> getCurrentTask() {
		return (currentTask);
	}

	public final List<Task<T>> getTaskQueue() {

		List<Task<T>> ret = new LinkedList<Task<T>>();

		ret.addAll(taskQueue);

		return (ret);
	}

	public final int getMaxQueueSize() {
		return (maxQueueSize);
	}

	public final long getAverageExecutionTime() {
		return (averageExecutionTime);
	}

	public int getMaxAgents() {
		return (maxAgents);
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

		return (null);
	}

	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="protected methods">
	protected AgentService getBlackboardService() {
		return (agentService);
	}

	// <editor-fold defaultstate="collapsed" desc="abstract methods">
	public abstract Class getSupportedTaskType();

	public final boolean isSuspended() {
		return (suspended.get());
	}

	public final boolean isAcceptingTasks() {
		return (acceptingTasks.get());
	}

	//~--- set methods ----------------------------------------------------

	// <editor-fold defaultstate="expanded" desc="public methods">
	public final void setAgentService(AgentService service) {

		// NOTE: this is important! We do not want running tasks to die when the
		// server is going down!
		this.setDaemon(false);

		this.agentService = service;

		// initialize lastStartTime
		lastStartTime = System.nanoTime();
	}

	// </editor-fold>
}
