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
package org.structr.web.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.dynamic.File;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.ConfigurationProvider;
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
import org.structr.web.entity.Folder;
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

	private static final String[] hrefElements       = new String[]{"link"};
	private static final String[] ignoreElementNames = new String[]{"#declaration", "#doctype"};
	private static final String[] srcElements        = new String[]{"img", "script", "audio", "video", "input", "source", "track"};

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

		try {
			originalUrl = new URL(this.address);

		} catch (MalformedURLException ex) {}
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

			String type = CaseHelper.toUpperCamelCase(tag);
			String comment = null;
			String content = null;
			String id = null;
			StringBuilder classString = new StringBuilder();

			if (ArrayUtils.contains(ignoreElementNames, type)) {

				continue;
			}

			if (node instanceof Element) {

				Element el = ((Element) node);
				Set<String> classes = el.classNames();

				for (String cls : classes) {

					classString.append(cls).append(" ");
				}

				id = el.id();

				// do not download files when called from DeployCommand!
				if (!isDeployment) {

					String downloadAddressAttr = (ArrayUtils.contains(srcElements, tag)
						? "src" : ArrayUtils.contains(hrefElements, tag)
						? "href" : null);

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

						content = ((TextNode) node).getWholeText();

					} else {

						content = ((TextNode) node).text();
					}

					// Add content node for whitespace within <p> elements only
					if (!("p".equals(startNode.nodeName().toLowerCase())) && StringUtils.isWhitespace(content)) {

						continue;
					}
				}
			}

			org.structr.web.entity.dom.DOMNode newNode = null;

			// create node
			if (StringUtils.isBlank(tag)) {

				// create comment or content node
				if (!StringUtils.isBlank(comment)) {

					newNode = (DOMNode) page.createComment(comment);
					newNode.setProperty(org.structr.web.entity.dom.Comment.contentType, "text/html");

				} else {

					newNode = (Content) page.createTextNode(content);
				}

			} else if ("structr:template".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode template = null;

					if (DeployCommand.isUuid(src)) {

						template = (DOMNode) StructrApp.getInstance().nodeQuery(Template.class).and(GraphObject.id, src).getFirst();

					} else {

						template = Importer.findSharedComponentByName(src);
						if (template == null) {

							template = Importer.findTemplateByName(src);
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

				newNode = (org.structr.web.entity.dom.DOMElement) page.createElement(tag);
			}

			if (newNode != null) {

				// save root element for later use
				if (rootElement == null && !(newNode instanceof org.structr.web.entity.dom.Comment)) {
					rootElement = newNode;
				}

				newNode.setProperty(AbstractNode.visibleToPublicUsers, publicVisible);
				newNode.setProperty(AbstractNode.visibleToAuthenticatedUsers, authVisible);

				if (res != null) {

					newNode.setProperty(LinkSource.linkable, res);

				}

				// "id" attribute: Put it into the "_html_id" field
				if (StringUtils.isNotBlank(id)) {

					newNode.setProperty(DOMElement._id, id);
				}

				if (StringUtils.isNotBlank(classString.toString())) {

					newNode.setProperty(DOMElement._class, StringUtils.trim(classString.toString()));
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

									if (value.equalsIgnoreCase("true")) {

										newNode.setProperty(new BooleanProperty(camelCaseKey), true);

									} else if (value.equalsIgnoreCase("false")) {

										newNode.setProperty(new BooleanProperty(camelCaseKey), false);

									} else {

										newNode.setProperty(new StringProperty(camelCaseKey), nodeAttr.getValue());
									}
								}

							} else if (key.startsWith(DATA_STRUCTR_PREFIX)) { // don't convert data-structr-* attributes as they are internal

								final PropertyKey propertyKey = config.getPropertyKeyForJSONName(newNode.getClass(), key);
								if (propertyKey != null) {

									final PropertyConverter inputConverter = propertyKey.inputConverter(securityContext);
									if (value != null && inputConverter != null) {

										newNode.setProperty(propertyKey, propertyKey.inputConverter(securityContext).convert(value));
									} else {

										newNode.setProperty(propertyKey, value);
									}
								}

							} else {

								// store data-* attributes in node
								final PropertyKey propertyKey = new StringProperty(key);
								if (value != null) {

									newNode.setProperty(propertyKey, value);
								}
							}

						} else {

							boolean notBlank = StringUtils.isNotBlank(value);
							boolean isAnchor = notBlank && value.startsWith("#");
							boolean isLocal = notBlank && !value.startsWith("http");
							boolean isActive = notBlank && value.contains("${");
							boolean isStructrLib = notBlank && value.startsWith("/structr/js/");

							if ("link".equals(tag) && "href".equals(key) && isLocal && !isActive && !isDeployment) {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(key)), "${link.path}?${link.version}");

							} else if (("href".equals(key) || "src".equals(key)) && isLocal && !isActive && !isAnchor && !isStructrLib && !isDeployment) {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(key)), "${link.path}");

							} else {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(key)), value);
							}

						}
					}

				}

				final StringProperty typeKey = new StringProperty(PropertyView.Html.concat("type"));

				if ("script".equals(tag)) {

					final String contentType = newNode.getProperty(typeKey);

					if (contentType == null) {

						// Set default type of script tag to "text/javascript" to ensure inline JS gets imported properly
						newNode.setProperty(typeKey, "text/javascript");

					} else if (contentType.equals("application/schema+json")) {

						for (final Node scriptContentNode : node.childNodes()) {

							final String source = scriptContentNode.toString();

							// Import schema JSON
							SchemaJsonImporter.importSchemaJson(source);
						}
					}
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

				if (instructions != null) {

					if (commentHandler != null) {

						commentHandler.handleComment(page, newNode, instructions, true);
					}

					instructions = null;
				}

				// Link new node to its parent node
				// linkNodes(parent, newNode, page, localIndex);
				// Step down and process child nodes
				createChildNodes(node, newNode, page, removeHashAttribute, depth + 1);
			}
		}

		// reset instructions when leaving a level
		if (instructions != null) {

			final Content contentNode = (Content)page.createTextNode("");
			parent.appendChild(contentNode);

			if (commentHandler != null) {

				commentHandler.handleComment(page, contentNode, instructions, true);
			}

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

		final String uuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
		String contentType;

		// Create temporary file with new uuid
		final String relativeFilePath = File.getDirectoryPath(uuid) + "/" + uuid;
		final String filePath = FileHelper.getFilePath(relativeFilePath);
		final java.io.File fileOnDisk = new java.io.File(filePath);

		fileOnDisk.getParentFile().mkdirs();

		long size;
		long checksum;
		URL downloadUrl;

		try {

			downloadUrl = new URL(base, downloadAddress);

			logger.info("Starting download from {}", downloadUrl);

			copyURLToFile(downloadUrl.toString(), fileOnDisk);

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

				copyURLToFile(downloadUrl.toString(), fileOnDisk);

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

			contentType = FileHelper.getContentMimeType(fileOnDisk, fileName);
			size = fileOnDisk.length();
			checksum = FileUtils.checksumCRC32(fileOnDisk);

		} catch (IOException ioe) {

			logger.warn("Unable to determine MIME type, size or checksum of {}", fileOnDisk);
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


		logger.info("Relative path: {}, final path: {}", new Object[]{relativePath, path});

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(fileName, ".")), "text/plain");
		}

		final String ct = contentType;

		try {

			final String fullPath = path + fileName;

			FileBase fileNode = fileExists(fullPath, checksum);
			if (fileNode == null) {

				if (ImageHelper.isImageType(fileName)) {

					fileNode = createImageNode(uuid, fullPath, ct, size, checksum);
				} else {

					fileNode = createFileNode(uuid, fullPath, ct, size, checksum);
				}

				if (contentType.equals("text/css")) {

					processCssFileNode(fileNode, downloadUrl);
				}

			} else {

				fileOnDisk.delete();
			}

			alreadyDownloaded.put(downloadAddress, fileNode);
			return fileNode;

		} catch (final FrameworkException | IOException ex) {

			logger.warn("Could not create file node.", ex);

		}

		return null;

	}

	private FileBase createFileNode(final String uuid, final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return createFileNode(uuid, path, contentType, size, checksum, null);
	}

	private FileBase createFileNode(final String uuid, final String path, final String contentType, final long size, final long checksum, final Class fileClass) throws FrameworkException {

		final String name = PathHelper.getName(path);
		final String relativeFilePath = File.getDirectoryPath(uuid) + "/" + uuid;

		final FileBase fileNode = app.create(fileClass != null ? fileClass : File.class,
			new NodeAttribute(GraphObject.id, uuid),
			new NodeAttribute(AbstractNode.name, name),
			new NodeAttribute(File.relativeFilePath, relativeFilePath),
			new NodeAttribute(File.contentType, contentType),
			new NodeAttribute(File.size, size),
			new NodeAttribute(File.checksum, checksum),
			new NodeAttribute(File.version, 1),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

		final Folder parentFolder = FileHelper.createFolderPath(securityContext, PathHelper.getFolderPath(path));
		fileNode.setProperty(FileBase.parent, parentFolder);

		if (!fileNode.validatePath(securityContext, new ErrorBuffer())) {

			final String newName = name.concat("_").concat(FileHelper.getDateString());

			logger.warn("File {} already exists, renaming to {}", new Object[] { path, newName });

			fileNode.setProperty(AbstractNode.name, newName);
		}

		return fileNode;

	}

	private Image createImageNode(final String uuid, final String path, final String contentType, final long size, final long checksum) throws FrameworkException {

		return (Image) createFileNode(uuid, path, contentType, size, checksum, Image.class);

	}

	private void processCssFileNode(final FileBase fileNode, final URL base) throws IOException {

		StringWriter sw = new StringWriter();

		IOUtils.copy(fileNode.getInputStream(), sw, "UTF-8");

		String css = sw.toString();

		processCss(css, base);

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

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(DOMNode.class).andName(name).and(DOMNode.ownerDocument, CreateComponentCommand.getOrCreateHiddenDocument()).getAsList()) {

			// only return toplevel nodes in shared components
			if (n.getProperty(DOMNode.parent) == null) {
				return n;
			}
		}

		return null;
	}

	public static DOMNode findTemplateByName(final String name) throws FrameworkException {

		for (final DOMNode n : StructrApp.getInstance().nodeQuery(Template.class).andName(name).getAsList()) {

			// IGNORE everything that REFERENCES a shared component!
			if (n.getProperty(DOMNode.sharedComponent) == null) {

				return n;
			}
		}

		return null;
	}
}
