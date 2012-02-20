/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.websocket;

import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class SynchronizationController implements VetoableGraphObjectListener {

	private static final Logger logger = Logger.getLogger(SynchronizationController.class.getName());

	private Map<Long, WebSocketMessage> messageMap = new LinkedHashMap<Long, WebSocketMessage>();
	private Set<StructrWebSocket> clients          = null;
	private Gson gson                              = null;
	
	public SynchronizationController(Gson gson) {
		this.clients = new LinkedHashSet<StructrWebSocket>();
		this.gson = gson;
	}

	public void registerClient(StructrWebSocket client) {
		clients.add(client);
	}

	public void unregisterClient(StructrWebSocket client) {
		clients.remove(client);
	}

	// ----- private methods -----
	private void broadcast(final WebSocketMessage webSocketData) {

		logger.log(Level.INFO, "Broadcasting message to {0} clients..", clients.size());

		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		// create message
		String message = gson.toJson(webSocketData, WebSocketMessage.class);
		logger.log(Level.INFO, "############################################################ SENDING \n{0}", message);

		for (StructrWebSocket socket : clients) {

			Connection socketConnection = socket.getConnection();
			if ((socketConnection != null) && socket.isAuthenticated()) {

				try {
					socketConnection.sendMessage(message);

				} catch (Throwable t) {
					logger.log(Level.WARNING, "Error sending message to client.", t);
				}
			}
		}
	}

	// ----- interface VetoableGraphObjectListener -----
	@Override
	public boolean begin(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {
		messageMap.put(transactionKey, new WebSocketMessage());
		return false;
	}

	@Override
	public boolean commit(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {
			broadcast(message);

		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		messageMap.remove(transactionKey);
		return false;
	}

	@Override
	public boolean rollback(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

		// roll back transaction
		messageMap.remove(transactionKey);
		return false;
	}

	@Override
	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, String key, Object oldValue, Object newValue) {

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {
			// message.setData(key, newValue != null ? newValue.toString() : "null");
			message.getModifiedProperties().add(key);
		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}
		return false;
	}

	public boolean relationshipCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractRelationship relationship) {

		AbstractNode startNode = relationship.getStartNode();
		AbstractNode endNode = relationship.getEndNode();

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {

			message.setCommand("ADD");
			message.setGraphObject(relationship);
			message.setId(startNode.getStringProperty("uuid"));
			message.setData("id", endNode.getStringProperty("uuid"));

		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		logger.log(Level.INFO, "{0} -> {1}", new Object[] { startNode.getId(), endNode.getId() } );
		return false;
	}

	public boolean relationshipDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, AbstractRelationship relationship) {

		AbstractNode startNode = relationship.getStartNode();
		AbstractNode endNode = relationship.getEndNode();

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {

			message.setCommand("REMOVE");
			message.setGraphObject(relationship);
			message.setId(startNode.getStringProperty("uuid"));
			message.setData("id", endNode.getStringProperty("uuid"));


		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		logger.log(Level.INFO, "{0} -> {1}", new Object[] { startNode.getId(), endNode.getId() } );
		return false;
	}

	@Override
	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

		if(graphObject instanceof AbstractRelationship) {

			return relationshipCreated(securityContext, transactionKey, errorBuffer, (AbstractRelationship)graphObject);

		} else {

			WebSocketMessage message = messageMap.get(transactionKey);
			if(message != null) {

				message.setCommand("CREATE");
				message.setGraphObject(graphObject);

				List<GraphObject> list = new LinkedList<GraphObject>();
				list.add(graphObject);
				message.setResult(list);

			} else {
				logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
			}
			return false;
		}
	}

	@Override
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {
			String uuid = graphObject.getProperty("uuid").toString();
			message.setId(uuid);
			message.setCommand("UPDATE");
			message.setGraphObject(graphObject);

		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		return false;
	}

	@Override
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, Map<String, Object> properties) {

		WebSocketMessage message = messageMap.get(transactionKey);
		if(message != null) {
			String uuid = properties.get("uuid").toString();
			message.setId(uuid);
			message.setCommand("DELETE");
		} else {
			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}
		return false;
	}

	@Override
	public boolean wasVisited(List<GraphObject> traversedNodes, long transactionKey, ErrorBuffer errorBuffer, SecurityContext securityContext) {
		return false;
	}
}
