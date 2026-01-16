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
package org.structr.console.rest;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 */
public class AsRestCommand extends RestCommand {

	private RestCommand command = null;

	static {

		RestCommand.registerCommand("as", AsRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {
		command.run(console, writable);
	}

	@Override
	public boolean parseNext(final String line, final Writable writable) throws IOException {

		final String trimmed    = line.trim();
		final String userString = StringUtils.substringBefore(trimmed, " ");
		final String remaining  = StringUtils.substringAfter(trimmed, " ");

		if (StringUtils.isNoneBlank(userString, remaining)) {

			command = RestCommand.parse(remaining, writable);
			if (command != null) {

				final String[] parts = userString.split("[:]+");
				if (parts.length == 2) {

					command.authenticate(parts[0], parts[1]);

					// success
					return true;

				} else {

					writable.println("Syntax error, user string must be <username>:<password>.");
				}
			}

		} else {

			writable.println("Syntax error, user string must be <username>:<password>.");
		}

		return false;
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Runs a REST command in the security context of a given user.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("as <user:password> <command> - Runs the given command as the given user.");
	}
}
