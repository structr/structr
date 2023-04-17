/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.storage;

import org.structr.web.entity.AbstractFile;

import javax.activation.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

public interface StorageProvider extends DataSource {
	AbstractFile getAbstractFile();
	InputStream getInputStream();
	OutputStream getOutputStream();
	OutputStream getOutputStream(final boolean append);
	default SeekableByteChannel getSeekableByteChannel() {
		return getSeekableByteChannel(false, false);
	}
	default SeekableByteChannel getSeekableByteChannel(boolean append) {
		return getSeekableByteChannel(append, false);
	}
	SeekableByteChannel getSeekableByteChannel(boolean append, boolean truncate);
	void moveTo(final StorageProvider newFileStorageProvider);
	void delete();
	long size();
}
