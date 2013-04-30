/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket;

import com.google.gson.Gson;

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.URIUtil;

import org.eclipse.jetty.websocket.WebSocket.Connection;
import static org.mockito.Mockito.mock;
import org.structr.common.AccessMode;

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.TransactionChangeSet;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.rest.ResourceProvider;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class SynchronizationController implements StructrTransactionListener {

	private static final Logger logger                              = Logger.getLogger(SynchronizationController.class.getName());

	private final Map<Long, List<WebSocketMessage>> messageStackMap = new LinkedHashMap<Long, List<WebSocketMessage>>();
	private final Set<StructrWebSocket> clients                     = new LinkedHashSet<StructrWebSocket>();
	private List<WebSocketMessage> messageStack                     = null;
	private ResourceProvider resourceProvider                       = null;
	private Gson gson                                               = null;

	public SynchronizationController(Gson gson) {

		this.gson = gson;

	}

	public void registerClient(StructrWebSocket client) {

		synchronized (clients) {

			clients.add(client);
		}

	}

	public void unregisterClient(StructrWebSocket client) {

		synchronized (clients) {

			clients.remove(client);
		}

	}
	
	public void setResourceProvider(final ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}

	// ----- private methods -----
	private void broadcast(final WebSocketMessage webSocketData) {

		logger.log(Level.FINE, "Broadcasting message to {0} clients..", clients.size());

		// session must be valid to be received by the client
		webSocketData.setSessionValid(true);

		String message;
		String pagePath = (String) webSocketData.getNodeData().get("pagePath");

		//synchronized (clients) {

			// create message
			for (StructrWebSocket socket : clients) {
				
				String clientPagePath = socket.getPathPath();
				if (clientPagePath != null && !clientPagePath.equals(URIUtil.encodePath(pagePath))) {
					continue;
				}
				
				Connection socketConnection = socket.getConnection();

				webSocketData.setCallback(socket.getCallback());

				if ((socketConnection != null)) { //&& socket.isAuthenticated()) {

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
		//}

	}

	private <T extends GraphObject> List<T> filter(final SecurityContext securityContext, final List<T> all) {

		List<T> filteredResult = new LinkedList<T>();
		for (T obj : all) {
			
			if (securityContext.isVisible((AbstractNode)obj)) {

				filteredResult.add(obj);
			}
		}

		return filteredResult;

	}
	
	private void broadcastPartials(String type) throws FrameworkException {

		// create list of dynmiac elements
		SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();
		List<SearchAttribute> attrs              = new LinkedList<SearchAttribute>();

		// Find all DOMElements which render data of the type of the obj
		attrs.add(Search.andExactTypeAndSubtypes(DOMElement.class.getSimpleName()));
		SearchAttributeGroup g = new SearchAttributeGroup(SearchOperator.AND);
		g.add(Search.orExactProperty(DOMElement.dataKey, EntityContext.denormalizeEntityName(type)));
		g.add(Search.orExactProperty(DOMElement.partialUpdateKey, EntityContext.denormalizeEntityName(type)));
		attrs.add(g);

		Result results = Services.command(superUserSecurityContext, SearchNodeCommand.class).execute(attrs);
		List<DOMElement> dynamicElements = results.getResults();
			
		// create message
		for (StructrWebSocket socket : clients) {

			SecurityContext securityContext = socket.getSecurityContext();
			
			// For non-authenticated clients, construct a security context without user
			if (securityContext == null) {

				try {

					securityContext = SecurityContext.getInstance(null, AccessMode.Frontend);

				} catch (FrameworkException ex) {

					continue;
				}
			}
			
			// filter elements
			List<DOMElement> filteredElements = filter(securityContext, dynamicElements);
			List<WebSocketMessage> partialMessages = createPartialMessages(securityContext, filteredElements);

			for (WebSocketMessage webSocketData : partialMessages) {
				
				webSocketData.setSessionValid(true);

				String pagePath = (String) webSocketData.getNodeData().get("pagePath");
				String clientPagePath = socket.getPathPath();
				
				if (clientPagePath != null && !clientPagePath.equals(URIUtil.encodePath(pagePath))) {
					continue;
				}

				Connection socketConnection = socket.getConnection();

				webSocketData.setCallback(socket.getCallback());

				if ((socketConnection != null)) {

					String message = gson.toJson(webSocketData, WebSocketMessage.class);

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
	}

	private List<WebSocketMessage> createPartialMessages(SecurityContext securityContext, List<DOMElement> elements) {
		
		HttpServletRequest request             = mock(HttpServletRequest.class);
		List<WebSocketMessage> partialMessages = new LinkedList<WebSocketMessage>();
		RenderContext ctx                      = new RenderContext(request, null, false, Locale.GERMAN);
		
		ctx.setResourceProvider(resourceProvider);
		
		for (DOMElement el : elements) {
			
			try {
				
				Page page = el.getProperty(DOMNode.ownerDocument);
				if (page != null) {
					
					DOMElement parent = (DOMElement) el.getParentNode();
				
					if (parent != null) {
						
						parent.render(securityContext, ctx, 0);
				
						String partialContent = ctx.getBuffer().toString();

						WebSocketMessage message = new WebSocketMessage();

						message.setCommand("PARTIAL");

						message.setNodeData("pageId", page.getUuid());
						message.setNodeData("pagePath", "/" + page.getName());
						message.setNodeData("parentPositionPath", parent.getPositionPath());

						message.setMessage(StringUtils.remove(partialContent, "\n"));

						partialMessages.add(message);
					}
					
				}
				
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
			
		}

		return partialMessages;
	}
	
	/*
	private void sendPartial(SecurityContext securityContext, String type) {
		
		List<DOMElement> dynamicElements = null;
		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

		// Find all DOMElements which render data of the type of the obj
		attrs.add(Search.andExactTypeAndSubtypes(DOMElement.class.getSimpleName()));
		SearchAttributeGroup g = new SearchAttributeGroup(SearchOperator.AND);
		g.add(Search.orExactProperty(DOMElement.dataKey, EntityContext.denormalizeEntityName(type)));
		g.add(Search.orExactProperty(DOMElement.partialUpdateKey, EntityContext.denormalizeEntityName(type)));
		attrs.add(g);

		try {
			Result results = Services.command(securityContext, SearchNodeCommand.class).execute(attrs);
			
			dynamicElements = results.getResults();
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Something went wrong while searching for dynamic elements of type " + type, ex);
		}
		
		HttpServletRequest request = mock(HttpServletRequest.class);
		RenderContext ctx = new RenderContext(request, null, false, Locale.GERMAN);
		ctx.setResourceProvider(resourceProvider);
		
		for (DOMElement el : dynamicElements) {
			
			logger.log(Level.FINE, "Found dynamic element for type {0}: {1}", new Object[]{type, el});
			
			try {
				
				Page page = el.getProperty(DOMNode.ownerDocument);
				
				// render only when contained in a page
				if (page != null) {
					
					DOMElement parent = (DOMElement) el.getParentNode();
				
					if (parent != null) {
						
						parent.render(securityContext, ctx, 0);
				
						String partialContent = ctx.getBuffer().toString();

						logger.log(Level.FINE, "Partial output:\n{0}", partialContent);

						WebSocketMessage message = new WebSocketMessage();

						message.setCommand("PARTIAL");

						message.setNodeData("pageId", page.getUuid());

						String pageName = page.getName();

						message.setNodeData("pagePath", "/" + pageName);
						message.setMessage(StringUtils.remove(partialContent, "\n"));
						message.setNodeData("parentPositionPath", parent.getPositionPath());

						broadcast(message);
					}
					
				}
				
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
			
			
		}
	}
	*/
	
	// ----- interface StructrTransactionListener -----
	@Override
	public void commitStarts(SecurityContext securityContext, long transactionKey) {

		messageStack = new LinkedList<WebSocketMessage>();

		messageStackMap.put(transactionKey, messageStack);
	}

	@Override
	public void commitFinishes(SecurityContext securityContext, long transactionKey) {

		messageStack = messageStackMap.get(transactionKey);

		if (messageStack != null) {

			for (WebSocketMessage message : messageStack) {

				broadcast(message);
			}

		} else {

			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}

		messageStackMap.remove(transactionKey);

	}

	@Override
	public void willRollback(SecurityContext securityContext, long transactionKey) {

		// roll back transaction
		messageStackMap.remove(transactionKey);
	}

	@Override
	public void afterCommit(SecurityContext securityContext, long transactionKey) {
		
		TransactionChangeSet changeSet = EntityContext.getTransactionChangeSet(transactionKey);
		if (changeSet != null) {

			Set<String> types = new LinkedHashSet<String>();
			
			for (AbstractNode node : changeSet.getCreatedNodes()) {
				types.add(node.getType());
			}
			
			for (AbstractNode node : changeSet.getDeletedNodes()) {
				types.add(node.getType());
			}
			
			for (AbstractNode node : changeSet.getModifiedNodes()) {
				types.add(node.getType());
			}
			
			// broadcast partials
			for (String type : types) {
				
				try {
					
					if (type != null) {
						
						broadcastPartials(type);
					}
					
				} catch (Throwable t) {
					
					logger.log(Level.WARNING, "Unable to broadcast partials for type {0}: {1}", new Object[] { type, t.getMessage() } );
				}
			}
		} else {
			
			logger.log(Level.WARNING, "Unable to broadcast partial updates, changeSet not found");
		}
		
	}
	
	@Override
	public boolean propertyModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue, Object newValue) {

		messageStack = messageStackMap.get(transactionKey);

		WebSocketMessage message = new WebSocketMessage();

		message.setCommand("UPDATE");

		String uuid = graphObject.getProperty(AbstractNode.uuid);

		if (graphObject instanceof AbstractRelationship) {

			AbstractRelationship relationship = (AbstractRelationship) graphObject;
			AbstractNode startNode            = relationship.getStartNode();
			AbstractNode endNode              = relationship.getEndNode();

			try {

				PropertyMap relProperties = relationship.getProperties();

				relProperties.put(new StringProperty("startNodeId"), startNode.getUuid());
				relProperties.put(new StringProperty("endNodeId"), endNode.getUuid());

				Map<String, Object> properties = PropertyMap.javaTypeToInputType(securityContext, relationship.getClass(), relProperties);

				message.setRelData(properties);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to convert properties from type {0} to input type", relationship.getClass());

			}

		} else if (graphObject instanceof AbstractNode) {

			AbstractNode node = (AbstractNode) graphObject;

			try {

				Map<String, Object> nodeProperties = new HashMap<String, Object>();

				nodeProperties.put(key.dbName(), newValue);
				nodeProperties.put(AbstractNode.type.dbName(), node.getType());    // needed for type resolution

				Map<String, Object> properties = PropertyMap.javaTypeToInputType(securityContext, node.getClass(),
									 PropertyMap.databaseTypeToJavaType(securityContext, graphObject, nodeProperties));

				properties.remove(AbstractNode.type.jsonName());    // remove type again
				message.setNodeData(properties);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to convert properties from type {0} to input type", node.getClass());

			}

		}

		message.setId(uuid);
		message.setGraphObject(graphObject);
		message.getModifiedProperties().add(key);
		messageStack.add(message);

		return true;

	}

	@Override
	public boolean propertyRemoved(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyKey key, Object oldValue) {

		messageStack = messageStackMap.get(transactionKey);

		WebSocketMessage message = new WebSocketMessage();

		message.setCommand("UPDATE");

		String uuid = graphObject.getProperty(AbstractNode.uuid);

		if (graphObject instanceof AbstractRelationship) {

			AbstractRelationship relationship = (AbstractRelationship) graphObject;
			AbstractNode startNode            = relationship.getStartNode();
			AbstractNode endNode              = relationship.getEndNode();

			try {

				PropertyMap relProperties = relationship.getProperties();

				relProperties.put(new StringProperty("startNodeId"), startNode.getUuid());
				relProperties.put(new StringProperty("endNodeId"), endNode.getUuid());

				Map<String, Object> properties = PropertyMap.javaTypeToInputType(securityContext, relationship.getClass(), relProperties);

				message.setRelData(properties);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to convert properties from type {0} to input type", relationship.getClass());

			}

		}

		message.setId(uuid);
		message.setGraphObject(graphObject);
		message.getRemovedProperties().add(key);
		messageStack.add(message);

		return true;

	}

	@Override
	public boolean graphObjectCreated(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {

		messageStack = messageStackMap.get(transactionKey);

		AbstractRelationship relationship;

		if (graphObject instanceof AbstractRelationship) {

			relationship = (AbstractRelationship) graphObject;

			if (!ignoreRelationship(relationship)) {

				AbstractNode startNode   = (AbstractNode) relationship.getStartNode();
				AbstractNode endNode     = (AbstractNode) relationship.getEndNode();
				WebSocketMessage message = new WebSocketMessage();

				message.setResult(Arrays.asList(new GraphObject[] { endNode }));
				message.setId(endNode.getUuid());
				message.setNodeData("parentId", startNode.getUuid());

				org.w3c.dom.Node refNode = null;

				message.setCommand("APPEND_CHILD");

				if (endNode instanceof DOMNode) {

					refNode = ((DOMNode) endNode).getNextSibling();
					if (refNode != null) {
						message.setCommand("INSERT_BEFORE");
						message.setNodeData("refId", ((AbstractNode) refNode).getUuid());
					} else {
					}
				
				} else if (endNode instanceof User) {
					
					message.setCommand("APPEND_USER");
					message.setNodeData("refId", startNode.getUuid());
					
				}

				// message.setResult(Arrays.asList(new GraphObject[] { endNode }));
				messageStack.add(message);
				logger.log(Level.FINE, "Relationship created: {0}({1} -> {2}{3}", new Object[] { startNode.getId(), startNode.getProperty(AbstractNode.uuid),
					endNode.getProperty(AbstractNode.uuid) });

			}

		} else {

			WebSocketMessage message = new WebSocketMessage();

			message.setCommand("CREATE");
			message.setGraphObject(graphObject);

			List<GraphObject> list = new LinkedList<GraphObject>();

			list.add(graphObject);
			
			message.setResult(list);
			messageStack.add(message);
			logger.log(Level.FINE, "Node created: {0}", ((AbstractNode) graphObject).getProperty(AbstractNode.uuid));

		}

		return true;

	}

	@Override
	public boolean graphObjectModified(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject) {
		return true;
	}

	@Override
	public boolean graphObjectDeleted(SecurityContext securityContext, long transactionKey, ErrorBuffer errorBuffer, GraphObject graphObject, PropertyMap properties) {

		messageStack = messageStackMap.get(transactionKey);

		AbstractRelationship relationship;

		if ((graphObject != null) && (graphObject instanceof AbstractRelationship)) {

			relationship = (AbstractRelationship) graphObject;

			if (ignoreRelationship(relationship)) {

				return true;
			}

			// do not access nodes of delete relationships
			// AbstractNode startNode   = relationship.getStartNode();
			// AbstractNode endNode     = relationship.getEndNode();
			WebSocketMessage message = new WebSocketMessage();
			String startNodeId       = relationship.getCachedStartNodeId();
			String endNodeId         = relationship.getCachedEndNodeId();

			if ((startNodeId != null) && (endNodeId != null)) {

				message.setCommand("REMOVE_CHILD");

				// message.setGraphObject(relationship);
				message.setId(endNodeId);
				message.setNodeData("parentId", startNodeId);

//                              message.setNodeData("pageId", pageId);
				messageStack.add(message);

			}

			// logger.log(Level.FINE, "{0} -> {1}", new Object[] { startNode.getId(), endNode.getId() });

		} else {

			WebSocketMessage message = new WebSocketMessage();
			String uuid              = properties.get(AbstractNode.uuid).toString();

			message.setId(uuid);
			message.setCommand("DELETE");
			
			messageStack.add(message);

		}

		return true;

	}

	/**
	 * Ignore all relationships which should not trigger an UI update at all
	 * 
	 * @param rel
	 * @return 
	 */
	private boolean ignoreRelationship(final AbstractRelationship rel) {

		String relType = rel.getRelType().name();

		if (relType.equals("CONTAINS")) {

			return false;
			
		} else {
			
			return true;
			
		}
	}

}
