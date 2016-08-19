/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.files.ssh.filesystem.path.page;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.web.entity.dom.Page;

/**
 *
 */
public class StructrNonexistingPagePath extends StructrPath {

	private static final Logger logger = Logger.getLogger(StructrNonexistingPagePath.class.getName());

	private Page newPage = null;

	public StructrNonexistingPagePath(final StructrFilesystem fs, final StructrPath parent, final String name) {
		super(fs, parent, name);
	}

	@Override
	public FileChannel newFileChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(final DirectoryStream.Filter<? super Path> filter) {
		return null;
	}

	@Override
	public void createDirectory(final FileAttribute<?>... attrs) throws IOException {

		final App app = StructrApp.getInstance(fs.getSecurityContext());
		try (final Tx tx = app.tx()) {

			final StructrPath.HiddenFileEntry entry = new StructrPath.HiddenFileEntry();
			newPage                                 = app.create(Page.class, name);

			// mark all new properties as hidden upon creation
			for (final PropertyKey key : newPage.getPropertyKeys(PropertyView.Ui)) {
				entry.add(key.jsonName());
			}

			StructrPath.HIDDEN_PROPERTY_FILES.put(newPage.getUuid(), entry);

			tx.success();

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "", fex);
		}
	}

	@Override
	public void delete() throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {
		return null;
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {

		if (newPage == null) {
			throw new NoSuchFileException(name);
		}

		return new StructrDOMAttributes(fs.getSecurityContext(), newPage).toMap(attributes);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {

		if (newPage == null) {
			throw new NoSuchFileException(name);
		}

		return (T)new StructrDOMAttributes(fs.getSecurityContext(), newPage);
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
	}

	@Override
	public boolean isSameFile(final Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean dontCache() {
		return true;
	}
}
