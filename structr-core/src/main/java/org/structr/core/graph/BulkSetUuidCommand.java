/*
 *  Copyright (C) 2012 Axel Morgner
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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Sets a new UUID on each graph object of the given type. For nodes, set type,
 * for relationships relType.
 *
 * @author Axel Morgner
 */
public class BulkSetUuidCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetUuidCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		final String entityType                = (String) attributes.get("type");
		final String relType                   = (String) attributes.get("relType");
		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final RelationshipFactory relFactory   = new RelationshipFactory(superUserContext);
		long nodeCount                         = 0L;
		long relCount                          = 0L;

		if (entityType != null) {

			final Class type = EntityContext.getEntityClassForRawType(entityType);

			if (type != null) {

				// final Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));
				final Result<AbstractNode> result = nodeFactory.createAllNodes(GlobalGraphOperations.at(graphDb).getAllNodes());
				final List<AbstractNode> nodes    = result.getResults();

				logger.log(Level.INFO, "Start setting UUID on all nodes of type {1}", new Object[] { AbstractNode.uuid, type.getSimpleName() });

				final Iterator<AbstractNode> nodeIterator = nodes.iterator();

				while (nodeIterator.hasNext()) {

					nodeCount += Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Integer>() {

						@Override
						public Integer execute() throws FrameworkException {

							int count = 0;

							while (nodeIterator.hasNext()) {

								AbstractNode abstractNode = nodeIterator.next();

								if (!abstractNode.getClass().equals(type)) {

									continue;
								}

								try {

									abstractNode.setProperty(AbstractNode.uuid, UUID.randomUUID().toString().replaceAll("[\\-]+", ""));

								} catch (Throwable t) {

									logger.log(Level.WARNING, "Unable to set UUID on {0}: {1}", new Object[] { type.getSimpleName(), t.getMessage() });

								}

								// restart transaction after 1000 iterations
								if (++count == 1000) {

									break;
								}

							}

							return count;

						}

					});

					logger.log(Level.INFO, "Set UUID on {0} nodes ...", nodeCount);

				}

				logger.log(Level.INFO, "Done");

				return;
			}

		} else if (relType != null) {

			// final Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(true, false, Search.andExactType(type.getSimpleName()));
			final List<AbstractRelationship> rels = relFactory.instantiateRelationships(superUserContext, GlobalGraphOperations.at(graphDb).getAllRelationships());

			logger.log(Level.INFO, "Start setting UUID on all rels of type {1}", new Object[] { AbstractRelationship.uuid, relType });

			final Iterator<AbstractRelationship> nodeIterator = rels.iterator();

			while (nodeIterator.hasNext()) {

				relCount += Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Integer>() {

					@Override
					public Integer execute() throws FrameworkException {

						int count = 0;

						while (nodeIterator.hasNext()) {

							AbstractRelationship abstractRel = nodeIterator.next();

							if (!abstractRel.getType().equals(relType)) {

								continue;
							}

							try {

								abstractRel.setProperty(AbstractRelationship.uuid, UUID.randomUUID().toString().replaceAll("[\\-]+", ""));

							} catch (Throwable t) {

								logger.log(Level.WARNING, "Unable to set UUID on {0}: {1}", new Object[] { relType, t.getMessage() });

							}

							// restart transaction after 1000 iterations
							if (++count == 1000) {

								break;
							}

						}

						return count;

					}

				});

				logger.log(Level.INFO, "Set UUID on {0} rels ...", relCount);

			}

			logger.log(Level.INFO, "Done");

			return;
		}

		logger.log(Level.INFO, "Unable to determine entity type to set UUID.");

	}

}
