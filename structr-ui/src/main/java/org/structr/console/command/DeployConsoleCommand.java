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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.util.Writable;
import org.structr.web.maintenance.DeployCommand;

/**
 *
 */
public class DeployConsoleCommand extends ConsoleCommand {

	static {
		ConsoleCommand.registerCommand("deploy", DeployConsoleCommand.class);
	}

	@Override
	public String run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException {

		final StringBuilder buf = new StringBuilder("\r\n");
		final String command    = getParameter(parameters, 1);

		if (command != null) {

			final Principal user = securityContext.getUser(false);
			if (user != null && user.isAdmin()) {

				final DeployCommand cmd = StructrApp.getInstance(securityContext).command(DeployCommand.class);
				cmd.setLogBuffer(writable);

				switch (command) {

					case "import":
						cmd.execute(toMap("mode", "import", "source", getParameter(parameters, 2)));
						break;

					case "export":
						cmd.execute(toMap("mode", "export", "target", getParameter(parameters, 2)));
						break;
				}

			} else {

				buf.append("You must be admin user to use this command.");
			}

		} else {

			buf.append("Missing command, must be one of 'export' or 'import'.");
		}

		return buf.toString();
	}

	@Override
	public String commandHelp() {
		return "Manages deployments on this Structr instance.";
	}

	@Override
	public String detailHelp() {

		final StringBuilder buf = new StringBuilder();

		buf.append("deploy export <target> - exports this application to the given target directory.\r\n");
		buf.append("deploy import <source> - imports an application from the given target directory.\r\n");

		return buf.toString();
	}
}
