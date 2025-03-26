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
package org.structr.bolt;

import org.structr.api.graph.Identity;

/**
 */
public class BoltIdentity implements Identity {

	private final long id;
	private final String str;

	public BoltIdentity(final long id) {

		this.id  = id;
		this.str = Long.toString(id);
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return str;
	}

	@Override
	public boolean equals(final Object other) {
		return ((BoltIdentity)other).getId() == id;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(id).hashCode();
	}

	// ----- interface Identity -----
	@Override
	public int compareTo(final Object o) {

		final long otherId = ((BoltIdentity)o).getId();

		if (id > otherId) {
			return 1;
		}

		if (id < otherId) {
			return -1;
		}

		return 0;
	}
}
