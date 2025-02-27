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
package org.structr.web.maintenance.deploy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.DeployCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class PageImporter extends HtmlFileImporter {

	private static final Logger logger        = LoggerFactory.getLogger(PageImporter.class.getName());
	private static final String DoctypeString = "<!DOCTYPE";

	private Map<String, Object> pagesConfiguration = null;
	private SecurityContext securityContext        = null;
	private boolean relativeVisibility             = false;
	private Path basePath                          = null;
	private App app                                = null;

	private Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties = null;

	public PageImporter(final Path basePath, final Map<String, Object> pagesConfiguration, final boolean relativeVisibility) {

		this.pagesConfiguration = pagesConfiguration;
		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.securityContext.setDoTransactionNotifications(false);
		this.basePath           = basePath;
		this.app                = StructrApp.getInstance(this.securityContext);
		this.relativeVisibility = relativeVisibility;
	}

	@Override
	public void processFile(final Path file, final String fileName) throws IOException {

		try {

			createPage(file, fileName);

		} catch (FrameworkException fex) {
			logger.warn("Exception while importing page {}: {}", new Object[] { fileName, fex.toString()});
		}
	}

	public void setDeferredNodesAndTheirProperties(final Map<DOMNode, PropertyMap> data) {
		this.deferredNodesAndTheirProperties = data;
	}

	// ----- private methods -----
	private Page getExistingPage(final String name) throws FrameworkException {

		final NodeInterface node = StructrApp.getInstance().nodeQuery(StructrTraits.PAGE).andName(name).getFirst();
		if (node != null) {

			return node.as(Page.class);
		}

		return null;
	}

	private void deletePage(final App app, final String name) throws FrameworkException {

		final Page page = getExistingPage(name);
		if (page != null) {

			for (final DOMNode child : page.getElements()) {
				app.delete(child);
			}

			app.delete(page);
		}
	}

	private PropertyMap getPropertiesForPage(final String name) {

		final Object data = pagesConfiguration.get(name);
		if (data != null && data instanceof Map) {

			try {

				DeployCommand.checkOwnerAndSecurity((Map<String, Object>)data);

				return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), StructrTraits.PAGE, (Map<String, Object>)data);

			} catch (FrameworkException ex) {
				logger.warn("Unable to resolve properties for page: {}", ex.getMessage());
			}
		}

		return null;
	}

	private <T> T get(final PropertyMap src, final PropertyKey<T> key, final T defaultValue) {

		if (src != null) {

			final T t = src.get(key);
			if (t != null) {

				return t;
			}
		}

		return defaultValue;
	}

	private void createFolder(final Path file) {

		try (final Tx tx = app.tx(true, false, false)) {

			// create folder
			FileHelper.createFolderPath(securityContext, basePath.relativize(file).toString());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	private void createPage(final Path file, final String fileName) throws IOException, FrameworkException {

		final String name = StringUtils.substringBeforeLast(fileName, ".html");

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final PropertyMap properties = getPropertiesForPage(name);
			if (properties == null) {

				logger.info("Ignoring {} (not in pages.json)", fileName);

			} else {

				final Page existingPage = getExistingPage(name);
				if (existingPage != null) {

					deletePage(app, name);
				}

				final String src         = new String(Files.readAllBytes(file),Charset.forName("UTF-8"));
				final Traits traits      = Traits.of(StructrTraits.PAGE);
				final String contentType = get(properties, traits.key("contentType"),                 "text/html");
				boolean visibleToPublic  = get(properties, traits.key("visibleToPublicUsers"),        false);
				boolean visibleToAuth    = get(properties, traits.key("visibleToAuthenticatedUsers"), false);

				final Importer importer = new Importer(securityContext, src, null, name, visibleToPublic, visibleToAuth, false, relativeVisibility);

				// enable literal import of href attributes
				importer.setIsDeployment(true);

				// set deferred DOMNodes to be able to connect two imported nodes
				importer.setDeferredNodesAndTheirProperties(deferredNodesAndTheirProperties);

				if (StringUtils.startsWithIgnoreCase(src, DoctypeString) && "text/html".equals(contentType)) {

					// Import document starts with <!DOCTYPE> definition, so we treat it as an HTML
					// document and use the Structr HTML importer.

					final boolean parseOk = importer.parse();
					if (parseOk) {

						logger.info("Importing page {} from {}..", new Object[] { name, fileName } );

						// set comment handler that can parse and apply special Structr comments in HTML source files
						importer.setCommentHandler(new DeploymentCommentHandler());

						// parse page
						final Page newPage = importer.readPage();

						// remove duplicate elements
						fixDocumentElements(newPage);

						// store properties from pages.json
						newPage.setProperties(securityContext, properties);
					}

				} else {

					// Import document does NOT start with a <!DOCTYPE> definition, so we assume a
					// template or shared component that we need to parse.

					final boolean parseOk = importer.parse(true);
					if (parseOk) {

						logger.info("Importing page {} from {}..", new Object[] { name, fileName } );

						// set comment handler that can parse and apply special Structr comments in HTML source files
						importer.setCommentHandler(new DeploymentCommentHandler());

						// parse page
						final Page newPage = app.create(StructrTraits.PAGE, name).as(Page.class);

						// store properties from pages.json
						newPage.setProperties(securityContext, properties);

						// add children
						importer.createChildNodes(newPage, newPage);
					}
				}

				deferredNodesAndTheirProperties = importer.getDeferredNodesAndTheirProperties();
			}

			tx.success();
		}
	}

	/**
	 * Remove duplicate Head element from import process.
	 * @param page
	 */
	private void fixDocumentElements(final Page page) throws FrameworkException {

		final List<DOMNode> heads = page.getElementsByTagName("head");
		if (heads.size() > 1) {

			final DOMNode head1  = heads.get(0);
			final DOMNode head2  = heads.get(1);
			final DOMNode parent = head1.getParent();

			final boolean h1 = head1.hasChildNodes();
			final boolean h2 = head2.hasChildNodes();

			if (h1 && h2) {

				// merge
				for (DOMNode child = head2.getFirstChild(); child != null; child = child.getNextSibling()) {

					head2.removeChild(child);
					head1.appendChild(child);
				}

				parent.removeChild(head2);

			} else if (h1 && !h2) {

				// remove head2
				parent.removeChild(head2);

			} else if (!h1 && h2) {

				// remove head1
				parent.removeChild(head1);

			} else {

				// remove first, doesn't matter
				parent.removeChild(head1);
			}
		}
	}
}
