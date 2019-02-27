/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.memory;

import java.util.concurrent.atomic.AtomicLong;
import org.structr.api.graph.Identity;

/**
 *
 * @author Christian Morgner
 */
public class MemoryIdentity implements Identity {

	private static final AtomicLong idCounter = new AtomicLong();
	private String type                       = null;
	private long id                           = -1L;

	public MemoryIdentity(final String type) {

		this.type = type;
		this.id   = idCounter.getAndIncrement();
	}

	@Override
	public String toString() {
		return Long.toString(id);
	}

	@Override
	public int hashCode() {
		return Long.valueOf(id).hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return id == ((MemoryIdentity)other).getId();
	}

	@Override
	public int compareTo(final Object o) {

		final long other = ((MemoryIdentity)o).getId();

		if (id > other) {
			return 1;
		}

		if (id < other) {
			return -1;
		}

		return 0;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}
}
