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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import org.structr.api.schema.JsonDateProperty;
import org.structr.api.schema.JsonFunctionProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.attribute.Name;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.*;

public class FunctionPropertyTest extends StructrRestTestBase {

	@Test
	public void testFunctionProperty() {

		final String uuid = createEntity("/TestTen");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
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

			type.addFunctionProperty("test").setReadFunction("{ return 'value1'; }").setTypeHint("String");

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
			.body("{ readFunction: '{ return \"changed\"; }' }")
			.expect()
			.statusCode(200)
			.when()
			.put("/SchemaProperty/" + functionPropertyId);

		// check via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result[0].type",  equalTo("TestType"))
			.body("result[0].name",  equalTo("test"))
			.body("result[0].test",       equalTo("changed"))
			.body("result",                    hasSize(1))
			.when()
			.get("/TestType");
	}

	@Test
	public void testFunctionPropertySorting() {

		// create test type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema                     = StructrSchema.createFromDatabase(app);
			final JsonType type                         = schema.addType("FunctionPropertyTest");
			final JsonDateProperty dateProperty         = type.addDateProperty("date");
			final JsonFunctionProperty sortTestProperty = type.addFunctionProperty("sortTest", PropertyView.Public);

			sortTestProperty.setTypeHint("Date");
			sortTestProperty.setReadFunction("this.date");
			sortTestProperty.setIsCachingEnabled(true);
			sortTestProperty.setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final PropertyKey<Date> dateKey = Traits.of("FunctionPropertyTest").key("date");

		try (final Tx tx = app.tx()) {

			for (int i=0; i<20; i++) {

				final Calendar calendar = Calendar.getInstance();

				calendar.set(2000 + i, 0, 1);

				app.create("FunctionPropertyTest",
					new Name("test" + i),
					new NodeAttribute<>(dateKey, calendar.getTime())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// check via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result[0].sortTest",  startsWith("2000-01-01"))
			.body("result[1].sortTest",  startsWith("2001-01-01"))
			.body("result[2].sortTest",  startsWith("2002-01-01"))
			.body("result[3].sortTest",  startsWith("2003-01-01"))
			.body("result[4].sortTest",  startsWith("2004-01-01"))
			.body("result[5].sortTest",  startsWith("2005-01-01"))
			.body("result[6].sortTest",  startsWith("2006-01-01"))
			.body("result[7].sortTest",  startsWith("2007-01-01"))
			.body("result[8].sortTest",  startsWith("2008-01-01"))
			.body("result[9].sortTest",  startsWith("2009-01-01"))
			.body("result[10].sortTest", startsWith("2010-01-01"))
			.body("result[11].sortTest", startsWith("2011-01-01"))
			.body("result[12].sortTest", startsWith("2012-01-01"))
			.body("result[13].sortTest", startsWith("2013-01-01"))
			.body("result[14].sortTest", startsWith("2014-01-01"))
			.body("result[15].sortTest", startsWith("2015-01-01"))
			.body("result[16].sortTest", startsWith("2016-01-01"))
			.body("result[17].sortTest", startsWith("2017-01-01"))
			.body("result[18].sortTest", startsWith("2018-01-01"))
			.body("result[19].sortTest", startsWith("2019-01-01"))
			.when()
			.get("/FunctionPropertyTest?_sort=sortTest");

		// check via REST
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.expect()
			.statusCode(200)
			.body("result[0].sortTest",  startsWith("2019-01-01"))
			.body("result[1].sortTest",  startsWith("2018-01-01"))
			.body("result[2].sortTest",  startsWith("2017-01-01"))
			.body("result[3].sortTest",  startsWith("2016-01-01"))
			.body("result[4].sortTest",  startsWith("2015-01-01"))
			.body("result[5].sortTest",  startsWith("2014-01-01"))
			.body("result[6].sortTest",  startsWith("2013-01-01"))
			.body("result[7].sortTest",  startsWith("2012-01-01"))
			.body("result[8].sortTest",  startsWith("2011-01-01"))
			.body("result[9].sortTest",  startsWith("2010-01-01"))
			.body("result[10].sortTest", startsWith("2009-01-01"))
			.body("result[11].sortTest", startsWith("2008-01-01"))
			.body("result[12].sortTest", startsWith("2007-01-01"))
			.body("result[13].sortTest", startsWith("2006-01-01"))
			.body("result[14].sortTest", startsWith("2005-01-01"))
			.body("result[15].sortTest", startsWith("2004-01-01"))
			.body("result[16].sortTest", startsWith("2003-01-01"))
			.body("result[17].sortTest", startsWith("2002-01-01"))
			.body("result[18].sortTest", startsWith("2001-01-01"))
			.body("result[19].sortTest", startsWith("2000-01-01"))
			.when()
			.get("/FunctionPropertyTest?_sort=sortTest&_order=desc");

	}

	@Test
	public void testFunctionPropertySerializationWithNullValue() {

		try (final Tx tx = app.tx()) {

			final NodeInterface schemaNode = app.create(StructrTraits.SCHEMA_NODE,"FunctionPropertyTest");

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnBool"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "Boolean"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : true; }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnDate"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "Date"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : new Date(2026, 0, 1); }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnDouble"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "Double"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : 13; }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnInt"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "Int"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : 37; }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnLong"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "Long"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : 42; }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "fnString"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.TYPE_HINT_PROPERTY),     "String"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return ($.this.name === 'shouldHaveNullValues') ? null : 'some string'; }")
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		String resource = "/FunctionPropertyTest";

		final String uuidForNodeWithNulls = getUuidFromLocation(
				RestAssured
						.given()
							.contentType("application/json; charset=UTF-8")
							.header("Accept", "application/json; charset=UTF-8")
						.body(" { 'name' : 'shouldHaveNullValues' } ")

						.expect()
							.statusCode(201)

						.when()
							.post(resource).getHeader("Location")
		);

		final String uuidForNodeWithoutNulls = getUuidFromLocation(
				RestAssured
						.given()
							.contentType("application/json; charset=UTF-8")
							.header("Accept", "application/json; charset=UTF-8")
						.body(" { 'name' : 'shouldNOTHaveNullValues' } ")

						.expect()
							.statusCode(201)

						.when()
							.post(resource).getHeader("Location")
		);

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
				.expect()
					.statusCode(200)
					.body("result_count",     equalTo(1))
					.body("result",           isEntity("FunctionPropertyTest"))
					.body("result.id",        equalTo(uuidForNodeWithoutNulls))
					.body("result.name",      equalTo("shouldNOTHaveNullValues"))
					.body("result.fnBool",    equalTo(true))
					.body("result.fnDate",    equalTo("2026-01-01T00:00:00+0000"))
					.body("result.fnDouble",  equalTo(13.0F))
					.body("result.fnInt",     equalTo(37))
					.body("result.fnLong",    equalTo(42))
					.body("result.fnString",  equalTo("some string"))
				.when()
					.get(resource + "/" + uuidForNodeWithoutNulls + "/all");

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
				.expect()
					.statusCode(200)
					.body("result_count",     equalTo(1))
					.body("result",           isEntity("FunctionPropertyTest"))
					.body("result.id",        equalTo(uuidForNodeWithNulls))
					.body("result.name",      equalTo("shouldHaveNullValues"))
					.body("result.fnBool",    equalTo(false))                  // this WILL break once scripting returns real return values instead of empty string for null (just change "false" to null when this broader fix has arrived)
					.body("result.fnDate",    equalTo(null))
					.body("result.fnDouble",  equalTo(null))
					.body("result.fnInt",     equalTo(null))
					.body("result.fnLong",    equalTo(null))
					.body("result.fnString",  equalTo(""))                     // this WILL break once scripting returns real return values instead of empty string for null (just change "false" to null when this broader fix has arrived)
				.when()
					.get(resource + "/" + uuidForNodeWithNulls + "/all");
	}
}
