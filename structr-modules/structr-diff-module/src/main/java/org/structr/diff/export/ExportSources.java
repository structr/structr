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
package org.structr.diff.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens an export without the caller having to know how it is stored.
 *
 * A directory and a zip are the same export in two shapes, and every caller that resolves one from
 * user input, a REST parameter or a VFS path faces the same choice. Deciding it here keeps that
 * out of each of them, and keeps the rule in one place when a third shape appears.
 */
public final class ExportSources {

	public static ExportSource open(final Path path, final String name) throws IOException {

		if (Files.isDirectory(path)) {

			return new PathExportSource(path, name);
		}

		if (Files.isRegularFile(path)) {

			return ZipExportSource.open(path, name);
		}

		throw new IOException("Not an export: " + path + " is neither a directory nor a file.");
	}

	private ExportSources() {}
}
