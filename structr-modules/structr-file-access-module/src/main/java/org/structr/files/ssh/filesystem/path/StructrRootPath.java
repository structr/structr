/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.ssh.filesystem.path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.files.ssh.filesystem.AbstractDirectoryStream;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrRootAttributes;
import org.structr.files.ssh.filesystem.path.file.StructrFilesPath;
import org.structr.files.ssh.filesystem.path.schema.StructrSchemaPath;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class StructrRootPath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrRootPath.class.getName());

	private final StructrRootAttributes rootAttributes = new StructrRootAttributes(StructrPath.ROOT_DIRECTORY);
	private final Map<String, StructrPath> rootPaths   = new LinkedHashMap<>();

	public StructrRootPath(final StructrFilesystem fs) {

		super(fs);

		this.rootPaths.put(StructrPath.CURRENT_DIRECTORY,    this);
		this.rootPaths.put(StructrPath.ROOT_DIRECTORY,       this);
		this.rootPaths.put(StructrPath.FILES_DIRECTORY,      new StructrFilesPath(fs, this));
		this.rootPaths.put(StructrPath.SCHEMA_DIRECTORY,     new StructrSchemaPath(fs, this));
	}

	@Override
	public String toString() {
		return StructrPath.ROOT_DIRECTORY;
	}

	@Override
	public SeekableByteChannel newChannel(Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(final DirectoryStream.Filter<? super Path> filter) {
		return new RootDirectoryStream(filter);
	}

	@Override
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {
		throw new FileAlreadyExistsException(this.toString());
	}

	@Override
	public void delete() throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {

		// this method is responsible for creating possible path entries,
		// so we return only those that we want to exist, and don't allow
		// the creation of anything else, effectively preventing the
		// creation of files or directories in the Structr root dir.
		return rootPaths.get(pathComponent);
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) {
		return rootAttributes.toMap(attributes);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) {
		return (T)rootAttributes;
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException {
		return (V)getAttributes((Class)null, options);
	}

	@Override
	public void copy(final Path target, final CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(final Path target, final CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setAttribute(final String attribute, final Object value, final LinkOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isSameFile(final Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private class RootDirectoryStream extends AbstractDirectoryStream {

		public RootDirectoryStream(final DirectoryStream.Filter<? super Path> filter) {

			// make all root paths available to the system
			for (final StructrPath p : rootPaths.values()) {

				// we need to make sure here that only those path entries are put
				// into the directory stream that are not the root itself
				if (p.equals(StructrRootPath.this))  {
					continue;
				}

				try {

					if (filter == null || filter.accept(p)) {
						this.paths.add(p);
					}

				} catch (IOException ioex) {
					logger.warn("Unable to create root directory stream", ioex);
				}
			}
		}
	}
}
