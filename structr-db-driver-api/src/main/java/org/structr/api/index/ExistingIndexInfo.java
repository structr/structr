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
package org.structr.api.index;

import java.util.List;

public class ExistingIndexInfo {

	private final String identifier;
	private final String state;
	private final String type;
	private final List<String> properties;

	public ExistingIndexInfo(final String identifier, final String state, final String type) {

		this(identifier, state, type, null);
	}

	/**
	 * @param properties the indexed properties, or null if the database does not report them
	 */
	public ExistingIndexInfo(final String identifier, final String state, final String type, final List<String> properties) {

		this.identifier = identifier;
		this.state      = state;
		this.type       = type;
		this.properties = properties != null ? properties.stream().sorted().toList() : null;
	}

	public String getIdentifier() {

		return identifier;
	}

	public String getState() {

		return state;
	}

	public String getType() {

		return type;
	}

	/**
	 * The indexed properties, sorted, or null if unknown.
	 */
	public List<String> getProperties() {

		return properties;
	}
}
