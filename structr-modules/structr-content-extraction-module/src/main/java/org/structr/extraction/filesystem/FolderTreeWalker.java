/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.filesystem;

import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Walks folder trees.
 */
public final class FolderTreeWalker {

	private FolderTreeWalker() {
	}

	/**
	 * The files in a folder, and with {@code recursive} in all its subfolders, breadth first.
	 */
	public static List<File> files(final Folder root, final boolean recursive) {

		final List<File> files      = new ArrayList<>();
		final Deque<Folder> pending = new ArrayDeque<>();

		pending.add(root);

		while (!pending.isEmpty()) {

			final Folder folder = pending.removeFirst();

			for (final File file : folder.getFiles()) {

				files.add(file);
			}

			if (recursive) {

				for (final Folder child : folder.getFolders()) {

					pending.add(child);
				}
			}
		}

		return files;
	}
}
