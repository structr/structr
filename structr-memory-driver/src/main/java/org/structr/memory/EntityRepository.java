/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.memory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 */
public abstract class EntityRepository {

	protected static final int STORAGE_FORMAT_VERSION = 1;

	protected ZipOutputStream getZipOutputStream(final File dbFile) throws IOException {

		final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dbFile, false)));

		zos.putNextEntry(new ZipEntry("data"));

		return zos;
	}

	protected ZipInputStream getZipInputStream(final File dbFile) throws IOException {

		final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(dbFile)));

		// there should be only one entry named "data"..
		zis.getNextEntry();

		return zis;
	}
}
