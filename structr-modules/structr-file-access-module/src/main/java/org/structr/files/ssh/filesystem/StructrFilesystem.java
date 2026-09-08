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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.path.file.StructrFilePath;
import org.structr.files.ssh.filesystem.path.file.StructrFilesRootPath;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Set;

/**
 *
 */
public class StructrFilesystem extends FileSystem {

	private static final Logger logger = LoggerFactory.getLogger(StructrFilesystem.class.getName());

	private StructrFilesystemProvider provider = null;
	private SecurityContext securityContext    = null;
	private String lastFullPath                = null;
	private StructrPath last                   = null;
	private StructrPath root                   = null;
	private String user                        = "";
	private final StructrFileStore fileStore   = new StructrFileStore(this);

	public StructrFilesystem(final SecurityContext securityContext) {

		this(securityContext, new StructrFilesystemProvider());
	}

	/**
	 * A filesystem belonging to an existing provider.
	 *
	 * The installed provider is a single instance the JDK creates and keeps, and every filesystem it
	 * hands out has to report that instance from provider(). A filesystem that built its own would send
	 * Files.newInputStream and friends to a provider that has never heard of it.
	 */
	public StructrFilesystem(final SecurityContext securityContext, final StructrFilesystemProvider provider) {

		this(securityContext, provider, "");
	}

	/**
	 * A filesystem for a named user.
	 *
	 * The name is kept so a Path can name it again in toUri(): a URI that dropped it would round-trip
	 * back to the superuser's view of the filesystem rather than this one.
	 */
	public StructrFilesystem(final SecurityContext securityContext, final StructrFilesystemProvider provider, final String user) {

		this.provider        = provider;
		this.root            = new StructrFilesRootPath(this);
		this.securityContext = securityContext;
		this.user            = user;
	}

	/** The user this filesystem acts as, empty for the superuser. */
	public String getUser() {

		return user;
	}

	@Override
	public FileSystemProvider provider() {

		return provider;
	}

	@Override
	public void close() throws IOException {

		// closing not supported
	}

	@Override
	public boolean isOpen() {

		return true;
	}

	@Override
	public boolean isReadOnly() {

		return false;
	}

	@Override
	public String getSeparator() {

		return "/";
	}

	@Override
	public Iterable<Path> getRootDirectories() {

		return Arrays.asList(new Path[] { root });
	}

	@Override
	public Iterable<FileStore> getFileStores() {

		return Arrays.asList(new FileStore[] { fileStore });
	}

	public FileStore getFileStore() {

		return fileStore;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {

		return StructrFileAttributes.SUPPORTED_VIEWS;
	}

	@Override
	public Path getPath(final String first, final String... more) {

		// build a full path string
		final StringBuilder pathBuilder = new StringBuilder(first);

		for (final String component : more) {

			pathBuilder.append("/");
			pathBuilder.append(component);
		}

		final String fullPath = pathBuilder.toString();

		if (fullPath.equals(lastFullPath) && last != null && !last.dontCache()) {

			return last;
		}

		// starts empty, NOT at the cached path: the cache above is an exact-match shortcut, and using it
		// as the base for a relative path made getPath("z") resolve against whatever was asked for last,
		// so the same argument returned different paths depending on call history
		StructrPath path = null;

		// avoid multiple transactions
		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String[] parts  = fullPath.split("/");

			if (fullPath.startsWith("/")) {

				path = root;
			}

			for (int i=0; i<parts.length; i++) {

				final String component = parts[i];
				if (!component.isEmpty()) {

					if ("..".equals(component)) {

						// navigate to parent, but not above root
						if (path != null && path.getParent() != null) {

							final Path parent = path.getParent();
							path = (parent instanceof StructrPath) ? (StructrPath) parent : root;

						} else {

							path = root;
						}

					} else if (".".equals(component)) {

						// current directory, skip

					} else if (path != null) {

						// resolve against existing path
						path = path.resolveStructrPath(component);

					} else {

						// create new, relative path
						path = new StructrFilePath(this, null, component);
					}
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}

		// cache a single path instance until a different path is requested
		// (should increase performance of repeated evaulations of the same path)
		lastFullPath = fullPath;
		last = path;

		if (path == null) {
			
			path = root;
		}

		return path;
	}

	@Override
	public PathMatcher getPathMatcher(final String syntaxAndPattern) {

		// glob and regex are defined over the path string, and this filesystem uses the same separator as
		// the platform's, so the platform's own matcher gives exactly the right answers. A second glob
		// compiler here would only be a new place for the two to disagree.
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);

		return path -> matcher.matches(Paths.get(path.toString()));
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {

		return new StructrUserPrincipalLookupService(securityContext);
	}

	/**
	 * Not supported, and that is the contract rather than a gap: a FileSystem is required to throw here
	 * when it does not watch for changes, and nothing in the graph reports file events to poll.
	 */
	@Override
	public WatchService newWatchService() throws IOException {

		throw new UnsupportedOperationException("The Structr filesystem does not support watch services.");
	}

	// ----- package methods -----
	Path getRoot() {

		return root;
	}

	public SecurityContext getSecurityContext() {

		return securityContext;
	}
}
