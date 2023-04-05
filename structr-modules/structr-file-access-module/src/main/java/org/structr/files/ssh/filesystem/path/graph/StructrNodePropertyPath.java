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
package org.structr.files.ssh.filesystem.path.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;

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
public class StructrNodePropertyPath extends StructrPath {

	private static final Logger logger = LoggerFactory.getLogger(StructrNodePropertyPath.class.getName());

	private PropertyKey key  = null;
	private GraphObject node = null;

	public StructrNodePropertyPath(final StructrFilesystem fs, final StructrPath parent, final GraphObject node, final PropertyKey property) {

		super(fs, parent, property.jsonName());

		this.node   = node;
		this.key    = property;
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public SeekableByteChannel newChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {

		parent.enablePropertyFile(name);

		if (options.contains(StandardOpenOption.WRITE)) {
			StructrPropertyValueChannel.checkWriteAccess(key);
		}

		return new StructrPropertyValueChannel(fs.getSecurityContext(), node, key, true, false);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {

		if (!parent.hasPropertyFile(name)) {
			throw new NoSuchFileException(name);
		}

		return (T)new StructrPropertyValueAttributes(fs.getSecurityContext(), node, key);
	}

	@Override
	public Map<String, Object> getAttributes(String attributes, LinkOption... options) throws IOException {

		if (!parent.hasPropertyFile(name)) {
			throw new NoSuchFileException(name);
		}

		return new StructrPropertyValueAttributes(fs.getSecurityContext(), node, key).toMap(attributes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException {

		if (!parent.hasPropertyFile(name)) {
			throw new NoSuchFileException(name);
		}

		return (V)getAttributes((Class)null, options);
	}

	@Override
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void delete() throws IOException {

		// no error, no modification (allows rm -rf of a page / node dir)
		if (key.isSystemInternal() || key.isReadOnly() || key.isWriteOnce()) {
			return;
		}

		try (final Tx tx = StructrApp.getInstance(fs.getSecurityContext()).tx()) {

			node.setProperty(key, null);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to set property {} to null: {}", new Object[] { key.jsonName(), fex.getMessage() } );
		}
	}

	@Override
	public void copy(Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(Path target, CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setAttribute(String attribute, Object value, LinkOption... options) throws IOException {
	}

	@Override
	public boolean isSameFile(Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public StructrPath resolveStructrPath(String pathComponent) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
