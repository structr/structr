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
import java.io.InputStream;
import java.util.List;

/**
 * A deployment export, addressed by relative path.
 *
 * The whole point of the interface is that the parser never learns where an export is stored.
 * A zip held as a File node in the VFS, a folder, a temporary directory written by
 * DeployCommand: all three answer the same two questions, and the parser is written once.
 *
 * <p>Paths are always relative to the export root, always forward-slashed, and always in
 * Unicode NFC. The normalisation is not cosmetic: two host filesystems disagree about how to
 * spell a name containing an umlaut, and an implementation reading directory entries has to
 * settle that before the parser sees it, or one export appears to contain a file the other
 * does not. A VFS-backed implementation gets this for free, because the path is a property in
 * the database rather than a directory entry.
 */
public interface ExportSource {

	/** Every file in the export, relative to its root, NFC-normalised and sorted. */
	List<String> paths() throws IOException;

	/** The bytes at a path. Callers close the stream. */
	InputStream open(final String path) throws IOException;

	boolean exists(final String path) throws IOException;

	/** A human-readable name for this export, used in messages rather than in matching. */
	String getName();
}
