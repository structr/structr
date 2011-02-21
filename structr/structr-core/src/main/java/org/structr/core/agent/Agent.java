/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An experimental interface description for a structr agent.
 * To be discussed..
 *
 * @author cmorgner
 */
public abstract class Agent extends Thread implements StatusInfo {

    public static final String MAX_QUEUE_SIZE = "max_queue_size";
    public static final String AVERAGE_EXECUTION_TIME = "average_execution_time";
    public static final String EXECUTION_STATUS = "execution_status";
    private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
    private final AtomicBoolean suspended = new AtomicBoolean(false);
    private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<Task>();
    private AgentService agentService = null;
    private Task currentTask = null;
    private long averageExecutionTime = 0;
    private long lastStartTime = 0;
    private int maxQueueSize = 10;
    private int maxAgents = 4;

    // <editor-fold defaultstate="expanded" desc="public methods">
    public final void setAgentService(AgentService service) {
        // NOTE: this is important! We do not want running tasks to die when the
        // server is going down!
        this.setDaemon(false);

        this.agentService = service;

        // initialize lastStartTime
        lastStartTime = System.nanoTime();
    }

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

                try {
                    ret = processTask(currentTask);

                } catch (Throwable t) {
                    // someone killed us or the task processing failed..
                }

                if (ret != null) {
                    // handle return value
                    switch (ret) {
                        case Success:
                        case Abort:
                            // task finished, nothing to do in these cases
                            break;

                        case Retry:
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

        agentService.notifyAgentStop(this);
    }

    public final boolean assignTask(Task task) {
        // TODO: do type check here

        if (canHandleMore() && acceptingTasks.get()) {
            synchronized (taskQueue) {
                taskQueue.add(task);
            }

            return (true);
        }

        return (false);
    }

    public final Task getCurrentTask() {
        return (currentTask);
    }

    public final List<Task> getTaskQueue() {
        List<Task> ret = new LinkedList<Task>();
        ret.addAll(taskQueue);

        return (ret);
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

    public final boolean isSuspended() {
        return (suspended.get());
    }

    public final boolean isAcceptingTasks() {
        return (acceptingTasks.get());
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
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="private methods">
    private boolean canHandleMore() {
        return (taskQueue.isEmpty());
//		int size = 0;
//
//		synchronized(taskQueue)
//		{
//			// FIXME: size may not be a constant time operation! slow?
//			size = taskQueue.size();
//		}
//
//		// queue is empty, assume new agent
//		if(size == 0)
//		{
//			return(true);
//		}
//
//		long actualExecutionTime = System.nanoTime() - lastStartTime;
//
//		// calculate thresholds for queue size adaption
//		long upperThreshold = averageExecutionTime + (averageExecutionTime / 2);
//		long lowerThreshold = averageExecutionTime - (averageExecutionTime / 2);
//
//		if(actualExecutionTime > upperThreshold && maxQueueSize > 2)
//		{
//			// FIXME
//
//			maxQueueSize -= 2;
//
//			// do not take the next task
//			return(false);
//
//		} else
//		if(actualExecutionTime < lowerThreshold && maxQueueSize < 200)
//		{
//			// FIXME
//
//			maxQueueSize += 2;
//
//			// can take the next task
//			return(true);
//		}
//
//
//		return(size < maxQueueSize);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="abstract methods">
    public abstract Class getSupportedTaskType();

    /**
     * This method will be called by the AgentService
     * @param task
     */
    public abstract ReturnValue processTask(Task task) throws Throwable;
    // </editor-fold>
}
