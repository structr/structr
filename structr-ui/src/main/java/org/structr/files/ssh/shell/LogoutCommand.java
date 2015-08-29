package org.structr.files.ssh.shell;

import java.io.IOException;
import org.structr.files.ssh.StructrShellCommand;

/**
 *
 * @author Christian Morgner
 */
public class LogoutCommand extends NonInteractiveShellCommand {

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {
		term.stopEmulator();
	}
}
