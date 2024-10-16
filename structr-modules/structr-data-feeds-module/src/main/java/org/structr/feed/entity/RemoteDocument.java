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
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.fulltext.Indexable;
import org.structr.core.graph.NodeInterface;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.SchemaService;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.structr.rest.common.HttpHelper.getAsStream;

/**
 *
 *
 */
public interface RemoteDocument extends NodeInterface, Indexable {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("RemoteDocument");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/RemoteDocument"));
		type.setImplements(URI.create("#/definitions/Indexable"));

		type.addStringProperty("url",              PropertyView.Ui, PropertyView.Public);
		type.addLongProperty("checksum",           PropertyView.Ui).setReadOnly(true);
		type.addIntegerProperty("cacheForSeconds", PropertyView.Ui);
		type.addIntegerProperty("version",         PropertyView.Ui).setReadOnly(true);

		type.addPropertyGetter("url",              String.class);
		type.addPropertyGetter("contentType",      String.class);
		type.addPropertyGetter("extractedContent", String.class);

		// methods shared with FeedItemContent
		type.overrideMethod("afterCreation",    false,             FeedItemContent.class.getName() + ".updateIndex(this, arg0);");
		type.overrideMethod("getSearchContext", false, "return " + FeedItemContent.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);").setDoExport(true);
		type.overrideMethod("getInputStream",   false, "return " + RemoteDocument.class.getName() + ".getInputStream(this);");

		// view configuration
		type.addViewProperty(PropertyView.Public, "owner");
		type.addViewProperty(PropertyView.Public, "name");
	}}

	String getUrl();

	static InputStream getInputStream(final RemoteDocument thisDocument) {

		final String remoteUrl = thisDocument.getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			final Map<String, Object> responseData =  HttpHelper.getAsStream(remoteUrl);
			if (responseData != null && responseData.containsKey(HttpHelper.FIELD_BODY) && responseData.get(HttpHelper.FIELD_BODY) instanceof InputStream) {

				return (InputStream) responseData.get(HttpHelper.FIELD_BODY);
			}
		}

		return null;
	}

	@Override
	default boolean indexingEnabled() {
		return Settings.RemoteDocumentIndexingEnabled.getValue();
	}

	@Override
	default Integer maximumIndexedWords() {
		return Settings.RemoteDocumentIndexingLimit.getValue();
	}

	@Override
	default Integer indexedWordMinLength() {
		return Settings.RemoteDocumentIndexingMinLength.getValue();
	}

	@Override
	default Integer indexedWordMaxLength() {
		return Settings.RemoteDocumentIndexingMaxLength.getValue();
	}
}
