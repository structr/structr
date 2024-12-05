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
package org.structr.common.fulltext;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.fulltext.relationship.IndexableINDEXED_WORDIndexedWord;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

import java.io.InputStream;

/**
 */
public interface Indexable extends NodeInterface {

	Property<Iterable<NodeInterface>> wordsProperty = new EndNodes("words", "IndexableINDEXED_WORDIndexedWord").partOfBuiltInSchema();
	Property<Iterable<String>> indexedWordsProperty = new CollectionNotionProperty<>("indexedWords", wordsProperty, new PropertyNotion(AbstractNode.name, true)).partOfBuiltInSchema();
	Property<String> contentTypeProperty            = new StringProperty("contentType").partOfBuiltInSchema();
	Property<String> extractedContentProperty       = new StringProperty("extractedContent").partOfBuiltInSchema();

	View defaultView = new View(Indexable.class, PropertyView.Public,
		contentTypeProperty
	);

	View uiView = new View(Indexable.class, PropertyView.Ui,
		contentTypeProperty
	);

	InputStream getInputStream();
	String getExtractedContent();
	String getContentType();

	@Export
	default GraphObject getSearchContext(final SecurityContext ctx, final String searchTerm, final int contextLength) {

		final String text = getExtractedContent();
		if (StringUtils.isNotBlank(text)) {

			final FulltextIndexer indexer = StructrApp.getInstance(ctx).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	default boolean indexingEnabled() {
		return Settings.IndexingEnabled.getValue();
	}

	default Integer maximumIndexedWords() {
		return Settings.IndexingLimit.getValue();
	}

	default Integer indexedWordMinLength() {
		return Settings.IndexingMinLength.getValue();
	}

	default Integer indexedWordMaxLength() {
		return Settings.IndexingMaxLength.getValue();
	}
}
