package org.structr.files.ssh.shell;

import java.io.IOException;
import org.structr.files.ssh.StructrShellCommand;

/**
 *
 * @author Christian Morgner
 */
public class LsCommand extends NonInteractiveShellCommand {

	// http://invisible-island.net/xterm/ctlseqs/ctlseqs.html

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		term.setBold(true);
		term.setTextColor(4);
		term.print("bin boot cdrom data dev etc home ");
		term.setTextColor(6);
		term.print("initrd.img");
		term.setTextColor(4);
		term.print(" lib lib32");
		term.setTextColor(7);
		term.setBold(false);
	}
}
