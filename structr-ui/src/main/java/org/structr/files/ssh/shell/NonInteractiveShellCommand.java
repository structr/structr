package org.structr.files.ssh.shell;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Christian Morgner
 */
public abstract class NonInteractiveShellCommand extends AbstractShellCommand {

	@Override
	public void handleExit() {
	}

	@Override
	public List<String> getCommandHistory() {
		return null;
	}

	@Override
	public void handleLine(final String line) throws IOException {
	}

	@Override
	public void displayPrompt() throws IOException {
	}

	@Override
	public void handleLogoutRequest() throws IOException {
	}

	@Override
	public void handleCtrlC() throws IOException {
	}
}
