/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.ssh.shell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.io.IOException;

/**
 *
 * See also http://invisible-island.net/xterm/ctlseqs/ctlseqs.html
 */
public class LsCommand extends NonInteractiveShellCommand {

	private static final Logger logger = LoggerFactory.getLogger(LsCommand.class.getName());

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final Folder currentFolder = parent.getCurrentFolder();
			if (currentFolder != null) {

				listFolder(parent, currentFolder.getChildren());

			} else {

				listFolder(parent, app.nodeQuery(AbstractFile.class).and(StructrApp.key(AbstractFile.class, "parent"), null).sort(AbstractNode.name).getAsList());
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	// ----- private methods -----
	private void listFolder(final StructrShellCommand parent, final Iterable<AbstractFile> folder) throws FrameworkException, IOException {

		boolean hasContents = false;

		for (final AbstractFile child : folder) {

			if (parent.isAllowed(child, Permission.read, false)) {

				hasContents = true;

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

		if (hasContents) {
			term.println();
		}
	}
}
