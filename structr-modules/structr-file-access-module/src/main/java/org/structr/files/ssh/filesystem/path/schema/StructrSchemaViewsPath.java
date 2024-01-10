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
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrToplevelAttributes;

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
public class StructrSchemaViewsPath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrSchemaViewsPath.class.getName());

	private AbstractSchemaNode schemaNode = null;

	public StructrSchemaViewsPath(final StructrFilesystem fs, final StructrPath parent, final AbstractSchemaNode schemaNode) {
		super(fs, parent, "views");

		this.schemaNode = schemaNode;
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {

		if (schemaNode != null) {

			return new DirectoryStream() {

				boolean closed = false;

				@Override
				public Iterator iterator() {

					final App app                 = StructrApp.getInstance(fs.getSecurityContext());
					final List<StructrPath> nodes = new LinkedList<>();

					try (final Tx tx = app.tx()) {

						for (final SchemaView schemaView : schemaNode.getProperty(SchemaNode.schemaViews)) {

							nodes.add(new StructrSchemaViewPath(fs, StructrSchemaViewsPath.this,schemaNode, schemaView));
						}

						tx.success();

					} catch (FrameworkException fex) {
						logger.warn("", fex);
					}

					return nodes.iterator();
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

		final App app    = StructrApp.getInstance(fs.getSecurityContext());
		StructrPath path = null;

		try (final Tx tx = app.tx()) {

			for (final SchemaView schemaView : schemaNode.getProperty(SchemaNode.schemaViews)) {

				if (pathComponent.equals(schemaView.getName())) {

					path = new StructrSchemaViewPath(fs, StructrSchemaViewsPath.this,schemaNode, schemaView);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return path;
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {
		return new StructrToplevelAttributes("views").toMap(attributes);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {
		return (T)new StructrToplevelAttributes("views");
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
