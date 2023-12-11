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
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.config.Settings;
import org.structr.common.RequestKeywords;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.entity.TestTwo;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.fail;

/**
 * Test for property views
 */
public class PropertyViewRestTest extends StructrRestTestBase {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		super.setup(testDatabaseConnection);

		// modify default date format
		Settings.DefaultDateFormat.setValue("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	}

	@Test
	public void testPropertyViewsAndResultSetLayout() {

		// the new test setup method requires a whole new test class for
		// configuration changes, so this test class is a duplicate of
		// the existing StructrRestTestBase.. :(

		String resource = "/test_twos";

		// create entity
		final String uuid = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'TestTwo-0', 'anInt' : 0, 'aLong' : 0, 'aDate' : '2012-09-18T00:33:12+0200' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location")
		);

		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                 notNullValue())
				.body("serialization_time",         notNullValue())
				.body("result_count",               equalTo(1))
				.body("result",                     hasSize(1))

				.body("result[0]",                  isEntity(TestTwo.class))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	            equalTo(TestTwo.class.getSimpleName()))
				.body("result[0].name",             equalTo("TestTwo-0"))
				.body("result[0].anInt",            equalTo(0))
				.body("result[0].aLong",            equalTo(0))
				.body("result[0].aDate",            equalTo("2012-09-17T22:33:12.000Z"))

			.when()
				.get(resource);


		// test all view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                            notNullValue())
				.body("serialization_time",                    notNullValue())
				.body("result_count",                          equalTo(1))
				.body("result",                                hasSize(1))

				.body("result[0]",                             isEntity(TestTwo.class))

				.body("result[0].id",                          equalTo(uuid))
				.body("result[0].type",	                       equalTo(TestTwo.class.getSimpleName()))
				.body("result[0].name",                        equalTo("TestTwo-0"))
				.body("result[0].anInt",                       equalTo(0))
				.body("result[0].aLong",                       equalTo(0))
				.body("result[0].aDate",                       equalTo("2012-09-17T22:33:12.000Z"))
				.body("result[0].test_ones",                   notNullValue())
				.body("result[0].base",                        nullValue())
				.body("result[0].createdDate",                 notNullValue())
				.body("result[0].lastModifiedDate",            notNullValue())
				.body("result[0].visibleToPublicUsers",        equalTo(false))
				.body("result[0].visibleToAuthenticatedUsers", equalTo(false))
				.body("result[0].visibilityStartDate",         nullValue())
				.body("result[0].visibilityEndDate",           nullValue())
				.body("result[0].createdBy",                   nullValue())
				.body("result[0].hidden",                      equalTo(false))
				.body("result[0].owner",                       nullValue())
				.body("result[0].ownerId",                     nullValue())

			.when()
				.get(concat(resource, "/all"));



	}

	@Test
	public void testOutputDepthScriptingProperty() {

		try (final Tx tx = app.tx()) {

			final SchemaNode node = app.create(SchemaNode.class,
				new NodeAttribute<>(AbstractNode.name, "ScriptTest"),
				new NodeAttribute<>(new StringProperty("_depth"), "Function(depth)"),
				new NodeAttribute<>(new StringProperty("__public"), "name, depth, children, parents")
			);

			app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, node),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, node),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "test"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "parents"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "children")
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}


		// the new test setup method requires a whole new test class for
		// configuration changes, so this test class is a duplicate of
		// the existing StructrRestTest.. :(

		String resource = "/ScriptTest";

		// create entity
		final String uuid = getUuidFromLocation(RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'ScriptTest1' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location")
		);

		// create second entity
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'ScriptTest2', 'parents': [{ 'id': '" + uuid + "' }] } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource).getHeader("Location");

		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                                         notNullValue())
				.body("serialization_time",                                 notNullValue())
				.body("result_count",                                       equalTo(2))
				.body("result",                                             hasSize(2))

				.body("result[0].type",	                                    equalTo("ScriptTest"))
				.body("result[0].depth",                                    equalTo(0))
				.body("result[0].name",                                     equalTo("ScriptTest1"))
				.body("result[0].children[0].type",                         equalTo("ScriptTest"))
				.body("result[0].children[0].depth",                        equalTo(1))
				.body("result[0].children[0].name",                         equalTo("ScriptTest2"))
				.body("result[0].children[0].parents[0].type",              equalTo("ScriptTest"))
//				.body("result[0].children[0].parents[0].depth",             equalTo(2))
				.body("result[0].children[0].parents[0].name",              equalTo("ScriptTest1"))
//				.body("result[0].children[0].parents[0].children[0].type",  equalTo("ScriptTest"))
//				.body("result[0].children[0].parents[0].children[0].depth", equalTo(3))
//				.body("result[0].children[0].parents[0].children[0].name",  equalTo("ScriptTest2"))
 				.body("result[1].type",	                                    equalTo("ScriptTest"))
				.body("result[1].depth",                                    equalTo(0))
				.body("result[1].name",                                     equalTo("ScriptTest2"))
				.body("result[1].parents[0].type",	                        equalTo("ScriptTest"))
				.body("result[1].parents[0].depth",                         equalTo(1))
				.body("result[1].parents[0].name",                          equalTo("ScriptTest1"))

			.when().get(resource + "?_sort=name");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                                      notNullValue())
				.body("serialization_time",                              notNullValue())
				.body("result_count",                                    equalTo(1))

				.body("result.type",                                     equalTo("ScriptTest"))
				.body("result.depth",                                    equalTo(0))
				.body("result.name",                                     equalTo("ScriptTest1"))
				.body("result.children[0].type",                         equalTo("ScriptTest"))
				.body("result.children[0].depth",                        equalTo(1))
				.body("result.children[0].name",                         equalTo("ScriptTest2"))
				.body("result.children[0].parents[0].type",              equalTo("ScriptTest"))
//				.body("result.children[0].parents[0].depth",             equalTo(2))
				.body("result.children[0].parents[0].name",              equalTo("ScriptTest1"))
//				.body("result.children[0].parents[0].children[0].type",  equalTo("ScriptTest"))
//				.body("result.children[0].parents[0].children[0].depth", equalTo(3))
//				.body("result.children[0].parents[0].children[0].name",  equalTo("ScriptTest2"))

			.when()
				.get(resource.concat("/").concat(uuid));

	}

	@Test
	public void testOutputDepthScriptingPropertyInCustomView() {

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute<>(AbstractNode.name, "DepthTest"),
				new NodeAttribute<>(new StringProperty("_depth"), "Function(depth)"),
				new NodeAttribute<>(new StringProperty("__customView"), "depth, type")
			);

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception.");
		}

		final String resource = "/DepthTest";

		// create entities
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'DepthTest1' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource);

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.body(" { 'name' : 'DepthTest2' } ")

			.expect()
				.statusCode(201)

			.when()
				.post(resource);


		// test default view with properties in it
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.header("Accept", "application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))

			.expect()
				.statusCode(200)
				.body("query_time",                                         notNullValue())
				.body("serialization_time",                                 notNullValue())
				.body("result_count",                                       equalTo(2))
				.body("result",                                             hasSize(2))

				.body("result[0].type",	                                    equalTo("DepthTest"))
				.body("result[0].depth",                                    equalTo(0))
				.body("result[1].type",	                                    equalTo("DepthTest"))
				.body("result[1].depth",                                    equalTo(0))

			.when()
				.get(resource.concat("/customView"));

	}

	@Test
	public void testOutputReductionDepthRequestKeyword() {

		final String testFiveUUID  = createEntity("/TestFive", "{ \"name\": \"TestFive\" }");
		final String testThreeUUID = createEntity("/TestThree", "{ \"name\": \"TestThree\", \"oneToOneTestFive\": \"" + testFiveUUID + "\" }");

		// test that the default outputReductionDepth is 0 and nested elements are limited to id,type,name when using the "all" view
		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
					.statusCode(200)
					.body("result.id",                                 equalTo(testThreeUUID))
					.body("result.type",                               equalTo("TestThree"))
					.body("result.name",                               equalTo("TestThree"))
					.body("result.oneToOneTestFive.id",                equalTo(testFiveUUID))
					.body("result.oneToOneTestFive.type",              equalTo("TestFive"))
					.body("result.oneToOneTestFive.name",              equalTo("TestFive"))
					.body("result.oneToOneTestFive.oneToOneTestThree", nullValue())
				.when()
					.get("/TestThree/" + testThreeUUID + "/all");

		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
					.statusCode(200)
					.body("result.id",                                                  equalTo(testThreeUUID))
					.body("result.type",                                                equalTo("TestThree"))
					.body("result.name",                                                equalTo("TestThree"))
					.body("result.oneToOneTestFive.id",                                 equalTo(testFiveUUID))
					.body("result.oneToOneTestFive.type",                               equalTo("TestFive"))
					.body("result.oneToOneTestFive.name",                               equalTo("TestFive"))

					// test that the nested object is limited to 3 keys (id, type, name)
					.body("result.oneToOneTestFive.oneToOneTestThree",                  aMapWithSize(3))
					.body("result.oneToOneTestFive.oneToOneTestThree.id",               equalTo(testThreeUUID))
					.body("result.oneToOneTestFive.oneToOneTestThree.type",             equalTo("TestThree"))
					.body("result.oneToOneTestFive.oneToOneTestThree.name",             equalTo("TestThree"))

				.when()
					.get("/TestThree/" + testThreeUUID + "/all?" + RequestKeywords.OutputReductionDepth.keyword() + "=1");

		// remove redundancy reduction, so that TestThree.TestFive.TestThree is not limited by that and can contain TestFive again
		Settings.JsonRedundancyReduction.setValue(false);

		RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
				.statusCode(200)
				.body("result.id",                                                  equalTo(testThreeUUID))
				.body("result.type",                                                equalTo("TestThree"))
				.body("result.name",                                                equalTo("TestThree"))
				.body("result.oneToOneTestFive.id",                                 equalTo(testFiveUUID))
				.body("result.oneToOneTestFive.type",                               equalTo("TestFive"))
				.body("result.oneToOneTestFive.name",                               equalTo("TestFive"))

				// test that the nested object is limited to 3 keys (id, type, name)
				.body("result.oneToOneTestFive.oneToOneTestThree.oneToOneTestFive",                  aMapWithSize(3))
				.body("result.oneToOneTestFive.oneToOneTestThree.oneToOneTestFive.id",               equalTo(testFiveUUID))
				.body("result.oneToOneTestFive.oneToOneTestThree.oneToOneTestFive.type",             equalTo("TestFive"))
				.body("result.oneToOneTestFive.oneToOneTestThree.oneToOneTestFive.name",             equalTo("TestFive"))

				.when()
				.get("/TestThree/" + testThreeUUID + "/all?" + RequestKeywords.OutputReductionDepth.keyword() + "=2");


		Settings.JsonRedundancyReduction.getValue(true);
	}

}
