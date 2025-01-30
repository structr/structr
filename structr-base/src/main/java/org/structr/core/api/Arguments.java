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

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Value;
import org.structr.common.SecurityContext;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.*;
import java.util.Map.Entry;

/**
 * Base class for arguments that can be passed to Method implementations.
 * This class exists because we support several different ways to call a
 * method, and we might need map-like argument objects as well as Java-
 * like argument passing (arg0, arg1, arg2) etc.
 */
public class Arguments {

	private final List<Argument> arguments = new LinkedList<>();

	@Override
	public String toString() {
		return this.arguments.toString();
	}

	public void add(final Entry<String, Object> entry) {
		arguments.add(new Argument(entry.getKey(), entry.getValue()));
	}

	public void add(final Object value) {
		arguments.add(new Argument(null, value));
	}

	public void add(final String name, final Object value) {
		arguments.add(new Argument(name, value));
	}

	public void prepend(final Object value) {
		arguments.add(0, new Argument(null, value));
	}

	public void prepend(final String name, final Object value) {
		arguments.add(0, new Argument(name, value));
	}

	public boolean isEmpty() {
		return arguments.isEmpty();
	}

	public Object get(final String name) {

		for (final Argument a : arguments) {

			if (name.equals(a.getName())) {

				return a.getValue();
			}
		}

		return null;
	}

	public Object get(final int index) {

		if (arguments.size() > index) {

			final Argument a = arguments.get(index);

			return a.getValue();
		}

		return null;
	}

	public static Arguments fromMap(final Map<String, Object> map) {

		final Arguments arguments = new Arguments();

		for (final Entry<String, Object> entry : map.entrySet()) {

			arguments.add(entry.getKey(), entry.getValue());
		}

		return arguments;
	}

	public static Arguments fromPath(final List<String> parts) {

		final Arguments arguments = new Arguments();

		for (final String part : parts) {

			arguments.add(part);
		}

		return arguments;
	}

	public static Arguments fromValues(final ActionContext actionContext, final Value... values) {

		final Arguments arguments = new Arguments();

		for (final Value value : values) {

			final Object unwrapped = PolyglotWrapper.unwrap(actionContext, value);
			if (unwrapped instanceof Map) {

				final Map<String, Object> map = (Map<String, Object>) unwrapped;
				for (final Entry<String, Object> entry : map.entrySet()) {

					arguments.add(entry);
				}

			} else {

				arguments.add(unwrapped);
			}
		}

		return arguments;
	}

	public Map<String, Object> toMap() {

		// this can only work if we have named arguments
		final Map<String, Object> map = new LinkedHashMap<>();

		for (final Argument a : arguments) {

			final String name  = a.getName();
			final Object value = a.getValue();

			if (name == null) {

				if (value instanceof Map m) {

					// support map arguments
					map.putAll(m);

				} else {

					throw new IllegalArgumentTypeException();
				}

			} else {

				map.put(name, value);
			}
		}

		return map;
	}

	public Object[] toArray() {

		final List<Object> result = new ArrayList();

		for (final Argument a : arguments) {
			result.add(a.getValue());
		}

		return result.toArray();
	}

	public List<Argument> getAll() {
		return arguments;
	}

	public String formatForErrorMessage() {

		final List<String> elements = new LinkedList<>();

		for (final Argument argument : arguments) {

			final Object value = argument.getValue();
			final String type  = value != null ? value.getClass().getSimpleName() : "null";

			// do not include internal SecurityContext parameter in error message
			if (value instanceof SecurityContext) {

				// omit

			} else {

				elements.add(type);
			}
		}

		return StringUtils.join(elements);
	}

	// ----- nested classes -----
	public class Argument {

		private String name  = null;
		private Object value = null;

		public Argument(final String name, final Object value) {

			this.name  = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "(" + name + " = " + value + ")";
		}

		public String getName() {
			return name;
		}

		public Object getValue() {
			return value;
		}
	}
}
