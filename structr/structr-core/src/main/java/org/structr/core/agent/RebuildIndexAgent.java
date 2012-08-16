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



package org.structr.core.agent;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.GraphObject;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 */
public class RebuildIndexAgent extends Agent {

	private static final Logger logger = Logger.getLogger(RebuildIndexAgent.class.getName());

	//~--- constructors ---------------------------------------------------

	public RebuildIndexAgent() {
		setName("RebuildIndexAgent");
	}

	//~--- methods --------------------------------------------------------

	@Override
	public ReturnValue processTask(Task task) throws FrameworkException {

		if (task instanceof RebuildIndexTask) {

			long t0 = System.currentTimeMillis();

			logger.log(Level.INFO, "Starting rebuilding index ...");

			long nodes = rebuildIndex();
			long t1    = System.currentTimeMillis();

			logger.log(Level.INFO, "Re-indexing nodes finished, {0} nodes processed in {1} s", new Object[] { nodes, (t1 - t0) / 1000 });

			long rels = rebuildRelationshipIndex();
			long t2   = System.currentTimeMillis();

			logger.log(Level.INFO, "Re-indexing relationships finished, {0} relationships processed in {1} s", new Object[] { rels, (t2 - t1) / 1000 });

		}

		return (ReturnValue.Success);
	}

	private long rebuildIndex() throws FrameworkException {

		// FIXME: superuser security context
		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		Command transactionCommand            = Services.command(securityContext, TransactionCommand.class);
		Command graphDbCommand                = Services.command(securityContext, GraphDatabaseCommand.class);
		final GraphDatabaseService graphDb    = (GraphDatabaseService) graphDbCommand.execute();
		Long noOfNodes                        = (Long) transactionCommand.execute(new BatchTransaction() {

			@Override
			public Object execute(Transaction tx) throws FrameworkException {

				long nodes      = 0;
				Command indexer = Services.command(securityContext, IndexNodeCommand.class);

				logger.log(Level.INFO, "Get all nodes ...");

				Result allNodes = (Result) Services.command(securityContext, GetAllNodes.class).execute();

				logger.log(Level.INFO, "... done. Start indexing {0} nodes ...", allNodes.size());

				for (GraphObject obj : allNodes.getResults()) {

					indexer.execute(obj);

					nodes++;

					if (nodes > 1000 && nodes % 1000 == 0) {

						logger.log(Level.INFO, "Indexed {0} nodes, committing results to database.", nodes);
						tx.success();
						tx.finish();

						tx = graphDb.beginTx();

						logger.log(Level.FINE, "######## committed ########", nodes);

					}

				}
                                logger.log(Level.INFO, "Finished indexing {0} nodes", nodes);
                                
				return nodes;
			}

		});

		return noOfNodes;
	}

	private long rebuildRelationshipIndex() throws FrameworkException {

		// FIXME: superuser security context
		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		Command transactionCommand            = Services.command(securityContext, TransactionCommand.class);
		Command graphDbCommand                = Services.command(securityContext, GraphDatabaseCommand.class);
		final GraphDatabaseService graphDb    = (GraphDatabaseService) graphDbCommand.execute();
		Long noOfRels                         = (Long) transactionCommand.execute(new BatchTransaction() {

			@Override
			public Object execute(Transaction tx) throws FrameworkException {

				long rels       = 0;
				Command indexer = Services.command(securityContext, IndexRelationshipCommand.class);

				logger.log(Level.INFO, "Get all relationships ...");

				List<AbstractRelationship> allRelationships = (List<AbstractRelationship>) Services.command(securityContext, GetAllRelationships.class).execute();

				logger.log(Level.INFO, "... done. Got {0} relationships.", allRelationships.size());

				for (AbstractRelationship s : allRelationships) {

					indexer.execute(s);

					rels++;

					if (rels % 100 == 0) {

						logger.log(Level.INFO, "Indexed {0} relationships, committing results to database.", rels);
						tx.success();
						tx.finish();

						tx = graphDb.beginTx();

						logger.log(Level.FINE, "######## committed ########", rels);

					}

				}

				return rels;
			}

		});

		return noOfRels;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Class getSupportedTaskType() {
		return (RebuildIndexTask.class);
	}
}
