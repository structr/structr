package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.TestOne;

/**
 *
 * @author Christian Morgner
 */
public class PropertyViewTest extends StructrUiTest {

	public void testResourceAccessGrants() {

		final String username = "tester";
		final String password = "test";

		// create initial user
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", "superadmin")
				.header("X-Password", "sehrgeheim")
				.body(" { 'name' : '" + username + "', 'password': '" + password + "' } ")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(201)

			.when()
				.post("/users");

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
			expected response code of 400 for PUT is correct because
			we don't supply a correct resource URL, this test is only
			about having sufficient permissions to cause a 400 error.
		*/

		String resource = "/test_ones";

		// first: test failures without resource access object
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
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 400);
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
		testGet(   resource, "", "",                                200);
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
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             400);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 64
		grant("TestOne", 64, true);

		// failures with flags == 64 (NON_AUTH_USER_POST)
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             201);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                401);
		testDelete(resource, username, password,                    401);

		// grant with flags == 128
		grant("TestOne", 128, true);

		// failures with flags == 128 (NON_AUTH_USER_DELETE)
		testGet(   resource, "", "",                                401);
		testGet(   resource, username, password,                    401);
		testPut(   resource, "", "", "{'name':'test'}",             401);
		testPut(   resource, username, password, "{'name':'test'}", 401);
		testPost(  resource, "", "", "{'name':'test'}",             401);
		testPost(  resource, username, password, "{'name':'test'}", 401);
		testDelete(resource, "", "",                                200);
		testDelete(resource, username, password,                    401);


	}

	public void testPropertyViewsAndResultSetLayout() {

		final String username = "tester";
		final String password = "test";

		// create initial user
		final String userId = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", "superadmin")
				.header("X-Password", "sehrgeheim")
				.body(" { 'name' : '" + username + "', 'password': '" + password + "' } ")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(201)

			.when()
				.post("/users").header("Location"));

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

		String resource = "/test_ones";

		// grant GET and POST for authenticated users
		grant("TestOne", 5, true);
		grant("TestOne/_All", 1, false);
		grant("TestOne/_Ui", 1, false);
		grant("Page", 5, false);
		grant("Page/_Ui", 1, false);
		grant("Page/_Html", 1, false);


		// create entity
		final String uuid = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'TestOne-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location")
		);

		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("query_time",                 notNullValue())
				.body("serialization_time",         notNullValue())
				.body("result_count",               equalTo(1))
				.body("result",                     hasSize(1))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo(TestOne.class.getSimpleName()))
				.body("result[0].name",             equalTo("TestOne-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo("2012-09-17T22:33:12+0000"))

			.when()
				.get(resource);

		// test all view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0].id",                          equalTo(uuid))
				.body("result[0].type",	                       equalTo(TestOne.class.getSimpleName()))
				.body("result[0].name",                        equalTo("TestOne-0"))
				.body("result[0].anInt",                       equalTo(0))
				.body("result[0].aLong",                       equalTo(0))
				.body("result[0].aDate",                       equalTo("2012-09-17T22:33:12+0000"))
				.body("result[0].base",                        nullValue())
				.body("result[0].createdDate",                 notNullValue())
				.body("result[0].lastModifiedDate",            notNullValue())
				.body("result[0].visibleToPublicUsers",        equalTo(false))
				.body("result[0].visibleToAuthenticatedUsers", equalTo(false))
				.body("result[0].visibilityStartDate",         nullValue())
				.body("result[0].visibilityEndDate",           nullValue())
				.body("result[0].createdBy",                   equalTo(userId))
				.body("result[0].deleted",                     equalTo(false))
				.body("result[0].hidden",                      equalTo(false))
				.body("result[0].owner",                       notNullValue())
				.body("result[0].ownerId",                     equalTo(userId))

			.when()
				.get(resource + "/all");


		// test ui view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("X-User", username)
				.header("X-Password", password)
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo(TestOne.class.getSimpleName()))
				.body("result[0].name",             equalTo("TestOne-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo("2012-09-17T22:33:12+0000"))

			.when()
				.get(resource + "/ui");

	}
}
