/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.storage.util.VirtualFileChannel;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class StructrFilesystemProvider extends FileSystemProvider {

	private static final Logger logger = LoggerFactory.getLogger(StructrFilesystemProvider.class.getName());

	@Override
	public synchronized String getScheme() {
		logger.warn("NOT SUPPORTED: getScheme");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
		logger.warn("NOT SUPPORTED: newFileSystem {}, {}", uri, env );
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized FileSystem getFileSystem(final URI uri) {
		logger.warn("NOT SUPPORTED: getFileSystem {}", new Object[] { uri } );
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized Path getPath(URI uri) {
		logger.warn("NOT SUPPORTED: getPath {}", uri );
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized FileChannel newFileChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		return new VirtualFileChannel(newByteChannel(path, options, attrs));
	}

	@Override
	public synchronized SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		return checkPath(path).newChannel(options, attrs);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
		return checkPath(dir).getDirectoryStream(filter);
	}

	@Override
	public synchronized void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
		checkPath(dir).createDirectory(attrs);
	}

	@Override
	public synchronized void delete(final Path path) throws IOException {
		checkPath(path).delete();
	}

	@Override
	public synchronized void copy(Path source, Path target, CopyOption... options) throws IOException {
		checkPath(source).copy(target, options);
	}

	@Override
	public synchronized void move(Path source, Path target, CopyOption... options) throws IOException {
		checkPath(source).move(target, options);
	}

	@Override
	public synchronized boolean isSameFile(Path path, Path path2) throws IOException {
		return checkPath(path).isSameFile(path2);
	}

	@Override
	public synchronized boolean isHidden(Path path) throws IOException {
		logger.warn("NOT SUPPORTED: isHidden {}", path );
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized FileStore getFileStore(Path path) throws IOException {
		logger.warn("NOT SUPPORTED: getFileStore {}", path );
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public synchronized void checkAccess(final Path path, final AccessMode... modes) throws IOException {
		checkPath(path).checkAccess(modes);
	}

	@Override
	public synchronized <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {

		try {

			return checkPath(path).getFileAttributeView(type, options);

		} catch (IOException ignore) {
		}

		return null;
	}

	@Override
	public synchronized <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type, final LinkOption... options) throws IOException {
		return checkPath(path).getAttributes(type, options);
	}

	@Override
	public synchronized Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		return checkPath(path).getAttributes(attributes, options);
	}

	@Override
	public synchronized void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		checkPath(path).setAttribute(attribute, value, options);
	}

	// ----- protected methods -----
	private StructrPath checkPath(final Path obj) {

		if (obj == null) {
			throw new NullPointerException();
		}

		if (!(obj instanceof StructrPath)) {
			throw new ProviderMismatchException();
		}

		return (StructrPath)obj;
	}
}
