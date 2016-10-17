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
package org.structr.console.command;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.util.Writable;

/**
 *
 */
public class HelpConsoleCommand extends ConsoleCommand {

	static {
		ConsoleCommand.registerCommand("help", HelpConsoleCommand.class);
	}

	@Override
	public String run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException {

		final StringBuilder buf = new StringBuilder("\r\n");

		if (parameters.size() > 1) {

			final String key         = parameters.get(1);
			final ConsoleCommand cmd = ConsoleCommand.getCommand(key);

			if (cmd != null) {

				buf.append(cmd.detailHelp());
				buf.append("\r\n");

			} else {

				buf.append("Unknown command '");
				buf.append(key);
				buf.append("'");
			}

		} else {

			for (final String key : ConsoleCommand.commandNames()) {

				final ConsoleCommand cmd = ConsoleCommand.getCommand(key);

				buf.append(StringUtils.rightPad(key, 10));
				buf.append(" - ");
				buf.append(cmd.commandHelp());
				buf.append("\r\n");
			}
		}

		return buf.toString();
	}

	@Override
	public String commandHelp() {
		return "Prints a list of all commands and a short help text. Use 'help <command> to get more details.";
	}

	@Override
	public String detailHelp() {
		return commandHelp();
	}
}
