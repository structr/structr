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
package org.structr.files.ssh.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.files.ssh.filesystem.path.file.StructrFilePath;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class StructrPath implements Path {

	private static final Logger logger                 = LoggerFactory.getLogger(StructrPath.class.getName());
	public static final Map<String, HiddenFileEntry> HIDDEN_PROPERTY_FILES = new ConcurrentHashMap<>();

	public static final String ROOT_DIRECTORY      = "/";
	public static final String CURRENT_DIRECTORY   = ".";
	public static final String SCHEMA_DIRECTORY    = "schema";
	public static final String GRAPH_DIRECTORY     = "graph";
	public static final String FILES_DIRECTORY     = "files";

	protected StructrFilesystem fs = null;
	protected StructrPath parent   = null;
	protected String name          = null;

	public StructrPath(final StructrFilesystem fs) {
		this(fs, null, null);
	}

	public StructrPath(final StructrFilesystem fs, final StructrPath parent, final String name) {

		this.parent   = parent;
		this.name     = name;
		this.fs       = fs;
	}

	// ----- public abstract methods -----
	public abstract DirectoryStream<Path> getDirectoryStream(final DirectoryStream.Filter<? super Path> filter);
	public abstract SeekableByteChannel newChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException;
	public abstract <T extends BasicFileAttributes> T getAttributes(final Class<T> type, final LinkOption... options) throws IOException;
	public abstract <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException;
	public abstract Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException;
	public abstract void createDirectory(final FileAttribute<?>... attrs) throws IOException;
	public abstract void delete() throws IOException;
	public abstract void copy(final Path target, final CopyOption... options) throws IOException;
	public abstract void move(final Path target, final CopyOption... options) throws IOException;
	public abstract void setAttribute(final String attribute, final Object value, LinkOption... options) throws IOException;
	public abstract boolean isSameFile(final Path path2) throws IOException;
	public abstract StructrPath resolveStructrPath(final String pathComponent) throws FrameworkException;

	// ----- public methods -----
	public void checkAccess(final AccessMode... modes) {

		//logger.info("{}: {}", new Object[] { toString(), Arrays.asList(modes) });
	}

	public void enablePropertyFile(final String name) {
	}

	public boolean hasPropertyFile(final String name) {
		return true;
	}

	public boolean dontCache() {
		return false;
	}

	// ----- interface Path -----
	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		if (parent != null) {

			final String parentPath = parent.toString();
			if (!"/".equals(parentPath)) {

				buf.append(parentPath);
			}

			buf.append("/");
		}

		if (name != null) {
			buf.append(name);
		}

		return buf.toString();
	}

	@Override
	public FileSystem getFileSystem() {
		return fs;
	}

	@Override
	public boolean isAbsolute() {

		// ask parent
		if (parent != null) {
			return parent.isAbsolute();
		}

		return name == null;
	}

	@Override
	public Path getRoot() {

		if (isAbsolute()) {

			if (parent != null) {

				return parent.getRoot();
			}
		}

		return null;
	}

	@Override
	public Path getParent() {
		return parent;
	}

	@Override
	public Path getFileName() {

		if (name != null) {
			return new StructrFilePath(fs, null, name);
		}

		return null;
	}

	@Override
	public int getNameCount() {

		if (parent != null) {
			return parent.getNameCount() + 1;
		}

		if (name != null) {
			return 1;
		}

		return 0;
	}

	@Override
	public Path getName(int index) {

		final List<Path> paths = new ArrayList<>();
		Path path              = this;

		paths.add(this);

		// find root
		while (path.getParent() != null) {
			path = path.getParent();
			paths.add(0, path);
		}

		return paths.get(index);
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		logger.info("{}, {}", new Object[] { beginIndex, endIndex });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean startsWith(Path other) {
		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean startsWith(String other) {
		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean endsWith(Path other) {
		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean endsWith(String other) {
		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Path normalize() {
		return this;
	}

	@Override
	public Path resolve(Path other) {

		if (other.isAbsolute()) {
			return other;
		}

		logger.info("{}", new Object[] { other });

		return null;
	}

	@Override
	public Path resolve(final String other) {

		if (other.startsWith(ROOT_DIRECTORY)) {
			return fs.getPath(other);
		}

		if (CURRENT_DIRECTORY.equals(other)) {
			return this;
		}

		logger.info("{}", new Object[] { other });

		// fallback
		return fs.getPath(toString(), other);
	}

	@Override
	public Path resolveSibling(Path other) {

		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Path resolveSibling(String other) {

		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Path relativize(Path other) {

		logger.info("{}, {}", new Object[] { other });
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public URI toUri() {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Path toAbsolutePath() {

		if (isAbsolute()) {
			return this;
		}

		return fs.getPath(toString(), this.name);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public File toFile() {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterator<Path> iterator() {

		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public int compareTo(Path other) {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- protected methods -----
	protected String normalizeFileNameForJavaIdentifier(final String src) {

		String dst = src;

		dst = dst.replace('/', '_');
		dst = dst.replace('.', '_');
		dst = dst.replace('-', '_');
		dst = dst.replace('+', '_');
		dst = dst.replace('~', '_');
		dst = dst.replace('#', '_');
		dst = dst.replace('\'', '_');
		dst = dst.replace('\"', '_');
		dst = dst.replace('`', '_');
		dst = dst.replace('(', '_');
		dst = dst.replace(')', '_');
		dst = dst.replace('[', '_');
		dst = dst.replace(']', '_');
		dst = dst.replace('{', '_');
		dst = dst.replace('}', '_');
		dst = dst.replace('!', '_');
		dst = dst.replace('$', '_');
		dst = dst.replace('§', '_');
		dst = dst.replace('%', '_');
		dst = dst.replace('&', '_');
		dst = dst.replace('=', '_');
		dst = dst.replace(':', '_');
		dst = dst.replace('<', '_');
		dst = dst.replace('>', '_');
		dst = dst.replace('|', '_');
		dst = dst.replace('^', '_');
		dst = dst.replace('°', '_');

		return dst;
	}

	// ----- nested classes -----
	public static class HiddenFileEntry {

		private final Set<String> dynamicNames = new LinkedHashSet<>();
		private final Set<String> names        = new LinkedHashSet<>();

		public void add(final String name) {
			names.add(name);
		}

		public boolean has(final String name) {
			return names.contains(name);
		}

		public void remove(final String name) {
			names.remove(name);
		}

		public boolean isEmpty() {
			return names.isEmpty();
		}

		public void addDynamicWithValue(final String name) {
			dynamicNames.add(name);
		}

		public boolean hasDynamicWithValue(final String name) {
			return dynamicNames.contains(name);
		}

		public void removeDynamicWithValue(final String name) {
			dynamicNames.remove(name);
		}
	}
}
