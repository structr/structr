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
package org.structr.files.ssh.filesystem.path.components;

import org.structr.files.ssh.filesystem.path.page.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrToplevelAttributes;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.relationship.DOMChildren;

/**
 *
 */
public class StructrComponentsPath extends StructrPath {

	private static final Logger logger = Logger.getLogger(StructrComponentsPath.class.getName());

	public StructrComponentsPath(final StructrFilesystem fs, final StructrPath parent) {
		super(fs, parent, StructrPath.COMPONENTS_DIRECTORY);
	}

	@Override
	public FileChannel newFileChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(final DirectoryStream.Filter<? super Path> filter) {

		return new DirectoryStream() {

			boolean closed = false;

			@Override
			public Iterator iterator() {

				if (!closed) {


					final App app                      = StructrApp.getInstance(fs.getSecurityContext());
					final List<StructrPath> components = new LinkedList<>();
					int pos                            = 0;

					try (final Tx tx = app.tx()) {

						final ShadowDocument doc = app.nodeQuery(ShadowDocument.class).includeDeletedAndHidden().getFirst();
						if (doc != null) {

							for (final DOMNode node : doc.getProperty(Page.elements)) {

								if (!node.hasIncomingRelationships(DOMChildren.class)) {

									components.add(new StructrDOMNodePath(fs, StructrComponentsPath.this, doc, null, node, createComponentName(node, pos++)));
								}
							}
						}

						tx.success();

						return components.iterator();

					} catch (FrameworkException fex) {
						logger.log(Level.WARNING, "Unable to create directory stream", fex);
					}
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
	public void createDirectory(FileAttribute<?>... attrs) throws IOException {
		throw new FileAlreadyExistsException(this.toString());
	}

	@Override
	public void delete() throws IOException {
		throw new AccessDeniedException(toString());
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) throws FrameworkException {

		int pos = 0;

		// resolve node by path component
		final ShadowDocument doc = StructrApp.getInstance(fs.getSecurityContext()).nodeQuery(ShadowDocument.class).includeDeletedAndHidden().getFirst();
		if (doc != null) {

			for (final DOMNode child : doc.getProperty(Page.elements)) {

				if (!child.hasIncomingRelationships(DOMChildren.class)) {

					if (pathComponent.equals(createComponentName(child, pos++))) {

						return new StructrDOMNodePath(fs, this, doc, null, child, pathComponent);
					}
				}
			}
		}

		return new StructrNonexistingComponentPath(fs, this, pathComponent);
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) {
		return new StructrToplevelAttributes(StructrPath.COMPONENTS_DIRECTORY).toMap(attributes);
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) {
		return (T)new StructrToplevelAttributes(StructrPath.COMPONENTS_DIRECTORY);
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

	// ----- private methods -----
	private String createComponentName(final DOMNode node, final int pos) {

		final StringBuilder buf = new StringBuilder();

		if (node != null) {

			buf.append(node.getType().toLowerCase());
		}

		buf.append("-");

		String name = node.getProperty(AbstractNode.name);
		if (name != null) {

			buf.append(name);

		} else {

			buf.append("unnamed");
			buf.append(pos);
		}

		return buf.toString();
	}

	private DOMNode getActualComponent(final String componentName) throws FrameworkException {

		int pos = 0;

		// resolve node by path component
		final ShadowDocument doc = StructrApp.getInstance(fs.getSecurityContext()).nodeQuery(ShadowDocument.class).includeDeletedAndHidden().getFirst();
		if (doc != null) {

			for (final DOMNode child : doc.getProperty(Page.elements)) {

				if (!child.hasIncomingRelationships(DOMChildren.class)) {

					if (componentName.equals(createComponentName(child, pos++))) {

						return child;
					}
				}
			}
		}

		return null;
	}
}
