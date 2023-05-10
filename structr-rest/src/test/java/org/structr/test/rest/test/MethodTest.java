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
package org.structr.test.rest.test;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class MethodTest extends StructrRestTestBase {

	private static final Logger logger = LoggerFactory.getLogger(MethodTest.class.getName());

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
}
