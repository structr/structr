/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.*;
import org.structr.schema.SchemaService;

@ServiceDependency(SchemaService.class)
public class MigrationService implements SingletonService {

	private static final Logger logger = LoggerFactory.getLogger(MigrationService.class);
	private boolean running            = false;

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, final String serviceName) throws ReflectiveOperationException {

		migrateEventActionMapping();
		migrateRestQueryRepeaters();

		return new ServiceResult(true);
	}

	@Override
	public void shutdown() {
		running = false;
	}

	@Override
	public void initialized() {
		running = true;
	}

	@Override
	public String getName() {
		return MigrationService.class.getSimpleName();
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	@Override
	public String getModuleName() {
		return "core";
	}

	// ----- private methods -----
	private void migrateEventActionMapping() {

		logger.info("Migrating Event Action Mapping..");

	}

	private void migrateRestQueryRepeaters() {

		logger.info("Migrating REST query repeaters..");
	}
}
