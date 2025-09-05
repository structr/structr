/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.helper.VersionHelper;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;

/**
 * Main startup class for Structr.
 *
 *
 */
public class Server {

	private static final Logger logger = LoggerFactory.getLogger(Server.class.getName());

	public static void main(String[] args) {

		logger.info("Starting Structr {}", VersionHelper.getFullVersionInfo());

		StructrApp.initializeSchemaIds();

		// start service layer using default configuration
		// augmented by local structr.conf
		Services.getInstance();
	}
}
