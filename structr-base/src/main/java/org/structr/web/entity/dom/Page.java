/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.*;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.relationship.DOMNodePAGEPage;
import org.structr.web.entity.dom.relationship.PageHAS_PATHPagePath;
import org.structr.web.entity.dom.relationship.SiteCONTAINSPage;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.path.PagePath;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;
import org.w3c.dom.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a page resource.
 */
public class Page extends DOMNode implements Linkable, Document, DOMImplementation {

	static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("Page");

		type.setExtends(Page.class);
	}

	public static final Set<String> nonBodyTags = new HashSet<>(Arrays.asList(new String[] { "html", "head", "body", "meta", "link" } ));

	public static final Property<Iterable<DOMNode>> elementsProperty = new StartNodes<>("elements", DOMNodePAGEPage.class).category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<PagePath>> pathsProperty   = new EndNodes<>("paths", PageHAS_PATHPagePath.class).partOfBuiltInSchema();
	public static final Property<Iterable<Site>> sitesProperty       = new StartNodes<>("sites", SiteCONTAINSPage.class).partOfBuiltInSchema();

	public static final Property<Boolean> isPageProperty             = new ConstantBooleanProperty("isPage", true).partOfBuiltInSchema();
	public static final Property<Boolean> pageCreatesRawDataProperty = new BooleanProperty("pageCreatesRawData").defaultValue(false).partOfBuiltInSchema();

	public static final Property<Integer> versionProperty         = new IntProperty("version").indexed().readOnly().defaultValue(0).partOfBuiltInSchema();
	public static final Property<Integer> positionProperty        = new IntProperty("position").indexed().partOfBuiltInSchema();
	public static final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds").partOfBuiltInSchema();

	public static final Property<String> pathProperty             = new StringProperty("path").indexed().partOfBuiltInSchema();
	public static final Property<String> showOnErrorCodesProperty = new StringProperty("showOnErrorCodes").indexed().partOfBuiltInSchema();
	public static final Property<String> contentTypeProperty      = new StringProperty("contentType").indexed().partOfBuiltInSchema();
	public static final Property<String> categoryProperty         = new StringProperty("category").indexed().partOfBuiltInSchema();

	public static final View defaultView = new View(Page.class, PropertyView.Public,
		linkingElementsProperty, enableBasicAuthProperty, basicAuthRealmProperty, dontCacheProperty, childrenProperty, name, owner, sitesProperty,
		isPageProperty, pageCreatesRawDataProperty, versionProperty, positionProperty, cacheForSecondsProperty, pathProperty,
		showOnErrorCodesProperty, contentTypeProperty, categoryProperty, pathsProperty
	);

	public static final View uiView = new View(Page.class, PropertyView.Ui,
		isPageProperty, pageCreatesRawDataProperty, dontCacheProperty, childrenProperty, sitesProperty, versionProperty, positionProperty, cacheForSecondsProperty,
		pathProperty, showOnErrorCodesProperty, contentTypeProperty, categoryProperty, pathsProperty
	);

	public static final View categoryView = new View(Page.class, "category",
		categoryProperty
	);

	public void setVersion(final int version) throws FrameworkException {
		setProperty(versionProperty, version);
	}

	public int getVersion() {
		return getProperty(versionProperty);
	}

	public Integer getCacheForSeconds() {
		return getProperty(cacheForSecondsProperty);
	}

	public String getPath() {
		return getProperty(pathProperty);
	}

	public Iterable<DOMNode> getElements() {
		return getProperty(elementsProperty);
	}

	public Iterable<PagePath> getPaths() {
		return getProperty(pathsProperty);
	}

	public Iterable<Site> getSites() {
		return getProperty(sitesProperty);
	}

	@Override
	public String getContextName() {
		return getProperty(name);
	}

	@Override
	public String getNodeName() {
		return "#document";
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeValue() {
		return null;
	}

	@Override
	public void setNodeValue(final String value) {
	}

	@Override
	public short getNodeType() {
		return DOCUMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	public void updateFromNode(final DOMNode otherNode) throws FrameworkException {
	}

	@Override
	public boolean isSynced() {
		return false;
	}

	@Override
	public boolean contentEquals(final Node node) {
		return false;
	}

	@Override
	public void normalizeDocument() {
		normalize();
	}

	@Override
	public DocumentType getDoctype() {
		return new Html5DocumentType(this);
	}

	@Override
	public DOMImplementation getImplementation() {
		return this;
	}

	@Override
	public boolean hasFeature(String feature, String version) {
		return false;
	}

	@Override
	public Node importNode(Node importedNode, boolean deep) throws DOMException {
		return importNode(importedNode, deep, true);
	}

	@Override
	public String getInputEncoding() {
		return null;
	}

	@Override
	public String getXmlEncoding() {
		return "utf-8";
	}

	@Override
	public boolean getXmlStandalone() {
		return true;
	}

	@Override
	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
	}

	@Override
	public String getXmlVersion() {
		return "1.0";
	}

	@Override
	public void setXmlVersion(String xmlVersion) throws DOMException {
	}

	@Override
	public boolean getStrictErrorChecking() {
		return true;
	}

	@Override
	public void setStrictErrorChecking(boolean strictErrorChecking) {

	}

	@Override
	public String getDocumentURI() {
		return null;
	}

	@Override
	public void setDocumentURI(String documentURI) {
	}

	@Override
	public Node adoptNode(Node source) throws DOMException {
		return adoptNode(source, true);
	}

	@Override
	public DOMConfiguration getDomConfig() {
		return null;
	}

	@Override
	public void renderContent(RenderContext renderContext, int depth) throws FrameworkException {
	}

	public void setContent(final Map<String, Object> parameters, final SecurityContext ctx) throws FrameworkException {

		final String content = (String)parameters.get("content");
		if (content == null) {

			throw new FrameworkException(422, "Cannot set content of page " + this.getName() + ", no content provided");
		}

		final Importer importer = new Importer(this.getSecurityContext(), content, null, null, false, false, false, false);
		final App app           = StructrApp.getInstance(ctx);

		importer.setIsDeployment(true);
		importer.setCommentHandler(new DeploymentCommentHandler());

		if (importer.parse(false)) {

			for (final DOMNode node : this.getAllChildNodes()) {
				app.delete(node);
			}

			importer.createChildNodesWithHtml(this, this, true);
		}
	}

	/**
	 * Creates a new Page entity with the given name in the database.
	 *
	 * @param securityContext the security context to use
	 * @param name the name of the new ownerDocument, defaults to
	 * "ownerDocument" if not set
	 *
	 * @return the new ownerDocument
	 * @throws FrameworkException
	 */
	public static Page createNewPage(final SecurityContext securityContext, final String name) throws FrameworkException {
		return createNewPage(securityContext, null, name);
	}

	/**
	 * Creates a new Page entity with the given name in the database.
	 *
	 * @param securityContext the security context to use
	 * @param uuid the UUID of the new page or null
	 * @param name the name of the new page, defaults to "page"
	 * "ownerDocument" if not set
	 *
	 * @return the new ownerDocument
	 * @throws FrameworkException
	 */
	public static Page createNewPage(final SecurityContext securityContext, final String uuid, final String name) throws FrameworkException {

		final PropertyKey<String> contentTypeKey      = StructrApp.key(Page.class, "contentType");
		final PropertyKey<Boolean> hideOnDetailKey    = StructrApp.key(Page.class, "hideOnDetail");
		final PropertyKey<Boolean> hideOnIndexKey     = StructrApp.key(Page.class, "hideOnIndex");
		final PropertyKey<Boolean> enableBasicAuthKey = StructrApp.key(Page.class, "enableBasicAuth");
		final App app                                 = StructrApp.getInstance(securityContext);
		final PropertyMap properties                  = new PropertyMap();

		// set default values for properties on creation to avoid them
		// being set separately when indexing later
		properties.put(AbstractNode.name, name != null ? name : "page");
		properties.put(AbstractNode.type, Page.class.getSimpleName());
		properties.put(contentTypeKey,     "text/html");
		properties.put(hideOnDetailKey,    false);
		properties.put(hideOnIndexKey,     false);
		properties.put(enableBasicAuthKey, false);

		if (uuid != null) {
			properties.put(Page.id, uuid);
		}

		return app.create(Page.class, properties);
	}

	/**
	 * Creates a default simple page for the Structr backend "add page" button.
	 *
	 * @param securityContext
	 * @param name
	 * @return
	 * @throws FrameworkException
	 */
	public static Page createSimplePage(final SecurityContext securityContext, final String name) throws FrameworkException {

		final Page page = Page.createNewPage(securityContext, name);
		if (page != null) {

			Element html  = page.createElement("html");
			Element head  = page.createElement("head");
			Element body  = page.createElement("body");
			Element title = page.createElement("title");
			Element h1    = page.createElement("h1");
			Element div   = page.createElement("div");

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);

				// add H1 element to BODY
				body.appendChild(h1);

				// add DIV element to BODY
				body.appendChild(div);

				// add text nodes
				title.appendChild(page.createTextNode("${capitalize(page.name)}"));
				h1.appendChild(page.createTextNode("${capitalize(page.name)}"));
				div.appendChild(page.createTextNode("Initial body text"));

			} catch (DOMException dex) {

				final Logger logger = LoggerFactory.getLogger(Page.class);
				logger.warn("", dex);

				throw new FrameworkException(422, dex.getMessage());
			}
		}

		return page;
	}

	public String getContent(final Page page, final RenderContext.EditMode editMode) throws FrameworkException {

		DOMNode.prefetchDOMNodes(page.getUuid());

		final RenderContext ctx = new RenderContext(page.getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();
		ctx.setBuffer(buffer);
		page.render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	public Element createElement (final String tag, final boolean suppressException) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Logger logger                = LoggerFactory.getLogger(Page.class);
		final App app                      = StructrApp.getInstance(getSecurityContext());
		String elementType                 = StringUtils.capitalize(tag);

		// Avoid creating an (invalid) 'Content' DOMElement
		if (elementType == null || "Content".equals(elementType)) {

			logger.warn("Blocked attempt to create a DOMElement of type Content");

			return null;

		}

		// Template is already taken => we need to modify the type :(
		if ("Template".equals(elementType)) {
			elementType = "TemplateElement";
		}

		Class entityClass = config.getNodeEntityClass(elementType);
		if (entityClass == null) {

			// No HTML type element found so lets try the dynamic DOMElement class
			entityClass = config.getNodeEntityClass("DOMElement");
		}

		try {

			final DOMElement element = (DOMElement) app.create(entityClass,
				new NodeAttribute(StructrApp.key(entityClass, "tag"),          tag),
				new NodeAttribute(StructrApp.key(entityClass, "hideOnDetail"), false),
				new NodeAttribute(StructrApp.key(entityClass, "hideOnIndex"),  false)
			);

			element.doAdopt(this);

			return element;

		} catch (Throwable t) {

			if (!suppressException) {

				logger.error("Unable to instantiate element of type " + elementType, t);
			}
		}

		return null;
	}

	@Override
	public NodeList getElementsByTagNameNS(final String tagName, final String namespaceURI) {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(this.getSecurityContext(), this, results, new Predicate<Node>() {

			@Override
			public boolean accept(Node obj) {

				if (obj instanceof DOMElement) {

					DOMElement elem = (DOMElement) obj;

					if (tagName.equals(elem.getTag())) {
						return true;
					}
				}

				return false;
			}

		}, 0, false);

		return results;
	}

	@Override
	public void render(final RenderContext renderContext, final int depth) throws FrameworkException {

		renderContext.setPage(this);

		// Skip DOCTYPE node
		DOMNode subNode = (DOMNode) this.getFirstChild().getNextSibling();

		if (subNode == null) {

			this.checkReadAccess();
			subNode = (DOMNode) this.treeGetFirstChild();


		} else {

			renderContext.getBuffer().append("<!DOCTYPE html>\n");

		}

		while (subNode != null) {

			if (renderContext.getSecurityContext().isVisible(subNode)) {

				subNode.render(renderContext, depth);
			}

			subNode = (DOMNode) subNode.getNextSibling();

		}

	}

	public Node doAdopt(final Page page) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC);
	}

	public Node doImport(final Page newPage) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC);
	}

	public Node renameNode(final Node node, final String string, final String string1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_RENAME);
	}

	public DocumentType createDocumentType(final String string, final String string1, final String string2) throws DOMException {
		throw new UnsupportedOperationException("Not supported.");
	}

	public Document createDocument(final String string, final String string1, final DocumentType dt) throws DOMException {
		throw new UnsupportedOperationException("Not supported.");
	}

	public Node importNode(final Node node, final boolean deep, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 1: use type-specific import impl.
			Node importedNode = domNode.doImport(this);

			// step 2: do recursive import?
			if (deep && domNode.hasChildNodes()) {

				// FIXME: is it really a good idea to do the
				// recursion inside of a transaction?
				Node child = domNode.getFirstChild();

				while (child != null) {

					// do not remove parent for child nodes
					importNode(child, deep, false);
					child = child.getNextSibling();

					final Logger logger = LoggerFactory.getLogger(Page.class);
					logger.info("sibling is {}", child);
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			return importedNode;

		}

		return null;
	}

	public Node adoptNode(final Node node, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 2: do recursive import?
			if (domNode.hasChildNodes()) {

				Node child = domNode.getFirstChild();
				while (child != null) {

					// do not remove parent for child nodes
					adoptNode(child, false);
					child = child.getNextSibling();
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			// step 1: use type-specific adopt impl.
			Node adoptedNode = domNode.doAdopt(this);

			return adoptedNode;

		}

		return null;
	}

	public Element getElementById(final String id) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(this.getSecurityContext(), this, results, new Predicate<Node>() {

			@Override
			public boolean accept(Node obj) {

				if (obj instanceof DOMElement) {

					DOMElement elem = (DOMElement) obj;

					if (id.equals(elem.getProperty(StructrApp.key(DOMElement.class, "_html_id")))) {
						return true;
					}
				}

				return false;
			}

		}, 0, true);

		// return first result
		if (results.getLength() == 1) {
			return (DOMElement) results.item(0);
		}

		return null;
	}

	public Element createElement(final String tag) throws DOMException {
		return createElement(tag, false);

	}

	public void handleNewChild(final Node node) {

		try {

			final DOMNode newChild = (DOMNode)node;

			newChild.setOwnerDocument(this);

			for (final DOMNode child : (Set<DOMNode>)newChild.getAllChildNodes()) {

					child.setOwnerDocument(this);

			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("Unable to add new child element to page: {}", ex.getMessage());
		}

	}

	public DocumentFragment createDocumentFragment() {

		final App app = StructrApp.getInstance(this.getSecurityContext());

		try {

			// create new content element
			final DocumentFragment fragment = app.create(DocumentFragment.class);

			fragment.setOwnerDocument(this);

			return fragment;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;

	}

	public Text createTextNode(final String text) {

		try {

			final App app = StructrApp.getInstance(getSecurityContext());

			// create new content element
			final Content content = app.create(Content.class,
				new NodeAttribute(StructrApp.key(Content.class, "hideOnDetail"), false),
				new NodeAttribute(StructrApp.key(Content.class, "hideOnIndex"), false),
				new NodeAttribute(StructrApp.key(Content.class, "content"), text)
			);

			content.setOwnerDocument(this);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public Comment createComment(final String comment) {

		try {

			final App app = StructrApp.getInstance(getSecurityContext());

			// create new content element
			final Comment commentNode = app.create(Comment.class, new NodeAttribute(Content.contentProperty, comment));

			commentNode.setOwnerDocument(this);

			return commentNode;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public CDATASection createCDATASection(final String string) throws DOMException {

		try {

			final App app = StructrApp.getInstance(getSecurityContext());

			// create new content element
			final Cdata content = app.create(Cdata.class);

			content.setOwnerDocument(this);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public ProcessingInstruction createProcessingInstruction(final String string, final String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Attr createAttribute(final String name) throws DOMException {
		return new DOMAttribute(this, null, name, null);
	}

	public EntityReference createEntityReference(final String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Element createElementNS(final String string, final String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	public Attr createAttributeNS(final String string, final String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	public Node getFirstChild() {

		synchronized (this) {

			final Map<String, Object> tmp = this.getTemporaryStorage();
			Html5DocumentType docTypeNode = (Html5DocumentType)tmp.get("doctypeNode");

			if (docTypeNode == null) {

				docTypeNode = new Html5DocumentType(this);
				tmp.put("doctypeNode", docTypeNode);
			}

			return docTypeNode;
		}
	}

	public Element getDocumentElement() {

		Node node = this.treeGetFirstChild();

		if (node instanceof Element) {

			return (Element) node;

		} else {

			return null;
		}
	}

	public void checkHierarchy(final Node otherNode) throws DOMException {

		// verify that this document has only one document element
		if (this.getDocumentElement() != null) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT);
		}

		if (!(otherNode instanceof Html || otherNode instanceof Comment || otherNode instanceof Template)) {

			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT);
		}
	}

	/*
	@Override
	public boolean hasChildNodes() {
		return true;
	}
	*/

	public DOMNodeList getChildNodes() {

		DOMNodeList _children = new DOMNodeList();

		_children.add(this.getFirstChild());
		_children.addAll(this.treeGetChildren());

		return _children;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = this.getVersion();

		this.unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			this.setVersion(1);

		} else {

			this.setVersion(_version + 1);
		}
	}
}
