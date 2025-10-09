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
package org.structr.bolt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Transaction;
import org.structr.api.index.ExistingIndexInfo;
import org.structr.api.index.NewIndexConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IndexUpdater {

	private static final Logger logger = LoggerFactory.getLogger(IndexUpdater.class);

	private final boolean supportsRelationshipIndexes;
	private final AtomicBoolean isFinished;
	private final BoltDatabaseService db;

	protected abstract String getIndexInfoQuery();
	protected abstract String getIndexIdentifier(final NewIndexConfig config);
	protected abstract String getIndexIdentifier(final ExistingIndexInfo config);
	protected abstract ExistingIndexInfo convertIndexInfoRow(final Map<String, Object> indexInfoRow);
	protected abstract String getCreateIndexStatement(final NewIndexConfig newIndexConfig);
	protected abstract String getDropIndexStatement(final ExistingIndexInfo existingIndexInfo);

	protected IndexUpdater(final BoltDatabaseService db, final boolean supportsRelationshipIndexes) {

		this.supportsRelationshipIndexes = supportsRelationshipIndexes;
		this.isFinished                  = new AtomicBoolean(false);
		this.db                          = db;
	}

	public void updateIndexConfiguration(final List<NewIndexConfig> indexesToBeCreated) {

		isFinished.set(false);

		try {

			// 1. fetch existing indexes
			final Map<String, ExistingIndexInfo> existingIndexes = getExistingIndexes();

			// 2. create indexes that don't exist and reduce list so that indexes to drop remain
			createIndexes(indexesToBeCreated, existingIndexes);

			// 3. drop indexes that exist but are not to be created
			dropIndexes(existingIndexes);

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

		logger.info("Found {} existing indexes", existingIndexes.size());

		return existingIndexes;
	}

	private void createIndexes(final List<NewIndexConfig> newIndexes, final Map<String, ExistingIndexInfo> existingIndexes) {

		int newIndexCount = 0;

		try (final Transaction tx = db.beginTx()) {

			// create indexes that dont exist yet
			for (final NewIndexConfig newIndexConfig : newIndexes) {

				final String identifier = getIndexIdentifier(newIndexConfig);

				if (existingIndexes.containsKey(identifier)) {

					// index exists AND is in the list of indexes to be created => remove from list
					// (remaining indexes are dropped)
					existingIndexes.remove(identifier);

				} else {

					db.execute(getCreateIndexStatement(newIndexConfig));

					newIndexCount++;
				}
			}

			tx.success();
		}

		logger.info("Created {} new indexes", newIndexCount);
	}

	private void dropIndexes(final Map<String, ExistingIndexInfo> existingIndexes) {

		try (final Transaction tx = db.beginTx()) {

			// the list now contains only those indexes that are to be dropped
			for (final ExistingIndexInfo existingIndexInfo : existingIndexes.values()) {

				db.execute(getDropIndexStatement(existingIndexInfo));
			}

			tx.success();
		}

		logger.info("Dropped {} indexes", existingIndexes.size());
	}
}
