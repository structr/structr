/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.files.ssh.filesystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public abstract class AbstractDirectoryStream implements DirectoryStream<Path>, Iterator<Path> {

	protected final List<Path> paths = new ArrayList<>();
	private int index                = 0;

	@Override
	public Iterator<Path> iterator() {
		return this;
	}

	@Override
	public void close() throws IOException {
	}

	// ----- interface Iterator<Path> -----
	@Override
	public boolean hasNext() {
		return index < paths.size();
	}

	@Override
	public Path next() {
		return paths.get(index++);
	}
}
