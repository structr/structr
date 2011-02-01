/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An experimental interface description for a structr agent.
 * To be discussed..
 *
 * @author cmorgner
 */
public abstract class Agent extends Thread
{
	private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
	private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<Task>();
	private AgentService agentService = null;

	private long averageExecutionTime = 0;
	private long lastStartTime = 0;
	private int maxQueueSize = 10;

	// <editor-fold defaultstate="expanded" desc="public methods">
	public final void setAgentService(AgentService service)
	{
		// NOTE: this is important! We do not want running tasks to die when the
		// server is going down!
		this.setDaemon(false);

		this.agentService = service;

		// initialize lastStartTime
		lastStartTime = System.nanoTime();
	}

	@Override
	public final void run()
	{
		agentService.notifyAgentStart(this);

		do
		{
			Task task = null;

			synchronized(taskQueue)
			{
				task = taskQueue.poll();
			}

			if(task != null)
			{
				lastStartTime = System.nanoTime();

				ReturnValue ret = processTask(task);

				// handle return value
				switch(ret)
				{
					case Success:
					case Abort:
						// task finished, nothing to do in these cases
						break;

					case Retry:
						// TODO: schedule task for re-execution
						break;
				}

				long endTime = System.nanoTime();

				// calc. average execution time
				averageExecutionTime += endTime;
				averageExecutionTime /= 2;

			} else
			{
				// queue is empty, unregister and
				// quit.
				agentService.notifyAgentStop(this);
				acceptingTasks.set(false);
			}

		} while(acceptingTasks.get());

	}

	public final boolean assignTask(Task task)
	{
		// TODO: do type check here

		if(canHandleMore() && acceptingTasks.get())
		{
			synchronized(taskQueue)
			{
				taskQueue.add(task);
			}

			return(true);
		}

		return(false);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="protected methods">
	protected AgentService getBlackboardService()
	{
		return(agentService);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private boolean canHandleMore()
	{
		int size = 0;

		synchronized(taskQueue)
		{
			// FIXME: size may not be a constant time operation! slow?
			size = taskQueue.size();
		}

		// queue is empty, assume new agent
		if(size == 0)
		{
			return(true);
		}

		long actualExecutionTime = System.nanoTime() - lastStartTime;

		// calculate thresholds for queue size adaption
		long upperThreshold = averageExecutionTime + (averageExecutionTime / 2);
		long lowerThreshold = averageExecutionTime - (averageExecutionTime / 2);

		if(actualExecutionTime > upperThreshold && maxQueueSize > 2)
		{
			// FIXME

			maxQueueSize -= 2;

			// do not take the next task
			return(false);

		} else
		if(actualExecutionTime < lowerThreshold && maxQueueSize < 200)
		{
			// FIXME

			maxQueueSize += 2;

			// can take the next task
			return(true);
		}


		return(size < maxQueueSize);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="abstract methods">
	public abstract Class getSupportedTaskType();

	/**
	 * This method will be called by the AgentService
	 * @param task
	 */
	public abstract ReturnValue processTask(Task task);
	// </editor-fold>
}
