/**
 * Copyright (C) 2010-2017 Structr GmbH
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
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.Transaction;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.service.Command;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;


/**
 * The graph/node service main class.
 *
 *
 *
 */
public class NodeService implements SingletonService {

	private static final Logger logger            = LoggerFactory.getLogger(NodeService.class.getName());
	private static final String INITIAL_SEED_FILE = "seed.zip";

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
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final String databaseDriver = Settings.DatabaseDriver.getValue();
		graphDb = (DatabaseService)Class.forName(databaseDriver).newInstance();
		if (graphDb != null) {

			if (graphDb.initialize()) {

				filesPath = Settings.FilesPath.getValue();

				// check existence of files path
				File files = new File(filesPath);
				if (!files.exists()) {

					files.mkdir();
				}

				logger.info("Database driver loaded, initializing indexes..");

				// index creation transaction
				try ( final Transaction tx = graphDb.beginTx() ) {

					nodeIndex = graphDb.nodeIndex();
					relIndex  = graphDb.relationshipIndex();

					tx.success();

					// if the above operations fail, the database is probably not available
					isInitialized = true;

					logger.info("Indexes successfully initialized.");

				} catch (Throwable t) {

					logger.warn("Error while initializing indexes: {}", t.getMessage());
				}
			}
		}

		return isInitialized;
	}

	@Override
	public void initialized() {

		// check for empty database and seed file
		String basePath = Settings.getBasePath();

		if (StringUtils.isEmpty(basePath)) {
			basePath = ".";
		}

		importSeedFile(basePath);
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

		final File seedFile = new File(Settings.trim(basePath) + "/" + INITIAL_SEED_FILE);

		if (seedFile.exists()) {

			boolean hasApplicationNodes = false;

			logger.info("Checking if seed file should be imported..");

			try (final Tx tx = StructrApp.getInstance().tx()) {

				// do two very quick count queries to determine the number of Structr nodes in the database
				final int abstractNodeCount  = getCount("MATCH (n:AbstractNode) RETURN count(n) AS count", "count");
				final int nodeInterfaceCount = getCount("MATCH (n:NodeInterface) RETURN count(n) AS count", "count");

				hasApplicationNodes = abstractNodeCount == nodeInterfaceCount && abstractNodeCount > 0;

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

			logger.info("Done.");
		}
	}

	private int getCount(final String query, final String resultKey) {

		final NativeResult result = graphDb.execute(query);
		if (result.hasNext()) {

			final Map<String, Object> row = result.next();
			if (row.containsKey(resultKey)) {

				final Object value = row.get(resultKey);
				if (value != null && value instanceof Number) {

					final Number number = (Number)value;
					return number.intValue();
				}
			}
		}

		return 0;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}
}
