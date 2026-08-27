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
import org.structr.storage.util.VirtualFileChannel;

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
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;

/**
 *
 */
public class StructrFilesystemProvider extends FileSystemProvider {

	private static final Logger logger = LoggerFactory.getLogger(StructrFilesystemProvider.class.getName());

	/**
	 * The URI scheme this provider answers to, as in structr:///path/to/file.
	 *
	 * The authority names the user the filesystem acts as: structr://alice/ is Alice's view of the
	 * virtual filesystem, structr:/// is the superuser's. Two filesystems for different users are
	 * separate instances, because the SecurityContext is what decides which files are visible at all.
	 */
	public static final String SCHEME = "structr";

	/** The env key for handing in a SecurityContext directly, instead of naming a user in the URI. */
	public static final String SECURITY_CONTEXT_KEY = "securityContext";

	/**
	 * The filesystems created through newFileSystem, by user name.
	 *
	 * The JDK creates exactly one provider instance per installed provider and keeps it for the life of
	 * the VM, so this map is the registry the FileSystems facade looks into. It is not a cache that may
	 * be dropped: getFileSystem must return the same instance newFileSystem returned, or a Path created
	 * from one would not be equal to a Path created from the other.
	 */
	private final Map<String, StructrFilesystem> filesystems = new ConcurrentHashMap<>();

	@Override
	public String getScheme() {

		return SCHEME;
	}

	@Override
	public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {

		final String user = userOf(uri);

		// computeIfAbsent would hide an existing filesystem, and the contract is to reject one
		synchronized (filesystems) {

			if (filesystems.containsKey(user)) {

				throw new FileSystemAlreadyExistsException(uri.toString());
			}

			final StructrFilesystem fs = new StructrFilesystem(securityContextFor(user, env), this, user);

			filesystems.put(user, fs);

			return fs;
		}
	}

	@Override
	public FileSystem getFileSystem(final URI uri) {

		final FileSystem fs = filesystems.get(userOf(uri));

		if (fs == null) {

			throw new FileSystemNotFoundException(uri.toString());
		}

		return fs;
	}

	@Override
	public Path getPath(final URI uri) {

		final String path = uri.getPath();

		if (path == null || !path.startsWith(StructrPath.ROOT_DIRECTORY)) {

			throw new IllegalArgumentException("Expected an absolute path in " + uri + ", for example " + SCHEME + ":///dir/file.txt");
		}

		// unlike getFileSystem, this creates the filesystem on demand: Paths.get(URI) is the entry point
		// for code that only has a URI, and requiring a newFileSystem call first would make every caller
		// carry the same two lines
		return fileSystemFor(userOf(uri), null).getPath(path);
	}

	/**
	 * The filesystem for the given user, created if it does not exist yet.
	 *
	 * This is the entry point for code inside Structr, which has a SecurityContext in hand and no reason
	 * to build a URI for it.
	 */
	public StructrFilesystem fileSystemFor(final String user, final Map<String, ?> env) {

		return filesystems.computeIfAbsent(user, key -> new StructrFilesystem(securityContextFor(key, env), this, key));
	}

	@Override
	public synchronized FileChannel newFileChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {

		return new VirtualFileChannel(null, newByteChannel(path, options, attrs));
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
	public boolean isHidden(final Path path) throws IOException {

		final Path name = path.getFileName();

		// the Unix rule, which is the one this filesystem's paths follow
		return name != null && name.toString().startsWith(".");
	}

	@Override
	public FileStore getFileStore(final Path path) throws IOException {

		return ((StructrFilesystem)checkPath(path).getFileSystem()).getFileStore();
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

	// ----- private methods -----
	/**
	 * The user name in the authority of the URI, or the empty string for the superuser.
	 *
	 * The empty string is a real key rather than a null: it keeps the superuser filesystem in the same
	 * map as the others, so there is one lookup path instead of two.
	 */
	private String userOf(final URI uri) {

		if (uri == null || !SCHEME.equalsIgnoreCase(uri.getScheme())) {

			throw new IllegalArgumentException("Expected a URI with scheme '" + SCHEME + "', got " + uri);
		}

		final String authority = uri.getAuthority();

		return authority != null ? authority : "";
	}

	/**
	 * The SecurityContext a filesystem acts under.
	 *
	 * A context handed in through the env map wins: the caller already knows who it is acting as, and
	 * resolving a name to a user again could only get it wrong. Otherwise the name is looked up, and an
	 * empty name is the superuser.
	 */
	private SecurityContext securityContextFor(final String user, final Map<String, ?> env) {

		if (env != null && env.get(SECURITY_CONTEXT_KEY) instanceof SecurityContext ctx) {

			return ctx;
		}

		if (StringUtils.isBlank(user)) {

			return SecurityContext.getSuperUserInstance();
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(user).getFirst();

			if (node == null) {

				throw new IllegalArgumentException("No such user: " + user);
			}

			final Principal principal      = node.as(Principal.class);
			final SecurityContext instance = SecurityContext.getInstance(principal, principal.isAdmin() ? org.structr.common.AccessMode.Backend : org.structr.common.AccessMode.Frontend);

			tx.success();

			return instance;

		} catch (FrameworkException fex) {

			throw new IllegalArgumentException("Unable to resolve user " + user + ": " + fex.getMessage(), fex);
		}
	}

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
