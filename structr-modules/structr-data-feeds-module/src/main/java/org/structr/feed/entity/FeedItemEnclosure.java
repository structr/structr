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
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.fulltext.Indexable;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Represents feed enclosures
 */
public interface FeedItemEnclosure extends NodeInterface, Indexable {

	static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType type      = schema.addType("FeedItemEnclosure");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/FeedItemEnclosure"));
		type.setImplements(URI.create("#/definitions/Indexable"));

 		type.addStringProperty("url",           PropertyView.Public, PropertyView.Ui);
 		type.addLongProperty("enclosureLength", PropertyView.Public, PropertyView.Ui);
 		type.addStringProperty("enclosureType", PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("url",              String.class);
		type.addPropertyGetter("contentType",      String.class);
		type.addPropertyGetter("extractedContent", String.class);

		// methods shared with FeedItemContent
		type.overrideMethod("afterCreation",    false,             FeedItemContent.class.getName() + ".updateIndex(this, arg0);");
		type.overrideMethod("getSearchContext", false, "return " + FeedItemContent.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);").setDoExport(true);

		type.overrideMethod("getInputStream",   false, "return " + FeedItemEnclosure.class.getName() + ".getInputStream(this);");

		// view configuration
		type.addViewProperty(PropertyView.Public, "item");
		type.addViewProperty(PropertyView.Public, "owner");

		type.addViewProperty(PropertyView.Ui, "item");
	}}

	String getUrl();

	static InputStream getInputStream(final FeedItemEnclosure thisItem) {
		return IOUtils.toInputStream(thisItem.getUrl(), Charset.forName("utf-8"));
	}

	@Override
	default boolean indexingEnabled() {
		return Settings.FeedItemEnclosureIndexingEnabled.getValue();
	}

	@Override
	default Integer maximumIndexedWords() {
		return Settings.FeedItemEnclosureIndexingLimit.getValue();
	}

	@Override
	default Integer indexedWordMinLength() {
		return Settings.FeedItemEnclosureIndexingMinLength.getValue();
	}

	@Override
	default Integer indexedWordMaxLength() {
		return Settings.FeedItemEnclosureIndexingMaxLength.getValue();
	}
}
