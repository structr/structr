/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 */
public class ComponentImporter extends HtmlFileImporter {

	private static final Logger logger       = LoggerFactory.getLogger(ComponentImporter.class.getName());
	private static final GenericProperty internalSharedTemplateKey = new GenericProperty("shared");

	private Map<String, Object> configuration = null;
	private SecurityContext securityContext   = null;
	private boolean relativeVisibility        = false;
	private App app                           = null;
	private boolean isHullMode                = false;

	private Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties = null;

	public ComponentImporter(final Map<String, Object> pagesConfiguration, final boolean relativeVisibility) {

		this.relativeVisibility = relativeVisibility;
		this.configuration      = pagesConfiguration;
		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.app                = StructrApp.getInstance();
	}

	@Override
	public void processFile(final Path file, final String fileName) throws IOException {

		try {

			createComponentChildren(file, fileName);

		} catch (FrameworkException fex) {
			logger.warn("Exception while importing shared component {}: {}", fileName, fex.toString());
		}
	}

	public void setDeferredNodesAndTheirProperties(final Map<DOMNode, PropertyMap> data) {
		this.deferredNodesAndTheirProperties = data;
	}

	// ----- private methods -----
	private NodeInterface getExistingComponent(final String name) {

		final App app  = StructrApp.getInstance();
		NodeInterface result = null;

		try (final Tx tx = app.tx()) {

			if (DeployCommand.isUuid(name)) {

				result = StructrApp.getInstance().nodeQuery(StructrTraits.DOM_NODE).key(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), name).getFirst();

			} else {

				result = Importer.findSharedComponentByName(name);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to determine if component {} already exists, ignoring.", name);
		}

		return result;
	}

	private void deleteRecursively(final App app, final DOMNode node) throws FrameworkException {

		for (DOMNode child : node.getChildren()) {
			deleteRecursively(app, child);
		}

		for (DOMNode sync : node.getSyncedNodes()) {

			deleteRecursively(app, sync);
		}

		app.delete(node);

		FlushCachesCommand.flushAll();
	}

	private PropertyMap getPropertiesForComponent(final String name) {

		final Object data = configuration.get(name);
		if (data != null && data instanceof Map) {

			try {

				final Map dataMap = ((Map<String, Object>)data);

				// remove unnecessary "shared" key
				dataMap.remove(internalSharedTemplateKey.jsonName());

				DeployCommand.checkOwnerAndSecurity(dataMap);

				return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), StructrTraits.TEMPLATE, dataMap);

			} catch (FrameworkException ex) {
				logger.warn("Unable to resolve properties for shared component: {}", ex.getMessage());
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

	private void createComponentChildren(final Path file, final String fileName) throws IOException, FrameworkException {

		final String componentName      = StringUtils.substringBeforeLast(fileName, ".html");

		// either the template was exported with name + uuid or just the uuid
		final boolean byNameAndId       = (DeployCommand.getUuidOrNullFromEndOfString(componentName) != null);
		final boolean byId              = DeployCommand.isUuid(componentName);

		try (final Tx tx = app.tx(true, false, false)) {

			tx.disableChangelog();

			final NodeInterface existingComponent;

			if (DeployCommand.isUuid(componentName)) {

				existingComponent = app.getNodeById(StructrTraits.DOM_NODE, componentName);

			} else {

				final String uuidAtEnd = DeployCommand.getUuidOrNullFromEndOfString(componentName);
				if (uuidAtEnd != null) {

					existingComponent = app.getNodeById(StructrTraits.DOM_NODE, uuidAtEnd);

				} else {

					existingComponent = getExistingComponent(componentName);
				}
			}

			final PropertyMap properties = getPropertiesForComponent(componentName);

			if (properties == null) {

				logger.info("Ignoring {} (not in components.json)", fileName);

			} else {

				if (existingComponent != null && isHullMode()) {

					final PropertyKey<String> contentKey = Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_PROPERTY);
					final DOMNode component              = existingComponent.as(DOMNode.class);

					properties.put(contentKey, existingComponent.getProperty(contentKey));

					component.setOwnerDocument(null);

					if (component.is(StructrTraits.TEMPLATE)) {

						properties.put(contentKey, existingComponent.getProperty(contentKey));
						component.setOwnerDocument(null);

					} else {

						deleteRecursively(app, component);
					}
				}

				final Traits traits     = Traits.of(StructrTraits.NODE_INTERFACE);
				final String src        = new String(Files.readAllBytes(file), Charset.forName("UTF-8"));
				boolean visibleToPublic = get(properties, traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), false);
				boolean visibleToAuth   = get(properties, traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);
				final Importer importer = new Importer(securityContext, src, null, componentName, visibleToPublic, visibleToAuth, false, relativeVisibility);

				// enable literal import of href attributes
				importer.setIsDeployment(true);

				// set deferred DOMNodes to be able to connect two imported nodes
				importer.setDeferredNodesAndTheirProperties(deferredNodesAndTheirProperties);

				final boolean parseOk = importer.parse(false);
				if (parseOk) {

					// set comment handler that can parse and apply special Structr comments in HTML source files
					importer.setCommentHandler(new DeploymentCommentHandler());

					// parse page
					final ShadowDocument shadowDocument = CreateComponentCommand.getOrCreateHiddenDocument();
					final DOMNode rootElement;

					if (isHullMode()) {

						logger.info("Importing outer component shell for {} from {}..", componentName, fileName);

						importer.retainHullOnly();

						rootElement = importer.createComponentChildNodes(shadowDocument);

					} else {

						logger.info("Importing inner component contents for {} from {}..", componentName, fileName);

						rootElement = importer.createComponentHullChildNodes(existingComponent, shadowDocument);
					}

					if (rootElement != null) {

						if (byId) {

							DeployCommand.updateDeferredPagelink(rootElement.getUuid(), componentName);

							// set UUID
							rootElement.unlockSystemPropertiesOnce();
							rootElement.setProperty(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), componentName);

						} else if (byNameAndId) {

							// the last characters in the name string are the uuid
							final String uuid = DeployCommand.getUuidOrNullFromEndOfString(componentName);
							final String name = componentName.substring(0, componentName.length() - uuid.length() - 1);

							DeployCommand.updateDeferredPagelink(rootElement.getUuid(), uuid);

							rootElement.unlockSystemPropertiesOnce();
							rootElement.setProperty(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid);
							properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

						} else {

							// set name
							rootElement.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), componentName);
						}

						// store properties from components.json if present
						rootElement.setProperties(securityContext, properties);
					}
				}

				deferredNodesAndTheirProperties.putAll(importer.getDeferredNodesAndTheirProperties());
			}

			tx.success();

		}
	}

	public boolean isHullMode() {
		return isHullMode;
	}

	public void setHullMode(boolean hullMode) {
		isHullMode = hullMode;
	}
}
