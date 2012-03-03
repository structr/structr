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
import org.structr.common.StandaloneTestHelper;
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
	private static Command searchNode;

	//~--- static initializers --------------------------------------------

	static {

		contentTypeForExtension.put("css", "text/css");
		contentTypeForExtension.put("js", "text/javascript");

	}

	//~--- constructors ---------------------------------------------------

	public Importer() {}

	//~--- methods --------------------------------------------------------

	public static void main(String[] args) throws Exception {

		StandaloneTestHelper.prepareStandaloneTest("/home/axel/NetBeansProjects/structr/structr/structr-ui/db");

		String address = "http://structr.org";

		start(address, "index", 5000);
		StandaloneTestHelper.finishStandaloneTest();

	}

	public static void start(final String address, final String name, final int timeout) throws FrameworkException {

		searchNode = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);

		final Document page;

		try {
			page = Jsoup.parse(new URL(address), timeout);
		} catch (IOException ioe) {
			throw new FrameworkException(500, "Error while importing content from " + address);
		}

		final URL baseUrl;

		try {

			baseUrl = new URL(address);

			Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

					attrs.add(new NodeAttribute("type", "Resource"));
					attrs.add(new NodeAttribute("name", name));

					AbstractNode page1 = createNode(attrs);

					createChildNodes(page, page1, page1.getStringProperty(AbstractNode.Key.uuid), baseUrl);

					return null;
				}

			});

		} catch (MalformedURLException ex) {
			logger.log(Level.SEVERE, "Could not resolve address " + address, ex);
		}
	}

	private static void createChildNodes(final Node startNode, final AbstractNode parent, final String resourceId, final URL baseUrl) throws FrameworkException {

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
			attrs.add(new NodeAttribute(AbstractNode.Key.name.name(), "New " + type + " " + Math.random()));

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

			// create node
			// TODO: Do we need the name here?
			AbstractNode newNode = createNode(attrs);

			// Link new node to its parent node
			linkNodes(parent, newNode, resourceId, localIndex);

			// Step down and process child nodes
			createChildNodes(node, newNode, resourceId, baseUrl);

			// Count up position index
			localIndex++;

		}
	}

	private static AbstractNode createNode(List<NodeAttribute> attributes) throws FrameworkException {

		SecurityContext context   = SecurityContext.getSuperUserInstance();
		Command createNodeCommand = Services.command(context, CreateNodeCommand.class);
		AbstractNode node         = (AbstractNode) createNodeCommand.execute(attributes);

		if (node != null) {

			logger.log(Level.INFO, "Created node with name {0}, type {1}", new Object[] { node.getName(), node.getType() });

		} else {

			logger.log(Level.WARNING, "Could not create node");

		}

		return node;
	}

	private static AbstractRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) throws FrameworkException {

		SecurityContext context  = SecurityContext.getSuperUserInstance();
		Command createRelCommand = Services.command(context, CreateRelationshipCommand.class);
		AbstractRelationship rel = (AbstractRelationship) createRelCommand.execute(startNode, endNode, RelType.CONTAINS);

		rel.setProperty(resourceId, index);

		return rel;
	}

	/**
	 * Check whether a file with given checksum already exists
	 */
	private static boolean fileExists(final String name, final long checksum) throws FrameworkException {

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		searchAttrs.add(Search.andExactProperty(AbstractNode.Key.name.name(), name));
		searchAttrs.add(Search.andExactProperty(File.Key.checksum.name(), String.valueOf(checksum)));
		searchAttrs.add(Search.andExactType(File.class.getSimpleName()));

		List<File> files = (List<File>) searchNode.execute(null, false, false, searchAttrs);

		return !files.isEmpty();
	}

	private static void downloadFiles(String downloadAddress, final URL baseUrl) {

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

			contentType = contentTypeForExtension.get(StringUtils.substringAfterLast(name, "."));

		}

		final String ct                = contentType;
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.Key.uuid.name(), uuid),
							new NodeAttribute(AbstractNode.Key.type.name(), File.class.getSimpleName()), new NodeAttribute(AbstractNode.Key.name.name(), name),
							new NodeAttribute(File.Key.contentType.name(), ct));
			}
		};

		try {

			if (!(fileExists(name, FileUtils.checksumCRC32(fileOnDisk)))) {

				// create node in transaction
				File fileNode = (File) Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);

				fileNode.setRelativeFilePath(relativeFilePath);
				fileNode.getChecksum();    // calculates and stores checksum

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

	private static void processsCssFileNode(File fileNode, final URL baseUrl) throws IOException {

		StringWriter sw = new StringWriter();

		IOUtils.copy(fileNode.getInputStream(), sw, "UTF-8");

		String css = sw.toString();

		processCss(css, baseUrl);
	}

	private static void processCss(final String css, final URL baseUrl) throws IOException {

		Pattern pattern = Pattern.compile("url\\((.*)\\)");
		Matcher matcher = pattern.matcher(css);

		while (matcher.find()) {

			String url = StringUtils.strip(matcher.group(1), "'\"");

			logger.log(Level.INFO, "Trying to download form URL found in CSS: {0}", url);

			downloadFiles(url, baseUrl);

		}
	}
}
