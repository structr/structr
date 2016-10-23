/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.resource;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.specification.ResponseSpecification;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;

/**
 *
 *
 */
public class RestVerbsTest extends StructrRestTest {

	@Test
	public void test01GET() {

		createNodes(100);

		expectOk(200).body("result_count", Matchers.equalTo(100)).when().get("/TestOne");
		expectOk(200).body("result_count", Matchers.equalTo(  1)).when().get("/TestOne?name=node055");

	}

	@Test
	public void test02PUT() {

		/*
		{
			"code": 400,
			"message": "PUT not allowed on TestOne collection resource",
			"errors": [
    			]
		}
		*/

		expectNotOk(400)
			.body("code",    Matchers.equalTo(400))
			.body("message", Matchers.equalTo("PUT not allowed on TestOne collection resource"))
			.when().put("/TestOne");

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

		createNodes(100);

		// delete exactly one element by name
		expectOk(200).when().delete("/TestOne?name=node055");
		expectOk(200).body("result_count", Matchers.equalTo(99)).when().get("/TestOne");

		// delete 11 elements
		expectOk(200).when().delete("/TestOne?name=02&loose=1");
		expectOk(200).body("result_count", Matchers.equalTo(88)).when().get("/TestOne");

		// delete 18 elements
		expectOk(200).when().delete("/TestOne?name=7&loose=1");
		expectNotOk(200).body("result_count", Matchers.equalTo(70)).when().get("/TestOne");
	}

	// ----- private methods -----
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
