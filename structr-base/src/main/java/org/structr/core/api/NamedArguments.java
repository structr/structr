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

import org.graalvm.polyglot.Value;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for arguments that can be passed to Method implementations.
 * This class exists because we support several different ways to call a
 * method, and we might need map-like argument objects as well as Java-
 * like argument passing (arg0, arg1, arg2) etc.
 */
public class NamedArguments extends Arguments {

	@Override
	public String toString() {
		return this.arguments.toString();
	}

	public void add(final Entry<String, Object> entry) {
		arguments.add(new Argument(entry.getKey(), entry.getValue()));
	}

	public void add(final String name, final Object value) {
		arguments.add(new Argument(name, value));
	}

	public void prepend(final String name, final Object value) {
		arguments.add(0, new Argument(name, value));
	}

	public Object get(final String name) {

		for (final Argument a : arguments) {

			if (name.equals(a.getName())) {

				return a.getValue();
			}
		}

		return null;
	}

	public static Arguments fromMap(final Map<String, Object> map) {

		final NamedArguments arguments = new NamedArguments();

		for (final Entry<String, Object> entry : map.entrySet()) {

			arguments.add(entry.getKey(), entry.getValue());
		}

		return arguments;
	}

	public static NamedArguments fromValues(final ActionContext actionContext, final Value... values) {

		final NamedArguments arguments = new NamedArguments();

		for (final Value value : values) {

			final Object unwrapped = PolyglotWrapper.unwrap(actionContext, value);
			if (unwrapped instanceof Map) {

				final Map<String, Object> map = (Map<String, Object>) unwrapped;
				for (final Entry<String, Object> entry : map.entrySet()) {

					arguments.add(entry);
				}

			} else {

				throw new IllegalArgumentTypeException();
			}
		}

		return arguments;
	}
}
