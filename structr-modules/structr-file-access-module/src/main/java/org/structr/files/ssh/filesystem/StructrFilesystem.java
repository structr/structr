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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.path.StructrRootPath;
import org.structr.files.ssh.filesystem.path.file.StructrFilePath;

import java.io.IOException;
import java.nio.file.*;
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

	public StructrFilesystem(final SecurityContext securityContext) {

		this.provider        = new StructrFilesystemProvider();
		this.root            = new StructrRootPath(this);
		this.securityContext = securityContext;
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

		logger.info("x");

		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
		StructrPath path = last;

		if (fullPath.equals(lastFullPath) && last != null && !last.dontCache()) {
			return last;
		}

		// avoid multiple transactions
		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String[] parts  = fullPath.split("/");

			if (fullPath.startsWith("/")) {
				path = root;
			}

			for (int i=0; i<parts.length; i++) {

				final String component = parts[i];
				if (!component.isEmpty()) {

					if (path != null) {

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
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public WatchService newWatchService() throws IOException {
		logger.info("x");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- package methods -----
	Path getRoot() {
		return root;
	}

	public SecurityContext getSecurityContext() {
		return securityContext;
	}
}
