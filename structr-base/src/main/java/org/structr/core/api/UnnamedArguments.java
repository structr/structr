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
package org.structr.core.api;

import java.util.List;

/**
 * Base class for arguments that can be passed to Method implementations.
 * This class exists because we support several different ways to call a
 * method, and we might need map-like argument objects as well as Java-
 * like argument passing (arg0, arg1, arg2) etc.
 */
public class UnnamedArguments extends Arguments {

	@Override
	public String toString() {
		return this.arguments.toString();
	}

	public void add(final Object value) {
		arguments.add(new Argument(null, value));
	}

	public void prepend(final Object value) {
		arguments.add(0, new Argument(null, value));
	}

	public boolean isEmpty() {
		return arguments.isEmpty();
	}

	public Object get(final int index) {

		if (arguments.size() > index) {

			final Argument a = arguments.get(index);

			return a.getValue();
		}

		return null;
	}

	public static UnnamedArguments fromPath(final List<String> parts) {

		final UnnamedArguments arguments = new UnnamedArguments();

		for (final String part : parts) {

			arguments.add(part);
		}

		return arguments;
	}
}
