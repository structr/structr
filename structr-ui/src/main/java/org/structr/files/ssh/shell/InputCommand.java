package org.structr.files.ssh.shell;

import java.io.IOException;

/**
 *
 * @author Christian Morgner
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
