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
package org.structr.websocket.command;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Base class for all WebSocket commands in Structr.
 */
public abstract class AbstractCommand {

	private static final Logger logger        = LoggerFactory.getLogger(AbstractCommand.class.getName());

	protected Session session                 = null;
	protected StructrWebSocket webSocket      = null;
	protected String callback                 = null;

	public abstract void processMessage(final WebSocketMessage webSocketData) throws FrameworkException;

	public abstract String getCommand();

	public Session getSession() {
		return session;
	}

	public StructrWebSocket getWebSocket() {
		return webSocket;
	}

	public Page getPage(final String id) {
		return getNodeAs(id, Page.class, StructrTraits.PAGE);
	}

	public DOMNode getDOMNode(final String id) {
		return getNodeAs(id, DOMNode.class, StructrTraits.DOM_NODE);
	}

	public <T> T getNodeAs(final String id, final Class<T> type, final String traitName) {

		final NodeInterface node = getNode(id);

		if (node != null && node.is(traitName)) {

			return (T)node.as(type);
		}

		return null;
	}

	/**
	 * Returns the graph object with the given id.
	 *
	 * @param id
	 * @return the graph object
	 */
	public GraphObject getGraphObject(final String id) {
		return getGraphObject(id, null);
	}

	/**
	 * Returns the graph object with the given id.
	 *
	 * If no node with the given id is found and nodeId is not null,
	 * this method will search for a relationship in the list of relationships
	 * of the node with the given nodeId.
	 *
	 * @param id
	 * @param nodeId
	 * @return the graph object
	 */
	public GraphObject getGraphObject(final String id, final String nodeId) {

		if (isValidUuid(id)) {

			final NodeInterface node = getNode(id);
			if (node != null) {

				return node;

			} else {

				if (nodeId == null) {
					logger.warn("Relationship access by UUID can take a very long time. Please examine the following stack trace and amend.");
				}

				final RelationshipInterface rel = getRelationship(id, nodeId);
				if (rel != null) {

					return rel;
				}
			}

		} else {

			logger.warn("Invalid UUID used for getGraphObject: {} is not a valid UUID.", id);
		}

		return null;
	}

	/**
	 * Returns the node with the given id.
	 *
	 * @param id
	 * @return the node
	 */
	public NodeInterface getNode(final String id) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(id);

			tx.success();

			return node;

		} catch (FrameworkException fex) {
			logger.warn("Unable to get node", fex);
		}

		return null;
	}

	/**
	 * Returns the relationship with the given id by looking up a node
	 * with the given nodeId and filtering the relationships.
	 *
	 * This avoids the performance issues of getRelationshipById due to missing index support.
	 *
	 * @param id
	 * @param nodeId
	 * @return the node
	 */
	public RelationshipInterface getRelationship(final String id, final String nodeId) {

		if (id == null) {
			return null;
		}

		if (nodeId == null) {
			return getRelationship(id);
		}

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(nodeId);

			for (final RelationshipInterface rel : node.getRelationships()) {

				if (rel.getUuid().equals(id)) {
					return rel;
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unable to get relationship", fex);
		}

		return null;

	}

	/**
	 * Returns the relationship to which the uuid parameter
	 * of this command refers to.
	 *
	 * @param id
	 * @return the node
	 */
	public RelationshipInterface getRelationship(final String id) {

		if (id == null) {
			return null;
		}

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final RelationshipInterface rel = app.getRelationshipById(id);

			tx.success();

			return rel;

		} catch (FrameworkException fex) {
			logger.warn("Unable to get relationship", fex);
		}

		return null;
	}

	/**
	 * Override this method if the websocket command will create its own
	 * transaction context
	 *
	 * @return a boolean
	 */
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	/**
	 * Make child nodes of the source nodes child nodes of the target node.
	 *
	 * @param sourceNode
	 * @param targetNode
	 */
	protected void moveChildNodes(final DOMNode sourceNode, final DOMNode targetNode) throws FrameworkException {

		for (final DOMNode child : sourceNode.getChildren()) {

			targetNode.appendChild(child);
		}
	}

	/**
	 * Search for a hidden page named __ShadowDocument__ of type.
	 *
	 * If found, return it, if not, create it.
	 * The shadow page is the DOM document all reusable components are connected to.
	 * It is necessary to comply with DOM standards.
	 *
	 * @return shadow document
	 * @throws FrameworkException
	 */
	public static ShadowDocument getOrCreateHiddenDocument() throws FrameworkException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			NodeInterface doc = app.nodeQuery(StructrTraits.SHADOW_DOCUMENT).includeHidden().getFirst();
			if (doc == null) {

				final PropertyMap properties = new PropertyMap();
				final Traits traits          = Traits.of(StructrTraits.SHADOW_DOCUMENT);

				properties.put(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY), traits.getName());
				properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "__ShadowDocument__");
				properties.put(traits.key(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY), true);
				properties.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

				doc = app.create(StructrTraits.SHADOW_DOCUMENT, properties);
			}

			tx.success();

			return doc.as(ShadowDocument.class);

		} catch (FrameworkException fex) {
			logger.warn("Unable to create container for shared components: {}", fex.getMessage());
		}

		return null;
	}

	public void setSession(final Session session) {
		this.session = session;
	}

	public void setWebSocket(final StructrWebSocket webSocket) {
		this.webSocket = webSocket;
	}

	public void setCallback(final String callback) {
		this.callback = callback;
	}

	public void setDoTransactionNotifications(final boolean notify) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		if (securityContext != null) {
			securityContext.setDoTransactionNotifications(notify);
		}

	}

	// ----- private methods -----
	private static boolean isValidUuid(final String id) {
		return Settings.isValidUuid(id);
	}
}
