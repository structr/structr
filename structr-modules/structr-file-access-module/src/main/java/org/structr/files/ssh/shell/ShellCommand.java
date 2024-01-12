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

import org.structr.files.ssh.StructrShellCommand;
import org.structr.files.ssh.TerminalEmulator;
import org.structr.files.ssh.TerminalHandler;

import java.io.IOException;

/**
 *
 *
 */
public interface ShellCommand extends TerminalHandler {

	public void execute(final StructrShellCommand parent) throws IOException;
	public void setTerminalEmulator(final TerminalEmulator term);
	public void setCommand(final String command) throws IOException;
	public void handleTabCompletion(final StructrShellCommand parent, final String line, final int tabCount) throws IOException;
}
