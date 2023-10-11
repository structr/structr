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
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.shell.*;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class StructrShellCommand implements Command, SignalListener, TerminalHandler {

	private static final Logger logger = LoggerFactory.getLogger(StructrShellCommand.class.getName());
	private static final Map<String, Class<? extends ShellCommand>> commands = new LinkedHashMap<>();

	static {

		commands.put("cat",        CatCommand.class);
		commands.put("cd",         CdCommand.class);
		commands.put("exit",       LogoutCommand.class);
		commands.put("logout",     LogoutCommand.class);
		commands.put("ls",         LsCommand.class);
		commands.put("mkdir",      MkdirCommand.class);
		commands.put("passwd",     PasswordCommand.class);
	}

	private final List<String> commandHistory = new LinkedList<>();
	private Folder currentFolder              = null;
	private TerminalEmulator term             = null;
	private ExitCallback callback             = null;
	private InputStream in                    = null;
	private OutputStream out                  = null;
	private OutputStream err                  = null;
	private User user                         = null;

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
		this.err = err;
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
				if (user != null) {

					// set home directory first
					if (Settings.FilesystemEnabled.getValue()) {

						currentFolder = user.getHomeDirectory();
					}
				}
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

				term.print("Welcome to ");
				term.setBold(true);
				term.print("Structr");
				term.print(" 2.0");
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
	public void destroy(final ChannelSession session) {

		if (term != null) {
			term.stopEmulator();
		}
	}

	@Override
	public void signal(final Channel channel, final Signal signal) {
		logger.info("Received signal {}", signal.name());
	}

	@Override
	public void handleLine(final String line) throws IOException {

		if (StringUtils.isNotBlank(line)) {

			final ShellCommand cmd = getCommandForLine(line);
			if (cmd != null) {

				cmd.setCommand(line);
				cmd.setUser(user);
				cmd.setTerminalEmulator(term);
				cmd.execute(this);
			}

			commandHistory.add(line);
		}
	}

	@Override
	public void handleExit() {

		if (callback != null) {

			callback.onExit(0);
		}
	}

	public String getPrompt() {

		final App app           = StructrApp.getInstance();
		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			buf.append(user.getName());
			buf.append("@structr:");

			if (currentFolder != null) {

				String folderPart = currentFolder.getPath();

				final Folder homeFolder = user.getHomeDirectory();
				if (homeFolder != null) {

					// replace home directory with ~ if at the beginning of the full path
					final String homeFolderPath = homeFolder.getPath();
					if (folderPart.startsWith(homeFolderPath)) {

						folderPart = "~" + folderPart.substring(homeFolderPath.length());
					}
				}

				buf.append(folderPart);

			} else {

				buf.append("/");
			}
			buf.append(user.isAdmin() ? "#" : "$");
			buf.append(" ");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return buf.toString();
	}

	public Folder getCurrentFolder() {
		return currentFolder;
	}

	public void setCurrentFolder(final Folder folder) {
		this.currentFolder = folder;
	}

	public Folder findRelativeFolder(final Folder baseFolder, final String path) throws FrameworkException {

		final App app = StructrApp.getInstance();
		Folder folder = baseFolder;
		boolean found = false;

		for (final String part : path.split("[/]+")) {

			if (folder == null) {

				folder = app.nodeQuery(Folder.class).and(Folder.name, part).getFirst();

			} else {

				for (final Folder child : folder.getFolders()) {

					if (part.equals(child.getName())) {

						folder = child;
						found     = true;
					}
				}

				if (!found) {
					return null;
				}
			}
		}

		return folder;
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
	private ShellCommand getCommandForLine(final String line) {

		final int pos                             = line.indexOf(" ");
		final String commandString                = pos > 0 ? line.substring(0, pos) : line;
		final Class<? extends ShellCommand> clazz = commands.get(commandString);

		if (clazz != null) {

			try {

				return clazz.newInstance();

			} catch (Throwable t) {
				logger.warn("", t);
			}
		}

		return null;
	}

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
	public void handleShiftTab() throws IOException {

	}

	@Override
	public void handleTab(final int tabCount) throws IOException {

		final String line = term.getLineBuffer().toString();
		if (StringUtils.isNotBlank(line)) {

			final ShellCommand cmd = getCommandForLine(line);
			if (cmd != null) {

				cmd.setUser(user);
				cmd.setTerminalEmulator(term);
				cmd.handleTabCompletion(this, line, tabCount);
			}
		}
	}

	public boolean isInteractive() {
		return true;
	}

	public void flush() throws IOException {

		if (term != null) {
			term.flush();
		}
	}
}
