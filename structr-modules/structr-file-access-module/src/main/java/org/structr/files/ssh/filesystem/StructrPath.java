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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.files.ssh.filesystem.path.file.StructrFilePath;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.net.URISyntaxException;
import java.nio.file.*;
import org.structr.files.ssh.filesystem.path.file.StructrFilePath;
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
	public static final String PARENT_DIRECTORY    = "..";
	public static final String SCHEME              = "structr";

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
	/**
	 * Whether the file behind this path can be accessed the given ways.
	 *
	 * Files.exists() is this method with the exception caught, so a subclass that never throws makes
	 * every path on this filesystem report as existing. The default stays permissive for paths that are
	 * not backed by a node, such as the root, and StructrFilePath decides for real files.
	 */
	public void checkAccess(final AccessMode... modes) throws IOException {
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

		// a relative path has no root, an absolute one has the filesystem's. Walking to the parent and
		// returning null there, as this did, means every path reports no root at all.
		if (isAbsolute()) {

			return fs.getRootDirectories().iterator().next();
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
	public Path getName(final int index) {

		final List<String> elements = getNameElements();
		if (index < 0 || index >= elements.size()) {

			throw new IllegalArgumentException("No name element at index " + index + " in " + this);
		}

		// a single name element as a relative path of its own, not the ancestor path down to it: the
		// difference decides what Files.walkFileTree and every relativize() built on it produce

		return relativeOf(fs, elements.subList(index, index + 1));
	}

	@Override
	public Path subpath(final int beginIndex, final int endIndex) {

		final List<String> elements = getNameElements();
		if (beginIndex < 0 || beginIndex >= elements.size() || endIndex > elements.size() || beginIndex >= endIndex) {

			throw new IllegalArgumentException("Illegal subpath(" + beginIndex + ", " + endIndex + ") of " + this);
		}

		return relativeOf(fs, elements.subList(beginIndex, endIndex));
	}

	@Override
	public boolean startsWith(final Path other) {

		if (!(other instanceof StructrPath) || !other.getFileSystem().equals(fs) || other.isAbsolute() != isAbsolute()) {

			return false;
		}

		final List<String> mine   = getNameElements();
		final List<String> theirs = ((StructrPath)other).getNameElements();

		return theirs.size() <= mine.size() && mine.subList(0, theirs.size()).equals(theirs);
	}

	@Override
	public boolean startsWith(final String other) {

		return startsWith(pathOf(other));
	}

	@Override
	public boolean endsWith(final Path other) {

		if (!(other instanceof StructrPath) || !other.getFileSystem().equals(fs)) {

			return false;
		}

		final List<String> mine   = getNameElements();
		final List<String> theirs = ((StructrPath)other).getNameElements();

		// an absolute path only ends with another absolute path when they are the same path
		if (other.isAbsolute()) {

			return isAbsolute() && mine.equals(theirs);
		}

		return theirs.size() <= mine.size() && mine.subList(mine.size() - theirs.size(), mine.size()).equals(theirs);
	}

	@Override
	public boolean endsWith(final String other) {

		return endsWith(pathOf(other));
	}

	@Override
	public Path normalize() {

		final List<String> normalized = new ArrayList<>();

		for (final String element : getNameElements()) {

			if (CURRENT_DIRECTORY.equals(element)) {

				continue;
			}

			if (PARENT_DIRECTORY.equals(element)) {

				// ".." above the root is the root itself, as on a Unix filesystem. On a relative path
				// there is nothing above to drop, so it has to be kept.
				if (!normalized.isEmpty() && !PARENT_DIRECTORY.equals(normalized.get(normalized.size() - 1))) {

					normalized.remove(normalized.size() - 1);
					continue;

				} else if (isAbsolute()) {

					continue;
				}
			}

			normalized.add(element);
		}

		if (isAbsolute()) {

			return fs.getPath(ROOT_DIRECTORY + String.join(ROOT_DIRECTORY, normalized));
		}

		return relativeOf(fs, normalized);
	}

	@Override
	public Path resolve(final Path other) {

		if (other.isAbsolute()) {

			return other;
		}

		// returning null here, as this did, turns every Files call that resolves a child into a
		// NullPointerException somewhere else entirely

		return resolve(other.toString());
	}

	@Override
	public Path resolve(final String other) {

		if (other.startsWith(ROOT_DIRECTORY)) {

			return fs.getPath(other);
		}

		if (CURRENT_DIRECTORY.equals(other)) {

			return this;
		}

		logger.info("{}", other);

		// fallback

		return fs.getPath(toString(), other);
	}

	@Override
	public Path resolveSibling(final Path other) {

		return parent != null ? parent.resolve(other) : other;
	}

	@Override
	public Path resolveSibling(final String other) {

		return resolveSibling(pathOf(other));
	}

	@Override
	public Path relativize(final Path other) {

		if (!(other instanceof StructrPath) || !other.getFileSystem().equals(fs)) {

			throw new IllegalArgumentException("Cannot relativize " + other + " against " + this + ": different filesystem");
		}

		// "a relative path cannot be constructed if only one of the paths is absolute", and there is no
		// working directory here to fall back on
		if (other.isAbsolute() != isAbsolute()) {

			throw new IllegalArgumentException("Cannot relativize " + other + " against " + this + ": one is absolute, the other is not");
		}

		final List<String> mine   = getNameElements();
		final List<String> theirs = ((StructrPath)other).getNameElements();
		int common = 0;

		while (common < mine.size() && common < theirs.size() && mine.get(common).equals(theirs.get(common))) {

			common++;
		}

		final List<String> result = new ArrayList<>();

		// up out of what is left of this path, then down into what is left of the other
		for (int i = common; i < mine.size(); i++) {

			result.add(PARENT_DIRECTORY);
		}

		result.addAll(theirs.subList(common, theirs.size()));

		return relativeOf(fs, result);
	}

	@Override
	public URI toUri() {

		final String path = toAbsolutePath().toString();

		try {

			// the authority names the user the filesystem acts as, so the URI round-trips back to a
			// filesystem with the same view rather than to whatever the default one is

			return new URI(StructrPath.SCHEME, fs.getUser(), path, null, null);

		} catch (URISyntaxException uex) {

			throw new IllegalStateException("Unable to build a URI for " + path, uex);
		}
	}

	@Override
	public Path toAbsolutePath() {

		if (isAbsolute()) {

			return this;
		}

		// there is no working directory in this filesystem, so a relative path can only be anchored at
		// the root. The previous version appended this path's own last name to itself.

		return fs.getPath(ROOT_DIRECTORY + toString());
	}

	@Override
	public Path toRealPath(final LinkOption... options) throws IOException {

		// no links and no case folding here, so the real path is just the absolute, normalized one

		return toAbsolutePath().normalize();
	}

	@Override
	public File toFile() {
		
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
		
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
		
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterator<Path> iterator() {

		final List<Path> elements = new ArrayList<>();
		final int count           = getNameCount();

		for (int i = 0; i < count; i++) {

			elements.add(getName(i));
		}

		return elements.iterator();
	}

	@Override
	public int compareTo(final Path other) {

		// the contract is a ClassCastException, not false: a path of another provider is not orderable
		// against this one, and returning an ordering anyway would corrupt any sort it takes part in
		if (!(other instanceof StructrPath)) {

			throw new ClassCastException("Cannot compare " + other + " to " + this + ": different provider");
		}

		return toString().compareTo(other.toString());
	}

	@Override
	public boolean equals(final Object other) {

		// without this, two paths naming the same file are unequal, because Object identity is all that
		// is left. Files and every collection of paths depend on it.
		if (this == other) {

			return true;
		}

		if (!(other instanceof StructrPath)) {

			return false;
		}

		final StructrPath path = (StructrPath)other;

		return fs.equals(path.fs) && toString().equals(path.toString());
	}

	@Override
	public int hashCode() {

		return Objects.hash(fs, toString());
	}

	// ----- protected methods -----
	/**
	 * The name elements of this path, root excluded, outermost first.
	 *
	 * The path is a chain of parents rather than a list, and every one of the name operations is defined
	 * on the list. Building it once here keeps that translation in a single place.
	 */
	protected List<String> getNameElements() {

		final LinkedList<String> elements = new LinkedList<>();
		StructrPath current               = this;

		while (current != null) {

			if (current.name != null) {

				elements.addFirst(current.name);
			}

			current = current.parent;
		}

		return elements;
	}

	/**
	 * The given string as a path of this filesystem, relative if it does not start at the root.
	 *
	 * Not fs.getPath(), which anchors everything at the root: "c" through that becomes "/c", and an
	 * absolute path never ends with another absolute path unless they are equal, so endsWith("c") would
	 * answer false for /a/b/c.
	 */
	protected StructrPath pathOf(final String path) {

		if (path.startsWith(ROOT_DIRECTORY)) {

			return (StructrPath)fs.getPath(path);
		}

		final List<String> elements = new ArrayList<>();

		for (final String element : path.split(ROOT_DIRECTORY)) {

			if (!element.isEmpty()) {

				elements.add(element);
			}
		}

		return relativeOf(fs, elements);
	}

	/**
	 * A relative path made of the given name elements.
	 *
	 * StructrPath is abstract, and StructrFilePath is what getFileName() already uses to represent a
	 * bare name, so a chain of those is what a relative path is here. An empty list is the empty path,
	 * which is what relativize() returns for two equal paths.
	 */
	protected static StructrPath relativeOf(final StructrFilesystem fs, final List<String> elements) {

		if (elements.isEmpty()) {

			return new StructrFilePath(fs, null, "");
		}

		StructrPath result = null;

		for (final String element : elements) {

			result = new StructrFilePath(fs, result, element);
		}

		return result;
	}

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
