/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.importer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.CaseHelper;
import org.structr.common.PathHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.Actions;
import org.structr.schema.importer.SchemaJsonImporter;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.diff.*;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Input;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The importer creates a new page by downloading and parsing markup from a URL.
 */
public class Importer {

	private static final Logger logger = LoggerFactory.getLogger(Importer.class.getName());

	private static final Set<String> hrefElements       = new LinkedHashSet<>(Arrays.asList(new String[]{"link"}));
	private static final Set<String> ignoreElementNames = new LinkedHashSet<>(Arrays.asList(new String[]{"#declaration", "#doctype"}));
	private static final Set<String> srcElements        = new LinkedHashSet<>(Arrays.asList(new String[]{"img", "script", "audio", "video", "input", "source", "track"}));

	private static final Map<String, String> contentTypeForExtension = new HashMap<>();

	private static App app;

	private final static String DATA_STRUCTR_PREFIX = "data-structr-";
	private final static String DATA_META_PREFIX    = "data-structr-meta-";

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js",  "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	private final StringBuilder commentSource    = new StringBuilder();
	private final SecurityContext securityContext;
	private final boolean includeInExport;
	private final boolean publicVisible;
	private final boolean authVisible;
	private CommentHandler commentHandler;
	private boolean relativeVisibility = false;
	private boolean withTemplate       = false;
	private boolean isDeployment       = false;
	private Document parsedDocument    = null;
	private final String name;
	private URL originalUrl;
	private String address;
	private String code;
	private String tableChildElement;

	private Map<String, Linkable> alreadyDownloaded = new HashMap<>();

	private Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties = new HashMap<>();

	/**
	 * Construct an instance of the importer to either read the given code, or download code from the given address.
	 *
	 * The importer will create a page with the given name. Visibility can be controlled by publicVisible and authVisible.
	 *
	 * @param securityContext
	 * @param code
	 * @param address
	 * @param name
	 * @param publicVisible
	 * @param authVisible
	 * @param includeInExport
	 * @param relativeVisibility
	 */
	public Importer(final SecurityContext securityContext, final String code, final String address, final String name, final boolean publicVisible, final boolean authVisible, final boolean includeInExport, final boolean relativeVisibility) {
		this(securityContext, code, address, name, publicVisible, authVisible, includeInExport, relativeVisibility, false);
	}

	/**
	 * Construct an instance of the importer to either read the given code, or download code from the given address.
	 *
	 * The importer will create a page with the given name. Visibility can be controlled by publicVisible and authVisible.
	 *
	 * @param securityContext
	 * @param code
	 * @param address
	 * @param name
	 * @param publicVisible
	 * @param authVisible
	 * @param includeInExport
	 * @param relativeVisibility
	 * @param withTemplate
	 */
	public Importer(final SecurityContext securityContext, final String code, final String address, final String name, final boolean publicVisible, final boolean authVisible, final boolean includeInExport, final boolean relativeVisibility, final boolean withTemplate) {

		this.code               = code;
		this.address            = address;
		this.name               = name;
		this.securityContext    = securityContext;
		this.publicVisible      = publicVisible;
		this.authVisible        = authVisible;
		this.includeInExport    = includeInExport;
		this.relativeVisibility = relativeVisibility;
		this.withTemplate       = withTemplate;

		if (address != null && !address.endsWith("/") && !address.endsWith(".html")) {
			this.address = this.address.concat("/");
		}

		// only do this if address is non-null
		if (this.address != null) {

			try {
				originalUrl = new URL(this.address);

			} catch (MalformedURLException ex) {
				logger.info("Cannot convert '{}' to URL - is the protocol ok? Trying to resume anyway...", this.address);
			}
		}
	}

	private void init() {
		app = StructrApp.getInstance(securityContext);
	}

	public void setCommentHandler(final CommentHandler handler) {
		this.commentHandler = handler;
	}

	/**
	 * Parse the code previously read by {@link Importer#readPage()} and treat it as complete page.
	 *
	 * @return
	 * @throws FrameworkException
	 */
	public boolean parse() throws FrameworkException {
		return parse(false);
	}

	/**
	 * Parse the code previously read by {@link Importer#readPage()} and treat it as page fragment.
	 *
	 * @param fragment
	 * @return
	 * @throws FrameworkException
	 */
	public boolean parse(final boolean fragment) throws FrameworkException {

		init();

		if (StringUtils.isNotBlank(code)) {

			if (isDeployment) {

				// a trailing slash to all void/self-closing tags so the XML parser can parse it correctly
				code = code.replaceAll("<(area|base|br|col(?!group)|command|embed|hr|img|input|keygen|link|meta|param|source|track|wbr)([^>]*)>", "<$1$2/>");
			}

			if (fragment) {

				if (isDeployment) {

					final List<Node> nodeList = Parser.parseXmlFragment(code, "");
					parsedDocument            = Document.createShell("");
					final Element body        = parsedDocument.body();
					final Node[] nodes        = nodeList.toArray(new Node[nodeList.size()]);

					for (int i = nodes.length - 1; i > 0; i--) {
					    nodes[i].remove();
					}

					for (Node node : nodes) {
					    body.appendChild(node);
					}

				} else {

					final Matcher matcher = Pattern.compile("^\\s*<(thead|tbody|caption|colgroup|th|tr|tfoot).*", Pattern.CASE_INSENSITIVE).matcher(code);

					if (matcher.matches()) {

						// if outermost tag is a table element so use <table> as context element
						parsedDocument      = Document.createShell("");
						final Element body  = parsedDocument.body();
						final Element table = body.appendElement("table");

						final List<Node> nodeList = Parser.parseFragment(code, table, "");
						final Node[] nodes        = nodeList.toArray(new Node[nodeList.size()]);

						for (int i = nodes.length - 1; i > 0; i--) {
							nodes[i].remove();
						}

						for (Node node : nodes) {
							table.appendChild(node);
						}

						tableChildElement = matcher.group(1);

					} else {

						parsedDocument = Jsoup.parseBodyFragment(code);
					}

				}

			} else {

				if (isDeployment) {

					parsedDocument = Jsoup.parse(code, "", Parser.xmlParser());

				} else {

					parsedDocument = Jsoup.parse(code);
				}

			}

		} else {

			if (!isDeployment) {
				logger.info("##### Start fetching {} for page {} #####", new Object[]{address, name});
			}

			code = HttpHelper.get(address);
			parsedDocument = Jsoup.parse(code);

		}

		return true;

	}

	public Page readPage() throws FrameworkException {
		return readPage(null);
	}

	public Page readPage(final String uuid) throws FrameworkException {

		Page page = Page.createNewPage(securityContext, uuid, name);
		if (page != null) {

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);
			changedProperties.put(AbstractNode.visibleToPublicUsers, publicVisible);
			page.setProperties(securityContext, changedProperties);

			createChildNodes(parsedDocument, page, page);

			if (!isDeployment) {
				logger.info("##### Finished fetching {} for page {} #####", new Object[]{address, name});
			}
		}

		return page;
	}

	public DOMNode createComponentChildNodes(final Page page) throws FrameworkException {
		return createComponentChildNodes(null, page);
	}

	public DOMNode createComponentChildNodes(final DOMNode parent, final Page page) throws FrameworkException {

		// head() and body() automatically create elements (inlcuding "html") - but we want to keep the original intact
		final String initialDocument = parsedDocument.toString();
		Element head                 = parsedDocument.head();

		if (!initialDocument.equals(parsedDocument.toString())) {

			// at least "head" element was added
			final Element headParent = head.parent();
			head.remove();

			if (!initialDocument.equals(parsedDocument.toString())) {
				// also "html" element was added
				headParent.remove();
			}

			head = null;
		}

		Element body = parsedDocument.body();

		if (!initialDocument.equals(parsedDocument.toString())) {

			// at least "body" element was added
			final Element bodyParent = body.parent();
			body.remove();

			if (!initialDocument.equals(parsedDocument.toString())) {
				// also "html" element was added
				bodyParent.remove();
			}

			body = null;
		}

		if (head != null && !head.html().isEmpty()) {

			// create Head element and append nodes to it
			final Head headElement = (Head)page.createElement("head");
			headElement.setProperty(AbstractNode.visibleToPublicUsers,        publicVisible);
			headElement.setProperty(AbstractNode.visibleToAuthenticatedUsers, authVisible);
			createChildNodes(head, headElement, page);

			// head is a special case
			return headElement;
		}

		if (body != null && !body.html().isEmpty()) {

			// create Head element and append nodes to it
			final Body bodyElement = (Body)page.createElement("body");
			bodyElement.setProperty(AbstractNode.visibleToPublicUsers,        publicVisible);
			bodyElement.setProperty(AbstractNode.visibleToAuthenticatedUsers, authVisible);
			createChildNodes(body, bodyElement, page);

			// body is another special case
			return bodyElement;
		}

		// fallback, no head no body => document is parent
		return createChildNodes(parsedDocument, parent, page);
	}

	public DOMNode createChildNodes(final DOMNode parent, final Page page) throws FrameworkException {

		return createChildNodes(parsedDocument.body(), parent, page);
	}

	public DOMNode createChildNodes(final DOMNode parent, final Page page, final boolean removeHashAttribute) throws FrameworkException {

		return createChildNodes(parsedDocument.body(), parent, page, removeHashAttribute, 0);
	}

	public DOMNode createChildNodesWithHtml(final DOMNode parent, final Page page, final boolean removeHashAttribute) throws FrameworkException {

		return createChildNodes(parsedDocument, parent, page, removeHashAttribute, 0);
	}

	public void setIsDeployment(final boolean isDeployment) {
		this.isDeployment = isDeployment;
	}

	public void setDeferredNodesAndTheirProperties(final Map<DOMNode, PropertyMap> props) {
		this.deferredNodesAndTheirProperties = props;
	}

	public Map<DOMNode, PropertyMap> getDeferredNodesAndTheirProperties() {
		return this.deferredNodesAndTheirProperties;
	}

	public String getTableChildElement() {
		return tableChildElement;
	}

	public void retainHullOnly() {

		final List<Node> nodeList = new LinkedList<>();

		for (final Node node : parsedDocument.childNodes()) {

			for (final Node child : node.childNodes()) {
				nodeList.add(child);
			}
		}

		for (final Node deleteNode : nodeList) {
			deleteNode.remove();
		}
	}

	public DOMNode createComponentHullChildNodes (final DOMNode parent, final Page page) throws FrameworkException {

		for (final Node node : parsedDocument.childNodes()) {

			String tag = node.nodeName();
			final String type = CaseHelper.toUpperCamelCase(tag);

			if (ignoreElementNames.contains(type)) {

				continue;
			}

			if (!type.equals("#comment")) {
				return createChildNodes(node, parent, page, false, 1, parent);
			}
		}

		logger.warn("Empty shared component!");

		return null;
	}

	// ----- public static methods -----
	public static Page parsePageFromSource(final SecurityContext securityContext, final String source, final String name) throws FrameworkException {

		return parsePageFromSource(securityContext, source, name, false);
	}

	public static Page parsePageFromSource(final SecurityContext securityContext, final String source, final String name, final boolean removeHashAttribute) throws FrameworkException {

		final Importer importer = new Importer(securityContext, source, null, "source", false, false, false, false);
		final App localAppCtx = StructrApp.getInstance(securityContext);
		Page page = null;

		try (final Tx tx = localAppCtx.tx(true, false, false)) {

			page = localAppCtx.create(Page.class, new NodeAttribute<>(Page.name, name));

			if (importer.parse()) {

				importer.createChildNodesWithHtml(page, page, removeHashAttribute);
			}

			tx.success();

			return page;

		} catch (Throwable t) {
			logger.warn("Unable to parse source:\n\n" + source);
			return null;
		}

	}

	public static List<InvertibleModificationOperation> diffNodes(final DOMNode sourceNode, final DOMNode modifiedNode) {

		if (sourceNode == null) {

			logger.warn("Source node was null, returning empty change set.");
			return Collections.EMPTY_LIST;
		}

		if (modifiedNode == null) {

			logger.warn("Modified node was null, returning empty change set.");
			return Collections.EMPTY_LIST;
		}

		final List<InvertibleModificationOperation> changeSet = new LinkedList<>();
		final Map<String, DOMNode>                  indexMappedExistingNodes = new LinkedHashMap<>();
		final Map<String, DOMNode>                  hashMappedExistingNodes  = new LinkedHashMap<>();
		final Map<DOMNode, Integer>                 depthMappedExistingNodes = new LinkedHashMap<>();
		final Map<String, DOMNode>                  indexMappedNewNodes      = new LinkedHashMap<>();
		final Map<String, DOMNode>                  hashMappedNewNodes       = new LinkedHashMap<>();
		final Map<DOMNode, Integer>                 depthMappedNewNodes      = new LinkedHashMap<>();

		InvertibleModificationOperation.collectNodes(sourceNode, indexMappedExistingNodes, hashMappedExistingNodes, depthMappedExistingNodes);
		InvertibleModificationOperation.collectNodes(modifiedNode, indexMappedNewNodes, hashMappedNewNodes, depthMappedNewNodes);

		// iterate over existing nodes and try to find deleted ones
		for (final Iterator<Map.Entry<String, DOMNode>> it = hashMappedExistingNodes.entrySet().iterator(); it.hasNext();) {

			final Map.Entry<String, DOMNode> existingNodeEntry = it.next();
			final DOMNode existingNode = existingNodeEntry.getValue();
			final String existingHash = existingNode.getIdHash();

			// check for deleted nodes ignoring Page nodes
			if (!hashMappedNewNodes.containsKey(existingHash) && !(existingNode instanceof Page)) {

				changeSet.add(new DeleteOperation(hashMappedExistingNodes, existingNode));
			}
		}

		// iterate over new nodes and try to find new ones
		for (final Iterator<Map.Entry<String, DOMNode>> it = indexMappedNewNodes.entrySet().iterator(); it.hasNext();) {

			final Map.Entry<String, DOMNode> newNodeEntry = it.next();
			final DOMNode newNode = newNodeEntry.getValue();

			// if newNode is a content element, do not rely on local hash property
			String newHash = newNode.getDataHash();
			if (newHash == null) {

				newHash = newNode.getIdHash();
			}

			// check for deleted nodes ignoring Page nodes
			if (!hashMappedExistingNodes.containsKey(newHash) && !(newNode instanceof Page)) {

				final DOMNode newParent = newNode.getParent();

				changeSet.add(new CreateOperation(hashMappedExistingNodes, getHashOrNull(newParent), getSiblingHashes(newNode), newNode, depthMappedNewNodes.get(newNode)));
			}
		}

		// compare all new nodes with all existing nodes
		for (final Map.Entry<String, DOMNode> newNodeEntry : indexMappedNewNodes.entrySet()) {

			final String newTreeIndex = newNodeEntry.getKey();
			final DOMNode newNode = newNodeEntry.getValue();

			for (final Map.Entry<String, DOMNode> existingNodeEntry : indexMappedExistingNodes.entrySet()) {

				final String existingTreeIndex = existingNodeEntry.getKey();
				final DOMNode existingNode = existingNodeEntry.getValue();
				DOMNode newParent = null;
				int equalityBitmask = 0;

				if (newTreeIndex.equals(existingTreeIndex)) {
					equalityBitmask |= 1;
				}

				if (newNode.getIdHashOrProperty().equals(existingNode.getIdHash())) {
					equalityBitmask |= 2;
				}

				if (newNode.contentEquals(existingNode)) {
					equalityBitmask |= 4;
				}

				switch (equalityBitmask) {

					case 7: // same tree index (1), same node (2), same content (4) => node is completely unmodified
						break;

					case 6: // same content (2), same node (4), NOT same tree index => node has moved
						newParent = newNode.getParent();
						changeSet.add(new MoveOperation(hashMappedExistingNodes, getHashOrNull(newParent), getSiblingHashes(newNode), newNode, existingNode));
						break;

					case 5: // same tree index (1), NOT same node, same content (5) => node was deleted and restored, maybe the identification information was lost
						break;

					case 4: // NOT same tree index, NOT same node, same content (4) => different node, content is equal by chance?
						break;

					case 3: // same tree index, same node, NOT same content => node was modified but not moved
						changeSet.add(new UpdateOperation(hashMappedExistingNodes, existingNode, newNode));
						break;

					case 2: // NOT same tree index, same node (2), NOT same content => node was moved and changed
						newParent = newNode.getParent();
						changeSet.add(new UpdateOperation(hashMappedExistingNodes, existingNode, newNode));
						changeSet.add(new MoveOperation(hashMappedExistingNodes, getHashOrNull(newParent), getSiblingHashes(newNode), newNode, existingNode));
						break;

					case 1: // same tree index (1), NOT same node, NOT same content => ignore
						break;

					case 0: // NOT same tree index, NOT same node, NOT same content => ignore
						break;
				}
			}
		}

		return changeSet;
	}

	private static List<String> getSiblingHashes(final DOMNode node) {

		final List<String> siblingHashes = new LinkedList<>();
		DOMNode nextSibling = node.getNextSibling();

		while (nextSibling != null) {

			siblingHashes.add(nextSibling.getIdHashOrProperty());
			nextSibling = nextSibling.getNextSibling();
		}

		return siblingHashes;
	}

	private static String getHashOrNull(final DOMNode node) {

		if (node != null) {
			return node.getIdHashOrProperty();
		}

		return null;
	}

	// ----- private methods -----
	private DOMNode createChildNodes(final Node startNode, final DOMNode parent, final Page page) throws FrameworkException {
		return createChildNodes(startNode, parent, page, false, 0);
	}

	private DOMNode createChildNodes(final Node startNode, final DOMNode parent, final Page page, final boolean removeHashAttribute, final int depth) throws FrameworkException {
		return createChildNodes(startNode, parent, page, false, 0, null);
	}

	private DOMNode createChildNodes(final Node startNode, final DOMNode parent, final Page page, final boolean removeHashAttribute, final int depth, final DOMNode suppliedRoot) throws FrameworkException {

		DOMNode rootElement     = suppliedRoot;
		Linkable linkable       = null;
		String instructions     = null;

		final List<Node> children = startNode.childNodes();
		for (Node node : children) {

			String tag = node.nodeName();

			// clean tag, remove non-word characters except : and #
			if (tag != null) {
				tag = tag.replaceAll("[^a-zA-Z0-9#:.\\-_]+", "");
			}

			final StringBuilder classString  = new StringBuilder();
			final String type                = CaseHelper.toUpperCamelCase(tag);
			String comment                   = null;
			String content                   = null;
			String id                        = null;
			boolean isNewTemplateOrComponent = false;
			boolean dontSetParent            = false;

			if (ignoreElementNames.contains(type)) {

				continue;
			}

			if (node instanceof Element) {

				final Element el          = ((Element) node);
				final Set<String> classes = el.classNames();

				for (String cls : classes) {

					classString.append(cls).append(" ");
				}

				id = el.id();

				// do not download files when called from DeployCommand!
				if (!isDeployment) {

					String downloadAddressAttr = srcElements.contains(tag)
						? "src" : hrefElements.contains(tag)
						? "href" : null;

					if (originalUrl != null && downloadAddressAttr != null && StringUtils.isNotBlank(node.attr(downloadAddressAttr))) {

						String downloadAddress = node.attr(downloadAddressAttr);
						linkable = downloadFile(downloadAddress, originalUrl);
					} else {
						linkable = null;
					}
				}

				if (removeHashAttribute) {

					// Remove data-structr-hash attribute
					node.removeAttr("data-structr-hash");
				}
			}

			// Data and comment nodes: Trim the text and put it into the "content" field without changes
			if (type.equals("#comment")) {

				comment = ((Comment) node).getData();
				tag     = "";

				// Don't add content node for whitespace
				if (StringUtils.isBlank(comment)) {

					continue;
				}

				// store for later use
				commentSource.append(comment).append("\n");

				// check if comment contains instructions
				if (commentHandler != null && commentHandler.containsInstructions(comment)) {

					if (instructions != null) {

						// unhandled instructions from previous iteration => empty content element
						createEmptyContentNode(page, parent, commentHandler, instructions);
					}

					instructions = comment;
					continue;
				}

			} else if (type.equals("#data")) {

				tag = "";
				content = ((DataNode) node).getWholeData();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}

			} else // Text-only nodes: Trim the text and put it into the "content" field
			{
				if (type.equals("#text")) {

					tag = "";

					if (isDeployment) {

						content = trimTrailingNewline(((TextNode) node).getWholeText());

						if (content == null || content.length() == 0) {
							continue;
						}

					} else {

						content = trimTrailingNewline(((TextNode) node).text());

						if (StringUtils.isBlank(content)) {
							continue;
						}
					}
				}
			}

			org.structr.web.entity.dom.DOMNode newNode = null;

			// create node
			if (StringUtils.isBlank(tag)) {

				if (page != null) {

					final PropertyKey<String> contentTypeKey = StructrApp.key(Content.class, "contentType");

					// create comment or content node
					if (!StringUtils.isBlank(comment)) {

						newNode = (DOMNode) page.createComment(DOMNode.unescapeForHtml(comment));
						newNode.setProperty(contentTypeKey, "text/html");

					} else {

						newNode = (Content) page.createTextNode(content);

						final PropertyKey<String> typeKey = StructrApp.key(Input.class, "_html_type");

						if (parent != null && "text/css".equals(parent.getProperty(typeKey))) {
							newNode.setProperty(contentTypeKey, "text/css");
						}
					}
				}

			} else if ("structr:template".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode template = null;

					if (DeployCommand.isUuid(src)) {

						template = (DOMNode)StructrApp.getInstance().nodeQuery(NodeInterface.class).and(GraphObject.id, src).getFirst();

						if (template == null) {

							logger.warn("##################################### template with UUID {} not found, this is a known bug", src);

						}

					} else if (DeployCommand.endsWithUuid(src)) {

						final String uuid = src.substring(src.length() - 32);
						template = (DOMNode)StructrApp.getInstance().nodeQuery(NodeInterface.class).and(GraphObject.id, uuid).getFirst();

						if (template == null) {

							logger.warn("##################################### template with UUID not found, this is a known bug", uuid);
						}

					} else {

						template = Importer.findSharedComponentByName(src);
						if (template == null) {

							template = Importer.findTemplateByName(src);

							if (template == null) {

								template = createNewTemplateNode(parent, node.childNodes());
								isNewTemplateOrComponent = true;
							}
						}
					}

					if (template != null) {

						newNode = template;

						if (template.isSharedComponent()) {

							newNode = (DOMNode) template.cloneNode(false);

							newNode.setSharedComponent(template);
							newNode.setOwnerDocument(page);

						} else if (page != null) {

							newNode.setOwnerDocument(page);
						}

					} else {

						logger.warn("Unable to find template or shared component {}, template ignored!", src);
					}

				} else {

					logger.warn("Invalid template definition, missing src attribute!");
				}

			} else if ("structr:shared-template".equals(tag)) {

				final String name = node.attr("name");
				if (StringUtils.isNotBlank(name)) {

					DOMNode template = Importer.findSharedComponentByName(name);
					if (template == null) {

						newNode = createNewTemplateNode(null, node.childNodes());

						isNewTemplateOrComponent = true;
						dontSetParent            = true;

						// create shared component (and nothing else) from this template
						newNode.setOwnerDocument(CreateComponentCommand.getOrCreateHiddenDocument());
						newNode.setProperty(AbstractNode.name, name);
					}

				} else {

					logger.warn("Invalid shared template definition, missing name attribute!");
				}

			} else if ("structr:component".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode component = null;
					if (DeployCommand.isUuid(src)) {

						component = app.nodeQuery(DOMNode.class).and(GraphObject.id, src).getFirst();

					} else if (DeployCommand.endsWithUuid(src)) {

						final String uuid = src.substring(src.length() - 32);
						component = app.nodeQuery(DOMNode.class).and(GraphObject.id, uuid).getFirst();

					} else {

						component = Importer.findSharedComponentByName(src);
					}

					if (component == null) {

						component = createSharedComponent(node);
					}

					isNewTemplateOrComponent = true;

					if (component != null) {

						newNode = (DOMNode) component.cloneNode(false);

						final String _html_src = newNode.getProperty(new StringProperty("_html_src"));
						if (!StringUtils.isEmpty(_html_src)) {
							node.attr("src", _html_src);
						} else {
							node.removeAttr("src");
						}

						newNode.setSharedComponent(component);
						newNode.setOwnerDocument(page);

					} else {

						logger.warn("Unable to find shared component '{}' - ignored! This should not happen.", src);
					}

				} else {

					logger.warn("Invalid component definition, missing src attribute!");
				}

			} else {

				if (page != null) {

					newNode = (org.structr.web.entity.dom.DOMElement) page.createElement(tag, true);
				}

			}

			if (newNode != null) {

				// save root element for later use
				if (rootElement == null && !(newNode instanceof org.structr.web.entity.dom.Comment)) {
					rootElement = newNode;
				}

				// set linkable
				if (linkable != null && newNode instanceof LinkSource) {
					((LinkSource)newNode).setLinkable(linkable);
				}

				// container for bulk setProperties()
				final PropertyMap newNodeProperties      = new PropertyMap();
				final PropertyMap deferredNodeProperties = new PropertyMap();
				final Class newNodeType                  = newNode.getClass();

				if (isDeployment && !relativeVisibility) {
					newNodeProperties.put(AbstractNode.visibleToPublicUsers,        publicVisible);
					newNodeProperties.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);
				} else {
					newNodeProperties.put(AbstractNode.visibleToPublicUsers,        parent != null ? parent.getProperty(AbstractNode.visibleToPublicUsers) : publicVisible);
					newNodeProperties.put(AbstractNode.visibleToAuthenticatedUsers, parent != null ? parent.getProperty(AbstractNode.visibleToAuthenticatedUsers) : authVisible);
				}

				// "id" attribute: Put it into the "_html_id" field
				if (StringUtils.isNotBlank(id)) {

					newNodeProperties.put(StructrApp.key(DOMElement.class, "_html_id"), id);
				}

				if (StringUtils.isNotBlank(classString.toString())) {

					newNodeProperties.put(StructrApp.key(DOMElement.class, "_html_class"), StringUtils.trim(classString.toString()));
				}

				for (Attribute nodeAttr : node.attributes()) {

					final String key = nodeAttr.getKey();

					if (!key.equals("text")) { // Don't add text attribute as _html_text because the text is already contained in the 'content' attribute

						final String value = nodeAttr.getValue();

						if (key.startsWith("data-")) {

							if (key.startsWith(DATA_META_PREFIX)) { // convert data-structr-meta-* attributes to local camel case properties on the node,

								int l = DATA_META_PREFIX.length();

								String upperCaseKey = WordUtils.capitalize(key.substring(l), new char[]{'-'}).replaceAll("-", "");
								String camelCaseKey = key.substring(l, l + 1).concat(upperCaseKey.substring(1));

								if (value != null) {

									// store value using actual input converter
									final PropertyKey actualKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(newNodeType, camelCaseKey, false);
									if (actualKey != null) {

										final PropertyConverter converter = actualKey.inputConverter(securityContext);
										if (converter != null) {

											final Object convertedValue = converter.convert(value);

											if (value != null && convertedValue == null) {

												// DOMNode to be linked is not yet imported, so we store it to handle it later
												deferredNodeProperties.put(actualKey, value);

											} else {

												newNodeProperties.put(actualKey, convertedValue);
											}

										} else {

											newNodeProperties.put(actualKey, value);
										}

									} else {

										logger.warn("Unknown meta property key {}, ignoring.", camelCaseKey);
									}
								}

							} else if (key.startsWith(DATA_STRUCTR_PREFIX)) { // don't convert data-structr-* attributes as they are internal

								final PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(newNodeType, key);
								if (propertyKey != null) {

									final PropertyConverter inputConverter = propertyKey.inputConverter(securityContext);
									if (value != null && inputConverter != null) {

										newNodeProperties.put(propertyKey, propertyKey.inputConverter(securityContext).convert(value));

									} else {

										newNodeProperties.put(propertyKey, value);
									}
								}

							} else {

								// store data-* attributes in node
								if (value != null) {

									newNodeProperties.put(new StringProperty(CustomHtmlAttributeProperty.CUSTOM_HTML_ATTRIBUTE_PREFIX + key), value);
								}
							}

						} else {

							boolean notBlank = StringUtils.isNotBlank(value);
							boolean isAnchor = notBlank && value.startsWith("#");
							boolean isLocal = notBlank && !value.startsWith("http");
							boolean isActive = notBlank && value.contains("${");
							boolean isStructrLib = notBlank && value.startsWith("/structr/js/");

							if (linkable != null && "link".equals(tag) && "href".equals(key) && isLocal && !isActive && !isDeployment) {

								newNodeProperties.put(new StringProperty(PropertyView.Html + key), "${link.path}?${link.version}");

							} else if (linkable != null && ("href".equals(key) || "src".equals(key)) && isLocal && !isActive && !isAnchor && !isStructrLib && !isDeployment) {

								newNodeProperties.put(new StringProperty(PropertyView.Html + key), "${link.path}");

							} else {

								if (key.startsWith("aria-")) {

									// use custom key
									newNodeProperties.put(new StringProperty(CustomHtmlAttributeProperty.CUSTOM_HTML_ATTRIBUTE_PREFIX + key), value);

								} else {

									newNodeProperties.put(new StringProperty(PropertyView.Html + key), value);
								}
							}
						}
					}
				}

				// bulk set properties on new node
				newNode.setProperties(securityContext, newNodeProperties);

				deferredNodesAndTheirProperties.put(newNode, deferredNodeProperties);

				if ("script".equals(tag)) {

					final PropertyKey<String> typeKey = StructrApp.key(Input.class, "_html_type");
					final String contentType          = newNode.getProperty(typeKey);

					if (contentType == null) {

						// Set default type of script tag to "text/javascript" to ensure inline JS gets imported properly
						newNode.setProperty(typeKey, "text/javascript");

					} else if (contentType.equals("application/schema+json")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							// Import schema JSON
							SchemaJsonImporter.importSchemaJson(source);
						}

					} else if (contentType.equals("application/x-structr-script")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							try {

								Actions.execute(securityContext, null, source, null, null);

							} catch (UnlicensedScriptException ex) {
								ex.log(logger);
							}
						}

						continue;

					} else if (contentType.equals("application/x-structr-javascript")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							try {

								Actions.execute(securityContext, null, source, null, null);

							} catch (UnlicensedScriptException ex) {
								ex.log(logger);
							}
						}

						continue;

					}

				} else if ("style".equals(tag)) {

					final PropertyKey<String> typeKey = StructrApp.key(Input.class, "_html_type");
					final String contentType          = newNode.getProperty(typeKey);

					if (contentType == null) {

						// Set default type of script tag to "text/css" to ensure inline CSS gets imported properly
						newNode.setProperty(typeKey, "text/css");
					}

					if ("text/css".equals(contentType)) {

						// parse content of style elements and add referenced files to list of resources to be downloaded
						for (final Node styleContentNode : node.childNodes()) {

							final String source = styleContentNode.toString();

							try {
								// Import referenced resources
								processCss(source, originalUrl);

							} catch (IOException ex) {
								logger.warn("Couldn't process CSS source", ex);
							}
						}
					}


				}

				if (instructions != null) {

					if (instructions.contains("@structr:content") && !(newNode instanceof Content)) {

						// unhandled instructions from previous iteration => empty content element
						createEmptyContentNode(page, parent, commentHandler, instructions);

					} else {

						// apply instructions to new DOM element
						if (commentHandler != null) {

							commentHandler.handleComment(page, newNode, instructions, true);

							if (newNodeProperties.containsKey(AbstractNode.id)) {

								// id of the newNode was changed => if a pagelink instruction was present, we need to update it because the node itself was not yet updated
								DeployCommand.updateDeferredPagelink(newNode.getUuid(), newNodeProperties.get(AbstractNode.id));
							}
						}
					}

					instructions = null;
				}

				// allow parent to be null to prevent direct child relationship
				if (parent != null) {

					// special handling for <head> elements
					if (newNode instanceof Head && parent instanceof Body) {

						final org.w3c.dom.Node html = parent.getParentNode();
						html.insertBefore(newNode, parent);

					} else {

						// don't do same page check or hierarchy check
						parent.getTemporaryStorage().put("import", true);

						if (!dontSetParent) {

							parent.appendChild(newNode);
						}
					}
				}

				// Link new node to its parent node
				// linkNodes(parent, newNode, page, localIndex);
				// Step down and process child nodes except for newly created templates
				if (!isNewTemplateOrComponent) {

					createChildNodes(node, newNode, page, removeHashAttribute, depth + 1);
				}
			}
		}

		// reset instructions when leaving a level
		if (instructions != null) {

			createEmptyContentNode(page, parent, commentHandler, instructions);

			instructions = null;
		}

		return rootElement;
	}

	/**
	 * Check whether a file with given path and checksum already exists
	 */
	private File fileExists(final String path, final long checksum) throws FrameworkException {

		final PropertyKey<Long> checksumKey = StructrApp.key(File.class, "checksum");
		final PropertyKey<String> pathKey   = StructrApp.key(File.class, "path");

		return app.nodeQuery(File.class).and(pathKey, path).and(checksumKey, checksum).getFirst();
	}

	private Linkable downloadFile(final String downloadAddress, final URL base) {

		URL downloadUrl = null;

		try {

			downloadUrl = new URL(base, downloadAddress);

		} catch (MalformedURLException ex) {

			logger.error("Could not resolve address {}", address != null ? address.concat("/") : "");
			return null;
		}

		final String alreadyDownloadedKey = downloadUrl.getPath();

		// Don't download the same file twice
		if (alreadyDownloaded.containsKey(alreadyDownloadedKey)) {
			return alreadyDownloaded.get(alreadyDownloadedKey);
		}

		long size;
		long checksum;
		String contentType;
		java.io.File tmpFile;

		try {
			// create temporary file on disk
			final Path tmpFilePath = Files.createTempFile("structr", "download");
			tmpFile                = tmpFilePath.toFile();

		} catch (IOException ioex) {

			logger.error("Unable to create temporary file for download, aborting.");
			return null;
		}

		try {

			logger.info("Starting download from {}", downloadUrl);

			copyURLToFile(downloadUrl.toString(), tmpFile);

		} catch (IOException ioe) {

			if (originalUrl == null || address == null) {

				logger.info("Cannot download from {} without base address", downloadAddress);
				return null;

			}

			logger.warn("Unable to download from {} {}", new Object[]{originalUrl, downloadAddress});

			try {
				// Try alternative baseUrl with trailing "/"
				if (address.endsWith("/")) {

					// don't append a second slash!
					logger.info("Starting download from alternative URL {} {} {}", new Object[]{originalUrl, address, downloadAddress});
					downloadUrl = new URL(new URL(originalUrl, address), downloadAddress);

				} else {

					// append a slash
					logger.info("Starting download from alternative URL {} {} {}", new Object[]{originalUrl, address.concat("/"), downloadAddress});
					downloadUrl = new URL(new URL(originalUrl, address.concat("/")), downloadAddress);
				}

				copyURLToFile(downloadUrl.toString(), tmpFile);

			} catch (MalformedURLException ex) {
				logger.error("Could not resolve address {}", address.concat("/"));
				return null;
			} catch (IOException ex) {
				logger.warn("Unable to download from {}", address.concat("/"));
				return null;
			}

			logger.info("Starting download from alternative URL {}", downloadUrl);

		}

		//downloadAddress = StringUtils.substringBefore(downloadAddress, "?");
		final String downloadName = cleanFileName(StringUtils.substringBefore(downloadUrl.getFile(), "?"));
		final String fileName     = PathHelper.getName(downloadName);

		if (StringUtils.isBlank(fileName)) {

			logger.warn("Can't figure out filename from download address {}, aborting.", downloadAddress);

			return null;
		}

		// TODO: Add security features like null/integrity/virus checking before copying it to
		// the files repo
		try {

			contentType = FileHelper.getContentMimeType(tmpFile, fileName);
			checksum    = FileHelper.getChecksum(tmpFile);
			size        = tmpFile.length();

		} catch (IOException ioe) {

			logger.warn("Unable to determine MIME type, size or checksum of {}", tmpFile);
			return null;
		}


		logger.info("Download URL: {}, address: {}, cleaned address: {}, filename: {}",
			new Object[]{downloadUrl, address, StringUtils.substringBeforeLast(address, "/"), fileName});

		String relativePath = StringUtils.substringAfter(downloadUrl.toString(), StringUtils.substringBeforeLast(address, "/"));
		if (StringUtils.isBlank(relativePath)) {

			relativePath = downloadAddress;
		}

		final String path;
		final String httpPrefix     = "http://";
		final String httpsPrefix    = "https://";
		final String flexiblePrefix = "//";

		if (downloadAddress.startsWith(httpsPrefix)) {

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, httpsPrefix)), "/");

		} else if (downloadAddress.startsWith(httpPrefix)) {

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, httpPrefix)), "/");

		} else if (downloadAddress.startsWith(flexiblePrefix)) {

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, flexiblePrefix)), "/");

		} else {

			path = StringUtils.substringBeforeLast(relativePath, "/");
		}


		logger.info("Relative path: {}, final path: {}", relativePath, path);

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(fileName, ".")), "text/plain");
		}

		try {

			final String fullPath = path + "/" + fileName;


			File fileNode = fileExists(PathHelper.removeRelativeParts(fullPath), checksum);
			if (fileNode == null) {

				if (ImageHelper.isImageType(fileName)) {

					fileNode = createImageNode(fullPath, contentType, size, checksum);

				} else {

					fileNode = createFileNode(fullPath, contentType, size, checksum);
				}

				final java.io.File imageFile = fileNode.getFileOnDisk(false);
				final Path imagePath         = imageFile.toPath();

				// rename / move file to final location
				Files.move(tmpFile.toPath(), imagePath);

				if (contentType.equals("text/css")) {

					processCssFileNode(fileNode, downloadUrl);
				}

				// set export flag according to user preference
				fileNode.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), includeInExport);

			} else {

				tmpFile.delete();
			}

			alreadyDownloaded.put(alreadyDownloadedKey, fileNode);
			return fileNode;

		} catch (final FrameworkException | IOException ex) {

			logger.warn("Could not create file node.", ex);

		}

		return null;

	}

	private File createFileNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return createFileNode(path, contentType, size, checksum, null);
	}

	private File createFileNode(final String path, final String contentType, final long size, final long checksum, final Class fileClass) throws FrameworkException {

		final PropertyKey<Integer> versionKey    = StructrApp.key(File.class, "version");
		final PropertyKey<Folder> parentKey      = StructrApp.key(File.class, "parent");
		final PropertyKey<String> contentTypeKey = StructrApp.key(File.class, "contentType");
		final PropertyKey<Long> checksumKey      = StructrApp.key(File.class, "checksum");
		final PropertyKey<Long> sizeKey          = StructrApp.key(File.class, "size");
		final Folder parentFolder                = FileHelper.createFolderPath(securityContext, PathHelper.getFolderPath(path));

		if (parentFolder != null) {

			// set export flag according to user preference
			parentFolder.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), includeInExport);
		}

		return app.create(fileClass != null ? fileClass : File.class,
			new NodeAttribute(AbstractNode.name, PathHelper.getName(path)),
			new NodeAttribute(parentKey,      parentFolder),
			new NodeAttribute(contentTypeKey, contentType),
			new NodeAttribute(sizeKey,        size),
			new NodeAttribute(checksumKey,    checksum),
			new NodeAttribute(versionKey,     1),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));
	}

	private Image createImageNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return (Image) createFileNode(path, contentType, size, checksum, Image.class);
	}

	private void processCssFileNode(final File fileNode, final URL base) throws IOException {

		final StringWriter sw = new StringWriter();

		try (final InputStream is = fileNode.getInputStream()) {
			IOUtils.copy(is, sw, "UTF-8");
		}

		processCss(sw.toString(), base);

	}

	private void processCss(final String css, final URL base) throws IOException {

		Pattern pattern = Pattern.compile("(url\\(['|\"]?)([^'|\"|)]*)");
		Matcher matcher = pattern.matcher(css);

		while (matcher.find()) {

			String url = matcher.group(2);

			logger.info("Trying to download from URL found in CSS: {}", url);
			downloadFile(url, base);

		}

		pattern = Pattern.compile("(@import\\s*([\"']|url\\('|url\\(\"))([^\"']*)");
		matcher = pattern.matcher(css);

		while (matcher.find()) {

			String url = matcher.group(3);

			logger.info("Trying to download file referenced by @import found in CSS: {}", url);
			downloadFile(url, base);

		}

	}

	private void copyURLToFile(final String address, final java.io.File fileOnDisk) throws IOException {

		try {

			HttpHelper.streamURLToFile(address, fileOnDisk);

		} catch (FrameworkException ex) {

			logger.warn(null, ex);

		}

	}

	public static DOMNode findSharedComponentByName(final String name) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			return null;
		}

		final PropertyKey<Page> ownerDocumentKey = StructrApp.key(DOMNode.class, "ownerDocument");
		final PropertyKey<DOMNode> parentKey     = StructrApp.key(DOMNode.class, "parent");

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(DOMNode.class).andName(name).and(ownerDocumentKey, CreateComponentCommand.getOrCreateHiddenDocument()).getAsList()) {

			// only return toplevel nodes in shared components
			if (n.getProperty(parentKey) == null) {
				return n;
			}
		}

		return null;
	}

	public static DOMNode findTemplateByName(final String name) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			return null;
		}

		final PropertyKey<DOMNode> sharedComponentKey = StructrApp.key(DOMNode.class, "sharedComponent");

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(Template.class).andName(name).and().notBlank(AbstractNode.name).getAsList()) {

			// IGNORE everything that REFERENCES a shared component!
			if (n.getProperty(sharedComponentKey) == null) {

				return n;
			}
		}

		return null;
	}

	private static String trimTrailingNewline(final String source) {

		String workString = source;

		// Leading and trailing whitespace may be replaced by a single space in HTML
		while (workString.startsWith("\n") || workString.startsWith("\r") || workString.startsWith("\t")) {
			workString = workString.substring(1);
		}

		while (workString.endsWith("\n") || workString.endsWith("\r") || workString.endsWith("\t")) {
			workString = workString.substring(0, workString.length() - 1);
		}

		return workString;
	}

	private Content createEmptyContentNode(final Page page, final DOMNode parent, final CommentHandler commentHandler, final String instructions) throws FrameworkException {

		final Content contentNode = (Content)page.createTextNode("");

		final PropertyMap emptyContentProperties = new PropertyMap();

		if (isDeployment && !relativeVisibility) {
			emptyContentProperties.put(AbstractNode.visibleToPublicUsers,        publicVisible);
			emptyContentProperties.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);
		} else {
			emptyContentProperties.put(AbstractNode.visibleToPublicUsers,        parent != null ? parent.getProperty(AbstractNode.visibleToPublicUsers) : publicVisible);
			emptyContentProperties.put(AbstractNode.visibleToAuthenticatedUsers, parent != null ? parent.getProperty(AbstractNode.visibleToAuthenticatedUsers) : authVisible);
		}

		contentNode.setProperties(securityContext, emptyContentProperties);

		if (parent != null) {

			parent.appendChild(contentNode);
		}

		if (commentHandler != null) {

			commentHandler.handleComment(page, contentNode, instructions, true);
		}

		return contentNode;
	}

	private Template createNewTemplateNode(final DOMNode parent, final List<Node> children) throws FrameworkException {

		final StringBuilder sb = new StringBuilder();

		for (final Node c : children) {
			sb.append(nodeToString(c));
		}

		return createNewTemplateNode(parent, sb.toString(), null);
	}

	private Template createNewTemplateNode(final DOMNode parent, final String content, final String contentType) throws FrameworkException {

		final PropertyKey<String> contentTypeKey = StructrApp.key(Content.class, "contentType");
		final PropertyKey<String> contentKey     = StructrApp.key(Content.class, "content");
		final PropertyMap map                    = new PropertyMap();

		map.put(AbstractNode.visibleToPublicUsers, publicVisible);
		map.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);
		map.put(contentTypeKey, contentType);
		map.put(contentKey,     content);

		final Template newTemplate = StructrApp.getInstance(securityContext).create(Template.class, map);

		if (parent != null) {
			parent.appendChild(newTemplate);
		}

		return newTemplate;

	}

	private DOMNode createSharedComponent(final Node node) throws FrameworkException {
		return createChildNodes(node, null, CreateComponentCommand.getOrCreateHiddenDocument());
	}

	private String nodeToString(Node node) {

		if (node instanceof TextNode) {

			return ((TextNode) node).getWholeText();

		} else if (node instanceof Element) {

			final Element el = (Element) node;

			final boolean prettyPrintBackup = el.ownerDocument().outputSettings().prettyPrint();

			el.ownerDocument().outputSettings().prettyPrint(false);

			final String result = el.outerHtml();

			el.ownerDocument().outputSettings().prettyPrint(prettyPrintBackup);

			return result;

		} else {

			return node.toString();

		}

	}

	private String cleanFileName(final String src) {

		String result = src;

		result = result.replace("?", ".");
		result = result.replace("#", ".");
		result = result.replace(",", ".");
		result = result.replace("=", ".");
		result = result.replace(":", ".");
		result = result.replace("+", ".");

		// replace multiple dots by a single dot
		result = result.replaceAll("\\.+", ".");

		return result;
	}
}
