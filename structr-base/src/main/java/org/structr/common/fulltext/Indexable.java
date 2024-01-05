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

import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.io.InputStream;
import java.net.URI;

/**
 */
public interface Indexable extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Indexable");
		final JsonObjectType word = schema.addType("IndexedWord");

		type.setIsInterface();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Indexable"));

		final JsonReferenceType rel = type.relate(word, "INDEXED_WORD", Cardinality.ManyToMany, "indexables", "words").setCascadingCreate(JsonSchema.Cascade.sourceToTarget);

		type.addReferenceProperty("indexedWords", rel.getTargetProperty()).setProperties("name", "true");

		type.addStringProperty("contentType",       PropertyView.Ui, PropertyView.Public);
		type.addStringProperty("extractedContent");
	}}

	String getContentType();
	String getExtractedContent();
	InputStream getInputStream();

	@Export
	GraphObject getSearchContext(final SecurityContext ctx, final String searchTerm, final int contextLength);

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
