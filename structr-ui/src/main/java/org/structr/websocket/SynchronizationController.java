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
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.eclipse.jetty.util.URIUtil;

import org.eclipse.jetty.websocket.WebSocket.Connection;
import static org.mockito.Mockito.mock;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.structr.common.AccessMode;

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import static org.structr.core.EntityContext.getPropertyKeyForDatabaseName;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.rest.ResourceProvider;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Html;
import org.w3c.dom.Node;

/**
 *
 * @author Christian Morgner
 */
public class SynchronizationController implements StructrTransactionListener, TransactionEventHandler<Long> {

	private static final Logger logger                              = Logger.getLogger(SynchronizationController.class.getName());

	private final Map<Long, List<WebSocketMessage>> messageStackMap = new ConcurrentHashMap<Long, List<WebSocketMessage>>();
	private final Map<Long, Set<DOMNode>> markupElementsMap         = new ConcurrentHashMap<Long, Set<DOMNode>>();
	private final Map<Long, Set<Class>> typesMap                    = new ConcurrentHashMap<Long, Set<Class>>();
	private final Set<StructrWebSocket> clients                     = new LinkedHashSet<StructrWebSocket>();
	private final AtomicLong transactionCounter                     = new AtomicLong(0);
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
				
				String clientPagePath = socket.getPagePath();
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
	
//	private void broadcastPartials(Set<Class> types, Set<DOMNode> markupElements) {
//
//		// create set of dynamic elements
//		Set<DOMNode> dynamicElements = new HashSet(markupElements);
//		SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance();
//		List<SearchAttribute> attrs              = new LinkedList<SearchAttribute>();
//
//		if (!types.isEmpty()) {
//		
//			// Find all DOMElements which render data of the type of the obj
//			attrs.add(Search.andExactTypeAndSubtypes(DOMElement.class.getSimpleName()));
//			SearchAttributeGroup g = new SearchAttributeGroup(Occur.MUST);
//
//			for (Class type : types) {
//
//				g.add(Search.orExactProperty(superUserSecurityContext, DOMElement.dataKey, EntityContext.denormalizeEntityName(type.getSimpleName())));
//				g.add(Search.orExactProperty(superUserSecurityContext, DOMElement.partialUpdateKey, EntityContext.denormalizeEntityName(type.getSimpleName())));
//
//			}
//
//			attrs.add(g);
//
//			Result results;
//			try {
//
//				results = Services.command(superUserSecurityContext, SearchNodeCommand.class).execute(attrs);
//				dynamicElements.addAll(results.getResults());
//
//			} catch (FrameworkException ex) {}
//		
//		}
//
//		// create message
//		for (StructrWebSocket socket : clients) {
//
//			SecurityContext securityContext = socket.getSecurityContext();
//			
//			// For non-authenticated clients, construct a security context without user
//			if (securityContext == null) {
//
//				try {
//
//					securityContext = SecurityContext.getInstance(null, AccessMode.Frontend);
//
//				} catch (FrameworkException ex) {
//
//					continue;
//				}
//			}
//			
//			broadcastDynamicElements(securityContext, socket, new LinkedList(dynamicElements));
//			
//		}
//	}
//
//	private void broadcastDynamicElements(SecurityContext securityContext, StructrWebSocket socket, final List<DOMNode> dynamicElements) {
//
//		// filter elements
//		List<DOMNode> filteredElements = filter(securityContext, dynamicElements);
//		List<WebSocketMessage> partialMessages = createPartialMessages(securityContext, filteredElements);
//
//		for (WebSocketMessage webSocketData : partialMessages) {
//
//			webSocketData.setSessionValid(true);
//
//			String pagePath = (String) webSocketData.getNodeData().get("pagePath");
//			String clientPagePath = socket.getPagePath();
//
//			if (clientPagePath != null && !clientPagePath.equals(URIUtil.encodePath(pagePath))) {
//				continue;
//			}
//
//			Connection socketConnection = socket.getConnection();
//
//			webSocketData.setCallback(socket.getCallback());
//
//			if ((socketConnection != null)) {
//
//				String message = gson.toJson(webSocketData, WebSocketMessage.class);
//
//				try {
//
//					socketConnection.sendMessage(message);
//
//				} catch (org.eclipse.jetty.io.EofException eof) {
//
//					logger.log(Level.FINE, "EofException irgnored, may occour on SSL connections.", eof);
//
//				} catch (Throwable t) {
//
//					logger.log(Level.WARNING, "Error sending message to client.", t);
//
//				}
//			}
//		}
//	}
//	
//	private List<WebSocketMessage> createPartialMessages(SecurityContext securityContext, List<DOMNode> elements) {
//		
//		HttpServletRequest request             = mock(HttpServletRequest.class);
//		List<WebSocketMessage> partialMessages = new LinkedList<WebSocketMessage>();
//		RenderContext ctx                      = new RenderContext(request, null, false, Locale.GERMAN);
//		
//		ctx.setResourceProvider(resourceProvider);
//		
//		// Get unique parent elements
//		Set<DOMNode> parents = new HashSet();
//		
//		for (DOMNode el : elements) {
//			
//			
//			DOMNode parent = (DOMNode) el.getParentNode();
//			
//			if (parent != null && !(parent instanceof Html)) {
//				
//				boolean ancestorAlreadyInSet = false;
//				for (Node ancestor : parent.getAncestors()) {
//
//					if (parents.contains((DOMNode) ancestor)) {
//						ancestorAlreadyInSet = true;
//					}
//
//				}
//				
//				if (!ancestorAlreadyInSet) {
//					parents.add(parent);
//				}
//			}
//			
//		}
//		
//		for (DOMNode parent : parents) {
//			
//			try {
//				if (parent != null) {
//
//					Page page = parent.getProperty(DOMNode.ownerDocument);
//					
//					if (page != null) {
//
//						parent.render(securityContext, ctx, 0);
//
//						String partialContent = ctx.getBuffer().toString();
//
//						WebSocketMessage message = new WebSocketMessage();
//
//						message.setCommand("PARTIAL");
//
//						message.setNodeData("pageId", page.getUuid());
//						message.setNodeData("pagePath", "/" + page.getName());
//						message.setNodeData("parentPositionPath", parent.getPositionPath());
//
//						message.setMessage(StringUtils.remove(partialContent, "\n"));
//
//						partialMessages.add(message);
//						
//					}
//				}
//				
//			} catch (FrameworkException ex) {
//				logger.log(Level.SEVERE, null, ex);
//			}
//			
//		}
//
//		return partialMessages;
//	}
	
	// ----- interface StructrTransactionListener -----
	@Override
	public void commitStarts(SecurityContext securityContext, long transactionKey) {

		messageStackMap.put(transactionKey, new LinkedList<WebSocketMessage>());
		markupElementsMap.put(transactionKey, new LinkedHashSet<DOMNode>());
		typesMap.put(transactionKey, new LinkedHashSet<Class>());
	}

	@Override
	public void commitFinishes(SecurityContext securityContext, long transactionKey) {

		List<WebSocketMessage> messageStack = messageStackMap.get(transactionKey);

		if (messageStack != null) {

			for (WebSocketMessage message : messageStack) {

				broadcast(message);
			}

		} else {

			logger.log(Level.WARNING, "No message found for transaction key {0}", transactionKey);
		}
	}

	@Override
	public void afterRollback(SecurityContext securityContext, long transactionKey) {

		// roll back transaction
		messageStackMap.remove(transactionKey);
		markupElementsMap.remove(transactionKey);
		typesMap.remove(transactionKey);
	}

	@Override
	public void afterCommit(SecurityContext securityContext, long transactionKey) {
		
		Set<DOMNode> markupElements = markupElementsMap.get(transactionKey);
		Set<Class> types            = typesMap.get(transactionKey);

		//broadcastPartials(types, markupElements);

		// roll back transaction
		messageStackMap.remove(transactionKey);
		markupElementsMap.remove(transactionKey);
		typesMap.remove(transactionKey);
	}
	
	@Override
	public void propertyModified(SecurityContext securityContext, long transactionKey, GraphObject graphObject, PropertyKey key, Object oldValue, Object newValue) {

		List<WebSocketMessage> messageStack = messageStackMap.get(transactionKey);

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

				registerPartialType(graphObject, transactionKey);
			
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
	}

	@Override
	public void propertyRemoved(SecurityContext securityContext, long transactionKey, GraphObject graphObject, PropertyKey key, Object oldValue) {

		List<WebSocketMessage> messageStack = messageStackMap.get(transactionKey);

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

		} else {

			registerPartialType(graphObject, transactionKey);
		}

		message.setId(uuid);
		message.setGraphObject(graphObject);
		message.getRemovedProperties().add(key);
		messageStack.add(message);
	}

	@Override
	public void graphObjectCreated(SecurityContext securityContext, long transactionKey, GraphObject graphObject) {

		List<WebSocketMessage> messageStack = messageStackMap.get(transactionKey);

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

			registerPartialType(graphObject, transactionKey);
			
			WebSocketMessage message = new WebSocketMessage();

			message.setCommand("CREATE");
			message.setGraphObject(graphObject);

			List<GraphObject> list = new LinkedList<GraphObject>();

			list.add(graphObject);
			
			message.setResult(list);
			messageStack.add(message);
			logger.log(Level.FINE, "Node created: {0}", ((AbstractNode) graphObject).getProperty(AbstractNode.uuid));

		}
	}

	@Override
	public void graphObjectModified(SecurityContext securityContext, long transactionKey, GraphObject graphObject) {
	}

	@Override
	public void graphObjectDeleted(SecurityContext securityContext, long transactionKey, GraphObject graphObject, PropertyMap properties) {

		List<WebSocketMessage> messageStack = messageStackMap.get(transactionKey);

		AbstractRelationship relationship;

		if ((graphObject != null) && (graphObject instanceof AbstractRelationship)) {

			relationship = (AbstractRelationship) graphObject;

			if (ignoreRelationship(relationship)) {

				return;
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

	private void registerPartialType(GraphObject graphObject, long transactionKey) {

		if (graphObject instanceof AbstractNode) {
			
			AbstractNode node = (AbstractNode)graphObject;

			// fill type map for partials rendering
			Set<Class> types = typesMap.get(transactionKey);
			types.add(node.getClass());

			/* DISABLED
			Set<DOMNode> markupElememts        = markupElementsMap.get(transactionKey);
			if (node instanceof DOMNode) {
				markupElememts.add((DOMNode)node);
			}
			*/
		}
	}

	// ----- interface TransactionEventHandler -----
	@Override
	public Long beforeCommit(TransactionData data) throws Exception {

		Long transactionKeyValue = transactionCounter.incrementAndGet();
		long transactionKey      = transactionKeyValue.longValue();

		// check if node service is ready
		if (!Services.isReady(NodeService.class)) {

			logger.log(Level.WARNING, "Node service is not ready yet.");
			return transactionKey;

		}

		SecurityContext securityContext                             = SecurityContext.getSuperUserInstance();
		Map<Relationship, Map<String, Object>> removedRelProperties = new LinkedHashMap<Relationship, Map<String, Object>>();
		Map<org.neo4j.graphdb.Node, Map<String, Object>> removedNodeProperties        = new LinkedHashMap<org.neo4j.graphdb.Node, Map<String, Object>>();
		RelationshipFactory relFactory                              = new RelationshipFactory(securityContext);
		NodeFactory nodeFactory                                     = new NodeFactory(securityContext);

		commitStarts(securityContext, transactionKey);

		// collect properties
		collectRemovedNodeProperties(securityContext, transactionKey, data, nodeFactory, removedNodeProperties);
		collectRemovedRelationshipProperties(securityContext, transactionKey, data, relFactory, removedRelProperties);

		// call onCreation
		callOnNodeCreation(securityContext, transactionKey, data, nodeFactory);
		callOnRelationshipCreation(securityContext, transactionKey, data, relFactory);

		// call onDeletion
		callOnRelationshipDeletion(securityContext, transactionKey, data, relFactory, removedRelProperties);
		callOnNodeDeletion(securityContext, transactionKey, data, nodeFactory, removedNodeProperties);

		// call validators
		callNodePropertyModified(securityContext, transactionKey, data, nodeFactory);
		callRelationshipPropertyModified(securityContext, transactionKey, data, relFactory);

		commitFinishes(securityContext, transactionKey);

		return transactionKey;
	}

	@Override
	public void afterCommit(TransactionData data, Long transactionKey) {

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		afterCommit(securityContext, transactionKey);
	}

	@Override
	public void afterRollback(TransactionData data, Long transactionKey) {

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		afterRollback(securityContext, transactionKey);
	}

	private void collectRemovedNodeProperties(SecurityContext securityContext, long transactionKey, TransactionData data,
					   NodeFactory nodeFactory, Map<org.neo4j.graphdb.Node, Map<String, Object>> removedNodeProperties) throws FrameworkException {

		for (PropertyEntry<org.neo4j.graphdb.Node> entry : data.removedNodeProperties()) {

			org.neo4j.graphdb.Node node                       = entry.entity();
			Map<String, Object> propertyMap = removedNodeProperties.get(node);

			if (propertyMap == null) {

				propertyMap = new LinkedHashMap<String, Object>();

				removedNodeProperties.put(node, propertyMap);

			}

			propertyMap.put(entry.key(), entry.previouslyCommitedValue());

			if (!data.isDeleted(node)) {

				AbstractNode modifiedNode = nodeFactory.instantiate(node, true, false);
				if (modifiedNode != null) {

					PropertyKey key = getPropertyKeyForDatabaseName(modifiedNode.getClass(), entry.key());
					propertyRemoved(securityContext, transactionKey, modifiedNode, key, entry.previouslyCommitedValue());
				}

			}
		}
	}

	private void collectRemovedRelationshipProperties(SecurityContext securityContext, long transactionKey, TransactionData data,
					   RelationshipFactory relFactory, Map<Relationship, Map<String, Object>> removedRelProperties) throws FrameworkException {

		for (PropertyEntry<Relationship> entry : data.removedRelationshipProperties()) {

			Relationship rel                = entry.entity();
			Map<String, Object> propertyMap = removedRelProperties.get(rel);

			if (propertyMap == null) {

				propertyMap = new LinkedHashMap<String, Object>();

				removedRelProperties.put(rel, propertyMap);

			}

			propertyMap.put(entry.key(), entry.previouslyCommitedValue());

			if (!data.isDeleted(rel)) {

				AbstractRelationship modifiedRel = relFactory.instantiate(rel);
				if (modifiedRel != null) {

					PropertyKey key = getPropertyKeyForDatabaseName(modifiedRel.getClass(), entry.key());
					propertyRemoved(securityContext, transactionKey, modifiedRel, key, entry.previouslyCommitedValue());
				}
			}

		}
	}

	private void callOnNodeCreation(SecurityContext securityContext, long transactionKey, TransactionData data, NodeFactory nodeFactory) throws FrameworkException {

		for (org.neo4j.graphdb.Node node : sortNodes(data.createdNodes())) {

			AbstractNode entity = nodeFactory.instantiate(node, true, false);
			if (entity != null) {

				graphObjectCreated(securityContext, transactionKey, entity);
			}

		}
	}

	private boolean callOnRelationshipCreation(SecurityContext securityContext, long transactionKey, TransactionData data, RelationshipFactory relFactory) throws FrameworkException {

		boolean hasError = false;

		for (Relationship rel : sortRelationships(data.createdRelationships())) {

			AbstractRelationship entity = relFactory.instantiate(rel);
			if (entity != null) {

				// notify registered listeners
				graphObjectCreated(securityContext, transactionKey, entity);
			}

		}

		return hasError;
	}

	private void callOnRelationshipDeletion(SecurityContext securityContext, long transactionKey, TransactionData data,
				RelationshipFactory relFactory, Map<Relationship, Map<String, Object>> removedRelProperties) throws FrameworkException {

		for (Relationship rel : data.deletedRelationships()) {

			AbstractRelationship entity = relFactory.instantiate(rel);
			if (entity != null) {

				// convertFromInput properties
				PropertyMap properties = PropertyMap.databaseTypeToJavaType(securityContext, entity, removedRelProperties.get(rel));
				graphObjectDeleted(securityContext, transactionKey, entity, properties);
			}
		}
	}

	private void callOnNodeDeletion(SecurityContext securityContext, long transactionKey, TransactionData data,
					   NodeFactory nodeFactory, Map<org.neo4j.graphdb.Node, Map<String, Object>> removedNodeProperties) throws FrameworkException {

		for (org.neo4j.graphdb.Node node : data.deletedNodes()) {

			String type = (String)removedNodeProperties.get(node).get(AbstractNode.type.dbName());
			AbstractNode entity = nodeFactory.instantiateDummy(type);

			if (entity != null) {

				PropertyMap properties = PropertyMap.databaseTypeToJavaType(securityContext, entity, removedNodeProperties.get(node));
				graphObjectDeleted(securityContext, transactionKey, entity, properties);
			}
		}
	}

	private void callNodePropertyModified(SecurityContext securityContext, long transactionKey, TransactionData data, NodeFactory nodeFactory) throws FrameworkException {

		for (PropertyEntry<org.neo4j.graphdb.Node> entry : data.assignedNodeProperties()) {

			AbstractNode nodeEntity = nodeFactory.instantiate(entry.entity(), true, false);
			if (nodeEntity != null) {

				PropertyKey key  = getPropertyKeyForDatabaseName(nodeEntity.getClass(), entry.key());
				Object value     = entry.value();

				propertyModified(securityContext, transactionKey, nodeEntity, key, entry.previouslyCommitedValue(), value);
			}
		}
	}

	private void callRelationshipPropertyModified(SecurityContext securityContext, long transactionKey, TransactionData data, RelationshipFactory relFactory) throws FrameworkException {

		for (PropertyEntry<Relationship> entry : data.assignedRelationshipProperties()) {

			AbstractRelationship relEntity    = relFactory.instantiate(entry.entity());
			if (relEntity != null) {

				PropertyKey key = getPropertyKeyForDatabaseName(relEntity.getClass(), entry.key());
				Object value    = entry.value();

				propertyModified(securityContext, transactionKey, relEntity, key, entry.previouslyCommitedValue(), value);
			}
		}
	}
	
	private ArrayList<org.neo4j.graphdb.Node> sortNodes(final Iterable<org.neo4j.graphdb.Node> it) {
		
		ArrayList<org.neo4j.graphdb.Node> list = new ArrayList<org.neo4j.graphdb.Node>();
		
		for (org.neo4j.graphdb.Node p : it) {
			
			list.add(p);
			
		}
		
		
		Collections.sort(list, new Comparator<org.neo4j.graphdb.Node>() {

			@Override
			public int compare(org.neo4j.graphdb.Node o1, org.neo4j.graphdb.Node o2) {
				Long id1 = o1.getId();
				Long id2 = o2.getId();
				return id1.compareTo(id2);
			}
		});
		
		return list;
		
	}
	
	private ArrayList<Relationship> sortRelationships(final Iterable<Relationship> it) {
		
		ArrayList<Relationship> list = new ArrayList<Relationship>();
		
		for (Relationship p : it) {
			
			list.add(p);
			
		}
		
		
		Collections.sort(list, new Comparator<Relationship>() {

			@Override
			public int compare(Relationship o1, Relationship o2) {
				Long id1 = o1.getId();
				Long id2 = o2.getId();
				return id1.compareTo(id2);
			}
		});
		
		return list;
		
	}
}
