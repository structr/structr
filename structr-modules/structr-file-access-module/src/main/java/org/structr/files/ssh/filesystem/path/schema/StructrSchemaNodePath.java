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
package org.structr.files.ssh.filesystem.path.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrToplevelAttributes;
import org.structr.files.ssh.filesystem.path.graph.StructrNodePropertyPath;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;

/**
 *
 */
public class StructrSchemaNodePath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrSchemaNodePath.class.getName());

	private AbstractSchemaNode cachedSchemaNode = null;

	public StructrSchemaNodePath(final StructrFilesystem fs, final StructrPath parent, final String name) {
		super(fs, parent, name);
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {

		final AbstractSchemaNode schemaNode = getSchemaNode();
		if (schemaNode != null) {

			return new DirectoryStream() {

				boolean closed = false;

				@Override
				public Iterator iterator() {

					if (!closed) {

						final List<StructrPath> children = new ArrayList<>();

						//children.add(new StructrSchemaPropertiesPath(fs, StructrSchemaNodePath.this, schemaNode));
						//children.add(new StructrSchemaViewsPath(fs, StructrSchemaNodePath.this, schemaNode));
						children.add(new StructrSchemaMethodsPath(fs, StructrSchemaNodePath.this, schemaNode));
						children.add(resolveStructrPath("extendsClass"));

						return children.iterator();

					}

					return Collections.emptyIterator();
				}

				@Override
				public void close() throws IOException {
					closed = true;
				}
			};

		}

		return null;
	}

	@Override
	public SeekableByteChannel newChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {
		throw new FileAlreadyExistsException(toString());
	}

	@Override
	public void delete() throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {

		switch (pathComponent) {

			/* disabled for now..
			case "properties":
				return new StructrSchemaPropertiesPath(fs, this, getSchemaNode());

			case "views":
				return new StructrSchemaViewsPath(fs, this, getSchemaNode());
			*/

			case "methods":
				return new StructrSchemaMethodsPath(fs, this, getSchemaNode());

			case "extendsClass":
				return new StructrNodePropertyPath(fs, this, getSchemaNode(), SchemaNode.extendsClass);
		}

		return null;
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {

		final AbstractSchemaNode schemaNode = getSchemaNode();
		if (schemaNode != null) {

			return new StructrToplevelAttributes(schemaNode.getName()).toMap(attributes);
		}

		throw new NoSuchFileException(toString());
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {

		final AbstractSchemaNode schemaNode = getSchemaNode();
		if (schemaNode != null) {

			final App app = StructrApp.getInstance(fs.getSecurityContext());
			String name   = null;

			try (final Tx tx = app.tx()) {

				name = schemaNode.getName();

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

			return (T)new StructrToplevelAttributes(name);
		}

		throw new NoSuchFileException(toString());
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
	public <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException {
		return (V)getAttributes((Class)null, options);
	}

	@Override
	public boolean isSameFile(final Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public AbstractSchemaNode getSchemaNode() {

		if (cachedSchemaNode == null) {

			final App app = StructrApp.getInstance(fs.getSecurityContext());
			try (final Tx tx = app.tx()) {

				// remove /files from path since it is a virtual directory
				cachedSchemaNode = app.nodeQuery(AbstractSchemaNode.class).and(AbstractNode.name, name).sort(AbstractNode.name).getFirst();

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("Unable to load actual file for path {}: {}", new Object[] { toString(), fex.getMessage() } );
			}
		}

		return cachedSchemaNode;
	}
}
