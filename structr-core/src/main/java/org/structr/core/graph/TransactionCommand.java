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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
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
	private static final long THRESHOLD                                 = 5000;
	
	private static final ThreadLocal<TransactionCommand> currentCommand = new ThreadLocal<TransactionCommand>();
	private static final ThreadLocal<Transaction>        transactions   = new ThreadLocal<Transaction>();
	
	private ModificationQueue modificationQueue = null;
	private ErrorBuffer errorBuffer             = null;

	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {
		
		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Transaction tx               = transactions.get();
		boolean topLevel             = (tx == null);
		FrameworkException error     = null;
		T result                     = null;
		long t0                      = System.currentTimeMillis();
		
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

				long t1 = System.currentTimeMillis();

				if (t1-t0 > THRESHOLD) {
					logger.log(Level.INFO, "Actual StructrTransaction took {0} ms", t1-t0);
				}
				
				if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer, transaction.doValidation)) {

					// create error
					throw new FrameworkException(422, errorBuffer);
				}

				long t2 = System.currentTimeMillis();
				if (t2-t1 > THRESHOLD) {
					logger.log(Level.INFO, "Inner callbacks took {0} ms", t2-t1);
				}
			}
			
		} catch (Throwable t) {

			// TODO: add debugging switch!
			// t.printStackTrace();
			
			// catch everything
			tx.failure();
			
			if (t instanceof FrameworkException) {
				error = (FrameworkException)t;
			}
		}

		// finish toplevel transaction
		if (topLevel) {
				
			long t3 = System.currentTimeMillis();
			
			tx.success();
			tx.finish();

			long t4 = System.currentTimeMillis();
			if (t4-t3 > THRESHOLD) {
				logger.log(Level.INFO, "Neo tx took {0} ms", t4-t3);
			}

			transactions.remove();
			
			// no error, notify entities
			if (error == null) {
				modificationQueue.doOuterCallbacksAndCleanup(securityContext);
			}
			
			// cleanup
			currentCommand.remove();
				
			long t5 = System.currentTimeMillis();
			if (t5-t4 > THRESHOLD) {
				logger.log(Level.INFO, "Outer callbacks took {0} ms", t5-t4);
			}

			if (t5-t0 > THRESHOLD) {
				logger.log(Level.INFO, "Transaction took {0} ms", t5-t0);
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

	private ModificationQueue getModificationQueue() {
		return modificationQueue;
	}
}
