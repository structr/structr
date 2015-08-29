package org.structr.files.ssh.shell;

import java.io.IOException;
import java.util.List;
import org.structr.files.ssh.StructrShellCommand;

/**
 *
 * @author Christian Morgner
 */
public abstract class InteractiveShellCommand extends AbstractShellCommand {

	@Override
	public void handleExit() {
	}

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {
		term.setTerminalHandler(this);
	}

	@Override
	public List<String> getCommandHistory() {
		return null;
	}

	@Override
	public void handleLogoutRequest() throws IOException {
	}

	@Override
	public void handleCtrlC() throws IOException {

		term.print("^C");
		term.clearLineBuffer();
		term.restoreRootTerminalHandler();
		term.handleNewline();
	}
}
