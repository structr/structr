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
package org.structr.files.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 *
 */
public class StructrSSHFile implements Path {

	private static final Logger logger = LoggerFactory.getLogger(StructrSSHFile.class.getName());

	protected SecurityContext securityContext = null;
	protected FileSystem fileSystem           = null;
	protected StructrSSHFile parent           = null;
	protected AbstractFile actualFile         = null;
	protected String name                     = null;

	public StructrSSHFile(final SecurityContext securityContext) {

		this(null, "/", null);

		this.securityContext = securityContext;
	}

	public StructrSSHFile(final StructrSSHFile parent, final String name, final AbstractFile actualFile) {

		this.actualFile = actualFile;
		this.parent     = parent;
		this.name       = name;

		if (parent != null) {
			this.fileSystem = parent.getFileSystem();
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + name + ")";
	}

	public SecurityContext getSecurityContext() {

		if (parent != null) {
			return parent.getSecurityContext();
		}

		return securityContext;
	}

	public AbstractFile getActualFile() {
		return actualFile;
	}

	public void setActualFile(final AbstractFile file) {
		this.actualFile = file;
	}

	public void setFileSystem(final FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public StructrSSHFile findFile(final String path) {

		if ("/".equals(path)) {
			return getRootFolder();
		}

		final App app              = StructrApp.getInstance(getRootFolder().getSecurityContext());
		final boolean isAbsolute   = path.startsWith("/");
		final String localPath     = isAbsolute ? path.substring(1) : path;
		final String[] parts       = localPath.split("[/]+");
		final String localPart     = parts[0];

		if (".".equals(path)) {
			return this;
		}

		if ("..".equals(path)) {
			return parent;
		}

		if (isAbsolute && parent != null) {

			return getRootFolder().findFile(path);

		} else {

			// look for files/folders without parent
			try (final Tx tx = app.tx()) {

				for (final Folder folder : getFolders()) {

					final String folderName = folder.getName();
					if (localPart.equals(folderName)) {

						final StructrSSHFile matchingFolder = new StructrSSHFile(this, folderName, folder);
						if (parts.length > 1) {

							return matchingFolder.findFile(localPath.substring(folderName.length() + 1));

						} else {

							// match found
							return matchingFolder;
						}
					}
				}

				for (final File file : getFiles()) {

					final String fileName = file.getName();
					if (localPart.equals(fileName)) {

						if (parts.length > 1) {

							throw new IllegalStateException("Found file where folder was expected, aborting.");
						}

						return new StructrSSHFile(this, fileName, file);
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}

		return new StructrSSHFile(this, path, null);
	}

	private StructrSSHFile getRootFolder() {

		if (parent != null) {
			return parent;
		}

		return this;
	}

	protected Iterable<Folder> getFolders() throws FrameworkException {

		if (actualFile != null && parent != null && actualFile.is(StructrTraits.FOLDER)) {

			return actualFile.as(Folder.class).getFolders();

		} else {

			final Iterable<NodeInterface> folders = StructrApp.getInstance(getSecurityContext()).nodeQuery(StructrTraits.FOLDER).and(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), null).getResultStream();

			return Iterables.map(n -> n.as(Folder.class), folders);
		}
	}

	protected Iterable<File> getFiles() throws FrameworkException {

		if (actualFile != null && parent != null && actualFile.is(StructrTraits.FOLDER)) {

			return actualFile.as(Folder.class).getFiles();

		} else {

			final Iterable<NodeInterface> files = StructrApp.getInstance(getSecurityContext()).nodeQuery(StructrTraits.FILE).and(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), null).getResultStream();

			return Iterables.map(n -> n.as(File.class), files);
		}
	}

	@Override
	public FileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public Path getRoot() {
		return getFileSystem().getPath("/");
	}

	@Override
	public Path getFileName() {
		return Paths.get(name);
	}

	@Override
	public Path getParent() {
		return parent;
	}

	@Override
	public int getNameCount() {
		logger.info("Method not implemented yet"); return 0;
	}

	@Override
	public Path getName(int i) {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public Path subpath(int i, int i1) {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public boolean startsWith(Path path) {
		logger.info("Method not implemented yet"); return false;
	}

	@Override
	public boolean startsWith(String string) {
		logger.info("Method not implemented yet"); return false;
	}

	@Override
	public boolean endsWith(Path path) {
		logger.info("Method not implemented yet"); return false;
	}

	@Override
	public boolean endsWith(String string) {
		logger.info("Method not implemented yet"); return false;
	}

	@Override
	public Path normalize() {
		// We assume no redundant path parts, see https://docs.oracle.com/javase/7/docs/api/java/nio/file/Path.html#normalize()
		return this;
	}

	@Override
	public Path resolve(Path path) {
		return findFile(path.toString());
	}

	@Override
	public Path resolve(String string) {
		return findFile(string);
	}

	@Override
	public Path resolveSibling(Path path) {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public Path resolveSibling(String string) {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public Path relativize(Path path) {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public URI toUri() {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public Path toAbsolutePath() {
		// We assume this is already an absolute path, see https://docs.oracle.com/javase/7/docs/api/java/nio/file/Path.html#toAbsolutePath()
		return this;
	}

	@Override
	public Path toRealPath(LinkOption... los) throws IOException {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public java.io.File toFile() {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public WatchKey register(WatchService ws, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier... mdfrs) throws IOException {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public WatchKey register(WatchService ws, WatchEvent.Kind<?>... kinds) throws IOException {
		logger.info("Method not implemented yet"); return null;
	}

	@Override
	public Iterator<Path> iterator() {

		Path _parent = this.getParent();

		final List<Path> pathElements = new ArrayList<>();
		pathElements.add(this);

		while (_parent != null) {

			pathElements.add(_parent);
			_parent = _parent.getParent();
		}

		Collections.reverse(pathElements);

		return pathElements.iterator();
	}

	@Override
	public int compareTo(Path path) {
		logger.info("Method not implemented yet"); return 0;
	}
}
