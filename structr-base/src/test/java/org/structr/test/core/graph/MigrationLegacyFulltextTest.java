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
package org.structr.test.core.graph;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * The migration that removes what the removed text-search module left behind: File.extractedContent,
 * File.indexed, Folder.mountDoFulltextIndexing and the fulltext index on File.extractedContent.
 */
public class MigrationLegacyFulltextTest extends StructrTest {

	private static final String INDEX = "File_extractedContent_fulltext";

	@Test
	public void testLegacyFulltextDataIsRemoved() throws FrameworkException {

		final String fileId;
		final String folderId;

		try (final Tx tx = app.tx()) {

			fileId   = app.create(StructrTraits.FILE, "legacy.txt").getUuid();
			folderId = app.create(StructrTraits.FOLDER, "mount").getUuid();

			tx.success();
		}

		// what the text-search module and the old mount dialog wrote; the properties no longer exist in the schema
		try (final Tx tx = app.tx()) {

			app.query("MATCH (n:File { id: $id }) SET n.extractedContent = 'the extracted text', n.indexed = true", Map.of("id", fileId));
			app.query("MATCH (n:Folder { id: $id }) SET n.mountDoFulltextIndexing = true", Map.of("id", folderId));

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.query("CREATE FULLTEXT INDEX " + INDEX + " IF NOT EXISTS FOR (n:File) ON EACH [n.extractedContent]", Map.of());

			tx.success();
		}

		// a dry run reports the pending removal and leaves everything in place
		assertTrue("The legacy data must be reported as pending", MigrationService.execute(true));
		assertEquals("[the extracted text, true]", fileProperties(fileId).toString());
		assertTrue(indexExists());

		MigrationService.execute(false);

		assertEquals("[null, null]", fileProperties(fileId).toString());
		assertNull(folderProperty(folderId));
		assertFalse("The legacy fulltext index must be dropped", indexExists());

		// nothing left to do
		assertFalse(MigrationService.execute(true));
	}

	// ----- private methods -----
	private List<Object> fileProperties(final String id) throws FrameworkException {

		final Map<String, Object> row = single("MATCH (n:File { id: $id }) RETURN { content: n.extractedContent, indexed: n.indexed } AS row", id);

		return Arrays.asList(row.get("content"), row.get("indexed"));
	}

	private Object folderProperty(final String id) throws FrameworkException {

		return single("MATCH (n:Folder { id: $id }) RETURN { value: n.mountDoFulltextIndexing } AS row", id).get("value");
	}

	private Map<String, Object> single(final String query, final String id) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = Iterables.toList(app.query(query, Map.of("id", id)));

			tx.success();

			assertEquals(1, result.size());

			// a map result comes back as a map nested under the column name
			final Object row = ((GraphObjectMap) result.get(0)).toMap().get("row");

			return row instanceof GraphObjectMap map ? map.toMap() : ((GraphObjectMap) result.get(0)).toMap();
		}
	}

	private boolean indexExists() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final boolean exists = app.query("SHOW INDEXES YIELD name WHERE name = $name RETURN name", Map.of("name", INDEX)).iterator().hasNext();

			tx.success();

			return exists;
		}
	}
}
