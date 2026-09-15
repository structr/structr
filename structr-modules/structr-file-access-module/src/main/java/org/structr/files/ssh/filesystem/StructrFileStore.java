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
package org.structr.files.ssh.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * The single store behind the virtual filesystem.
 *
 * There is one, and its capacity is not a number this filesystem knows: the bytes live in whichever
 * storage provider a file is configured for, which may be a local disk, S3, or the database itself. The
 * space methods therefore report "unknown" rather than inventing a figure a caller might act on.
 */
public class StructrFileStore extends FileStore {

	private final StructrFilesystem fs;

	public StructrFileStore(final StructrFilesystem fs) {

		this.fs = fs;
	}

	@Override
	public String name() {

		return StructrPath.SCHEME;
	}

	@Override
	public String type() {

		return StructrPath.SCHEME;
	}

	@Override
	public boolean isReadOnly() {

		return fs.isReadOnly();
	}

	/**
	 * Long.MAX_VALUE is what the platform reports for a store whose size it cannot determine, and it is
	 * the honest answer here: the real limit belongs to the storage provider, and differs per file.
	 */
	@Override
	public long getTotalSpace() throws IOException {

		return Long.MAX_VALUE;
	}

	@Override
	public long getUsableSpace() throws IOException {

		return Long.MAX_VALUE;
	}

	@Override
	public long getUnallocatedSpace() throws IOException {

		return Long.MAX_VALUE;
	}

	@Override
	public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {

		return supportsFileAttributeView(type.getSimpleName().replace("FileAttributeView", "").toLowerCase());
	}

	@Override
	public boolean supportsFileAttributeView(final String name) {

		return StructrFileAttributes.SUPPORTED_VIEWS.contains(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(final Class<V> type) {

		// the contract is null for an unsupported view, and this store has none at all

		return null;
	}

	@Override
	public Object getAttribute(final String attribute) throws IOException {

		throw new UnsupportedOperationException("Unknown file store attribute '" + attribute + "'.");
	}
}
