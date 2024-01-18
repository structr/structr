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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.service.Command;
import org.structr.api.service.ServiceResult;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.api.util.CountResult;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;

import java.io.File;

/**
 * The graph/node service.
 */
public class NodeService implements SingletonService {

	private static final Logger logger      = LoggerFactory.getLogger(NodeService.class.getName());
	private StructrServices servicesParent  = null;
	private DatabaseService databaseService = null;
	private Index<Node> nodeIndex           = null;
	private Index<Relationship> relIndex    = null;
	private String filesPath                = null;
	private boolean isInitialized           = false;
	private CountResult initialCount        = null;

	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("graphDb",           databaseService);
			command.setArgument("nodeIndex",         nodeIndex);
			command.setArgument("relationshipIndex", relIndex);
			command.setArgument("filesPath",         filesPath);
		}
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

		this.servicesParent = services;

		final String databaseDriver = Settings.DatabaseDriver.getPrefixedValue(serviceName);
		String errorMessage         = null;

		databaseService = (DatabaseService)Class.forName(databaseDriver).getDeclaredConstructor().newInstance();
		if (databaseService != null) {

			if (databaseService.initialize(serviceName, services.getVersion(), services.getInstanceName())) {

				filesPath = Settings.FilesPath.getValue();

				// check existence of files path
				File files = new File(filesPath);
				if (!files.exists()) {

					files.mkdir();
				}

				logger.info("Database driver loaded, initializing indexes..");

				// index creation transaction
				try ( final Transaction tx = databaseService.beginTx() ) {

					nodeIndex = databaseService.nodeIndex();
					relIndex  = databaseService.relationshipIndex();

					tx.success();

					// if the above operations fail, the database is probably not available
					isInitialized = true;

					logger.info("Indexes successfully initialized.");



				} catch (Throwable t) {

					logger.warn("Error while initializing indexes: {}", t.getMessage());
				}

			} else {

				errorMessage = databaseService.getErrorMessage();
			}
		}

		return new ServiceResult(errorMessage, isInitialized);
	}

	@Override
	public void initialized() {

		// check for empty database and seed file
		String basePath = Settings.getBasePath();

		if (StringUtils.isEmpty(basePath)) {
			basePath = ".";
		}

		// don't check cache sizes when testing..
		if (!Services.isTesting()) {
			checkCacheSizes();
		}

		createAdminUser();
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			logger.info("Shutting down database service");
			databaseService.shutdown();

			databaseService = null;
			isInitialized   = false;
		}
	}

	@Override
	public String getName() {
		return NodeService.class.getSimpleName();
	}

	public DatabaseService getDatabaseService() {
		return databaseService;
	}

	@Override
	public boolean isRunning() {

		return ((databaseService != null) && isInitialized);
	}

	@Override
	public boolean isVital() {
		return true;
	}

	@Override
	public int getRetryDelay() {
		return Settings.NodeServiceStartTimeout.getValue();
	}

	@Override
	public int getRetryCount() {
		return Settings.NodeServiceStartRetries.getValue();
	}

	@Override
	public boolean waitAndRetry() {
		return true;
	}

	public Index<Node> getNodeIndex() {
		return nodeIndex;
	}

	public Index<Relationship> getRelationshipIndex() {
		return relIndex;
	}

	public CountResult getInitialCounts() {

		if (initialCount == null) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				initialCount = databaseService.getNodeAndRelationshipCount();
				tx.success();

			} catch (Throwable t) {
				logger.warn("Unable to count number of nodes and relationships: {}", t.getMessage());
			}
		}

		return initialCount;
	}

	public CountResult getCurrentCounts() {
		return databaseService.getNodeAndRelationshipCount();
	}

	public void createAdminUser() {

		if (Boolean.TRUE.equals(Settings.InitialAdminUserCreate.getValue())) {

			if (!Services.isTesting() && servicesParent.hasExclusiveDatabaseAccess()) {

				// do two very quick count queries to determine the number of Structr nodes in the database
				final CountResult count           = getInitialCounts();
				final long nodeCount              = count.getNodeCount();
				final boolean hasApplicationNodes = nodeCount > 0;

				if (!hasApplicationNodes) {

					logger.info("Creating initial user..");

					final Class userType = StructrApp.getConfiguration().getNodeEntityClass("User");
					if (userType != null) {

						final App app = StructrApp.getInstance();

						try (final Tx tx = app.tx()) {

							app.create(userType,
								new NodeAttribute<>(StructrApp.key(userType, "name"),     Settings.InitialAdminUserName.getValue()),
								new NodeAttribute<>(StructrApp.key(userType, "password"), Settings.InitialAdminUserPassword.getValue()),
								new NodeAttribute<>(StructrApp.key(userType, "isAdmin"),  true)
							);

							tx.success();

						} catch (Throwable t) {

							logger.warn("Unable to create initial user: {}", t.getMessage());
						}
					}
				}
			}

		} else {

			logger.info("Not creating initial user, as per configuration");
		}
	}

	// ----- private methods -----
	private void checkCacheSizes() {

		final CountResult counts = getInitialCounts();
		final long nodeCount     = counts.getNodeCount();
		final long relCount      = counts.getRelationshipCount();

		logger.info("Database contains {} nodes, {} relationships.", nodeCount, relCount);
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}
}
