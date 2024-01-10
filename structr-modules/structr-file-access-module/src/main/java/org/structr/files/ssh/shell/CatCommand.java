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
import org.structr.api.util.Iterables;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class CatCommand extends NonInteractiveShellCommand {

	private static final Logger logger = LoggerFactory.getLogger(CatCommand.class.getName());

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

	@Override
	public void handleTabCompletion(final StructrShellCommand parent, final String line, final int tabCount) throws IOException {

		if (line.contains(" ") && line.length() >= 3) {

			String incompletePath = line.substring(line.indexOf(" ") + 1);
			Folder baseFolder     = null;
			String lastPathPart   = null;

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

							baseFolder = app.nodeQuery(Folder.class).and(StructrApp.key(AbstractFile.class, "parent"), baseFolder).and(Folder.name, parts[i]).sort(AbstractNode.name).getFirst();
							if (baseFolder == null) {

								return;
							}
						}
					}
				}

				final List<AbstractFile> allFiles = new LinkedList<>();
				final List<AbstractFile> files    = new LinkedList<>();

				if (baseFolder != null) {

					allFiles.addAll(Iterables.toList(baseFolder.getChildren()));

				} else {

					allFiles.addAll(app.nodeQuery(AbstractFile.class).and(StructrApp.key(File.class, "parent"), null).sort(AbstractNode.name).getAsList());
				}

				for (final AbstractFile file : allFiles) {

					if (file.getName().startsWith(lastPathPart)) {

						files.add(file);
					}
				}

				if (files.size() > 1) {

					// only display autocomplete suggestions after second tab
					if (tabCount > 1) {

						displayAutocompleteSuggestions(parent, files, line);
					}

				} else if (!files.isEmpty()) {

					final AbstractFile file = files.get(0);
					if (file instanceof Folder) {

						final Folder folder = (Folder)file;
						if (parent.isAllowed(folder, Permission.read, false)) {

							if (lastPathPart.equals(folder.getName())) {

								// only display autocomplete suggestions after second tab
								if (tabCount > 1) {

									displayAutocompleteSuggestions(parent, folder.getChildren(), line);

								} else {

									if (!line.endsWith("/")) {
										term.handleCharacter('/');
									}
								}

							} else {

								displayAutocompleteItem(folder, lastPathPart);
							}

						}

					} else {

						final AbstractFile existingFile = files.get(0);

						if (parent.isAllowed(existingFile, Permission.read, false)) {

							if (!lastPathPart.equals(existingFile.getName())) {

								displayAutocompleteItem(existingFile, lastPathPart);
							}

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

	private void displayAutocompleteItem(final AbstractFile file, final String part) throws IOException {

		final String name = file.getName();
		if (name.startsWith(part)) {

			final String remainder = file.getName().substring(part.length());
			if (StringUtils.isNotEmpty(remainder)) {

				term.handleString(remainder);

				if (file instanceof Folder) {

					term.handleCharacter('/');
				}
			}
		}
	}

	private void displayAutocompleteSuggestions(final StructrShellCommand parent, final Iterable<AbstractFile> files, final String line) throws IOException {

		final StringBuilder buf = new StringBuilder();

		for (final AbstractFile file : files) {

			if (parent.isAllowed(file, Permission.read, false)) {

				buf.append(file.getName());

				if (file instanceof Folder) {

					buf.append("/  ");
				}
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
