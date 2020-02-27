/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.web.common;

import java.io.FileOutputStream;
import java.io.IOException;
import org.structr.web.entity.File;

/**
 */
public class ClosingFileOutputStream extends FileOutputStream {

	private boolean closed = false;
	private File thisFile  = null;

	public ClosingFileOutputStream(final File thisFile, final boolean append, final boolean notifyIndexerAfterClosing) throws IOException {

		super(thisFile.getFileOnDisk(), append);

		this.thisFile = thisFile;
	}

	@Override
	public void close() throws IOException {

		if (closed) {
			return;
		}

		super.close();

		thisFile.notifyUploadCompletion();

		closed = true;
	}
}
