/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test;

import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.IndexingTest;
import org.structr.web.common.FileHelper;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class FulltextIndexingTest extends IndexingTest {

	@Test
	public void testBasicFulltextSearchOnNodes() {

		// create fulltext indexed property
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addStringProperty("test").setIndexed(true, true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		waitForIndex(60);

		// create some data
		try (final Tx tx = app.tx()) {

			final PropertyKey<String> key = Traits.of("Test").key("test");

			app.create("Test", "Test1").setProperty(key, "one two three four five");
			app.create("Test", "Test2").setProperty(key, "two three four six");
			app.create("Test", "Test3").setProperty(key, "four three one eight");
			app.create("Test", "Test4").setProperty(key, "five six seven");
			app.create("Test", "Test5").setProperty(key, "eight");

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final String indexName                  = "Test_test";
			final String searchString               = "eight";
			final Map<NodeInterface, Double> result = app.getNodesFromFulltextIndex(indexName, searchString, 10, 1);
			final List<NodeInterface> list          = new LinkedList<>(result.keySet());

			final NodeInterface node1               = list.get(0);
			final NodeInterface node2               = list.get(1);

			assertEquals("Wrong fulltext index query result", "Test5", node1.getName());
			assertEquals("Wrong fulltext index query result", "Test3", node2.getName());

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			assertEquals("Wrong fulltext index query result in $.searchFulltext() function", "Test5", Scripting.replaceVariables(new ActionContext(securityContext), null, "${{ $.searchFulltext('Test_test', 'eight')[0].node.name; }}"));

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testBasicFulltextSearchOnRelationships() {

		// create fulltext indexed property
		try (final Tx tx = app.tx()) {

			final JsonSchema schema     = StructrSchema.createFromDatabase(app);
			final JsonObjectType type1  = schema.addType("Test1");
			final JsonObjectType type2  = schema.addType("Test2");
			final JsonReferenceType rel = type1.relate(type2, "TEST", Cardinality.OneToMany, "parent", "children");

			rel.addStringProperty("test").setIndexed(true, true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		Settings.CypherDebugLogging.setValue(true);

		waitForIndex(60);

		final PropertyKey<Iterable<NodeInterface>> relationshipKey = Traits.of("Test1").key("children");
		final PropertyKey<String> key                              = Traits.of("Test1TESTTest2").key("test");

		// create some data
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.create("Test1");

			final NodeInterface test21 = app.create("Test2");
			final NodeInterface test22 = app.create("Test2");
			final NodeInterface test23 = app.create("Test2");
			final NodeInterface test24 = app.create("Test2");
			final NodeInterface test25 = app.create("Test2");

			final List<RelationshipInterface> rels = Iterables.toList((Iterable) test1.setProperty(relationshipKey, List.of(test21, test22, test23, test24, test25)));

			rels.get(0).setProperty(key, "one two three four five");
			rels.get(1).setProperty(key, "two three four six");
			rels.get(2).setProperty(key, "four three one eight");
			rels.get(3).setProperty(key, "five six seven");
			rels.get(4).setProperty(key, "eight");

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final String indexName                          = "TEST_test";
			final String searchString                       = "eight";
			final Map<RelationshipInterface, Double> result = app.getRelationshipsFromFulltextIndex(indexName, searchString, 10, 1);
			final List<RelationshipInterface> list          = new LinkedList<>(result.keySet());
			final RelationshipInterface rel1                = list.get(0);
			final RelationshipInterface rel2                = list.get(1);

			System.out.println(list);

			assertEquals("Wrong fulltext index query result", "eight",                rel1.getProperty(key));
			assertEquals("Wrong fulltext index query result", "four three one eight", rel2.getProperty(key));

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			assertEquals("Wrong fulltext index query result in $.searchRelationshipsFulltext() function", "eight", Scripting.replaceVariables(new ActionContext(securityContext), null, "${{ $.searchRelationshipsFulltext('TEST_test', 'eight')[0].relationship.test; }}"));

			tx.success();

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testODTSearch() {

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.odt").getUuid();
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testODT() {

		try (final Tx tx = app.tx()) {

			try( final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.odt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.odt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPDF() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.pdf")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.pdf");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPlaintext01() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.txt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("sit");
	}

	@Test
	public void testPlaintext02() {

		try (final Tx tx = app.tx()) {

			try(final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test2.txt")) {
				FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "test2.txt");
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		waitForIndex();
		testFile("ignoring");
	}

	// ----- private methods -----
	private void testFile(final String searchText) {

		final PropertyKey key = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.EXTRACTED_CONTENT_PROPERTY);
		final long timeout    = System.currentTimeMillis() + 30000;
		String value = null;

		// wait for value (indexer is async)
		while (value == null) {

			// test result
			try (final Tx tx = app.tx()) {

				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).getFirst();

				assertNotNull("File should exist", file);

				value = (String) file.getProperty(key);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			if (value == null) {

				try {
					Thread.sleep(1000);
				} catch (Throwable t) {
				}

				if (System.currentTimeMillis() > timeout) {
					throw new RuntimeException("Timeout waiting for indexer to write content into property value.");
				}
			}
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result = app.nodeQuery(StructrTraits.FILE).key(key, searchText, false).getAsList();

			assertEquals("Invalid index query result size", 1, result.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Map<NodeInterface, Double> result = app.getNodesFromFulltextIndex("File_extractedContent", searchText, 10, 1);

			assertEquals("Invalid index query result size", 1, result.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	private void waitForIndex() {
		waitForIndex(60);
	}

	private void waitForIndex(final long seconds) {

		// wait for one minute maximum
		final long timeoutTimestamp = System.currentTimeMillis() + (seconds * 1000);

		while (System.currentTimeMillis() < timeoutTimestamp) {

			if (StructrApp.getInstance().getDatabaseService().isIndexUpdateFinished()) {

				try { Thread.sleep(10000); } catch (Throwable t) {}

				return;
			}

			try { Thread.sleep(1000); } catch (Throwable t) {}
		}

		throw new RuntimeException("Timeout waiting for index update.");
	}
}

