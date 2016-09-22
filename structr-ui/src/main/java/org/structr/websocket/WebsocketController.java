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
package org.structr.websocket;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.TransactionSource;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class WebsocketController implements StructrTransactionListener {

	private static final Logger logger                 = Logger.getLogger(WebsocketController.class.getName());
	private static final Set<String> BroadcastCommands = new HashSet<>(Arrays.asList(new String[] { "UPDATE", "ADD", "CREATE" } ));

	private final Set<StructrWebSocket> clients = new ConcurrentHashSet<>();
	private Gson gson = null;

	public WebsocketController(final Gson gson) {

		this.gson = gson;

	}

	public void registerClient(final StructrWebSocket client) {

		clients.add(client);

	}

	public void unregisterClient(final StructrWebSocket client) {

		clients.remove(client);

	}

	// ----- private methods -----
	private void broadcast(final WebSocketMessage webSocketData) {

		//logger.log(Level.FINE, "Broadcasting message to {0} clients..", clients.size());
		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		final String pagePath                        = (String) webSocketData.getNodeData().get("pagePath");
		final String encodedPath                     = URIUtil.encodePath(pagePath);
		final List<StructrWebSocket> clientsToRemove = new LinkedList<>();
		final List<? extends GraphObject> result     = webSocketData.getResult();
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

				// if the object IS NOT of type AbstractNode AND the client is NOT priviledged OR
				// if the object IS of type AbstractNode AND the client has no access to the node
				// THEN skip sending a message
				if (obj instanceof AbstractNode) {

					final AbstractNode node = (AbstractNode)obj;

					if (node.isHidden() || !securityContext.isVisible(node)) {
						continue;
					}

				} else {

					if (!socket.isPrivilegedUser(socket.getCurrentUser())) {
						continue;
					}
				}

				if (result != null && !result.isEmpty() && BroadcastCommands.contains(command)) {

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

					logger.log(Level.FINE, "Error sending message to client.", t);
				}

			}

		}

		for (StructrWebSocket s : clientsToRemove) {

			unregisterClient(s);

			logger.log(Level.WARNING, "Client removed from broadcast list: {0}", s);
		}

	}

	private <T extends GraphObject> List<T> filter(final SecurityContext securityContext, final List<T> all) {

		List<T> filteredResult = new LinkedList<>();
		for (T obj : all) {

			if (securityContext.isVisible((AbstractNode) obj)) {

				filteredResult.add(obj);
			}
		}

		return filteredResult;

	}

	// ----- interface StructrTransactionListener -----
	@Override
	public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents, final TransactionSource source) {
	}

	@Override
	public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents, final TransactionSource source) {

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

	// ----- private methods -----
	private WebSocketMessage getMessageForEvent(final SecurityContext securityContext, final ModificationEvent modificationEvent) throws FrameworkException {

		final String callbackId = modificationEvent.getCallbackId();

		if (modificationEvent.isNode()) {

			final NodeInterface node = (NodeInterface) modificationEvent.getGraphObject();

			if (modificationEvent.isDeleted()) {

				final WebSocketMessage message = createMessage("DELETE", callbackId);

				message.setId(modificationEvent.getRemovedProperties().get(GraphObject.id));

				return message;
			}

			if (modificationEvent.isCreated()) {

				final WebSocketMessage message = createMessage("CREATE", callbackId);

				message.setGraphObject(node);
				message.setResult(Arrays.asList(new GraphObject[]{node}));

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE", callbackId);

				message.setGraphObject(node);
				message.setResult(Arrays.asList(new GraphObject[]{node}));
				message.setId(node.getUuid());
				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));

				return message;
			}

		} else {

			// handle relationship
			final RelationshipInterface relationship = (RelationshipInterface) modificationEvent.getGraphObject();
			final RelationshipType relType = modificationEvent.getRelationshipType();

			// special treatment of CONTAINS relationships
			if ("CONTAINS".equals(relType.name())) {

				if (modificationEvent.isDeleted()) {

					final WebSocketMessage message = createMessage("REMOVE_CHILD", callbackId);

					message.setNodeData("parentId", relationship.getSourceNodeId());
					message.setId(relationship.getTargetNodeId());

					return message;
				}

				if (modificationEvent.isCreated()) {

					final WebSocketMessage message = new WebSocketMessage();
					final NodeInterface startNode = relationship.getSourceNode();
					final NodeInterface endNode = relationship.getTargetNode();

					message.setResult(Arrays.asList(new GraphObject[]{endNode}));
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
			}

			if (modificationEvent.isDeleted()) {

				final WebSocketMessage message = createMessage("DELETE", callbackId);
				message.setId(modificationEvent.getRemovedProperties().get(GraphObject.id));

				return message;
			}

			if (modificationEvent.isModified()) {

				final WebSocketMessage message = createMessage("UPDATE", callbackId);

				message.getModifiedProperties().addAll(modificationEvent.getModifiedProperties().keySet());
				message.getRemovedProperties().addAll(modificationEvent.getRemovedProperties().keySet());
				message.setNodeData(modificationEvent.getData(securityContext));

				message.setGraphObject(relationship);
				message.setId(relationship.getUuid());

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
