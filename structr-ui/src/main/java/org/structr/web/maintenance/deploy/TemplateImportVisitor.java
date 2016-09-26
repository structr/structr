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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.importer.Importer;
import org.structr.websocket.command.CreateComponentCommand;

/**
 *
 */
public class TemplateImportVisitor implements FileVisitor<Path> {

	private static final Logger logger       = Logger.getLogger(TemplateImportVisitor.class.getName());

	private Map<String, Object> configuration = null;
	private SecurityContext securityContext   = null;
	private App app                           = null;

	public TemplateImportVisitor(final Map<String, Object> pagesConfiguration) {

		this.configuration   = pagesConfiguration;
		this.securityContext = SecurityContext.getSuperUserInstance();
		this.app             = StructrApp.getInstance();
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

					createTemplate(file, fileName);

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
	private DOMNode getExistingTemplate(final String name) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			return Importer.findSharedComponentByName(name);

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to determine if template {0} already exists, ignoring.", name);
		}

		return null;
	}

	private void deleteTemplate(final App app, final String name) throws FrameworkException {

		final DOMNode node = getExistingTemplate(name);
		if (node != null) {

			deleteRecursively(app, node);
		}
	}

	private void deleteRecursively(final App app, final DOMNode node) throws FrameworkException {

		for (DOMNode child : node.treeGetChildren()) {
			deleteRecursively(app, child);
		}

		app.delete(node);
	}

	private PropertyMap getPropertiesForTemplate(final String name) throws FrameworkException {

		final Object data = configuration.get(name);
		if (data != null && data instanceof Map) {

			return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), DOMNode.class, (Map<String, Object>)data);

		}

		return null;
	}

	private void createTemplate(final Path file, final String fileName) throws IOException, FrameworkException {

		final String name              = StringUtils.substringBeforeLast(fileName, ".html");
		final PropertyMap properties   = getPropertiesForTemplate(name);
		final DOMNode existingTemplate = getExistingTemplate(name);

		try (final Tx tx = app.tx(false, false, false)) {

			if (existingTemplate != null) {

				deleteTemplate(app, name);
			}

			logger.log(Level.INFO, "Importing template {0} from {1}..", new Object[] { name, fileName } );

			final String src = new String (Files.readAllBytes(file),Charset.forName("UTF-8"));

			// parse page
			final ShadowDocument shadowDocument = CreateComponentCommand.getOrCreateHiddenDocument();

			final Template template             = app.create(Template.class,
				new NodeAttribute(Template.ownerDocument, shadowDocument),
				new NodeAttribute(AbstractNode.name, name),
				new NodeAttribute(Template.content, src)
			);

			// set name and content type
			template.setProperty(AbstractNode.name, name);
			template.setProperty(Template.contentType, "text/html");

			// store properties from templates.json if present
			if (properties != null) {
				template.setProperties(securityContext, properties);
			}

			tx.success();
		}
	}
}
