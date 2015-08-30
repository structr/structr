package org.structr.files.ssh.shell;

import java.io.IOException;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.files.ssh.TerminalEmulator;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractShellCommand implements ShellCommand {

	protected TerminalEmulator term = null;
	protected String command        = null;
	protected User user             = null;

	@Override
	public void setTerminalEmulator(final TerminalEmulator term) {
		this.term = term;
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
	public void setCommand(final String command) throws IOException {
		this.command = command;
	}

	@Override
	public void handleTab(final int tabCount) throws IOException {
	}

	@Override
	public void handleTabCompletion(final StructrShellCommand parent, final String line, final int tabCount) throws IOException {
	}
}
