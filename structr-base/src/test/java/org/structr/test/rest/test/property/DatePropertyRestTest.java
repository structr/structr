/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class DatePropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'dateProperty' : '2013-04-05T10:43:40+0200' } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location");



		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].dateProperty", equalTo("2013-04-05T08:43:40+0000"))
		.when()
			.get("/TestThree");

	}

	@Test
	public void testSearch() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'name'         : 'test'                     } ").expect().statusCode(201).when().post("/TestThree");

		// test for three elements
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(4))
		.when()
			.get("/TestThree");

		// test strict search
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].dateProperty", equalTo("2013-04-03T10:34:56+0000"))
		.when()
			.get("/TestThree?dateProperty=2013-04-03T10:34:56+0000");

		// test empty value
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].name", equalTo("test"))
		.when()
			.get("/TestThree?dateProperty=");
	}

	@Test
	public void testRangeSearch1() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");

		// test range query
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))
		.when()
			.get("/TestThree?dateProperty=[2013-04-03T10:34:56+0000 TO 2013-04-06T23:59:59+0000]");

	}

	@Test
	public void testRangeSearch2() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");

		// test range query
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(2))
		.when()
			.get("/TestThree?dateProperty=[2013-04-01T00:00:00+0000 TO 2013-04-06T23:59:59+0000]");

	}

	@Test
	public void testRangeSearch3() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");

		// range search pattern with null start
		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result_count", equalTo(2))
				.when()
				.get("/TestThree?dateProperty=[ TO 2013-04-06T23:59:59+0000]");
	}

	@Test
	public void testRangeSearch4() {

		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-03T10:34:56+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-05T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");
		RestAssured.given().contentType("application/json; charset=UTF-8").body(" { 'dateProperty' : '2013-04-07T08:43:40+0000' } ").expect().statusCode(201).when().post("/TestThree");

		// range search pattern with null end
		RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
				.when()
				.get("/TestThree?dateProperty=[2013-04-06T23:59:59+0000 TO ]");
	}

	@Test
	public void testPatchOnNestedResourceWithoutType() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema                 = StructrSchema.createFromDatabase(app);
			final JsonObjectType baseType           = schema.addType("BaseType");
			final JsonObjectType actualType         = schema.addType("ActualType");
			final JsonObjectType propertyBase       = schema.addType("PropertyBaseType");
			final JsonObjectType datePropertyType   = schema.addType("DateType");
			final JsonObjectType stringPropertyType = schema.addType("StringType");

			baseType.addDateProperty("validFrom", PropertyView.Public);
			baseType.addDateProperty("validUntil", PropertyView.Public);

			datePropertyType.addDateProperty("value", PropertyView.Public);
			stringPropertyType.addStringProperty("value", PropertyView.Public);

			actualType.addTrait("BaseType");
			propertyBase.addTrait("BaseType");
			datePropertyType.addTrait("PropertyBaseType");
			stringPropertyType.addTrait("PropertyBaseType");

			actualType.relate(propertyBase, "HAS_PROPERTY", Cardinality.OneToMany, "base", "properties");
			actualType.addViewProperty(PropertyView.Public, "properties");

			propertyBase.addViewProperty(PropertyView.Public, "base");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String type1   = createEntity("/ActualType", "{ name: 'ActualType' }");
		final String date1   = createEntity("/DateType", "{ name: 'DateType', base: '" + type1 + "', value: '2020-07-26T10:34:56+0000' }");
		final String string1 = createEntity("/StringType", "{ name: 'StringType', base: '" + type1 + "', value: 'A string' }");

		// verify setup
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].properties[0].value", equalTo("2020-07-26T10:34:56+0000"))
			.body("result[0].properties[1].value", equalTo("A string"))
		.when()
			.get("/ActualType");

		// create patch document
		final List<Map<String, Object>> data = List.of(
			Map.of(
				"id", type1,
				"properties", List.of(
					Map.of(
						"id", date1,
						"value", "2021-07-26T10:34:56+0000"
					),
					Map.of(
						"id", string1,
						"value", "Another string"
					)
				)
			)
		);

		// test
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(gson.toJson(data))
		.expect()
			.statusCode(200)
		.when()
			.patch("/ActualType");


		final String stringType = "StringType";
		final String dateType   = "DateType";

		// verify result
		try (final Tx tx = app.tx()) {

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(dateType).getResultStream()) {
				assertTrue("Date values must be of type Date", n.getProperty(n.getTraits().key("value")) instanceof Date);
			}

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(stringType).getResultStream()) {
				assertTrue("String values must be of type String", n.getProperty(n.getTraits().key("value")) instanceof String);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPatchOnNestedResourceWithType() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema                 = StructrSchema.createFromDatabase(app);
			final JsonObjectType baseType           = schema.addType("BaseType");
			final JsonObjectType actualType         = schema.addType("ActualType");
			final JsonObjectType propertyBase       = schema.addType("PropertyBaseType");
			final JsonObjectType datePropertyType   = schema.addType("DateType");
			final JsonObjectType stringPropertyType = schema.addType("StringType");

			baseType.addDateProperty("validFrom", PropertyView.Public);
			baseType.addDateProperty("validUntil", PropertyView.Public);

			datePropertyType.addDateProperty("value", PropertyView.Public);
			stringPropertyType.addStringProperty("value", PropertyView.Public);

			actualType.addTrait("BaseType");
			propertyBase.addTrait("BaseType");
			datePropertyType.addTrait("PropertyBaseType");
			stringPropertyType.addTrait("PropertyBaseType");

			actualType.relate(propertyBase, "HAS_PROPERTY", Cardinality.OneToMany, "base", "properties");
			actualType.addViewProperty(PropertyView.Public, "properties");

			propertyBase.addViewProperty(PropertyView.Public, "base");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String type1   = createEntity("/ActualType", "{ name: 'ActualType' }");
		final String date1   = createEntity("/DateType", "{ name: 'DateType', base: '" + type1 + "', value: '2020-07-26T10:34:56+0000' }");
		final String string1 = createEntity("/StringType", "{ name: 'StringType', base: '" + type1 + "', value: 'A string' }");

		// verify setup
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].properties[0].value", equalTo("2020-07-26T10:34:56+0000"))
			.body("result[0].properties[1].value", equalTo("A string"))
		.when()
			.get("/ActualType");

		// create patch document
		final List<Map<String, Object>> data = List.of(
			Map.of(
				"id", type1,
				"properties", List.of(
					Map.of(
						"id", date1,
						"type", "DateType",
						"value", "2021-07-26T10:34:56+0000"
					),
					Map.of(
						"id", string1,
						"type", "StringType",
						"value", "Another string"
					)
				)
			)
		);

		// test
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(gson.toJson(data))
		.expect()
			.statusCode(200)
		.when()
			.patch("/ActualType");


		final String stringType = "StringType";
		final String dateType   = "DateType";

		// verify result
		try (final Tx tx = app.tx()) {

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(dateType).getResultStream()) {
				assertTrue("Date values must be of type Date", n.getProperty(n.getTraits().key("value")) instanceof Date);
			}

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(stringType).getResultStream()) {
				assertTrue("String values must be of type String", n.getProperty(n.getTraits().key("value")) instanceof String);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPatchWithoutNesting() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema                 = StructrSchema.createFromDatabase(app);
			final JsonObjectType baseType           = schema.addType("BaseType");
			final JsonObjectType actualType         = schema.addType("ActualType");
			final JsonObjectType propertyBase       = schema.addType("PropertyBaseType");
			final JsonObjectType datePropertyType   = schema.addType("DateType");
			final JsonObjectType stringPropertyType = schema.addType("StringType");

			baseType.addDateProperty("validFrom", PropertyView.Public);
			baseType.addDateProperty("validUntil", PropertyView.Public);

			datePropertyType.addDateProperty("value", PropertyView.Public);
			stringPropertyType.addStringProperty("value", PropertyView.Public);

			actualType.addTrait("BaseType");
			propertyBase.addTrait("BaseType");
			datePropertyType.addTrait("PropertyBaseType");
			stringPropertyType.addTrait("PropertyBaseType");

			actualType.relate(propertyBase, "HAS_PROPERTY", Cardinality.OneToMany, "base", "properties");
			actualType.addViewProperty(PropertyView.Public, "properties");

			propertyBase.addViewProperty(PropertyView.Public, "base");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String type1   = createEntity("/ActualType", "{ name: 'ActualType' }");
		final String date1   = createEntity("/DateType", "{ name: 'DateType', base: '" + type1 + "', value: '2020-07-26T10:34:56+0000' }");
		final String string1 = createEntity("/StringType", "{ name: 'StringType', base: '" + type1 + "', value: 'A string' }");

		// verify setup
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].properties[0].value", equalTo("2020-07-26T10:34:56+0000"))
			.body("result[0].properties[1].value", equalTo("A string"))
		.when()
			.get("/ActualType");


		// create patch document
		final List<Map<String, Object>> data = List.of(
			Map.of(
				"id", date1,
				"value", "2021-07-26T10:34:56+0000"
			),
			Map.of(
				"id", string1,
				"value", "Another string"
			)
		);

		// test
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(gson.toJson(data))
		.expect()
			.statusCode(200)
		.when()
			.patch("/PropertyBaseType");

		final String stringType = "StringType";
		final String dateType   = "DateType";

		// verify result
		try (final Tx tx = app.tx()) {

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(dateType).getResultStream()) {
				assertTrue("Date values must be of type Date", n.getProperty(n.getTraits().key("value")) instanceof Date);
			}

			for (final NodeInterface n : StructrApp.getInstance().nodeQuery(stringType).getResultStream()) {
				assertTrue("String values must be of type String", n.getProperty(n.getTraits().key("value")) instanceof String);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPatchOnNestedResourceWithMultipleObjects() {

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema                 = StructrSchema.createFromDatabase(app);
			final JsonObjectType baseType           = schema.addType("BaseType");
			final JsonObjectType actualType         = schema.addType("ActualType");
			final JsonObjectType propertyBase       = schema.addType("PropertyBaseType");
			final JsonObjectType datePropertyType   = schema.addType("DateType");
			final JsonObjectType stringPropertyType = schema.addType("StringType");

			baseType.addDateProperty("validFrom", PropertyView.Public);
			baseType.addDateProperty("validUntil", PropertyView.Public);

			datePropertyType.addDateProperty("value", PropertyView.Public);
			stringPropertyType.addStringProperty("value", PropertyView.Public);

			actualType.addTrait("BaseType");
			propertyBase.addTrait("BaseType");
			datePropertyType.addTrait("PropertyBaseType");
			stringPropertyType.addTrait("PropertyBaseType");

			actualType.relate(propertyBase, "HAS_PROPERTY", Cardinality.OneToMany, "base", "properties").setCascadingCreate(JsonSchema.Cascade.sourceToTarget);
			actualType.addViewProperty(PropertyView.Public, "properties");

			propertyBase.addViewProperty(PropertyView.Public, "base");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String type1   = createEntity("/ActualType", "{ name: 'ActualType' }");
		final String date1   = createEntity("/DateType", "{ name: 'DateType', base: '" + type1 + "', value: '2020-07-26T10:34:56+0000' }");
		final String string1 = createEntity("/StringType", "{ name: 'StringType', base: '" + type1 + "', value: 'A string' }");

		// verify setup
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result_count", equalTo(1))
			.body("result[0].properties[0].value", equalTo("2020-07-26T10:34:56+0000"))
			.body("result[0].properties[1].value", equalTo("A string"))
		.when()
			.get("/ActualType");

		{
			// create patch document
			final List<Map<String, Object>> data = List.of(
				Map.of(
					"id", type1,
					"properties", List.of(
						Map.of(
							"id", date1,
							"type", "DateType",
							"value", "2021-07-26T10:34:56+0000"
						),
						Map.of(
							"id", string1,
							"type", "StringType",
							"value", "Another string"
						)
					)
				)
			);

			// test
			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(gson.toJson(data))
			.expect()
				.statusCode(200)
			.when()
				.patch("/ActualType");


			// verify setup
			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
				.body("result[0].properties[0].value", equalTo("2021-07-26T10:34:56+0000"))
				.body("result[0].properties[1].value", equalTo("Another string"))
			.when()
				.get("/ActualType");

			final String stringType = "StringType";
			final String dateType   = "DateType";

			// verify result
			try (final Tx tx = app.tx()) {

				for (final NodeInterface n : StructrApp.getInstance().nodeQuery(dateType).getResultStream()) {
					assertTrue("Date values must be of type Date", n.getProperty(n.getTraits().key("value")) instanceof Date);
				}

				for (final NodeInterface n : StructrApp.getInstance().nodeQuery(stringType).getResultStream()) {
					assertTrue("String values must be of type String", n.getProperty(n.getTraits().key("value")) instanceof String);
				}

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception");
			}
		}

		// second test, add a new property
		{
			// create patch document
			final List<Map<String, Object>> data = List.of(
				Map.of(
					"id", type1,
					"properties", List.of(
						Map.of(
							"id", date1,
							"value", "2021-07-26T10:34:56+0000"
						),
						Map.of(
							"type", "DateType",
							"value", "2022-08-22T10:00:00Z"
						),
						Map.of(
							"id", string1,
							"type", "StringType",
							"value", "Another string"
						)
					)
				)
			);

			// test
			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(gson.toJson(data))
			.expect()
				.statusCode(200)
			.when()
				.patch("/ActualType");


			// verify setup
			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(1))
				.body("result[0].properties[0].value", equalTo("2021-07-26T10:34:56+0000"))
				.body("result[0].properties[1].value", equalTo("Another string"))
			.when()
				.get("/ActualType");

			final String stringType = "StringType";
			final String dateType   = "DateType";

			// verify result
			try (final Tx tx = app.tx()) {

				for (final NodeInterface n : StructrApp.getInstance().nodeQuery(dateType).getResultStream()) {
					assertTrue("Date values must be of type Date", n.getProperty(n.getTraits().key("value")) instanceof Date);
				}

				for (final NodeInterface n : StructrApp.getInstance().nodeQuery(stringType).getResultStream()) {
					assertTrue("String values must be of type String", n.getProperty(n.getTraits().key("value")) instanceof String);
				}

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception");
			}
		}
	}
}
