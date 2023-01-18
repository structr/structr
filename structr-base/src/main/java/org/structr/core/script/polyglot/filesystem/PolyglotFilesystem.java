/*
 * Copyright (C) 2010-2022 Structr GmbH
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
package org.structr.core.script.polyglot.filesystem;

import org.graalvm.polyglot.io.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

public class PolyglotFilesystem implements FileSystem {
	private static Logger logger = LoggerFactory.getLogger(PolyglotFilesystem.class);

	@Override
	public Path parsePath(URI uri) {
		logger.info("parsePath : {}", uri);
		return null;
	}

	@Override
	public Path parsePath(String path) {
		logger.info("parsePath : {}", path);
		return null;
	}

	@Override
	public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
		logger.info("checkAccess : {} , {}", path, linkOptions);
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		logger.info("createDirectory : {}", attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		logger.info("delete : {}", path);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		logger.info("newByteChannel : {} , {}", path, attrs);
		return null;
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
		logger.info("newDirectoryStream : {} , {}", dir, filter);
		return null;
	}

	@Override
	public Path toAbsolutePath(Path path) {
		logger.info("toAbsolutePath : {}", path);
		return null;
	}

	@Override
	public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
		logger.info("toRealPath : {} , {}", path, linkOptions);
		return null;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		logger.info("readAttributes : {} , {} , {}", path, attributes, options);
		return null;
	}
}
