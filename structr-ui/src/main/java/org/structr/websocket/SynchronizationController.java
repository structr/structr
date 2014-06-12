/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket;

import com.google.gson.Gson;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.message.WebSocketMessage;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.AccessMode;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 * @author Christian Morgner
 */
public class SynchronizationController implements StructrTransactionListener {

	private static final Logger logger          = Logger.getLogger(SynchronizationController.class.getName());

	private final Set<StructrWebSocket> clients = new ConcurrentHashSet<>();
	private Gson gson                           = null;

	public SynchronizationController(final Gson gson) {

		this.gson = gson;

	}

	public void registerClient(final StructrWebSocket client) {

		synchronized (clients) {

			clients.add(client);
		}

	}

	public void unregisterClient(final StructrWebSocket client) {

		synchronized (clients) {

			clients.remove(client);
		}

	}

	// ----- private methods -----
	private void broadcast(final WebSocketMessage webSocketData) {

		//logger.log(Level.FINE, "Broadcasting message to {0} clients..", clients.size());

		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		String message;
		String pagePath = (String) webSocketData.getNodeData().get("pagePath");

		synchronized (clients) {

			// create message
			for (StructrWebSocket socket : clients) {

				String clientPagePath = socket.getPagePath();
				if (clientPagePath != null && !clientPagePath.equals(URIUtil.encodePath(pagePath))) {
					continue;
				}

				Session session = socket.getSession();

				webSocketData.setCallback(socket.getCallback());

				if ((session != null)) { //&& socket.isAuthenticated()) {

					List<? extends GraphObject> result = webSocketData.getResult();

					if ((result != null) && (result.size() > 0)
						&& (webSocketData.getCommand().equals("UPDATE") || webSocketData.getCommand().equals("ADD") || webSocketData.getCommand().equals("CREATE"))) {

						WebSocketMessage clientData     = webSocketData.copy();
						SecurityContext securityContext = socket.getSecurityContext();

						// For non-authenticated clients, construct a security context without user
						if (securityContext == null) {

							try {

								securityContext = SecurityContext.getInstance(null, AccessMode.Frontend);

							} catch (FrameworkException ex) {

								continue;
							}
						}

						clientData.setResult(filter(securityContext, result));

						message = gson.toJson(clientData, WebSocketMessage.class);

					} else {

						message = gson.toJson(webSocketData, WebSocketMessage.class);
					}

					// logger.log(Level.INFO, "############################################################ SENDING \n{0}", message);

					try {

						session.getRemote().sendString(message);

					} catch (Throwable t) {

						t.printStackTrace();
						logger.log(Level.WARNING, "Error sending message to client.", t);
					}

				}

			}
		}

	}

	private <T extends GraphObject> List<T> filter(final SecurityContext securityContext, final List<T> all) {

		List<T> filteredResult = new LinkedList<>();
		for (T obj : all) {

			if (securityContext.isVisible((AbstractNode)obj)) {

				filteredResult.add(obj);
			}
		}

		return filteredResult;

	}

	// ----- interface StructrTransactionListener -----
	@Override
	public void transactionCommited(final SecurityContext securityContext, final List<ModificationEvent> modificationEvents) {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			for (final ModificationEvent event : modificationEvents) {

				try {
					final WebSocketMessage message = getMessageForEvent(securityContext, event);
					if (message != null) {
						broadcast(message);
					}

				} catch (FrameworkException ignore) {}
			}

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	// ----- private methods -----
	private WebSocketMessage getMessageForEvent(final SecurityContext securityContext, final ModificationEvent modificationEvent) throws FrameworkException {

		if (modificationEvent.isNode()) {

			final NodeInterface node = (NodeInterface)modificationEvent.getGraphObject();

			if (modificationEvent.isDeleted()) {

				final WebSocketMessage message = createMessage("DELETE");

				message.setId(modificationEvent.getRemovedProperties().get(GraphObject.id));

				return message;
			}

			if (modificationEvent.isCreated()) {

				final WebSocketMessage message = createMessage("CREATE");

				message.setGraphObject(node);
				message.setResult(Arrays.asList(new GraphObject[] { node } ));

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE");

				message.setGraphObject(node);
				message.setResult(Arrays.asList(new GraphObject[] { node } ));
				message.setId(node.getUuid());
				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));

				return message;
			}

		} else {

			// handle relationship
			final RelationshipInterface relationship = (RelationshipInterface)modificationEvent.getGraphObject();
			final RelationshipType relType           = modificationEvent.getRelationshipType();

			// only interested in CONTAINS relationships
			if (!("CONTAINS".equals(relType.name()))) {
				return null;
			}

			if (modificationEvent.isDeleted()) { // && "CONTAINS".equals(relType.name())) {

				final WebSocketMessage message = createMessage("REMOVE_CHILD");

				message.setNodeData("parentId", relationship.getSourceNodeId());
				message.setId(relationship.getTargetNodeId());

				return message;
			}

			if (modificationEvent.isCreated()) {


				final WebSocketMessage message = new WebSocketMessage();
				final NodeInterface startNode  = relationship.getSourceNode();
				final NodeInterface endNode    = relationship.getTargetNode();

				message.setResult(Arrays.asList(new GraphObject[] { endNode }));
				message.setId(endNode.getUuid());
				message.setNodeData("parentId", startNode.getUuid());

				message.setCommand("APPEND_CHILD");

				if (endNode instanceof DOMNode) {

					org.w3c.dom.Node refNode = ((DOMNode) endNode).getNextSibling();
					if (refNode != null) {

						message.setCommand("INSERT_BEFORE");
						message.setNodeData("refId", ((AbstractNode) refNode).getUuid());
					}

				} else if (endNode instanceof User) {

					message.setCommand("APPEND_USER");
					message.setNodeData("refId", startNode.getUuid());
				}

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE");

				message.setGraphObject(relationship);
				message.setId(relationship.getUuid());
				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));

				final PropertyMap relProperties = relationship.getProperties();
				final NodeInterface startNode   = relationship.getSourceNode();
				final NodeInterface endNode     = relationship.getTargetNode();

				relProperties.put(new StringProperty("startNodeId"), startNode.getUuid());
				relProperties.put(new StringProperty("endNodeId"), endNode.getUuid());

				final Map<String, Object> properties = PropertyMap.javaTypeToInputType(securityContext, relationship.getClass(), relProperties);

				message.setRelData(properties);

				return message;
			}

		}

		return null;
	}

	private WebSocketMessage createMessage(final String command) {

		final WebSocketMessage newMessage = new WebSocketMessage();

		newMessage.setCommand(command);

		return newMessage;
	}
}
