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

import org.structr.api.index.ExistingIndexInfo;
import org.structr.api.index.NewIndexConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public class Neo5IndexUpdater extends IndexUpdater {

	/** The name of every vector index Structr manages ends with this; other vector indexes are left alone. */
	public static final String VECTOR_SUFFIX = "_vector";

	public Neo5IndexUpdater(final EmbeddedDatabaseService db, final boolean supportsRelationshipIndexes) {

		super(db, supportsRelationshipIndexes);
	}

	/**
	 * Lists only the indexes Structr manages. Every listed index that is not in the schema is dropped,
	 * so other indexes must stay out of this list: LOOKUP and POINT indexes, and VECTOR indexes whose
	 * name does not end in "_vector", e.g. ones created by hand with Cypher.
	 */
	@Override
	protected String getIndexInfoQuery() {

		return "SHOW INDEXES YIELD name, type, state, labelsOrTypes, properties, options "
			+ "WHERE (type = 'RANGE' OR type = 'TEXT' OR type = 'FULLTEXT' OR (type = 'VECTOR' AND name ENDS WITH '" + VECTOR_SUFFIX + "')) "
			+ "RETURN {name: name, type: type, labels: labelsOrTypes, properties: properties, state: state, "
			+ "dimensions: options.indexConfig['vector.dimensions'], similarity: options.indexConfig['vector.similarity_function']}";
	}

	@Override
	protected boolean supportsVectorIndexes() {

		return true;
	}

	@Override
	protected String getIndexIdentifier(final NewIndexConfig config) {

		if (config.isFulltextIndex()) {

			return config.getType() + "_" + config.getIndexName() + "_fulltext";
		}

		if (config.isVectorIndex()) {

			return config.getType() + "_" + config.getPropertyKey() + VECTOR_SUFFIX;
		}

		return config.getType() + "_" + config.getPropertyKey();
	}

	@Override
	protected String getIndexIdentifier(final ExistingIndexInfo config) {

		return config.getIdentifier();
	}

	@Override
	protected ExistingIndexInfo convertIndexInfoRow(final Map<String, Object> indexInfoRow) {

		final String indexIdentifier = (String) indexInfoRow.get("name");

		final Map<String, Object> options = new LinkedHashMap<>();

		if (indexInfoRow.get("dimensions") != null) {
			options.put("dimensions", indexInfoRow.get("dimensions"));
		}

		if (indexInfoRow.get("similarity") != null) {
			options.put("similarity", indexInfoRow.get("similarity"));
		}

		return new ExistingIndexInfo(indexIdentifier, (String) indexInfoRow.get("state"), (String) indexInfoRow.get("type"), properties(indexInfoRow.get("properties")), options);
	}

	@Override
	protected String getExpectedIndexType(final NewIndexConfig config) {

		if (config.isFulltextIndex()) return "FULLTEXT";
		if (config.isVectorIndex())   return "VECTOR";
		if (config.isTextIndex())     return "TEXT";

		return "RANGE";
	}

	@Override
	protected String getCreateIndexStatement(final NewIndexConfig newIndexConfig) {

		final String indexDescription = newIndexConfig.getIndexDescriptionForStatement();
		final String identifier       = getIndexIdentifier(newIndexConfig);
		final String propertyKey      = newIndexConfig.getPropertyKey();

		if (newIndexConfig.isVectorIndex()) {

			return "CREATE VECTOR INDEX `" + identifier + "` FOR " + indexDescription + " ON (n.`" + propertyKey + "`) OPTIONS { indexConfig: { "
				+ "`vector.dimensions`: " + newIndexConfig.getVectorDimensions() + ", `vector.similarity_function`: '" + newIndexConfig.getVectorSimilarity() + "' } }";

		} else if (newIndexConfig.isFulltextIndex()) {

			return "CREATE FULLTEXT INDEX `" + identifier + "` FOR " + indexDescription + " ON EACH [" + fulltextProperties(newIndexConfig) + "]";

		} else if (newIndexConfig.isTextIndex()) {

			return "CREATE TEXT INDEX `" + identifier + "` FOR " + indexDescription + " ON (n.`" + propertyKey + "`)";

		} else {

			return "CREATE INDEX `" + identifier + "` FOR " + indexDescription + " ON (n.`" + propertyKey + "`)";
		}
	}

	@Override
	protected String getDropIndexStatement(final ExistingIndexInfo existingIndexInfo) {

		final String identifier = getIndexIdentifier(existingIndexInfo);

		return "DROP INDEX `" + identifier + "` IF EXISTS";
	}
}
