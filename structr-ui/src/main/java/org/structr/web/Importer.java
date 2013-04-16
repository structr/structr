/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import org.structr.common.CaseHelper;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.common.Path;
import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NewIndexNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

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
	private static NewIndexNodeCommand indexNode;
	private static SearchNodeCommand searchNode;

	//~--- static initializers --------------------------------------------

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js", "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	//~--- fields ---------------------------------------------------------

	private String code;
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
	public Importer(final SecurityContext securityContext, final String code, final String address, final String name, final int timeout, final boolean publicVisible, final boolean authVisible) {

		this.code            = code;
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
		indexNode  = Services.command(securityContext, NewIndexNodeCommand.class);

	}

	public boolean parse() throws FrameworkException {

		init();
		
		if (StringUtils.isNotBlank(code)) {

			logger.log(Level.INFO, "##### Start parsing code for page {0} #####", new Object[] { name });
			parsedDocument = Jsoup.parse(code);


		} else {

			logger.log(Level.INFO, "##### Start fetching {0} for page {1} #####", new Object[] { address, name });

			try {

				parsedDocument = Jsoup.parse(new URL(address), timeout);


			} catch (IOException ioe) {

				throw new FrameworkException(500, "Error while parsing content from " + address);

			}

		}

		return true;

	}

	public String readPage() throws FrameworkException {

		try {
			
			final URL baseUrl = StringUtils.isNotBlank(address) ? new URL(address) : null;

			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<String>() {

				@Override
				public String execute() throws FrameworkException {

					// AbstractNode page = findOrCreateNode(attrs, "/");
					Page page = Page.createNewPage(securityContext, name);

					if (page != null) {

						page.setVisibleToAuthenticatedUsers(authVisible);
						page.setVisibleToPublicUsers(publicVisible);
						createChildNodes(parsedDocument, page, page, baseUrl);
						logger.log(Level.INFO, "##### Finished fetching {0} for page {1} #####", new Object[] { address, name });

						return page.getProperty(AbstractNode.uuid);

					}

					return null;
				}

			});

		} catch (MalformedURLException ex) {

			logger.log(Level.SEVERE, "Could not resolve address " + address, ex);

		}

		return null;

	}

	public void createChildNodes(final Node startNode, final DOMNode parent, Page page, final URL baseUrl) throws FrameworkException {

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

				if (baseUrl != null && downloadAddressAttr != null && StringUtils.isNotBlank(node.attr(downloadAddressAttr))) {

					String downloadAddress = node.attr(downloadAddressAttr);

					downloadFiles(downloadAddress, baseUrl);

				}

			}

			// Data and comment nodes: Trim the text and put it into the "content" field without changes
			if (type.equals("#data") || type.equals("#comment")) {

				type    = "Content";
				tag     = "";
				content = ((DataNode) node).getWholeData().trim();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}

			}

			// Text-only nodes: Trim the text and put it into the "content" field
			if (type.equals("#text")) {

//                              type    = "Content";
				tag     = "";
				content = ((TextNode) node).toString().trim();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}
			}

			org.structr.web.entity.dom.DOMNode newNode;

			// create node
			if (StringUtils.isBlank(tag)) {

				newNode = (Content) page.createTextNode(content);
			} else {

				newNode = (org.structr.web.entity.dom.DOMElement) page.createElement(tag);
			}

			// "id" attribute: Put it into the "_html_id" field
			if (StringUtils.isNotBlank(id)) {

				newNode.setProperty(DOMElement._id, id);
			}

			if (StringUtils.isNotBlank(classString.toString())) {

				newNode.setProperty(DOMElement._class, StringUtils.trim(classString.toString()));
			}

			for (Attribute nodeAttr : node.attributes()) {

				String key = nodeAttr.getKey();

				// Don't add text attribute as _html_text because the text is already contained in the 'content' attribute
				if (!key.equals("text")) {

					newNode.setProperty(new StringProperty(PropertyView.Html.concat(nodeAttr.getKey())), nodeAttr.getValue());
				}

			}

			parent.appendChild(newNode);

			// Link new node to its parent node
			// linkNodes(parent, newNode, page, localIndex);
			// Step down and process child nodes
			createChildNodes(node, newNode, page, baseUrl);

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
	private DOMNode findExistingNode(final List<NodeAttribute> attrs, final String nodePath) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		for (NodeAttribute attr : attrs) {

			PropertyKey key = attr.getKey();
			String value    = attr.getValue().toString();

			// Exclude data attribute because it may contain code with special characters, too
			if (!key.equals(DOMElement._data)) {

				searchAttrs.add(Search.andExactProperty(key, value));
			}

		}

		Result<DOMNode> result = searchNode.execute(searchAttrs);

		if (result.size() > 1) {

			logger.log(Level.WARNING, "More than one node found.");
		}

		if (result.isEmpty()) {

			return null;
		}

		for (DOMNode foundNode : result.getResults()) {

			String foundNodePath = foundNode.getProperty(DOMElement.path);

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

	private DOMNode findOrCreateNode(final List<NodeAttribute> attributes, final String nodePath) throws FrameworkException {

		DOMNode node = findExistingNode(attributes, nodePath);

		if (node != null) {

			return node;
		}

		node = (DOMNode) createNode.execute(attributes);

		node.setProperty(DOMElement.path, nodePath);

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
		String relativeFilePath = org.structr.web.entity.File.getDirectoryPath(uuid) + "/" + uuid;
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

			indexNode.updateNode(folder);

		}

		return folder;

	}

	private File createFileNode(final String uuid, final String name, final String contentType) throws FrameworkException {

		String relativeFilePath = File.getDirectoryPath(uuid) + "/" + uuid;
		File fileNode           = (File) createNode.execute(new NodeAttribute(AbstractNode.uuid, uuid), new NodeAttribute(AbstractNode.type, File.class.getSimpleName()),
						  new NodeAttribute(AbstractNode.name, name), new NodeAttribute(File.relativeFilePath, relativeFilePath),
						  new NodeAttribute(File.contentType, contentType), new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
						  new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

//              fileNode.getChecksum();
//		indexNode.addNode(fileNode);

		return fileNode;

	}

	private Image createImageNode(final String uuid, final String name, final String contentType) throws FrameworkException {

		String relativeFilePath = Image.getDirectoryPath(uuid) + "/" + uuid;
		Image imageNode         = (Image) createNode.execute(new NodeAttribute(AbstractNode.uuid, uuid), new NodeAttribute(AbstractNode.type, Image.class.getSimpleName()),
						  new NodeAttribute(AbstractNode.name, name), new NodeAttribute(File.relativeFilePath, relativeFilePath),
						  new NodeAttribute(File.contentType, contentType), new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
						  new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

//              imageNode.getChecksum();
//		indexNode.addNode(imageNode);

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
