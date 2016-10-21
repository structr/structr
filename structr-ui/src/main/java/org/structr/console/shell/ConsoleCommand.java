/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.console.shell;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.util.Writable;

/**
 *
 */
public abstract class ConsoleCommand {

	private static final Map<String, Class<? extends ConsoleCommand>> commands = new TreeMap<>();

	public abstract void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException;
	public abstract void commandHelp(final Writable writable) throws IOException;
	public abstract void detailHelp(final Writable writable) throws IOException;

	public static Set<String> commandNames() {
		return commands.keySet();
	}

	public static void registerCommand(final String name, final Class<? extends ConsoleCommand> cmd) {
		commands.put(name, cmd);
	}

	public static ConsoleCommand getCommand(final String name) {

		final Class<? extends ConsoleCommand> cls = commands.get(name);
		if (cls != null) {

			try {

				return cls.newInstance();

			} catch (Throwable t) {}
		}

		return null;
	}

	// ----- protected methods -----
	protected String getParameter(final List<String> params, final int index) {

		if (params.size() > index) {

			return params.get(index);
		}

		return null;
	}

	protected Map<String, Object> toMap(final String key, final Object value) {

		final Map<String, Object> map = new HashMap<>();

		if (key != null && value != null) {
			map.put(key, value);
		}

		return map;
	}

	protected Map<String, Object> toMap(final String key, final Object value, final String key2, final Object value2) {

		final Map<String, Object> map = toMap(key, value);

		if (key2 != null && value2 != null) {
			map.put(key2, value2);
		}

		return map;
	}

	protected Map<String, Object> toMap(final String key, final Object value, final String key2, final Object value2, final String key3, final Object value3) {

		final Map<String, Object> map = toMap(key, value, key2, value2);

		if (key3 != null && value3 != null) {
			map.put(key3, value3);
		}

		return map;
	}
}
