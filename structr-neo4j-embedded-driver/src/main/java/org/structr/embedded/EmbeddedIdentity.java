/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.embedded;

import org.structr.api.graph.Identity;

/**
 */
public class EmbeddedIdentity implements Identity<String> {

	private final String id;

	public EmbeddedIdentity(final String id) {

		this.id  = id;
	}

	@Override
	public String getId() {

		return id;
	}

	@Override
	public String toString() {

		return id;
	}

	@Override
	public boolean equals(final Object other) {

		return ((EmbeddedIdentity)other).getId().equals(id);
	}

	@Override
	public int hashCode() {

		return id.hashCode();
	}

	/**
	 * A 64-bit hash of the element id. The other drivers can return their numeric id here (see
	 * BoltIdentity and MemoryIdentity), this one identifies elements by string, so the value has to be
	 * derived. String.hashCode() would only fill 32 of the 64 bits and collide accordingly - the value
	 * keys the modification state of a transaction (ModificationQueue), where a collision would merge
	 * two different elements. It is never persisted, so the algorithm can change at any time.
	 */
	@Override
	public long hash() {

		// FNV-1a, 64 bit
		long hash = 0xcbf29ce484222325L;

		for (int i = 0; i < id.length(); i++) {

			hash ^= id.charAt(i);
			hash *= 0x100000001b3L;
		}

		return hash;
	}

	// ----- interface Identity -----
	@Override
	public int compareTo(final Object o) {

		final String otherId = ((EmbeddedIdentity)o).getId();

		return id.compareTo(otherId);
	}
}
