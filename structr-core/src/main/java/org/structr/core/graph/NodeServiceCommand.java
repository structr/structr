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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
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
	 * 
	 * @param <T>
	 * @param securityContext
	 * @param nodes
	 * @param operation
	 * @return
	 * @throws FrameworkException 
	 */
	public static <T extends GraphObject> long bulkGraphOperation(final SecurityContext securityContext, final List<T> nodes, final long commitCount, String description, final BulkGraphOperation<T> operation) throws FrameworkException {

		final Iterator<T> iterator = nodes.iterator();
		long objectCount           = 0L;
		
		while (iterator.hasNext()) {

			try {

				objectCount += Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Integer>() {

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
			
			logger.log(Level.INFO, "{0}: {1} objects processed", new Object[] { description, objectCount } );
		}
		
		return objectCount;
	}
}
