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
package org.structr.common.error;

import java.util.Set;

/**
 * Exception to be thrown when an unlicensed scripting function is encountered.
 */
public class UnlicensedTypeException extends Exception {

	private Set<String> types = null;

	public UnlicensedTypeException(final Set<String> types) {

		super(types + " are not part of the currently licensed edition.");

		this.types = types;
	}

	public Set<String> getTypes() {
		return this.types;
	}
}
