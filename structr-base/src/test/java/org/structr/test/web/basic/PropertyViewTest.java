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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.Matchers.*;

public class PropertyViewTest extends StructrUiTest {

	@Test
	public void testResourceAccessGrants() {

		final String username = "tester";
		final String password = "test";

		// create initial user
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, "superadmin")
				.header(X_PASSWORD_HEADER, "sehrgeheim")
				.body(" { 'name' : '" + username + "', 'password': '" + password + "' } ")

			.expect()
				.statusCode(201)

			.when()
				.post("/User");

		// create resource access objects
		/*
			#   FORBIDDEN             = 0
			#   AUTH_USER_GET         = 1
			#   AUTH_USER_PUT         = 2
			#   AUTH_USER_POST        = 4
			#   AUTH_USER_DELETE      = 8
			#   NON_AUTH_USER_GET     = 16
			#   NON_AUTH_USER_PUT     = 32
			#   NON_AUTH_USER_POST    = 64
			#   NON_AUTH_USER_DELETE  = 128


			caution: we're only testing resource ACCESS here, so the
			expected response code of 405 for PUT is correct because
			we don't supply a correct resource URL, this test is only
			about having sufficient permissions to cause a 405 error.
		*/

		String resource = "/TestOne";

		// first: test failures without resource access object
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags 0
		grant("TestOne", 0, true);

		// failures with flags == 0
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags 1
		grant("TestOne", 1, true);

		// failures with flags == 1 (AUTH_USER_GET)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    200);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags 2
		grant("TestOne", 2, true);

		// failures with flags == 2 (AUTH_USER_PUT)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 405);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 4
		grant("TestOne", 4, true);

		// failures with flags == 4 (AUTH_USER_POST)
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 201);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 8
		grant("TestOne", 8, true);

		// failures with flags == 8 (AUTH_USER_DELETE)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    200);

		// grant with flags == 16
		grant("TestOne", 16, true);

		// failures with flags == 16 (NON_AUTH_USER_GET)
		testGet(   resource,                                        200);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 32
		grant("TestOne", 32, true);

		// failures with flags == 32 (NON_AUTH_USER_PUT)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource,         "{'name':'test'}",             405);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 64
		grant("TestOne", 64, true);

		// failures with flags == 64 (NON_AUTH_USER_POST)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource,         "{'name':'test'}",             201);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 128
		grant("TestOne", 128, true);

		// failures with flags == 128 (NON_AUTH_USER_DELETE)
		testGet(   resource,                                        401);
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource,                                        200);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);


	}

	@Test
	public void testPropertyViewsAndResultSetLayoutWeb() {

		final String username = "tester";
		final String password = "test";

		// create initial user
		final String userId = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, "superadmin")
				.header(X_PASSWORD_HEADER, "sehrgeheim")
				.body(" { 'name' : '" + username + "', 'password': '" + password + "' } ")

			.expect()
				.statusCode(201)

			.when()
				.post("/User").header("Location"));

		// create resource access objects
		/*
			#   FORBIDDEN             = 0
			#   AUTH_USER_GET         = 1
			#   AUTH_USER_PUT         = 2
			#   AUTH_USER_POST        = 4
			#   AUTH_USER_DELETE      = 8
			#   NON_AUTH_USER_GET     = 16
			#   NON_AUTH_USER_PUT     = 32
			#   NON_AUTH_USER_POST    = 64
			#   NON_AUTH_USER_DELETE  = 128

		*/

		String resource = "/TestOne";

		// grant GET and POST for authenticated users
		grant("TestOne", 5, true);
		grant("TestOne/_All", 1, false);
		grant("TestOne/_Ui", 1, false);
		grant("Page", 5, false);
		grant("Page/_Ui", 1, false);
		grant("Page/_Html", 1, false);


		// create entity
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final Date testDate = new Date(112, 9, 18, 0, 33, 2);
        final String expectedDate = format.format(testDate);
		final String uuid = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, username)
				.header(X_PASSWORD_HEADER, password)
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'TestOne-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '" + expectedDate + "' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location")
		);

		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, username)
				.header(X_PASSWORD_HEADER, password)
				.header("Accept", "application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("query_time",                 notNullValue())
				.body("serialization_time",         notNullValue())
				.body("result_count",               equalTo(1))
				.body("result",                     hasSize(1))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo("TestOne"))
				.body("result[0].name",             equalTo("TestOne-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo(expectedDate))

			.when()
				.get(resource);

		// test all view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, username)
				.header(X_PASSWORD_HEADER, password)
				.header("Accept", "application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0].id",                          equalTo(uuid))
				.body("result[0].type",	                       equalTo("TestOne"))
				.body("result[0].name",                        equalTo("TestOne-0"))
				.body("result[0].anInt",                       equalTo(0))
				.body("result[0].aLong",                       equalTo(0))
				.body("result[0].aDate",                       equalTo(expectedDate))
				.body("result[0].base",                        nullValue())
				.body("result[0].createdDate",                 notNullValue())
				.body("result[0].lastModifiedDate",            notNullValue())
				.body("result[0].visibleToPublicUsers",        equalTo(false))
				.body("result[0].visibleToAuthenticatedUsers", equalTo(false))
				.body("result[0].createdBy",                   equalTo(userId))
				.body("result[0].hidden",                      equalTo(false))
				.body("result[0].owner",                       notNullValue())
				.body("result[0].ownerId",                     equalTo(userId))

			.when()
				.get(resource + "/all");


		// test ui view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, username)
				.header(X_PASSWORD_HEADER, password)
				.header("Accept", "application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo("TestOne"))
				.body("result[0].name",             equalTo("TestOne-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo(expectedDate))

			.when()
				.get(resource + "/ui");

	}
}
