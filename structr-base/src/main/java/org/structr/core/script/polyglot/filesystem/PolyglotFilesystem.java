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
package org.structr.core.script.polyglot.filesystem;

import org.graalvm.polyglot.io.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

public class PolyglotFilesystem implements FileSystem {
	private static final Logger logger = LoggerFactory.getLogger(PolyglotFilesystem.class);

	@Override
	public Path parsePath(URI uri) {
		logger.info("parsePath(uri) : {}", uri);

		if (uri != null) {

			return parsePath(uri.getPath());
		} else {

			return null;
		}
	}

	@Override
	public Path parsePath(String path) {

		if (path != null) {

			return Path.of(path);
		}
		return null;
	}

	@Override
	public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		Files.createDirectory(dir, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		Files.delete(path);
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			PropertyKey pathKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(AbstractFile.class, "path");
			File file = (File)app.nodeQuery(File.class).and(pathKey, path.toString()).getFirst();

			if (file != null) {

				tx.success();
				return StorageProviderFactory.getStorageProvider(file).getSeekableByteChannel();
			}

			tx.success();
		} catch (FrameworkException ex) {

			logger.error("Unexpected exception while trying to parse virtual filesystem path", ex);
		}

		return Files.newByteChannel(path, options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
		return Files.newDirectoryStream(dir, filter);
	}

	@Override
	public Path toAbsolutePath(Path path) {
		return path;
	}

	@Override
	public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
		return path;
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		return Files.readAttributes(path, attributes, options);
	}
}
