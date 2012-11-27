/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;


import java.util.LinkedHashSet;
import java.util.Queue;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;
import java.util.logging.Level;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import javax.sound.midi.SysexMessage;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.TransactionChangeSet;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class TransactionCommand extends NodeServiceCommand {

	private static final Logger logger                 = Logger.getLogger(TransactionCommand.class.getName());
	private static final String debugProperty          = System.getProperty("DEBUG_TRANSACTIONS", "false");
	private static final AtomicLong transactionCounter = new AtomicLong(0);
	private static final int MAX_DEPTH                 = 16;
	private static final boolean debug                 = "true".equals(debugProperty);

	private static final ThreadLocal<Transaction> transactions = new ThreadLocal<Transaction>();
	private static final ThreadLocal<Long> transactionKeys     = new ThreadLocal<Long>();
	private static final ThreadLocal<Long> depths              = new ThreadLocal<Long>();
	
	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Long transactionKey          = transactionKeys.get();
		Transaction tx               = transactions.get();
		Throwable exception          = null;
		T ret                        = null;
		
		if (tx == null || transactionKey == null) {
			
			tx = graphDb.beginTx();
			transactionKey = nextLong();
			
			transactionKeys.set(transactionKey);
			transactions.set(tx);
			
			EntityContext.setSecurityContext(securityContext);
			EntityContext.setTransactionKey(transactionKey);

			try {

				ret = transaction.execute();

				tx.success();

			} catch (Throwable t) {

				if (debug) {
					t.printStackTrace();
				}

				tx.failure();

				// store exception for later use
				exception = t;

			} finally {

				transactions.remove();
				transactionKeys.remove();

				synchronized(TransactionCommand.class) {

					try {

						tx.finish();


					} catch (Throwable t) {

						if (debug) {
							t.printStackTrace();
						}

						// transaction failed, look for "real" cause..
						exception = EntityContext.getFrameworkException(transactionKey);
					}
				}
			}

			if(exception != null) {

				if (debug) {
					exception.printStackTrace();
				}

				
				if (exception instanceof FrameworkException) {
					
					logger.log(Level.WARNING, ((FrameworkException) exception).toString());
					
					EntityContext.clearTransactionData(transactionKey);
					throw (FrameworkException)exception;
				}
			}

			final TransactionChangeSet changeSet = EntityContext.getTransactionChangeSet(transactionKey);
			Long depthValue = depths.get();
			long depth      = 0;
			
			if (depthValue != null) {
				depth = depthValue.longValue();
			}

			if (debug) {
				
				indent();
				System.out.println("BEFORE CHANGESET: " + transactionKey);
			}
			
			if (changeSet != null) {
			
				if (debug) {
				
					indent();
					System.out.println("BEFORE DEPTH: " + depth);
				}

				if (depth < MAX_DEPTH && !changeSet.systemOnly()) {
					
					if (debug) {

						indent();
						System.out.println("AFTER DEPTH: " + depth);
					}
					
					depths.set(depth + 1);

					try {
						
						Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

							@Override
							public Object execute() throws FrameworkException {

								notifyChangeSet(changeSet);

								return null;
							}

						});

					} catch(Throwable t) {
						
						if (debug) {
							t.printStackTrace();
						}
						
						if (t instanceof FrameworkException) {

							logger.log(Level.WARNING, ((FrameworkException) t).toString());

							EntityContext.clearTransactionData(transactionKey);
							throw (FrameworkException)t;
						}
						
					}

					depths.set(depth);
					
				} else {
					
					if (depth == MAX_DEPTH) {
						logger.log(Level.SEVERE, "Maximum depth of nested modifications reached! You probably forgot to mark a property as a system property.");
					}
					
					if (debug) {

						indent();
						System.out.println("CHANGESET STATS: " + changeSet.toString());
					}
				}
			}

			EntityContext.clearTransactionData(transactionKey);
			
			return ret;
			
		} else {

			if (debug) {
				
				indent();
				System.out.println("EXISTING TRANSACTION: " + transactionKey);
			}
			
			return transaction.execute();
		}
	}

	public <T> T execute(BatchTransaction<T> transaction) throws FrameworkException {
		throw new IllegalStateException("Batch transactions are not supported any more.");
	}
	
	private void notifyChangeSet(TransactionChangeSet changeSet) {

		// determine propagation set
		final Queue<AbstractNode> propagationQueue = changeSet.getPropagationQueue();
		final Set<AbstractNode> propagationSet     = new LinkedHashSet<AbstractNode>();

		// add initial set of modified nodes; this line makes sure that the
		// modified nodes themselves are notified of a propagated change
		// as well.
		propagationSet.addAll(propagationQueue);

		if (!propagationQueue.isEmpty()) {

			do {

				final AbstractNode node = propagationQueue.poll();
				if (!propagationSet.contains(node)) {

					propagationSet.addAll(node.getNodesForModificationPropagation());
				}

			} while(!propagationQueue.isEmpty());
		}

		propagateModification(securityContext, propagationSet);

		afterOwnerModification(securityContext, changeSet.getOwnerModifiedNodes());
		afterSecurityModification(securityContext, changeSet.getSecurityModifiedNodes());
		afterLocationModification(securityContext, changeSet.getLocationModifiedNodes());

		afterDeletion(securityContext, changeSet.getDeletedNodes());
		afterDeletion(securityContext, changeSet.getDeletedRelationships());

		afterModification(securityContext, changeSet.getModifiedNodes());
		afterModification(securityContext, changeSet.getModifiedRelationships());

		// after transaction callbacks
		afterCreation(securityContext, changeSet.getCreatedNodes());
		afterCreation(securityContext, changeSet.getCreatedRelationships());
		
		changeSet.clear();
	}
	
	private void afterCreation(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.afterCreation(securityContext);
			}
		}
		
	}

	private void afterModification(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.afterModification(securityContext);
			}
		}
	}

	private void afterDeletion(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.afterDeletion(securityContext);
			}
		}
	}

	private void afterOwnerModification(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.ownerModified(securityContext);
			}
		}
	}

	private void afterSecurityModification(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.securityModified(securityContext);
			}
		}
	}

	private void afterLocationModification(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.locationModified(securityContext);
			}
		}
	}

	private void propagateModification(SecurityContext securityContext, Set<? extends GraphObject> data) {
		
		if(data != null && !data.isEmpty()) {
			
			for(GraphObject obj : data) {
				obj.propagatedModification(securityContext);
			}
		}
	}

	private long nextLong() {
		return transactionCounter.incrementAndGet();
	}
	
	private void indent() {
		Long depthValue = depths.get();
		long depth      = 0;

		if (depthValue != null) {
			depth = depthValue.longValue();
		}
		
		for (int i=0; i<depth+1; i++) {
			System.out.print("        ");
		}
	}
}
