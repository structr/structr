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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
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

			type.addMethod("getDate",             "{ new Date(); }").setIsStatic(true);
			type.addMethod("getNowJavascript",    "{ $.now; }").setIsStatic(true);
			type.addMethod("getNowStructrscript", "now").setIsStatic(true);

			type.addMethod("test1", "{ ({ test1: new Date(), test2: $.now, test3: $.Test.getDate(), test4: $.Test.getNowJavascript(), test5: $.Test.getNowStructrscript() }); }").setIsStatic(true);
			type.addMethod("test2", "{ $.Test.test1(); }").setIsStatic(true);
			type.addMethod("test3", "{ $.Test.test2(); }").setIsStatic(true);
			type.addMethod("test4", "{ ({ test1: typeof new Date(), test2: typeof $.now, test3: typeof $.Test.getDate(), test4: typeof $.Test.getNowJavascript(), test5: typeof $.Test.getNowStructrscript() }); }").setIsStatic(true);

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
			final JsonMethod method = type.addMethod("doTest", "{ $.create('Group', { name: 'Test' }); $.rollbackTransaction(); ({ errorCode: 42, obj1: { key1: 'value1', key2: 22, list: [ 1, 2, 3 ] } }) }");

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

	@Test
	public void testReturnRawResultDoesNotLeakWhenCallingMethodsFromGlobalMethod() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonType type = schema.addType("API");
			type.addMethod("test1", "'static structr script method'"        ).setIsStatic(true).setReturnRawResult(true);
			type.addMethod("test2", "{ 'static javascript method'; }").setIsStatic(true).setReturnRawResult(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("name"), "calledTestMethod"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"), "{ $.test1(); $.test2(); $.API.test1(); $.API.test2(); 'test'; }")
			);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("name"), "test1"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"), "'global structr script method'"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult"), true)
			);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("name"), "test2"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"), "{ 'global javascript method'; }"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult"), true)
			);

			tx.success();

		} catch (Throwable fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// ensure that called methods do not change returnRawResult behavior
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
				.body("result", equalTo("test"))
				.statusCode(200)
				.when()
				.post("/calledTestMethod");
	}

	@Test
	public void testReturnRawResultDoesNotLeakWhenCallingMethodsFromStaticMethod() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonType type = schema.addType("API");
			type.addMethod("calledTestMethod", "{ $.test1(); $.test2(); $.API.test1(); $.API.test2(); 'test'; }").setIsStatic(true);

			type.addMethod("test1", "'static structr script method'"        ).setIsStatic(true).setReturnRawResult(true);
			type.addMethod("test2", "{ 'static javascript method'; }").setIsStatic(true).setReturnRawResult(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("name"), "test1"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"), "'global structr script method'"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult"), true)
			);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("name"), "test2"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"), "{ 'global javascript method'; }"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult"), true)
			);

			tx.success();

		} catch (Throwable fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// ensure that called methods do not change returnRawResult behavior
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
				.body("result", equalTo("test"))
				.statusCode(200)
				.when()
				.post("/API/calledTestMethod");
	}
}
