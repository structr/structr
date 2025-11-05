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

import org.structr.api.index.ExistingIndexInfo;
import org.structr.api.index.NewIndexConfig;

import java.util.Map;

public class Neo5IndexUpdater extends IndexUpdater {

	public Neo5IndexUpdater(final BoltDatabaseService db, final boolean supportsRelationshipIndexes) {

		super(db, supportsRelationshipIndexes);
	}

	@Override
	protected String getIndexInfoQuery() {
		return "SHOW INDEXES YIELD name, type, state, labelsOrTypes, properties WHERE (type = 'RANGE' OR type = 'TEXT' OR type = 'FULLTEXT') RETURN {name: name, type: type, labels: labelsOrTypes, properties: properties, state: state}";
	}

	@Override
	protected String getIndexIdentifier(final NewIndexConfig config) {

		final String identifier = config.getType() + "_" + config.getPropertyKey();

		if (config.isFulltextIndex()) {

			return identifier + "_fulltext";
		}

		return identifier;
	}

	@Override
	protected String getIndexIdentifier(final ExistingIndexInfo config) {
		return config.getIdentifier();
	}

	@Override
	protected ExistingIndexInfo convertIndexInfoRow(final Map<String, Object> indexInfoRow) {

		final String indexIdentifier = (String) indexInfoRow.get("name");

		return new ExistingIndexInfo(indexIdentifier, (String) indexInfoRow.get("state"));
	}

	@Override
	protected String getCreateIndexStatement(final NewIndexConfig newIndexConfig) {

		final String indexDescription = newIndexConfig.getIndexDescriptionForStatement();
		final String identifier       = getIndexIdentifier(newIndexConfig);
		final String propertyKey      = newIndexConfig.getPropertyKey();

		if (newIndexConfig.isFulltextIndex()) {

			return "CREATE FULLTEXT INDEX " + identifier + " IF NOT EXISTS FOR " + indexDescription + " ON EACH [n.`" + propertyKey + "`]";

		} else if (newIndexConfig.isTextIndex()) {

			return "CREATE TEXT INDEX " + identifier + " IF NOT EXISTS FOR " + indexDescription + " ON (n.`" + propertyKey + "`)";

		} else {

			return "CREATE INDEX " + identifier + " IF NOT EXISTS FOR " + indexDescription + " ON (n.`" + propertyKey + "`)";
		}
	}

	@Override
	protected String getDropIndexStatement(final ExistingIndexInfo existingIndexInfo) {

		final String identifier = getIndexIdentifier(existingIndexInfo);

		return "DROP INDEX " + identifier + " IF EXISTS";
	}
}
