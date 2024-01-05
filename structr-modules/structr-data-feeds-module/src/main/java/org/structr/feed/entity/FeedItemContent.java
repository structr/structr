/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.feed.entity;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Represents a content element of a feed item
 *
 */
public interface FeedItemContent extends NodeInterface, Indexable {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType type      = schema.addType("FeedItemContent");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/FeedItemContent"));
		type.setImplements(URI.create("#/definitions/Indexable"));

		type.addStringProperty("mode",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("itemType", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("value",    PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("value",            String.class);
		type.addPropertyGetter("contentType",      String.class);
		type.addPropertyGetter("extractedContent", String.class);
		type.addPropertySetter("value",            String.class);

		// methods shared with FeedItem
		type.overrideMethod("afterCreation",    false,             FeedItemContent.class.getName() + ".updateIndex(this, arg0);");
		type.overrideMethod("getSearchContext", false, "return " + FeedItemContent.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);").setDoExport(true);
		type.overrideMethod("getInputStream",   false, "return " + FeedItemContent.class.getName() + ".getInputStream(this);");

		// view configuration
		type.addViewProperty(PropertyView.Public, "owner");
		type.addViewProperty(PropertyView.Ui, "item");
	}}

	String getValue();
	void setValue(final String value) throws FrameworkException;

	static void updateIndex(final Indexable thisIndexable, final SecurityContext securityContext) {

		try {

			if (thisIndexable.indexingEnabled()) {

				final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
				indexer.addToFulltextIndex(thisIndexable);
			}

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(FeedItemContent.class);
			logger.warn("Unable to index {}: {}", thisIndexable, fex.getMessage());
		}
	}

	static GraphObject getSearchContext(final Indexable thisIndexable, final SecurityContext ctx, final String searchTerm, final int contextLength) {

		final String text = thisIndexable.getExtractedContent();
		if (StringUtils.isNotBlank(text)) {

			final FulltextIndexer indexer = StructrApp.getInstance(ctx).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	static InputStream getInputStream(final FeedItemContent thisContent) {
		return IOUtils.toInputStream(thisContent.getValue(), Charset.forName("utf-8"));
	}

	@Override
	default boolean indexingEnabled() {
		return Settings.FeedItemContentIndexingEnabled.getValue();
	}

	@Override
	default Integer maximumIndexedWords() {
		return Settings.FeedItemContentIndexingLimit.getValue();
	}

	@Override
	default Integer indexedWordMinLength() {
		return Settings.FeedItemContentIndexingMinLength.getValue();
	}

	@Override
	default Integer indexedWordMaxLength() {
		return Settings.FeedItemContentIndexingMaxLength.getValue();
	}
}
