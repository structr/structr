/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.CaseHelper;
import org.structr.common.PathHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
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
import org.structr.dynamic.File;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.Actions;
import org.structr.schema.importer.GraphGistImporter;
import org.structr.schema.importer.SchemaJsonImporter;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.diff.CreateOperation;
import org.structr.web.diff.DeleteOperation;
import org.structr.web.diff.InvertibleModificationOperation;
import org.structr.web.diff.MoveOperation;
import org.structr.web.diff.UpdateOperation;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Image;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Input;
import org.structr.web.maintenance.DeployCommand;
import org.structr.websocket.command.CreateComponentCommand;

//~--- classes ----------------------------------------------------------------
/**
 * The importer creates a new page by downloading and parsing markup from a URL.
 *
 *
 *
 */
public class Importer {

	private static final Logger logger = LoggerFactory.getLogger(Importer.class.getName());

	private static final Set<String> hrefElements       = new LinkedHashSet<>(Arrays.asList(new String[]{"link"}));
	private static final Set<String> ignoreElementNames = new LinkedHashSet<>(Arrays.asList(new String[]{"#declaration", "#doctype"}));
	private static final Set<String> srcElements        = new LinkedHashSet<>(Arrays.asList(new String[]{"img", "script", "audio", "video", "input", "source", "track"}));

	private static final Map<String, String> contentTypeForExtension = new HashMap<>();

	private static App app;
	private static ConfigurationProvider config;

	private final static String DATA_STRUCTR_PREFIX = "data-structr-";
	private final static String DATA_META_PREFIX    = "data-structr-meta-";

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js",  "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	private final StringBuilder commentSource = new StringBuilder();
	private final SecurityContext securityContext;
	private final boolean publicVisible;
	private final boolean authVisible;
	private CommentHandler commentHandler;
	private boolean isDeployment    = false;
	private Document parsedDocument = null;
	private final String name;
	private URL originalUrl;
	private String address;
	private String code;

	private Map<String, Linkable> alreadyDownloaded = new HashMap<>();

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
	 */
	public Importer(final SecurityContext securityContext, final String code, final String address, final String name, final boolean publicVisible, final boolean authVisible) {

		this.code            = code;
		this.address         = address;
		this.name            = name;
		this.securityContext = securityContext;
		this.publicVisible   = publicVisible;
		this.authVisible     = authVisible;
		this.config          = StructrApp.getConfiguration();

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

			if (!isDeployment) {
				logger.info("##### Start parsing code for page {} #####", new Object[]{name});
			} else {

				// a trailing slash to all void/self-closing tags so the XML parser can parse it correctly
				code = code.replaceAll("<(area|base|br|col|command|embed|hr|img|input|keygen|link|meta|param|source|track|wbr)([^>]*)>", "<$1$2/>");
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

					parsedDocument = Jsoup.parseBodyFragment(code);
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

		final Element head = parsedDocument.head();
		final Element body = parsedDocument.body();

		if (head != null && !head.html().isEmpty()) {

			// create Head element and append nodes to it
			final Head headElement = (Head)page.createElement("head");
			createChildNodes(head, headElement, page);

			// head is a special case
			return headElement;
		}

		if (body != null && !body.html().isEmpty()) {

			return createChildNodes(body, parent, page);
		}

		// fallback, no head no body => document is parent
		return createChildNodes(parsedDocument, parent, page);
	}

	public DOMNode createChildNodes(final DOMNode parent, final Page page) throws FrameworkException {

		return createChildNodes(parsedDocument.body(), parent, page);
	}

	public void createChildNodes(final DOMNode parent, final Page page, final boolean removeHashAttribute) throws FrameworkException {

		createChildNodes(parsedDocument.body(), parent, page, removeHashAttribute, 0);
	}

	public void createChildNodesWithHtml(final DOMNode parent, final Page page, final boolean removeHashAttribute) throws FrameworkException {

		createChildNodes(parsedDocument, parent, page, removeHashAttribute, 0);
	}

	public void importDataComments() throws FrameworkException {

		// try to import graph gist from comments
		final GraphGistImporter importer = app.command(GraphGistImporter.class);
		final byte[] data                = commentSource.toString().getBytes();
		final ByteArrayInputStream bis   = new ByteArrayInputStream(data);
		final List<String> sources       = importer.extractSources(bis);

		importer.importCypher(sources);
	}

	public void setIsDeployment(final boolean isDeployment) {
		this.isDeployment = isDeployment;
	}

	// ----- public static methods -----
	public static Page parsePageFromSource(final SecurityContext securityContext, final String source, final String name) throws FrameworkException {

		return parsePageFromSource(securityContext, source, name, false);
	}

	public static Page parsePageFromSource(final SecurityContext securityContext, final String source, final String name, final boolean removeHashAttribute) throws FrameworkException {

		final Importer importer = new Importer(securityContext, source, null, "source", false, false);
		final App localAppCtx = StructrApp.getInstance(securityContext);
		Page page = null;

		try (final Tx tx = localAppCtx.tx(true, false, false)) {

			page = localAppCtx.create(Page.class, new NodeAttribute<>(Page.name, name));

			if (importer.parse()) {

				importer.createChildNodesWithHtml(page, page, removeHashAttribute);
			}

			tx.success();

		}

		return page;
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
			String newHash = newNode.getProperty(DOMNode.dataHashProperty);
			if (newHash == null) {
				newHash = newNode.getIdHash();
			}

			// check for deleted nodes ignoring Page nodes
			if (!hashMappedExistingNodes.containsKey(newHash) && !(newNode instanceof Page)) {

				final DOMNode newParent = newNode.getProperty(DOMNode.parent);

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
						newParent = newNode.getProperty(DOMNode.parent);
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
						newParent = newNode.getProperty(DOMNode.parent);
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
		DOMNode nextSibling = node.getProperty(DOMNode.nextSibling);

		while (nextSibling != null) {

			siblingHashes.add(nextSibling.getIdHashOrProperty());
			nextSibling = nextSibling.getProperty(DOMNode.nextSibling);
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

		DOMNode rootElement     = null;
		Linkable res            = null;
		String instructions     = null;

		final List<Node> children = startNode.childNodes();
		for (Node node : children) {

			String tag = node.nodeName();

			// clean tag, remove non-word characters except : and #
			if (tag != null) {
				tag = tag.replaceAll("[^a-zA-Z0-9#:.-_]+", "");
			}

			final StringBuilder classString  = new StringBuilder();
			final String type                = CaseHelper.toUpperCamelCase(tag);
			String comment                   = null;
			String content                   = null;
			String id                        = null;
			boolean isNewTemplateOrComponent = false;

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

					if (downloadAddressAttr != null && StringUtils.isNotBlank(node.attr(downloadAddressAttr))) {

						String downloadAddress = node.attr(downloadAddressAttr);
						res = downloadFile(downloadAddress, originalUrl);
					}
				}

				if (removeHashAttribute) {

					// Remove data-structr-hash attribute
					node.removeAttr(DOMNode.dataHashProperty.jsonName());
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

					// create comment or content node
					if (!StringUtils.isBlank(comment)) {

						newNode = (DOMNode) page.createComment(comment);
						newNode.setProperty(org.structr.web.entity.dom.Comment.contentType, "text/html");

					} else {

						newNode = (Content) page.createTextNode(content);
					}
				}

			} else if ("structr:template".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode template = null;

					if (DeployCommand.isUuid(src)) {

						template = (DOMNode)StructrApp.getInstance().nodeQuery(NodeInterface.class).and(GraphObject.id, src).getFirst();

						if (template == null) {

							System.out.println("##################################### template with UUID " + src + " not found, this is a known bug");

						}

					} else {

						template = Importer.findSharedComponentByName(src);
						if (template == null) {

							template = Importer.findTemplateByName(src);

								if(template == null){

									template = createNewTemplateNode(parent, node.childNodes());
									isNewTemplateOrComponent = true;

								}
						}
					}

					if (template != null) {

						newNode = template;

						if (template.isSharedComponent()) {

							newNode = (DOMNode) template.cloneNode(false);
							newNode.setProperty(DOMNode.sharedComponent, template);
							newNode.setProperty(DOMNode.ownerDocument, page);

						} else if (page != null) {

							newNode.setProperty(DOMNode.ownerDocument, page);
						}

					} else {

						logger.warn("Unable to find template or shared component {}, template ignored!", src);
					}

				} else {

					logger.warn("Invalid template definition, missing src attribute!");
				}

			} else if ("structr:component".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode component = null;
					if (DeployCommand.isUuid(src)) {

						component = app.nodeQuery(DOMNode.class).and(GraphObject.id, src).getFirst();

					} else {

						component = Importer.findSharedComponentByName(src);
					}

					if(component == null){

						component = createSharedComponent(node);
					}

					isNewTemplateOrComponent = true;

					if (component != null) {

						newNode = (DOMNode) component.cloneNode(false);
						newNode.setProperty(DOMNode.sharedComponent, component);
						newNode.setProperty(DOMNode.ownerDocument, page);

					} else {

						logger.warn("Unable to find shared component {} - ignored!", src);
					}

				} else {

					logger.warn("Invalid component definition, missing src attribute!");
				}

			} else {

				if (page != null) {

					newNode = (org.structr.web.entity.dom.DOMElement) page.createElement(tag, true);
				}

				if (newNode == null) {

					// experimental: create DOM element with literal tag
					newNode = (DOMElement) app.create(DOMElement.class,
						new NodeAttribute(DOMElement.tag, node.nodeName()),
						new NodeAttribute(DOMElement.hideOnDetail, false),
						new NodeAttribute(DOMElement.hideOnIndex, false)
					);

					if (newNode != null && page != null) {
						newNode.doAdopt(page);
					}

					/* disabled / replaced by implementation above
					newNode = createNewHTMLTemplateNodeForUnsupportedTag(parent, node);
					isNewTemplateOrComponent = true;
					*/
				}
			}

			if (newNode != null) {

				// save root element for later use
				if (rootElement == null && !(newNode instanceof org.structr.web.entity.dom.Comment)) {
					rootElement = newNode;
				}

				// set linkable
				if (res != null) {
					newNode.setProperty(LinkSource.linkable, res);
				}

				// container for bulk setProperties()
				final PropertyMap newNodeProperties = new PropertyMap();
				final Class newNodeType             = newNode.getClass();

				newNodeProperties.put(AbstractNode.visibleToPublicUsers, publicVisible);
				newNodeProperties.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);

				// "id" attribute: Put it into the "_html_id" field
				if (StringUtils.isNotBlank(id)) {

					newNodeProperties.put(DOMElement._id, id);
				}

				if (StringUtils.isNotBlank(classString.toString())) {

					newNodeProperties.put(DOMElement._class, StringUtils.trim(classString.toString()));
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
											newNodeProperties.put(actualKey, convertedValue);

										} else {

											newNodeProperties.put(actualKey, value);
										}

									} else {

										logger.warn("Unknown meta property key {}, ignoring.", camelCaseKey);
									}
								}

							} else if (key.startsWith(DATA_STRUCTR_PREFIX)) { // don't convert data-structr-* attributes as they are internal

								final PropertyKey propertyKey = config.getPropertyKeyForJSONName(newNodeType, key);
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
								final PropertyKey propertyKey = new StringProperty(key);
								if (value != null) {

									newNodeProperties.put(propertyKey, value);
								}
							}

						} else {

							boolean notBlank = StringUtils.isNotBlank(value);
							boolean isAnchor = notBlank && value.startsWith("#");
							boolean isLocal = notBlank && !value.startsWith("http");
							boolean isActive = notBlank && value.contains("${");
							boolean isStructrLib = notBlank && value.startsWith("/structr/js/");

							if ("link".equals(tag) && "href".equals(key) && isLocal && !isActive && !isDeployment) {

								newNodeProperties.put(new StringProperty(PropertyView.Html.concat(key)), "${link.path}?${link.version}");

							} else if (("href".equals(key) || "src".equals(key)) && isLocal && !isActive && !isAnchor && !isStructrLib && !isDeployment) {

								newNodeProperties.put(new StringProperty(PropertyView.Html.concat(key)), "${link.path}");

							} else {

								newNodeProperties.put(new StringProperty(PropertyView.Html.concat(key)), value);
							}

						}
					}

				}

				// bulk set properties on new node
				newNode.setProperties(securityContext, newNodeProperties);

				if ("script".equals(tag)) {

					final String contentType = newNode.getProperty(Input._type);

					if (contentType == null) {

						// Set default type of script tag to "text/javascript" to ensure inline JS gets imported properly
						newNode.setProperty(Input._type, "text/javascript");

					} else if (contentType.equals("application/schema+json")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							// Import schema JSON
							SchemaJsonImporter.importSchemaJson(source);
						}

					} else if (contentType.equals("application/x-cypher")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							// import Cypher queries from script source
							final GraphGistImporter importer = app.command(GraphGistImporter.class);
							final List<String> sources       = new ArrayList<>();
							sources.add(source);

							importer.importCypher(sources);
						}

						continue;

					} else if (contentType.equals("application/x-structr-script")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							try {

								Actions.execute(securityContext, null, source, null);

							} catch (UnlicensedException ex) {
								ex.log(logger);
							}
						}

						continue;

					} else if (contentType.equals("application/x-structr-javascript")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							try {

								Actions.execute(securityContext, null, source, null);

							} catch (UnlicensedException ex) {
								ex.log(logger);
							}
						}

						continue;

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

						parent.appendChild(newNode);
					}
				}

				// Link new node to its parent node
				// linkNodes(parent, newNode, page, localIndex);
				// Step down and process child nodes except for newly created templates
				if(!isNewTemplateOrComponent){

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
	private FileBase fileExists(final String path, final long checksum) throws FrameworkException {
		return app.nodeQuery(FileBase.class).and(FileBase.path, path).and(File.checksum, checksum).getFirst();
	}

	private Linkable downloadFile(final String downloadAddress, final URL base) {

		// Don't download the same file twice
		if (alreadyDownloaded.containsKey(downloadAddress)) {
			return alreadyDownloaded.get(downloadAddress);
		}

		long size;
		long checksum;
		URL downloadUrl;
		String contentType;
		java.io.File tmpFile;

		try {
			// create temporary file on disk
			final Path tmpFilePath = Files.createTempFile("structr", "download");
			tmpFile = tmpFilePath.toFile();

		} catch (IOException ioex) {

			logger.error("Unable to create temporary file for download, aborting.");
			return null;
		}

		try {

			downloadUrl = new URL(base, downloadAddress);

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
		final String fileName = PathHelper.getName(downloadAddress);

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

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, httpsPrefix)), fileName);

		} else if (downloadAddress.startsWith(httpPrefix)) {

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, httpPrefix)), fileName);

		} else if (downloadAddress.startsWith(flexiblePrefix)) {

			path = StringUtils.substringBefore((StringUtils.substringAfter(downloadAddress, flexiblePrefix)), fileName);

		} else {

			path = StringUtils.substringBefore(relativePath, fileName);
		}


		logger.info("Relative path: {}, final path: {}", relativePath, path);

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(fileName, ".")), "text/plain");
		}

		try {

			final String fullPath = path + fileName;

			FileBase fileNode = fileExists(fullPath, checksum);
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

			} else {

				tmpFile.delete();
			}

			alreadyDownloaded.put(downloadAddress, fileNode);
			return fileNode;

		} catch (final FrameworkException | IOException ex) {

			logger.warn("Could not create file node.", ex);

		}

		return null;

	}

	private FileBase createFileNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return createFileNode(path, contentType, size, checksum, null);
	}

	private FileBase createFileNode(final String path, final String contentType, final long size, final long checksum, final Class fileClass) throws FrameworkException {

		return app.create(fileClass != null ? fileClass : File.class,
			new NodeAttribute(AbstractNode.name, PathHelper.getName(path)),
			new NodeAttribute(File.parent,  FileHelper.createFolderPath(securityContext, PathHelper.getFolderPath(path))),
			new NodeAttribute(File.contentType, contentType),
			new NodeAttribute(File.size, size),
			new NodeAttribute(File.checksum, checksum),
			new NodeAttribute(File.version, 1),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));
	}

	private Image createImageNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return (Image) createFileNode(path, contentType, size, checksum, Image.class);
	}

	private void processCssFileNode(final FileBase fileNode, final URL base) throws IOException {

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

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(DOMNode.class).andName(name).and(DOMNode.ownerDocument, CreateComponentCommand.getOrCreateHiddenDocument()).getAsList()) {

			// only return toplevel nodes in shared components
			if (n.getProperty(DOMNode.parent) == null) {
				return n;
			}
		}

		return null;
	}

	public static DOMNode findTemplateByName(final String name) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			return null;
		}

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(Template.class).andName(name).and().notBlank(AbstractNode.name).getAsList()) {

			// IGNORE everything that REFERENCES a shared component!
			if (n.getProperty(DOMNode.sharedComponent) == null) {

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

		contentNode.setVisibility(publicVisible, authVisible);

		if(parent != null){

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

		final PropertyMap map = new PropertyMap();

		map.put(AbstractNode.visibleToPublicUsers, publicVisible);
		map.put(AbstractNode.visibleToAuthenticatedUsers, authVisible);
		map.put(Content.contentType, contentType);
		map.put(Content.content, content);

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
}
