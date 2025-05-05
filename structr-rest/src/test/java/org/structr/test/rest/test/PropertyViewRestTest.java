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
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.RequestKeywords;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
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

		String resource = "/TestTwo";

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

				.body("result[0]",                  isEntity("TestTwo"))

				.body("result[0].id",               equalTo(uuid))
				.body("result[0].type",	         equalTo("TestTwo"))
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

				.body("result[0]",                             isEntity("TestTwo"))

				.body("result[0].id",                          equalTo(uuid))
				.body("result[0].type",	                    equalTo("TestTwo"))
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
				.body("result[0].createdBy",                   equalTo(Principal.SUPERUSER_ID))
				.body("result[0].hidden",                      equalTo(false))
				.body("result[0].owner",                       nullValue())
				.body("result[0].ownerId",                     nullValue())

			.when()
				.get(concat(resource, "/all"));



	}

	@Test
	public void testOutputDepthScriptingProperty() {

		try (final Tx tx = app.tx()) {

			final NodeInterface schemaNode = app.create(StructrTraits.SCHEMA_NODE,"ScriptTest");

			final NodeInterface property = app.create(StructrTraits.SCHEMA_PROPERTY, "depth");
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode);
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function");
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "depth");

			final NodeInterface view = app.create(StructrTraits.SCHEMA_VIEW, "public");
			view.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode);
			view.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "name, depth, children, parents");

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY),         schemaNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY),         schemaNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY),   "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "*"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY),     "parents"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY),     "children")
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

			final NodeInterface schemaNode = app.create(StructrTraits.SCHEMA_NODE,"DepthTest");

			final NodeInterface property = app.create(StructrTraits.SCHEMA_PROPERTY, "depth");
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode);
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function");
			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "depth");

			final NodeInterface view = app.create(StructrTraits.SCHEMA_VIEW, "customView");
			view.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY),   schemaNode);
			view.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "type, depth");

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

	@Test
	public void testThatAllViewDoesNotContainBinaryData() {

		// create files and images and check for offending properties in view
		final String imageData     = "iVBORw0KGgoAAAANSUhEUgAAAFYAAAAXCAYAAAHmVYioAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sEBxYRJKsLwDYAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAD00lEQVRYw+1Y3XXyMAy9zuE1XSALhAHsBVigXsAL4AUyAQOQBeoF0gHIANECZIB6gTCAvhecY0wClNLv0HO4L4Cw/CNL8pWElBIAUFUVbzYbgUvouo632y0zM0spsd1uWUqJruv44+ODpZRYr9fcdR0vrLWCiDgoO+fG2eu6FgBARAIAi7CNOTjn2BgjACAzxjAAKKW4bVsOvwHAGMNlWY7fs7AsEYn9fp/OKuLvZ9sgIlZKXbZKgimdTGvNVVXxlEJd1xy2m8rmoJRiAMiGYYD3/kQhnggADocDjDFsjGHv/TgmtVusc/VGriHPcz4cDqdmwA+htT6TZUTExhherVYc26ZpmvGIwRHjz9gEQSfIF6l7HD0VRVGcyCccVQR7Bp0gz5RSQmvNbdsyHoTMGMN5niPPcwCAtZbzPD9bIL3hcPzgHd+6/XsC47swxrC1FlfXkVLi6+uLY3Rdx1JKSCkRy4Ms1QnpLB2TzhXGpHOm8jD+/f39RL6o65qLorh4qvQ/ImIigtb6Wxa/Zr30v6ZpYK0dnT2z1oqjgImItdaMJ0UWwlQpJZRSwnuP+AF6Jiw2mw2vVqsTYVVV8aMwbj6+kql8s1wuOT5o3/ew1oqpueKrT3XnXOViNlBKsdYaVVX9ejbo+37WEDe9MXVdj3nyN2Gtvd1n/wzmcl+aY2N5rDMMA4ex6/X6LM/O5WZm5kA1p2RSSux2uxP5wlqLtm2Rcr44SOq6Prs25xyGYbjZKE3TcFEUJ3MR0YnL9X2PQLy01rxarcbxRITFMfpnHXsYhknHDzz6VhRFASKaZTBpgIVYiceHPMtFUfCzu2ymlBJEhKZp8Eja9WsvmLVWKKXE8conqdzTbDbi5WKOnz/Fc5sS3BjeeyilRhJ8id6HLLHb7RgAlsvlxblChPd9L2Jd7z3atp1+bpmZ06gMxW+ccuI3e4qQl2XJzrkzjhEvHM8V0pVzThRFwU3TjAew1oopQv6jOjGqxk4Iy19EMM41wnZXfL3wwFzwiEne3t7GnkF4oEOM/0UcW2bjefb7PdLuzVXDHptct4T9bLiXZXlGy5RSs6F2qay9NSQvpaFra6V581olcBx30iZzziHOwen4RVwo3tt1uSXHhs3+j3z807Wu5Vit9WjUubHZVEv1hQeQQ2utCG4e2g+hVf+sjPZPGDZUfaHJZa2F9x6h3CUids5xWZYvI/+EbhGRMMaMhm6aBmVZwjmHS9XEC9/ksW3bvqx0B0TXdZxSo0uvofde3EJr0tfy1irtGpWZqJjOOPM9dCvPc/78/Jxs7KZ7voUS/gORqW62f9bxTgAAAABJRU5ErkJggg==";
		final String imageFileName = "test.png";
		final String textData      = "VGhpcyBpcyBhIHRlc3QK";
		final String textFileName  = "test.txt";

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.body("{ name: '" + textFileName + "', base64Data: '" + textData + "' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/File");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.body("{ name: '" + imageFileName + "', base64Data: '" + imageData + "' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/Image");

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result[0].type",         equalTo("Image"))
			.body("result[0].name",         equalTo("test.png"))
			.body("result[0].path",         equalTo("/test.png"))
			.body("result[0].size",         equalTo(1126))
			.body("result[0].width",        equalTo(86))
			.body("result[0].height",       equalTo(23))
			.body("result[0].isFile",       equalTo(true))
			.body("result[0].isImage",      equalTo(true))
			.body("result[1].type",         equalTo("File"))
			.body("result[1].name",         equalTo("test.txt"))
			.body("result[1].path",         equalTo("/test.txt"))
			.body("result[1].size",         equalTo(15))
			.body("result[1].isFile",       equalTo(true))
			.body("result[1].isImage",      equalTo(null))

			.body("result[0].extractedContent", equalTo(null))
			.body("result[0].base64Data",       equalTo(null))
			.body("result[0].imageData",        equalTo(null))

			.body("result[1].extractedContent", equalTo(null))
			.body("result[1].base64Data",       equalTo(null))
			.body("result[1].imageData",        equalTo(null))

			.when()
			.get("/File/all?_sort=name");

	}

	@Test
	public void testThatPropertiesWithSerializationDisabledFlagShouldNotBeSerialized() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema  = StructrSchema.createFromDatabase(app);
			final JsonType dummyType = schema.addType("DummyType");

			dummyType.addStringProperty("notSerialized", "public", "ui").setSerializationDisabled(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {
			logger.error("", t);
			fail("Unexpected exception during test setup.");
		}

		final String dummyName = "dummy";

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body("{ name: '" + dummyName + "', notSerialized: \"should not be serialized in any view\" }")
				.expect()
					.statusCode(201)
				.when()
					.post("/DummyType");

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
					.body("result[0].type",          equalTo("DummyType"))
					.body("result[0].name",          equalTo(dummyName))
					.body("result[0].notSerialized", equalTo(null))
				.when()
					.get("/DummyType/public?_sort=name");

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
					.body("result[0].type",          equalTo("DummyType"))
					.body("result[0].name",          equalTo(dummyName))
					.body("result[0].notSerialized", equalTo(null))
				.when()
					.get("/DummyType/ui?_sort=name");

		RestAssured
				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.expect()
					.body("result[0].type",          equalTo("DummyType"))
					.body("result[0].name",          equalTo(dummyName))
					.body("result[0].notSerialized", equalTo(null))
				.when()
					.get("/DummyType/all?_sort=name");
	}
}
