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
package org.structr.test.common;

import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.neo4j.Neo4jContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

public final class SharedNeo4jContainer {

	private static final Logger logger = LoggerFactory.getLogger(SharedNeo4jContainer.class);

	private static final String NEO4J_VERSION = "neo4j:2025";
	private static final String NEO4J_PASSWORD = "admin123";
	private static final String NEO4J_USERNAME = "neo4j";

	private static final Neo4jContainer CONTAINER;
	private static final boolean CONTAINER_STARTED;

	static {
		Neo4jContainer container = null;
		boolean started = false;

		final String FORK_ID = System.getProperty("surefire.forkNumber", "0");
		String module = System.getProperty("structr.test.module", "default");
		if (StringUtils.isBlank(module)) {
			module = "default";
		}

		String MODULE_NAME = module;
		final String CONTAINER_LABEL = "structr-test-neo4j-" + MODULE_NAME + FORK_ID;

		logger.info("SharedNeo4jContainer: MODULE_NAME = {}", MODULE_NAME);
		logger.info("SharedNeo4jContainer: CONTAINER_LABEL = {}", CONTAINER_LABEL);

		// allow reuse of docker container
		TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "true");

		String testDriver = System.getProperty("testDatabaseDriver", "");
		boolean useNeo4j = testDriver.isEmpty() || testDriver.contains("Bolt");

		if (useNeo4j) {
			try {
				container = new Neo4jContainer(NEO4J_VERSION)
						.withAdminPassword(NEO4J_PASSWORD)
						.withReuse(true) // reuse containers for performance
						.withLabel("purpose", CONTAINER_LABEL)
						.withEnv("STRUCTR_TEST_MODULE", MODULE_NAME); // changes hash for reused container

				container.start();

				started = true;

			} catch (Exception e) {
				logger.error("Failed to start/connect to Neo4j container", e);
				throw new ExceptionInInitializerError(e);
			}
		} else {
			logger.info("Using in-memory database driver, skipping Neo4j container");
		}

		CONTAINER = container;
		CONTAINER_STARTED = started;
	}

	private SharedNeo4jContainer() {
	}

	public static String getBoltUrl() {
		checkContainerAvailable();
		return CONTAINER.getBoltUrl();
	}

	public static String getUsername() {
		return NEO4J_USERNAME;
	}

	public static String getPassword() {
		return NEO4J_PASSWORD;
	}

	public static boolean isAvailable() {
		return CONTAINER_STARTED && CONTAINER != null && CONTAINER.isRunning();
	}

	public static String getVersion() {
		return NEO4J_VERSION;
	}

	private static void checkContainerAvailable() {
		if (!CONTAINER_STARTED || CONTAINER == null) {
			throw new IllegalStateException("Shared Neo4j container is not available. This may happen if you're using the in-memory database driver.Check the testDatabaseDriver system property.");
		}
		if (!CONTAINER.isRunning()) {
			throw new IllegalStateException("Shared Neo4j container is not running. Container ID was: " + CONTAINER.getContainerId() + ".The container may have been stopped externally.");
		}
	}
}