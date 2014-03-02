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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.core.graph.NodeAttribute;
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
import org.apache.commons.lang.WordUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.BooleanProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.relation.Files;
import org.structr.web.entity.relation.Folders;

//~--- classes ----------------------------------------------------------------
/**
 * The importer creates a new page by downloading and parsing markup from a URL.
 *
 * @author Axel Morgner
 */
public class Importer {

	private static String[] hrefElements = new String[]{"link"};
	private static String[] ignoreElementNames = new String[]{"#declaration", "#comment", "#doctype"};
	private static String[] srcElements = new String[]{
		"img", "script", "audio", "video", "input", "source", "track"
	};
	private static final Logger logger = Logger.getLogger(Importer.class.getName());
	private static final Map<String, String> contentTypeForExtension = new HashMap<String, String>();

	private static App app;

	private final static String DATA_META_PREFIX = "data-structr-meta-";

	//~--- static initializers --------------------------------------------
	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js", "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	//~--- fields ---------------------------------------------------------
	private String code;
	private final String address;
	private final boolean authVisible;
	private final String name;
	private Document parsedDocument;
	private final boolean publicVisible;
	private final SecurityContext securityContext;
	private final int timeout;

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

		this.code = code;
		this.address = address;
		this.name = name;
		this.timeout = timeout;
		this.securityContext = securityContext;
		this.publicVisible = publicVisible;
		this.authVisible = authVisible;

	}

	//~--- methods --------------------------------------------------------
	public void init() {
		app = StructrApp.getInstance(securityContext);
//		searchNode = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class);
//		createNode = StructrApp.getInstance(securityContext).command(CreateNodeCommand.class);
//		createRel  = StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class);
	}

	public boolean parse() throws FrameworkException {

		return parse(false);

	}

	public boolean parse(final boolean fragment) throws FrameworkException {

		init();

		if (StringUtils.isNotBlank(code)) {

			logger.log(Level.INFO, "##### Start parsing code for page {0} #####", new Object[]{name});

			if (fragment) {

				parsedDocument = Jsoup.parseBodyFragment(code);

			} else {

				parsedDocument = Jsoup.parse(code);

			}

		} else {

			logger.log(Level.INFO, "##### Start fetching {0} for page {1} #####", new Object[]{address, name});

			try {

				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(address);
				get.setHeader("User-Agent", "Mozilla");
				get.setHeader("Connection", "close");

				// Skip BOM to workaround this Jsoup bug: https://github.com/jhy/jsoup/issues/348
				code = IOUtils.toString(client.execute(get).getEntity().getContent(), "UTF-8");

				if (code.charAt(0) == 65279) {
					code = code.substring(1);
				}

				parsedDocument = Jsoup.parse(code);

//				parsedDocument = Jsoup.connect(address)
//					.userAgent("Mozilla")
//					.timeout(timeout)
//					.get().normalise();
			} catch (IOException ioe) {

				throw new FrameworkException(500, "Error while parsing content from " + address);

			}

		}

		return true;

	}

	public String readPage() throws FrameworkException {

		try {
			final URL baseUrl = StringUtils.isNotBlank(address) ? new URL(address) : null;

			// AbstractNode page = findOrCreateNode(attrs, "/");
			Page page = Page.createNewPage(securityContext, name);

			String pageId = null;
			if (page != null) {

				page.setProperty(AbstractNode.visibleToAuthenticatedUsers, authVisible);
				page.setProperty(AbstractNode.visibleToPublicUsers, publicVisible);
				createChildNodes(parsedDocument, page, page, baseUrl);
				logger.log(Level.INFO, "##### Finished fetching {0} for page {1} #####", new Object[]{address, name});

				pageId = page.getProperty(GraphObject.id);
			}

			return pageId;

		} catch (MalformedURLException ex) {

			logger.log(Level.SEVERE, "Could not resolve address " + address, ex);

		}
		return null;

	}

	public void createChildNodes(final DOMNode parent, final Page page, final String baseUrl) throws FrameworkException {

		try {

			createChildNodes(parsedDocument.body(), parent, page, new URL(baseUrl));

		} catch (MalformedURLException ex) {

			logger.log(Level.WARNING, "Could not read URL {1}, calling method without base URL", baseUrl);

			createChildNodes(parsedDocument.body(), parent, page, null);

		}
	}

	// ----- private methods -----
	private void createChildNodes(final Node startNode, final DOMNode parent, Page page, final URL baseUrl) throws FrameworkException {

		Linkable res = null;
		final List<Node> children = startNode.childNodes();
		for (Node node : children) {

			String tag = node.nodeName();
			String type = CaseHelper.toUpperCamelCase(tag);
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

				String downloadAddressAttr = (ArrayUtils.contains(srcElements, tag)
					? "src" : ArrayUtils.contains(hrefElements, tag)
					? "href" : null);

				if (baseUrl != null && downloadAddressAttr != null && StringUtils.isNotBlank(node.attr(downloadAddressAttr))) {

					String downloadAddress = node.attr(downloadAddressAttr);
					res = downloadFile(downloadAddress, baseUrl);

				}

			}

			// Data and comment nodes: Trim the text and put it into the "content" field without changes
			if (type.equals("#data") || type.equals("#comment")) {

				type = "Content";
				tag = "";
				content = ((DataNode) node).getWholeData().trim();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;
				}

			}

			// Text-only nodes: Trim the text and put it into the "content" field
			if (type.equals("#text")) {

//                              type    = "Content";
				tag = "";
				//content = ((TextNode) node).getWholeText();
				content = ((TextNode) node).text();

				// Add content node for whitespace within <p> elements only
				if (!("p".equals(startNode.nodeName().toLowerCase())) && StringUtils.isWhitespace(content)) {

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

			if (newNode != null) {

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

					String key = nodeAttr.getKey();

					// Don't add text attribute as _html_text because the text is already contained in the 'content' attribute
					if (!key.equals("text")) {

						// convert data-* attributes to local camel case properties on the node,
						// but don't convert data-structr-* attributes as they are internal
						if (key.startsWith("data-")) {

							if (!key.startsWith(DATA_META_PREFIX)) {

								newNode.setProperty(new StringProperty(nodeAttr.getKey()), nodeAttr.getValue());

							} else {

								int l = DATA_META_PREFIX.length();

								String upperCaseKey = WordUtils.capitalize(key.substring(l), new char[]{'-'}).replaceAll("-", "");
								String camelCaseKey = key.substring(l, l + 1).concat(upperCaseKey.substring(1));

								String value = nodeAttr.getValue();

								if (value != null) {
									if (value.equalsIgnoreCase("true")) {
										newNode.setProperty(new BooleanProperty(camelCaseKey), true);
									} else if (value.equalsIgnoreCase("false")) {
										newNode.setProperty(new BooleanProperty(camelCaseKey), false);
									} else {
										newNode.setProperty(new StringProperty(camelCaseKey), nodeAttr.getValue());
									}
								}

							}

						} else {

							if ("link".equals(tag) && "href".equals(key) && nodeAttr.getValue() == null) {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(key)), "${link.path}?${link.version}");

							} else if (("href".equals(key) || "src".equals(key)) && nodeAttr.getValue() == null) {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(key)), "${link.path}");

							} else {

								newNode.setProperty(new StringProperty(PropertyView.Html.concat(nodeAttr.getKey())), nodeAttr.getValue());
							}

						}
					}

				}

				parent.appendChild(newNode);

				// Link new node to its parent node
				// linkNodes(parent, newNode, page, localIndex);
				// Step down and process child nodes
				createChildNodes(node, newNode, page, baseUrl);

			}
		}
	}

	/**
	 * Check whether a file with given name and checksum already exists
	 */
	private boolean fileExists(final String name, final long checksum) throws FrameworkException {

		return app.nodeQuery(File.class).andName(name).and(File.checksum, checksum).getFirst() != null;

	}

	/**
	 * Return an eventually existing folder with given name, or create a new
	 * one.
	 *
	 * Don't create a folder for ".."
	 */
	private Folder findOrCreateFolder(final String name) throws FrameworkException {

		if ("..".equals(name)) {
			return null;
		}

		Folder folder = app.nodeQuery(Folder.class).andName(name).getFirst();

		if (folder != null) {

			return folder;
		}

		return (Folder) app.create(Folder.class,
			new NodeAttribute(AbstractNode.name, name),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible)
		);

	}

	private Linkable downloadFile(String downloadAddress, final URL baseUrl) {

		final String uuid = UUID.randomUUID().toString().replaceAll("[\\-]+", "");
		String contentType;

		// Create temporary file with new uuid
		// FIXME: This is much too dangerous!
		final String relativeFilePath = org.structr.web.entity.File.getDirectoryPath(uuid) + "/" + uuid;
		final String filePath = FileHelper.getFilePath(relativeFilePath);
		final java.io.File fileOnDisk = new java.io.File(filePath);

		fileOnDisk.getParentFile().mkdirs();

		URL downloadUrl;
		long size;
		long checksum;

		try {

			downloadUrl = new URL(baseUrl, downloadAddress);
			FileUtils.copyURLToFile(downloadUrl, fileOnDisk);

			logger.log(Level.INFO, "Starting download from {0}", downloadUrl);

		} catch (IOException ioe) {

			logger.log(Level.WARNING, "Unable to download from " + downloadAddress, ioe);

			try {
				// Try alternative baseUrl with trailing "/"
				downloadUrl = new URL(new URL(address.concat("/")), downloadAddress);
				FileUtils.copyURLToFile(downloadUrl, fileOnDisk);

			} catch (MalformedURLException ex) {
				logger.log(Level.SEVERE, "Could not resolve address " + address.concat("/"), ex);
				return null;
			} catch (IOException ex) {
				logger.log(Level.WARNING, "Unable to download from " + address.concat("/"), ex);
				return null;
			}

			logger.log(Level.INFO, "Starting download from alternative URL {0}", downloadUrl);

		}

		// TODO: Add security features like null/integrity/virus checking before copying it to
		// the files repo
		try {

			size = fileOnDisk.length();

			checksum = FileUtils.checksumCRC32(fileOnDisk);

		} catch (IOException ioe) {

			logger.log(Level.WARNING, "Unable to calc checksum of " + fileOnDisk, ioe);
			return null;
		}

		contentType = FileHelper.getContentMimeType(fileOnDisk);
		downloadAddress = StringUtils.substringBefore(downloadAddress, "?");

		final String fileName = (downloadAddress.indexOf("/") > -1)
			? StringUtils.substringAfterLast(downloadAddress, "/")
			: downloadAddress;
		String httpPrefix = "http://";
		String path = StringUtils.substringBefore(((downloadAddress.indexOf(httpPrefix) > -1)
			? StringUtils.substringAfter(downloadAddress, "http://")
			: downloadAddress), fileName);

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(fileName, ".")), "text/plain");
		}

		final String ct = contentType;

		try {

			if (!(fileExists(fileName, checksum))) {

				File fileNode;

				if (ImageHelper.isImageType(fileName)) {

					fileNode = createImageNode(uuid, fileName, ct, size, checksum);
				} else {

					fileNode = createFileNode(uuid, fileName, ct, size, checksum);
				}

				if (fileNode != null) {

					Folder parent = createFolderPath(path);

					if (parent != null) {

						app.create(parent, fileNode, Files.class);
						//createRel.execute(parent, fileNode, Folders.class);
					}

					if (contentType.equals("text/css")) {

						processCssFileNode(fileNode, downloadUrl);
					}
				}

				return fileNode;

			} else {

				fileOnDisk.delete();
			}

		} catch (FrameworkException | IOException fex) {

			logger.log(Level.WARNING, "Could not create file node.", fex);

		}

		return null;

	}

	/**
	 * Create one folder per path item and return the last folder.
	 *
	 * F.e.: /a/b/c => Folder["name":"a"] --HAS_CHILD--> Folder["name":"b"]
	 * --HAS_CHILD--> Folder["name":"c"], returns Folder["name":"c"]
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
		Folder folder = null;

		for (String part : parts) {

			Folder parent = folder;

			folder = findOrCreateFolder(part);

			if (folder != null && parent != null) {
				app.create(parent, folder, Folders.class);
				folder.updateInIndex();
			}

		}

		return folder;

	}

	private File createFileNode(final String uuid, final String name, final String contentType, final long size, final long checksum) throws FrameworkException {

		String relativeFilePath = File.getDirectoryPath(uuid) + "/" + uuid;
		File fileNode = app.create(File.class,
			new NodeAttribute(GraphObject.id, uuid),
			new NodeAttribute(AbstractNode.name, name),
			new NodeAttribute(File.relativeFilePath, relativeFilePath),
			new NodeAttribute(File.contentType, contentType),
			new NodeAttribute(File.size, size),
			new NodeAttribute(File.checksum, checksum),
			new NodeAttribute(File.version, 1),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

		return fileNode;

	}

	private Image createImageNode(final String uuid, final String name, final String contentType, final long size, final long checksum) throws FrameworkException {

		String relativeFilePath = Image.getDirectoryPath(uuid) + "/" + uuid;
		Image imageNode = app.create(Image.class,
			new NodeAttribute(GraphObject.id, uuid),
			new NodeAttribute(AbstractNode.name, name),
			new NodeAttribute(File.relativeFilePath, relativeFilePath),
			new NodeAttribute(File.contentType, contentType),
			new NodeAttribute(File.size, size),
			new NodeAttribute(File.checksum, checksum),
			new NodeAttribute(AbstractNode.visibleToPublicUsers, publicVisible),
			new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, authVisible));

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

			logger.log(Level.INFO, "Trying to download from URL found in CSS: {0}", url);
			downloadFile(url, baseUrl);

		}

	}

}
