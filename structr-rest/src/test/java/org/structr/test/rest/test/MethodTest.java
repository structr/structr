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
package org.structr.test.rest.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class MethodTest extends StructrRestTestBase {

	@Test
	public void testMethodInheritance() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");
			final JsonObjectType ext1  = schema.addType("Extended1");
			final JsonObjectType ext11 = schema.addType("Extended11");
			final JsonObjectType ext2  = schema.addType("Extended2");

			ext1.setExtends(base);
			ext2.setExtends(base);

			// two levels
			ext11.setExtends(ext1);

			// methods
			base.addMethod("doTest", "'BaseType'");
			base.addMethod("doBase", "'BaseType'");
			ext1.addMethod("doTest", "'Extended1'");
			ext11.addMethod("doTest", "'Extended11'");
			ext2.addMethod("doTest", "'Extended2'");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType",   "{ name: 'BaseType' }");
		final String ext1  = createEntity("/Extended1",  "{ name: 'Extended1' }");
		final String ext11 = createEntity("/Extended11", "{ name: 'Extended11' }");
		final String ext2  = createEntity("/Extended2",  "{ name: 'Extended2' }");

		try (final Tx tx = app.tx()) {

			for (final String type : List.of("BaseType", "Extended1", "Extended11", "Extended2")) {

				System.out.println("##################################################### " + type);
				System.out.println(StructrApp.getInstance().nodeQuery(SchemaNode.class).andName(type).getFirst().getGeneratedSourceCode(securityContext));

				tx.success();
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// test inherited methods
		assertEquals("Invalid inheritance result, overriding method is not called", "BaseType", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + base + "/doTest")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, overriding method is not called", "Extended1", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext1 + "/doTest")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, overriding method is not called", "Extended11", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext11 + "/doTest")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, overriding method is not called", "Extended2", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext2 + "/doTest")
			.body().as(Map.class).get("result")
		);

		// test base methods
		assertEquals("Invalid inheritance result, base method is not called", "BaseType", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + base + "/doBase")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, base method is not called", "BaseType", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext1 + "/doBase")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, base method is not called", "BaseType", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext11 + "/doBase")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid inheritance result, base method is not called", "BaseType", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + ext2 + "/doBase")
			.body().as(Map.class).get("result")
		);

	}

	@Test
	public void testAllLowercaseMethods() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test", "'BaseType: test'");
			base.addMethod("base", "'BaseType: base'");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType",   "{ name: 'BaseType' }");

		// test inherited methods
		assertEquals("Invalid method call result, lower-case method is not called", "BaseType: test", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + base + "/test")
			.body().as(Map.class).get("result")
		);

		assertEquals("Invalid method call result, lower-case method is not called", "BaseType: base", RestAssured.given().contentType("application/json; charset=UTF-8")
			.expect().statusCode(200)
			.when().post("/BaseType/" + base + "/base")
			.body().as(Map.class).get("result")
		);
	}

	@Test
	public void testParameterPassing() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test1", "{ return $.this.test2($.methodParameters); }");
			base.addMethod("test2", "{ return $.methodParameters; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		// test inherited methods
		final Map map = (Map)RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ name: 'Test', key: 3, map: { m: 'test' }, set: [1, 2, 3], list: [{ id: 25, name: 'aaa' }] }")
			.expect()
				.statusCode(200)
			.when()
				.post("/BaseType/" + base + "/test1")

			.body().as(Map.class).get("result");

		//assert that the method parameters are passed through internally
		assertEquals("Test",  map.get("name"));
		assertEquals(3,       map.get("key"));
		assertEquals("test",  ((Map)map.get("map")).get("m"));
		assertEquals(1,       ((List)map.get("set")).get(0));
		assertEquals(2,       ((List)map.get("set")).get(1));
		assertEquals(3,       ((List)map.get("set")).get(2));
		assertEquals(25,      ((Map)((List)map.get("list")).get(0)).get("id"));
		assertEquals("aaa",   ((Map)((List)map.get("list")).get(0)).get("name"));
	}

	@Test
	public void testRESTParameterConversionInJavascriptMethod() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// first method with parameter definition => input should be converted to Date
			base.addMethod("test1", "{ return { date: $.args.date, isDate: $.args.date instanceof Date };}").addParameter("date", "Date");

			// second method without parameter definition => input should not be converted (result: string)
			base.addMethod("test2", "{ return { date: $.args.date, isDate: $.args.date instanceof Date };}");

			// third method calls first, but input should not be converted because it doesn't come from a REST call
			base.addMethod("test3", "{ return $.this.test1({ date: new Date(2022, 0, 1) }); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		// test call via REST, date must be converted
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ date: '2024-01-01T00:00:00+0000' }")
			.expect()
				.statusCode(200)
				.body("result.date", equalTo("Mon Jan 01 00:00:00 UTC 2024"))
				.body("result.isDate", equalTo(true))
			.when()
				.post("/BaseType/" + base + "/test1");

		// test call via Javascript, date is not sent via REST
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ date: '2024-01-01T00:00:00+0000' }")
			.expect()
				.statusCode(200)
				.body("result.date",   equalTo("2024-01-01T00:00:00+0000"))
				.body("result.isDate", equalTo(false))
			.when()
				.post("/BaseType/" + base + "/test2");

		// test call via Javascript, date is not sent via REST
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result.date",   equalTo("2022-01-01T00:00:00+0000"))
				.body("result.isDate", equalTo(true))
			.when()
				.post("/BaseType/" + base + "/test3");
	}

	@Test
	public void testRESTParameterConversionInStructrScriptMethod() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test", "methodParameters")
				.addParameter("name", "String")
				.addParameter("key", "int")
				.addParameter("map", "Map")
				.addParameter("set", "Set")
				.addParameter("list", "List")
				.addParameter("date", "Date");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		// FIXME: this test doesn't test anything yet..

		// test inherited methods
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ data: '2023-01-05T22:00:00+0000', name: 'Test', key: 3, map: { m: 'test' }, set: [1, 2, 3], list: [{ id: 25, name: 'aaa' }] }")
			.expect()
				.statusCode(200)
			.when()
				.post("/BaseType/" + base + "/test");
	}

	@Test
	public void testJavaArgumentPassing1() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			base.addMethod("testJavaArgumentPassing")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.addParameter("message", String.class.getName())
				.setSource("return new RestMethodResult(200, topic + \", \" + message);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ topic: 'topic', message: 'message' }")
			.expect()
				.statusCode(200)
				.body("message", equalTo("topic, message"))
			.when()
				.post("/BaseType/" + base + "/testJavaArgumentPassing");
	}

	@Test
	public void testJavaArgumentPassing2() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			base.addMethod("testJavaArgumentPassing")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.addParameter("message", String.class.getName())
				.setSource("return new RestMethodResult(200, topic + \", \" + message);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

			// define method without parameters so it must be called with a map argument
			base.addMethod("doTest1", "{ return $.this.testJavaArgumentPassing('topic', 'message'); }");

			// define method with parameters so it can be called with unnamed arguments
			base.addMethod("doTest2", "{ return $.this.testJavaArgumentPassing('topic', 'message'); }")
				.addParameter("topic", "String")
				.addParameter("message", "String");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String uuid  = createEntity("/BaseType", "{ name: 'BaseType' }");

		try (final Tx tx = app.tx()) {

			final Class type       = StructrApp.getConfiguration().getNodeEntityClass("BaseType");
			final GraphObject node = app.get(type, uuid);

			try {

				Scripting.evaluate(new ActionContext(securityContext), node, "${{ return $.this.doTest1('topic', 'message'); }}", "test script that expects an exception");

				fail("Calling a method with illegal arguments should throw an exception.");

			} catch (FrameworkException fex) {
				// success
			}

			{
				// test doTest1 with map-based arguments (should succeed)
				final Object value = Scripting.evaluate(new ActionContext(securityContext), node, "${{ return $.this.doTest1({ topic: 'topic', message: 'message' }); }}", "test script that expects success");
				assertTrue("Invalid method result", value instanceof RestMethodResult);
				final RestMethodResult result = (RestMethodResult)value;
				assertEquals("Invalid method result", "topic, message", result.getMessage());
			}

			{
				// test doTest2 with unnamed arguments (should succeed)
				final Object value = Scripting.evaluate(new ActionContext(securityContext), node, "${{ return $.this.doTest2('topic', 'message'); }}", "test script that expects success");
				assertTrue("Invalid method result", value instanceof RestMethodResult);
				final RestMethodResult result = (RestMethodResult)value;
				assertEquals("Invalid method result", "topic, message", result.getMessage());
			}


			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMethodParametersObject() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test1", "{ return $.methodParameters; }");
			base.addMethod("test2", "{ return $.args; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ key1: 'value1', key2: 2 }")
			.expect()
				.statusCode(200)
				.body("result.key1", equalTo("value1"))
				.body("result.key2", equalTo(2))
			.when()
				.post("/BaseType/" + base + "/test1");

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ key1: 'value1', key2: 2 }")
			.expect()
				.statusCode(200)
				.body("result.key1", equalTo("value1"))
				.body("result.key2", equalTo(2))
			.when()
				.post("/BaseType/" + base + "/test2");
	}

	@Test
	public void testHttpVerbs() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("doGet",    "{ return 'get'; }").setHttpVerb("GET");
			base.addMethod("doPut",    "{ return 'put'; }").setHttpVerb("PUT");
			base.addMethod("doPost",   "{ return 'post'; }").setHttpVerb("POST");
			base.addMethod("doPatch",  "{ return 'patch'; }").setHttpVerb("PATCH");
			base.addMethod("doDelete", "{ return 'delete'; }").setHttpVerb("DELETE");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base = createEntity("/BaseType", "{ name: 'BaseType' }");

		RestAssured.expect().statusCode(200).body("result", equalTo("get")).when().get("/BaseType/" + base + "/doGet");
		RestAssured.expect().statusCode(405).when().put("/BaseType/" + base + "/doGet");
		RestAssured.expect().statusCode(405).when().post("/BaseType/" + base + "/doGet");
		RestAssured.expect().statusCode(405).when().patch("/BaseType/" + base + "/doGet");
		RestAssured.expect().statusCode(405).when().delete("/BaseType/" + base + "/doGet");

		RestAssured.expect().statusCode(405).when().get("/BaseType/" + base + "/doPut");
		RestAssured.expect().statusCode(200).body("result", equalTo("put")).when().put("/BaseType/" + base + "/doPut");
		RestAssured.expect().statusCode(405).when().post("/BaseType/" + base + "/doPut");
		RestAssured.expect().statusCode(405).when().patch("/BaseType/" + base + "/doPut");
		RestAssured.expect().statusCode(405).when().delete("/BaseType/" + base + "/doPut");

		RestAssured.expect().statusCode(405).when().get("/BaseType/" + base + "/doPost");
		RestAssured.expect().statusCode(405).when().put("/BaseType/" + base + "/doPost");
		RestAssured.expect().statusCode(200).body("result", equalTo("post")).when().post("/BaseType/" + base + "/doPost");
		RestAssured.expect().statusCode(405).when().patch("/BaseType/" + base + "/doPost");
		RestAssured.expect().statusCode(405).when().delete("/BaseType/" + base + "/doPost");

		RestAssured.expect().statusCode(405).when().get("/BaseType/" + base + "/doPatch");
		RestAssured.expect().statusCode(405).when().put("/BaseType/" + base + "/doPatch");
		RestAssured.expect().statusCode(405).when().post("/BaseType/" + base + "/doPatch");
		RestAssured.expect().statusCode(200).body("result", equalTo("patch")).when().patch("/BaseType/" + base + "/doPatch");
		RestAssured.expect().statusCode(405).when().delete("/BaseType/" + base + "/doPatch");

		RestAssured.expect().statusCode(405).when().get("/BaseType/" + base + "/doDelete");
		RestAssured.expect().statusCode(405).when().put("/BaseType/" + base + "/doDelete");
		RestAssured.expect().statusCode(405).when().post("/BaseType/" + base + "/doDelete");
		RestAssured.expect().statusCode(405).when().patch("/BaseType/" + base + "/doDelete");
		RestAssured.expect().statusCode(200).body("result", equalTo("delete")).when().delete("/BaseType/" + base + "/doDelete");
	}


	@Test
	public void testGETMethodParametersInURL() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test1", "{ return $.methodParameters; }")
				.addParameter("key1", "String")
				.addParameter("key2", "Integer")
				.setHttpVerb("GET");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result.key1", equalTo("value1"))
				.body("result.key2", equalTo(2))
			.when()
				.get("/BaseType/" + base + "/test1/value1/2");

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(422)
				.body("code",                equalTo(422))
				.body("message",             equalTo("Cannot parse input for ‛key2‛ in method ‛BaseType.test1‛"))
				.body("errors[0].method",    equalTo("test1"))
				.body("errors[0].parameter", equalTo("key2"))
				.body("errors[0].token",     equalTo("must_be_numerical"))
				.body("errors[0].value",     equalTo("two"))
			.when()
				.get("/BaseType/" + base + "/test1//two");
	}

}
