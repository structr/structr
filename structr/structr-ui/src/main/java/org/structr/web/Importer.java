/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web;

import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.IndexNodeCommand;
import org.structr.core.property.PropertyKey;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import org.structr.common.CaseHelper;
import org.structr.common.FileHelper;
import org.structr.common.ImageHelper;
import org.structr.common.Path;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.File;
import org.structr.core.entity.Folder;
import org.structr.core.entity.Image;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.HtmlElement;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.StringWriter;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.core.property.GenericProperty;
import org.structr.core.Result;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * The importer creates a new page by downloading and parsing markup from a URL.
 *
 * @author Axel Morgner
 */
public class Importer {

	private static String[] hrefElements                             = new String[] { "link" };
	private static String[] ignoreElementNames                       = new String[] { "#declaration", "#comment", "#doctype" };
	private static String[] srcElements                              = new String[] {

		"img", "script", "audio", "video", "input", "source", "track"
	};
	private static final Logger logger                               = Logger.getLogger(Importer.class.getName());
	private static final Map<String, String> contentTypeForExtension = new HashMap<String, String>();
	private static CreateNodeCommand createNode;
	private static CreateRelationshipCommand createRel;
	private static IndexNodeCommand indexNode;
	private static SearchNodeCommand searchNode;

	//~--- static initializers --------------------------------------------

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js", "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	//~--- fields ---------------------------------------------------------

	private String address;
	private boolean authVisible;
	private String name;
	private Document parsedDocument;
	private boolean publicVisible;
	private SecurityContext securityContext;
	private int timeout;

	//~--- constructors ---------------------------------------------------

//      public static void main(String[] args) throws Exception {
//              
//              String css = "background: url(\"/images/common/menu-bg.png\") repeat-x;\nbackground: url('/images/common/menu-bg.png') repeat-x;\nbackground: url(/images/common/menu-bg.png) repeat-x;";
//
//              Pattern pattern = Pattern.compile("(url\\(['|\"]?)([^'|\"|)]*)");
//              Matcher matcher = pattern.matcher(css);
//
//              while (matcher.find()) {
//
//
//                      String url = matcher.group(2);
//                      logger.log(Level.INFO, "Trying to download form URL found in CSS: {0}", url);
//
//
//              }
//              
//      }
	public Importer(final SecurityContext securityContext, final String address, final String name, final int timeout, final boolean publicVisible, final boolean authVisible) {

		this.address         = address;
		this.name            = name;
		this.timeout         = timeout;
		this.securityContext = securityContext;
		this.publicVisible   = publicVisible;
		this.authVisible     = authVisible;

	}

	//~--- methods --------------------------------------------------------

	public void init() {

		searchNode = Services.command(securityContext, SearchNodeCommand.class);
		createNode = Services.command(securityContext, CreateNodeCommand.class);
		createRel  = Services.command(securityContext, CreateRelationshipCommand.class);
		indexNode  = Services.command(securityContext, IndexNodeCommand.class);

	}

	public boolean parse() throws FrameworkException {

		logger.log(Level.INFO, "##### Start fetching {0} for page {1} #####", new Object[] { address, name });
		init();

		try {

			parsedDocument = Jsoup.parse(new URL(address), timeout);

			return true;

		} catch (IOException ioe) {

			throw new FrameworkException(500, "Error while importing content from " + address);

		}

	}

	public String readPage() throws FrameworkException {

		final URL baseUrl;

		try {

			baseUrl = new URL(address);

			AbstractNode res = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

					attrs.add(new NodeAttribute(AbstractNode.type, Page.class.getSimpleName()));
					attrs.add(new NodeAttribute(AbstractNode.name, name));
					attrs.add(new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible));
					attrs.add(new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

					AbstractNode page = findOrCreateNode(attrs, "/");

					createChildNodes(parsedDocument, page, page.getProperty(AbstractNode.uuid), baseUrl);

					return page;

				}

			});

			if (res != null) {

				logger.log(Level.INFO, "##### Finished fetching {0} for page {1} #####", new Object[] { address, name });

				return res.getProperty(AbstractNode.uuid);

			}

		} catch (MalformedURLException ex) {

			logger.log(Level.SEVERE, "Could not resolve address " + address, ex);

		}

		return null;

	}

	public void createChildNodes(final Node startNode, final AbstractNode parent, String pageId, final URL baseUrl) throws FrameworkException {

		List<Node> children = startNode.childNodes();
		int localIndex      = 0;

		for (Node node : children) {

			String tag                = node.nodeName();
			String type               = CaseHelper.toUpperCamelCase(tag);
			String content            = null;
			String id                 = null;
			StringBuilder classString = new StringBuilder();

			if (ArrayUtils.contains(ignoreElementNames, type)) {

				continue;
			}

			if (node instanceof Element) {

				Element el          = ((Element) node);
				Set<String> classes = el.classNames();

				for (String cls : classes) {

					classString.append(cls).append(" ");
				}

				id = el.id();

				String downloadAddressAttr = (ArrayUtils.contains(srcElements, tag)
							      ? "src"
							      : ArrayUtils.contains(hrefElements, tag)
								? "href"
								: null);

				if ((downloadAddressAttr != null) && StringUtils.isNotBlank(node.attr(downloadAddressAttr))) {

					String downloadAddress = node.attr(downloadAddressAttr);

					downloadFiles(downloadAddress, baseUrl);

				}

			}

			// Data and comment nodes: Just put the text into the "content" field without changes
			if (type.equals("#data") || type.equals("#comment")) {

				type    = "Content";
				tag     = "";
				content = ((DataNode) node).getWholeData();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}

			}

			// Text-only nodes: Put the text content into the "content" field
			if (type.equals("#text")) {

				type    = "Content";
				tag     = "";
				content = ((TextNode) node).toString();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}

			}

			List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

			// In case of a content node, put content into the "content" field
			if (content != null) {

				attrs.add(new NodeAttribute(Content.content, content));
			}

			// Type
			attrs.add(new NodeAttribute(AbstractNode.type, type));
			//attrs.add(new NodeAttribute(AbstractNode.name.name(), "New " + type));

			// Tag name
			attrs.add(new NodeAttribute(HtmlElement.tag, tag));

			// "id" attribute: Put it into the "_html_id" field
			if (StringUtils.isNotBlank(id)) {

				attrs.add(new NodeAttribute(HtmlElement._id, id));
			}

			// "class" attribute: Put it into the "_html_class" field
			if (StringUtils.isNotBlank(classString.toString())) {

				attrs.add(new NodeAttribute(HtmlElement._class, StringUtils.trim(classString.toString())));
			}

			// Other attributes: Put them into the respective fields with "_html_" prefix
			for (Attribute nodeAttr : node.attributes()) {

				String key = nodeAttr.getKey();

				// Don't add text attribute as _html_text because the text is already contained in the 'content' attribute
				if (!key.equals("text")) {

					attrs.add(new NodeAttribute(new StringProperty(PropertyView.Html.concat(nodeAttr.getKey())), nodeAttr.getValue()));
				}

			}

			String nodePath = getNodePath(node);

			// create node
			// TODO: Do we need the name here?
			AbstractNode newNode = findOrCreateNode(attrs, nodePath);

			// Link new node to its parent node
			linkNodes(parent, newNode, pageId, localIndex);

			// Step down and process child nodes
			createChildNodes(node, newNode, pageId, baseUrl);

			// Count up position index
			localIndex++;

		}

	}

	/**
	 * Find an existing node where the following matches:
	 *
	 * <ul>
	 * <li>attributes
	 * <li>parent node id
	 * </ul>
	 *
	 * @param attrs
	 * @param parentNodeId
	 * @return
	 * @throws FrameworkException
	 */
	private AbstractNode findExistingNode(final List<NodeAttribute> attrs, final String nodePath) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		for (NodeAttribute attr : attrs) {

			PropertyKey key = attr.getKey();

//                      String value = Search.escapeForLucene(attr.getValue().toString());
			String value = attr.getValue().toString();

//                      if (type.equals("Content") && key.equals(Content.UiKey.content.name())) {
//
//                              value = Search.escapeForLucene(value);
//
//                              // value = Search.clean(value);
//
//                      }
			// Exclude data attribute because it may contain code with special characters, too
			if (!key.equals(HtmlElement._data)) {

				searchAttrs.add(Search.andExactProperty(key, value));
			}

		}

		Result<AbstractNode> result = searchNode.execute(searchAttrs);

		if (result.size() > 1) {

			logger.log(Level.WARNING, "More than one node found.");
		}

		if (result.isEmpty()) {

			return null;
		}

		for (AbstractNode foundNode : result.getResults()) {
			
			String foundNodePath = foundNode.getProperty(HtmlElement.path);

			logger.log(Level.INFO, "Found a node with path {0}", foundNodePath);

			if (foundNodePath != null && foundNodePath.equals(nodePath)) {

				logger.log(Level.INFO, "MATCH!");

				// First match wins
				return foundNode;

			}

		}

		logger.log(Level.INFO, "Does not match expected path {0}", nodePath);

		return null;

	}

	private AbstractNode findOrCreateNode(final List<NodeAttribute> attributes, final String nodePath) throws FrameworkException {

		AbstractNode node = findExistingNode(attributes, nodePath);

		if (node != null) {

			return node;
		}

		node = (AbstractNode) createNode.execute(attributes);

		node.setProperty(HtmlElement.path, nodePath);

		if (node != null) {

			logger.log(Level.INFO, "Created node with name {0}, type {1}", new Object[] { node.getName(), node.getType() });
		} else {

			logger.log(Level.WARNING, "Could not create node");
		}

		return node;

	}

	private AbstractRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String pageId, int index) throws FrameworkException {

		AbstractRelationship rel   = (AbstractRelationship) createRel.execute(startNode, endNode, RelType.CONTAINS);
		PropertyKey pageIdProperty = new GenericProperty(pageId);

		rel.setProperty(pageIdProperty, index);

		return rel;

	}

	/**
	 * Check whether a file with given name and checksum already exists
	 */
	private boolean fileExists(final String name, final long checksum) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		searchAttrs.add(Search.andExactProperty(AbstractNode.name, name));
		searchAttrs.add(Search.andExactProperty(File.checksum, checksum));
		searchAttrs.add(Search.andExactTypeAndSubtypes(File.class.getSimpleName()));

		Result files = searchNode.execute(searchAttrs);

		return !files.isEmpty();

	}

	/**
	 * Return an eventually existing folder with given name,
	 * or create a new one.
	 */
	private Folder findOrCreateFolder(final String name) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		searchAttrs.add(Search.andExactProperty(AbstractNode.name, name));
		searchAttrs.add(Search.andExactType(Folder.class.getSimpleName()));

		Result folders = searchNode.execute(searchAttrs);

		if (!folders.isEmpty()) {

			return (Folder) folders.get(0);
		}

		return (Folder) createNode.execute(new NodeAttribute(AbstractNode.type, Folder.class.getSimpleName()), new NodeAttribute(AbstractNode.name, name));

	}

	private void downloadFiles(String downloadAddress, final URL baseUrl) {

		final String uuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
		String contentType;

		// Create temporary file with new uuid
		// FIXME: This is much too dangerous!
		String relativeFilePath = org.structr.core.entity.File.getDirectoryPath(uuid) + "/" + uuid;
		String filePath         = Services.getFilePath(Path.Files, relativeFilePath);
		java.io.File fileOnDisk = new java.io.File(filePath);

		fileOnDisk.getParentFile().mkdirs();

		URL downloadUrl = null;

		try {

			downloadUrl = new URL(baseUrl, downloadAddress);

			logger.log(Level.INFO, "Starting download from {0}", downloadUrl);

			// TODO: Add security features like null/integrity/virus checking before copying it to
			// the files repo
			FileUtils.copyURLToFile(downloadUrl, fileOnDisk);

		} catch (IOException ioe) {

			logger.log(Level.WARNING, "Unable to download from " + downloadAddress, ioe);

			return;

		}

		contentType     = FileHelper.getContentMimeType(fileOnDisk);
		downloadAddress = StringUtils.substringBefore(downloadAddress, "?");

		final String fileName = (downloadAddress.indexOf("/") > -1)
					? StringUtils.substringAfterLast(downloadAddress, "/")
					: downloadAddress;
		String httpPrefix     = "http://";
		String path           = StringUtils.substringBefore(((downloadAddress.indexOf(httpPrefix) > -1)
			? StringUtils.substringAfter(downloadAddress, "http://")
			: downloadAddress), fileName);

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(fileName, ".")), "text/plain");
		}

		final String ct = contentType;

		try {

			if (!(fileExists(fileName, FileUtils.checksumCRC32(fileOnDisk)))) {

				File fileNode;

				if (ImageHelper.isImageType(fileName)) {

					fileNode = createImageNode(uuid, fileName, ct);
				} else {

					fileNode = createFileNode(uuid, fileName, ct);
				}

				if (fileNode != null) {

					Folder parent = createFolderPath(path);

					if (parent != null) {

						createRel.execute(parent, fileNode, RelType.CONTAINS);
					}

					if (contentType.equals("text/css")) {

						processCssFileNode(fileNode, downloadUrl);
					}

				}

			} else {

				fileOnDisk.delete();
			}

		} catch (Exception fex) {

			logger.log(Level.WARNING, "Could not create file node.", fex);

		}

	}

	/**
	 * Create one folder per path item and return the last folder.
	 *
	 * F.e.: /a/b/c  => Folder["name":"a"] --HAS_CHILD--> Folder["name":"b"] --HAS_CHILD--> Folder["name":"c"],
	 * returns Folder["name":"c"]
	 *
	 * @param path
	 * @return
	 * @throws FrameworkException
	 */
	private Folder createFolderPath(final String path) throws FrameworkException {

		if (path == null) {

			return null;
		}

		String[] parts = StringUtils.split(path, "/");
		Folder folder  = null;

		for (String part : parts) {

			Folder parent = folder;

			folder = findOrCreateFolder(part);

			if (parent != null) {

				createRel.execute(parent, folder, RelType.CONTAINS);
			}

			indexNode.update(folder);

		}

		return folder;

	}

	private File createFileNode(final String uuid, final String name, final String contentType) throws FrameworkException {

		String relativeFilePath = File.getDirectoryPath(uuid) + "/" + uuid;
		File fileNode           = (File) createNode.execute(new NodeAttribute(AbstractNode.uuid, uuid), new NodeAttribute(AbstractNode.type, File.class.getSimpleName()),
					new NodeAttribute(AbstractNode.name, name), new NodeAttribute(File.relativeFilePath, relativeFilePath),
					new NodeAttribute(File.contentType, contentType), new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
					new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

//		fileNode.getChecksum();
		indexNode.add(fileNode);

		return fileNode;

	}

	private Image createImageNode(final String uuid, final String name, final String contentType) throws FrameworkException {

		String relativeFilePath = Image.getDirectoryPath(uuid) + "/" + uuid;
		Image imageNode         = (Image) createNode.execute(new NodeAttribute(AbstractNode.uuid, uuid), new NodeAttribute(AbstractNode.type, Image.class.getSimpleName()),
					  new NodeAttribute(AbstractNode.name, name), new NodeAttribute(File.relativeFilePath, relativeFilePath),
					  new NodeAttribute(File.contentType, contentType), new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
					  new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

//		imageNode.getChecksum();
		indexNode.add(imageNode);

		return imageNode;

	}

	private void processCssFileNode(File fileNode, final URL baseUrl) throws IOException {

		StringWriter sw = new StringWriter();

		IOUtils.copy(fileNode.getInputStream(), sw, "UTF-8");

		String css = sw.toString();

		processCss(css, baseUrl);

	}

	private void processCss(final String css, final URL baseUrl) throws IOException {

		Pattern pattern = Pattern.compile("(url\\(['|\"]?)([^'|\"|)]*)");
		Matcher matcher = pattern.matcher(css);

		while (matcher.find()) {

			String url = matcher.group(2);

			logger.log(Level.INFO, "Trying to download form URL found in CSS: {0}", url);
			downloadFiles(url, baseUrl);

		}

	}

	//~--- get methods ----------------------------------------------------

	private String getNodePath(final Node node) {

		Node n      = node;
		String path = "";

		while ((n.nodeName() != null) && !n.nodeName().equals("html")) {

			int index = n.siblingIndex();

			path = n.nodeName() + "[" + index + "]" + "/" + path;
			n    = n.parent();

		}

		return "html/" + path;

	}

}
