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

import org.structr.core.entity.Principal;

import java.util.*;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 */
public class AbstractTask<T> implements Task<T> {

	private final Map<String, Object> statusProperties = new LinkedHashMap<>();
	private final List<T> objects                      = new LinkedList<>();
	private Principal user                             = null;
	private int priority                               = 0;
	private Date scheduledTime                         = null;
	private Date creationTime                          = null;
	private String type                                = null;
	private long delay                                 = 0L;
	private int retryCount                             = 0;

	public AbstractTask(final String type, final Principal user) {
		this(type, user, null);
	}
	
	public AbstractTask(final String type, final Principal user, final T node) {

		this.type = type;
		this.user = user;

		if (node != null) {
			this.objects.add(node);
		}
	}

	@Override
	public Principal getUser() {
		return user;
	}

	@Override
	public List<T> getWorkObjects() {
		return objects;
	}

	@Override
	public int priority() {
		return priority;
	}

	@Override
	public Date getScheduledTime() {
		return scheduledTime;
	}

	@Override
	public Date getCreationTime() {
		return creationTime;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public long getDelay(final TimeUnit unit) {

		if (unit != null) {
			return unit.convert(delay, TimeUnit.MILLISECONDS);
		}

		return delay;
	}

	@Override
	public Object getStatusProperty(final String key) {
		return statusProperties.get(key);
	}

	public void setStatusProperty(final String key, final Object value) {
		statusProperties.put(key, value);
	}

	public void addNode(final T node) {
		this.objects.add(node);
	}

	public void setUser(final Principal user) {
		this.user = user;
	}

	public void setPriority(final int priority) {
		this.priority = priority;
	}

	public void setScheduledTime(final Date scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	public void setCreationTime(final Date creationTime) {
		this.creationTime = creationTime;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public void setDelay(final long delay) {
		this.delay = delay;
	}

	@Override
	public int compareTo(final Delayed other) {

		final long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);

		return delay > otherDelay ? 1 : delay < otherDelay ? -1 : 0;
	}

	@Override
	public void incrementRetryCount() {
		retryCount++;
	}

	@Override
	public int getRetryCount() {
		return retryCount;
	}
}
