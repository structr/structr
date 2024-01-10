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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.io.IOException;

/**
 *
 *
 */
public class MkdirCommand extends CdCommand {

	private static final Logger logger = LoggerFactory.getLogger(MkdirCommand.class.getName());

	private String target = null;

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		final App app = StructrApp.getInstance();
		final Folder currentFolder = parent.getCurrentFolder();

		try (final Tx tx = app.tx()) {

			if (target != null) {

				switch (target) {

					case "..":
					case ".":
					case "/":
					case "~":
						term.println("Folder " + target + " already exists");
						break;

					default:
						createFolder(parent, currentFolder, target);
						break;
				}

			} else {

				term.println("mkdir needs parameter");
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	@Override
	public void setCommand(final String command) throws IOException {

		if (command.contains(" ") && command.length() > 3) {

			target = command.substring(command.indexOf(" ") + 1);

			if (target.startsWith("\"")) {

				if (target.endsWith("\"")) {

					target = target.substring(1, target.length() - 2);

				} else {

					term.print("Unmatched quotes");
				}
			}

			// remove trailing slash
			if (target != null && target.endsWith("/") && target.length() > 1) {
				target = target.substring(0, target.length() - 1);
			}
		}
	}

	// ----- private methods -----
	private void createFolder(final StructrShellCommand parent, final Folder currentFolder, final String target) throws FrameworkException, IOException {

		final App app = StructrApp.getInstance();
		if (target.contains("/")) {

			final int lastSlashIndex      = target.lastIndexOf("/");
			final String targetFolderPath = target.substring(0, lastSlashIndex);
			final String targetFolderName = target.substring(lastSlashIndex + 1);
			final Folder parentFolder     = parent.findRelativeFolder(currentFolder, targetFolderPath);

			if (parentFolder != null) {

				checkAndCreateFolder(app, parent, parentFolder, targetFolderName);

			} else {

				term.println("Folder " + targetFolderPath + " does not exist");
			}

		} else {

			checkAndCreateFolder(app, parent, currentFolder, target);
		}
	}

	private void checkAndCreateFolder(final App app, final StructrShellCommand parent, final Folder parentFolder, final String name) throws FrameworkException, IOException {

		final Folder checkFolder = app.nodeQuery(Folder.class).and(StructrApp.key(AbstractFile.class, "parent"), parentFolder).and(Folder.name, name).sort(AbstractNode.name).getFirst();
		if (checkFolder != null) {

			term.println("Folder " + target + " already exists");

		} else {

			if (parentFolder != null) {

				if (parent.isAllowed(parentFolder, Permission.write, true)) {

					app.create(Folder.class,
						new NodeAttribute(StructrApp.key(AbstractFile.class, "parent"), parentFolder),
						new NodeAttribute(StructrApp.key(Folder.class, "owner"), user),
						new NodeAttribute(StructrApp.key(Folder.class, "name"), name)
					);

					return;
				}
			}

			term.println("Permission denied");
		}
	}
}
