/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.server.*;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.console.Console.ConsoleMode;
import org.structr.console.tabcompletion.TabCompletionResult;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.util.Writable;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.User;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class StructrConsoleCommand implements Command, SignalListener, TerminalHandler {

	private static final Logger logger = LoggerFactory.getLogger(StructrConsoleCommand.class.getName());

	private final List<String> commandHistory  = new LinkedList<>();
	private final StringBuilder lastBlockChars = new StringBuilder();
	private final StringBuilder buf            = new StringBuilder();
	private ConsoleMode consoleMode            = ConsoleMode.JavaScript;
	private String command                     = null;
	private Console console                    = null;
	private TerminalEmulator term              = null;
	private ExitCallback callback              = null;
	private InputStream in                     = null;
	private OutputStream out                   = null;
	private User user                          = null;
	private int inBlock                        = 0;
	private int inSingleQuotes                 = 0;
	private int inDoubleQuotes                 = 0;
	private int inArray                        = 0;
	private int inBraces                       = 0;

	public StructrConsoleCommand(final SecurityContext securityContext) {
		this(securityContext, ConsoleMode.JavaScript, null);
	}

	public StructrConsoleCommand(final SecurityContext securityContext, final ConsoleMode consoleMode, final String command) {

		this.console     = new Console(securityContext, consoleMode, null);
		this.consoleMode = consoleMode;
		this.command     = command;

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
	public void start(final ChannelSession session, final Environment env) throws IOException {

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

		// abort if no user was found
		if (user == null) {

			logger.warn("Cannot start Structr shell, user not found for name {}!", userName);
			return;
		}

		final String terminalType = env.getEnv().get("TERM");
		if (terminalType != null) {

			if (terminalType.startsWith("xterm") || terminalType.startsWith("vt100") || terminalType.startsWith("vt220")) {

				term = new XTermTerminalEmulator(in, out, this);

			} else {

				logger.warn("Unsupported terminal type {}, aborting.", terminalType);
			}

			logger.warn("No terminal type provided, aborting.", terminalType);
		}

		if (term != null) {

			term.start();

			term.print("Welcome to the ");
			term.setBold(true);
			term.print("Structr " + VersionHelper.getFullVersionInfo());
			term.setBold(false);
			term.print(" JavaScript console. Use <Shift>+<Tab> to switch modes.");
			term.println();

			// display first prompt
			displayPrompt();

		} else {

			final OutputStreamWritable writable = new OutputStreamWritable(out);

			if (command != null) {

				try {

					this.console.run(command, writable);

				} catch (FrameworkException fex) {

					writable.println(fex.getMessage());
				}

			} else {

				writable.println("No command specified, aborting.");
			}

			callback.onExit(1);
		}
	}

	@Override
	public void destroy(ChannelSession channelSession) throws Exception {

	}

	@Override
	public void handleLine(final String line) throws IOException {

		try {
			term.flush();

			if (StringUtils.isNotBlank(line)) {

				if (insideOfBlockOrStructure()) {

					buf.append(line);
					buf.append("\r\n");

				} else {

					buf.append(line);
				}

				if ("exit".equals(line) || "quit".equals(line)) {

						term.stopEmulator();

				} else {

					checkForBlockChars(line.trim());

					if (!insideOfBlockOrStructure()) {

						final String command = buf.toString();

						clearBlockStatus();

						commandHistory.add(command);

						try (final Tx tx = StructrApp.getInstance(console.getSecurityContext()).tx()) {

							console.run(command, term);

							tx.success();

						} catch (Throwable t) {

							final String message = t.getMessage();
							if (message != null) {

								term.println(message);

							} else {

								logger.warn("", t);
								term.println(t.getClass().getSimpleName() + " encountered.");
							}
						}
					}
				}
			}

		} catch (Throwable t) {

			logger.warn("", t);
			term.println(t.getClass().getSimpleName() + " encountered.");

		} finally {

			term.flush();
		}
	}

	@Override
	public void handleExit() {

		if (callback != null) {

			callback.onExit(0);
		}
	}

	public String getPrompt() {

		final StringBuilder buffer = new StringBuilder();

		buffer.append("\u001b[1m");
		buffer.append(console.getPrompt());

		if (insideOfBlockOrStructure() && lastBlockChars.length() > 0) {
			buffer.append(lastBlockChars.charAt(lastBlockChars.length() - 1));
		} else {
			buffer.append("/");
		}

		buffer.append(">");
		buffer.append("\u001b[0m");
		buffer.append(" ");

		return buffer.toString();
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
		term.setBold(true);
		term.setTextColor(7);
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

		clearBlockStatus();

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
	public void handleShiftTab() throws IOException {

		if (!insideOfBlockOrStructure()) {

			switch (consoleMode) {

				case REST:
					consoleMode = ConsoleMode.JavaScript;
					break;

				case JavaScript:
					consoleMode = ConsoleMode.StructrScript;
					break;

				case StructrScript:
					consoleMode = ConsoleMode.Cypher;
					break;

				case Cypher:
					consoleMode = ConsoleMode.AdminShell;
					break;

				case AdminShell:
					consoleMode = ConsoleMode.REST;
					break;
			}

			term.handleString("Console.setMode('" + consoleMode.name() + "')");
			term.clearTabCount();

			term.setBold(true);
			term.setTextColor(3);
			term.handleNewline();
		}
	}

	@Override
	public void handleTab(final int tabCount) throws IOException {

		if (!insideOfBlockOrStructure()) {

			final StringBuilder lineBuffer                = term.getLineBuffer();
			final List<TabCompletionResult> tabCompletion = console.getTabCompletion(lineBuffer.toString());

			if (!tabCompletion.isEmpty()) {

				// exactly one result => success
				if (tabCompletion.size() == 1) {

					final TabCompletionResult result = tabCompletion.iterator().next();
					term.handleString(result.getCompletion());
					term.handleString(result.getSuffix());
					term.clearTabCount();

				} else {

					if (tabCount > 1) {

						// display alternatives
						term.println();

						for (final Iterator<TabCompletionResult> it = tabCompletion.iterator(); it.hasNext();) {

							final TabCompletionResult result = it.next();

							term.print(result.getCommand());

							if (it.hasNext()) {

								term.print(" ");
							}
						}

						term.println();
						term.print(getPrompt());
						term.print(term.getLineBuffer().toString());
					}
				}
			}
		}
	}

	public void flush() throws IOException {

		if (term != null) {
			term.flush();
		}
	}

	// ----- private method -----
	private boolean insideOfBlockOrStructure() {

		final boolean singleQuotes = (inSingleQuotes % 2) != 0;
		final boolean doubleQuotes = (inDoubleQuotes % 2) != 0;

		return inBlock > 0 || doubleQuotes || singleQuotes || inArray > 0 || inBraces > 0;
	}

	private void checkForBlockChars(final String line) {

		try {

			for (final char c : line.toCharArray()) {

				switch (c) {

					case '{':
						lastBlockChars.append("{");
						inBlock++;
						break;

					case '}':
						lastBlockChars.setLength(Math.max(0, lastBlockChars.length() - 1));
						inBlock--;
						break;

					case '[':
						lastBlockChars.append("[");
						inArray++;
						break;

					case ']':
						lastBlockChars.setLength(Math.max(0, lastBlockChars.length() - 1));
						inArray--;
						break;

					case '(':
						lastBlockChars.append("(");
						inBraces++;
						break;

					case ')':
						lastBlockChars.setLength(Math.max(0, lastBlockChars.length() - 1));
						inBraces--;
						break;

					case '"':
						inDoubleQuotes++;
						if ((inDoubleQuotes % 2) == 0) {
							lastBlockChars.setLength(Math.max(0, lastBlockChars.length() - 1));
						} else {
							lastBlockChars.append("\"");
						}
						break;

					case '\'':
						inSingleQuotes++;
						if ((inSingleQuotes % 2) == 0) {
							lastBlockChars.setLength(Math.max(0, lastBlockChars.length() - 1));
						} else {
							lastBlockChars.append("'");
						}
						break;
				}
			}

		} catch (Throwable t) {}
	}

	private void clearBlockStatus() {

		inSingleQuotes = 0;
		inDoubleQuotes = 0;
		inBraces = 0;
		inBlock = 0;
		inArray = 0;

		buf.setLength(0);
	}

	@Override
	public void signal(Channel channel, Signal signal) {

	}

	// ----- nested classes -----
	private static class OutputStreamWritable implements Writable {

		private Writer writer = null;

		public OutputStreamWritable(final OutputStream out) {
			this.writer = new OutputStreamWriter(out);
		}

		@Override
		public void print(final Object... text) throws IOException {

			if (text != null) {

				for (final Object o : text) {

					if (o != null) {

						writer.write(o.toString().replaceAll("\n", "\r\n"));

					} else {

						writer.write("null");
					}
				}
			}

			writer.flush();
		}

		@Override
		public void println(final Object... text) throws IOException {

			print(text);
			println();
			writer.flush();
		}

		@Override
		public void println() throws IOException {
			writer.write(10);
			writer.write(13);
		}

		@Override
		public void flush() throws IOException {
			writer.flush();
		}
	}
}
