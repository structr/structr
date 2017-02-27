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
package org.structr.neo4j;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.structr.api.DatabaseService;
import org.structr.api.service.Command;
import org.structr.api.service.RunnableService;
import org.structr.api.service.StructrServices;

/**
 *
 *
 */
public class Neo4jService implements RunnableService {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jService.class.getName());

	public static final String NEO4J_BROWSER_HOST          = "neo4j.server.host";
	public static final String NEO4J_BROWSER_PORT          = "neo4j.server.port";

	private static String host = "127.0.0.1";
	private static int port    = 7474;

	private Bootstrapper neoServerBootstrapper = null;
	private GraphDatabaseService graphDb                        = null;
	private boolean isRunning                                   = false;

	@Override
	public void startService() {

		try {
			GraphDatabaseAPI api = (GraphDatabaseAPI) graphDb;

			ServerConfigurator config = new ServerConfigurator(api);

			config.configuration()
				.addProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, host);

			config.configuration()
				.addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, port);

			logger.info("Starting Neo4j server on port {}", new Object[] { String.valueOf(port) });

			neoServerBootstrapper = new StructrWrappingNeoServerBootstrapper(api, config);
			neoServerBootstrapper.start();

		} catch (Exception e) {

			logger.error(e.getMessage());

		}

	}

	@Override
	public void stopService() {

		if (isRunning) {
			this.shutdown();
		}
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return neoServerBootstrapper != null;
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final DatabaseService databaseService = services.getDatabaseService();
		if (databaseService != null && databaseService instanceof Neo4jDatabaseService) {

			this.graphDb = ((Neo4jDatabaseService)databaseService).getGraphDb();

		} else {

			throw new IllegalStateException("Cannot start Neo4jService when DatabaseService is not Neo4jDatabaseService");
		}

		final Properties finalConfig = new Properties();

		// Default config
		finalConfig.setProperty(NEO4J_BROWSER_PORT,      "7474");

		StructrServices.mergeConfiguration(finalConfig, config);

		final String configuredPort = finalConfig.getProperty(NEO4J_BROWSER_PORT);
		host = finalConfig.getProperty(NEO4J_BROWSER_HOST);

		try {
			port = Integer.parseInt(configuredPort);

		} catch (Throwable t) {

			logger.error("Unable to parse Neo4j Browser port {}", configuredPort);

			port = -1;
		}

		if (port == -1) {
			logger.error("Unable to start Neo4j service.");
		}
	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {
		if (isRunning) {
			neoServerBootstrapper.stop();
			this.isRunning = false;
		}
	}

	@Override
	public String getName() {
		return StructrWrappingNeoServerBootstrapper.class.getSimpleName();
	}

	@Override
	public boolean isVital() {
		return false;
	}
}
