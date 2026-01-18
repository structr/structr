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
package org.structr.console.shell;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.List;

/**
 * A console command that displays help texts for other console commands.
 */
public class HelpConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("help", HelpConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		if (parameters.size() > 1) {

			final String key              = parameters.get(1);
			final AdminConsoleCommand cmd = AdminConsoleCommand.getCommand(key);

			if (cmd != null) {

				cmd.detailHelp(writable);

			} else {

				writable.println("Unknown command '" + key + "'.");
			}

		} else {

			int maxCommandNameLength = 0;

			for (final String key : AdminConsoleCommand.commandNames()) {
				maxCommandNameLength = Math.max(maxCommandNameLength, key.length());
			}

			for (final String key : AdminConsoleCommand.commandNames()) {

				final AdminConsoleCommand cmd = AdminConsoleCommand.getCommand(key);

				writable.print(StringUtils.rightPad(key, maxCommandNameLength));
				writable.print(" - ");
				cmd.commandHelp(writable);
			}
		}
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
