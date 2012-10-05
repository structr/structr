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



package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * This command takes a property set as parameter.
 *
 * Sets the properties found in the property set on all nodes matching the type.
 * If no type property is found, set the properties on all nodes.
 *
 * @author Axel Morgner
 */
public class BulkSetNodePropertiesCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(BulkSetNodePropertiesCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		final GraphDatabaseService graphDb     = (GraphDatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);
		final Command searchNode               = Services.command(superUserContext, SearchNodeCommand.class);

		if (!((parameters != null) && (parameters.length == 1) && (parameters[0] instanceof Map) && !((Map) parameters[0]).isEmpty())) {

			throw new IllegalArgumentException("This command requires one argument of type Map. Map must not be empty.");
		}

		final Map<String, Object> properties = (Map<String, Object>) parameters[0];

		if (graphDb != null) {

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			transactionCommand.execute(new BatchTransaction() {

				@Override
				public Object execute(Transaction tx) throws FrameworkException {

					long n        = 0L;
					Result result = null;

					if (properties.containsKey(AbstractNode.Key.type.name())) {

						List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

						attrs.add(Search.andExactType((String) properties.get(AbstractNode.Key.type.name())));

						result = (Result) searchNode.execute(null, false, false, attrs);

						properties.remove(AbstractNode.Key.type.name());

					} else {

						result = (Result) nodeFactory.createAllNodes(GlobalGraphOperations.at(graphDb).getAllNodes());
					}

					for (GraphObject obj : result.getResults()) {

						AbstractNode node = (AbstractNode) obj;

						// Treat only "our" nodes
						if (node.getStringProperty(AbstractNode.Key.uuid) != null) {

							for (Entry entry : properties.entrySet()) {

								String key = (String) entry.getKey();
								Object val = entry.getValue();

								node.unlockReadOnlyPropertiesOnce();
								node.setProperty(key, val);

							}

							if (n > 1000 && n % 1000 == 0) {

								logger.log(Level.INFO, "Set properties on {0} nodes, committing results to database.", n);
								tx.success();
								tx.finish();

								tx = graphDb.beginTx();

								logger.log(Level.FINE, "######## committed ########", n);

							}

							n++;

						}

					}

					logger.log(Level.INFO, "Finished setting properties on {0} nodes", n);

					return null;

				}

			});

		}

		return null;

	}

}
