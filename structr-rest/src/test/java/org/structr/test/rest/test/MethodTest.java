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
import java.util.List;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.Map;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

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

			// methods
			base.addMethod("test", """
{
	$.log('Test: ', $.methodParameters.name);
	return {
                name: $.methodParameters.name,
                key: $.methodParameters.key,
                map: $.methodParameters.map,
                set: $.methodParameters.set,
                list: $.methodParameters.list,
                date: $.methodParameters.date,
                test: typeof $.methodParameters.date
        };
}
                        """)
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
				.body("{ date: '2023-01-05T22:00:00+0000', name: 'Test', key: 3, map: { m: 'test' }, set: [1, 2, 3], list: [{ id: 25, name: 'aaa' }] }")
			.expect()
				.statusCode(200)
			.when()
				.post("/BaseType/" + base + "/test");
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
}
