package org.structr.files.ssh.shell;

import java.io.IOException;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.files.ssh.TerminalEmulator;
import org.structr.files.ssh.TerminalHandler;

/**
 *
 * @author Christian Morgner
 */
public interface ShellCommand extends TerminalHandler {

	public void execute(final StructrShellCommand parent) throws IOException;
	public void setTerminalEmulator(final TerminalEmulator term);
	public void setCommand(final String command) throws IOException;
	public void handleTabCompletion(final StructrShellCommand parent, final String line, final int tabCount) throws IOException;
}
