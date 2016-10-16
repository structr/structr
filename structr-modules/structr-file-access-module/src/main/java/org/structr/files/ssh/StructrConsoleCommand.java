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
package org.structr.files.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.User;

/**
 *
 *
 */
public class StructrConsoleCommand implements Command, SignalListener, TerminalHandler {

	private static final Logger logger = LoggerFactory.getLogger(StructrConsoleCommand.class.getName());

	private final List<String> commandHistory = new LinkedList<>();
	private StringBuilder buf                 = new StringBuilder();
	private Console console                   = null;
	private TerminalEmulator term             = null;
	private ExitCallback callback             = null;
	private InputStream in                    = null;
	private OutputStream out                  = null;
	private User user                         = null;
	private int inBlock                       = 0;
	private int inSingleQuotes                = 0;
	private int inDoubleQuotes                = 0;
	private int inArray                       = 0;
	private int inBraces                      = 0;

	public StructrConsoleCommand(final SecurityContext securityContext) {
		this.console = new Console(securityContext, null);
	}

	@Override
	public void setInputStream(final InputStream in) {
		this.in = in;
	}

	@Override
	public void setOutputStream(final OutputStream out) {
		this.out = out;
	}

	@Override
	public void setErrorStream(final OutputStream err) {
	}

	@Override
	public void setExitCallback(final ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(final Environment env) throws IOException {

		env.addSignalListener(this);

		final String userName = env.getEnv().get("USER");
		if (userName != null) {

			final App app = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				user = app.nodeQuery(User.class).andName(userName).getFirst();

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

		} else {

			logger.warn("Cannot start Structr shell, no username set!");

			return;
		}

		if (isInteractive()) {

			// abort if no user was found
			if (user == null) {

				logger.warn("Cannot start Structr shell, user not found for name {}!", userName);
				return;
			}

			final String terminalType = env.getEnv().get("TERM");
			if (terminalType != null) {

				switch (terminalType) {

					case "xterm":
					case "vt100":
					case "vt220":
						term = new XTermTerminalEmulator(in, out, this);
						break;

					default:
						logger.warn("Unsupported terminal type {}, aborting.", terminalType);
						break;


				}

				logger.warn("No terminal type provided, aborting.", terminalType);
			}

			if (term != null) {

				term.start();

				term.print("Welcome to the ");
				term.setBold(true);
				term.print("Structr");
				term.print(" 2.1 JavaScript console");
				term.setBold(false);
				term.println();

				// display first prompt
				displayPrompt();

			} else {

				callback.onExit(1);
			}

		} else {

			// create terminal emulation
			term = new XTermTerminalEmulator(in, out, this);
		}
	}

	@Override
	public void destroy() {

		if (term != null) {
			term.stopEmulator();
		}
	}

	@Override
	public void signal(final Signal signal) {
		logger.info("Received signal {}", signal.name());
	}

	@Override
	public void handleLine(final String line) throws IOException {

		if (StringUtils.isNotBlank(line)) {

			if (insideOfSomething()) {

				buf.append(line);
				buf.append("\n");

			} else {

				buf.append(line);
			}

			checkForBlockChars(line.trim());

			if (!insideOfSomething()) {

				commandHistory.add(buf.toString());

				try (final Tx tx = StructrApp.getInstance(console.getSecurityContext()).tx()) {

					term.println(console.run(buf.toString()));

					tx.success();

				} catch (FrameworkException fex) {

					term.println(fex.getMessage());
				}

				buf.setLength(0);

				inSingleQuotes = 0;
				inDoubleQuotes = 0;
				inBraces = 0;
				inBlock = 0;
				inArray = 0;

			}
		}
	}

	@Override
	public void handleExit() {

		if (callback != null) {

			callback.onExit(0);
		}
	}

	public String getPrompt() {

		final StringBuilder buf = new StringBuilder();

		if (insideOfSomething()) {

			int indent = 0;

			if ((inSingleQuotes % 2) != 0) {
				buf.append("'");
				indent++;
			}

			if ((inDoubleQuotes % 2) != 0) {
				buf.append("\"");
				indent++;
			}

			if (inArray > 0) {
				buf.append("[");
				indent++;
			}

			if (inBraces > 0) {
				buf.append("(");
				indent++;
			}

			if (inBlock > 0) {
				buf.append("{");
				indent++;
			}

			for (int i=0; i<indent; i++) {
				buf.append("    ");
			}

		} else {

			buf.append("\u001b[1m");
			buf.append("Structr >");
			buf.append("\u001b[0m");
			buf.append(" ");
		}

		return buf.toString();
	}

	public boolean isAllowed(final AbstractFile file, final Permission permission, final boolean explicit) {

		if (file == null) {
			return false;
		}

		final SecurityContext securityContext = SecurityContext.getInstance(user, AccessMode.Backend);

		if (Permission.read.equals(permission) && !explicit) {

			return file.isVisibleToAuthenticatedUsers() || file.isVisibleToPublicUsers() || file.isGranted(permission, securityContext);
		}

		return file.isGranted(permission, securityContext);
	}

	// ----- private methods -----
	@Override
	public List<String> getCommandHistory() {
		return commandHistory;
	}

	@Override
	public void displayPrompt() throws IOException {

		// output prompt
		term.print(getPrompt());
	}

	@Override
	public void handleLogoutRequest() throws IOException {

		// Ctrl-D is logout
		term.println("logout");
		term.stopEmulator();

	}

	@Override
	public void handleCtrlC() throws IOException {

		// Ctrl-C
		term.print("^C");
		term.clearLineBuffer();
		term.handleNewline();
	}

	@Override
	public void setUser(final User user) {
		this.user = user;
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public void handleTab(final int tabCount) throws IOException {
	}

	public boolean isInteractive() {
		return true;
	}

	public void flush() throws IOException {

		if (term != null) {
			term.flush();
		}
	}

	// ----- private method -----
	private boolean insideOfSomething() {

		final boolean singleQuotes = (inSingleQuotes % 2) != 0;
		final boolean doubleQuotes = (inDoubleQuotes % 2) != 0;

		return inBlock > 0 || doubleQuotes || singleQuotes || inArray > 0 || inBraces > 0;
	}

	private void checkForBlockChars(final String line) {

		for (final char c : line.toCharArray()) {

			switch (c) {

				case '{':
					inBlock++;
					break;

				case '}':
					inBlock--;
					break;

				case '[':
					inArray++;
					break;

				case ']':
					inArray--;
					break;

				case '(':
					inBraces++;
					break;

				case ')':
					inBraces--;
					break;

				case '"':
					inDoubleQuotes++;
					break;

				case '\'':
					inSingleQuotes++;
					break;
			}
		}
	}
}
