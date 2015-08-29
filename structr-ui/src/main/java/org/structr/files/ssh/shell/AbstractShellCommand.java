package org.structr.files.ssh.shell;

import org.structr.files.ssh.TerminalEmulator;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractShellCommand implements ShellCommand {

	protected TerminalEmulator term = null;
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
}
