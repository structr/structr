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
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class RestScriptingTest extends StructrRestTestBase {

	@Test
	public void testDateWrappingAndFormats() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("getDate",             "{ return new Date(); }").setIsStatic(true);
			type.addMethod("getNowJavascript",    "{ return $.now; }").setIsStatic(true);
			type.addMethod("getNowStructrscript", "now").setIsStatic(true);

			type.addMethod("test1", "{ return { test1: new Date(), test2: $.now, test3: $.Test.getDate(), test4: $.Test.getNowJavascript(), test5: $.Test.getNowStructrscript() }; }").setIsStatic(true);
			type.addMethod("test2", "{ return $.Test.test1(); }").setIsStatic(true);
			type.addMethod("test3", "{ return $.Test.test2(); }").setIsStatic(true);
			type.addMethod("test4", "{ return { test1: typeof new Date(), test2: typeof $.now, test3: typeof $.Test.getDate(), test4: typeof $.Test.getNowJavascript(), test5: typeof $.Test.getNowStructrscript() }; }").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String pattern = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}\\:[0-9]{2}\\:[0-9]{2}\\+[0-9]{4}";

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result.test1", matchesPattern(pattern))
			.body("result.test2", matchesPattern(pattern))
			.body("result.test3", matchesPattern(pattern))
			.body("result.test4", matchesPattern(pattern))
			.body("result.test5", matchesPattern(pattern))
			.statusCode(200)
			.when()
			.post("/Test/test1");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result.test1", matchesPattern(pattern))
			.body("result.test2", matchesPattern(pattern))
			.body("result.test3", matchesPattern(pattern))
			.body("result.test4", matchesPattern(pattern))
			.body("result.test5", matchesPattern(pattern))
			.statusCode(200)
			.when()
			.post("/Test/test2");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result.test1", matchesPattern(pattern))
			.body("result.test2", matchesPattern(pattern))
			.body("result.test3", matchesPattern(pattern))
			.body("result.test4", matchesPattern(pattern))
			.body("result.test5", matchesPattern(pattern))
			.statusCode(200)
			.when()
			.post("/Test/test3");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result.test1", equalTo("object"))
			.body("result.test2", equalTo("object"))
			.body("result.test3", equalTo("object"))
			.body("result.test4", equalTo("object"))
			.body("result.test5", equalTo("string"))
			.statusCode(200)
			.when()
			.post("/Test/test4");

	}

	@Test
	public void testRollbackFunctionWithCustomErrorResult() {

		// test setup, create a supernode with 10000 relationships
		try (final Tx tx = app.tx()) {

			// create test group
			JsonSchema schema       = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("API");
			final JsonMethod method = type.addMethod("doTest", "{ $.create('Group', { name: 'Test' }); $.rollbackTransaction(); return { errorCode: 42, obj1: { key1: 'value1', key2: 22, list: [ 1, 2, 3 ] } } }");

			method.setIsStatic(true);
			method.setReturnRawResult(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check that the method call has NOT created any groups
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result", hasSize(0))
			.body("result_count", equalTo(0))
			.body("page_count", equalTo(0))
			.statusCode(200)
			.when()
			.get("/Group");

		// check returned object (no "result" container)
		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("errorCode", equalTo(42))
			.body("obj1.key1", equalTo("value1"))
			.body("obj1.key2", equalTo(22))
			.body("obj1.list[0]", equalTo(1))
			.body("obj1.list[1]", equalTo(2))
			.body("obj1.list[2]", equalTo(3))
			.statusCode(200)
			.when()
			.post("/API/doTest");
	}
}
