/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Delayed;
import org.structr.core.entity.StructrNode;

/**
 * An experimental interface description for a structr task.
 * To be discussed..
 *
 * First thoughts:
 *  - tasks operate on one or more nodes
 *  - tasks can have a priority
 *  - tasks can be scheduled for later execution
 *  - no need to hold a reference to the db since each node knows its
 *    GraphDatabaseService.
 *  - tasks whose scheduledTime has expired will be handeled with highest
 *    priority?
 *
 *Questions:
 *  - can / should tasks depend on other tasks? It might be simpler to
 *    see tasks as single, self-contained entities.
 *  - identification: Java class name or String identifier?
 *  - implement Delayed for DelayQueue? what about priority?
 *
 * @author cmorgner
 */
public interface Task extends Delayed, StatusInfo
{
	/**
	 * Returns the nodes this task should operate on.
	 *
	 * TODO: Set or List?
	 * TODO: StructrNode or StructrNode?
	 *
	 * @return a set of nodes relevant to this task.
	 */
	public Set<StructrNode> getNodes();

	/**
	 * Returns the priority of this task.
	 *
	 * @return the priority of this task
	 */
	public int priority();

	/**
	 * Returns the time this task is scheduled for.
	 *
	 * TODO: return Date, long, or Calendar, or something else?
	 * TODO: relative / absolute time? (relative only with timestamped tasks)
	 *
	 * @return
	 */
	public Date getScheduledTime();

	/**
	 * Returns the time this task was created.
	 *
	 * @return the time this task was created
	 */
	public Date getCreationTime();
}
