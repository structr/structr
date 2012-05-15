/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.agent;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Delayed;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

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
public interface Task extends Delayed, StatusInfo {

    /**
     * Principal to process the task as
     *
     * @return
     */
    public Principal getUser();

    /**
     * Returns the nodes this task should operate on.
     *
     * TODO: Set or List?
     *
     * @return a set of nodes relevant to this task.
     */
    public Set<AbstractNode> getNodes();

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

    /**
     * Returns the task type
     *
     * @return the task type
     */
    public String getType();
}
