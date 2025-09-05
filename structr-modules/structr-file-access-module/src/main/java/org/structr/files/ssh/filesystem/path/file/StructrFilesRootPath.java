/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.files.ssh.filesystem.path.file;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrRootAttributes;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;

/**
 *
 */
public class StructrFilesRootPath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrFilesRootPath.class.getName());
	private static final StructrRootAttributes rootAttributes = new StructrRootAttributes(StructrPath.ROOT_DIRECTORY);

	public StructrFilesRootPath(final StructrFilesystem fs) {

		super(fs);
	}

	@Override
	public SeekableByteChannel newChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(final DirectoryStream.Filter<? super Path> filter) {

		return new DirectoryStream<>() {

			boolean closed = false;

			@Override
			public @NotNull Iterator<Path> iterator() {

				if (!closed) {

					final App app                           = StructrApp.getInstance(fs.getSecurityContext());
					final Traits traits                     = Traits.of(StructrTraits.ABSTRACT_FILE);
					final PropertyKey<String> nameKey       = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
					final PropertyKey<Boolean> hasParentKey = traits.key(AbstractFileTraitDefinition.HAS_PARENT_PROPERTY);
					final List<Path> files                  = new LinkedList<>();

					try (final Tx tx = app.tx()) {

						for (final NodeInterface folder : app.nodeQuery(StructrTraits.FOLDER).key(hasParentKey, false).sort(nameKey).getAsList()) {

							files.add(new StructrFilePath(fs, StructrFilesRootPath.this, folder.getName()));
						}

						for (final NodeInterface file : app.nodeQuery(StructrTraits.FILE).key(hasParentKey, false).sort(nameKey).getAsList()) {

							files.add(new StructrFilePath(fs, StructrFilesRootPath.this, file.getName()));
						}

						tx.success();

					} catch (FrameworkException fex) {
						logger.warn("", fex);
					}

					return files.iterator();

				}

				return Collections.emptyIterator();
			}

			@Override
			public void close() throws IOException {
				closed = true;
			}
		};

	}

	@Override
	public void createDirectory(final FileAttribute<?>... attrs) throws IOException {
		throw new FileAlreadyExistsException(this.toString());
	}

	@Override
	public void delete() throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {
		return new StructrFilePath(fs, this, pathComponent);
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
}
