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
package org.structr.test.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.*;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.Tx;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class IndexManagementTest extends StructrTest {

	private static final Logger logger                               = LoggerFactory.getLogger(IndexManagementTest.class);
	private static final Set<String> INDEXED_RELATIONSHIP_PROPERTIES = Set.of("test");
	private static final long INDEX_UPDATE_TIMEOUT                   = TimeUnit.MINUTES.toMillis(5);
	private static final long INDEX_UPDATE_WAIT_TIME                 = TimeUnit.SECONDS.toMillis(10);

	@Test
	public void testIndexCreationAndRemovalForNonStringNodePropertyWithIndexedFlag() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			final String expectedRangeIndexType = Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.RangeIndexes) ? "RANGE" : "BTREE";

			{
				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.addType("Customer");

					customer.addIntegerProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, expectedRangeIndexType, "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.getType("Customer");

					for (Iterator<JsonProperty> it = customer.getProperties().iterator(); it.hasNext();) {

						final JsonProperty prop = it.next();
						if ("test".equals(prop.getName())) {

							prop.setIndexed(false);
						}
					}

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "Customer", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexUpdateAfterPropertyTypeChange() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			final String expectedRangeIndexType = Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.RangeIndexes) ? "RANGE" : "BTREE";

			{
				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.addType("Customer");

					customer.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, "TEXT", "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.getType("Customer");
					JsonProperty toRemove = null;

					for (final JsonProperty prop : customer.getProperties()) {

						if ("test".equals(prop.getName())) {

							toRemove = prop;
						}
					}

					if (toRemove != null) {

						customer.getProperties().remove(toRemove);
					}

					customer.addIntegerProperty("test").setIndexed(true).setUnique(true);

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, expectedRangeIndexType, "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForNodePropertyWithIndexedFlag() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			{
				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.addType("Customer");

					customer.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, "TEXT", "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.getType("Customer");

					for (Iterator<JsonProperty> it = customer.getProperties().iterator(); it.hasNext();) {

						final JsonProperty prop = it.next();
						if ("test".equals(prop.getName())) {

							prop.setIndexed(false);
						}
					}

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "Customer", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForNodeProperty() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			{

				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.addType("Customer");

					customer.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, "TEXT", "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.getType("Customer");

					for (Iterator<JsonProperty> it = customer.getProperties().iterator(); it.hasNext();) {

						final JsonProperty prop = it.next();
						if ("test".equals(prop.getName())) {

							it.remove();
						}
					}

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "Customer", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForNode() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			{

				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType customer       = sourceSchema.addType("Customer");

					customer.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, true, false, "TEXT", "Customer", Set.of("test"), 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema schema = StructrSchema.createFromDatabase(app);

					schema.removeType("Customer");

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				// Note: we KNOW that the index will not be removed, so we deliberately test the
				// wrong thing here in case it changes somehow in the future!
				while (!hasNumberOfIndexes(db, "Customer", 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationshipPropertyWithIndexedFlag() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			{

				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonObjectType customer = sourceSchema.addType("Customer");
					final JsonObjectType project  = sourceSchema.addType("Project");
					final JsonReferenceType rel   = customer.relate(project, "HAS_PROJECT", Cardinality.OneToMany, "customer", "projects");

					rel.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, false, true, "TEXT", "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType relationship   = sourceSchema.getType("CustomerHAS_PROJECTProject");

					for (Iterator<JsonProperty> it = relationship.getProperties().iterator(); it.hasNext();) {

						final JsonProperty prop = it.next();
						if ("test".equals(prop.getName())) {

							prop.setIndexed(false);
						}
					}

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "HAS_PROJECT", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationshipProperty() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			{

				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonObjectType customer = sourceSchema.addType("Customer");
					final JsonObjectType project  = sourceSchema.addType("Project");
					final JsonReferenceType rel   = customer.relate(project, "HAS_PROJECT", Cardinality.OneToMany, "customer", "projects");

					rel.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, false, true, "TEXT", "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonType relationship   = sourceSchema.getType("CustomerHAS_PROJECTProject");

					for (Iterator<JsonProperty> it = relationship.getProperties().iterator(); it.hasNext();) {

						final JsonProperty prop = it.next();
						if ("test".equals(prop.getName())) {

							it.remove();
						}
					}

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "HAS_PROJECT", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index update!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationship() {

		final DatabaseService db = app.getDatabaseService();
		long start               = 0;

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			{

				Services.enableIndexConfiguration();

				// This test creates a custom type with an indexed property and verifies index creation and removal.

				// setup 1: add type with indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
					final JsonObjectType customer = sourceSchema.addType("Customer");
					final JsonObjectType project  = sourceSchema.addType("Project");
					final JsonReferenceType rel   = customer.relate(project, "HAS_PROJECT", Cardinality.OneToMany, "customer", "projects");

					rel.addStringProperty("test").setIndexed(true).setRequired(true).setUnique(true);

					// apply schema changes
					StructrSchema.extendDatabaseSchema(app, sourceSchema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!indexCreatedSuccessfully(db, false, true, "TEXT", "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 1)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index creation!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}

				// setup 2: remove indexed property
				try (final Tx tx = app.tx()) {

					final JsonSchema schema = StructrSchema.createFromDatabase(app);

					schema.removeType("CustomerHAS_PROJECTProject");

					// apply schema changes
					StructrSchema.replaceDatabaseSchema(app, schema);

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}

				start = System.currentTimeMillis();

				while (!hasNumberOfIndexes(db, "HAS_PROJECT", 0)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {

						fail("Timeout waiting for index removal!");
					}

					// wait for index to be created
					try { Thread.sleep(INDEX_UPDATE_WAIT_TIME); } catch (Throwable t) {}
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}

	// ----- private methods -----
	@Test
	public void testIndexUpdateKeepsVectorIndexes() {

		final DatabaseService db = app.getDatabaseService();

		// only run this test if the database has vector indexes (Neo4j 5.13 and later)
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.VectorIndexes)) {

			final String index = "VectorIndexTest_embedding";

			try {

				// created outside of Structr's schema, as a module like the AI module does
				try (final Tx tx = app.tx()) {

					app.query("CREATE VECTOR INDEX " + index + " IF NOT EXISTS FOR (n:VectorIndexTest) ON n.embedding OPTIONS { indexConfig: { `vector.dimensions`: 2, `vector.similarity_function`: 'cosine' } }", Map.of());
					tx.success();
				}

				// an index update drops every index it lists that is not in the schema, here all of them
				db.updateIndexConfiguration(List.of());

				try (final Tx tx = app.tx()) {

					assertTrue("Structr's index update must not drop VECTOR indexes", app.query("SHOW VECTOR INDEXES YIELD name WHERE name = $name RETURN name", Map.of("name", index)).iterator().hasNext());
					tx.success();
				}

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception.");

			} finally {

				try (final Tx tx = app.tx()) {

					app.query("DROP INDEX " + index + " IF EXISTS", Map.of());
					tx.success();

				} catch (FrameworkException fex) {

					logger.warn("Unable to drop test index {}: {}", index, fex.getMessage());
				}
			}

		} else {

			System.out.println("Skipping test because vector indexes are not supported.");
		}
	}

	@Test
	public void testSchemaVectorIndexIsManagedAndQueried() {

		final DatabaseService db = app.getDatabaseService();

		// only run this test if the database has vector indexes (Neo4j 5.13 and later)
		if (!db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") || !db.supportsFeature(DatabaseFeature.VectorIndexes)) {

			System.out.println("Skipping test because vector indexes are not supported.");
			return;
		}

		final String index  = "VectorDoc_embedding_vector";
		final String custom = "VectorDoc_custom";

		Services.enableIndexConfiguration();

		try {

			// a vector index created by hand must survive Structr's index management
			try (final Tx tx = app.tx()) {

				app.query("CREATE VECTOR INDEX " + custom + " IF NOT EXISTS FOR (n:VectorDoc) ON n.other OPTIONS { indexConfig: { `vector.dimensions`: 2, `vector.similarity_function`: 'cosine' } }", Map.of());
				tx.success();
			}

			setVectorSize(3);
			awaitVectorIndex(db, index, 3);

			// three documents and a count
			try (final Tx tx = app.tx()) {

				final PropertyKey<Double[]> embedding = Traits.of("VectorDoc").key("embedding");
				final PropertyKey<String> name        = Traits.of("VectorDoc").key("name");

				app.create("VectorDoc", new NodeAttribute<>(name, "x"), new NodeAttribute<>(embedding, new Double[] { 1.0, 0.0, 0.0 }));
				app.create("VectorDoc", new NodeAttribute<>(name, "y"), new NodeAttribute<>(embedding, new Double[] { 0.0, 1.0, 0.0 }));
				app.create("VectorDoc", new NodeAttribute<>(name, "xy"), new NodeAttribute<>(embedding, new Double[] { 0.7, 0.7, 0.0 }));
				app.create("VectorDoc", new NodeAttribute<>(name, "none"));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				app.query("CALL db.awaitIndexes(60)", Map.of());
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final List<String> names = new ArrayList<>();

				// the index is shared by all tenants and filtered afterwards, so ask for more than the three of this test
				for (final NodeInterface node : app.getNodesFromVectorIndex(index, 100, new double[] { 0.9, 0.1, 0.0 }).keySet()) {
					names.add(node.getName());
				}

				assertEquals("nearest neighbours, best first", List.of("x", "xy", "y"), names);

				final PropertyKey<Double[]> embedding = Traits.of("VectorDoc").key("embedding");

				assertEquals(4, app.nodeQuery("VectorDoc").count());
				assertEquals(3, app.nodeQuery("VectorDoc").notBlank(embedding).count());
				assertEquals(1, app.nodeQuery("VectorDoc").blank(embedding).count());

				tx.success();
			}

			// a new size creates the index again
			setVectorSize(4);
			awaitVectorIndex(db, index, 4);

			// and so does a new similarity function
			setVectorIndex(4, "euclidean");
			awaitVectorIndex(db, index, 4, "euclidean");

			try (final Tx tx = app.tx()) {

				assertFalse("a hand-made vector index must not be dropped", db.getIndexInfo(custom).isEmpty());
				tx.success();
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			try (final Tx tx = app.tx()) {

				app.query("DROP INDEX " + custom + " IF EXISTS", Map.of());
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("Unable to drop test index {}: {}", custom, fex.getMessage());
			}
		}
	}

	private void setVectorSize(final int dimensions) throws FrameworkException {

		setVectorIndex(dimensions, "cosine");
	}

	private void setVectorIndex(final int dimensions, final String similarity) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType("VectorDoc") != null ? schema.getType("VectorDoc") : schema.addType("VectorDoc");
			JsonDoubleArrayProperty embedding = null;

			for (final JsonProperty property : type.getProperties()) {

				if ("embedding".equals(property.getName())) {
					embedding = (JsonDoubleArrayProperty) property;
				}
			}

			if (embedding == null) {
				embedding = type.addDoubleArrayProperty("embedding");
			}

			embedding.setVectorIndex(dimensions, similarity);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}
	}

	private void awaitVectorIndex(final DatabaseService db, final String index, final int dimensions) {

		awaitVectorIndex(db, index, dimensions, "cosine");
	}

	private void awaitVectorIndex(final DatabaseService db, final String index, final int dimensions, final String similarity) {

		final long start = System.currentTimeMillis();

		while (true) {

			try (final Tx tx = app.tx()) {

				final Map<String, Object> info = db.getIndexInfo(index);

				tx.success();

				if (info.get("dimensions") instanceof Number number && number.intValue() == dimensions && similarity.equalsIgnoreCase(String.valueOf(info.get("similarity"))) && "ONLINE".equals(info.get("state"))) {
					return;
				}

			} catch (FrameworkException fex) {
				logger.warn("Unable to read index {}: {}", index, fex.getMessage());
			}

			if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {
				fail("Timeout waiting for vector index " + index + " with " + dimensions + " dimensions and " + similarity + " similarity.");
			}

			try { Thread.sleep(1000); } catch (Throwable t) {}
		}
	}

	private boolean indexCreatedSuccessfully(final DatabaseService db, final boolean isNode, final boolean isRelationship, final String indexType, final String entityType, final Set<String> propertyNames, final int expectedEntryCount) {

		logger.info("Waiting for index update..");

		try (final Tx tx = app.tx()) {

			final List<IndexInfo> infos = queryIndexes(db, entityType);

			logger.info("Index query result: {}", infos);

			if (!infos.isEmpty()) {

				if (infos.size() != expectedEntryCount) {

					logger.info("Returning false because infos.size() was {} while expected count was {}", infos.size(), expectedEntryCount);

					return false;
				}

				final IndexInfo first = infos.get(0);
				if (isNode && !first.isNode()) {

					logger.info("Returning false because first entry is not a node index and a node index was expected.");

					return false;
				}

				if (isRelationship && !first.isRelationship()) {

					logger.info("Returning false because first entry is not a relationship index and a relationship index was expected.");

					return false;
				}

				if (!indexType.equals(first.getIndexType())) {

					logger.info("Returning false because returned index type was {} while expected index type was {}", first.getIndexType(), indexType);

					return false;
				}

				if (!entityType.equals(first.getEntityType())) {

					logger.info("Returning false because returned entity type was {} while expected type was {}", first.getEntityType(), entityType);

					return false;
				}

				if (!propertyNames.containsAll(first.getProperties())) {

					logger.info("Returning false because expected properties did not contain {}.", first.getProperties());

					return false;
				}

				// important, this is the only place where the test passes
				logger.info("Returning true, check successful.");

				return true;

			} else if (expectedEntryCount == 0) {

				// expected count matches empty result
				logger.info("Returning true, expected 0 entries and got 0 entries from index query.");

				return true;
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		logger.info("Returning false at the end of the check method.");

		return false;
	}

	private boolean hasNumberOfIndexes(final DatabaseService db, final String entityType, final int expectedNumberOfIndexes) {

		logger.info("Waiting for index update..");

		try (final Tx tx = app.tx()) {

			final List<IndexInfo> infos = queryIndexes(db, entityType);

			for (final IndexInfo info : infos) {

				System.out.println(info.type + ", " + info.types + ", " + info.props);
			}

			tx.success();

			final boolean result = infos.size() == expectedNumberOfIndexes;

			logger.info("Index query result: {}, expected number of indexes was {}, returning {}", infos, expectedNumberOfIndexes, result );

			return result;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		logger.info("Returning false at the end of the check method.");

		return false;
	}

	private List<IndexInfo> queryIndexes(final DatabaseService<?> db, final String labelOrType) {

		// Neo4j 5.x
		if (db.supportsFeature(DatabaseFeature.ShowIndexesQuery)) {

			final String query = "SHOW INDEXES YIELD type, properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"" + labelOrType + "\"] RETURN name, type, entityType, labelsOrTypes, properties";
			final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
			final Iterable<Map<String, Object>> result = db.execute(nativeQuery);

			return Iterables.toList(Iterables.map(r -> new Neo4IndexInfo(r), result));
		}

		// Neo4j 4.x
		if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

			final String query = "CALL db.indexes() YIELD type, properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"" + labelOrType + "\"] RETURN name, type, entityType, labelsOrTypes, properties";
			final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
			final Iterable<Map<String, Object>> result = db.execute(nativeQuery);

			return Iterables.toList(Iterables.map(r -> new Neo4IndexInfo(r), result));
		}

		// Neo4k 3.x
		final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"" + labelOrType + "\"] RETURN tokenNames, properties, type ORDER BY description";
		final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
		final Iterable<Map<String, Object>> result = db.execute(nativeQuery);

		return Iterables.toList(Iterables.map(r -> new Neo3IndexInfo(r), result));
	}

	private static abstract class IndexInfo {

		protected String type        = null;
		protected String indexType   = null;
		protected List<String> types = null;
		protected List<String> props = null;

		@Override
		public String toString() {

			return getClass().getSimpleName() + "(" + type + ", " + props + ")";
		}

		public boolean isNode() {

			return "NODE".equals(this.type);
		}

		public boolean isRelationship() {

			return "RELATIONSHIP".equals(this.type);
		}

		public String getEntityType() {

			if (types == null ||  types.isEmpty()) {

				return null;
			}

			return types.get(0);
		}

		public String getIndexType() {

			return indexType;
		}

		public String getPropertyName() {

			if (props == null || props.isEmpty()) {

				return null;
			}

			return this.props.get(0);
		}

		public List<String> getProperties() {

			return this.props;
		}
	}

	private static class Neo4IndexInfo extends IndexInfo {

		public Neo4IndexInfo(final Map<String, Object> data) {

			this.indexType = (String)data.get("type");
			this.type      = (String)data.get("entityType");
			this.types     = (List<String>)data.get("labelsOrTypes");
			this.props     = (List<String>)data.get("properties");
		}
	}

	private static class Neo3IndexInfo extends IndexInfo {

		public Neo3IndexInfo(final Map<String, Object> data) {

			this.type  = (String)data.get("type");
			this.types = (List<String>)data.get("tokenNames");
			this.props = (List<String>)data.get("properties");

			if ("node_label_property".equals(this.type)) {

				this.type = "NODE";
			}
		}
	}
}