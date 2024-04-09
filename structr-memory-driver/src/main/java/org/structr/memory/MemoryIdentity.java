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
package org.structr.memory;

import org.structr.api.graph.Identity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
public class MemoryIdentity implements Identity {

	private static final AtomicLong idCounter = new AtomicLong();
	private boolean isNode                    = false;
	private String type                       = null;
	private long id                           = -1L;

	private MemoryIdentity() throws IOException {
	}

	public MemoryIdentity(final boolean isNode, final String type) {

		this.id     = idCounter.getAndIncrement();
		this.isNode = isNode;
		this.type   = type;
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

	public boolean isNode() {
		return isNode;
	}


	// ----- package-private methods -----
	static MemoryIdentity loadFromStorage(final ObjectInputStream in) throws IOException {

		final MemoryIdentity identity = new MemoryIdentity();

		identity.isNode = in.readBoolean();
		identity.type   = in.readUTF();
		identity.id     = in.readLong();

		return identity;
	}

	void writeToStorage(final ObjectOutputStream out) throws IOException {

		out.writeBoolean(isNode);
		out.writeUTF(type);
		out.writeLong(id);
	}
}
