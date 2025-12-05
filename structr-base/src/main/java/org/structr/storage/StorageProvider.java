/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.web.entity.StorageConfiguration;

import javax.activation.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

public interface StorageProvider extends DataSource {

	AbstractFile getAbstractFile();

	StorageConfiguration getConfig();

	String getProviderName();

	// overridden method removes exception
	@Override
	InputStream getInputStream();

	// overridden method removes exception
	@Override
	OutputStream getOutputStream();

	OutputStream getOutputStream(final boolean append);

	default SeekableByteChannel getSeekableByteChannel() {
		return getSeekableByteChannel(new java.util.HashSet<OpenOption>(Set.of(CREATE, READ, WRITE, SYNC)));
	}

	SeekableByteChannel getSeekableByteChannel(final Set<? extends OpenOption> options);

	void moveTo(final StorageProvider newFileStorageProvider);

	void delete();

	long size();
}
