/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.Transaction;

public class Neo3IndexUpdater {

	private static final Logger logger = LoggerFactory.getLogger(Neo4IndexUpdater.class);
	private BoltDatabaseService db     = null;

	public Neo3IndexUpdater(final BoltDatabaseService db) {
		this.db = db;
	}

	public void updateIndexConfiguration(final Map<String, Map<String, Boolean>> schemaIndexConfigSource, final Map<String, Map<String, Boolean>> removedClassesSource, final boolean createOnly) {

		final ExecutorService executor              = Executors.newCachedThreadPool();
		final Map<String, String> existingDbIndexes = new HashMap<>();
		final int timeoutSeconds                    = 10;

		try {

			executor.submit(() -> {

				try (final Transaction tx = db.beginTx(timeoutSeconds)) {

					for (final Map<String, Object> row : db.execute("CALL db.indexes() YIELD description, state, type WHERE type = 'node_label_property' RETURN {description: description, state: state} ORDER BY description")) {

						for (final Object value : row.values()) {

							final Map<String, String> valueMap = (Map<String, String>)value;

							existingDbIndexes.put(valueMap.get("description"), valueMap.get("state"));
						}
					}

					tx.success();
				}

			}).get(timeoutSeconds, TimeUnit.SECONDS);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		logger.debug("Found {} existing indexes", existingDbIndexes.size());

		final AtomicInteger createdIndexes = new AtomicInteger(0);
		final AtomicInteger droppedIndexes = new AtomicInteger(0);

		// create indices for properties of existing classes
		for (final Map.Entry<String, Map<String, Boolean>> entry : schemaIndexConfigSource.entrySet()) {

			final String typeName = entry.getKey();

			for (final Map.Entry<String, Boolean> propertyIndexConfig : entry.getValue().entrySet()) {

				final String indexDescriptionForLookup = "INDEX ON :" + typeName + "(" + propertyIndexConfig.getKey() + ")";
				final String indexDescription          = "INDEX ON :" + typeName + "(`" + propertyIndexConfig.getKey() + "`)";
				final String currentState              = existingDbIndexes.get(indexDescriptionForLookup);
				final boolean indexAlreadyOnline       = Boolean.TRUE.equals("ONLINE".equals(currentState));
				final boolean configuredAsIndexed      = propertyIndexConfig.getValue();

				if ("FAILED".equals(currentState)) {

					logger.warn("Index is in FAILED state - dropping the index before handling it further. {}. If this error is recurring, please verify that the data in the concerned property is indexable by Neo4j", indexDescription);

					final AtomicBoolean retry = new AtomicBoolean(true);
					final AtomicInteger retryCount = new AtomicInteger(0);

					while (retry.get()) {

						retry.set(false);

						try {

							executor.submit(() -> {

								try (final Transaction tx = db.beginTx(timeoutSeconds)) {

									db.execute("DROP " + indexDescription);

									tx.success();

								} catch (RetryException rex) {

									retry.set(retryCount.incrementAndGet() < 3);
									logger.info("DROP INDEX: retry {}", retryCount.get());

								} catch (Throwable t) {
									logger.warn("Unable to drop failed index: {}", t.getMessage());
								}

								return null;

							}).get(timeoutSeconds, TimeUnit.SECONDS);

						} catch (Throwable t) {
							logger.error(ExceptionUtils.getStackTrace(t));
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

								if (configuredAsIndexed) {

									if (!indexAlreadyOnline) {

										try {

											db.execute("CREATE " + indexDescription);
											createdIndexes.incrementAndGet();

										} catch (Throwable t) {
											logger.warn("Unable to create {}: {}", indexDescription, t.getMessage());
										}
									}

								} else if (indexAlreadyOnline && !createOnly) {

									try {

										db.execute("DROP " + indexDescription);
										droppedIndexes.incrementAndGet();

									} catch (Throwable t) {
										logger.warn("Unable to drop {}: {}", indexDescription, t.getMessage());
									}
								}

								tx.success();

							} catch (RetryException rex) {

								retry.set(retryCount.incrementAndGet() < 3);
								logger.info("INDEX update: retry {}", retryCount.get());

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
			for (final Map.Entry<String, Map<String, Boolean>> entry : removedClassesSource.entrySet()) {

				final String typeName = entry.getKey();
				removedTypes.add(typeName);

				for (final Map.Entry<String, Boolean> propertyIndexConfig : entry.getValue().entrySet()) {

					final String indexDescriptionForLookup = "INDEX ON :" + typeName + "(" + propertyIndexConfig.getKey() + ")";
					final String indexDescription          = "INDEX ON :" + typeName + "(`" + propertyIndexConfig.getKey() + "`)";
					final boolean indexExists              = (existingDbIndexes.get(indexDescriptionForLookup) != null);
					final boolean configuredAsIndexed      = propertyIndexConfig.getValue();

					if (indexExists && configuredAsIndexed) {

						final AtomicBoolean retry = new AtomicBoolean(true);
						final AtomicInteger retryCount = new AtomicInteger(0);

						while (retry.get()) {

							retry.set(false);

							try {

								executor.submit(() -> {

									try (final Transaction tx = db.beginTx(timeoutSeconds)) {

										// drop index
										db.execute("DROP " + indexDescription);
										droppedIndexesOfRemovedTypes.incrementAndGet();

										tx.success();

									} catch (RetryException rex) {

										retry.set(retryCount.incrementAndGet() < 3);
										logger.info("DROP INDEX: retry {}", retryCount.get());

									} catch (Throwable t) {
										logger.warn("Unable to drop {}: {}", indexDescription, t.getMessage());
									}

								}).get(timeoutSeconds, TimeUnit.SECONDS);

							} catch (Throwable t) {}
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
