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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.attribute.Name;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class FunctionPropertyTest extends StructrRestTestBase {

	@Test
	public void testFunctionProperty() {

		final String uuid = createEntity("/TestTen");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
		.expect()
			.statusCode(200)
			.body("result[0].type",                               equalTo("TestTen"))
			.body("result[0].id",                                 equalTo(uuid))
			.body("result[0].functionTest.name",                  equalTo("test"))
			.body("result[0].functionTest.value",                 equalTo(123))
			.body("result[0].functionTest.me.type",               equalTo("TestTen"))
			.body("result[0].functionTest.me.id",                 equalTo(uuid))
//			.body("result[0].functionTest.me.functionTest.name",  equalTo("test"))
//			.body("result[0].functionTest.me.functionTest.value", equalTo(123))
		.when()
			.get("/TestTen");

	}

	@Test
	public void testFunctionPropertyCaching() {

		App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			NodeInterface testObj = app.create("TestTen", "testObject");
			final Traits traits   = testObj.getTraits();

			// Test cache invalidation
			String firstState =  testObj.getProperty(traits.key("getNameProperty"));
			String secondState = testObj.getProperty(traits.key("getNameProperty"));

			assertEquals(firstState, secondState);

			testObj.setProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testObject2");

			secondState = (String)testObj.getProperty(traits.key("getNameProperty"));

			assertNotSame(firstState, secondState);

			firstState = (String)testObj.getProperty(traits.key("getNameProperty"));

			assertEquals(firstState, secondState);

			// Test caching for random numbers
			Object firstNum  = testObj.getProperty(traits.key("getRandomNumProp"));
			Object secondNum = testObj.getProperty(traits.key("getRandomNumProp"));

			assertEquals(firstNum, secondNum);

			// Invalidate cache to test random caching
			testObj.setProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testObject3");

			firstNum  = testObj.getProperty(traits.key("getRandomNumProp"));
			secondNum = testObj.getProperty(traits.key("getRandomNumProp"));

			assertEquals(firstNum, secondNum);

		} catch (FrameworkException ex) {
			fail("Exception during test: " + ex.getMessage());
		}
	}

	@Test
	public void testSearchWithTypeHint() {

		testSearchWithType("Boolean", "eq(this.name, 'test')", true);
		testSearchWithType("Int",     "if(eq(this.name, 'test'), int(3), int(8))", 3);
		testSearchWithType("Long",    "if(eq(this.name, 'test'), int(3), int(8))", 3);
		testSearchWithType("Double",  "if(eq(this.name, 'test'), 3.0, 8.0)", 3.0f);
	}

	private void testSearchWithType(final String typeName, final String readFunction, final Object value) {

		try { Thread.sleep(1000); } catch (Throwable t) {}

		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType type     = schema.addType("TestType");

			type.addFunctionProperty("test" + typeName).setReadFunction(readFunction).setTypeHint(typeName).setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type     = "TestType";
		final PropertyKey key = Traits.of(type).key("test" + typeName);

		// data setup
		try (final Tx tx = app.tx()) {

			app.create(type, new Name("not test"));
			app.create(type, new Name("test"));

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// test
		try (final Tx tx = app.tx()) {

			assertEquals("Invalid search result for typed function property", 1, app.nodeQuery(type).key(key, value).getAsList().size());

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// search via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
		.expect()
			.statusCode(200)
			.body("result[0].type",            equalTo("TestType"))
			.body("result[0].name",            equalTo("test"))
			.body("result[0].test" + typeName, equalTo(value))
			.body("result",                    hasSize(1))
		.when()
			.get("/TestType/all?test" + typeName + "=" + value);
	}

	@Test
	public void testFunctionPropertyCacheInvalidation() {


		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType type     = schema.addType("TestType");

			type.addFunctionProperty("test").setReadFunction("{ 'value1'; }").setTypeHint("String");

			type.addViewProperty("public", "test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type = "TestType";

		// data setup
		try (final Tx tx = app.tx()) {

			app.create(type, new Name("test"));

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// check via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("result[0].type",  equalTo("TestType"))
			.body("result[0].name",  equalTo("test"))
			.body("result[0].test",       equalTo("value1"))
			.body("result",                    hasSize(1))
			.when()
			.get("/TestType");

		// fetch UUID of test property
		final String functionPropertyId = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.when()
			.get("/SchemaProperty?name=test")
			.andReturn()
			.body()
			.jsonPath()
			.getString("result[0].id");


		// change read function
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.body("{ readFunction: '{ \"changed\"; }' }")
			.expect()
			.statusCode(200)
			.when()
			.put("/SchemaProperty/" + functionPropertyId);

		// check via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("result[0].type",  equalTo("TestType"))
			.body("result[0].name",  equalTo("test"))
			.body("result[0].test",       equalTo("changed"))
			.body("result",                    hasSize(1))
			.when()
			.get("/TestType");
	}
}
