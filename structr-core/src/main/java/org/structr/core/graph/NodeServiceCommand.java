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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Predicate;
import org.structr.core.Services;

/**
 * Abstract base class for all graph service commands.
 *
 * @author Christian Morgner
 */
public abstract class NodeServiceCommand extends Command {
	
	private static final Logger logger = Logger.getLogger(NodeServiceCommand.class.getName());
	
	@Override
	public Class getServiceClass()	{
		return(NodeService.class);
	}
	
	/**
	 * Executes the given operation on all nodes in the given list.
	 * 
	 * @param <T>
	 * @param securityContext
	 * @param nodes the nodes to operate on
	 * @param operation the operation to execute
	 * @return the number of nodes processed
	 * @throws FrameworkException 
	 */
	public static <T extends GraphObject> long bulkGraphOperation(final SecurityContext securityContext, final Iterable<T> nodes, final long commitCount, String description, final BulkGraphOperation<T> operation) throws FrameworkException {
		return bulkGraphOperation(securityContext, nodes, commitCount, description, operation, true);
	}
	/**
	 * Executes the given operation on all nodes in the given list.
	 * 
	 * @param <T>
	 * @param securityContext
	 * @param nodes the nodes to operate on
	 * @param operation the operation to execute
	 * @return the number of nodes processed
	 * @throws FrameworkException 
	 */
	public static <T extends GraphObject> long bulkGraphOperation(final SecurityContext securityContext, final Iterable<T> nodes, final long commitCount, String description, final BulkGraphOperation<T> operation, boolean validation) throws FrameworkException {

		final Iterator<T> iterator = nodes.iterator();
		long objectCount           = 0L;
		
		while (iterator.hasNext()) {

			try {

				objectCount += Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Integer>(validation) {

					@Override
					public Integer execute() throws FrameworkException {

						int count = 0;

						while (iterator.hasNext()) {

							T node = iterator.next();

							try {

								operation.handleGraphObject(securityContext, node);

							} catch (Throwable t) {

								operation.handleThrowable(securityContext, t, node);
							}

							// commit transaction after commitCount
							if (++count >= commitCount) {
								break;
							}
						}

						return count;
					}
				});

			} catch (Throwable t) {
				
				// bulk transaction failed, what to do?
				operation.handleTransactionFailure(securityContext, t);
			}
			
			if (description != null) {
				logger.log(Level.INFO, "{0}: {1} objects processed", new Object[] { description, objectCount } );
			}
		}
		
		return objectCount;
	}
	
	/**
	 * Executes the given transaction until the stop condition evaluates to
	 * <b>true</b>.
	 * 
	 * @param <T>
	 * @param securityContext
	 * @param commitCount the number of executions after which the transaction is committed
	 * @param transaction the operation to execute
	 * @return the number of nodes processed
	 * @throws FrameworkException 
	 */
	public static void bulkTransaction(final SecurityContext securityContext, final long commitCount, final StructrTransaction transaction, final Predicate<Long> stopCondition) throws FrameworkException {

		final AtomicLong objectCount = new AtomicLong(0L);
		
		while (!stopCondition.evaluate(securityContext, objectCount.get())) {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					long loopCount = 0;
					
					while (loopCount++ < commitCount && !stopCondition.evaluate(securityContext, objectCount.get())) {
						
						transaction.execute();
						objectCount.incrementAndGet();
					}

					return null;
				}
			});
		}
	}
}
