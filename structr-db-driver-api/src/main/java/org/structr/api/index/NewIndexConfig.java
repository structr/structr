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
package org.structr.api.index;

import java.util.ArrayList;
import java.util.List;

public class NewIndexConfig {

	private final List<String> propertyKeys = new ArrayList<>();
	private final String indexName;
	private final String type;
	private final boolean isNodeIndex;
	private final boolean isTextIndex;
	private final boolean isFulltextIndex;
	private final Integer vectorDimensions;
	private final String vectorSimilarity;

	public NewIndexConfig(final String type, final String propertyKey, final boolean isNodeIndex, final boolean isTextIndex, final boolean isFulltextIndex) {

		this(type, propertyKey, propertyKey, isNodeIndex, isTextIndex, isFulltextIndex);
	}

	/**
	 * @param indexName the name part of the index identifier; a fulltext index can cover more than one
	 *                  property, which are then added with {@link #addPropertyKey(String)}
	 */
	public NewIndexConfig(final String type, final String indexName, final String propertyKey, final boolean isNodeIndex, final boolean isTextIndex, final boolean isFulltextIndex) {

		this(type, indexName, propertyKey, isNodeIndex, isTextIndex, isFulltextIndex, null, null);
	}

	private NewIndexConfig(final String type, final String indexName, final String propertyKey, final boolean isNodeIndex, final boolean isTextIndex, final boolean isFulltextIndex, final Integer vectorDimensions, final String vectorSimilarity) {

		this.isNodeIndex      = isNodeIndex;
		this.isTextIndex      = isTextIndex;
		this.isFulltextIndex  = isFulltextIndex;
		this.indexName        = indexName;
		this.type             = type;
		this.vectorDimensions = vectorDimensions;
		this.vectorSimilarity = vectorSimilarity;

		this.propertyKeys.add(propertyKey);
	}

	/**
	 * A vector index on a node property that holds number arrays, for nearest-neighbour queries.
	 *
	 * @param dimensions the size of the indexed vectors; vectors of another size are not indexed
	 * @param similarity "cosine" or "euclidean"
	 */
	public static NewIndexConfig vector(final String type, final String propertyKey, final int dimensions, final String similarity) {

		return new NewIndexConfig(type, propertyKey, propertyKey, true, false, false, dimensions, similarity);
	}

	/**
	 * Adds a property to a fulltext index. The properties are kept sorted, so the index is the same
	 * regardless of the order in which they are added.
	 */
	public void addPropertyKey(final String propertyKey) {

		if (!propertyKeys.contains(propertyKey)) {

			propertyKeys.add(propertyKey);
			propertyKeys.sort(null);
		}
	}

	public boolean isFulltextIndex() {

		return isFulltextIndex;
	}

	public boolean isTextIndex() {

		return isTextIndex;
	}

	public boolean isVectorIndex() {

		return vectorDimensions != null;
	}

	/**
	 * The size of the vectors of a vector index, or null for other indexes.
	 */
	public Integer getVectorDimensions() {

		return vectorDimensions;
	}

	/**
	 * The similarity function of a vector index ("cosine" or "euclidean"), or null for other indexes.
	 */
	public String getVectorSimilarity() {

		return vectorSimilarity;
	}

	public String getType() {

		return type;
	}

	public String getIndexName() {

		return indexName;
	}

	/**
	 * The (first) property of the index; range and text indexes have exactly one.
	 */
	public String getPropertyKey() {

		return propertyKeys.getFirst();
	}

	/**
	 * The properties of the index, sorted.
	 */
	public List<String> getPropertyKeys() {

		return List.copyOf(propertyKeys);
	}

	public String getIndexDescriptionForStatement() {

		if (this.isNodeIndex) {

			return "(n:" + type + ")";
		}

		return "()-[n:" + type + "]-()";
	}

}
