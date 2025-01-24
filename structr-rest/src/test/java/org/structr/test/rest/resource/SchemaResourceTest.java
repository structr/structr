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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.config.Settings;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 *
 *
 */
public class SchemaResourceTest extends StructrRestTestBase {

	@Test
	public void testCustomSchema0() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType0\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].type", equalTo("String"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].declaringClass", equalTo("TestType0"))

			.when()
				.get("/_schema/TestType0/custom");

	}

	@Test
	public void testCustomSchema1() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType1\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"dbName\": \"fooDb\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].type", equalTo("String"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("fooDb"))
				.body("result[3].declaringClass", equalTo("TestType1"))

			.when()
				.get("/_schema/TestType1/custom");

	}

	@Test
	public void testCustomSchema2() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType2\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result",	      hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].type", equalTo("String"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType2/custom");

	}

	@Test
	public void testCustomSchema3() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType3\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].unique", equalTo(true))

			.when()
				.get("/_schema/TestType3/custom");

	}

	@Test
	public void testCustomSchema4() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType4\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].unique", equalTo(true))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType4/custom");

	}

	@Test
	public void testCustomSchema5() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType5\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"format\": \"bar\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].format", equalTo("bar"))

			.when()
				.get("/_schema/TestType5/custom");

	}

	@Test
	public void testCustomSchema6() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType6\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true, \"format\": \"bar\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].unique", equalTo(true))
				.body("result[3].format", equalTo("bar"))

			.when()
				.get("/_schema/TestType6/custom");

	}

	@Test
	public void testCustomSchema7() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType7\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"contentType\": \"text/html\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))

			.when()
				.get("/_schema/TestType7/custom");

	}

	@Test
	public void testCustomSchema8() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType8\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true, \"contentType\": \"text/html\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].unique", equalTo(true))

			.when()
				.get("/_schema/TestType8/custom");

	}

	@Test
	public void testCustomSchema9() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType9\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true, \"notNull\": true, \"contentType\": \"text/html\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType9/custom");

	}


	@Test
	public void testCustomSchema10() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType10\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"notNull\": true, \"contentType\": \"text/html\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType10/custom");

	}

	@Test
	public void testCustomSchema11() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType11\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"unique\": true, \"notNull\": true, \"format\": \"[a-f0-9]{32}\", \"contentType\": \"text/html\", \"defaultValue\": \"xyz\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("[a-f0-9]{32}"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType11/custom");

	}

	@Test
	public void testCustomSchema12() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType12\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Date\", \"unique\": true, \"notNull\": true, \"format\": \"yyyy-MM-dd\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].type", equalTo("Date"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("yyyy-MM-dd"))

			.when()
				.get("/_schema/TestType12/custom");

	}

	@Test
	public void testCustomSchema13() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType13\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"dbName\": \"fooDb\", \"contentType\": \"text/html\", \"notNull\": true, \"format\": \"[a-f0-9]{32}\", \"unique\": true, \"defaultValue\": \"xyz\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("fooDb"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("[a-f0-9]{32}"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType13/custom");

	}

	@Test
	public void testCustomSchema14() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType14\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"dbName\": \"fooDb\", \"contentType\": \"text/html\", \"format\": \"multi-line\", \"notNull\": true, \"defaultValue\": \"xyz\", \"unique\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("fooDb"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("multi-line"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType14/custom");

	}

	@Test
	public void testCustomSchema15() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType15\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"dbName\": \"fooDb\", \"contentType\": \"text/html\", \"format\": \"some-format with | pipe in it\", \"notNull\": true, \"defaultValue\": \"xyz\", \"unique\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("fooDb"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("some-format with | pipe in it"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType15/custom");

	}


	@Test
	public void testCustomSchema16() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType16\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"contentType\": \"text/html\", \"format\": \"some-format with no pipe in it\", \"notNull\": true, \"defaultValue\": \"xyz\", \"unique\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("some-format with no pipe in it"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType16/custom");

	}

	@Test
	public void testCustomSchema17() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType17\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"String\", \"contentType\": \"text/html\", \"format\": \"some-format with a | pipe in it\", \"notNull\": true, \"defaultValue\": \"xyz\", \"unique\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].contentType", equalTo("text/html"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("some-format with a | pipe in it"))
				.body("result[3].defaultValue", equalTo("xyz"))

			.when()
				.get("/_schema/TestType17/custom");

	}

	@Test
	public void testCustomSchema18() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType18\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Date\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Date"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType18/custom");

	}

	@Test
	public void testCustomSchema19() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType19\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Date\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true, \"format\": \"yyyy-MM-dd\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Date"))
				.body("result[3].notNull", equalTo(true))
				.body("result[3].format", equalTo("yyyy-MM-dd"))

			.when()
				.get("/_schema/TestType19/custom");

	}

	@Test
	public void testCustomSchema20() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType20\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Boolean\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Boolean"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType20/custom");

	}

	@Test
	public void testCustomSchema21() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType21\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Double\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Double"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType21/custom");

	}

	@Test
	public void testCustomSchema22() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType22\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Enum\", \"format\": \"a,b,c\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("foo"))
				.body("result[3].type", equalTo("Enum"))
				.body("result[3].format", equalTo("a,b,c"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType22/custom");

	}

	@Test
	public void testCustomSchema23() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType23\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Enum\", \"dbName\": \"Foo\", \"format\": \"a,b,c\", \"unique\": true, \"notNull\": true } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Enum"))
				.body("result[3].format", equalTo("a,b,c"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType23/custom");

	}

	@Test
	public void testCustomSchema24() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType24\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Enum\", \"dbName\": \"Foo\", \"format\": \"a,b,c\", \"unique\": true, \"notNull\": true, \"defaultValue\": \"b\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Enum"))
				.body("result[3].format", equalTo("a,b,c"))
				.body("result[3].defaultValue", equalTo("b"))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType24/custom");

	}

	@Test
	public void testCustomSchema25() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType25\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Boolean\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true, \"defaultValue\": \"true\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Boolean"))
				.body("result[3].defaultValue", equalTo(true))
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType25/custom");

	}

	@Test
	public void testCustomSchema26() {

		createEntity("/SchemaNode", "{ \"name\": \"TestType26\", \"schemaProperties\": [ { \"name\": \"foo\", \"propertyType\": \"Double\", \"dbName\": \"Foo\", \"unique\": true, \"notNull\": true, \"defaultValue\": \"12.34\" } ] }");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result", hasSize(4))
				.body("result_count", equalTo(4))
				.body("result[0].type", equalTo("String"))
				.body("result[0].jsonName", equalTo("id"))
				.body("result[0].dbName", equalTo("id"))
				.body("result[0].declaringClass", equalTo("GraphObject"))
				.body("result[1].type", equalTo("String"))
				.body("result[1].jsonName", equalTo("type"))
				.body("result[1].dbName", equalTo("type"))
				.body("result[1].declaringClass", equalTo("GraphObject"))
				.body("result[2].type", equalTo("String"))
				.body("result[2].jsonName", equalTo("name"))
				.body("result[2].dbName", equalTo("name"))
				.body("result[2].declaringClass", equalTo("NodeInterface"))
				.body("result[3].jsonName", equalTo("foo"))
				.body("result[3].dbName", equalTo("Foo"))
				.body("result[3].type", equalTo("Double"))
				.body("result[3].defaultValue", equalTo(12.34f)) // The restassured lib parses floating-point numbers to Float
				.body("result[3].notNull", equalTo(true))

			.when()
				.get("/_schema/TestType26/custom");

	}

	@Test
	public void testSchemaMethodExecution() {

		createEntity("/SchemaNode", "{ name: Test, schemaViews: [ { name: public, nonGraphProperties: \"name, type\" } ], schemaMethods: [ { name: test, source: \"find('Test')\" } ] }");
		final String uuid = createEntity("Test", "{ name: Test }");

		// default setting for "force arrays" is false..
		Settings.ForceArrays.setValue(false);

		// trying to execute a non-static method via static path should result in 404
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(404)

			.when()
				.post("/Test/test");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result[0].type", equalTo("Test"))
				.body("result[0].name", equalTo("Test"))

			.when()
				.post("/Test/" + uuid + "/test");

		// test with true as well
		Settings.ForceArrays.setValue(true);

		// trying to execute a non-static method via static path should result in 404
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(404)

			.when()
				.post("/Test/test");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result[0][0].type", equalTo("Test"))
				.body("result[0][0].name", equalTo("Test"))

			.when()
				.post("/Test/" + uuid + "/test");
	}

	@Test
	public void testInheritedSchemaMethodExecution() {

		final String uuid1 = createEntity("/SchemaNode", "{ name: TestBase1, schemaMethods: [ { name: test, source: \"find('Test1')\" } ] }");
		final String uuid2 = createEntity("/SchemaNode", "{ name: TestBase2, schemaMethods: [ { name: test, source: \"find('Test2')\" } ] }");

		createEntity("/SchemaNode", "{ name: Test1, schemaViews: [ { name: public, nonGraphProperties: \"name, type\" } ], inheritedTraits: [ TestBase1 ] }");

		final String test11 = createEntity("Test1", "{ name: Test1 }");

		// wait a little bit so the sort order is correct when fetching nodes
		try { Thread.sleep(10); } catch (Throwable t) {}

		final String test12 = createEntity("Test1", "{ name: Test2 }");

		createEntity("/SchemaNode", "{ name: Test2, schemaViews: [ { name: public, nonGraphProperties: \"name, type\" } ], inheritedTraits: [ TestBase2 ] }");
		final String test2 = createEntity("Test2", "{ name: Test1 }");

		// default setting for "force arrays" is false..
		Settings.ForceArrays.setValue(false);

		// test 404 error for non-static entity methods
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(404)

			.when()
				.post("/Test1/test");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(2))
				.body("result[0].type", equalTo("Test1"))
				.body("result[0].name", equalTo("Test1"))
				.body("result[1].type", equalTo("Test1"))
				.body("result[1].name", equalTo("Test2"))

			.when()
				.post("/Test1/" + test11 + "/test");

		// test 404 on non-static method
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(404)

			.when()
				.post("/Test2/test");

		RestAssured

				.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

				.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result[0].type", equalTo("Test2"))
				.body("result[0].name", equalTo("Test1"))

				.when()
				.post("/Test2/" + test2 + "/test");

		// test with true as well
		Settings.ForceArrays.setValue(true);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result_count", equalTo(1))
				.body("result[0][0].type", equalTo("Test2"))
				.body("result[0][0].name", equalTo("Test1"))

			.when()
				.post("/Test2/" + test2 + "/test");
	}

	@Test
	public void testSchemaResourceResponseForAdminUi() {

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				//.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result_count",                  equalTo(1))
				.body("result",	                    hasSize(1))
				.body("result[0].url",                 equalTo("/Group"))
				.body("result[0].type",                equalTo("Group"))
				.body("result[0].className",           equalTo("Group"))
				.body("result[0].traits",                   equalTo(List.of("PropertyContainer", "GraphObject", "NodeInterface", "AccessControllable", "Principal", "Group")))
				.body("result[0].isRel",                    equalTo(false))
				.body("result[0].flags",                    equalTo(0))

				.body("result[0].views.all.id.jsonName",    equalTo("id"))
				.body("result[0].views.all.id.className",   equalTo("org.structr.core.property.UuidProperty"))
				.body("result[0].views.all.type.jsonName",  equalTo("type"))
				.body("result[0].views.all.type.className", equalTo("org.structr.core.property.TypeProperty"))
				.body("result[0].views.all.name.jsonName",  equalTo("name"))
				.body("result[0].views.all.name.className", equalTo("org.structr.core.property.StringProperty"))

				.body("result[0].views.ui.id.jsonName",    equalTo("id"))
				.body("result[0].views.ui.id.className",   equalTo("org.structr.core.property.UuidProperty"))
				.body("result[0].views.ui.type.jsonName",  equalTo("type"))
				.body("result[0].views.ui.type.className", equalTo("org.structr.core.property.TypeProperty"))
				.body("result[0].views.ui.name.jsonName",  equalTo("name"))
				.body("result[0].views.ui.name.className", equalTo("org.structr.core.property.StringProperty"))

				.body("result[0].views.public.id.jsonName",    equalTo("id"))
				.body("result[0].views.public.id.className",   equalTo("org.structr.core.property.UuidProperty"))
				.body("result[0].views.public.type.jsonName",  equalTo("type"))
				.body("result[0].views.public.type.className", equalTo("org.structr.core.property.TypeProperty"))
				.body("result[0].views.public.name.jsonName",  equalTo("name"))
				.body("result[0].views.public.name.className", equalTo("org.structr.core.property.StringProperty"))

			.when()
				.get("/_schema/Group");

	}
}
