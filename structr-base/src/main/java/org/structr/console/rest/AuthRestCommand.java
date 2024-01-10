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
package org.structr.console.rest;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.util.Writable;

import java.io.IOException;

/**
 *
 */
public class AuthRestCommand extends RestCommand {

	protected String username = null;
	protected String password = null;

	static {

		RestCommand.registerCommand("auth", AuthRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {
		console.setUsername(username);
		console.setPassword(password);
	}

	@Override
	public boolean parseNext(final String line, final Writable writable) throws IOException {

		if (StringUtils.isNotBlank(line)) {

			final String[] parts = line.split("[\\s]+");
			if (parts.length == 2) {

				username = parts[0];
				password = parts[1];
			}

		} else {

			username = null;
			password = null;
		}

		return true;
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Sets authentication information for subsequent requests.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("auth <username> <password> - Sets authentication information for subsequent requests. Run without parameters to reset credentials.");
	}
}
