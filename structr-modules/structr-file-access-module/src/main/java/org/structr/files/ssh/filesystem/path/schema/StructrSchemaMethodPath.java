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
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.path.graph.StructrPropertyValueAttributes;
import org.structr.files.ssh.filesystem.path.graph.StructrPropertyValueChannel;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class StructrSchemaMethodPath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrSchemaMethodPath.class.getName());

	private AbstractSchemaNode schemaNode = null;

	public StructrSchemaMethodPath(final StructrFilesystem fs, final StructrPath parent, final AbstractSchemaNode schemaNode, final String name) {
		super(fs, parent, name);

		this.schemaNode   = schemaNode;
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public SeekableByteChannel newChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {

		// Possible open options are: CREATE, READ, WRITE, TRUNCATE_EXISTING
		// The filesystem code will first try to fetch the attributes of a file, then call this
		// method with the CREATE and WRITE OPTIONS (and an optional TRUNCATE_EXISTING)

		SchemaMethod method       = getSchemaMethodNode();
		final boolean read        = options.contains(StandardOpenOption.READ);
		final boolean create      = options.contains(StandardOpenOption.CREATE);
		final boolean createNew   = options.contains(StandardOpenOption.CREATE_NEW);
		final boolean write       = options.contains(StandardOpenOption.WRITE);
		final boolean truncate    = options.contains(StandardOpenOption.TRUNCATE_EXISTING);
		final boolean append      = options.contains(StandardOpenOption.APPEND);

		// creation of a new file requested (=> create a new schema method)
		if (create || createNew) {

			// if CREATE_NEW, file must not exist, otherwise an error should be thrown
			if (createNew && method != null) {
				throw new java.nio.file.FileAlreadyExistsException(toString());
			}

			final App app = StructrApp.getInstance(fs.getSecurityContext());
			try (final Tx tx = app.tx()) {

				// create a new schema method with an empty source string
				method = app.create(SchemaMethod.class,
					new NodeAttribute<>(SchemaMethod.schemaNode, schemaNode),
					new NodeAttribute<>(SchemaMethod.virtualFileName, name),
					new NodeAttribute<>(AbstractNode.name, normalizeFileNameForJavaIdentifier(name)),
					new NodeAttribute<>(SchemaMethod.source, "")
				);

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}

		return new StructrPropertyValueChannel(fs.getSecurityContext(), method, SchemaMethod.source, truncate, append);
	}

	@Override
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {
		throw new java.nio.file.AccessDeniedException(toString());
	}

	@Override
	public void delete() throws IOException {

		final SchemaMethod schemaMethod = getSchemaMethodNode();
		if (schemaMethod != null) {

			final App app = StructrApp.getInstance(fs.getSecurityContext());
			try (final Tx tx = app.tx()) {

				app.delete(schemaMethod);
				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

		} else {

			throw new NoSuchFileException(name);
		}
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {
		return null;
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {

		final SchemaMethod method = getSchemaMethodNode();
		if (method != null) {

			return new StructrPropertyValueAttributes(fs.getSecurityContext(), getSchemaMethodNode(), SchemaMethod.source).toMap(attributes);
		}

		throw new NoSuchFileException(name);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(final Class<T> type, final LinkOption... options) throws IOException {

		final SchemaMethod method = getSchemaMethodNode();
		if (method != null) {

			return (T)new StructrPropertyValueAttributes(fs.getSecurityContext(), getSchemaMethodNode(), SchemaMethod.source);
		}

		throw new NoSuchFileException(name);
	}

	@Override
	public void copy(final Path target, final CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(final Path target, final CopyOption... options) throws IOException {

		if (target instanceof StructrSchemaMethodPath) {

			final App app                       = StructrApp.getInstance(fs.getSecurityContext());
			final StructrSchemaMethodPath other = (StructrSchemaMethodPath)target;
			final AbstractSchemaNode otherNode  = other.getSchemaNode();
			final SchemaMethod method           = getSchemaMethodNode();
			final String targetName             = target.getFileName().toString();

			try (final Tx tx = app.tx()) {

				if (otherNode.getUuid().equals(schemaNode.getUuid())) {

					// move from node to same node
					method.setProperty(SchemaMethod.name, normalizeFileNameForJavaIdentifier(targetName));
					method.setProperty(SchemaMethod.virtualFileName, targetName);
				}

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
			}

		}
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
		return schemaNode;
	}

	public SchemaMethod getSchemaMethodNode() {

		final App app       = StructrApp.getInstance(fs.getSecurityContext());
		SchemaMethod method = null;

		try (final Tx tx = app.tx()) {

			for (final SchemaMethod schemaMethod : schemaNode.getProperty(SchemaNode.schemaMethods)) {

				final String fileName   = schemaMethod.getProperty(SchemaMethod.virtualFileName);
				final String methodName = schemaMethod.getProperty(AbstractNode.name);

				if (fileName != null && fileName.equals(name)) {
					method = schemaMethod;
					break;
				}

				if (methodName != null && methodName.equals(name)) {

					method = schemaMethod;
					break;
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return method;
	}
}
