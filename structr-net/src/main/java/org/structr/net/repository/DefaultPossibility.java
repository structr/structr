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
package org.structr.net.repository;

import org.structr.net.data.time.Clock;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 */
public class DefaultPossibility {

	private static final long DEFAULT_TIMEOUT = 10000;

	public enum State {
		Waiting,
		Completed,
		Aborted
	}

	private Set<DefaultRepositoryObject> objects = new HashSet<>();
	private String uuid                          = null;
	private State state                          = State.Waiting;
	private Clock clock                          = null;
	private long timeout                         = 0L;

	public DefaultPossibility(final Clock clock) {
		this(clock, UUID.randomUUID().toString().replaceAll("\\-", ""));
	}

	public DefaultPossibility(final Clock clock, final long timeout) {
		this(clock, UUID.randomUUID().toString().replaceAll("\\-", ""), timeout);
	}

	public DefaultPossibility(final Clock clock, final String uuid) {
		this(clock, uuid, DEFAULT_TIMEOUT);
	}

	public DefaultPossibility(final Clock clock, final String uuid, final long timeout) {
		this(clock, uuid, timeout, State.Waiting);
	}

	public DefaultPossibility(final Clock clock, final String uuid, final long timeout, final State state) {

		this.clock   = clock;
		this.state   = state;
		this.uuid    = uuid;
		this.timeout = clock.getTime() + timeout;
	}

	@Override
	public String toString() {
		return "DefaultPossibility(" + uuid + ", " + state + ")";
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isComplete() {

		if (State.Completed.equals(state)) {
			return true;
		}

		if (isTimedOut()) {
			state = State.Aborted;
		}

		return false;
	}

	public boolean isAborted() {
		return State.Aborted.equals(state);
	}

	public void complete() {

		if (isTimedOut()) {

			this.state = State.Aborted;

		} else if (State.Waiting.equals(state)) {

			this.state = State.Completed;

			// commit notification
			for (final DefaultRepositoryObject obj : objects) {
				obj.onCommit(uuid);
			}
		}
	}

	public void addObject(final DefaultRepositoryObject obj) {
		objects.add(obj);
	}

	// ----- private methods -----
	private boolean isTimedOut() {

		if (clock.getTime() > timeout) {
			return true;
		}

		return false;
	}
}
