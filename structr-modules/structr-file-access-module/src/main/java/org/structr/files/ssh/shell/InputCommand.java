/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.ssh.shell;

import java.io.IOException;

/**
 *
 *
 */
public class InputCommand extends InteractiveShellCommand {

	@Override
	public void handleLine(final String line) throws IOException {

		term.println("Hello " + line + ", nice to meet you!");
		term.restoreRootTerminalHandler();
	}

	@Override
	public void displayPrompt() throws IOException {

		term.print("Test input command: please enter your name: ");
	}

	@Override
	public void handleLogoutRequest() throws IOException {
	}
}
