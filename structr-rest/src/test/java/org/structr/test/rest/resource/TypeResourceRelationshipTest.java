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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.entity.*;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class TypeResourceRelationshipTest extends StructrRestTestBase {

	private static final Logger logger = LoggerFactory.getLogger(TypeResourceRelationshipTest.class.getName());

	@Test
	public void testCreateRelationship() {

		String sourceNodeId = null;
		String targetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestTwo sourceNode = app.create(TestTwo.class);
			final TestOne targetNode = app.create(TestOne.class);

			// store IDs for later use
			sourceNodeId = sourceNode.getUuid();
			targetNodeId = targetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// Check nodes exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
				.body("result",		    isEntity(TestTwo.class))
			.when()
				.get(concat("/TestTwo/", sourceNodeId));

		/* Create relationship using the TypeResource.

		 * The relation class is TwoOneOneToMany: (:TestTwo) -1-[:OWNS]-*-> (:TestOne)
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"startNodeId\" : \""+ sourceNodeId +"\", \"endNodeId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/TwoOneOneToMany");


		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/TwoOneOneToMany");
	}


	@Test
	public void testCardinalityOneToMany() {

		String sourceNodeId = null;
		String targetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestTwo sourceNode = app.create(TestTwo.class);
			final TestOne targetNode = app.create(TestOne.class);

			// store IDs for later use
			sourceNodeId = sourceNode.getUuid();
			targetNodeId = targetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		/** Create two relationship using the TypeResource.
		 *
		 * The relation class is TwoOneOneToMany: (:TestTwo) -1-[:OWNS]-*-> (:TestOne),
		 * so between the same nodes, the second relationship should replace the first one
		 * to enforce the correct cardinality.
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"startNodeId\" : \""+ sourceNodeId +"\", \"endNodeId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/TwoOneOneToMany");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"startNodeId\" : \""+ sourceNodeId +"\", \"endNodeId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/TwoOneOneToMany");

		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/TwoOneOneToMany");

	}

	@Test
	public void testCardinalityOneToOne() {

		String sourceNodeId = null;
		String targetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestFour  sourceNode = app.create(TestFour.class);
			final TestThree targetNode = app.create(TestThree.class);

			// store IDs for later use
			sourceNodeId = sourceNode.getUuid();
			targetNodeId = targetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		/** Create two relationship using the TypeResource.
		 *
		 * The relation class is FourThreeOneToOne: (:TestFour) -1-[:OWNS]-1-> (:TestThree),
		 * so between the same nodes, the second relationship should replace the first one
		 * to enforce the correct cardinality.
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FourThreeOneToOne");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FourThreeOneToOne");

		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/FourThreeOneToOne");

	}

	@Test
	public void testCardinalityManyToOne() {

		String sourceNodeId = null;
		String targetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestFive sourceNode = app.create(TestFive.class);
			final TestOne  targetNode = app.create(TestOne.class);

			// store IDs for later use
			sourceNodeId = sourceNode.getUuid();
			targetNodeId = targetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		/** Create two relationship using the TypeResource.
		 *
		 * The relation class is FiveOneManyToOne: (:TestFive) -*-[:OWNS]-1-> (:TestOne),
		 * so between the same nodes, the second relationship should replace the first one
		 * to enforce the correct cardinality.
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FiveOneManyToOne");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FiveOneManyToOne");

		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/FiveOneManyToOne");

	}

	@Test
	public void testCardinalityOneToOneThreeNodes() {

		String sourceNodeId = null;
		String targetNodeId = null;
		String newTargetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestFour  sourceNode    = app.create(TestFour.class);
			final TestThree targetNode    = app.create(TestThree.class);
			final TestThree newTargetNode = app.create(TestThree.class);

			// store IDs for later use
			sourceNodeId    = sourceNode.getUuid();
			targetNodeId    = targetNode.getUuid();
			newTargetNodeId = newTargetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		/** Create two relationship using the TypeResource.
		 *
		 * The relation class is FourThreeOneToOne: (:TestFour) -1-[:OWNS]-1-> (:TestThree),
		 * so the second relationship should replace the first one
		 * to enforce the correct cardinality.
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FourThreeOneToOne");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ newTargetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FourThreeOneToOne");

		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/FourThreeOneToOne");

	}

	@Test
	public void testCardinalityManyToOneThreeNodes() {

		String sourceNodeId = null;
		String targetNodeId = null;
		String newTargetNodeId = null;

		try (final Tx tx = app.tx()) {

			final TestFive sourceNode   = app.create(TestFive.class);
			final TestOne targetNode    = app.create(TestOne.class);
			final TestOne newTargetNode = app.create(TestOne.class);

			// store IDs for later use
			sourceNodeId = sourceNode.getUuid();
			targetNodeId = targetNode.getUuid();
			newTargetNodeId = newTargetNode.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		/** Create two relationship using the TypeResource.
		 *
		 * The relation class is FiveOneManyToOne: (:TestFive) -*-[:OWNS]-1-> (:TestOne),
		 * so between the same nodes, the second relationship should replace the first one
		 * to enforce the correct cardinality.
		 */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ targetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FiveOneManyToOne");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.body(" { \"sourceId\" : \""+ sourceNodeId +"\", \"targetId\" : \""+ newTargetNodeId +"\" } ")

			.expect()
				.statusCode(201)
			.when()
				.post("/FiveOneManyToOne");

		// Check results: Only one relationship must exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(1))
			.when()
				.get("/FiveOneManyToOne");

	}

	@Test
	public void testCreationOfDerivedTypeObjects() {

		// Verify that the creation of derived type objects is possible
		// using the REST resource of the base type. This is important
		// when a user wants to create a more specific type using the
		// base type resource URL.

		final String uuid = createEntity("/SchemaNode", "{ name: BaseType }");
		createEntity("/SchemaNode", "{ name: DerivedType, extendsClass: \"" + uuid + "\" }");

		createEntity("/BaseType", "{ name: BaseType }");
		createEntity("/BaseType", "{ name: DerivedType, type: DerivedType }");

		// Check nodes exist
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(200)
				.body("result_count",       equalTo(2))
				.body("result[0].type",     equalTo("BaseType"))
				.body("result[1].type",     equalTo("DerivedType"))
			.when()
				.get("/BaseType?_sort=name");
	}
}
