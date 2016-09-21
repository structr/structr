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
package org.structr.web.maintenance.deploy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.importer.Importer;
import org.structr.websocket.command.CreateComponentCommand;

/**
 *
 */
public class ComponentImportVisitor implements FileVisitor<Path> {

	private static final Logger logger       = Logger.getLogger(ComponentImportVisitor.class.getName());

	private Map<String, Object> configuration = null;
	private SecurityContext securityContext   = null;
	private App app                           = null;

	public ComponentImportVisitor(final Map<String, Object> pagesConfiguration) {

		this.configuration = pagesConfiguration;
		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.app                = StructrApp.getInstance();
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

		if (attrs.isRegularFile()) {

			final String fileName = file.getFileName().toString();
			if (fileName.endsWith(".html")) {

				try {

					createComponent(file, fileName);

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "Exception while importing shared component {0}: {1}", new Object[] { name, fex.getMessage() });
				}
			}

		} else {

			logger.log(Level.WARNING, "Unexpected directory {0} found in components/ directory, ignoring", file.getFileName().toString());
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {

		logger.log(Level.WARNING, "Exception while importing file {0}: {1}", new Object[] { file.toString(), exc.getMessage() });
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	// ----- private methods -----
	private DOMNode getExistingComponent(final String name) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			return Importer.findSharedComponentByName(name);

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to determine if page {0} already exists, ignoring.", name);
		}

		return null;
	}

	private void deleteComponent(final App app, final String name) throws FrameworkException {

		final DOMNode node = getExistingComponent(name);
		if (node != null) {

			deleteRecursively(app, node);
		}
	}

	private void deleteRecursively(final App app, final DOMNode node) throws FrameworkException {

		for (DOMNode child : node.treeGetChildren()) {
			deleteRecursively(app, child);
		}

		for (DOMNode sync : node.getProperty(DOMNode.syncedNodes)) {

			deleteRecursively(app, sync);
		}

		app.delete(node);
	}

	private PropertyMap getPropertiesForComponent(final String name) throws FrameworkException {

		final Object data = configuration.get(name);
		if (data != null && data instanceof Map) {

			return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), DOMNode.class, (Map<String, Object>)data);

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

	private void createComponent(final Path file, final String fileName) throws IOException, FrameworkException {

		final String name               = StringUtils.substringBeforeLast(fileName, ".html");
		final PropertyMap properties    = getPropertiesForComponent(name);
		final DOMNode existingComponent = getExistingComponent(name);

		try (final Tx tx = app.tx()) {

			if (existingComponent != null) {

				deleteComponent(app, name);
			}

			final String src        = new String (Files.readAllBytes(file),Charset.forName("UTF-8"));
			boolean visibleToPublic = get(properties, GraphObject.visibleToPublicUsers, false);
			boolean visibleToAuth   = get(properties, GraphObject.visibleToPublicUsers, false);
			final Importer importer = new Importer(securityContext, src, null, name, visibleToPublic, visibleToAuth);

			// enable literal import of href attributes
			importer.setIsDeployment(true);

			final boolean parseOk = importer.parse(false);
			if (parseOk) {

				logger.log(Level.INFO, "Importing page {0} from {1}..", new Object[] { name, fileName } );

				// set comment handler that can parse and apply special Structr comments in HTML source files
				importer.setCommentHandler(new DeploymentCommentHandler());

				// parse page
				final ShadowDocument shadowDocument = CreateComponentCommand.getOrCreateHiddenDocument();
				final DOMNode rootElement           = importer.createComponentChildNodes(shadowDocument);

				if (rootElement != null) {

					// set name
					rootElement.setProperty(AbstractNode.name, name);

					// store properties from pages.json if present
					if (properties != null) {
						rootElement.setProperties(securityContext, properties);
					}
				}
			}

			tx.success();
		}
	}
}
