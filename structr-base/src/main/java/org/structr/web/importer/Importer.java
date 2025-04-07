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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.helper.CaseHelper;
import org.structr.common.helper.PathHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.Actions;
import org.structr.schema.importer.SchemaJsonImporter;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.*;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.html.Input;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.*;
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

		this.code               = code;
		this.address            = address;
		this.name               = name;
		this.securityContext    = securityContext;
		this.publicVisible      = publicVisible;
		this.authVisible        = authVisible;
		this.includeInExport    = includeInExport;
		this.relativeVisibility = relativeVisibility;

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

		if (StringUtils.isNotBlank(code) || StringUtils.isBlank(address)) {

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

			final Map<String, Object> responseData = HttpHelper.get(address);
			code = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : null;
			if (code != null) {

				parsedDocument = Jsoup.parse(code);
			} else {

				throw new FrameworkException(422, "Could not parse requested url for import. Response body is empty.");
			}
		}

		return true;

	}

	public Page readPage() throws FrameworkException {
		return readPage(null);
	}

	public Page readPage(final String uuid) throws FrameworkException {

		Page page = Page.createNewPage(securityContext, uuid, name);
		if (page != null) {

			page.setVisibility(publicVisible, authVisible);

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
			final DOMElement headElement = page.createElement("head");

			headElement.setVisibility(publicVisible, authVisible);

			createChildNodes(head, headElement, page);

			// head is a special case
			return headElement;
		}

		if (body != null && !body.html().isEmpty()) {

			// create Head element and append nodes to it
			final DOMElement bodyElement = page.createElement("body");

			bodyElement.setVisibility(publicVisible, authVisible);

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

	public DOMNode createComponentHullChildNodes (final NodeInterface parent, final Page page) throws FrameworkException {

		for (final Node node : parsedDocument.childNodes()) {

			final String tag  = node.nodeName();
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
		NodeInterface node = null;

		try (final Tx tx = localAppCtx.tx(true, false, false)) {

			node = localAppCtx.create(StructrTraits.PAGE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));

			if (importer.parse()) {

				final Page page = node.as(Page.class);

				importer.createChildNodesWithHtml(page, page, removeHashAttribute);
			}

			tx.success();

			return node.as(Page.class);

		} catch (Throwable t) {
			logger.warn("Unable to parse source:\n\n" + source);
			return null;
		}

	}

	// ----- private methods -----
	private DOMNode createChildNodes(final Node startNode, final DOMNode parent, final Page page) throws FrameworkException {
		return createChildNodes(startNode, parent, page, false, 0);
	}

	private DOMNode createChildNodes(final Node startNode, final DOMNode parent, final Page page, final boolean removeHashAttribute, final int depth) throws FrameworkException {
		return createChildNodes(startNode, parent, page, false, 0, null);
	}

	private DOMNode createChildNodes(final Node startNode, final NodeInterface parent, final Page page, final boolean removeHashAttribute, final int depth, final NodeInterface suppliedRoot) throws FrameworkException {

		NodeInterface rootElement = suppliedRoot;
		Linkable linkable         = null;
		String instructions       = null;

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
					node.removeAttr(DOMNodeTraitDefinition.DATA_STRUCTR_HASH_PROPERTY);
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

			DOMNode newNode = null;

			// create node
			if (StringUtils.isBlank(tag)) {

				if (page != null) {

					final PropertyKey<String> contentTypeKey = Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY);

					// create comment or content node
					if (!StringUtils.isBlank(comment)) {

						newNode = page.createComment(DOMNode.unescapeForHtml(comment));
						newNode.setProperty(contentTypeKey, "text/html");

					} else {

						newNode = page.createTextNode(content);

						final PropertyKey<String> typeKey = Traits.of(StructrTraits.INPUT).key(Input.TYPE_PROPERTY);

						if (parent != null && "text/css".equals(parent.getProperty(typeKey))) {
							newNode.setProperty(contentTypeKey, "text/css");
						}
					}
				}
			} else if ("svg".equals(tag)) { // don't create elements for SVG

				final String source = node.toString();
				newNode = page.createTextNode(source);

				final PropertyKey<String> contentTypeKey = Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY);
				newNode.setProperty(contentTypeKey, "text/xml");

			} else if ("structr:template".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					NodeInterface template = null;

					if (DeployCommand.isUuid(src)) {

						template = StructrApp.getInstance().nodeQuery(StructrTraits.NODE_INTERFACE).and(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), src).getFirst();

						if (template == null) {

							logger.warn("##################################### template with UUID {} not found, this is a known bug", src);
						}

					} else {

						final String uuidAtEnd = DeployCommand.getUuidOrNullFromEndOfString(src);
						if (uuidAtEnd != null) {

							template = StructrApp.getInstance().nodeQuery(StructrTraits.NODE_INTERFACE).and(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuidAtEnd).getFirst();

							if (template == null) {

								logger.warn("##################################### template with UUID not found, this is a known bug", uuidAtEnd);
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
					}

					if (template != null) {

						newNode = template.as(DOMNode.class);

						if (newNode.isSharedComponent()) {

							// save previous reference
							final DOMNode prevNode = newNode;

							newNode = newNode.cloneNode(false);

							newNode.setSharedComponent(prevNode);
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

					NodeInterface template = Importer.findSharedComponentByName(name);
					if (template == null) {

						newNode = createNewTemplateNode(null, node.childNodes()).as(DOMNode.class);

						isNewTemplateOrComponent = true;
						dontSetParent            = true;

						// create shared component (and nothing else) from this template
						newNode.setOwnerDocument(CreateComponentCommand.getOrCreateHiddenDocument());
						newNode.setName(name);
					}

				} else {

					logger.warn("Invalid shared template definition, missing name attribute!");
				}

			} else if ("structr:component".equals(tag)) {

				final String src = node.attr("src");
				if (src != null) {

					DOMNode component = null;
					if (DeployCommand.isUuid(src)) {

						final NodeInterface n = app.nodeQuery(StructrTraits.DOM_NODE).and(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), src).getFirst();
						if (n != null) {

							component = n.as(DOMNode.class);
						}

					} else {

						final String uuidAtEnd = DeployCommand.getUuidOrNullFromEndOfString(src);

						if (uuidAtEnd != null) {

							final NodeInterface n = app.nodeQuery(StructrTraits.DOM_NODE).and(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuidAtEnd).getFirst();
							if (n != null) {

								component = n.as(DOMNode.class);
							}

						} else {

							final NodeInterface n = Importer.findSharedComponentByName(src);
							if (n != null) {

								component = n.as(DOMNode.class);
							}
						}
					}

					if (component == null) {

						component = createSharedComponent(node);
					}

					isNewTemplateOrComponent = true;

					if (component != null) {

						newNode = component.as(DOMNode.class).cloneNode(false);

						final String _html_src = newNode.getProperty(new StringProperty("_html_src"));
						if (!StringUtils.isEmpty(_html_src)) {
							node.attr("src", _html_src);
						} else {
							node.removeAttr("src");
						}

						newNode.setSharedComponent(component.as(DOMNode.class));
						newNode.setOwnerDocument(page);

					} else {

						logger.warn("Unable to find shared component '{}' - ignored! This should not happen.", src);
					}

				} else {

					logger.warn("Invalid component definition, missing src attribute!");
				}

			} else {

				if (page != null) {

					newNode = page.createElement(tag, true);
				}

			}

			if (newNode != null) {

				// save root element for later use
				if (rootElement == null && !(newNode.is(StructrTraits.COMMENT))) {
					rootElement = newNode;
				}

				// set linkable
				if (linkable != null && newNode.is(StructrTraits.LINK_SOURCE)) {
					newNode.as(LinkSource.class).setLinkable(linkable);
				}

				// container for bulk setProperties()
				final PropertyMap newNodeProperties      = new PropertyMap();
				final PropertyMap deferredNodeProperties = new PropertyMap();
				final Traits newNodeType                 = newNode.getTraits();
				final PropertyKey<Boolean> vtpuKey       = newNodeType.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
				final PropertyKey<Boolean> vtauKey       = newNodeType.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);

				if (isDeployment && !relativeVisibility) {

					newNodeProperties.put(vtpuKey, publicVisible);
					newNodeProperties.put(vtauKey, authVisible);

				} else {

					newNodeProperties.put(vtpuKey, parent != null ? parent.getProperty(vtpuKey) : publicVisible);
					newNodeProperties.put(vtauKey, parent != null ? parent.getProperty(vtauKey) : authVisible);
				}

				// "id" attribute: Put it into the "_html_id" field
				if (StringUtils.isNotBlank(id)) {

					newNodeProperties.put(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_ID_PROPERTY), id);
				}

				if (StringUtils.isNotBlank(classString.toString())) {

					newNodeProperties.put(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition._HTML_CLASS_PROPERTY), StringUtils.trim(classString.toString()));
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
									if (newNodeType.hasKey(camelCaseKey)) {

										final PropertyKey actualKey       = newNodeType.key(camelCaseKey);
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

								if (newNodeType.hasKey(key)) {

									final PropertyKey propertyKey = newNodeType.key(key);
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

					final PropertyKey<String> typeKey = Traits.of(StructrTraits.INPUT).key(Input.TYPE_PROPERTY);
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

					final PropertyKey<String> typeKey = Traits.of(StructrTraits.INPUT).key(Input.TYPE_PROPERTY);
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

					if (instructions.contains("@structr:content") && !(newNode.is(StructrTraits.CONTENT))) {

						// unhandled instructions from previous iteration => empty content element
						createEmptyContentNode(page, parent, commentHandler, instructions);

					} else {

						// apply instructions to new DOM element
						if (commentHandler != null) {

							commentHandler.handleComment(page, newNode, instructions, true);

							if (newNodeProperties.containsKey(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY))) {

								// id of the newNode was changed => if a pagelink instruction was present, we need to update it because the node itself was not yet updated
								DeployCommand.updateDeferredPagelink(newNode.getUuid(), newNodeProperties.get(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY)));
							}
						}
					}

					instructions = null;
				}

				// allow parent to be null to prevent direct child relationship
				if (parent != null) {

					final DOMNode parentDOMNode = parent.as(DOMNode.class);

					// special handling for <head> elements
					if (newNode.is("Head") && parent.is("Body")) {

						final DOMNode html = parentDOMNode.getParent();
						html.insertBefore(newNode, parentDOMNode);

					} else {

						// don't do same page check or hierarchy check
						parent.getTemporaryStorage().put("import", true);

						if (!dontSetParent) {

							parentDOMNode.appendChild(newNode);
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
		}

		if (rootElement != null) {
			return rootElement.as(DOMNode.class);
		}

		return null;
	}

	/**
	 * Check whether a file with given path and checksum already exists
	 */
	private File fileExists(final String path, final long checksum) throws FrameworkException {

		final PropertyKey<Long> checksumKey = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CHECKSUM_PROPERTY);
		final PropertyKey<String> pathKey   = Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY);

		final NodeInterface node = app.nodeQuery(StructrTraits.FILE).and(pathKey, path).and(checksumKey, checksum).getFirst();
		if (node != null) {

			return node.as(File.class);
		}

		return null;
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

				try (final FileInputStream is = new FileInputStream(tmpFile); final OutputStream os = StorageProviderFactory.getStorageProvider(fileNode).getOutputStream()) {
					// Copy contents of tmpFile to file in structr fs
					IOUtils.copy(is, os);

					if (contentType.equals("text/css")) {

						processCssFileNode(fileNode, downloadUrl);
					}

					// set export flag according to user preference
					fileNode.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), includeInExport);
				}

			} else {

				tmpFile.delete();
			}

			final Linkable linkable = fileNode.as(Linkable.class);

			alreadyDownloaded.put(alreadyDownloadedKey, linkable);

			return linkable;

		} catch (final FrameworkException | IOException ex) {

			logger.warn("Could not create file node.", ex);

		}

		return null;

	}

	private File createFileNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return createFileNode(path, contentType, size, checksum, null);
	}

	private File createFileNode(final String path, final String contentType, final long size, final long checksum, final String fileClass) throws FrameworkException {

		final PropertyKey<Integer> versionKey      = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.VERSION_PROPERTY);
		final PropertyKey<NodeInterface> parentKey = Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<String> contentTypeKey   = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CONTENT_TYPE_PROPERTY);
		final PropertyKey<Long> checksumKey        = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CHECKSUM_PROPERTY);
		final PropertyKey<Long> sizeKey            = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.SIZE_PROPERTY);
		final NodeInterface parentFolder           = FileHelper.createFolderPath(securityContext, PathHelper.getFolderPath(path));
		final Traits traits                        = Traits.of(fileClass != null ? fileClass : StructrTraits.FILE);

		if (parentFolder != null) {

			// set export flag according to user preference
			parentFolder.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), includeInExport);
		}

		return app.create(traits.getName(),
			new NodeAttribute(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                         PathHelper.getName(path)),
			new NodeAttribute(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY),                        parentFolder),
			new NodeAttribute(traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY),                          contentType),
			new NodeAttribute(traits.key(FileTraitDefinition.SIZE_PROPERTY),                                  size),
			new NodeAttribute(traits.key(FileTraitDefinition.CHECKSUM_PROPERTY),                              checksum),
			new NodeAttribute(traits.key(FileTraitDefinition.VERSION_PROPERTY),                               1),
			new NodeAttribute(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        publicVisible),
			new NodeAttribute(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), authVisible)

		).as(File.class);
	}

	private Image createImageNode(final String path, final String contentType, final long size, final long checksum) throws FrameworkException {
		return createFileNode(path, contentType, size, checksum, StructrTraits.IMAGE).as(Image.class);
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

	public static NodeInterface findSharedComponentByName(final String name) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			return null;
		}

		final PropertyKey<NodeInterface> ownerDocumentKey = Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.OWNER_DOCUMENT_PROPERTY);
		final PropertyKey<NodeInterface> parentKey        = Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.PARENT_PROPERTY);
		final ShadowDocument shadowDocument               = CreateComponentCommand.getOrCreateHiddenDocument();

		for (final NodeInterface n : StructrApp.getInstance().nodeQuery(StructrTraits.DOM_NODE).andName(name).and(ownerDocumentKey, shadowDocument).getAsList()) {

			// only return toplevel nodes in shared components
			if (n.getProperty(parentKey) == null) {
				return n;
			}
		}

		return null;
	}

	public static NodeInterface findTemplateByName(final String name) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			return null;
		}

		final Traits traits                                 = Traits.of(StructrTraits.DOM_NODE);
		final PropertyKey<NodeInterface> sharedComponentKey = traits.key(DOMNodeTraitDefinition.SHARED_COMPONENT_PROPERTY);
		final PropertyKey<String> nameKey                   = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		for (final NodeInterface n : StructrApp.getInstance().nodeQuery(StructrTraits.TEMPLATE).andName(name).and().notBlank(nameKey).getAsList()) {

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

	private Content createEmptyContentNode(final Page page, final NodeInterface parent, final CommentHandler commentHandler, final String instructions) throws FrameworkException {

		final Content contentNode                = page.createTextNode("");
		final Traits traits                      = contentNode.getTraits();
		final PropertyMap emptyContentProperties = new PropertyMap();

		if (isDeployment && !relativeVisibility) {

			emptyContentProperties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        publicVisible);
			emptyContentProperties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), authVisible);

		} else {

			emptyContentProperties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        parent != null ? parent.getProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)) :        publicVisible);
			emptyContentProperties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), parent != null ? parent.getProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)) : authVisible);
		}

		contentNode.setProperties(securityContext, emptyContentProperties);

		if (parent != null) {

			parent.as(DOMNode.class).appendChild(contentNode);
		}

		if (commentHandler != null) {

			commentHandler.handleComment(page, contentNode, instructions, true);
		}

		return contentNode;
	}

	private NodeInterface createNewTemplateNode(final NodeInterface parent, final List<Node> children) throws FrameworkException {

		final StringBuilder sb = new StringBuilder();

		for (final Node c : children) {
			sb.append(nodeToString(c));
		}

		return createNewTemplateNode(parent, sb.toString(), null);
	}

	private NodeInterface createNewTemplateNode(final NodeInterface parent, final String content, final String contentType) throws FrameworkException {

		final Traits traits                      = Traits.of(StructrTraits.TEMPLATE);
		final PropertyKey<String> contentTypeKey = traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY);
		final PropertyKey<String> contentKey     = traits.key(ContentTraitDefinition.CONTENT_PROPERTY);
		final PropertyKey<Boolean> vtpuKey       = traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
		final PropertyKey<Boolean> vtauKey       = traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);
		final PropertyMap map                    = new PropertyMap();

		map.put(vtpuKey,        publicVisible);
		map.put(vtauKey,        authVisible);
		map.put(contentTypeKey, contentType);
		map.put(contentKey,     content);

		final NodeInterface newTemplate = StructrApp.getInstance(securityContext).create(StructrTraits.TEMPLATE, map);

		if (parent != null) {
			parent.as(DOMNode.class).appendChild(newTemplate.as(Template.class));
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
