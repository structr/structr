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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.DeployCommand;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 */
public class TemplateImporter extends HtmlFileImporter {

	private static final Logger logger                             = LoggerFactory.getLogger(TemplateImporter.class.getName());
	private static final GenericProperty internalSharedTemplateKey = new GenericProperty("shared");

	private Map<String, Object> configuration = null;
	private SecurityContext securityContext   = null;
	private App app                           = null;

	public TemplateImporter(final Map<String, Object> pagesConfiguration) {

		this.configuration   = pagesConfiguration;
		this.securityContext = SecurityContext.getSuperUserInstance();
		this.securityContext.setDoTransactionNotifications(false);
		this.app             = StructrApp.getInstance(this.securityContext);
	}

	@Override
	public void processFile(final Path file, final String fileName) throws IOException {

		try {

			createTemplate(file, fileName);

		} catch (FrameworkException fex) {
			logger.warn("Exception while importing shared component {}: {}", fileName, fex.getMessage());
		}
	}

	// ----- private methods -----
	private DOMNode getExistingTemplate(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = Importer.findSharedComponentByName(name);
			if (node != null) {

				return node.as(DOMNode.class);
			}

		} catch (FrameworkException fex) {
			logger.warn("Unable to determine if template {} already exists, ignoring.", name);
		}

		return null;
	}

	private void deleteTemplate(final App app, final DOMNode template) throws FrameworkException {

		if (template != null) {

			deleteRecursively(app, template);
		}
	}

	private void deleteRecursively(final App app, final DOMNode node) throws FrameworkException {

		for (DOMNode child : node.getChildren()) {
			deleteRecursively(app, child);
		}

		app.delete(node);
	}

	private PropertyMap getPropertiesForTemplate(final String name) {

		final Object data = configuration.get(name);
		if (data != null && data instanceof Map) {

			try {

				final Map dataMap = ((Map<String, Object>)data);

				final Object sharedValue = dataMap.remove(internalSharedTemplateKey.jsonName());
				boolean isShared = ("true".equals(sharedValue));

				DeployCommand.checkOwnerAndSecurity(dataMap);

				final PropertyMap propMap = PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), StructrTraits.TEMPLATE, dataMap);

				if (isShared) {
					propMap.put(internalSharedTemplateKey, "true");
				}

				return propMap;

			} catch (FrameworkException ex) {
				logger.warn("Unable to resolve properties for template: {}", ex.getMessage());
			}
		}

		return null;
	}

	private void createTemplate(final Path file, final String fileName) throws IOException, FrameworkException {

		final String templateName = StringUtils.substringBeforeLast(fileName, ".html");

		// either the template was exported with name + uuid or just the uuid
		final boolean byNameAndId = (DeployCommand.getUuidOrNullFromEndOfString(templateName) != null);
		final boolean byId        = DeployCommand.isUuid(templateName);

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final PropertyMap properties = getPropertiesForTemplate(templateName);
			final Traits traits          = Traits.of(StructrTraits.TEMPLATE);

			if (properties == null) {

				logger.info("Ignoring {} (not in templates.json)", fileName);

			} else {

				final String src = new String(Files.readAllBytes(file), Charset.forName("UTF-8"));

				final Template template;

				if (byId) {

					logger.info("Importing template {} from {}..", new Object[] { templateName, fileName } );

					final NodeInterface existingTemplate = app.getNodeById(StructrTraits.DOM_NODE, templateName);
					if (existingTemplate != null) {

						deleteTemplate(app, existingTemplate.as(DOMNode.class));
					}

					template = app.create(StructrTraits.TEMPLATE, new NodeAttribute(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), templateName)).as(Template.class);

				} else if (byNameAndId) {

					// the last characters in the name string are the uuid
					final String uuid = DeployCommand.getUuidOrNullFromEndOfString(templateName);
					final String name = templateName.substring(0, templateName.length() - uuid.length() - 1);	// cut off uuid plus the dash

					logger.info("Importing template {} from {}..", new Object[] { name, fileName } );

					final NodeInterface existingTemplate = app.getNodeById(StructrTraits.DOM_NODE, uuid);
					if (existingTemplate != null) {

						deleteTemplate(app, existingTemplate.as(DOMNode.class));
					}

					template = app.create(StructrTraits.TEMPLATE, new NodeAttribute(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid)).as(Template.class);
					properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				} else {

					// old export format: only name of template in filename
					logger.info("Importing template {} from {}..", new Object[] { templateName, fileName } );

					final DOMNode existingTemplate = getExistingTemplate(templateName);
					if (existingTemplate != null) {

						deleteTemplate(app, existingTemplate);
					}

					template = app.create(StructrTraits.TEMPLATE).as(Template.class);
					properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), templateName);
				}

				properties.put(traits.key("content"), src);

				// insert "shared" templates into ShadowDocument
				final Object value = properties.remove(internalSharedTemplateKey);

				if ("true".equals(value)) {

					template.setOwnerDocument(CreateComponentCommand.getOrCreateHiddenDocument());
				}

				// store properties from templates.json if present
				template.setProperties(securityContext, properties);

			}

			tx.success();

		} catch (Throwable t) {

			logger.error("Error trying to create template {}", fileName);
		}
	}
}
