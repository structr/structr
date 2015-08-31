package org.structr.files.ssh.shell;

import java.io.IOException;
import java.util.List;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class LsCommand extends NonInteractiveShellCommand {

	// http://invisible-island.net/xterm/ctlseqs/ctlseqs.html

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final Folder currentFolder = parent.getCurrentFolder();
			if (currentFolder != null) {

				listFolder(parent, currentFolder.getProperty(AbstractFile.children));

			} else {

				listFolder(parent, app.nodeQuery(AbstractFile.class).and(AbstractFile.parent, null).getAsList());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	// ----- private methods -----
	private void listFolder(final StructrShellCommand parent, final List<AbstractFile> folder) throws FrameworkException, IOException {

		for (final AbstractFile child : folder) {

			if (parent.isAllowed(child, Permission.read, false)) {

				if (child instanceof Folder) {

					term.setBold(true);
					term.setTextColor(4);
					term.print(child.getName() + "  ");
					term.setTextColor(7);
					term.setBold(false);

				} else {

					term.print(child.getName() + "  ");
				}
			}
		}

		if (!folder.isEmpty()) {
			term.println();
		}
	}
}
