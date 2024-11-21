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
package org.structr.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 */
public class WebsocketController implements StructrTransactionListener {

	private static final Logger logger                 = LoggerFactory.getLogger(WebsocketController.class.getName());
	private static final Set<String> BroadcastCommands = new HashSet<>(Arrays.asList("UPDATE", "ADD", "CREATE"));

	private final Set<StructrWebSocket> clients = ConcurrentHashMap.newKeySet();
	private Gson gson                           = null;

	private static final Set<String> BroadcastBlacklistForNodeTypes           = new HashSet<>(Arrays.asList("IndexedWord"));
	private static final Set<PropertyKey> BroadcastBlacklistForNodeProperties = new HashSet<>(Arrays.asList(Principal.grantedNodes, Principal.ownedNodes));
	private static final Set<String> BroadcastBlacklistForRelTypes            = new HashSet<>(Arrays.asList("INDEXED_WORD"));

	public WebsocketController(final Gson gson) {

		this.gson = gson;
	}

	public void registerClient(final StructrWebSocket client) {

		clients.add(client);
	}

	public void unregisterClient(final StructrWebSocket client) {

		clients.remove(client);
	}

	private void broadcast(final WebSocketMessage webSocketData) {

		broadcast(webSocketData, null);
	}

	private void broadcast(final WebSocketMessage webSocketData, final Predicate<String> receiverSessionPredicate) {

		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		final String pagePath                        = webSocketData.getNodeDataStringValue("pagePath");
		final String encodedPath                     = URIUtil.encodePath(pagePath);
		final List<StructrWebSocket> clientsToRemove = new LinkedList<>();
		final Iterable<? extends GraphObject> result = webSocketData.getResult();
		final String command                         = webSocketData.getCommand();
		final GraphObject obj                        = webSocketData.getGraphObject();

		String message;

		// create message
		for (StructrWebSocket socket : clients) {

			String clientPagePath = socket.getPagePath();
			if (clientPagePath != null && !clientPagePath.equals(encodedPath)) {
				continue;
			}

			Session session = socket.getSession();

			if (session != null && socket.isAuthenticated()) {

				final SecurityContext securityContext = socket.getSecurityContext();

				if (receiverSessionPredicate != null && !receiverSessionPredicate.accept(securityContext.getSessionId())) {
					continue;
				}

				if (result != null && BroadcastCommands.contains(command)) {

					final WebSocketMessage clientData = webSocketData.copy();

					clientData.setResult(filter(securityContext, result));

					message = gson.toJson(clientData, WebSocketMessage.class);

				} else {

					message = gson.toJson(webSocketData, WebSocketMessage.class);
				}

				try {

					session.getRemote().sendString(message);

				} catch (Throwable t) {

					if (t instanceof WebSocketException) {

						WebSocketException wse = (WebSocketException) t;

						if ("RemoteEndpoint unavailable, current state [CLOSED], expecting [OPEN or CONNECTED]".equals(wse.getMessage())) {
							clientsToRemove.add(socket);
						}
					}

					logger.debug("Error sending message to client.", t);
				}
			}
		}

		for (StructrWebSocket s : clientsToRemove) {

			unregisterClient(s);

			logger.warn("Client removed from broadcast list: {}", s);
		}
	}

	private <T extends GraphObject> Iterable<T> filter(final SecurityContext securityContext, final Iterable<T> all) {
		return Iterables.filter(e -> { return securityContext.isVisible((AccessControllable)e); }, all);
	}

	// ----- interface StructrTransactionListener -----
	@Override
	public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {
	}

	@Override
	public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {

		for (final ModificationEvent event : modificationEvents) {

			try {
				final WebSocketMessage message = getMessageForEvent(securityContext, event);
				if (message != null) {

					broadcast(message);
				}

			} catch (FrameworkException ignore) {
			}
		}
	}

	@Override
	public void simpleBroadcast(final String commandName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {
		broadcast(MessageBuilder.forName(commandName).data(data).build(), sessionIdPredicate);
	}

	// ----- private methods -----
	private WebSocketMessage getMessageForEvent(final SecurityContext securityContext, final ModificationEvent modificationEvent) throws FrameworkException {

		final String callbackId = modificationEvent.getCallbackId();

		if (modificationEvent.isNode()) {

			final NodeInterface node = (NodeInterface) modificationEvent.getGraphObject();

			if (modificationEvent.isDeleted()) {

				final WebSocketMessage message = createMessage("DELETE", callbackId);

				message.setId(modificationEvent.getRemovedProperties().get(GraphObject.id));
				message.setCode(200);

				return message;
			}

			if (BroadcastBlacklistForNodeTypes.contains(node.getType())) {
				return null;
			}
			if (modificationEvent.getModifiedProperties().keySet().stream().anyMatch((property) -> { return BroadcastBlacklistForNodeProperties.contains(property); })) {
				return null;
			}

			if (modificationEvent.isCreated()) {

				final WebSocketMessage message = createMessage("CREATE", callbackId);

				message.setGraphObject(node);
				message.setResult(Arrays.asList(node));
				message.setCode(201);

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE", callbackId);

				// at login the securityContext is still null
				if (securityContext != null) {

					// only include changed properties (+ id and type)
					final LinkedHashSet<String> propertySet = new LinkedHashSet();

					propertySet.add("id");
					propertySet.add("type");

					for (Iterator<PropertyKey> it = modificationEvent.getModifiedProperties().keySet().iterator(); it.hasNext(); ) {

						final String jsonName = ((PropertyKey)it.next()).jsonName();
						if (!propertySet.contains(jsonName)) {

							propertySet.add(jsonName);
						}
					}

					for (Iterator<PropertyKey> it = modificationEvent.getRemovedProperties().keySet().iterator(); it.hasNext(); ) {

						final String jsonName = ((PropertyKey)it.next()).jsonName();
						if (!propertySet.contains(jsonName)) {

							propertySet.add(jsonName);
						}
					}

					if (propertySet.size() > 2) {
						securityContext.setCustomView(propertySet);
					}
				}

				message.setGraphObject(node);
				message.setResult(Arrays.asList(node));
				message.setId(node.getUuid());
				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));
				message.setCode(200);

				if (securityContext != null) {
					// Clear custom view here. This is necessary because the security context is reused for all websocket frames.
					securityContext.clearCustomView();
				}

				return message;
			}

		} else {

			// handle relationship
			final RelationshipInterface relationship = (RelationshipInterface) modificationEvent.getGraphObject();
			final RelationshipType relType = modificationEvent.getRelationshipType();

			if (BroadcastBlacklistForRelTypes.contains(relType.name())) {
				return null;
			}

			// special treatment of CONTAINS relationships
			if ("CONTAINS".equals(relType.name())) {

				if (modificationEvent.isDeleted()) {

					final WebSocketMessage message = createMessage("REMOVE_CHILD", callbackId);

					message.setNodeData("parentId", relationship.getSourceNodeId());
					message.setId(relationship.getTargetNodeId());
					message.setCode(200);

					return message;
				}

				if (modificationEvent.isCreated()) {

					final WebSocketMessage message = new WebSocketMessage();
					final NodeInterface startNode = relationship.getSourceNode();
					final NodeInterface endNode = relationship.getTargetNode();

					// If either start or end node are not visible for the user to be notified,
					// don't send a notification
					if (startNode == null || endNode == null) {
						return null;
					}

					message.setResult(Arrays.asList(endNode));
					message.setId(endNode.getUuid());
					message.setNodeData("parentId", startNode.getUuid());

					message.setCode(200);
					message.setCommand("APPEND_CHILD");

					if (endNode instanceof DOMNode) {

						org.w3c.dom.Node refNode = ((DOMNode) endNode).getNextSibling();
						if (refNode != null) {

							message.setCommand("INSERT_BEFORE");
							message.setNodeData("refId", ((AbstractNode) refNode).getUuid());
						}

					} else if (endNode instanceof User || endNode instanceof Group) {

						message.setCommand("APPEND_MEMBER");
						message.setNodeData("refId", startNode.getUuid());

					} else if (endNode instanceof AbstractFile) {

						message.setCommand("APPEND_FILE");
						message.setNodeData("refId", startNode.getUuid());
					}

					return message;
				}
			}

			if (modificationEvent.isDeleted()) {

				final WebSocketMessage message = createMessage("DELETE", callbackId);
				message.setId(modificationEvent.getRemovedProperties().get(GraphObject.id));
				message.setCode(200);

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE", callbackId);

				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));

				message.setGraphObject(relationship);
				message.setId(relationship.getUuid());
				message.setCode(200);

				final PropertyMap relProperties = relationship.getProperties();
				//final NodeInterface startNode = relationship.getSourceNode();
				//final NodeInterface endNode = relationship.getTargetNode();

				//relProperties.put(new StringProperty("startNodeId"), startNode.getUuid());
				//relProperties.put(new StringProperty("endNodeId"), endNode.getUuid());

				final Map<String, Object> properties = PropertyMap.javaTypeToInputType(securityContext, relationship.getClass(), relProperties);

				message.setRelData(properties);

				return message;
			}

		}

		return null;
	}

	private WebSocketMessage createMessage(final String command, final String callbackId) {

		final WebSocketMessage newMessage = new WebSocketMessage();

		newMessage.setCommand(command);
		if (callbackId != null) {
			newMessage.setCallback(callbackId);
		}

		return newMessage;
	}
}
