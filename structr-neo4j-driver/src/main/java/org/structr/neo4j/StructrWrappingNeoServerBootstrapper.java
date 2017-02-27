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

import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.NeoServer;
import org.neo4j.server.configuration.ConfigurationBuilder;
import org.neo4j.server.configuration.ConfigurationBuilder.ConfiguratorWrappingConfigurationBuilder;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import scala.util.logging.ConsoleLogger;

public class StructrWrappingNeoServerBootstrapper extends Bootstrapper {

	private final GraphDatabaseAPI db;
	private final ConfigurationBuilder configurator;

	public StructrWrappingNeoServerBootstrapper(GraphDatabaseAPI db) {
		this(db, new ServerConfigurator(db));
	}

	public StructrWrappingNeoServerBootstrapper(GraphDatabaseAPI db, Configurator configurator) {
		this(db, new ConfiguratorWrappingConfigurationBuilder(configurator));
	}

	private StructrWrappingNeoServerBootstrapper(GraphDatabaseAPI db, ConfigurationBuilder configurator) {
		this.db = db;
		this.configurator = configurator;
	}

	protected Configurator createConfigurator(ConsoleLogger log) {
		return new ConfigurationBuilder.ConfigurationBuilderWrappingConfigurator(createConfigurationBuilder(log));
	}

	protected ConfigurationBuilder createConfigurationBuilder(ConsoleLogger log) {
		return configurator;
	}

	@Override
	protected NeoServer createNeoServer() {
		return new StructrWrappingCommunityNeoServer(db, configurator);
	}
}
