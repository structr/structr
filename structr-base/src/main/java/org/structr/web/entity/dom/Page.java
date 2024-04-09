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
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaService;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.Site;
import org.structr.web.entity.html.Html;
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
public interface Page extends DOMNode, Linkable, Document, DOMImplementation {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType site = (JsonObjectType)schema.getType("Site");
		final JsonObjectType type = schema.addType("Page");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Page"));
		type.setImplements(URI.create("#/definitions/Linkable"));
		type.setExtends(URI.create("#/definitions/DOMNode"));
		type.setCategory("ui");

		type.addBooleanProperty("isPage", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());

		// if enabled, prevents asynchronous page rendering; enable this flag when using the stream() builtin method
		type.addBooleanProperty("pageCreatesRawData", PropertyView.Public).setDefaultValue("false");

		type.addIntegerProperty("version",         PropertyView.Public, PropertyView.Ui).setIndexed(true).setReadOnly(true).setDefaultValue("0");
		type.addIntegerProperty("position",        PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addIntegerProperty("cacheForSeconds", PropertyView.Public, PropertyView.Ui);

		type.addStringProperty("path",             PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("showOnErrorCodes", PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("contentType",      PropertyView.Public, PropertyView.Ui).setIndexed(true);

		// category is part of a special view named "category"
		type.addStringProperty("category", PropertyView.Public, PropertyView.Ui, "category" ).setIndexed(true);

		type.addPropertyGetter("path", String.class);
		type.addPropertyGetter("elements", Iterable.class);
		type.addPropertyGetter("cacheForSeconds", Integer.class);
		type.addPropertyGetter("sites", Iterable.class);

		type.addPropertyGetter("version", Integer.TYPE);
		type.addPropertySetter("version", Integer.TYPE);

		type.overrideMethod("updateFromNode",     false, "");
		type.overrideMethod("contentEquals",      false, "return false;");
		type.overrideMethod("getBasicAuthRealm",  false, "return getProperty(Linkable.basicAuthRealmProperty);");
		type.overrideMethod("getEnableBasicAuth", false, "return getProperty(Linkable.enableBasicAuthProperty);");

		type.overrideMethod("getLocalName",                false, "return null;");
		type.overrideMethod("getAttributes",               false, "return null;");
		type.overrideMethod("contentEquals",               false, "return false;");
		type.overrideMethod("hasAttributes",               false, "return false;");
		type.overrideMethod("getNodeName",                 false, "return \"#document\";");
		type.overrideMethod("setNodeValue",                false, "");
		type.overrideMethod("getNodeValue",                false, "return null;");
		type.overrideMethod("getNodeType",                 false, "return DOCUMENT_NODE;");
		type.overrideMethod("getElementsByTagNameNS",      false, "throw new UnsupportedOperationException(\"Namespaces not supported.\");");
		type.overrideMethod("getElementsByTagName",        false, "return " + Page.class.getName() + ".getElementsByTagName(this, arg0);");
		type.overrideMethod("getElementById",              false, "return " + Page.class.getName() + ".getElementById(this, arg0);");
		type.overrideMethod("getFirstChild",               false, "return " + Page.class.getName() + ".getFirstChild(this);");
		type.overrideMethod("getChildNodes",               false, "return " + Page.class.getName() + ".getChildNodes(this);");

		type.overrideMethod("render",                      false, Page.class.getName() + ".render(this, arg0, arg1);");
		type.overrideMethod("normalizeDocument",           false, "normalize();");
		type.overrideMethod("renderContent",               false, "");

		type.overrideMethod("checkHierarchy",              true,  Page.class.getName() + ".checkHierarchy(this, arg0);");
		type.overrideMethod("handleNewChild",              false, Page.class.getName() + ".handleNewChild(this, arg0);");
		type.overrideMethod("renameNode",                  false, "return " + Page.class.getName() + ".renameNode(this, arg0, arg1, arg2);");
		type.overrideMethod("doAdopt",                     false, "return " + Page.class.getName() + ".doAdopt(this, arg0);");
		type.overrideMethod("doImport",                    false, "return " + Page.class.getName() + ".doImport(this, arg0);");
		type.overrideMethod("createDocumentType",          false, "return " + Page.class.getName() + ".createDocumentType(this, arg0, arg1, arg2);");
		type.overrideMethod("createDocument",              false, "return " + Page.class.getName() + ".createDocument(this, arg0, arg1, arg2);");
		type.overrideMethod("getContent",                  false, "return " + Page.class.getName() + ".getContent(this, arg0);");
		type.overrideMethod("createDocumentFragment",      false, "return " + Page.class.getName() + ".createDocumentFragment(this);");
		type.overrideMethod("createTextNode",              false, "return " + Page.class.getName() + ".createTextNode(this, arg0);");
		type.overrideMethod("createComment",               false, "return " + Page.class.getName() + ".createComment(this, arg0);");
		type.overrideMethod("createCDATASection",          false, "return " + Page.class.getName() + ".createCDATASection(this, arg0);");
		type.overrideMethod("createProcessingInstruction", false, "return " + Page.class.getName() + ".createProcessingInstruction(this, arg0, arg1);");
		type.overrideMethod("createAttribute",             false, "return " + Page.class.getName() + ".createAttribute(this, arg0);");
		type.overrideMethod("createEntityReference",       false, "return " + Page.class.getName() + ".createEntityReference(this, arg0);");
		type.overrideMethod("createElementNS",             false, "return " + Page.class.getName() + ".createElementNS(this, arg0, arg1);");
		type.overrideMethod("createAttributeNS",           false, "return " + Page.class.getName() + ".createAttributeNS(this, arg0, arg1);");
		type.overrideMethod("getDocumentElement",          false, "return " + Page.class.getName() + ".getDocumentElement(this);");
		type.overrideMethod("getImplementation",           false, "return this;");
		type.overrideMethod("getDoctype",                  false, "return new " + Html5DocumentType.class.getName() + "(this);");
		type.overrideMethod("hasFeature",                  false, "return false;");

		type.overrideMethod("getDomConfig",                false, "return null;");
		type.overrideMethod("importNode",                  false, "return " + Page.class.getName() + ".importNode(this, arg0, arg1, true);");
		type.overrideMethod("adoptNode",                   false, "return " + Page.class.getName() + ".adoptNode(this, arg0, true);");
		type.overrideMethod("setDocumentURI",              false, "");
		type.overrideMethod("getDocumentURI",              false, "return null;");
		type.overrideMethod("getXmlStandalone",            false, "return true;");
		type.overrideMethod("setXmlStandalone",            false, "");
		type.overrideMethod("getXmlVersion",               false, "return \"1.0\";");
		type.overrideMethod("setXmlEncoding",              false, "");
		type.overrideMethod("getXmlEncoding",              false, "return \"utf-8\";");
		type.overrideMethod("setXmlVersion",               false, "");
		type.overrideMethod("getInputEncoding",            false, "return null;");
		type.overrideMethod("getStrictErrorChecking",      false, "return true;");
		type.overrideMethod("setStrictErrorChecking",      false, "");
		type.overrideMethod("getContextName",              false, "return getProperty(name);");

		type.overrideMethod("increaseVersion",             false, Page.class.getName() + ".increaseVersion(this);");

		type.addMethod("setContent")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setSource(Page.class.getName() + ".setContent(this, parameters, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		final JsonMethod createElement1 = type.addMethod("createElement");
		createElement1.setReturnType("org.w3c.dom.Element");
		createElement1.addParameter("tag", "String");
		createElement1.addParameter("suppressException", "boolean");
		createElement1.setSource("return " + Page.class.getName() + ".createElement(this, tag, suppressException);");

		final JsonMethod createElement2 = type.addMethod("createElement");
		createElement2.setReturnType("org.w3c.dom.Element");
		createElement2.addParameter("tag", "String");
		createElement2.setSource("return " + Page.class.getName() + ".createElement(this, tag, false);");

		site.relate(type, "CONTAINS", Cardinality.ManyToMany, "sites", "pages");

		// view configuration
		type.addViewProperty(PropertyView.Public, "linkingElements");
		type.addViewProperty(PropertyView.Public, "enableBasicAuth");
		type.addViewProperty(PropertyView.Public, "basicAuthRealm");
		type.addViewProperty(PropertyView.Public, "dontCache");
		type.addViewProperty(PropertyView.Public, "children");
		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "owner");
		type.addViewProperty(PropertyView.Public, "sites");

		type.addViewProperty(PropertyView.Ui, "pageCreatesRawData");
		type.addViewProperty(PropertyView.Ui, "dontCache");
		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "sites");
	}}

	public static final Set<String> nonBodyTags = new HashSet<>(Arrays.asList(new String[] { "html", "head", "body", "meta", "link" } ));

	Element createElement(final String tag, final boolean suppressException);
	Integer getCacheForSeconds();

	Iterable<DOMNode> getElements();

	Iterable<Site> getSites();

	void setVersion(int version) throws FrameworkException;
	void increaseVersion() throws FrameworkException;
	int getVersion();

	public static void setContent(final Page thisPage, final Map<String, Object> parameters, final SecurityContext ctx) throws FrameworkException {

		final String content = (String)parameters.get("content");
		if (content == null) {

			throw new FrameworkException(422, "Cannot set content of page " + thisPage.getName() + ", no content provided");
		}

		final Importer importer = new Importer(thisPage.getSecurityContext(), content, null, null, false, false, false, false);
		final App app           = StructrApp.getInstance(ctx);

		importer.setIsDeployment(true);
		importer.setCommentHandler(new DeploymentCommentHandler());

		if (importer.parse(false)) {

			for (final DOMNode node : thisPage.getAllChildNodes()) {
				app.delete(node);
			}

			importer.createChildNodesWithHtml(thisPage, thisPage, true);
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
	public static Page createNewPage(SecurityContext securityContext, String name) throws FrameworkException {
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

	static String getContent(final Page page, final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx = new RenderContext(page.getSecurityContext(), null, null, editMode);
		final StringRenderBuffer buffer = new StringRenderBuffer();
		ctx.setBuffer(buffer);
		page.render(ctx, 0);

		// extract source
		return buffer.getBuffer().toString();
	}

	static Element createElement (final Page page, final String tag, final boolean suppressException) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Logger logger                = LoggerFactory.getLogger(Page.class);
		final App app                      = StructrApp.getInstance(page.getSecurityContext());
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

			element.doAdopt(page);

			return element;

		} catch (Throwable t) {

			if (!suppressException) {

				logger.error("Unable to instantiate element of type " + elementType, t);
			}
		}

		return null;
	}

	public static NodeList getElementsByTagName(final Page thisPage, final String tagName) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(thisPage.getSecurityContext(), thisPage, results, new Predicate<Node>() {

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

	public static void render(final Page thisPage, final RenderContext renderContext, final int depth) throws FrameworkException {

		renderContext.setPage(thisPage);

		// Skip DOCTYPE node
		DOMNode subNode = (DOMNode) thisPage.getFirstChild().getNextSibling();

		if (subNode == null) {

			thisPage.checkReadAccess();
			subNode = (DOMNode) thisPage.treeGetFirstChild();


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

	public static Node doAdopt(final Page thisPage, final Page page) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC);
	}

	public static Node doImport(final Page thisPage, final Page newPage) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC);
	}

	public static Node renameNode(final Page thisPage, final Node node, final String string, final String string1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_RENAME);
	}

	public static DocumentType createDocumentType(final Page thisPage, final String string, final String string1, final String string2) throws DOMException {
		throw new UnsupportedOperationException("Not supported.");
	}

	public static Document createDocument(final Page thisPage, final String string, final String string1, final DocumentType dt) throws DOMException {
		throw new UnsupportedOperationException("Not supported.");
	}

	public static Node importNode(final Page thisPage, final Node node, final boolean deep, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 1: use type-specific import impl.
			Node importedNode = domNode.doImport(thisPage);

			// step 2: do recursive import?
			if (deep && domNode.hasChildNodes()) {

				// FIXME: is it really a good idea to do the
				// recursion inside of a transaction?
				Node child = domNode.getFirstChild();

				while (child != null) {

					// do not remove parent for child nodes
					Page.importNode(thisPage, child, deep, false);
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

	public static Node adoptNode(final Page thisPage, final Node node, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 2: do recursive import?
			if (domNode.hasChildNodes()) {

				Node child = domNode.getFirstChild();
				while (child != null) {

					// do not remove parent for child nodes
					adoptNode(thisPage, child, false);
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
			Node adoptedNode = domNode.doAdopt(thisPage);

			return adoptedNode;

		}

		return null;
	}

	public static Element getElementById(final Page thisPage, final String id) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(thisPage.getSecurityContext(), thisPage, results, new Predicate<Node>() {

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

	public static Element createElement(final Page thisPage, final String tag) throws DOMException {
		return createElement(thisPage, tag, false);

	}

	public static void handleNewChild(final Page thisPage, Node node) {

		try {

			final DOMNode newChild = (DOMNode)node;

			newChild.setOwnerDocument(thisPage);

			for (final DOMNode child : (Set<DOMNode>)newChild.getAllChildNodes()) {

					child.setOwnerDocument(thisPage);

			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("Unable to add new child element to page: {}", ex.getMessage());
		}

	}

	public static DocumentFragment createDocumentFragment(final Page thisPage) {

		final App app = StructrApp.getInstance(thisPage.getSecurityContext());

		try {

			// create new content element
			org.structr.web.entity.dom.DocumentFragment fragment = app.create(org.structr.web.entity.dom.DocumentFragment.class);

			fragment.setOwnerDocument(thisPage);

			return fragment;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;

	}

	public static Text createTextNode(final Page thisPage, final String text) {

		try {

			// create new content element
			Content content = (Content) StructrApp.getInstance(thisPage.getSecurityContext()).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, Content.class.getSimpleName()),
				new NodeAttribute(StructrApp.key(Content.class, "hideOnDetail"), false),
				new NodeAttribute(StructrApp.key(Content.class, "hideOnIndex"), false),
				new NodeAttribute(StructrApp.key(Content.class, "content"), text)
			);

			content.setOwnerDocument(thisPage);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public static Comment createComment(final Page thisPage, String comment) {

		try {

			// create new content element
			org.structr.web.entity.dom.Comment commentNode = (org.structr.web.entity.dom.Comment) StructrApp.getInstance(thisPage.getSecurityContext()).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, org.structr.web.entity.dom.Comment.class.getSimpleName()),
				new NodeAttribute(StructrApp.key(Content.class, "content"), comment)
			);

			commentNode.setOwnerDocument(thisPage);

			return commentNode;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public static CDATASection createCDATASection(final Page thisPage, String string) throws DOMException {

		try {

			// create new content element
			Cdata content = (Cdata) StructrApp.getInstance(thisPage.getSecurityContext()).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, Cdata.class.getSimpleName())
			);

			content.setOwnerDocument(thisPage);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			final Logger logger = LoggerFactory.getLogger(Page.class);
			logger.warn("", fex);
		}

		return null;
	}

	public static ProcessingInstruction createProcessingInstruction(final Page thisPage, String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static Attr createAttribute(final Page thisPage, String name) throws DOMException {
		return new DOMAttribute(thisPage, null, name, null);
	}

	public static EntityReference createEntityReference(final Page thisPage, String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static Element createElementNS(final Page thisPage, String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	public static Attr createAttributeNS(final Page thisPage, String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	public static Node getFirstChild(final Page thisPage) {

		synchronized (thisPage) {

			final Map<String, Object> tmp = thisPage.getTemporaryStorage();
			Html5DocumentType docTypeNode = (Html5DocumentType)tmp.get("doctypeNode");

			if (docTypeNode == null) {

				docTypeNode = new Html5DocumentType(thisPage);
				tmp.put("doctypeNode", docTypeNode);
			}

			return docTypeNode;
		}
	}

	public static Element getDocumentElement(final Page thisPage) {

		Node node = thisPage.treeGetFirstChild();

		if (node instanceof Element) {

			return (Element) node;

		} else {

			return null;
		}
	}

	static void checkHierarchy(final Page thisPage, final Node otherNode) throws DOMException {

		// verify that this document has only one document element
		if (thisPage.getDocumentElement() != null) {
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

	public static NodeList getChildNodes(final Page thisPage) {

		DOMNodeList _children = new DOMNodeList();

		_children.add(thisPage.getFirstChild());
		_children.addAll(thisPage.treeGetChildren());

		return _children;
	}

	static void increaseVersion(final Page thisPage) throws FrameworkException {

		final Integer _version = thisPage.getVersion();

		thisPage.unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			thisPage.setVersion(1);

		} else {

			thisPage.setVersion(_version + 1);
		}
	}
}
