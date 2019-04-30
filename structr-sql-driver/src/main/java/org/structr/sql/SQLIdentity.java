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
package org.structr.sql;

import org.structr.api.graph.Identity;

/**
 */
public class SQLIdentity implements Identity {

	private String type = null;
	private String id   = null;

	public SQLIdentity(final String id, final String type) {
		this.type = type;
		this.id   = id;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	@Override
	public int compareTo(final Object o) {

		// provoke ClassCastException if type doesn't match
		final SQLIdentity other = (SQLIdentity)o;
		final String otherType  = other.getType();
		final String otherId    = other.getId();

		final int types = type.compareTo(otherType);
		if (types == 0) {

			return id.compareTo(otherId);
		}

		return types;
	}

	static SQLIdentity getInstance(final String id, final String type) {
		return new SQLIdentity(id, type);
	}

}
