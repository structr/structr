/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.core.entity.PrincipalInterface;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Delayed;

/**
 * A task that an {@link Agent} can operate on.
 */
public interface Task<T> extends Delayed, StatusInfo {

	/**
	 * Principal to process the task as
	 *
	 * @return user
	 */
	PrincipalInterface getUser();

	/**
	 * Returns the objects this task should operate on.
	 *
	 * @return a list of objects relevant to this task.
	 */
	List<T> getWorkObjects();

	/**
	 * Returns the priority of this task.
	 *
	 * @return the priority of this task
	 */
	int priority();

	/**
	 * Returns the time this task is scheduled for.
	 *
	 * TODO: return Date, long, or Calendar, or something else? TODO: relative / absolute time? (relative only with timestamped tasks)
	 *
	 * @return date
	 */
	Date getScheduledTime();

	/**
	 * Returns the time this task was created.
	 *
	 * @return the time this task was created
	 */
	Date getCreationTime();

	/**
	 * Returns the task type
	 *
	 * @return the task type
	 */
	String getType();

	/**
	 * Increment retry count to allow agent service to
	 * limit the number of retries.
	 */
	void incrementRetryCount();

	/**
	 * Return retry count
	 * @return the retry count
	 */
	int getRetryCount();
}
