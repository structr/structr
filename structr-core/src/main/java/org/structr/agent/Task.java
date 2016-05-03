/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Date;
import java.util.List;
import java.util.concurrent.Delayed;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;

/**
 * A task that an {@link Agent} can operate on.

 *
 */
public interface Task<T extends NodeInterface> extends Delayed, StatusInfo {

    /**
     * Principal to process the task as
     *
     * @return user
     */
    public Principal getUser();

    /**
     * Returns the nodes this task should operate on.
     *
     * @return a set of nodes relevant to this task.
     */
    public List<T> getNodes();

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
     * @return date
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
