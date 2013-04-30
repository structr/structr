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


import java.util.logging.Level;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.neo4j.kernel.PlaceboTransaction;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.TransactionChangeSet;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
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

	private static final Logger logger                                  = Logger.getLogger(TransactionCommand.class.getName());

	private static final ThreadLocal<TransactionCommand> currentCommand = new ThreadLocal<TransactionCommand>();

	private TransactionChangeSet changeSet = null;

	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {
		
		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		FrameworkException error     = null;
		Transaction tx               = graphDb.beginTx();
		boolean topLevel             = !(tx instanceof PlaceboTransaction);	// PlaceboTransaction extends TopLevelTransaction!
		T result                     = null;
		
		if (topLevel) {
			
			this.changeSet = new TransactionChangeSet();
			currentCommand.set(this);
		}
	
		// execute structr transaction
		try {
		
			result = transaction.execute();
			
			if (topLevel && !changeSet.systemOnly()) {

				ErrorBuffer errorBuffer = new ErrorBuffer();

				callBeforeMethods(changeSet, errorBuffer);
				
				if (errorBuffer.hasError()) {
					
					// create error
					error = new FrameworkException(422, errorBuffer);
					
					// throw something to exit here
					throw new IllegalStateException();
				}
				
				callAfterMethods(changeSet);
			}
			
			
			tx.success();
			
		} catch (Throwable t) {

			// t.printStackTrace();
			
			tx.failure();
			
		} finally {
			
			tx.finish();
		}

		// finish toplevel transaction
		if (topLevel) {
				
			currentCommand.remove();
		}
		
		// throw actual error
		if (error != null) {
			throw error;
		}
		
		return result;
	}
	
	private void callBeforeMethods(TransactionChangeSet changeSet, ErrorBuffer errorBuffer) throws FrameworkException {

		IndexRelationshipCommand indexRelCommand = Services.command(securityContext, IndexRelationshipCommand.class);
		NewIndexNodeCommand indexNodeCommand     = Services.command(securityContext, NewIndexNodeCommand.class);
		PropertyMap properties               = new PropertyMap();
		
		for (AbstractNode node : changeSet.getCreatedNodes()) {
			node.beforeCreation(securityContext, errorBuffer);
			indexNodeCommand.addNode(node);
		}

		for (AbstractNode node : changeSet.getModifiedNodes()) {
			node.beforeModification(securityContext, errorBuffer);
			indexNodeCommand.updateNode(node);
		}

		for (AbstractNode node : changeSet.getDeletedNodes()) {
			node.beforeDeletion(securityContext, errorBuffer, properties);
		}
		
		for (AbstractRelationship rel : changeSet.getCreatedRelationships()) {
			rel.beforeCreation(securityContext, errorBuffer);
			indexRelCommand.execute(rel);
		}

		for (AbstractRelationship rel : changeSet.getModifiedRelationships()) {
			rel.beforeModification(securityContext, errorBuffer);
			indexRelCommand.execute(rel);
		}

		for (AbstractRelationship rel : changeSet.getDeletedRelationships()) {
			rel.beforeDeletion(securityContext, errorBuffer, properties);
		}
	}
				
	private void callAfterMethods(TransactionChangeSet changeSet) throws FrameworkException {

		for (AbstractNode node : changeSet.getCreatedNodes()) {
			node.afterCreation(securityContext);
		}

		for (AbstractNode node : changeSet.getModifiedNodes()) {
			node.afterModification(securityContext);
		}

		for (AbstractNode node : changeSet.getDeletedNodes()) {
			node.afterDeletion(securityContext);
		}

		for (AbstractRelationship rel : changeSet.getCreatedRelationships()) {
			rel.afterCreation(securityContext);
		}

		for (AbstractRelationship rel : changeSet.getModifiedRelationships()) {
			rel.afterModification(securityContext);
		}

		for (AbstractRelationship rel : changeSet.getDeletedRelationships()) {
			rel.afterDeletion(securityContext);
		}
	}
	
	public static void nonSystemProperty() {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.nonSystemProperty();
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Object modified while outside of transaction!");
		}
	}
	
	public static void nodeCreated(AbstractNode node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.create(node);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node created while outside of transaction!");
		}
	}
	
	public static void nodeModified(AbstractNode node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.modify(node);
				
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
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.delete(node);
				
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
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.create(relationship);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationships created while outside of transaction!");
		}
	}
	
	public static void relationshipModified(AbstractRelationship relationship) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.modify(relationship);
				
				AbstractNode startNode = relationship.getStartNode();
				AbstractNode endNode   = relationship.getEndNode();
				
				changeSet.modifyRelationshipEndpoint(startNode, relationship.getRelType());
				changeSet.modifyRelationshipEndpoint(endNode, relationship.getRelType());
				
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
			
			TransactionChangeSet changeSet = command.getChangeSet();
			if (changeSet != null) {
				
				changeSet.delete(relationship);
				
				AbstractNode startNode = relationship.getStartNode();
				AbstractNode endNode   = relationship.getEndNode();
				
				changeSet.modifyRelationshipEndpoint(startNode, relationship.getRelType());
				changeSet.modifyRelationshipEndpoint(endNode, relationship.getRelType());
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}

	private TransactionChangeSet getChangeSet() {
		return changeSet;
	}
}
