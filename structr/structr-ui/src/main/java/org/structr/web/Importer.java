/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import org.structr.common.CaseHelper;
import org.structr.common.FileHelper;
import org.structr.common.Path;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.File;
import org.structr.core.node.*;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
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

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class Importer {

	private static String[] hrefElements                             = new String[] { "link" };
	private static String[] ignoreElementNames                       = new String[] { "#declaration", "#comment", "#doctype" };
	private static String[] srcElements                              = new String[] {

		"img", "script", "audio", "video", "input", "source", "track"

	};
	private static final Logger logger                               = Logger.getLogger(Importer.class.getName());
	private static final Map<String, String> contentTypeForExtension = new HashMap<String, String>();
	private static Command createNode;
	private static Command createRel;
	private static Command searchNode;
	private static Command indexNode;

	private Document parsedDocument;
	private String address;
	private String name;
	private int timeout;


	//~--- static initializers --------------------------------------------

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js", "text/javascript");
		contentTypeForExtension.put("xml", "text/xml");
		contentTypeForExtension.put("php", "application/x-php");

	}

	//~--- constructors ---------------------------------------------------

	public Importer(final String address, final String name, final int timeout) {
		this.address = address;
		this.name = name;
		this.timeout = timeout;
	}

	//~--- methods --------------------------------------------------------



	public boolean parse() throws FrameworkException {

		logger.log(Level.INFO, "##### Start fetching {0} for resource {1} #####", new Object[] { address, name });

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		searchNode = Services.command(securityContext, SearchNodeCommand.class);
		createNode = Services.command(securityContext, CreateNodeCommand.class);
		createRel  = Services.command(securityContext, CreateRelationshipCommand.class);
		indexNode  = Services.command(securityContext, IndexNodeCommand.class);

		try {
			parsedDocument = Jsoup.parse(new URL(address), timeout);

			return true;

		} catch (IOException ioe) {
			throw new FrameworkException(500, "Error while importing content from " + address);
		}
	}


	public String readResource() throws FrameworkException {

		final URL baseUrl;

		try {

			baseUrl = new URL(address);

			AbstractNode res = (AbstractNode) Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

					attrs.add(new NodeAttribute("type", "Resource"));
					attrs.add(new NodeAttribute("name", name));

					AbstractNode resource = findOrCreateNode("Resource", attrs, "/");

					createChildNodes(parsedDocument, resource, resource.getStringProperty(AbstractNode.Key.uuid), baseUrl);

					return resource;
				}

			});

			if (res != null) {
				logger.log(Level.INFO, "##### Finished fetching {0} for resource {1} #####", new Object[] { address, name });
				return res.getStringProperty(AbstractNode.Key.uuid);
			}

		} catch (MalformedURLException ex) {
			logger.log(Level.SEVERE, "Could not resolve address " + address, ex);
		}

		return null;

	}

	private void createChildNodes(final Node startNode, final AbstractNode parent, String resourceId, final URL baseUrl) throws FrameworkException {

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
				content = ((TextNode) node).text();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;

				}

			}

			List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

			// In case of a content node, put content into the "content" field
			if (content != null) {

				attrs.add(new NodeAttribute("content", content));

			}

			// Type and name
			attrs.add(new NodeAttribute(AbstractNode.Key.type.name(), type));
			attrs.add(new NodeAttribute(AbstractNode.Key.name.name(), "New " + type));

			// Tag name
			attrs.add(new NodeAttribute("tag", tag));

			// "id" attribute: Put it into the "_html_id" field
			if (StringUtils.isNotBlank(id)) {

				attrs.add(new NodeAttribute(PropertyView.Html + "id", id));

			}

			// "class" attribute: Put it into the "_html_class" field
			if (StringUtils.isNotBlank(classString.toString())) {

				attrs.add(new NodeAttribute(PropertyView.Html + "class", StringUtils.trim(classString.toString())));

			}

			// Other attributes: Put them into the respective fields with "_html_" prefix
			for (Attribute nodeAttr : node.attributes()) {

				attrs.add(new NodeAttribute(PropertyView.Html + nodeAttr.getKey(), nodeAttr.getValue()));

			}

			String nodePath = getNodePath(node);

			// create node
			// TODO: Do we need the name here?
			AbstractNode newNode = findOrCreateNode(type, attrs, nodePath);

			// Link new node to its parent node
			linkNodes(parent, newNode, resourceId, localIndex);

			// Step down and process child nodes
			createChildNodes(node, newNode, resourceId, baseUrl);

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
	private AbstractNode findExistingNode(String type, List<NodeAttribute> attrs, final String nodePath) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

//		for (NodeAttribute attr : attrs) {
//
//			String key   = attr.getKey();
//			String value = attr.getValue().toString();
//
//			if (type.equals("Content") && key.equals(Content.UiKey.content.name())) {
//
//				value = Search.escapeForLucene(value);
//				//value = Search.clean(value);
//			}
//
//			// Exclude data attribute because it may contain code with special characters, too
//			if (!key.equals(PropertyView.Html.concat("data"))) {
//
//				searchAttrs.add(Search.andExactProperty(key, value));
//
//			}
//
//		}

		List<AbstractNode> nodes = (List<AbstractNode>) searchNode.execute(null, false, false, searchAttrs);

		if (nodes.size() > 1) {

			logger.log(Level.WARNING, "More than one node found.");

		}

		if (nodes.isEmpty()) {

			return null;

		}

		for (AbstractNode foundNode : nodes) {

			String foundNodePath = foundNode.getStringProperty(HtmlElement.UiKey.path);

			logger.log(Level.INFO, "Found a node with path {0}", foundNodePath);

			if (foundNodePath.equals(nodePath)) {

				logger.log(Level.INFO, "MATCH!");

				// First match wins
				return foundNode;

			}

		}

		logger.log(Level.INFO, "Does not match expected path {0}", nodePath);

		return null;

//
//              // Does found node have any incoming contains relationships?
//              if (!foundNode.hasRelationship(RelType.CONTAINS, Direction.INCOMING)) {
//
//                      logger.log(Level.INFO, "No INCOMING CONTAINS relationships.");
//
//                      return null;
//
//              }
//
//              List<AbstractRelationship> rels = foundNode.getRelationships(RelType.CONTAINS, Direction.INCOMING);
//
//              if (rels.isEmpty()) {
//
//                      logger.log(Level.INFO, "No INCOMING CONTAINS relationships.");
//
//                      return null;
//
//              } else if (rels.size() > 1) {
//
//                      logger.log(Level.INFO, "Too many INCOMING CONTAINS relationships: {0}", rels.size());
//
//                      return null;
//
//              } else {
//
//                      String parentId = rels.get(0).getStartNode().getStringProperty(AbstractNode.Key.uuid);
//
//                      if (parentId.equals(parentNodeId)) {
//
//                              return foundNode;
//
//                      }
//
//                      logger.log(Level.INFO, "Parent node id doesn't match");
//
//                      return null;
//
//              }
	}

	private AbstractNode findOrCreateNode(String type, List<NodeAttribute> attributes, final String nodePath) throws FrameworkException {

		AbstractNode node = findExistingNode(type, attributes, nodePath);

		if (node != null) {

			return node;

		}

		node = (AbstractNode) createNode.execute(attributes);

		node.setProperty(HtmlElement.UiKey.path, nodePath);

		if (node != null) {

			logger.log(Level.INFO, "Created node with name {0}, type {1}", new Object[] { node.getName(), node.getType() });

		} else {

			logger.log(Level.WARNING, "Could not create node");

		}

		return node;
	}

	private AbstractRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) throws FrameworkException {

		AbstractRelationship rel = (AbstractRelationship) createRel.execute(startNode, endNode, RelType.CONTAINS);

		rel.setProperty(resourceId, index);

		return rel;
	}

	/**
	 * Check whether a file with given checksum already exists
	 */
	private boolean fileExists(final String name, final long checksum) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		searchAttrs.add(Search.andExactProperty(AbstractNode.Key.name.name(), name));
		searchAttrs.add(Search.andExactProperty(File.Key.checksum.name(), String.valueOf(checksum)));
		searchAttrs.add(Search.andExactType(File.class.getSimpleName()));

		List<File> files = (List<File>) searchNode.execute(null, false, false, searchAttrs);

		return !files.isEmpty();
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

		final String name = (downloadAddress.indexOf("/") > -1)
				    ? StringUtils.substringAfterLast(downloadAddress, "/")
				    : downloadAddress;

		if (contentType.equals("text/plain")) {

			contentType = StringUtils.defaultIfBlank(contentTypeForExtension.get(StringUtils.substringAfterLast(name, ".")), "text/plain");

		}

		final String ct                = contentType;
		
//		StructrTransaction transaction = new StructrTransaction() {
//
//			@Override
//			public Object execute() throws FrameworkException {
//
//				return createNode.execute(new NodeAttribute(AbstractNode.Key.uuid.name(), uuid), new NodeAttribute(AbstractNode.Key.type.name(), File.class.getSimpleName()),
//							  new NodeAttribute(AbstractNode.Key.name.name(), name), new NodeAttribute(File.Key.contentType.name(), ct));
//			}
//		};

		try {

			if (!(fileExists(name, FileUtils.checksumCRC32(fileOnDisk)))) {

				// create node in transaction
				//File fileNode = (File) Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);

				File fileNode = (File) createNode.execute(
					new NodeAttribute(AbstractNode.Key.uuid.name(), uuid),
					new NodeAttribute(AbstractNode.Key.type.name(), File.class.getSimpleName()),
					new NodeAttribute(AbstractNode.Key.name.name(), name),
					new NodeAttribute(File.Key.contentType.name(), ct));

				fileNode.setRelativeFilePath(relativeFilePath);
				fileNode.getChecksum();    // calculates and stores checksum

				indexNode.execute(fileNode);

				if (contentType.equals("text/css")) {

					processsCssFileNode(fileNode, downloadUrl);

				}
			} else {

				fileOnDisk.delete();

			}

		} catch (Exception fex) {
			logger.log(Level.WARNING, "Could not create node.", fex);
		}
	}

	private void processsCssFileNode(File fileNode, final URL baseUrl) throws IOException {

		StringWriter sw = new StringWriter();

		IOUtils.copy(fileNode.getInputStream(), sw, "UTF-8");

		String css = sw.toString();

		processCss(css, baseUrl);
	}

	private void processCss(final String css, final URL baseUrl) throws IOException {

		Pattern pattern = Pattern.compile("url\\((.*)\\)");
		Matcher matcher = pattern.matcher(css);

		while (matcher.find()) {

			String url = StringUtils.strip(matcher.group(1), "'\"");

			logger.log(Level.INFO, "Trying to download form URL found in CSS: {0}", url);
			downloadFiles(url, baseUrl);

		}
	}

	//~--- get methods ----------------------------------------------------

	private String getNodePath(final Node node) {

		Node n      = node;
		String path = "";

		while ((n.nodeName() != null) &&!n.nodeName().equals("html")) {

			int index = n.siblingIndex();

			path = n.nodeName() + "[" + index + "]" + "/" + path;
			n    = n.parent();

		}

		return "html/" + path;
	}
}
