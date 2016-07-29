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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.PropertyKey;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.path.components.StructrNonexistingComponentPath;
import org.structr.files.ssh.filesystem.path.graph.StructrNodePropertyPath;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.w3c.dom.Document;

/**
 *
 * @author Christian Morgner
 */
public class StructrDOMNodePath extends StructrPath {

	private static final Set<String> blacklist   = new LinkedHashSet<>(Arrays.asList(new String[] {
		"type", "pageId", "parent", "children", "childrenIds", "syncedNodes", "mostUsedTags",
		"isDOMNode", "isPage", "createdDate", "createdBy", "lastModifiedDate", "linkingElements",
		"deleted", "hidden", "isContent", "version"
	} ));

	private static final Logger logger           = Logger.getLogger(StructrDOMNodePath.class.getName());

	private Page ownerDocument = null;
	private DOMNode parentNode = null;
	private DOMNode domNode    = null;
	private String uuid        = null;

	public StructrDOMNodePath(final StructrFilesystem fs, final StructrPath parent, final Page ownerDocument, final DOMNode parentNode, final DOMNode node, final String name) {
		super(fs, parent, name);

		this.ownerDocument = ownerDocument;
		this.parentNode    = parentNode;
		this.domNode       = node;

		if (node != null) {
			this.uuid = node.getUuid();
		}
	}

	@Override
	public DirectoryStream<Path> getDirectoryStream(DirectoryStream.Filter<? super Path> filter) {

		// this is called when a directory is opened, so we can safely begin our transaction here
		final Tx tx = StructrApp.getInstance(fs.getSecurityContext()).tx();

		return new DirectoryStream() {

			boolean closed = false;

			@Override
			public Iterator iterator() {

				if (!closed) {

					final StructrPath.HiddenFileEntry hiddenFiles = StructrPath.HIDDEN_PROPERTY_FILES.get(domNode.getUuid());
					final List<StructrPath> nodes                 = new LinkedList<>();
					int pos                                       = 0;

					try {

						for (final DOMNode child : domNode.treeGetChildren()) {

							final int domPosition = getDomPosition(child, pos);

							// store position just in case..
							child.setProperty(DOMNode.domSortPosition, domPosition);

							// add node to return set
							nodes.add(new StructrDOMNodePath(fs, StructrDOMNodePath.this, ownerDocument, domNode, child, getName(null, child, pos)));

							pos += 1;
						}

						final Set<PropertyKey> exportedKeys = new LinkedHashSet<>();

						Iterables.addAll(exportedKeys, domNode.getPropertyKeys(PropertyView.Ui));
						Iterables.addAll(exportedKeys, domNode.getPropertyKeys(PropertyView.Html));

						hidePropertyKeys(hiddenFiles);

						for (final PropertyKey key : exportedKeys) {

							final Object value  = domNode.getProperty(key);
							Object defaultValue = key.defaultValue();

							// boolean properties with default value "false" should not be visible
							if (key instanceof BooleanProperty) {
								defaultValue = Boolean.FALSE;
							}

							// do not export properties that return null or their default value
							if (value != null && !(value.equals(defaultValue))) {

								final StructrPath path = resolveStructrPath(key.jsonName());
								if (path != null) {

									if (hiddenFiles != null) {
										hiddenFiles.remove(key.jsonName());
									}

									nodes.add(path);
								}
							}
						}

					} catch (FrameworkException fex) {
						logger.log(Level.WARNING, "", fex);
					}

					return nodes.iterator();
				}

				return Collections.emptyIterator();
			}

			@Override
			public void close() throws IOException {

				closed = true;

				try {
					tx.success();
					tx.close();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}
		};
	}

	@Override
	public FileChannel newFileChannel(final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void createDirectory(final FileAttribute<?>... attrs) throws IOException {

		// create a new directory with the name of this path
		final App app        = StructrApp.getInstance(fs.getSecurityContext());
		final ElementName en = new ElementName(name);

		if (!en.hasTagName() || !en.hasPosition()) {
			throw new InvalidPathException(name, "New element needs tag and position, e.g. 001-div");
		}

		try (final Tx tx = app.tx()) {

			if (ownerDocument != null) {

				int index = 0;

				if (parentNode != null) {

					// check if parent node already has a child at the given position
					for (final DOMNode child : parentNode.treeGetChildren()) {

						final int childPosition = getDomPosition(child, index++);
						if (childPosition == en.getPosition()) {

							throw new InvalidPathException(name, "A child with position " + childPosition + " already exists.");
						}
					}
				}

				// all clear, create new child
				final DOMNode newChild = createNode(ownerDocument, en.getTagName());
				final int newPosition  = en.getPosition();

				if (parentNode != null) {
					insertDOMNodeAt(newChild, newPosition);
				}

				if (newChild instanceof DOMNode) {

					this.domNode = newChild;

					// Add all properties to the list of "hidden" properties so that copying
					// a directory does not fail with "file already exists". Properties will
					// be "activated" when the user requests a directory listing.
					final StructrPath.HiddenFileEntry entry = new StructrPath.HiddenFileEntry();
					for (final PropertyKey key : domNode.getPropertyKeys(PropertyView.Ui)) {
						entry.add(key.jsonName());
					}

					for (final PropertyKey key : domNode.getPropertyKeys(PropertyView.Html)) {
						entry.add(key.jsonName());
					}

					hidePropertyKeys(entry);

					StructrPath.HIDDEN_PROPERTY_FILES.put(domNode.getUuid(), entry);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public void delete() throws IOException {

		final App app = StructrApp.getInstance(fs.getSecurityContext());
		try (final Tx tx = app.tx()) {

			if (domNode.treeGetChildren().isEmpty()) {

				if (parentNode != null) {
					parentNode.removeChild(domNode);
				}

				app.delete(domNode);
				this.domNode = null;

			} else {

				throw new DirectoryNotEmptyException(name);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public StructrPath resolveStructrPath(final String pathComponent) {

		if (blacklist.contains(pathComponent)) {
			return null;
		}

		if ("name".equals(pathComponent)) {

			// pages don't have a settable name property
			if (domNode instanceof Page) {
				return null;
			}

			// shared components have their names set using the directory name
			if (domNode.getOwnerDocument() instanceof ShadowDocument) {
				return null;
			}
		}

		if (domNode != null) {

			// special handling for data-* attributes because there are no property keys
			// stored in the configuration provider
			if (pathComponent != null && pathComponent.startsWith("data-")) {

				final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(domNode.getClass(), pathComponent);
				if (key != null) {

					return new StructrNodePropertyPath(fs, this, domNode, key);
				}
			}

			final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(domNode.getClass(), pathComponent, false);
			if (key != null) {

				return new StructrNodePropertyPath(fs, this, domNode, key);

			} else {

				final ElementName en = new ElementName(pathComponent);
				final int pos        = en.getPosition();

				if (pos >= 0) {

					int childPosition = 0;
					DOMNode node      = null;

					// iterate over children, find node with given position
					for (final DOMNode child : domNode.treeGetChildren()) {

						final int domSortPosition = getDomPosition(child, childPosition);
						if (pos == domSortPosition) {

							node = child;
							break;
						}

						// advance default child position
						childPosition += 1;
					}

					return new StructrDOMNodePath(fs, StructrDOMNodePath.this, ownerDocument, domNode, node, pathComponent);
				}
			}
		}

		return new StructrDOMNodePath(fs, this, ownerDocument, domNode, null, pathComponent);
	}

	@Override
	public Map<String, Object> getAttributes(final String attributes, final LinkOption... options) throws IOException {

		if (domNode != null) {

			return new StructrDOMAttributes(fs.getSecurityContext(), domNode).toMap(attributes);
		}

		throw new NoSuchFileException(toString());
	}

	@Override
	public <T extends BasicFileAttributes> T getAttributes(Class<T> type, LinkOption... options) throws IOException {

		if (domNode != null) {

			return (T)new StructrDOMAttributes(fs.getSecurityContext(), domNode);
		}

		throw new NoSuchFileException(toString());
	}

	@Override
	public void copy(final Path target, final CopyOption... options) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void move(final Path target, final CopyOption... options) throws IOException {

		if (target instanceof StructrDOMNodePath) {

			final App app = StructrApp.getInstance(fs.getSecurityContext());
			final StructrDOMNodePath targetPath = (StructrDOMNodePath)target;

			try (final Tx tx = app.tx()) {

				if (targetPath.parentNode != null && targetPath.parentNode.getUuid().equals(parentNode.getUuid())) {

					final String targetName             = target.getFileName().toString();
					final ElementName targetElementName = new ElementName(targetName);
					final ElementName sourceElementName = new ElementName(name);

					if (targetElementName.getTagName().equals(sourceElementName.getTagName())) {

						final int newPosition = targetElementName.getPosition();

						// remove child from current position
						parentNode.removeChild(domNode);

						// insert at new position
						insertDOMNodeAt(domNode, newPosition);

					} else {

						throw new InvalidPathException(targetName, "Cannot change element type.");
					}

				} else {


				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.log(Level.WARNING, "Unable to move {0} to {1}: {2}", new Object[] { this, target, fex.getMessage() });
			}

		} else if (target instanceof StructrNonexistingComponentPath & domNode != null) {

			// don't move is source node is not a shared component (allow rename but not move)
			if (!(domNode.getOwnerDocument() instanceof ShadowDocument)) {

				throw new AccessDeniedException("Cannot move DOM node to shared components");

			} else {

				final String targetName = target.getFileName().toString();
				final int pos           = targetName.indexOf("-");
				String componentName    = null;
				String componentTag     = null;

				if (pos == -1) {
					throw new InvalidPathException(targetName, "Component name must contain tag and name, e.g. div-test");
				}

				componentTag  = targetName.substring(0, pos);
				componentName = targetName.substring(pos+1);

				if (componentName.isEmpty() || componentTag.isEmpty()) {
					throw new InvalidPathException(targetName, "Component name must contain tag and name, e.g. div-test");
				}

				final App app = StructrApp.getInstance(fs.getSecurityContext());
				try (final Tx tx = app.tx()) {

					final ShadowDocument doc = StructrApp.getInstance(fs.getSecurityContext()).nodeQuery(ShadowDocument.class).includeDeletedAndHidden().getFirst();
					for (final DOMNode child : doc.getProperty(Page.elements)) {

						if (!child.hasIncomingRelationships(DOMChildren.class)) {

							if (componentName.equals(child.getName()) && componentTag.equals(child.getProperty(DOMElement.tag))) {

								throw new FileAlreadyExistsException(targetName);
							}
						}
					}

					domNode.setProperty(DOMElement.name, componentName);

					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
				}
			}
		}
	}

	@Override
	public void setAttribute(final String attribute, final Object value, final LinkOption... options) throws IOException {
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(final Class<V> type, final LinkOption... options) throws IOException {
		return (V)getAttributes((Class)null, options);
	}

	@Override
	public boolean isSameFile(final Path path2) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void enablePropertyFile(final String name) {

		if (uuid != null) {

			final StructrPath.HiddenFileEntry entry = StructrPath.HIDDEN_PROPERTY_FILES.get(uuid);
			if (entry != null) {

				if (name.startsWith("data-")) {

					entry.addDynamicWithValue(name);

				}

				entry.remove(name);

				if (entry.isEmpty()) {

					StructrPath.HIDDEN_PROPERTY_FILES.remove(uuid);
				}
			}
		}
	}

	@Override
	public boolean hasPropertyFile(final String name) {

		boolean result = true;

		if (uuid != null) {

			// this code looks for the UUID of a DOMNode in the set of "currently
			// created DOMNodes" and returns whether the property with the given
			// name has been accessed yet.

			final StructrPath.HiddenFileEntry entry = StructrPath.HIDDEN_PROPERTY_FILES.get(uuid);
			if (entry != null) {

				if (entry.has(name)) {

					result = false;

				} else if (name.startsWith("data-")) {

					result = entry.hasDynamicWithValue(name);
				}
			}
		}

		// if the node is not in the set of nodes in the process of being created, return true
		return result;
	}

	// ----- private methods -----

	private void insertDOMNodeAt(final DOMNode newChild, int newPosition) throws FrameworkException {

		DOMNode refChild = null;
		int position     = 0;

		// find element with the lowest dom position greater than the new element's pos
		for (final DOMNode child : parentNode.treeGetChildren()) {

			final int domPosition = getDomPosition(child, position++);
			if (domPosition > newPosition && refChild == null) {

				refChild = child;
			}

			// store position in child
			if (child.getProperty(DOMNode.domSortPosition) == null) {
				child.setProperty(DOMNode.domSortPosition, domPosition);
			}
		}

		if (refChild != null) {

			// position was found, insert before
			parentNode.insertBefore(newChild, refChild);

		} else {

			// new position is larger than any existing position
			// or parent has no children yet => append new child
			parentNode.appendChild(newChild);
		}

		// store position in node
		newChild.setProperty(DOMNode.domSortPosition, newPosition);
	}

	private DOMNode createNode(final Document doc, final String tagName) {

		switch (tagName) {

			case "content":
				return (DOMNode)doc.createTextNode("#text");

			case "comment":
				return (DOMNode)doc.createComment("#comment");

			case "template":

				Template newNode = null;

				try {

					newNode = StructrApp.getInstance().create(Template.class,
						new NodeAttribute<>(Template.parent, (Page)doc),
						new NodeAttribute<>(Template.ownerDocument, (Page)doc),
						new NodeAttribute<>(Template.content, "#template")
					);

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "Unable to create new template node", fex);
				}

				return newNode;
		}

		return (DOMNode)doc.createElement(tagName);

	}

	private void hidePropertyKeys(final StructrPath.HiddenFileEntry hiddenKeys) {

		if (hiddenKeys != null) {

			hiddenKeys.add(DOMNode.visibleToAuthenticatedUsers.jsonName());
			hiddenKeys.add(DOMNode.visibleToPublicUsers.jsonName());
			hiddenKeys.add(DOMNode.sharedComponent.jsonName());
			hiddenKeys.add(DOMNode.renderDetails.jsonName());
			hiddenKeys.add(Indexable.contentType.jsonName());
		}
	}

	// ----- nested classes -----
	public static class ElementName {

		private int position = -1;
		private String src   = null;
		private String tag   = null;

		public ElementName(final String src) {

			this.src = src;

			parsePositionAndName(src);
		}

		@Override
		public String toString() {

			final StringBuilder buf = new StringBuilder();

			buf.append(src);
			buf.append(" = ElementName(");
			buf.append(tag);
			buf.append(", ");
			buf.append(position);

			buf.append(")");

			return buf.toString();
		}

		public int getPosition() {
			return position;
		}

		public boolean hasPosition() {
			return position >= 0;
		}

		public String getTagName() {
			return tag;
		}

		public boolean hasTagName() {
			return tag != null;
		}

		private void parsePositionAndName(final String src) {

			// split on "-" first
			final int pos = src.indexOf("-");
			if (pos >= 0) {

				final String positionSrc = src.substring(0, pos);
				final String nameSrc     = src.substring(pos+1);

				// no position, try to parse the rest from first part
				this.position = getPosition(positionSrc);
				if (this.position >= 0) {

					this.tag = nameSrc;

				} else {

					logger.log(Level.WARNING, "Unable to extract position from {0}: invalid source name", src);
				}

			} else {

				// fallback: no position
				this.tag = src;
			}
		}

		private int getPosition(final String src) {

			try {

				return Integer.valueOf(src);

			} catch (Throwable t) {}

			return -1;
		}
	}
}
