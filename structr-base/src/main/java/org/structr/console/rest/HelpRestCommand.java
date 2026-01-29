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
public class HelpRestCommand extends RestCommand {

	private String subCommand = null;

	static {

		RestCommand.registerCommand("help", HelpRestCommand.class);
	}

	@Override
	public void run(final Console console, final Writable writable) throws FrameworkException, IOException {

		if (subCommand != null) {

			final RestCommand cmd = RestCommand.getCommand(subCommand);
			if (subCommand != null) {

				cmd.detailHelp(writable);

			} else {

				writable.println("Unknown command '" + subCommand + "'.");
			}

		} else {

			for (final String key : RestCommand.commandNames()) {

				final RestCommand cmd = RestCommand.getCommand(key);

				writable.print(StringUtils.rightPad(key, 10));
				writable.print(" - ");
				cmd.commandHelp(writable);
			}
		}
	}

	@Override
	public boolean parseNext(final String line, final Writable writable) throws IOException {

		if (StringUtils.isNotBlank(line)) {

			this.subCommand = StringUtils.substringBefore(line, " ").trim();
		}

		return true;
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Prints a list of all commands and a short help text. Use 'help <command> to get more details.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		commandHelp(writable);
	}

}
