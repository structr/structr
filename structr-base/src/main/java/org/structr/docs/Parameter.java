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
package org.structr.docs;

public class Parameter {

	private final String name;
	private final String description;
	private final boolean optional;

	public Parameter(final String name, final String description, final boolean optional) {
		this.name = name;
		this.description = description;
		this.optional = optional;
	}

	public Parameter(final String name, final String description) {
		this(name, description, false);
	}

	public Parameter(final String name) {
		this(name, null, false);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isOptional() {
		return optional;
	}

	public static Parameter of(final String name, final String description, final boolean optional) {
		return new Parameter(name, description, optional);
	}

	public static Parameter optional(final String name, final String description) {
		return new Parameter(name, description, true);
	}

	public static Parameter mandatory(final String name, final String description) {
		return new Parameter(name, description, false);
	}
}
