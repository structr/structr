/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.embedded;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Transaction;
import org.structr.api.index.ExistingIndexInfo;
import org.structr.api.index.NewIndexConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IndexUpdater {

	private static final Logger logger = LoggerFactory.getLogger(IndexUpdater.class);

	private final boolean supportsRelationshipIndexes;
	private final AtomicBoolean           isFinished;
	private final EmbeddedDatabaseService db;

	protected abstract String getIndexInfoQuery();
	protected abstract String getIndexIdentifier(final NewIndexConfig config);
	protected abstract String getIndexIdentifier(final ExistingIndexInfo config);
	protected abstract ExistingIndexInfo convertIndexInfoRow(final Map<String, Object> indexInfoRow);
	protected abstract String getCreateIndexStatement(final NewIndexConfig newIndexConfig);
	protected abstract String getDropIndexStatement(final ExistingIndexInfo existingIndexInfo);
	protected abstract String getExpectedIndexType(final NewIndexConfig config);

	protected IndexUpdater(final EmbeddedDatabaseService db, final boolean supportsRelationshipIndexes) {

		this.supportsRelationshipIndexes = supportsRelationshipIndexes;
		this.isFinished                  = new AtomicBoolean(false);
		this.db                          = db;
	}

	public void updateIndexConfiguration(final List<NewIndexConfig> indexesToBeCreated) {

		isFinished.set(false);

		try {

			// 1. fetch existing indexes
			final Map<String, ExistingIndexInfo> existingIndexes = getExistingIndexes();
			final int existingIndexCount = existingIndexes.size();

			// 2. classify: determine which indexes to create and which to drop
			//    - existing index with matching type: keep (remove from drop candidates)
			//    - existing index with wrong type: drop old + create new
			//    - no existing index: create new
			final List<NewIndexConfig> toCreate = new ArrayList<>();

			for (final NewIndexConfig newIndexConfig : indexesToBeCreated) {

				final String identifier = getIndexIdentifier(newIndexConfig);
				if (existingIndexes.containsKey(identifier)) {

					final ExistingIndexInfo existing = existingIndexes.get(identifier);
					if (getExpectedIndexType(newIndexConfig).equals(existing.getType())) {

						// same type: nothing to do, remove from drop candidates
						existingIndexes.remove(identifier);

					} else {

						// type mismatch: leave in drop candidates and schedule for re-creation
						toCreate.add(newIndexConfig);
					}

				} else {

					toCreate.add(newIndexConfig);
				}
			}

			// 3. drop stale and type-mismatched indexes BEFORE creating new ones
			//    (Neo4j does not allow replacing an index with a different type under the same name)
			final int droppedIndexCount = dropIndexes(existingIndexes);

			// 4. create new and re-created indexes
			final int newIndexCount = createIndexes(toCreate);
			if (newIndexCount > 0 || droppedIndexCount > 0) {

				logger.info("Found {} existing indexes", existingIndexCount);
				logger.info("Created {} new indexes", newIndexCount);
				logger.info("Dropped {} indexes", droppedIndexCount);
			}

		} finally {

			isFinished.set(true);
		}
	}

	public boolean isFinished() {

		return isFinished.get();
	}

	// ----- private methods -----
	private Map<String, ExistingIndexInfo> getExistingIndexes() {

		final Map<String, ExistingIndexInfo> existingIndexes = new LinkedHashMap<>();

		// retrieve list of existing indexes
		try (final Transaction tx = db.beginTx()) {

			for (final Map<String, Object> row : db.execute(getIndexInfoQuery())) {

				for (final Object value : row.values()) {

					final ExistingIndexInfo indexInfo = convertIndexInfoRow((Map<String, Object>)value);
					final String identifier           = getIndexIdentifier(indexInfo);

					// store index config
					existingIndexes.put(identifier, indexInfo);
				}
			}

			tx.success();
		}

		return existingIndexes;
	}

	private int createIndexes(final List<NewIndexConfig> newIndexes) {

		int newIndexCount = 0;

		try (final Transaction tx = db.beginTx()) {

			for (final NewIndexConfig newIndexConfig : newIndexes) {

				db.execute(getCreateIndexStatement(newIndexConfig));
				newIndexCount++;
			}

			tx.success();
		}

		return newIndexCount;
	}

	private int dropIndexes(final Map<String, ExistingIndexInfo> existingIndexes) {

		try (final Transaction tx = db.beginTx()) {

			// the list now contains only those indexes that are to be dropped
			for (final ExistingIndexInfo existingIndexInfo : existingIndexes.values()) {

				db.execute(getDropIndexStatement(existingIndexInfo));
			}

			tx.success();
		}

		return existingIndexes.size();
	}
}
