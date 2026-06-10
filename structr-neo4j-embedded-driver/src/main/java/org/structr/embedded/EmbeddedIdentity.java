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

	@Override
	public long hash() {
		// FIXME: this is the only place where we need the actual long value
		return id.hashCode();
	}

	// ----- interface Identity -----
	@Override
	public int compareTo(final Object o) {

		final String otherId = ((EmbeddedIdentity)o).getId();

		return id.compareTo(otherId);
	}
}
