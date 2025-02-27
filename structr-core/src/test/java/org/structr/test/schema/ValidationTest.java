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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class ValidationTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(ValidationTest.class.getName());

	@Test
	public void testUUIDValidation() {

		try (final Tx tx = app.tx()) {

			// test 31 characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "1234567890123456789012345678901"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test 33 characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "123456789012345678901234567890123"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test 40 characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "1234567890123456789012345678901234567890"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test wrong characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "123456789012345678g0123456789012"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test wrong characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "!bcdefabcdefabcdefabcdefabcdefab"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test wrong characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "sdfkgjh34t"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			// test all allowed characters
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "abcdef1234567890ABCDEF1234567890"));
			tx.success();

		} catch (FrameworkException fex) {

			fail("UUID validation failed for valid result.");
		}

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "xy-"));
			tx.success();

			fail("UUID format constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
			assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne", token.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), ""));
			tx.success();

			fail("UUID not empty constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);

			assertEquals("Invalid uniqueness validation result", 2, tokens.size());
			assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());

			assertEquals("Invalid UUID uniqueness validation result", "id",                token1.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne",           token1.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid UUID uniqueness validation result", "id",         token2.getProperty());
			assertEquals("Invalid UUID uniqueness validation result", "TestOne",    token2.getType());
			assertEquals("Invalid UUID uniqueness validation result", "must_match", token2.getToken());
		}
	}

	@Test
	public void testSchemaNodeNameValidation() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "lowercase");
			tx.success();

			fail("SchemaNode name constraint violation!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid constraint violation error token", 422, fex.getStatus());
			assertEquals("Invalid constraint violation error token", "name", token.getProperty());
			assertEquals("Invalid constraint violation error token", StructrTraits.SCHEMA_NODE, token.getType());
			assertEquals("Invalid constraint violation error token", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "7NumberAsFirstChar");
			tx.success();

			fail("SchemaNode name constraint violation!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid constraint violation error token", 422, fex.getStatus());
			assertEquals("Invalid constraint violation error token", "name", token.getProperty());
			assertEquals("Invalid constraint violation error token", StructrTraits.SCHEMA_NODE, token.getType());
			assertEquals("Invalid constraint violation error token", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "7Number");
			tx.success();

			fail("SchemaNode name constraint violation!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid constraint violation error token", 422, fex.getStatus());
			assertEquals("Invalid constraint violation error token", "name", token.getProperty());
			assertEquals("Invalid constraint violation error token", StructrTraits.SCHEMA_NODE, token.getType());
			assertEquals("Invalid constraint violation error token", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "7Number");
			tx.success();

			fail("SchemaNode name constraint violation!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid constraint violation error token", 422, fex.getStatus());
			assertEquals("Invalid constraint violation error token", "name", token.getProperty());
			assertEquals("Invalid constraint violation error token", StructrTraits.SCHEMA_NODE, token.getType());
			assertEquals("Invalid constraint violation error token", "must_match", token.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, "Valid");
			app.create(StructrTraits.SCHEMA_NODE, "Valid");
			tx.success();

			fail("SchemaNode uniqueness constraint violation!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid uniqueness validation result", 1, tokens.size());
			assertEquals("Invalid constraint violation error token", 422, fex.getStatus());
			assertEquals("Invalid constraint violation error token", "name", token.getProperty());
			assertEquals("Invalid constraint violation error token", StructrTraits.SCHEMA_NODE, token.getType());
			assertEquals("Invalid constraint violation error token", "already_taken", token.getToken());
		}
	}

	@Test
	public void testGlobalUniqueness() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Test"));
			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			String uuid = null;

			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "00000000000000000000000000000000")).getUuid();
				tx.success();

			} catch (FrameworkException fex) {

				fail("Unexpected exception.");
			}

			for (int i=0; i<5; i++) {

				try (final Tx tx = app.tx()) {

					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"), "00000000000000000000000000000000"));
					tx.success();

					fail("UUID uniqueness constraint violated!");

				} catch (FrameworkException fex) {

					final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

					assertEquals("Invalid UUID uniqueness validation result", 422, fex.getStatus());
					assertEquals("Invalid UUID uniqueness validation result", "id", token.getProperty());
					assertEquals("Invalid UUID uniqueness validation result", "Test", token.getType());
					assertEquals("Invalid UUID uniqueness validation result", "already_taken", token.getToken());
					assertEquals("Invalid UUID uniqueness validation result", uuid, token.getDetail());
				}
			}

		}
	}

	@Test
	public void testConcurrentValidation() {

		final String typeName = "Item";
		final int count       = 100;

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType(typeName);

			type.addStringProperty("name").setUnique(true).setRequired(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final PropertyKey name = Traits.of(typeName).key("name");
		assertNotNull(name);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(typeName, "Item" + i);

						tx.success();

					} catch (FrameworkException ignore) {}


				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}

		List<NodeInterface> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(typeName).getAsList();


			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());


		executor.shutdownNow();
	}

	@Test
	public void testConcurrentValidationWithInheritance() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Item");
			final JsonObjectType type2 = schema.addType("ItemDerived");

			type2.addTrait("Item");

			type1.addStringProperty("name").setUnique(true).setRequired(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		final String baseType    = "Item";
		final String derivedType = "ItemDerived";

		assertNotNull(baseType);
		assertNotNull(derivedType);

		final PropertyKey name = Traits.of(baseType).key("name");
		assertNotNull(name);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						if (Math.random() < 0.5) {

							app.create(derivedType, "Item" + i);

						} else {

							app.create(baseType, "Item" + i);
						}

						tx.success();

					} catch (FrameworkException ignore) {}
				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<NodeInterface> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(baseType).getAsList();


			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());


		executor.shutdownNow();
	}

	@Test
	public void testConcurrentValidationOnDynamicProperty() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Item");

			type1.addStringProperty("testXYZ").setUnique(true).setRequired(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("name"), "Item"),
				new NodeAttribute<>(new StringProperty("_testXYZ"), "+String!")
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		final String type = "Item";
		assertNotNull(type);


		final PropertyKey testXYZ = Traits.of(type).key("testXYZ");
		assertNotNull(testXYZ);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(type, new NodeAttribute<>(testXYZ, "Item" + i));

						tx.success();

					} catch (FrameworkException fex) {}

				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<NodeInterface> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(type).getAsList();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());

		executor.shutdownNow();
	}

	@Test
	public void testNamePropertyValidation() {

		// The goal of this test is to ensure that validation
		// only includes actual derived classes.

		// override name property
		try (final Tx tx = app.tx()) {

			// create some nodes with identical names
			app.create(StructrTraits.GROUP,   "unique");
			app.create("TestOne", "unique");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// should succeed
			app.create("TestTwelve", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique"));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Uniqueness constraint includes wrong type(s)!");
		}
	}

	// ----- string property validation tests -----
	@Test
	public void testEmptyStringPropertyValidationWithEmptyStrings() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Test");

			type1.addStringProperty("testUnique").setUnique(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Test"),
				new NodeAttribute<>(new StringProperty("_testUnique"), "String!")
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("testUnique");
			if (key != null) {

				String uuid = null;

				try (final Tx tx = app.tx()) {

					// for string properties, null equals the empty string
					uuid = app.create(testType, new NodeAttribute<>(key, "")).getUuid();

					// wait a little while so the objects are created in different milliseconds
					try { Thread.sleep(5); } catch (InterruptedException ex) { }

					// create constraint violating object
					app.create(testType, new NodeAttribute<>(key, ""));

					tx.success();

					fail("Uniqueness constraint violated!");

				} catch (FrameworkException fex) {

					final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
					final ErrorToken token = tokens.get(0);

					assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
					assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
					assertEquals("Invalid uniqueness validation result", "testUnique", token.getProperty());
					assertEquals("Invalid uniqueness validation result", "Test", token.getType());
					assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
					assertEquals("Invalid uniqueness validation result", uuid, token.getDetail());
				}
			}
		}
	}

	@Test
	public void testEmptyStringPropertyValidationWithNulls() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Test");

			type1.addStringProperty("testUnique").setUnique(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Test"),
				new NodeAttribute<>(new StringProperty("_testUnique"), "String!")
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("testUnique");
			if (key != null) {

				String uuid = null;

				try (final Tx tx = app.tx()) {

					// for string properties, null equals the empty string
					uuid = app.create(testType, new NodeAttribute<>(key, null)).getUuid();

					// wait a little while so the objects are created in different milliseconds
					try { Thread.sleep(5); } catch (InterruptedException ex) { }

					// create constraint violating object
					app.create(testType, new NodeAttribute<>(key, null));

					tx.success();

				} catch (FrameworkException fex) {

					fail("Invalid uniqueness constraint validation for null values!");
				}
			}
		}
	}

	@Test
	public void testStringPropertyUniqueness() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Test");

			type1.addStringProperty("testUnique").setUnique(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Test"),
				new NodeAttribute<>(new StringProperty("_testUnique"), "String!")
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("testUnique");
			if (key != null) {

				try (final Tx tx = app.tx()) {

					// key must be unique, but can empty
					app.create(testType, new NodeAttribute<>(key, "unique"));
					app.create(testType, new NodeAttribute<>(key, ""));
					tx.success();

				} catch (FrameworkException fex) {
					logger.warn("", fex);
					fail("Unexpected exception.");
				}

				for (int i=0; i<5; i++) {

					try (final Tx tx = app.tx()) {

						app.create(testType, new NodeAttribute<>(key, "unique"));
						tx.success();

						fail("Uniqueness constraint violated!");

					} catch (FrameworkException fex) {

						final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
						final ErrorToken token = tokens.get(0);

						assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
						assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
						assertEquals("Invalid uniqueness validation result", "testUnique", token.getProperty());
						assertEquals("Invalid uniqueness validation result", "Test", token.getType());
						assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
					}
				}
			}
		}
	}

	@Test
	public void testInheritedStringPropertyUniqueness() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type1 = schema.addType("Test");
			final JsonObjectType type2 = schema.addType("TestDerived");

			type2.addTrait("Test");

			type1.addStringProperty("testUnique").setUnique(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testType = "TestDerived";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("testUnique");
			if (key != null) {

				try (final Tx tx = app.tx()) {

					// key must be unique, but can empty
					app.create(testType, new NodeAttribute<>(key, "unique"));
					app.create(testType, new NodeAttribute<>(key, ""));
					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
					fail("Unexpected exception.");
				}

				for (int i=0; i<5; i++) {

					try (final Tx tx = app.tx()) {

						app.create(testType, new NodeAttribute<>(key, "unique"));
						tx.success();

						fail("Uniqueness constraint violated!");

					} catch (FrameworkException fex) {

						final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
						final ErrorToken token = tokens.get(0);

						assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
						assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
						assertEquals("Invalid uniqueness validation result", "testUnique", token.getProperty());
						assertEquals("Invalid uniqueness validation result", "TestDerived", token.getType());
						assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
					}
				}
			}
		}
	}

	@Test
	public void testStringPropertyUniquenessAgain() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addStringProperty("testUnique").setUnique(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("testUnique");
			if (key != null) {

				final Random random = new Random();

				try (final Tx tx = app.tx()) {

					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique06"), new NodeAttribute<>(key, "unique00"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique05"), new NodeAttribute<>(key, "unique01"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique04"), new NodeAttribute<>(key, "unique02"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique03"), new NodeAttribute<>(key, "unique03"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique02"), new NodeAttribute<>(key, "unique04"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique01"), new NodeAttribute<>(key, "unique05"));
					app.create(testType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "unique00"), new NodeAttribute<>(key, "unique06"));
					tx.success();

				} catch (FrameworkException fex) {
					logger.warn("", fex);
					fail("Unexpected exception.");
				}

				for (int i=0; i<5; i++) {

					try (final Tx tx = app.tx()) {

						app.create(testType, new NodeAttribute<>(key, "unique0" + random.nextInt(7)));
						tx.success();

						fail("Uniqueness constraint violated!");

					} catch (FrameworkException fex) {

						final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
						final ErrorToken token = tokens.get(0);

						assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
						assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
						assertEquals("Invalid uniqueness validation result", "testUnique", token.getProperty());
						assertEquals("Invalid uniqueness validation result", "Test", token.getType());
						assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
					}
				}
			}
		}
	}

	@Test
	public void testStringPropertyNotNull() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addStringProperty("nonempty").setRequired(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Test"),
				new NodeAttribute<>(new StringProperty("_nonempty"), "+String")
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final String testType = "Test";
		if (testType != null) {

			final PropertyKey key = Traits.of(testType).key("nonempty");
			if (key != null) {

				try (final Tx tx = app.tx()) {

					app.create(testType, new NodeAttribute<>(key, null));
					tx.success();

					fail("Not empty constraint violated!");

				} catch (FrameworkException fex) {

					final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
					final ErrorToken token = tokens.get(0);

					assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
					assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
					assertEquals("Invalid uniqueness validation result", "nonempty", token.getProperty());
					assertEquals("Invalid uniqueness validation result", "Test", token.getType());
					assertEquals("Invalid uniqueness validation result", "must_not_be_empty", token.getToken());
				}
			}
		}
	}

	@Test
	public void testStringPropertyRegexMatch() {

		final String keyName  = "regex";
		final String testType  = createTypeWithProperty("Test", keyName, "String", false, false, "[a-zA-Z0-9]+");
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, "abcdefg_"));
				tx.success();

				fail("Regex constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid uniqueness validation result", 1,       tokens.size());
				assertEquals("Invalid regex validation result", 422,          fex.getStatus());
				assertEquals("Invalid regex validation result", "regex",      token.getProperty());
				assertEquals("Invalid regex validation result", "Test",       token.getType());
				assertEquals("Invalid regex validation result", "must_match", token.getToken());
			}
		}
	}

	// ----- array property validation tests -----
	@Test
	public void testArrayPropertyNotNullValidation() {

		final String keyName  = "stringArray";
		final String testType  = createTypeWithProperty("Test", keyName, "StringArray", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, null));
				tx.success();

				fail("Array property validation failed!");

			} catch (FrameworkException fex) {

				final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

				assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
				assertEquals("Invalid uniqueness validation result", keyName, token.getProperty());
				assertEquals("Invalid uniqueness validation result", "Test", token.getType());
				assertEquals("Invalid uniqueness validation result", "must_not_be_empty", token.getToken());
			}
		}
	}

	@Test
	public void testArrayPropertyUniquenessValidation() {

		final String keyName   = "stringArray";
		final String testType  = createTypeWithProperty("Test", keyName, "StringArray", true, false, null);
		final PropertyKey key  = Traits.of(testType).key(keyName);
		String uuid1           = null;
		String uuid2           = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid1 = app.create(testType, new NodeAttribute<>(key, new String[] { "one", "two" } )).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				uuid2 = app.create(testType, new NodeAttribute<>(key, new String[] { "one", "two" } )).getUuid();

				tx.success();

				fail("Array property validation failed!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid uniqueness validation result", 1, tokens.size());
				assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
				assertEquals("Invalid uniqueness validation result", keyName, token.getProperty());
				assertEquals("Invalid uniqueness validation result", "Test", token.getType());
				assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
				assertEquals("Invalid uniqueness validation result", uuid1, token.getDetail());

			} catch (Throwable t) {
				t.printStackTrace();
			}

			removeInstances(testType);

			// test success
			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, new String[] { "one" } ));
				app.create(testType, new NodeAttribute<>(key, new String[] { "one", "two" } ));
				app.create(testType, new NodeAttribute<>(key, new String[] { "one", "two", "three" } ));
				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Array property validation error!");
			}
		}
	}

	// ----- boolean property validation tests -----
	@Test
	public void testBooleanPropertyNotNullValidation() {

		final String keyName  = "notNull";
		final String testType = createTypeWithProperty("Test", keyName, "Boolean", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				/* Boolean properties are special. A boolean property will
				 * _always_ have a value, i.e. "null" equals "false" and
				 * will be returned as false.
				 *
				 * => a boolean property value can never be null!
				 */

				app.create(testType, new NodeAttribute<>(key, null));
				app.create(testType, new NodeAttribute<>(key, true));
				app.create(testType, new NodeAttribute<>(key, false));
				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected array property validation exception.");
			}
		}
	}

	@Test
	public void testBooleanPropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Boolean", true, false, null);
		final PropertyKey key = Traits.of(testType).key(keyName);
		String uuid                         = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(key, true)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, false));
				app.create(testType, new NodeAttribute<>(key, true));

				tx.success();

				fail("Array property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid uniqueness validation result", 1, tokens.size());
				assertEquals("Invalid uniqueness validation result", 422, fex.getStatus());
				assertEquals("Invalid uniqueness validation result", keyName, token.getProperty());
				assertEquals("Invalid uniqueness validation result", "Test", token.getType());
				assertEquals("Invalid uniqueness validation result", "already_taken", token.getToken());
				assertEquals("Invalid uniqueness validation result", uuid, token.getDetail());
			}
		}
	}

	// ----- date property validation tests -----
	@Test
	public void testDatePropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Date", true, false, null);
		final PropertyKey key = Traits.of(testType).key(keyName);
		final Date date       = new Date();
		String uuid           = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(key, date)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(2); }catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, date));

				tx.success();

				fail("Date property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid date validation result", 1, tokens.size());
				assertEquals("Invalid date validation result", 422, fex.getStatus());
				assertEquals("Invalid date validation result", keyName, token.getProperty());
				assertEquals("Invalid date validation result", "Test", token.getType());
				assertEquals("Invalid date validation result", "already_taken", token.getToken());
				assertEquals("Invalid date validation result", uuid, token.getDetail());
			}
		}
	}

	@Test
	public void testDatePropertyNotNullValidation() {

		final String keyName  = "notnull";
		final String testType = createTypeWithProperty("Test", keyName, "Date", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, null));

				tx.success();

				fail("Date property not null constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid date validation result", 1, tokens.size());
				assertEquals("Invalid date validation result", 422, fex.getStatus());
				assertEquals("Invalid date validation result", keyName, token.getProperty());
				assertEquals("Invalid date validation result", "Test", token.getType());
				assertEquals("Invalid date validation result", "must_not_be_empty", token.getToken());
			}
		}
	}

	// ----- double property validation tests -----
	@Test
	public void testDoublePropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Double", true, false, null);
		final PropertyKey key = Traits.of(testType).key(keyName);
		String uuid                         = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(key, 0.123)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, 0.123));

				tx.success();

				fail("Double property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid double validation result", 1, tokens.size());
				assertEquals("Invalid double validation result", 422, fex.getStatus());
				assertEquals("Invalid double validation result", keyName, token.getProperty());
				assertEquals("Invalid double validation result", "Test", token.getType());
				assertEquals("Invalid double validation result", "already_taken", token.getToken());
				assertEquals("Invalid double validation result", uuid, token.getDetail());
			}
		}
	}

	@Test
	public void testDoublePropertyNotNullValidation() {

		final String keyName  = "notnull";
		final String testType = createTypeWithProperty("Test", keyName, "Double", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, null));

				tx.success();

				fail("Double property not null constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid double validation result", 1, tokens.size());
				assertEquals("Invalid double validation result", 422, fex.getStatus());
				assertEquals("Invalid double validation result", keyName, token.getProperty());
				assertEquals("Invalid double validation result", "Test", token.getType());
				assertEquals("Invalid double validation result", "must_not_be_empty", token.getToken());
			}
		}
	}

	@Test
	public void testDoublePropertyRangeValidation1() {

		final String testType    = createTypeWithProperty("Test", "range1", "Double", false, true, "[1,5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1.0);
		checkRangeSuccess(testType, range1, 1.00001);
		checkRangeSuccess(testType, range1, 1.1);
		checkRangeSuccess(testType, range1, 2.2);
		checkRangeSuccess(testType, range1, 3.3);
		checkRangeSuccess(testType, range1, 4.4);
		checkRangeSuccess(testType, range1, 4.999999);
		checkRangeSuccess(testType, range1, 5.0);

		try { checkRangeError(testType, range1,    -0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,        0.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,  5.00000001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testDoublePropertyRangeValidation2() {

		final String testType    = createTypeWithProperty("Test", "range1", "Double", false ,true, "[0.0,0.5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, -0.0);
		checkRangeSuccess(testType, range1, 0.0);
		checkRangeSuccess(testType, range1, 0.00001);
		checkRangeSuccess(testType, range1, 0.1);
		checkRangeSuccess(testType, range1, 0.2);
		checkRangeSuccess(testType, range1, 0.3);
		checkRangeSuccess(testType, range1, 0.4);
		checkRangeSuccess(testType, range1, 0.49999);
		checkRangeSuccess(testType, range1, 0.5);

		try { checkRangeError(testType, range1, -0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     1.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testDoublePropertyRangeValidation3() {

		final String testType    = createTypeWithProperty("Test", "range1", "Double", false, true, "[0.0,0.5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, -0.0);
		checkRangeSuccess(testType, range1, 0.0);
		checkRangeSuccess(testType, range1, 0.00001);
		checkRangeSuccess(testType, range1, 0.1);
		checkRangeSuccess(testType, range1, 0.2);
		checkRangeSuccess(testType, range1, 0.3);
		checkRangeSuccess(testType, range1, 0.4);
		checkRangeSuccess(testType, range1, 0.49999);

		try { checkRangeError(testType, range1, -0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.5); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     1.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testDoublePropertyRangeValidation4() {

		final String testType    = createTypeWithProperty("Test", "range1", "Double", false, true, "]0.0,0.5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 0.00001);
		checkRangeSuccess(testType, range1, 0.1);
		checkRangeSuccess(testType, range1, 0.2);
		checkRangeSuccess(testType, range1, 0.3);
		checkRangeSuccess(testType, range1, 0.4);
		checkRangeSuccess(testType, range1, 0.49999);
		checkRangeSuccess(testType, range1, 0.5);

		try { checkRangeError(testType, range1,     -0.0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,      0.0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, -0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     1.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testDoublePropertyRangeValidation5() {

		final String testType    = createTypeWithProperty("Test", "range1", "Double", false ,true, "]0.0,0.5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 0.00001);
		checkRangeSuccess(testType, range1, 0.1);
		checkRangeSuccess(testType, range1, 0.2);
		checkRangeSuccess(testType, range1, 0.3);
		checkRangeSuccess(testType, range1, 0.4);
		checkRangeSuccess(testType, range1, 0.49999);

		try { checkRangeError(testType, range1,     -0.0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,      0.0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, -0.00001); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     0.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,     1.51); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,      5.0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
	}

	// ----- enum property validation tests -----
	@Test
	public void testEnumPropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Enum", true, false, "one, two, three");
		final PropertyKey key = Traits.of(testType).key(keyName);
		String uuid           = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				final Object value = ((EnumProperty)key).getEnumConstants().toArray()[0];

				uuid = app.create(testType, new NodeAttribute<>(key, value)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, value));

				tx.success();

				fail("Enum property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid enum validation result", 1, tokens.size());
				assertEquals("Invalid enum validation result", 422, fex.getStatus());
				assertEquals("Invalid enum validation result", keyName, token.getProperty());
				assertEquals("Invalid enum validation result", "Test", token.getType());
				assertEquals("Invalid enum validation result", "already_taken", token.getToken());
				assertEquals("Invalid enum validation result", uuid, token.getDetail());
			}
		}
	}

	@Test
	public void testEnumPropertyNotNullValidation() {

		final String keyName  = "notnull";
		final String testType = createTypeWithProperty("Test", keyName, "Enum", false, true, "one, two, three");
		final PropertyKey key = Traits.of(testType).key(keyName);

		// test failure
		try (final Tx tx = app.tx()) {

			app.create(testType, new NodeAttribute<>(key, null));

			tx.success();

			fail("Enum property not null constraint violated!");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token = tokens.get(0);

			assertEquals("Invalid enum validation result", 1, tokens.size());
			assertEquals("Invalid enum validation result", 422, fex.getStatus());
			assertEquals("Invalid enum validation result", keyName, token.getProperty());
			assertEquals("Invalid enum validation result", "Test", token.getType());
			assertEquals("Invalid enum validation result", "must_not_be_empty", token.getToken());
		}
	}

	// ----- int property validation tests -----
	@Test
	public void testIntPropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Integer", true, false, null);
		final PropertyKey key = Traits.of(testType).key(keyName);
		String uuid           = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(key, 42)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, 42));

				tx.success();

				fail("Int property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid int validation result", 1, tokens.size());
				assertEquals("Invalid int validation result", 422, fex.getStatus());
				assertEquals("Invalid int validation result", keyName, token.getProperty());
				assertEquals("Invalid int validation result", "Test", token.getType());
				assertEquals("Invalid int validation result", "already_taken", token.getToken());
				assertEquals("Invalid int validation result", uuid, token.getDetail());
			}
		}
	}

	@Test
	public void testIntPropertyNotNullValidation() {

		final String keyName  = "notnull";
		final String testType = createTypeWithProperty("Test", keyName, "Integer", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, null));

				tx.success();

				fail("Int property not null constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid int validation result", 1, tokens.size());
				assertEquals("Invalid int validation result", 422, fex.getStatus());
				assertEquals("Invalid int validation result", keyName, token.getProperty());
				assertEquals("Invalid int validation result", "Test", token.getType());
				assertEquals("Invalid int validation result", "must_not_be_empty", token.getToken());
			}
		}
	}

	@Test
	public void testIntPropertyRangeValidation1() {

		final String testType    = createTypeWithProperty("Test", "range1", "Integer", false, true, "[1,5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1);
		checkRangeSuccess(testType, range1, 2);
		checkRangeSuccess(testType, range1, 3);
		checkRangeSuccess(testType, range1, 4);
		checkRangeSuccess(testType, range1, 5);

		try { checkRangeError(testType, range1, -0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,  0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,  6); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testIntPropertyRangeValidation3() {

		final String testType    = createTypeWithProperty("Test", "range1", "Integer", false, true, "[0,5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, -0);
		checkRangeSuccess(testType, range1, 0);
		checkRangeSuccess(testType, range1, 1);
		checkRangeSuccess(testType, range1, 2);
		checkRangeSuccess(testType, range1, 3);
		checkRangeSuccess(testType, range1, 4);

		try { checkRangeError(testType, range1, 5); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testIntPropertyRangeValidation4() {

		final String testType    = createTypeWithProperty("Test", "range1", "Integer", false, true, "]0,5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1);
		checkRangeSuccess(testType, range1, 2);
		checkRangeSuccess(testType, range1, 3);
		checkRangeSuccess(testType, range1, 4);
		checkRangeSuccess(testType, range1, 5);

		try { checkRangeError(testType, range1, 0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
	}

	@Test
	public void testIntPropertyRangeValidation5() {

		final String testType    = createTypeWithProperty("Test", "range1", "Integer", false, true, "]0,5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1);
		checkRangeSuccess(testType, range1, 2);
		checkRangeSuccess(testType, range1, 3);
		checkRangeSuccess(testType, range1, 4);

		try { checkRangeError(testType, range1, 0); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 5); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
	}

	// ----- long property validation tests -----
	@Test
	public void testLongPropertyUniquenessValidation() {

		final String keyName  = "unique";
		final String testType = createTypeWithProperty("Test", keyName, "Long", true, false, null);
		final PropertyKey key = Traits.of(testType).key(keyName);
		String uuid           = null;

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				uuid = app.create(testType, new NodeAttribute<>(key, 42000000000L)).getUuid();

				// make sure creation of the two objects is more than 1ms apart
				try { Thread.sleep(5); } catch (Throwable t) {}

				app.create(testType, new NodeAttribute<>(key, 42000000000L));

				tx.success();

				fail("Long property uniqueness constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid long validation result", 1, tokens.size());
				assertEquals("Invalid long validation result", 422, fex.getStatus());
				assertEquals("Invalid long validation result", keyName, token.getProperty());
				assertEquals("Invalid long validation result", "Test", token.getType());
				assertEquals("Invalid long validation result", "already_taken", token.getToken());
				assertEquals("Invalid long validation result", uuid, token.getDetail());
			}
		}
	}

	@Test
	public void testLongPropertyNotNullValidation() {

		final String keyName  = "notnull";
		final String testType = createTypeWithProperty("Test", keyName, "Long", false, true, null);
		final PropertyKey key = Traits.of(testType).key(keyName);

		if (key != null) {

			// test failure
			try (final Tx tx = app.tx()) {

				app.create(testType, new NodeAttribute<>(key, null));

				tx.success();

				fail("Long property not null constraint violated!");

			} catch (FrameworkException fex) {

				final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
				final ErrorToken token = tokens.get(0);

				assertEquals("Invalid long validation result", 1, tokens.size());
				assertEquals("Invalid long validation result", 422, fex.getStatus());
				assertEquals("Invalid long validation result", keyName, token.getProperty());
				assertEquals("Invalid long validation result", "Test", token.getType());
				assertEquals("Invalid long validation result", "must_not_be_empty", token.getToken());
			}
		}
	}

	@Test
	public void testLongPropertyRangeValidation1() {

		final String testType    = createTypeWithProperty("Test", "range1", "Long", false, true, "[1,5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1L);
		checkRangeSuccess(testType, range1, 2L);
		checkRangeSuccess(testType, range1, 3L);
		checkRangeSuccess(testType, range1, 4L);
		checkRangeSuccess(testType, range1, 5L);

		try { checkRangeError(testType, range1, -0L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,  0L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1,  6L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testLongPropertyRangeValidation3() {

		final String testType    = createTypeWithProperty("Test", "range1", "Long", false, true, "[0,5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, -0L);
		checkRangeSuccess(testType, range1,  0L);
		checkRangeSuccess(testType, range1,  1L);
		checkRangeSuccess(testType, range1,  2L);
		checkRangeSuccess(testType, range1,  3L);
		checkRangeSuccess(testType, range1,  4L);

		try { checkRangeError(testType, range1, 5L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }

	}

	@Test
	public void testLongPropertyRangeValidation4() {

		final String testType    = createTypeWithProperty("Test", "range1", "Long", false, true, "]0,5]");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1L);
		checkRangeSuccess(testType, range1, 2L);
		checkRangeSuccess(testType, range1, 3L);
		checkRangeSuccess(testType, range1, 4L);
		checkRangeSuccess(testType, range1, 5L);

		try { checkRangeError(testType, range1, 0L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
	}

	@Test
	public void testLongPropertyRangeValidation5() {

		final String testType    = createTypeWithProperty("Test", "range1", "Long", false, true, "]0,5[");
		final PropertyKey range1 = Traits.of(testType).key("range1");

		checkRangeSuccess(testType, range1, 1L);
		checkRangeSuccess(testType, range1, 2L);
		checkRangeSuccess(testType, range1, 3L);
		checkRangeSuccess(testType, range1, 4L);

		try { checkRangeError(testType, range1, 0L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 5L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
		try { checkRangeError(testType, range1, 6L); } catch (FrameworkException fex) { checkException(fex, 1, 422, "Test", "range1", "must_be_in_range"); }
	}

	// schema relationship node validation
	@Test
	public void testSchemaRelationshipNodeValidation() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, source and target node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertEquals("Invalid SchemaRelationshipNode validation result", 5, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);
			final ErrorToken token3 = tokens.get(2);
			final ErrorToken token4 = tokens.get(3);
			final ErrorToken token5 = tokens.get(4);

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "relationshipType", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceNode", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token3.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceType", token3.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token3.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token4.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetNode", token4.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token4.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token5.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetType", token5.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token5.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("relationshipType"), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("sourceNode"), null),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("targetNode"), null)
			);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, source and target node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertEquals("Invalid SchemaRelationshipNode validation result", 4, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);
			final ErrorToken token3 = tokens.get(2);
			final ErrorToken token4 = tokens.get(3);

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceNode", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceType", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token3.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetNode", token3.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token3.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token4.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetType", token4.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token4.getToken());
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "TestType")
			);

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("relationshipType"), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("sourceNode"), node),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("targetNode"), null)
			);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, target node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);

			assertEquals("Invalid SchemaRelationshipNode validation result", 2, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetNode", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetType", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "TestType")
			);

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("relationshipType"), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("sourceNode"), null),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("targetNode"), node)
			);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, source node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);

			assertEquals("Invalid SchemaRelationshipNode validation result", 2, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceNode", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceType", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("relationshipType"), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("sourceType"), StructrTraits.GROUP),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("targetNode"), null)
			);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, target node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);
			final ErrorToken token3 = tokens.get(2);

			assertEquals("Invalid SchemaRelationshipNode validation result", 3, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceNode", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetNode", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token3.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetType", token3.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token3.getToken());
		}

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("relationshipType"), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("sourceNode"), null),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("targetType"), StructrTraits.GROUP)
			);

			tx.success();

			fail("SchemaRelationshipNode constraint violation, source node must not be null.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
			final ErrorToken token1 = tokens.get(0);
			final ErrorToken token2 = tokens.get(1);
			final ErrorToken token3 = tokens.get(2);

			assertEquals("Invalid SchemaRelationshipNode validation result", 3, tokens.size());
			assertEquals("Invalid SchemaRelationshipNode validation result", 422, fex.getStatus());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token1.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceNode", token1.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token1.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token2.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "sourceType", token2.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token2.getToken());

			assertEquals("Invalid SchemaRelationshipNode validation result", StructrTraits.SCHEMA_RELATIONSHIP_NODE, token3.getType());
			assertEquals("Invalid SchemaRelationshipNode validation result", "targetNode", token3.getProperty());
			assertEquals("Invalid SchemaRelationshipNode validation result", "must_not_be_empty", token3.getToken());
		}
	}

	@Test
	public void testCompoundUniqueness() {

		try (final Tx tx = app.tx()) {

			final NodeInterface typeNode = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("name"), "TestType"));

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("name"), "key1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("propertyType"), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("schemaNode"), typeNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("compound"), true)
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("name"), "key2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("propertyType"), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("schemaNode"), typeNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("compound"), true)
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("name"), "key3"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("propertyType"), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("schemaNode"), typeNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("compound"), true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String type           = "TestType";
		final PropertyKey key1      = Traits.of(type).key("key1");
		final PropertyKey key2      = Traits.of(type).key("key2");
		final PropertyKey key3      = Traits.of(type).key("key3");
		final Set<PropertyKey> keys = Set.of(key1, key2, key3);

		// test success
		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key2, "two"),
				new NodeAttribute<>(key3, "three")
			);

			// wait a little while so the objects are created in different milliseconds
			try { Thread.sleep(5); } catch (InterruptedException ex) { }

			app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key2, "one"),
				new NodeAttribute<>(key3, "three")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Invalid compound indexing validation result.");
		}

		// test success
		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key3, "three")
			);

			// wait a little while so the objects are created in different milliseconds
			try { Thread.sleep(5); } catch (InterruptedException ex) { }

			app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key3, "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Invalid compound indexing validation result.");
		}

		String uuid = null;

		// test failure
		try (final Tx tx = app.tx()) {

			uuid = app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key2, "two"),
				new NodeAttribute<>(key3, "three")
			).getUuid();

			// wait a little while so the objects are created in different milliseconds
			try { Thread.sleep(5); } catch (InterruptedException ex) { }

			app.create(type,
				new NodeAttribute<>(key1, "one"),
				new NodeAttribute<>(key2, "two"),
				new NodeAttribute<>(key3, "three")
			);

			tx.success();

			fail("Invalid compound indexing validation result.");

		} catch (FrameworkException fex) {

			System.out.println(fex.toJSON());

			final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals("Invalid validation status code", fex.getStatus(), 422);
			assertEquals("Invalid validation error token", "already_taken", token.getToken());
			assertEquals("Invalid validation error type", "TestType",       token.getType());
			assertEquals("Invalid validation error UUID", uuid,             token.getDetail());
			assertEquals("Invalid validation error type", keys,             token.getValue());

		}
	}

	@Test
	public void testSchemaGrantAndGroupUniqueness() {

		// test that two identical SchemaGrant objects (identical SchemaNode and Principal) throw an error
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "Project"));
			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("Project").getFirst();
			final NodeInterface group      = app.create(StructrTraits.GROUP, "Group1");

			app.create(StructrTraits.SCHEMA_GRANT, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"), group), new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("schemaNode"), schemaNode));
			app.create(StructrTraits.SCHEMA_GRANT, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"), group), new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("schemaNode"), schemaNode));

			tx.success();

			fail("SchemaGrant uniqueness constraint violation, creating two identical SchemaGrants should not be allowed");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertEquals("Invalid SchemaGrant validation result",   1, tokens.size());
			assertEquals("Invalid SchemaGrant validation result", 422, fex.getStatus());

			final ErrorToken token       = tokens.get(0);
			final List<PropertyKey> keys = new LinkedList<>((Set)token.getValue());

			assertEquals("Invalid SchemaGrant validation result", StructrTraits.SCHEMA_GRANT,          token.getType());
			assertEquals("Invalid SchemaGrant validation result", "already_taken",        token.getToken());
			assertEquals("Invalid SchemaGrant validation result", Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"),            keys.get(0));
			assertEquals("Invalid SchemaGrant validation result", Traits.of(StructrTraits.SCHEMA_GRANT).key("schemaNode"),           keys.get(1));
			assertEquals("Invalid SchemaGrant validation result", Traits.of(StructrTraits.SCHEMA_GRANT).key("staticSchemaNodeName"), keys.get(2));
		}

		// test that tho Group objects with the same name throw an error
		try (final Tx tx = app.tx()) {

			// create a second group with name Group1
			app.create(StructrTraits.GROUP, "Group1");
			app.create(StructrTraits.GROUP, "Group1");

			tx.success();

			fail("Group uniqueness constraint violation, creating two Groups with the same name should not be allowed");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertEquals("Invalid Group validation result",   1, tokens.size());
			assertEquals("Invalid Group validation result", 422, fex.getStatus());

			final ErrorToken token1 = tokens.get(0);

			assertEquals("Invalid Group validation result", StructrTraits.GROUP,         token1.getType());
			assertEquals("Invalid Group validation result", "name",          token1.getProperty());
			assertEquals("Invalid Group validation result", "already_taken", token1.getToken());
		}

		// verify that a Group cannot have an empty name
		try (final Tx tx = app.tx()) {

			// create a second group with name Group1
			app.create(StructrTraits.GROUP);

			tx.success();

			fail("Group constraint violation, creating a Group without a name should not be allowed");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertEquals("Invalid Group validation result",   1, tokens.size());
			assertEquals("Invalid Group validation result", 422, fex.getStatus());

			final ErrorToken token1 = tokens.get(0);

			assertEquals("Invalid Group validation result", StructrTraits.GROUP,             token1.getType());
			assertEquals("Invalid Group validation result", "name",              token1.getProperty());
			assertEquals("Invalid Group validation result", "must_not_be_empty", token1.getToken());
		}
	}

	// ----- private methods -----
	private void checkRangeSuccess(final String type, final PropertyKey key, final Object value) {

		try (final Tx tx = app.tx()) {

			app.create(type, new NodeAttribute<>(key, value));
			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Property range constraint validation failure!");
		}
	}

	private void checkRangeError(final String type, final PropertyKey key, final Object value) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			app.create(type, new NodeAttribute<>(key, value));
			tx.success();

			fail("Property range constraint violated!");

		} catch (FrameworkException fex) {
			checkException(fex, 1, 422, "Test", "range1", "must_be_in_range");
		}
	}

	private void checkException(final FrameworkException fex, final int numberOfTokens, final int statusCode, final String typeName, final String keyName, final String errorToken) {
		checkException(fex, numberOfTokens, statusCode, typeName, keyName, errorToken, null);
	}

	private void checkException(final FrameworkException fex, final int numberOfTokens, final int statusCode, final String typeName, final String keyName, final String errorToken, final String uuid) {

		final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();
		final ErrorToken token        = tokens.get(0);

		assertEquals("Invalid validation result", numberOfTokens, tokens.size());
		assertEquals("Invalid validation result", statusCode,     fex.getStatus());
		assertEquals("Invalid validation result", keyName,        token.getProperty());
		assertEquals("Invalid validation result", typeName,       token.getType());
		assertEquals("Invalid validation result", errorToken,     token.getToken());

		if (uuid != null) {

			assertEquals("Invalid validation result", uuid, token.getDetail());
		}

	}

	private String createTypeWithProperty(final String typeName, final String keyName, final String keyType, final boolean unique, final boolean required, final String format) {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType(typeName);

			switch (keyType) {

				case "Boolean"      -> type.addBooleanProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "BooleanArray" -> type.addBooleanArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "Date"         -> type.addDateProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "DateArray"    -> type.addDateArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "Integer"      -> type.addIntegerProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "IntegerArray" -> type.addIntegerArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "Long"         -> type.addLongProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "LongArray"    -> type.addLongArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "Double"       -> type.addDoubleProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "DoubleArray"  -> type.addDoubleArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "String"       -> type.addStringProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "StringArray"  -> type.addStringArrayProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);
				case "Enum"         -> type.addEnumProperty(keyName).setUnique(unique).setRequired(required).setFormat(format);

				default -> throw new RuntimeException("Unknown key type " + keyType);
			}

			StructrSchema.extendDatabaseSchema(app, schema);

			/*
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), typeName),
				new NodeAttribute<>(new StringProperty("_" + keyName), keyType)
			);
			*/

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		return typeName;
	}

	private void removeInstances(final String type) {

		// clear database
		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(type).getAsList()) {
				app.delete(node);
			}
			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
