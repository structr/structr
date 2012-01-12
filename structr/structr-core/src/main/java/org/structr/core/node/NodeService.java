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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.SingletonService;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class NodeService implements SingletonService {

	private static final Logger logger = Logger.getLogger(NodeService.class.getName());

	//~--- fields ---------------------------------------------------------

	private Index<Node> fulltextIndex      = null;
	private GraphDatabaseService graphDb   = null;
	private Index<Node> keywordIndex       = null;
	private StructrNodeFactory nodeFactory = null;
	private Index<Node> userIndex          = null;
	private Index<Node> uuidIndex          = null;

	/** Dependent services */
	private Set<RunnableService> registeredServices = new HashSet<RunnableService>();

	//~--- constant enums -------------------------------------------------

	public static enum NodeIndex { uuid, user, keyword, fulltext; }

	//~--- methods --------------------------------------------------------

	// <editor-fold defaultstate="collapsed" desc="interface SingletonService">
	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("graphDb", graphDb);
			command.setArgument(NodeIndex.uuid.name(), uuidIndex);
			command.setArgument(NodeIndex.fulltext.name(), fulltextIndex);
			command.setArgument(NodeIndex.user.name(), userIndex);
			command.setArgument(NodeIndex.keyword.name(), keywordIndex);
			command.setArgument("nodeFactory", nodeFactory);
			command.setArgument("filesPath", Services.getFilesPath());
			command.setArgument("indices", NodeIndex.values());

		}
	}

	@Override
	public void initialize(Map<String, Object> context) {

//              String dbPath = (String) context.get(Services.DATABASE_PATH);
		String dbPath = Services.getDatabasePath();

		try {

			logger.log(Level.INFO, "Initializing database ({0}) ...", dbPath);

			Map<String, String> configuration = null;

			try {

				configuration = EmbeddedGraphDatabase.loadConfigurations(dbPath + "/neo4j.conf");
				graphDb       = new EmbeddedGraphDatabase(dbPath, configuration);

			} catch (Throwable t) {

				logger.log(Level.INFO, "Database config {0}/neo4j.conf not found", dbPath);

				graphDb = new EmbeddedGraphDatabase(dbPath);
			}

			if(graphDb != null) {
				graphDb.registerTransactionEventHandler(EntityContext.getTransactionEventHandler());
			}

			String filesPath = Services.getFilesPath();

			// check existence of files path
			File files = new File(filesPath);

			if (!files.exists()) {

				files.mkdir();

			}

			logger.log(Level.INFO, "Database ready.");
			logger.log(Level.FINE, "Initializing UUID index...");

			uuidIndex = graphDb.index().forNodes("uuidAllNodes", LuceneIndexImplementation.EXACT_CONFIG);

			logger.log(Level.FINE, "UUID index ready.");
			logger.log(Level.FINE, "Initializing user index...");

			userIndex = graphDb.index().forNodes("nameEmailAllUsers", LuceneIndexImplementation.EXACT_CONFIG);

			logger.log(Level.FINE, "UUID index ready.");
			logger.log(Level.FINE, "Initializing fulltext index...");

			fulltextIndex = graphDb.index().forNodes("fulltextAllNodes", LuceneIndexImplementation.FULLTEXT_CONFIG);

			logger.log(Level.FINE, "Fulltext index ready.");
			logger.log(Level.FINE, "Initializing keyword index...");

			keywordIndex = graphDb.index().forNodes("keywordAllNodes", LuceneIndexImplementation.EXACT_CONFIG);

			logger.log(Level.FINE, "Keyword index ready.");
			logger.log(Level.FINE, "Initializing node factory...");

			nodeFactory = new StructrNodeFactory();

			logger.log(Level.FINE, "Node factory ready.");

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Database could not be initialized. {0}", e.getMessage());
			e.printStackTrace(System.out);
		}
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			for (RunnableService s : registeredServices) {

				s.stopService();

			}

			// Wait for all registered services to end
			waitFor(registeredServices.isEmpty());
			graphDb.shutdown();

			graphDb = null;

		}
	}

	public void registerService(final RunnableService service) {
		registeredServices.add(service);
	}

	public void unregisterService(final RunnableService service) {
		registeredServices.remove(service);
	}

	private void waitFor(final boolean condition) {

		while (!condition) {

			try {
				Thread.sleep(10);
			} catch (Throwable t) {}

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getName() {
		return NodeService.class.getSimpleName();
	}

	// </editor-fold>

	@Override
	public boolean isRunning() {
		return (graphDb != null);
	}
}
