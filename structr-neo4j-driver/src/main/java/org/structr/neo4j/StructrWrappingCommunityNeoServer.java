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
package org.structr.neo4j;

import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.GraphDatabaseDependencies;
import org.neo4j.kernel.logging.Logging;
import org.neo4j.kernel.monitoring.Monitors;
import org.neo4j.server.configuration.ConfigurationBuilder;
import org.neo4j.server.configuration.ConfigurationBuilder.ConfiguratorWrappingConfigurationBuilder;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;

import org.neo4j.server.CommunityNeoServer;
import static org.neo4j.server.database.WrappedDatabase.wrappedDatabase;
import org.neo4j.server.preflight.PreFlightTasks;

public class StructrWrappingCommunityNeoServer extends CommunityNeoServer {

	private final GraphDatabaseAPI db;

	public StructrWrappingCommunityNeoServer(GraphDatabaseAPI db) {
		this(db, new ServerConfigurator(db));
	}

	public StructrWrappingCommunityNeoServer(GraphDatabaseAPI db, Configurator configurator) {
		this(db, new ConfiguratorWrappingConfigurationBuilder(configurator));
	}

	public StructrWrappingCommunityNeoServer(GraphDatabaseAPI db, ConfigurationBuilder configurator) {
		super(configurator, wrappedDatabase(db), GraphDatabaseDependencies.newDependencies().logging(db.getDependencyResolver().resolveDependency(Logging.class)).monitors(db.getDependencyResolver().resolveDependency(Monitors.class)));
		this.db = db;
		this.configurator = configurator;
		init();
	}

	@Override
	protected PreFlightTasks createPreflightTasks() {
		return new PreFlightTasks(dependencies.logging());
	}
}
