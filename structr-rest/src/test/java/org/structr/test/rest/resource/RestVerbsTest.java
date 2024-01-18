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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.test.rest.common.StructrRestTestBase;
import org.structr.test.rest.entity.TestOne;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class RestVerbsTest extends StructrRestTestBase {

	@Test
	public void test01GET() {

		createNodes(100);

		expectOk(200).body("result_count", Matchers.equalTo(100)).when().get("/TestOne");
		expectOk(200).body("result_count", Matchers.equalTo(  1)).when().get("/TestOne?name=node055");

	}

	@Test
	public void test02PUT() {

		final List<TestOne> nodes = createNodes(1);

		// test collection resource
		expectNotOk(405)
			.body("code",    Matchers.equalTo(405))
			.body("message", Matchers.equalTo("PUT not allowed on ‛TestOne‛ collection resource"))
			.when().put("/TestOne");

		// test type + UUID resource
		expectOk(200)
			.when()
			.put("/TestOne/" + nodes.get(0).getUuid());

		// test type + UUID resource
		expectOk(200)
			.when()
			.put("/" + nodes.get(0).getUuid());

	}

	@Test
	public void test03POST() {

		/*
		{
			"result_count": 1,
			"result": [
				"cd917f586c704c3da43dcd8d0edd4639"
			],
			"serialization_time": "0.000550422"
		}
		*/

		expectOk(201)
			.body("result_count", Matchers.equalTo(1))
			.body("result",       Matchers.instanceOf(Collection.class))
			.body("result[0]",    Matchers.instanceOf(String.class))
			.when().post("/TestOne");

	}

	@Test
	public void test04DELETE() {

		final List<TestOne> nodes = createNodes(100);

		// delete exactly one element by name
		expectOk(200).when().delete("/TestOne?name=node055");
		expectOk(200).body("result_count", Matchers.equalTo(99)).when().get("/TestOne");

		// delete 11 elements
		expectOk(200).when().delete("/TestOne?name=02&_loose=1");
		expectOk(200).body("result_count", Matchers.equalTo(88)).when().get("/TestOne");

		// delete 18 elements
		expectOk(200).when().delete("/TestOne?name=7&_loose=1");
		expectNotOk(200).body("result_count", Matchers.equalTo(70)).when().get("/TestOne");

		// delete 18 elements
		expectOk(200).when().delete("/" + nodes.get(0).getUuid());
		expectNotOk(200).body("result_count", Matchers.equalTo(69)).when().get("/TestOne");
	}

	@Test
	public void test05PATCHSuccess() {

		final List<Object> ids = new LinkedList<>();

		ids.add(createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }"));
		ids.add(createEntity("/TestOne", "{ name: 'bbb', anInt: 2, aLong: 4 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ccc', anInt: 3, aLong: 6 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ddd', anInt: 4, aLong: 8 }"));

		// do PATCH
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(createPatchBody(ids))

			.expect()
				.statusCode(200)

			.when()

				.patch("/TestOne");


		// check result
		RestAssured

			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result_count",    Matchers.equalTo(4))
				.body("result[0].id",    equalTo(ids.get(0)))
				.body("result[0].anInt", equalTo(15))
				.body("result[1].id",    equalTo(ids.get(1)))
				.body("result[1].anInt", equalTo(16))
				.body("result[2].id",    equalTo(ids.get(2)))
				.body("result[2].anInt", equalTo(17))
				.body("result[3].id",    equalTo(ids.get(3)))
				.body("result[3].anInt", equalTo(18))

			.when()

				.get("/TestOne?_sort=name");
	}

	@Test
	public void testPATCHBatchSize() {

		final List<Object> ids = new LinkedList<>();

		ids.add(createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }"));
		ids.add(createEntity("/TestOne", "{ name: 'bbb', anInt: 2, aLong: 4 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ccc', anInt: 3, aLong: 6 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ddd', anInt: 4, aLong: 8 }"));

		// do PATCH
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(createPatchBody(ids))

			.expect()
				.statusCode(200)

			.when()

				.patch("/TestOne?_batchSize=3");


		// check result
		RestAssured

			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result_count",    Matchers.equalTo(4))
				.body("result[0].id",    equalTo(ids.get(0)))
				.body("result[0].anInt", equalTo(15))
				.body("result[1].id",    equalTo(ids.get(1)))
				.body("result[1].anInt", equalTo(16))
				.body("result[2].id",    equalTo(ids.get(2)))
				.body("result[2].anInt", equalTo(17))
				.body("result[3].id",    equalTo(ids.get(3)))
				.body("result[3].anInt", equalTo(18))

			.when()

				.get("/TestOne?_sort=name");
	}

	@Test
	public void test05PATCHFail404() {

		final List<Object> ids = new LinkedList<>();

		ids.add(createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }"));
		ids.add(createEntity("/TestOne", "{ name: 'bbb', anInt: 2, aLong: 4 }"));

		// add nonexisting UUID to test 404 failure
		ids.add(NodeServiceCommand.getNextUuid());

		ids.add(createEntity("/TestOne", "{ name: 'ccc', anInt: 3, aLong: 6 }"));

		// do PATCH
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(createPatchBody(ids))

			.expect()
				.statusCode(404)

			.when()

				.patch("/TestOne");
	}

	@Test
	public void test05PATCHCreateObjectForMissingId() {

		final List<Object> ids = new LinkedList<>();

		ids.add(createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }"));

		// add empty ID (causes id field to be missing in request body)
		ids.add("");

		ids.add(createEntity("/TestOne", "{ name: 'bbb', anInt: 2, aLong: 4 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ccc', anInt: 3, aLong: 6 }"));

		// do PATCH
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(createPatchBody(ids))

			.expect()
				.statusCode(200)

			.when()

				.patch("/TestOne");

		// check result
		RestAssured

			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)
				.body("result_count",    Matchers.equalTo(4))
				.body("result[0].id",    equalTo(ids.get(0)))
				.body("result[0].anInt", equalTo(15))
//				.body("result[1].id",    equalTo(ids.get(1)))	// this id was generated by PATCH
				.body("result[1].anInt", equalTo(16))
				.body("result[2].id",    equalTo(ids.get(2)))
				.body("result[2].anInt", equalTo(17))
				.body("result[3].id",    equalTo(ids.get(3)))
				.body("result[3].anInt", equalTo(18))

			.when()

				.get("/TestOne?_sort=anInt");
	}

	@Test
	public void test05PATCHFail422WrongId() {

		final List<Object> ids = new LinkedList<>();

		ids.add(createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }"));

		// add non-string ID
		ids.add(22);

		ids.add(createEntity("/TestOne", "{ name: 'bbb', anInt: 2, aLong: 4 }"));
		ids.add(createEntity("/TestOne", "{ name: 'ccc', anInt: 3, aLong: 6 }"));

		// do PATCH
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.body(createPatchBody(ids))

			.expect()
				.statusCode(422)

			.when()

				.patch("/TestOne");
	}

	@Test
	public void test05PATCHOnEntityResource() {

		final String id = createEntity("/TestOne", "{ name: 'aaa', anInt: 1, aLong: 2 }");

		// do PATCH 1
		RestAssured.given().contentType("application/json; charset=UTF-8").body("{ name: 'moep1', anInt: 42, aLong: 13 }")
			.expect().statusCode(200).when().patch("/TestOne/" + id);

		// check result 1
		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200)
			.body("result.id",       equalTo(id))
			.body("result.name",     equalTo("moep1"))
			.body("result.anInt",    equalTo(42))
			.body("result.aLong",    equalTo(13))
			.when().get("/TestOne/" + id);


		// do PATCH 2
		RestAssured.given().contentType("application/json; charset=UTF-8").body("[ { name: 'moep2' }, { anInt: 43 }, { aLong: 14 } ]")
			.expect().statusCode(200).when().patch("/TestOne/" + id);

		// check result 1
		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200)
			.body("result.id",       equalTo(id))
			.body("result.name",     equalTo("moep2"))
			.body("result.anInt",    equalTo(43))
			.body("result.aLong",    equalTo(14))
			.when().get("/TestOne/" + id);
	}

	// ----- private methods -----
	private String createPatchBody(final List<Object> ids) {

		final StringBuilder buf = new StringBuilder();

		// open array
		buf.append("[ ");

		buf.append(createPatchObject(ids.get(0), "anInt: 15"));
		buf.append(", ");
		buf.append(createPatchObject(ids.get(1), "anInt: 16"));
		buf.append(", ");
		buf.append(createPatchObject(ids.get(2), "anInt: 17"));
		buf.append(", ");
		buf.append(createPatchObject(ids.get(3), "anInt: 18"));

		// close array
		buf.append(" ]");

		return buf.toString();
	}

	private String createPatchObject(final Object id, final String body) {

		final StringBuilder buf = new StringBuilder();

		buf.append("{");

		// for testing, only add id field if it is non-empty
		if (StringUtils.isNotBlank(id.toString())) {

			buf.append(" id: ");

			// add quote for string values (to be able to test numerical values too)
			if (id instanceof String) {
				buf.append("'");
			}

			buf.append(id);

			// add quote for string values
			if (id instanceof String) {
				buf.append("'");
			}

			buf.append(", ");
		}

		buf.append(body);
		buf.append("}");

		return buf.toString();

	}

	private ResponseSpecification expectOk(final int statusCode) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
				.statusCode(statusCode);
	}

	private ResponseSpecification expectNotOk(final int statusCode) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.expect()
				.statusCode(statusCode);
	}

	private List<TestOne> createNodes(final int count) {

		// create 100 test nodes and set names
		try (final Tx tx = app.tx()) {

			final List<TestOne> nodes = createTestNodes(TestOne.class, 100);
			int i                     = 0;

			for (final TestOne node : nodes) {

				node.setProperty(AbstractNode.name, "node" + StringUtils.leftPad(Integer.toString(i++), 3, "0"));
			}

			tx.success();

			return nodes;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		return null;
	}
}
