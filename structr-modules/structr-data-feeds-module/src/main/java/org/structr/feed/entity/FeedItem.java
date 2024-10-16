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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.SchemaService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Represents a single item of a data feed.
 *
 */
public interface FeedItem extends NodeInterface, Indexable {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType type      = schema.addType("FeedItem");
		final JsonObjectType content   = schema.addType("FeedItemContent");
		final JsonObjectType enclosure = schema.addType("FeedItemEnclosure");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/FeedItem"));
		type.setImplements(URI.create("#/definitions/Indexable"));

		type.addStringProperty("url",              PropertyView.Public, PropertyView.Ui).setRequired(true).setIndexed(true);
		type.addStringProperty("author",           PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("comments",         PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("description",      PropertyView.Public, PropertyView.Ui);
		type.addDateProperty("pubDate",            PropertyView.Public, PropertyView.Ui);
		type.addDateProperty("updatedDate",        PropertyView.Public, PropertyView.Ui);
		type.addLongProperty("checksum",           PropertyView.Ui).setReadOnly(true);
		type.addIntegerProperty("cacheForSeconds", PropertyView.Ui);
		type.addIntegerProperty("version",         PropertyView.Ui).setReadOnly(true);

		type.addPropertyGetter("url",              String.class);
		type.addPropertyGetter("contentType",      String.class);
		type.addPropertyGetter("extractedContent", String.class);

		type.overrideMethod("getInputStream",   false, "return " + FeedItem.class.getName() + ".getInputStream(this);");

		// methods shared with FeedItemContent
		type.overrideMethod("onCreation",       true,              FeedItemContent.class.getName() + ".updateIndex(this, arg0);");
		type.overrideMethod("afterCreation",    false,             FeedItemContent.class.getName() + ".updateIndex(this, arg0);");
		type.overrideMethod("getSearchContext", false, "return " + FeedItemContent.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);").setDoExport(true);

		type.relate(content,   "FEED_ITEM_CONTENTS",   Cardinality.OneToMany, "item", "contents").setCascadingDelete(Cascade.sourceToTarget);
		type.relate(enclosure, "FEED_ITEM_ENCLOSURES", Cardinality.OneToMany, "item", "enclosures").setCascadingDelete(Cascade.sourceToTarget);

		// view configuration
		type.addViewProperty(PropertyView.Public, "enclosures");
		type.addViewProperty(PropertyView.Public, "contents");
		type.addViewProperty(PropertyView.Public, "owner");
		type.addViewProperty(PropertyView.Public, "name");

		type.addViewProperty(PropertyView.Ui, "enclosures");
		type.addViewProperty(PropertyView.Ui, "contents");
		type.addViewProperty(PropertyView.Ui, "feed");
	}}

	String getUrl();

	static void increaseVersion(final FeedItem thisItem) throws FrameworkException {

		final PropertyKey<Integer> versionKey = StructrApp.key(FeedItem.class, "version");
		final Integer _version = thisItem.getProperty(versionKey);

		thisItem.unlockSystemPropertiesOnce();
		if (_version == null) {

			thisItem.setProperty(versionKey, 1);

		} else {

			thisItem.setProperty(versionKey, _version + 1);
		}
	}

	static InputStream getInputStream(final FeedItem thisItem) {

		final boolean indexRemoteDocument = Settings.FeedItemIndexRemoteDocument.getValue();

		if (indexRemoteDocument) {

			final String remoteUrl = thisItem.getUrl();
			if (StringUtils.isNotBlank(remoteUrl)) {

				final Map<String, Object> responseData =  HttpHelper.getAsStream(remoteUrl);
				if (responseData != null && responseData.containsKey(HttpHelper.FIELD_BODY) && responseData.get(HttpHelper.FIELD_BODY) instanceof InputStream) {

					return (InputStream) responseData.get(HttpHelper.FIELD_BODY);
				}
			}
		}

		final String description = thisItem.getProperty(StructrApp.key(FeedItem.class, "description"));
		return new ByteArrayInputStream(description.getBytes());
	}

	@Override
	default boolean indexingEnabled() {
		return Settings.FeedItemIndexingEnabled.getValue();
	}

	@Override
	default Integer maximumIndexedWords() {
		return Settings.FeedItemIndexingLimit.getValue();
	}

	@Override
	default Integer indexedWordMinLength() {
		return Settings.FeedItemIndexingMinLength.getValue();
	}

	@Override
	default Integer indexedWordMaxLength() {
		return Settings.FeedItemIndexingMaxLength.getValue();
	}
}
