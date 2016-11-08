/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.config.Structr;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.service.Command;
import org.structr.api.service.InitializationCallback;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;


/**
 * The graph/node service main class.
 *
 *
 *
 */
public class NodeService implements SingletonService {

	private static final Logger logger = LoggerFactory.getLogger(NodeService.class.getName());

	//~--- fields ---------------------------------------------------------

	private DatabaseService graphDb      = null;
	private Index<Node> nodeIndex        = null;
	private Index<Relationship> relIndex = null;
	private String filesPath             = null;
	private boolean isInitialized        = false;

	//~--- constant enums -------------------------------------------------

	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("graphDb",           graphDb);
			command.setArgument("nodeIndex",         nodeIndex);
			command.setArgument("relationshipIndex", relIndex);
			command.setArgument("filesPath",         filesPath);
		}
	}

	@Override
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final String databaseDriver = config.getProperty(Structr.DATABASE_DRIVER, "org.structr.bolt.BoltDatabaseService");
		graphDb = (DatabaseService)Class.forName(databaseDriver).newInstance();
		if (graphDb != null) {

			graphDb.initialize(config);

			filesPath = config.getProperty(Services.FILES_PATH);

			// check existence of files path
			File files = new File(filesPath);
			if (!files.exists()) {

				files.mkdir();
			}

			logger.info("Database ready.");

			// index creation transaction
			try ( final Transaction tx = graphDb.beginTx() ) {

				nodeIndex = graphDb.nodeIndex();
				relIndex  = graphDb.relationshipIndex();

				tx.success();

			} catch (Throwable t) {

				logger.warn("Error while initializing indexes.", t);
			}

			isInitialized = true;

			final boolean firstInitialization = graphDb.getGlobalProperties().getProperty("initialized") == null;
			final boolean isTest              = Boolean.parseBoolean(config.getProperty(Services.TESTING, "false"));

			if (graphDb.needsIndexRebuild() || (firstInitialization && !isTest)) {

				logger.info("Scheduling index rebuild to happen after startup..");

				// schedule a low-priority index rebuild (must be executed AFTER
				// SchemaService in order to include instances of dynamic types.

				services.registerInitializationCallback(new InitializationCallback() {

					@Override
					public void initializationDone() {

						// start index rebuild immediately after initialization
						Services.getInstance().command(SecurityContext.getSuperUserInstance(), BulkRebuildIndexCommand.class).execute(Collections.EMPTY_MAP);
					}

					@Override
					public int priority() {
						return 10;
					}
				});

				services.registerInitializationCallback(new InitializationCallback() {

					@Override
					public void initializationDone() {

						// initialize type labels for all nodes in the database
						Services.getInstance().command(SecurityContext.getSuperUserInstance(), BulkCreateLabelsCommand.class).execute(Collections.EMPTY_MAP);

						graphDb.getGlobalProperties().setProperty("initialized", true);
					}

					@Override
					public int priority() {
						return 20;
					}
				});
			}
		}
	}

	@Override
	public void initialized() {

		// check for empty database and seed file
		importSeedFile(StructrApp.getConfigurationValue(Services.BASE_PATH));
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			logger.info("Shutting down graph database service");
			graphDb.shutdown();

			graphDb       = null;
			isInitialized = false;

		}

	}

	@Override
	public String getName() {

		return NodeService.class.getSimpleName();

	}

	public DatabaseService getGraphDb() {
		return graphDb;
	}

	@Override
	public boolean isRunning() {

		return ((graphDb != null) && isInitialized);
	}

	@Override
	public boolean isVital() {
		return true;
	}

	public Index<Node> getNodeIndex() {
		return nodeIndex;
	}

	public Index<Relationship> getRelationshipIndex() {
		return relIndex;
	}

	private void importSeedFile(final String basePath) {

		final File seedFile = new File(StructrServices.trim(basePath) + "/" + Services.INITIAL_SEED_FILE);

		if (seedFile.exists()) {

			boolean hasApplicationNodes   = false;

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final Iterator<Node> allNodes = graphDb.getAllNodes().iterator();
				final String idName           = GraphObject.id.dbName();

				while (allNodes.hasNext()) {

					if (allNodes.next().hasProperty(idName)) {

						hasApplicationNodes = true;
						break;
					}
				}

				tx.success();

			} catch (FrameworkException fex) { }

			if (!hasApplicationNodes) {

				logger.info("Found initial seed file and no application nodes, applying initial seed..");

				try {

					SyncCommand.importFromFile(graphDb, SecurityContext.getSuperUserInstance(), seedFile.getAbsoluteFile().getAbsolutePath(), false);

				} catch (FrameworkException fex) {

					logger.warn("Unable to import initial seed file.", fex);
				}
			}
		}
	}
}
