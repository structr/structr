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
package org.structr.bolt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.Transaction;
import org.structr.api.index.IndexConfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Neo5IndexUpdater {

	private static final Logger logger          = LoggerFactory.getLogger(Neo5IndexUpdater.class);
	private boolean supportsRelationshipIndexes = false;
	private BoltDatabaseService db              = null;

	public Neo5IndexUpdater(final BoltDatabaseService db, final boolean supportsRelationshipIndexes) {

		this.supportsRelationshipIndexes = supportsRelationshipIndexes;
		this.db                          = db;
	}

	public void updateIndexConfiguration(final Map<String, Map<String, IndexConfig>> schemaIndexConfigSource, final Map<String, Map<String, IndexConfig>> removedClassesSource, final boolean createOnly) {

		final ExecutorService executor                           = Executors.newCachedThreadPool();
		final Map<String, Map<String, Object>> existingDbIndexes = new HashMap<>();
		final int timeoutSeconds                                 = 1000;

		try {

			executor.submit(() -> {

				try (final Transaction tx = db.beginTx(timeoutSeconds)) {

					for (final Map<String, Object> row : db.execute("SHOW INDEXES YIELD name, type, state, labelsOrTypes, properties WHERE type = 'RANGE' RETURN {name: name, type: type, labels: labelsOrTypes, properties: properties, state: state}")) {

						for (final Object value : row.values()) {

							final Map<String, Object> valueMap = (Map<String, Object>)value;
							final List<String> labels          = (List<String>)valueMap.get("labels");
							final List<String> properties      = (List<String>)valueMap.get("properties");
							final String indexIdentifier       = StringUtils.join(labels, ", ") + "." + StringUtils.join(properties, ", ");

							// store index config
							existingDbIndexes.put(indexIdentifier, valueMap);
						}
					}

					tx.success();
				}

			}).get(timeoutSeconds, TimeUnit.SECONDS);

		} catch (Throwable t) {
			t.printStackTrace();
			//logger.error(ExceptionUtils.getStackTrace(t));
		}

		logger.debug("Found {} existing indexes", existingDbIndexes.size());

		final AtomicInteger createdIndexes = new AtomicInteger(0);
		final AtomicInteger droppedIndexes = new AtomicInteger(0);

		// create indices for properties of existing classes
		for (final Map.Entry<String, Map<String, IndexConfig>> entry : schemaIndexConfigSource.entrySet()) {

			final String typeName = entry.getKey();

			for (final Map.Entry<String, IndexConfig> propertyIndexConfig : entry.getValue().entrySet()) {

				final String indexIdentifier        = typeName + "." + propertyIndexConfig.getKey();
				final Map<String, Object> neoConfig = existingDbIndexes.get(indexIdentifier);
				String indexName                    = null;
				String currentState                 = null;
				boolean indexAlreadyOnline          = false;

				if (neoConfig != null) {

					currentState       = (String)neoConfig.get("state");
					indexAlreadyOnline = Boolean.TRUE.equals("ONLINE".equals(currentState));
					indexName          = (String)neoConfig.get("name");

				}

				final IndexConfig indexConfig         = propertyIndexConfig.getValue();
				final String propertyKey              = propertyIndexConfig.getKey();
				final boolean finalIndexAlreadyOnline = indexAlreadyOnline;
				final String finalIndexName           = indexName;

				// skip relationship indexes if not supported
				if (!indexConfig.isNodeIndex() && !supportsRelationshipIndexes) {
					continue;
				}

				if ("FAILED".equals(currentState)) {

					logger.warn("Index is in FAILED state - dropping the index before handling it further. {}.{}. If this error is recurring, please verify that the data in the concerned property is indexable by Neo4j", typeName, propertyKey);

					final AtomicBoolean retry = new AtomicBoolean(true);
					final AtomicInteger retryCount = new AtomicInteger(0);

					while (retry.get()) {

						retry.set(false);

						try {

							executor.submit(() -> {

								try (final Transaction tx = db.beginTx(timeoutSeconds)) {

									db.consume("DROP INDEX " + finalIndexName + " IF EXISTS");

									tx.success();

								} catch (RetryException rex) {

									retry.set(retryCount.incrementAndGet() < 3);
									logger.debug("DROP INDEX: retry {}", retryCount.get());

								} catch (Throwable t) {
									logger.warn("Unable to drop failed index: {}", t.getMessage());
								}

								return null;

							}).get(timeoutSeconds, TimeUnit.SECONDS);

						} catch (Throwable t) {
							t.printStackTrace();
							//logger.error(ExceptionUtils.getStackTrace(t));
						}
					}
				}

				final AtomicBoolean retry = new AtomicBoolean(true);
				final AtomicInteger retryCount = new AtomicInteger(0);

				while (retry.get()) {

					retry.set(false);

					try {

						executor.submit(() -> {

							try (final Transaction tx = db.beginTx(timeoutSeconds)) {

								if (indexConfig.createOrDropIndex()) {

									if (!finalIndexAlreadyOnline) {

										try {

											final String indexDescription = indexConfig.getIndexDescriptionForStatement(typeName);

											db.consume("CREATE INDEX IF NOT EXISTS FOR " + indexDescription + " ON (n.`" + propertyKey + "`)");
											createdIndexes.incrementAndGet();

										} catch (Throwable t) {
											logger.warn("Unable to create index for {}.{}: {}", typeName, propertyKey, t.getMessage());
										}
									}

								} else if (finalIndexAlreadyOnline && !createOnly) {

									try {

										db.consume("DROP INDEX " + finalIndexName + " IF EXISTS");
										droppedIndexes.incrementAndGet();

									} catch (Throwable t) {
										logger.warn("Unable to drop index {}.{}: {}", typeName, propertyKey, t.getMessage());
									}
								}

								tx.success();

							} catch (RetryException rex) {

								retry.set(retryCount.incrementAndGet() < 3);
								logger.debug("INDEX update: retry {}", retryCount.get());

							} catch (IllegalStateException i) {

								// if the driver instance is already closed, there is nothing we can do => exit
								return;

							} catch (Throwable t) {

								logger.warn("Unable to update index configuration: {}", t.getMessage());
							}

						}).get(timeoutSeconds, TimeUnit.SECONDS);

					} catch (Throwable t) {}
				}
			}
		}

		if (createdIndexes.get() > 0) {
			logger.debug("Created {} indexes", createdIndexes.get());
		}

		if (droppedIndexes.get() > 0) {
			logger.debug("Dropped {} indexes", droppedIndexes.get());
		}

		if (!createOnly) {

			final AtomicInteger droppedIndexesOfRemovedTypes = new AtomicInteger(0);
			final List removedTypes = new LinkedList();

			// drop indices for all indexed properties of removed classes
			for (final Map.Entry<String, Map<String, IndexConfig>> entry : removedClassesSource.entrySet()) {

				final String typeName = entry.getKey();
				removedTypes.add(typeName);

				for (final Map.Entry<String, IndexConfig> propertyIndexConfig : entry.getValue().entrySet()) {

					final String indexIdentifier        = typeName + "." + propertyIndexConfig.getKey();
					final Map<String, Object> neoConfig = existingDbIndexes.get(indexIdentifier);

					if (neoConfig != null) {

						final IndexConfig indexConfig = propertyIndexConfig.getValue();
						final String indexName        = (String)neoConfig.get("name");
						final boolean indexExists     = (existingDbIndexes.get(indexIdentifier) != null);
						final String propertyKey      = propertyIndexConfig.getKey();

						// skip relationship indexes if not supported
						if (!indexConfig.isNodeIndex() && !supportsRelationshipIndexes) {
							continue;
						}

						if (indexExists && indexConfig.createOrDropIndex()) {

							final AtomicBoolean retry = new AtomicBoolean(true);
							final AtomicInteger retryCount = new AtomicInteger(0);

							while (retry.get()) {

								retry.set(false);

								try {

									executor.submit(() -> {

										try (final Transaction tx = db.beginTx(timeoutSeconds)) {
											
											// drop index
											db.consume("DROP INDEX " + indexName + " IF EXISTS");
											droppedIndexesOfRemovedTypes.incrementAndGet();

											tx.success();

										} catch (RetryException rex) {

											retry.set(retryCount.incrementAndGet() < 3);
											logger.debug("DROP INDEX: retry {}", retryCount.get());

										} catch (Throwable t) {
											logger.warn("Unable to drop {}{}: {}", typeName, propertyKey, t.getMessage());
										}

									}).get(timeoutSeconds, TimeUnit.SECONDS);

								} catch (Throwable t) {
									t.printStackTrace();
								}
							}
						}
					}
				}
			}

			if (droppedIndexesOfRemovedTypes.get() > 0) {
				logger.debug("Dropped {} indexes of deleted types ({})", droppedIndexesOfRemovedTypes.get(), StringUtils.join(removedTypes, ", "));
			}
		}
	}
}
