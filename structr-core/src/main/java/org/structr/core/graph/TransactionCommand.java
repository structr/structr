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


package org.structr.core.graph;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import static org.structr.common.RelType.IS_AT;
import static org.structr.common.RelType.OWNS;
import static org.structr.common.RelType.SECURITY;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------

/**
 * Graph service command for database operations that need to be wrapped in
 * a transaction. All operations that modify the database need to be executed
 * in a transaction, which can be achieved using the following code:
 * 
 * <pre>
 * Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
 * 
 *	public Object execute() throws FrameworkException {
 *		// do stuff here
 *	}
 * });
 * </pre>
 * 
 * @author Christian Morgner
 */
public class TransactionCommand extends NodeServiceCommand {

	private static final Logger logger                                            = Logger.getLogger(TransactionCommand.class.getName());
	
	private static final ThreadLocal<TransactionCommand> currentCommand           = new ThreadLocal<TransactionCommand>();
	private static final ThreadLocal<Transaction>        transactions             = new ThreadLocal<Transaction>();
	
	private IndexRelationshipCommand indexRelationshipCommand = null;
	private NewIndexNodeCommand indexNodeCommand              = null;
	private ModificationQueue modificationQueue               = null;
	private ErrorBuffer errorBuffer                           = null;

	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {
		
		indexRelationshipCommand     = Services.command(SecurityContext.getSuperUserInstance(), IndexRelationshipCommand.class);
		indexNodeCommand             = Services.command(SecurityContext.getSuperUserInstance(), NewIndexNodeCommand.class);;
		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Transaction tx               = transactions.get();
		boolean topLevel             = (tx == null);
		FrameworkException error     = null;
		T result                     = null;
		
		if (topLevel) {
		
			// start new transaction
			this.modificationQueue = new ModificationQueue();
			this.errorBuffer       = new ErrorBuffer();
			tx                     = graphDb.beginTx();
			
			transactions.set(tx);
			currentCommand.set(this);
		}
	
		// execute structr transaction
		try {
		
			result = transaction.execute();
			
			if (topLevel) {

				if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer)) {
					
					// create error
					throw new FrameworkException(422, errorBuffer);
				}
			}
			
		} catch (Throwable t) {

			t.printStackTrace();
			
			// catch everything
			tx.failure();
			
			if (t instanceof FrameworkException) {
				error = (FrameworkException)t;
			}
		}

		// finish toplevel transaction
		if (topLevel) {
				
			tx.success();
			tx.finish();
			
			// cleanup
			currentCommand.remove();
			transactions.remove();
			
			// no error, notify entities
			if (error == null) {
				modificationQueue.doOuterCallbacks(securityContext);
			}
		}
		
		// throw actual error
		if (error != null) {
			throw error;
		}
		
		return result;
	}
	
	public static void nodeCreated(AbstractNode node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.create(node);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node created while outside of transaction!");
		}
	}
	
	public static void nodeModified(AbstractNode node, PropertyKey key, Object previousValue) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.modify(node, key, previousValue);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node deleted while outside of transaction!");
		}
	}
	
	public static void nodeDeleted(AbstractNode node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.delete(node);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node deleted while outside of transaction!");
		}
	}
	
	public static void relationshipCreated(AbstractRelationship relationship) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.create(relationship);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationships created while outside of transaction!");
		}
	}
	
	public static void relationshipModified(AbstractRelationship relationship, PropertyKey key, Object value) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.modify(relationship, null, null);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}
	
	public static void relationshipDeleted(AbstractRelationship relationship) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.delete(relationship);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}

	private ModificationQueue getModificationQueue() {
		return modificationQueue;
	}
	
	private class ModificationQueue {
		
		ConcurrentSkipListMap<String, State> modifications = new ConcurrentSkipListMap<String, State>();
		Map<String, State> immutableState                  = new LinkedHashMap<String, State>();
		
		public boolean doInnerCallbacks(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
			
			boolean valid = true;
			
			while (!modifications.isEmpty()) {
				
				Entry<String, State> entry = modifications.pollFirstEntry();
				if (entry != null) {
					
					// do callback according to entry state
					valid &= entry.getValue().doInnerCallback(securityContext, errorBuffer);
					
					// store entries for later notification
					if (!immutableState.containsKey(entry.getKey())) {
						immutableState.put(entry.getKey(), entry.getValue());
					}
				}
			}
			
			return valid;
		}
		
		public void doOuterCallbacks(SecurityContext securityContext) {
			
			// copy modifications, do after transaction callbacks
			for (State state : immutableState.values()) {
				
				if (!state.isDeleted()) {
					state.doOuterCallback(securityContext);
				}
			}
			
			// clear map afterwards
			immutableState.clear();
		}
		
		public void create(AbstractNode node) {
			getState(node).create();
		}
		
		public void create(AbstractRelationship relationship) {
			
			getState(relationship).create();
			
			modifyEndNodes(relationship.getStartNode(), relationship.getEndNode(), relationship.getRelType());
		}
		
		public void modify(AbstractNode node, PropertyKey key, Object previousValue) {
			getState(node).modify(key, previousValue);
		}
		
		public void modify(AbstractRelationship relationship, PropertyKey key, Object previousValue) {
			getState(relationship).modify(key, previousValue);
		}
		
		public void delete(AbstractNode node) {
			getState(node).delete();
		}
		
		public void delete(AbstractRelationship relationship) {

			getState(relationship).delete();
			
			modifyEndNodes(relationship.getStartNode(), relationship.getEndNode(), relationship.getRelType());
		}
		
		private void modifyEndNodes(AbstractNode startNode, AbstractNode endNode, RelationshipType relType) {
		
			if (OWNS.equals(relType)) {
				
				// modifyOwner()
				return;
			}
			
			if (SECURITY.equals(relType)) {
				
				// modifSecurity()
				return;
			}
			
			if (IS_AT.equals(relType)) {
				
				// modifyLocation()
				return;
			}

			modify(startNode, null, null);
			modify(endNode, null, null);
		}
		
		private State getState(AbstractNode node) {
			
			String hash = hash(node);
			State state = modifications.get(hash);
			
			if (state == null) {
				
				state = new State(node);
				modifications.put(hash, state);
			}
			
			return state;
		}
		
		private State getState(AbstractRelationship rel) {
			
			String hash = hash(rel);
			State state = modifications.get(hash);
			
			if (state == null) {
				
				state = new State(rel);
				modifications.put(hash, state);
			}
			
			return state;
		}
		
		private String hash(AbstractNode node) {
			return "N" + node.getId();
		}
		
		private String hash(AbstractRelationship rel) {
			return "R" + rel.getId();
		}
	}
	
	private class State {
		
		private PropertyMap removedProperties = new PropertyMap();
		private GraphObject object            = null;
		private int status                    = 0;
		
		public State(GraphObject object) {
			this.object = object;
		}
		
		@Override
		public String toString() {
			return object.getClass().getSimpleName() + "(" + object + "); " + status;
		}
		
		public void create() {
			status |= 4;
		}
		
		public void modify(PropertyKey key, Object previousValue) {
			
			status |= 2;
			
			// store previous value
			if (key != null && previousValue != null) {
				removedProperties.put(key, previousValue);
			}
		}
		
		public void delete() {
			status |= 1;
		}
		
		public boolean isCreated() {
			return (status & 4) == 4;
		}
		
		public boolean isModified() {
			return (status & 2) == 2;
		}
		
		public boolean isDeleted() {
			return (status & 1) == 1;
		}
		
		public boolean doInnerCallback(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
			
			boolean valid = true;
			
			switch (status) {
				
				case 7:	// created, modified, deleted, poor guy => no callback
					break;
					
				case 6: // created, modified => only creation callback will be called
					valid &= object.beforeCreation(securityContext, errorBuffer);
					addToIndex();
					break;
					
				case 5: // created, deleted => no callback
					break;
					
				case 4: // created => creation callback
					valid &= object.beforeCreation(securityContext, errorBuffer);
					addToIndex();
					break;
					
				case 3: // modified, deleted => deletion callback
					valid &= object.beforeDeletion(securityContext, errorBuffer, removedProperties);
					break;
					
				case 2: // modified => modification callback
					valid &= object.beforeModification(securityContext, errorBuffer);
					updateInIndex();
					break;
					
				case 1: // deleted => deletion callback
					valid &= object.beforeDeletion(securityContext, errorBuffer, removedProperties);
					break;
					
				case 0:	// no action, no callback
					break;
					
				default:
					break;
			}
			
			return valid;
		}
		
		public void doOuterCallback(SecurityContext securityContext) {
			
			switch (status) {
				
				case 7:	// created, modified, deleted, poor guy => no callback
					break;
					
				case 6: // created, modified => only creation callback will be called
					object.afterCreation(securityContext);
					break;
					
				case 5: // created, deleted => no callback
					break;
					
				case 4: // created => creation callback
					object.afterCreation(securityContext);
					break;
					
				case 3: // modified, deleted => deletion callback
					object.afterDeletion(securityContext);
					break;
					
				case 2: // modified => modification callback
					object.afterModification(securityContext);
					break;
					
				case 1: // deleted => deletion callback
					object.afterDeletion(securityContext);
					break;
					
				case 0:	// no action, no callback
					break;
					
				default:
					break;
			}
		}
		
		public GraphObject getObject() {
			return object;
		}

		private void addToIndex() throws FrameworkException {
			
			if (object instanceof AbstractNode) {
				
				indexNodeCommand.addNode((AbstractNode)object);
				
			} else if (object instanceof AbstractRelationship) {
				
				indexRelationshipCommand.execute((AbstractRelationship)object);
			}
		}

		private void updateInIndex() throws FrameworkException {
			
			if (object instanceof AbstractNode) {
				
				indexNodeCommand.updateNode((AbstractNode)object);
				
			} else if (object instanceof AbstractRelationship) {
				
				indexRelationshipCommand.execute((AbstractRelationship)object);
			}
		}
	}
}
