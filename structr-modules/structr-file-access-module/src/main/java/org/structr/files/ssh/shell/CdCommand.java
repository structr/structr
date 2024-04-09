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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class CdCommand extends NonInteractiveShellCommand {

	private static final Logger logger = LoggerFactory.getLogger(CdCommand.class.getName());

	private String target = null;

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		final App app = StructrApp.getInstance();
		final Folder currentFolder = parent.getCurrentFolder();

		try (final Tx tx = app.tx()) {

			if (target != null) {

				switch (target) {

					case "..":
						if (currentFolder != null) {

							final Folder parentFolder = currentFolder.getParent();
							if (parentFolder != null) {

								if (parent.isAllowed(parentFolder, Permission.read, true)) {
									parent.setCurrentFolder(parentFolder);
								}

							} else {

								parent.setCurrentFolder(null);
							}

						}
						break;

					case ".":
						break;

					case "/":
						parent.setCurrentFolder(null);
						break;

					case "~":
						parent.setCurrentFolder(user.getHomeDirectory());
						break;

					default:
						setFolder(parent, currentFolder, target);
						break;
				}

			} else {

				parent.setCurrentFolder(user.getHomeDirectory());
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

	@Override
	public void handleTabCompletion(final StructrShellCommand parent, final String line, final int tabCount) throws IOException {

		if (line.contains(" ") && line.length() >= 3) {

			final PropertyKey<AbstractFile> parentKey = StructrApp.key(AbstractFile.class, "parent");
			String incompletePath                     = line.substring(line.indexOf(" ") + 1);
			Folder baseFolder                         = null;
			String lastPathPart                       = null;

			if (incompletePath.startsWith("\"")) {

				incompletePath = incompletePath.substring(1);
			}

			final App app = StructrApp.getInstance();

			if ("..".equals(incompletePath)) {

				term.handleCharacter('/');
				return;
			}

			if (incompletePath.startsWith("/")) {

				incompletePath = incompletePath.substring(1);

			} else {

				baseFolder = parent.getCurrentFolder();
			}

			// identify full path parts and find folders
			final String[] parts = incompletePath.split("[/]+");
			final int partCount  = parts.length;

			try (final Tx tx = app.tx()) {

				// only a single path part
				if (partCount == 1) {

					lastPathPart = parts[0];

				} else {

					lastPathPart = parts[partCount-1];

					// more than a single path part, find preceding folders
					for (int i=0; i<partCount-1; i++) {

						// skip empty path parts
						if (StringUtils.isNotBlank(parts[i])) {

							baseFolder = app.nodeQuery(Folder.class).and(parentKey, baseFolder).and(Folder.name, parts[i]).sort(AbstractNode.name).getFirst();
							if (baseFolder == null) {

								return;
							}
						}
					}
				}

				final List<Folder> allFolders = app.nodeQuery(Folder.class).and(parentKey, baseFolder).sort(AbstractNode.name).getAsList();
				final List<Folder> folders    = new LinkedList<>();

				for (final Folder folder : allFolders) {

					if (folder.getName().startsWith(lastPathPart)) {

						folders.add(folder);
					}
				}

				if (folders.size() > 1) {

					// only display autocomplete suggestions after second tab
					if (tabCount > 1) {

						displayAutocompleteSuggestions(parent, folders, line);
					}

				} else if (!folders.isEmpty()) {

					final Folder folder = folders.get(0);

					if (parent.isAllowed(folder, Permission.read, false)) {

						if (lastPathPart.equals(folder.getName())) {

							// only display autocomplete suggestions after second tab
							if (tabCount > 1) {

								displayAutocompleteSuggestions(parent, folder.getFolders(), line);

							} else {

								if (!line.endsWith("/")) {
									term.handleCharacter('/');
								}
							}

						} else {

							displayAutocompleteFolder(folder, lastPathPart);
						}

					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}


		}
	}

	// ----- private methods -----
	private void setFolder(final StructrShellCommand parent, final Folder currentFolder, final String targetFolderName) throws IOException, FrameworkException {

		final App app = StructrApp.getInstance();
		String target = targetFolderName;

		// remove trailing slash
		if (target.endsWith("/")) {
			target = target.substring(0, target.length() - 1);
		}

		if (target.startsWith("/")) {

			final Folder folder = app.nodeQuery(Folder.class).and(StructrApp.key(Folder.class, "path"), target).sort(AbstractNode.name).getFirst();
			if (folder != null) {

				if (parent.isAllowed(folder, Permission.read, true)) {

					parent.setCurrentFolder(folder);

				} else {

				term.println("Permission denied");
				}

			} else {

				term.println("Folder " + target + " does not exist");
			}

		} else {

			final Folder newFolder = parent.findRelativeFolder(currentFolder, target);
			if (newFolder == null) {

				term.println("Folder " + target + " does not exist");

			} else {

				if (parent.isAllowed(newFolder, Permission.read, true)) {

					parent.setCurrentFolder(newFolder);

				} else {

					term.println("Permission denied");
				}
			}
		}
	}

	private void displayAutocompleteFolder(final Folder folder, final String part) throws IOException {

		final String name = folder.getName();
		if (name.startsWith(part)) {

			final String remainder = folder.getName().substring(part.length());
			if (StringUtils.isNotEmpty(remainder)) {

				term.handleString(remainder);
				term.handleCharacter('/');
			}
		}
	}

	private void displayAutocompleteSuggestions(final StructrShellCommand parent, final Iterable<Folder> folders, final String line) throws IOException {

		final StringBuilder buf = new StringBuilder();

		for (final Folder folder : folders) {

			if (parent.isAllowed(folder, Permission.read, false)) {

				buf.append(folder.getName()).append("/  ");
			}
		}

		if (buf.length() > 0) {

			term.println();
			term.print(buf.toString());
			term.println();

			parent.displayPrompt();
			term.print(line);
		}
	}
}
