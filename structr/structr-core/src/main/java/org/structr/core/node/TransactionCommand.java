/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.node;


import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.kernel.DeadlockDetectedException;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class TransactionCommand extends NodeServiceCommand {

	private static final AtomicLong transactionCounter = new AtomicLong(0);
	private static final Logger logger                 = Logger.getLogger(TransactionCommand.class.getName());
	private static final boolean threadDebugging       = false;

	private static final Map<Thread, Integer> indents = Collections.synchronizedMap(new WeakHashMap<Thread, Integer>());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		FrameworkException exception = null;
		Object ret                   = null;

		if ((parameters.length > 0) && (parameters[0] instanceof StructrTransaction)) {

			StructrTransaction transaction = (StructrTransaction) parameters[0];
			if (graphDb != null) {

				if(threadDebugging) {
					synchronized(TransactionCommand.class) {

						int indent = getIndent();

						System.out.print("################################# ");
						System.out.print(StringUtils.leftPad(new Long(Thread.currentThread().getId()).toString(), 5));
						System.out.print(" ");
						for(int i=0; i<indent; i++) System.out.print("  ");
						System.out.println("Starting  transaction " + transaction.toString() + " at " + System.currentTimeMillis());

						incIndent();
					}
				}

				Transaction tx = graphDb.beginTx();

				try {

					ret = transaction.execute();

					tx.success();
					logger.log(Level.FINEST, "Transaction successfull");

				} catch (FrameworkException frameworkException) {

					tx.failure();
					logger.log(Level.WARNING, "Transaction failure", frameworkException);

					// store exception for later use
					exception = frameworkException;

				} catch(DeadlockDetectedException ddex) {
					
					tx.failure();

					logger.log(Level.SEVERE, "Neo4j detected a deadlock, enable thread debugging here and try to modify entity/relationship creation order!", ddex.getMessage());

					/*
					 * Maybe the transaction can be restarted here
					 */

				} finally {

					long transactionKey = nextLong();
					EntityContext.setSecurityContext(securityContext);
					EntityContext.setTransactionKey(transactionKey);

					try {
						tx.finish();
					} catch (Throwable t) {

						// transaction failed, look for "real" cause..
						exception = EntityContext.getFrameworkException(transactionKey);
					}
					EntityContext.removeTransactionKey();
					EntityContext.removeSecurityContext();
				}

				if(threadDebugging) {
					synchronized(TransactionCommand.class) {

						decIndent();

						int indent = getIndent();

						System.out.print("################################# ");
						System.out.print(StringUtils.leftPad(new Long(Thread.currentThread().getId()).toString(), 5));
						System.out.print(" ");
						for(int i=0; i<indent; i++) System.out.print("  ");
						System.out.println("Finishing transaction " + transaction.toString() + " at " + System.currentTimeMillis());
					}
				}
			}

		} else if ((parameters.length > 0) && (parameters[0] instanceof BatchTransaction)) {

			BatchTransaction transaction = (BatchTransaction) parameters[0];
			Transaction tx               = graphDb.beginTx();

			try {

				ret = transaction.execute(tx);

				tx.success();
				logger.log(Level.FINEST, "Transaction successfull");

			} catch (FrameworkException frameworkException) {

				tx.failure();

				logger.log(Level.WARNING, "Transaction failure", frameworkException);

				exception = frameworkException;

			} finally {

				long transactionKey = nextLong();
				EntityContext.setTransactionKey(transactionKey);

				try {
					tx.finish();
				} catch (Throwable t) {

					// transaction failed, look for "real" cause..
					exception = EntityContext.getFrameworkException(transactionKey);
					EntityContext.removeTransactionKey();
				}
			}

		}

		if(exception != null) {
			throw exception;
		}

		return ret;
	}

	private long nextLong() {
		return transactionCounter.incrementAndGet();
	}

	private synchronized void incIndent() {

		Integer val = indents.get(Thread.currentThread());
		if(val == null) {
			val = new Integer(0);
		}

		indents.put(Thread.currentThread(), new Integer(val.intValue() + 1));
	}

	private synchronized void decIndent() {

		Integer val = indents.get(Thread.currentThread());
		if(val == null) {
			val = new Integer(0);
		}

		indents.put(Thread.currentThread(), new Integer(val.intValue() - 1));

	}

	private synchronized int getIndent() {

		Integer val = indents.get(Thread.currentThread());
		if(val == null) {
			return 0;
		}

		return val.intValue();
	}
}
