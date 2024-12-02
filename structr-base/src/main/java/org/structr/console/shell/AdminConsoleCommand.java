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
package org.structr.console.shell;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.*;

public abstract class AdminConsoleCommand {

	private static final Map<String, Class<? extends AdminConsoleCommand>> commands = new TreeMap<>();

	public abstract void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException;
	public abstract void commandHelp(final Writable writable) throws IOException;
	public abstract void detailHelp(final Writable writable) throws IOException;

	public static Set<String> commandNames() {
		return commands.keySet();
	}

	public static void registerCommand(final String name, final Class<? extends AdminConsoleCommand> cmd) {
		commands.put(name, cmd);
	}

	public static AdminConsoleCommand getCommand(final String name) {

		final Class<? extends AdminConsoleCommand> cls = commands.get(name);
		if (cls != null) {

			try {

				return cls.getDeclaredConstructor().newInstance();

			} catch (Throwable t) {}
		}

		return null;
	}

	/**
	 * Override this method if the admin console command will create its own
	 * transaction context (or does not require/allow one at all)
	 *
	 * The basic technique is identical to AbstractCommand.requiresEnclosingTransaction.
	 * It is replicated here because the main ConsoleCommand delegates this down to
	 * the ConsoleCommands as these are commands 'wrapped' in a websocket command and
	 * at least one of the commands (ImportCommand) does not work properly if a surrounding
	 * transaction is created.
	 *
	 * @return a boolean
	 */
	public boolean requiresEnclosingTransaction() {
		return true;
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

	protected Map<String, Object> toMap(final String key, final Object value, final String key2, final Object value2, final String key3, final Object value3, final String key4, final Object value4) {

		final Map<String, Object> map = toMap(key, value, key2, value2, key3, value3);

		if (key4 != null && value4 != null) {
			map.put(key4, value4);
		}

		return map;
	}

	protected Map<String, Object> toMap(final String key, final Object value, final String key2, final Object value2, final String key3, final Object value3, final String key4, final Object value4, final String key5, final Object value5) {

		final Map<String, Object> map = toMap(key, value, key2, value2, key3, value3, key4, value4);

		if (key5 != null && value5 != null) {
			map.put(key5, value5);
		}

		return map;
	}
}
