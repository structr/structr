/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.*;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class IndexManagementTest extends StructrTest {

	private static final Set<String> INDEXED_RELATIONSHIP_PROPERTIES = Set.of("sourceId", "targetId", "test", "lastModifiedDate", "visibleToAuthenticatedUsers", "relType", "visibleToPublicUsers", "internalTimestamp", "type", "createdDate", "id");
	private static final int INDEX_CREATION_WAIT_TIME                = 60000;
	private static final int INDEX_DELETION_WAIT_TIME                = 180000;

	@Test
	public void testIndexCreationAndRemovalForNodePropertyWithIndexedFlag() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "NODE",     map.get("entityType"));
							assertEquals("Created index has wrong label",    "Customer", ((List)map.get("labelsOrTypes")).get(0));
							assertEquals("Created index has wrong property", "test",     ((List)map.get("properties")).get(0));
						}

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "node_label_property", map.get("type"));
							assertEquals("Created index has wrong label",    "Customer",            ((List)map.get("tokenNames")).get(0));
							assertEquals("Created index has wrong property", "test",                ((List)map.get("properties")).get(0));
						}

					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						assertEquals("Index was not removed", 0, resultList.size());

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						assertEquals("Index was not removed", 0, resultList.size());
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForNodeProperty() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "NODE",     map.get("entityType"));
							assertEquals("Created index has wrong label",    "Customer", ((List)map.get("labelsOrTypes")).get(0));
							assertEquals("Created index has wrong property", "test",     ((List)map.get("properties")).get(0));
						}

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "node_label_property", map.get("type"));
							assertEquals("Created index has wrong label",    "Customer",            ((List)map.get("tokenNames")).get(0));
							assertEquals("Created index has wrong property", "test",                ((List)map.get("properties")).get(0));
						}
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						// Note: we KNOW that the index will not be removed, so we deliberately test the
						// wrong thing here in case it changes somehow in the future!
						assertEquals("Index was removed, which is not expected", 1, resultList.size());

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						// Note: we KNOW that the index will not be removed, so we deliberately test the
						// wrong thing here in case it changes somehow in the future!
						assertEquals("Index was removed, which is not expected", 1, resultList.size());
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForNode() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher")) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "NODE",     map.get("entityType"));
							assertEquals("Created index has wrong label",    "Customer", ((List)map.get("labelsOrTypes")).get(0));
							assertEquals("Created index has wrong property", "test",     ((List)map.get("properties")).get(0));
						}

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						assertEquals("Index was not created", 1, resultList.size());

						for (final Map<String, Object> map : resultList) {

							assertEquals("Created index has wrong type",     "node_label_property", map.get("type"));
							assertEquals("Created index has wrong label",    "Customer",            ((List)map.get("tokenNames")).get(0));
							assertEquals("Created index has wrong property", "test",                ((List)map.get("properties")).get(0));
						}
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

						final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"Customer\"] RETURN name, entityType, labelsOrTypes, properties";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						System.out.println(gson.toJson(resultList));

						assertEquals("Index was not removed", 0, resultList.size());

					} else {

						final String query = "CALL db.indexes() YIELD description, tokenNames, properties, state, type WHERE tokenNames = [\"Customer\"] RETURN tokenNames, properties, type ORDER BY description";
						final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
						final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
						final List<Map<String, Object>> resultList = Iterables.toList(result);

						assertEquals("Index was not removed", 0, resultList.size());
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher is not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationshipPropertyWithIndexedFlag() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					System.out.println(gson.toJson(resultList));

					assertEquals("Indexes were not created", 11, resultList.size());

					for (final Map<String, Object> map : resultList) {

						assertEquals("Created index has wrong type",     "RELATIONSHIP", map.get("entityType"));
						assertEquals("Created index has wrong label",    "HAS_PROJECT",  ((List)map.get("labelsOrTypes")).get(0));

						assertTrue("Created index has wrong property", INDEXED_RELATIONSHIP_PROPERTIES.containsAll(((List)map.get("properties"))));
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					System.out.println(gson.toJson(resultList));

					assertEquals("Index was not removed", 10, resultList.size());

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationshipProperty() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					System.out.println(gson.toJson(resultList));

					assertEquals("Indexes were not created", 11, resultList.size());

					for (final Map<String, Object> map : resultList) {


						assertEquals("Created index has wrong type",     "RELATIONSHIP", map.get("entityType"));
						assertEquals("Created index has wrong label",    "HAS_PROJECT",  ((List)map.get("labelsOrTypes")).get(0));

						assertTrue("Created index has wrong property", INDEXED_RELATIONSHIP_PROPERTIES.containsAll(((List)map.get("properties"))));
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					// Note: we KNOW that the index will not be removed, so we deliberately test the
					// wrong thing here in case it changes somehow in the future!
					assertEquals("Indexes were removed, which is not expected", 11, resultList.size());

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}

	@Test
	public void testIndexCreationAndRemovalForRelationship() {

		final DatabaseService db = app.getDatabaseService();
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// only run this test if Cypher is supported
		if (db.supportsFeature(DatabaseFeature.QueryLanguage, "text/cypher") && db.supportsFeature(DatabaseFeature.RelationshipIndexes)) {

			this.cleanDatabaseAndSchema();

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

				// wait for index to be created
				try { Thread.sleep(INDEX_CREATION_WAIT_TIME); } catch (Throwable t) {}

				// check if index exists
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					System.out.println(gson.toJson(resultList));

					assertEquals("Indexes were not created", 11, resultList.size());

					for (final Map<String, Object> map : resultList) {

						assertEquals("Created index has wrong type",     "RELATIONSHIP", map.get("entityType"));
						assertEquals("Created index has wrong label",    "HAS_PROJECT",  ((List)map.get("labelsOrTypes")).get(0));

						assertTrue("Created index has wrong property", INDEXED_RELATIONSHIP_PROPERTIES.containsAll(((List)map.get("properties"))));
					}

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
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

				// wait for index to be updated
				try { Thread.sleep(INDEX_DELETION_WAIT_TIME); } catch (Throwable t) {}

				// check that index doesn't exist any more
				try (final Tx tx = app.tx()) {

					final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"HAS_PROJECT\"] RETURN name, entityType, labelsOrTypes, properties";
					final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
					final Iterable<Map<String, Object>> result = db.execute(nativeQuery);
					final List<Map<String, Object>> resultList = Iterables.toList(result);

					assertEquals("Indexes were not removed", 0, resultList.size());

					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace();
					fail("Unexpected exception");
				}
			}

		} else {

			System.out.println("Skipping test because Cypher or relationship indexes are not supported.");
		}
	}
}
