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
package org.structr.test.schema;

import net.jcip.annotations.NotThreadSafe;
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
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.fail;

/**
 *
 */
@NotThreadSafe
public class IndexManagementTest extends StructrTest {

	private static final Logger logger                               = LoggerFactory.getLogger(IndexManagementTest.class);
	private static final Set<String> INDEXED_RELATIONSHIP_PROPERTIES = Set.of("test", "relType", "internalTimestamp", "type", "id");
	private static final long INDEX_UPDATE_TIMEOUT                   = TimeUnit.MINUTES.toMillis(10);
	private static final long INDEX_UPDATE_WAIT_TIME                 = TimeUnit.SECONDS.toMillis(10);

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

				while (!indexCreatedSuccessfully(db, true, false, "Customer", Set.of("test"), 1)) {

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

				while (!indexCreatedSuccessfully(db, true, false, "Customer", Set.of("test"), 1)) {

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

				while (!indexCreatedSuccessfully(db, true, false, "Customer", Set.of("test"), 1)) {

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

				while (!indexCreatedSuccessfully(db, false, true, "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 5)) {

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

				while (!hasNumberOfIndexes(db, "HAS_PROJECT", 4)) {

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

				while (!indexCreatedSuccessfully(db, false, true, "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 5)) {

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

				// Note: we KNOW that the index will not be removed, so we deliberately test the
				// wrong thing here in case it changes somehow in the future!
				while (!hasNumberOfIndexes(db, "HAS_PROJECT", 5)) {

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

				while (!indexCreatedSuccessfully(db, false, true, "HAS_PROJECT", INDEXED_RELATIONSHIP_PROPERTIES, 5)) {

					if (System.currentTimeMillis() > start + INDEX_UPDATE_TIMEOUT) {
						fail("Timeout waiting for index update!");
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

	// ----- private methods -----
	private boolean indexCreatedSuccessfully(final DatabaseService db, final boolean isNode, final boolean isRelationship, final String entityType, final Set<String> propertyNames, final int expectedEntryCount) {

		logger.info("Waiting for index update..");

		try (final Tx tx = app.tx()) {

			final List<IndexInfo> infos = queryIndexes(db, entityType);

			if (infos.size() == expectedEntryCount) {

				final IndexInfo first = infos.get(0);

				if (isNode && !first.isNode()) {
					return false;
				}

				if (isRelationship && !first.isRelationship()) {
					return false;
				}

				if (!entityType.equals(first.getEntityType())) {
					return false;
				}

				if (!propertyNames.containsAll(first.getProperties())) {
					return false;
				}

				// important, this is the only place where the test passes
				return true;
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return false;
	}

	private boolean hasNumberOfIndexes(final DatabaseService db, final String entityType, final int expectedNumberOfIndexes) {

		logger.info("Waiting for index update..");

		try (final Tx tx = app.tx()) {

			final List<IndexInfo> infos = queryIndexes(db, entityType);

			tx.success();

			return infos.size() == expectedNumberOfIndexes;

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return false;
	}


	private List<IndexInfo> queryIndexes(final DatabaseService db, final String labelOrType) {

		// Neo4j 5.x
		if (db.supportsFeature(DatabaseFeature.ShowIndexesQuery)) {

			final String query = "SHOW INDEXES YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"" + labelOrType + "\"] RETURN name, entityType, labelsOrTypes, properties";
			final NativeQuery<Iterable> nativeQuery    = db.query(query, Iterable.class);
			final Iterable<Map<String, Object>> result = db.execute(nativeQuery);

			return Iterables.toList(Iterables.map(r -> new Neo4IndexInfo(r), result));
		}

		// Neo4j 4.x
		if (db.supportsFeature(DatabaseFeature.NewDBIndexesFormat)) {

			final String query = "CALL db.indexes() YIELD properties, entityType, labelsOrTypes, name WHERE labelsOrTypes = [\"" + labelOrType + "\"] RETURN name, entityType, labelsOrTypes, properties";
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
		protected List<String> types = null;
		protected List<String> props = null;

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

			this.type  = (String)data.get("entityType");
			this.types = (List<String>)data.get("labelsOrTypes");
			this.props = (List<String>)data.get("properties");
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