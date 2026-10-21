/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.extraction;

import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.core.Services;
import org.structr.core.graph.Tx;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.*;

/**
 * The fulltext index of the content, created by Structr's index management, which is off in other tests.
 */
public class ContentIndexTest extends StructrUiTest {

	private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);

	@Test
	public void testTextAndEmbeddedPathShareOneFulltextIndex() throws Exception {

		if (!app.getDatabaseService().supportsFeature(DatabaseFeature.ShowIndexesQuery)) {

			throw new SkipException("Needs a database with SHOW INDEXES");
		}

		// an index of the same name on fewer properties, as left by an earlier version, must be replaced;
		// indexes are not separated by tenant, so one from an earlier test run is removed first
		try (final Tx tx = app.tx()) {

			app.query("DROP INDEX `" + ContentExtractionTraits.CONTENT_FULLTEXT_INDEX + "` IF EXISTS", Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.query("CREATE FULLTEXT INDEX `" + ContentExtractionTraits.CONTENT_FULLTEXT_INDEX + "` FOR (n:ExtractedContentNode) ON EACH [n.text]", Map.of());
			tx.success();
		}

		Services.enableIndexConfiguration();

		try {

			// a schema change runs the index update; the new fulltext property must keep the name it always had
			try (final Tx tx = app.tx()) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app);
				final JsonType type     = schema.addType("Memo");

				type.addStringProperty("body").setIndexed(false, true);

				StructrSchema.extendDatabaseSchema(app, schema);
				tx.success();
			}

			awaitIndex(ContentExtractionTraits.CONTENT_FULLTEXT_INDEX, List.of("embeddedPath", "text"));
			awaitIndex("Memo_body_fulltext", List.of("body"));

		} finally {

			Services.disableIndexConfiguration();
		}
	}

	// ----- private methods -----
	private void awaitIndex(final String name, final List<String> properties) throws Exception {

		final long start = System.currentTimeMillis();
		List<String> found = null;

		while (System.currentTimeMillis() < start + TIMEOUT) {

			found = indexProperties(name);
			if (properties.equals(found)) {

				return;
			}

			Thread.sleep(1000);
		}

		fail("Index " + name + " has properties " + found + ", expected " + properties);
	}

	private List<String> indexProperties(final String name) throws Exception {

		final DatabaseService<?> db = app.getDatabaseService();

		try (final Tx tx = app.tx()) {

			final NativeQuery<Iterable> query          = db.query("SHOW INDEXES YIELD name, type, state, properties WHERE name = '" + name + "' AND type = 'FULLTEXT' AND state = 'ONLINE' RETURN properties", Iterable.class);
			final Iterable<Map<String, Object>> result = db.execute(query);
			List<String> properties                    = null;

			for (final Map<String, Object> row : result) {

				properties = ((List<?>) row.get("properties")).stream().map(String::valueOf).sorted().toList();
			}

			tx.success();

			return properties;
		}
	}
}
