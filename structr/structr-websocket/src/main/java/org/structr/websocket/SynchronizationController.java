/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import org.eclipse.jetty.websocket.WebSocket.Connection;

import org.neo4j.graphdb.RelationshipType;

import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class SynchronizationController implements VetoableGraphObjectListener {

	private static final Logger logger = Logger.getLogger(SynchronizationController.class.getName());

	//~--- fields ---------------------------------------------------------

	private Set<StructrWebSocket> clients                     = null;
	private Gson gson                                         = null;
	private Map<Long, List<WebSocketMessage>> messageStackMap = new LinkedHashMap<Long, List<WebSocketMessage>>();
	private List<WebSocketMessage> messageStack;

	//~--- constructors ---------------------------------------------------

	public SynchronizationController(Gson gson) {

		this.clients = new LinkedHashSet<StructrWebSocket>();
		this.gson    = gson;

	}

	//~--- methods --------------------------------------------------------

	public void registerClient(StructrWebSocket client) {

		clients.add(client);

	}

	public void unregisterClient(StructrWebSocket client) {

		clients.remove(client);

	}

	// ----- private methods -----
	private void broadcast(final WebSocketMessage webSocketData) {

		logger.log(Level.FINE, "Broadcasting message to {0} clients..", clients.size());

		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		String message;

		// create message
		for (StructrWebSocket socket : clients) {

			Connection socketConnection = socket.getConnection();

			if ((socketConnection != null) && socket.isAuthenticated()) {

				List<GraphObject> result = webSocketData.getResult();

				if ((result != null) && (result.size() > 0)
					&& (webSocketData.getCommand().equals("UPDATE") || webSocketData.getCommand().equals("ADD") || webSocketData.getCommand().equals("CREATE"))) {

					WebSocketMessage clientData     = webSocketData.copy();
					SecurityContext securityContext = socket.getSecurityContext();

					clientData.setResult(filter(securityContext, result));

					message = gson.toJson(clientData, WebSocketMessage.class);

				} else {

					message = gson.toJson(webSocketData, WebSocketMessage.class);
				}

				logger.log(Level.FINE, "############################################################ SENDING \n{0}", message);

				try {

					socketConnection.sendMessage(message);

				} catch (org.eclipse.jetty.io.EofException eof) {

					logger.log(Level.FINE, "EofException irgnored, may occour on SSL connections.", eof);

				} catch (Throwable t) {

					logger.log(Level.WARNING, "Error sending message to client.", t);

				}

			}

		}

	}

	private List<GraphObject> filter(final SecurityContext securityContext, final List<GraphObject> all) {

		List<GraphObject> filteredResult = new LinkedList<GraphObject>();

		for (GraphObject obj : all) {

			if (securityContext.isVisible((AbstractNode) obj)) {

				filteredResult.add(obj);
			}

		}

		return filteredResult;

	}

	// ----- interface VetoableGraphObjectListener -----
	@Override
	public boolean begin(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

		messageStack = new LinkedList<WebSocketMessage>();

		messageStackMap.put(transactionKey, messageStack);

		return false;

	}

	@Override
	public boolean commit(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

		messageStack = messageStackMap.get(transactionKey);

		if (messageStack != null) {

			for (WebSocketMessage message : messageStack) {

				broadcast(message);
			}

		} else {

			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		messageStackMap.remove(transactionKey);

		return false;

	}

	@Override
	public boolean rollback(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer) {

		// roll back transaction
		messageStackMap.remove(transactionKey);

		return false;
	}

	@Override
	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, String key, Object oldValue, Object newValue) {

		messageStack = messageStackMap.get(transactionKey);

		WebSocketMessage message = new WebSocketMessage();

		message.setCommand("UPDATE");

		String uuid = graphObject.getStringProperty(AbstractNode.Key.uuid.name());
		
		if (graphObject instanceof AbstractRelationship) {

			AbstractRelationship relationship = (AbstractRelationship) graphObject;
			AbstractNode startNode            = relationship.getStartNode();
			AbstractNode endNode              = relationship.getEndNode();
			Map<String, Object> relProperties = relationship.getProperties();

			relProperties.put("startNodeId", startNode.getUuid());
			relProperties.put("endNodeId", endNode.getUuid());
			message.setRelData(relProperties);

		}

		message.setId(uuid);
		message.setGraphObject(graphObject);
		message.getModifiedProperties().add(key);
		messageStack.add(message);

		return false;

	}

	@Override
	public boolean propertyRemoved(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, String key, Object oldValue) {

		messageStack = messageStackMap.get(transactionKey);

		WebSocketMessage message = new WebSocketMessage();

		message.setCommand("UPDATE");

		String uuid = graphObject.getStringProperty(AbstractNode.Key.uuid.name());

		if (graphObject instanceof AbstractRelationship) {

			AbstractRelationship relationship = (AbstractRelationship) graphObject;
			AbstractNode startNode            = relationship.getStartNode();
			AbstractNode endNode              = relationship.getEndNode();
			Map<String, Object> relProperties = relationship.getProperties();

			relProperties.put("startNodeId", startNode.getUuid());
			relProperties.put("endNodeId", endNode.getUuid());
			message.setRelData(relProperties);

		}

		message.setId(uuid);
		message.setGraphObject(graphObject);
		message.getRemovedProperties().add(key);
		messageStack.add(message);

		return false;

	}

	@Override
	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

		messageStack = messageStackMap.get(transactionKey);

		AbstractRelationship relationship;

		if (graphObject instanceof AbstractRelationship) {

			relationship = (AbstractRelationship) graphObject;

			if (relationship.getRelType().equals(RelType.CONTAINS)) {

				AbstractNode startNode   = relationship.getStartNode();
				AbstractNode endNode     = relationship.getEndNode();
				WebSocketMessage message = new WebSocketMessage();

				message.setCommand("ADD");
				message.setGraphObject(relationship);
				message.setId(startNode.getStringProperty("uuid"));
				message.setResult(Arrays.asList(new GraphObject[] { endNode }));

				String pageId = relationship.getStringProperty("pageId");

				if (pageId != null) {

					Map<String, Object> props = new LinkedHashMap<String, Object>();

					props.put("pageId", pageId);
					message.setNodeData(props);

				}

				messageStack.add(message);
				logger.log(Level.FINE, "Relationship created: {0}({1} -> {2}{3}", new Object[] { startNode.getId(), startNode.getStringProperty(AbstractNode.Key.uuid),
					endNode.getStringProperty(AbstractNode.Key.uuid) });

			}

			return false;

		} else {

			WebSocketMessage message = new WebSocketMessage();

			message.setCommand("CREATE");
			message.setGraphObject(graphObject);

			List<GraphObject> list = new LinkedList<GraphObject>();

			list.add(graphObject);
			message.setResult(list);
			messageStack.add(message);
			logger.log(Level.FINE, "Node created: {0}", ((AbstractNode) graphObject).getStringProperty(AbstractNode.Key.uuid));

			return false;

		}

	}

	@Override
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

//              messageStack = messageStackMap.get(transactionKey);
//
//              WebSocketMessage message = new WebSocketMessage();
//              String uuid              = graphObject.getProperty("uuid").toString();
//
//              message.setId(uuid);
//              message.setCommand("UPDATE");
//              message.setGraphObject(graphObject);
//              messageStack.add(message);
		return false;
	}

	@Override
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject obj, Map<String, Object> properties) {

		messageStack = messageStackMap.get(transactionKey);

		AbstractRelationship relationship;

		if ((obj != null) && (obj instanceof AbstractRelationship)) {

			relationship = (AbstractRelationship) obj;

			// do not access nodes of delete relationships
			// AbstractNode startNode   = relationship.getStartNode();
			// AbstractNode endNode     = relationship.getEndNode();
			WebSocketMessage message = new WebSocketMessage();
			String startNodeId       = relationship.getCachedStartNodeId();
			String endNodeId         = relationship.getCachedEndNodeId();
			String pageId	 = (String) properties.get("pageId");

			if ((startNodeId != null) && (endNodeId != null)) {

				message.setCommand("REMOVE");
				message.setGraphObject(relationship);
				message.setId(startNodeId);
				message.setNodeData("id", endNodeId);
				message.setNodeData("pageId", pageId);
				messageStack.add(message);

			}

			// logger.log(Level.FINE, "{0} -> {1}", new Object[] { startNode.getId(), endNode.getId() });
			return false;

		} else {

			WebSocketMessage message = new WebSocketMessage();
			String uuid              = properties.get("uuid").toString();

			message.setId(uuid);
			message.setCommand("DELETE");
			messageStack.add(message);

			return false;

		}

	}

	@Override
	public boolean wasVisited(List<GraphObject> traversedNodes, long transactionKey, ErrorBuffer errorBuffer, SecurityContext securityContext) {

		return false;

	}

}
