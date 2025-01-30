/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.resource;

import io.restassured.RestAssured;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;


/**
 *
 *
 */
public class MeResourceTest extends StructrUiTest {

	@Test
	public void test01GET() {

		String uuid = null;

		// create 100 test nodes and set names
		try (final Tx tx = app.tx()) {

			final User user = app.create(User.class,
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "isAdmin"),  true)
			);

			uuid = user.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		// test default view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)
				.body("result.id",                          equalTo(uuid))
				.body("result.name",                        equalTo("admin"))
				.body("result.isUser",                      equalTo(true))
				.body("result.isAdmin",                     equalTo(null))
				.body("result.visibleToPublicUsers",        equalTo((Object)null))
				.body("result.visibleToAuthenticatedUsers", equalTo((Object)null))
				.body("result.password",                    equalTo((Object)null))

			.when()

				.get("/me");

		// test ui view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)
				.body("result.id",                          equalTo(uuid))
				.body("result.name",                        equalTo("admin"))
				.body("result.isUser",                      equalTo(true))
				.body("result.isAdmin",                     equalTo(true))
				.body("result.visibleToPublicUsers",        equalTo(false))
				.body("result.visibleToAuthenticatedUsers", equalTo(false))
				.body("result.password",                    equalTo("****** HIDDEN ******"))

			.when()

				.get("/me/ui");
	}

	@Test
	public void test02PUT() {

		String uuid = null;

		// create 100 test nodes and set names
		try (final Tx tx = app.tx()) {

			final User user = app.create(User.class,
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "isAdmin"),  true)
			);

			uuid = user.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)
				.body("result.id",      equalTo(uuid))
				.body("result.name",    equalTo("admin"))
				.body("result.isUser",  equalTo(true))
				.body("result.isAdmin", equalTo(true))
				.body("result.eMail",   equalTo(null))

			.when()

				.get("/me/ui");

		// change a value
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")
				.body("{ eMail: 'tester@test.com' }")

			.expect()
				.statusCode(200)

			.when()

				.put("/me");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)
				.body("result.id",      equalTo(uuid))
				.body("result.name",    equalTo("admin"))
				.body("result.isUser",  equalTo(true))
				.body("result.isAdmin", equalTo(true))
				.body("result.eMail",   equalTo("tester@test.com"))

			.when()

				.get("/me/ui");

		// change a value again, this time with a view in the URL
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")
				.body("{ eMail: 'tester2@test.com' }")

			.expect()
				.statusCode(200)

			.when()

				.put("/me/all");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)
				.body("result.id",      equalTo(uuid))
				.body("result.name",    equalTo("admin"))
				.body("result.isUser",  equalTo(true))
				.body("result.isAdmin", equalTo(true))
				.body("result.eMail",   equalTo("tester2@test.com"))

			.when()

				.get("/me/ui");
	}

	@Test
	public void test03POST() {

		String uuid = null;

		// create 100 test nodes and set names
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType("User");

			// method must be exported
			type.addMethod("doTest", "{ return { message: 'success', parameters: $.args }; }").setDoExport(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			final User user = app.create(User.class,
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "isAdmin"),  true)
			);

			uuid = user.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		// test simple method call
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")
				.body("{ value1: test, value2: 32 }")

			.expect()
				.statusCode(200)
				.body("result.message",           equalTo("success"))
				.body("result.parameters.value1", equalTo("test"))
				.body("result.parameters.value2", equalTo(32))

			.when()

				.post("/me/doTest");


		// test method call with view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")
				.body("{ value1: test, value2: 32 }")

			.expect()
				.statusCode(200)
				.body("result.message",           equalTo("success"))
				.body("result.parameters.value1", equalTo("test"))
				.body("result.parameters.value2", equalTo(32))

			.when()

				.post("/me/doTest/ui");

	}

	@Test
	public void test04DELETE() {

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(PrincipalInterface.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		// we actually allow users to delete themselves via /me (if a grant exists)
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(200)

			.when()

				.delete("/me");

		// we actually allow users to delete themselves via /me (if a grant exists)
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("x-user", "admin", "x-password", "admin")

			.expect()
				.statusCode(401)

			.when()

				.get("/me");
	}
}
