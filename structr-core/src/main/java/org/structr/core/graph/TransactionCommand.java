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


import java.util.Set;
import java.util.logging.Level;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.neo4j.kernel.DeadlockDetectedException;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.RetryException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;

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
	private static final ThreadLocal<Transaction>        transactions   = new ThreadLocal<Transaction>();
	private static final MultiSemaphore                  semaphore      = new MultiSemaphore();
	
	private ModificationQueue modificationQueue = null;
	private ErrorBuffer errorBuffer             = null;
	
	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {
		
		boolean topLevel = (transactions.get() == null);
		boolean retry    = true;
		int retryCount   = 0;
		
		if (topLevel) {
			
			T result = null;
			
			while (retry && retryCount++ < 100) {

				// assume success
				retry = false;

				try {
					result = executeInternal(transaction);

				} catch (RetryException rex) {

					logger.log(Level.INFO, "Deadlock encountered, retrying transaction, count {0}", retryCount);

					retry = true;
				}
			}
			
			return result;
			 
		} else {
			
			return executeInternal(transaction);
		}
	}

	private <T> T executeInternal(StructrTransaction<T> transaction) throws FrameworkException {
		
		GraphDatabaseService graphDb    = (GraphDatabaseService) arguments.get("graphDb");
		Transaction tx                  = transactions.get();
		boolean topLevel                = (tx == null);
		boolean error                   = false;
		boolean deadlock                = false;
		Set<String> synchronizationKeys = null;
		FrameworkException exception    = null;
		T result                        = null;
		
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

				// 1. do inner callbacks (may cause transaction to fail)
				if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer)) {

					// create error
					if (transaction.doValidation) {
						throw new FrameworkException(422, errorBuffer);
					}
				}
				
				// 2. fetch all types of entities modified in this tx
				synchronizationKeys = modificationQueue.getSynchronizationKeys();
				
				// we need to protect the validation and indexing part of every transaction
				// from being entered multiple times in the presence of validators
				// 3. acquire semaphores for each modified type
				try { semaphore.acquire(synchronizationKeys); } catch (InterruptedException iex) { return null; }

				// finally, do validation under the protection of the semaphores for each type
				if (!modificationQueue.doValidation(securityContext, errorBuffer, transaction.doValidation)) {

					// create error
					throw new FrameworkException(422, errorBuffer);
				}
			}
			
		} catch (DeadlockDetectedException ddex) {
			
			tx.failure();
			
			// this block is entered when we first
			// encounter a DeadlockDetectedException
			// => pass on to parent transaction
			deadlock = true;
			error = true;
			
		} catch (RetryException rex) {
			
			tx.failure();

			// this block is entered when we catch the
			// RetryException from a nested transaction
			// => pass on to parent transaction
			deadlock = true;
			error = true;

		} catch (FrameworkException fex) {
			
			tx.failure();
			
			exception = fex;
			error = true;
			
		} catch (Throwable t) {
			
			tx.failure();

			// TODO: add debugging switch!
			t.printStackTrace();
			
			error = true;

			
		} finally {

			// finish toplevel transaction
			if (topLevel) {

				try {
					tx.success();
					tx.finish();
					
				} finally {

					// release semaphores as the transaction is now finished
					semaphore.release(synchronizationKeys);	// careful: this can be null

					// cleanup
					currentCommand.remove();
					transactions.remove();
				}

				// no error, notify entities
				if (!error) {
					modificationQueue.doOuterCallbacksAndCleanup(securityContext);
				}
			}
		}
		
		if (deadlock) {
			throw new RetryException();
		}
		
		// throw actual exception
		if (exception != null && error) {
			throw exception;
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
	
	public static void relationshipDeleted(AbstractRelationship relationship, boolean passive) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.delete(relationship, passive);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}
	
	public static boolean inTransaction() {
		return currentCommand.get() != null;
	}

	private ModificationQueue getModificationQueue() {
		return modificationQueue;
	}
}
