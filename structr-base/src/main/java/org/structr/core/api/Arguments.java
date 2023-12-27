/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;

/**
 * Base class for arguments that can be passed to Method implementations.
 * This class exists because we support several different ways to call a
 * method, and we might need map-like argument objects as well as Java-
 * like argument passing (arg0, arg1, arg2) etc.
 */
public class Arguments {

	private final List<Argument> arguments = new LinkedList<>();

	public void add(final Object value) {
		arguments.add(new Argument(null, value));
	}

	public void add(final String name, final Object value) {
		arguments.add(new Argument(name, value));
	}

	public static Arguments fromMap(final Map<String, Object> map) {

		final Arguments arguments = new Arguments();

		return arguments;
	}

	public Map<String, Object> toMap() throws FrameworkException {

		// this can only work if we have named argumentss

		final Map<String, Object> map = new LinkedHashMap<>();

		for (final Argument a : arguments) {

			final String name  = a.getName();
			final Object value = a.getValue();

			if (name == null) {

				// FIXME: error message is way too technical here..
				throw new FrameworkException(422, "Cannot use unnamed arguments in map-based method call.");
			}

			map.put(name, value);
		}

		return map;
	}

	public Object[] toArray() {

		return null;
	}

	private class Argument {

		private String name  = null;
		private Object value = null;

		public Argument(final String name, final Object value) {

			this.name  = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}
	}
}
