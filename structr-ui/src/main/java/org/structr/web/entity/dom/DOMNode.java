/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Predicate;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.parser.Functions;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionIdProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.Renderable;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.dom.relationship.DOMSiblings;
import org.structr.web.entity.relation.PageLink;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 *
 * @author Christian Morgner
 */
public abstract class DOMNode extends LinkedTreeNode<DOMChildren, DOMSiblings, DOMNode> implements Node, Renderable, DOMAdoptable, DOMImportable, Syncable {

	private static final Logger logger = Logger.getLogger(DOMNode.class.getName());

	// ----- error messages for DOMExceptions -----
	protected static final String NO_MODIFICATION_ALLOWED_MESSAGE = "Permission denied.";
	protected static final String INVALID_ACCESS_ERR_MESSAGE = "Permission denied.";
	protected static final String INDEX_SIZE_ERR_MESSAGE = "Index out of range.";
	protected static final String CANNOT_SPLIT_TEXT_WITHOUT_PARENT = "Cannot split text element without parent and/or owner document.";
	protected static final String WRONG_DOCUMENT_ERR_MESSAGE = "Node does not belong to this document.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE = "A node cannot accept itself as a child.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR = "A node cannot accept its own ancestor as child.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT = "A document may only have one html element.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT = "A document may only accept an html element as its document element.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE = "Node type not supported.";
	protected static final String NOT_FOUND_ERR_MESSAGE = "Node is not a child.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC = "Document nodes cannot be imported into another document.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC = "Document nodes cannot be adopted by another document.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_RENAME = "Renaming of nodes is not supported by this implementation.";

	public static final Property<Boolean> hideOnIndex = new BooleanProperty("hideOnIndex").indexed();
	public static final Property<Boolean> hideOnDetail = new BooleanProperty("hideOnDetail").indexed();
	public static final Property<String> showForLocales = new StringProperty("showForLocales").indexed();
	public static final Property<String> hideForLocales = new StringProperty("hideForLocales").indexed();

	public static final Property<String> showConditions = new StringProperty("showConditions").indexed();
	public static final Property<String> hideConditions = new StringProperty("hideConditions").indexed();

	public static final Property<List<DOMNode>> children = new EndNodes<>("children", DOMChildren.class);
	public static final Property<DOMNode> parent = new StartNode<>("parent", DOMChildren.class);
	public static final Property<DOMNode> previousSibling = new StartNode<>("previousSibling", DOMSiblings.class);
	public static final Property<DOMNode> nextSibling = new EndNode<>("nextSibling", DOMSiblings.class);

	public static final Property<List<String>> childrenIds = new CollectionIdProperty("childrenIds", children);
	public static final Property<String> nextSiblingId = new EntityIdProperty("nextSiblingId", nextSibling);

	public static final Property<String> parentId = new EntityIdProperty("parentId", parent);

	public static final Property<Page> ownerDocument = new EndNode<>("ownerDocument", PageLink.class);
	public static final Property<String> pageId = new EntityIdProperty("pageId", ownerDocument);

	public static final Property<String> dataStructrIdProperty = new StringProperty("data-structr-id");
	public static final Property<String> dataHashProperty = new StringProperty("data-hash");

	static {

		// extend set of builtin functions
		Functions.functions.put("GET", new Function<Object, Object>() {

			@Override
			public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

				if (sources != null && sources.length > 0) {

					try {

						String address = sources[0].toString();
						String contentType = null;

						if (sources.length > 1) {
							contentType = sources[1].toString();
						}

						//long t0 = System.currentTimeMillis();
						if ("text/html".equals(contentType)) {

							String selector = null;

							if (sources.length > 2) {

								selector = sources[2].toString();

//								String raw = getFromUrl2(address);
//								long t1 = System.currentTimeMillis();
//								Jerry doc = jerry(raw);
//								String html = doc.$(selector).html();
//								logger.log(Level.INFO, "Jerry took {0} ms to get and {1} ms to parse page.", new Object[]{t1 - t0, System.currentTimeMillis() - t1});
								String html = Jsoup.parse(new URL(address), 5000).select(selector).html();
								return html;

							} else {

								String html = Jsoup.parse(new URL(address), 5000).html();
								//logger.log(Level.INFO, "Jsoup took {0} ms to get and parse page.", (System.currentTimeMillis() - t0));

								return html;

							}

						} else {

							return getFromUrl(address);
						}

					} catch (Throwable t) {
					}

					return "";
				}

				return usage();
			}

			@Override
			public String usage() {
				return "ERROR! Usage: ${GET(URL[, contentType[, selector]])}. Example: ${GET('http://structr.org', 'text/html')}";
			}
		});
	}

	public abstract boolean isSynced();
	public abstract boolean contentEquals(final DOMNode otherNode);
	public abstract void updateFromNode(final DOMNode otherNode) throws FrameworkException;

	public String getIdHash() {

		final String uuid = getUuid();

		return Integer.toHexString(uuid.hashCode());
	}

	public String getIdHashOrProperty() {

		String idHash = getProperty(DOMNode.dataHashProperty);
		if (idHash == null) {

			idHash = getIdHash();
		}

		return idHash;
	}

	/**
	 * This method will be called by the DOM logic when this node gets a new
	 * child. Override this method if you need to set properties on the
	 * child depending on its type etc.
	 *
	 * @param newChild
	 */
	protected void handleNewChild(Node newChild) {
		// override me
	}

	@Override
	public Class<DOMChildren> getChildLinkType() {
		return DOMChildren.class;
	}

	@Override
	public Class<DOMSiblings> getSiblingLinkType() {
		return DOMSiblings.class;
	}

	// ----- public methods -----
	@Override
	public String toString() {

		return getClass().getSimpleName() + " [" + getUuid() + "] (" + getTextContent() + ", " + treeGetChildPosition(this) + ")";
	}

	public List<DOMChildren> getChildRelationships() {
		return treeGetChildRelationships();
	}

	public String getPositionPath() {

		String path = "";

		DOMNode currentNode = this;
		while (currentNode.getParentNode() != null) {

			DOMNode parentNode = (DOMNode) currentNode.getParentNode();

			path = "/" + parentNode.treeGetChildPosition(currentNode) + path;

			currentNode = parentNode;

		}

		return path;

	}

	/**
	 * Get all ancestors of this node
	 *
	 * @return list of ancestors
	 */
	public List<Node> getAncestors() {

		List<Node> ancestors = new LinkedList();

		Node _parent = getParentNode();
		while (_parent != null) {

			ancestors.add(_parent);
			_parent = _parent.getParentNode();
		}

		return ancestors;

	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		try {

			increasePageVersion();

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Updating page version failed", ex);

		}

		return true;

	}

	// ----- private methods -----
	/**
	 * Do necessary updates on all containing pages
	 *
	 * @throws FrameworkException
	 */
	private void increasePageVersion() throws FrameworkException {

		Page page = (Page) getOwnerDocument();

		if (page != null) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();

		}

	}

	// ----- protected methods -----
	protected void checkIsChild(Node otherNode) throws DOMException {

		if (otherNode instanceof DOMNode) {

			Node _parent = otherNode.getParentNode();

			if (!isSameNode(_parent)) {

				throw new DOMException(DOMException.NOT_FOUND_ERR, NOT_FOUND_ERR_MESSAGE);
			}

			// validation successful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	protected void checkHierarchy(Node otherNode) throws DOMException {

		// we can only check DOMNodes
		if (otherNode instanceof DOMNode) {

			// verify that the other node is not this node
			if (isSameNode(otherNode)) {
				throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE);
			}

			// verify that otherNode is not one of the
			// the ancestors of this node
			// (prevent circular relationships)
			Node _parent = getParentNode();
			while (_parent != null) {

				if (_parent.isSameNode(otherNode)) {
					throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR);
				}

				_parent = _parent.getParentNode();
			}

			// TODO: check hierarchy constraints imposed by the schema
			// validation sucessful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	protected void checkSameDocument(Node otherNode) throws DOMException {

		Document doc = getOwnerDocument();

		if (doc != null) {

			Document otherDoc = otherNode.getOwnerDocument();

			// Shadow doc is neutral
			if (otherDoc != null && !doc.equals(otherDoc) && !(doc instanceof ShadowDocument)) {

				throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, WRONG_DOCUMENT_ERR_MESSAGE);
			}

			if (otherDoc == null) {

				((DOMNode) otherNode).doAdopt((Page) doc);

			}
		}
	}

	protected void checkWriteAccess() throws DOMException {

		if (!securityContext.isAllowed(this, Permission.write)) {

			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, NO_MODIFICATION_ALLOWED_MESSAGE);
		}
	}

	protected void checkReadAccess() throws DOMException {

		if (securityContext.isVisible(this) || securityContext.isAllowed(this, Permission.read)) {
			return;
		}

		throw new DOMException(DOMException.INVALID_ACCESS_ERR, INVALID_ACCESS_ERR_MESSAGE);
	}

	protected String indent(final int depth) {

		StringBuilder indent = new StringBuilder("\n");

		for (int d = 0; d < depth; d++) {

			indent.append("  ");

		}

		return indent.toString();
	}

	/**
	 * Decide whether this node should be displayed for the given conditions
	 * string.
	 *
	 * @param securityContext
	 * @param renderContext
	 * @return
	 */
	protected boolean displayForConditions(final SecurityContext securityContext, final RenderContext renderContext) {

		// In raw mode, render everything
		if (EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false)))) {
			return true;
		}

		String _showConditions = getProperty(DOMNode.showConditions);
		String _hideConditions = getProperty(DOMNode.hideConditions);

		// If both fields are empty, render node
		if (StringUtils.isBlank(_hideConditions) && StringUtils.isBlank(_showConditions)) {
			return true;
		}
		try {
			// If hide conditions evaluate to "true", don't render
			if (StringUtils.isNotBlank(_hideConditions) && Boolean.TRUE.equals(Functions.evaluate(securityContext, renderContext, this, _hideConditions))) {
				return false;
			}

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Hide conditions " + _hideConditions + " could not be evaluated.", ex);
		}
		try {
			// If show conditions don't evaluate to "true", don't render
			if (StringUtils.isNotBlank(_showConditions) && !(Boolean.TRUE.equals(Functions.evaluate(securityContext, renderContext, this, _showConditions)))) {
				return false;
			}

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Show conditions " + _showConditions + " could not be evaluated.", ex);
		}

		return true;

	}

	/**
	 * Decide whether this node should be displayed for the given locale
	 * settings.
	 *
	 * @param renderContext
	 * @return
	 */
	protected boolean displayForLocale(final RenderContext renderContext) {

		// In raw mode, render everything
		if (EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false)))) {
			return true;
		}

		String localeString = renderContext.getLocale().toString();

		String show = getProperty(DOMNode.showForLocales);
		String hide = getProperty(DOMNode.hideForLocales);

		// If both fields are empty, render node
		if (StringUtils.isBlank(hide) && StringUtils.isBlank(show)) {
			return true;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.contains(hide, localeString)) {
			return false;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.isNotBlank(show) && !StringUtils.contains(show, localeString)) {
			return false;
		}

		return true;

	}

	protected String escapeForHtml(final String raw) {

		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});

	}

	protected String escapeForHtmlAttributes(final String raw) {

		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">", "\"", "'"}, new String[]{"&amp;", "&lt;", "&gt;", "&quot;", "&#39;"});

	}

	protected void collectNodesByPredicate(Node startNode, DOMNodeList results, Predicate<Node> predicate, int depth, boolean stopOnFirstHit) {

		if (predicate.evaluate(securityContext, startNode)) {

			results.add(startNode);

			if (stopOnFirstHit) {

				return;
			}
		}

		NodeList _children = startNode.getChildNodes();
		if (_children != null) {

			int len = _children.getLength();
			for (int i = 0; i < len; i++) {

				Node child = _children.item(i);

				collectNodesByPredicate(child, results, predicate, depth + 1, stopOnFirstHit);
			}
		}
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getTextContent() throws DOMException {

		final DOMNodeList results = new DOMNodeList();
		final TextCollector textCollector = new TextCollector();

		collectNodesByPredicate(this, results, textCollector, 0, false);

		return textCollector.getText();
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		// TODO: implement?
	}

	@Override
	public Node getParentNode() {
		// FIXME: type cast correct here?
		return (Node) getProperty(parent);
	}

	@Override
	public NodeList getChildNodes() {
		checkReadAccess();
		return new DOMNodeList(treeGetChildren());
	}

	@Override
	public Node getFirstChild() {
		checkReadAccess();
		return treeGetFirstChild();
	}

	@Override
	public Node getLastChild() {
		return treeGetLastChild();
	}

	@Override
	public Node getPreviousSibling() {
		return listGetPrevious(this);
	}

	@Override
	public Node getNextSibling() {
		return listGetNext(this);
	}

	@Override
	public Document getOwnerDocument() {
		return getProperty(ownerDocument);
	}

	@Override
	public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {

		// according to DOM spec, insertBefore with null refChild equals appendChild
		if (refChild == null) {

			return appendChild(newChild);
		}

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(refChild);

		checkHierarchy(newChild);
		checkHierarchy(refChild);

		if (newChild instanceof DocumentFragment) {

			// When inserting document fragments, we must take
			// care of the special case that the nodes already
			// have a NEXT_LIST_ENTRY relationship coming from
			// the document fragment, so we must first remove
			// the node from the document fragment and then
			// add it to the new parent.
			DocumentFragment fragment = (DocumentFragment)newChild;
			Node currentChild = fragment.getFirstChild();

			while (currentChild != null) {

				// save next child in fragment list for later use
				Node savedNextChild = currentChild.getNextSibling();

				// remove child from document fragment
				fragment.removeChild(currentChild);

				// insert child into new parent
				insertBefore(currentChild, refChild);

				// next
				currentChild = savedNextChild;
			}

		} else {

			Node _parent = newChild.getParentNode();
			if (_parent != null) {

				_parent.removeChild(newChild);
			}

			try {

				// do actual tree insertion here
				treeInsertBefore((DOMNode)newChild, (DOMNode)refChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			handleNewChild(newChild);
		}

		return refChild;
	}

	@Override
	public Node replaceChild(final Node newChild, final Node oldChild) throws DOMException {

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(oldChild);

		checkHierarchy(newChild);
		checkHierarchy(oldChild);

		if (newChild instanceof DocumentFragment) {

			// When inserting document fragments, we must take
			// care of the special case that the nodes already
			// have a NEXT_LIST_ENTRY relationship coming from
			// the document fragment, so we must first remove
			// the node from the document fragment and then
			// add it to the new parent.
			// replace indirectly using insertBefore and remove
			DocumentFragment fragment = (DocumentFragment)newChild;
			Node currentChild = fragment.getFirstChild();

			while (currentChild != null) {

				// save next child in fragment list for later use
				Node savedNextChild = currentChild.getNextSibling();

				// remove child from document fragment
				fragment.removeChild(currentChild);

				// add child to new parent
				insertBefore(currentChild, oldChild);

				// next
				currentChild = savedNextChild;
			}

			// finally, remove reference element
			removeChild(oldChild);

		} else {

			Node _parent = newChild.getParentNode();
			if (_parent != null && _parent instanceof DOMNode) {

				_parent.removeChild(newChild);
			}

			try {
				// replace directly
				treeReplaceChild((DOMNode)newChild, (DOMNode)oldChild);

			} catch (FrameworkException frex) {

				if (frex.getStatus() == 404) {

					throw new DOMException(DOMException.NOT_FOUND_ERR, frex.getMessage());

				} else {

					throw new DOMException(DOMException.INVALID_STATE_ERR, frex.getMessage());
				}
			}

			// allow parent to set properties in new child
			handleNewChild(newChild);
		}

		return oldChild;
	}

	@Override
	public Node removeChild(final Node node) throws DOMException {

		checkWriteAccess();
		checkSameDocument(node);
		checkIsChild(node);

		try {

			treeRemoveChild((DOMNode) node);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return node;
	}

	@Override
	public Node appendChild(final Node newChild) throws DOMException {

		checkWriteAccess();
		checkSameDocument(newChild);
		checkHierarchy(newChild);

		try {

			if (newChild instanceof DocumentFragment) {

				// When inserting document fragments, we must take
				// care of the special case that the nodes already
				// have a NEXT_LIST_ENTRY relationship coming from
				// the document fragment, so we must first remove
				// the node from the document fragment and then
				// add it to the new parent.
				// replace indirectly using insertBefore and remove
				DocumentFragment fragment = (DocumentFragment) newChild;
				Node currentChild = fragment.getFirstChild();

				while (currentChild != null) {

					// save next child in fragment list for later use
					Node savedNextChild = currentChild.getNextSibling();

					// remove child from document fragment
					fragment.removeChild(currentChild);

					// append child to new parent
					appendChild(currentChild);

					// next
					currentChild = savedNextChild;
				}

			} else {

				Node _parent = newChild.getParentNode();

				if (_parent != null && _parent instanceof DOMNode) {
					_parent.removeChild(newChild);
				}

				treeAppendChild((DOMNode) newChild);

				// allow parent to set properties in new child
				handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return newChild;
	}

	@Override
	public boolean hasChildNodes() {
		return !getProperty(children).isEmpty();
	}

	@Override
	public Node cloneNode(boolean deep) {

		if (deep) {

			throw new UnsupportedOperationException("cloneNode with deep=true is not supported yet.");

		} else {

			final PropertyMap properties = new PropertyMap();

			for (Iterator<PropertyKey> it = getPropertyKeys(uiView.name()).iterator(); it.hasNext();) {

				PropertyKey key = it.next();

				// omit system properties (except type), parent/children and page relationships
				if (key.equals(GraphObject.type) || (!key.isUnvalidated()
					&& !key.equals(GraphObject.id)
					&& !key.equals(DOMNode.ownerDocument) && !key.equals(DOMNode.pageId)
					&& !key.equals(DOMNode.parent) && !key.equals(DOMNode.parentId)
					&& !key.equals(DOMElement.syncedNodes)
					&& !key.equals(DOMNode.children) && !key.equals(DOMNode.childrenIds))) {

					properties.put(key, getProperty(key));
				}
			}

			final App app = StructrApp.getInstance(securityContext);

			try {
				DOMNode node = app.create(getClass(), properties);

				return node;

			} catch (FrameworkException ex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, ex.toString());

			}

		}
	}

	@Override
	public boolean isSupported(String string, String string1) {
		return false;
	}

	@Override
	public String getNamespaceURI() {
		return null; //return "http://www.w3.org/1999/xhtml";
	}

	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public short compareDocumentPosition(Node node) throws DOMException {
		return 0;
	}

	@Override
	public boolean isSameNode(Node node) {

		if (node != null && node instanceof DOMNode) {

			String otherId = ((DOMNode) node).getProperty(GraphObject.id);
			String ourId = getProperty(GraphObject.id);

			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String lookupPrefix(String string) {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		return true;
	}

	@Override
	public String lookupNamespaceURI(String string) {
		return null;
	}

	@Override
	public boolean isEqualNode(Node node) {
		return equals(node);
	}

	@Override
	public Object getFeature(String string, String string1) {
		return null;
	}

	@Override
	public Object setUserData(String string, Object o, UserDataHandler udh) {
		return null;
	}

	@Override
	public Object getUserData(String string) {
		return null;
	}

	@Override
	public final void normalize() {

		Document document = getOwnerDocument();
		if (document != null) {

			// merge adjacent text nodes until there is only one left
			Node child = getFirstChild();
			while (child != null) {

				if (child instanceof Text) {

					Node next = child.getNextSibling();
					if (next != null && next instanceof Text) {

						String text1 = child.getNodeValue();
						String text2 = next.getNodeValue();

						// create new text node
						Text newText = document.createTextNode(text1.concat(text2));

						removeChild(child);
						insertBefore(newText, next);
						removeChild(next);

						child = newText;

					} else {

						// advance to next node
						child = next;
					}

				} else {

					// advance to next node
					child = child.getNextSibling();

				}
			}

			// recursively normalize child nodes
			if (hasChildNodes()) {

				Node currentChild = getFirstChild();
				while (currentChild != null) {

					currentChild.normalize();
					currentChild = currentChild.getNextSibling();
				}
			}
		}

	}

	// ----- interface DOMAdoptable -----
	@Override
	public Node doAdopt(final Page _page) throws DOMException {

		if (_page != null) {

			final App app = StructrApp.getInstance(securityContext);

			try {
				setProperty(ownerDocument, _page);

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

			}
		}

		return this;
	}

	@Override
	public boolean flush() {
		return false;
	}

	// ----- static methods -----
	private static String getFromUrl(final String requestUrl) throws IOException {

		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(requestUrl);

		get.setHeader("Connection", "close");

		return IOUtils.toString(client.execute(get).getEntity().getContent(), "UTF-8");

	}

	public static Set<DOMNode> getAllChildNodes(final DOMNode node) {

		Set<DOMNode> allChildNodes = new HashSet();

		Node n = node.getFirstChild();

		while (n != null) {

			if (n instanceof DOMNode) {

				DOMNode domNode = (DOMNode) n;

				allChildNodes.add(domNode);
				allChildNodes.addAll(getAllChildNodes(domNode));

			}

			n = n.getNextSibling();

		}

		return allChildNodes;
	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {

		final List<Syncable> data = new LinkedList<>();

		// nodes
		data.addAll(getProperty(DOMNode.children));

		final DOMNode sibling = getProperty(DOMNode.nextSibling);
		if (sibling != null) {

			data.add(sibling);
		}

		// relationships
		for (final DOMChildren child : getOutgoingRelationships(DOMChildren.class)) {
			data.add(child);
		}

		final DOMSiblings siblingRel = getOutgoingRelationship(DOMSiblings.class);
		if (siblingRel != null) {

			data.add(siblingRel);
		}

		return data;
	}

	@Override
	public boolean isNode() {
		return true;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return null;
	}

	// ----- nested classes -----
	protected static class TextCollector implements Predicate<Node> {

		private StringBuilder textBuffer = new StringBuilder(200);

		@Override
		public boolean evaluate(SecurityContext securityContext, Node... obj) {

			if (obj[0] instanceof Text) {
				textBuffer.append(((Text) obj[0]).getTextContent());
			}

			return false;
		}

		public String getText() {
			return textBuffer.toString();
		}
	}

	protected static class TagPredicate implements Predicate<Node> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean evaluate(SecurityContext securityContext, Node... obj) {

			if (obj[0] instanceof DOMElement) {

				DOMElement elem = (DOMElement) obj[0];

				if (tagName.equals(elem.getProperty(DOMElement.tag))) {
					return true;
				}
			}

			return false;
		}
	}
}
