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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Person;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

/**
 */
public class EntityResolverTest extends StructrRestTestBase {

	@Test
	public void testEntityResolver() {

		try {

			// create list of persons to test the resolver resource
			final List<Person> persons = createTestNodes(Person.class, 10);

			RestAssured

				.given()
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.contentType("application/json; charset=UTF-8")
					.body(" { ids: [\"" + persons.get(2) + "\", \"" + persons.get(5) + "\", \"" + persons.get(7) + "\", \"" + persons.get(8) + "\"] } ")
				.expect()
					.statusCode(200)
					.body("result_count", equalTo(4))
					.body("result[0].id", equalTo(persons.get(2).getUuid()))
					.body("result[1].id", equalTo(persons.get(5).getUuid()))
					.body("result[2].id", equalTo(persons.get(7).getUuid()))
					.body("result[3].id", equalTo(persons.get(8).getUuid()))
				.when()
					.post("/resolver");

			RestAssured

				.given()
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.contentType("application/json; charset=UTF-8")
					.body(" { ids: [\"" + persons.get(2) + "\", \"" + persons.get(5) + "\"] } ")
				.expect()
					.statusCode(200)
					.body("result_count", equalTo(2))
					.body("result[0].id", equalTo(persons.get(2).getUuid()))
					.body("result[1].id", equalTo(persons.get(5).getUuid()))
				.when()
					.post("/resolver/ui");

			// test failure
			RestAssured

				.given()
					.filter(ResponseLoggingFilter.logResponseTo(System.out))
					.contentType("application/json; charset=UTF-8")
					.body(" { name: test } ")
				.expect()
					.statusCode(422)
					.body("code", equalTo(422))
					.body("message", equalTo("Send a JSON object containing an array named 'ids' to use this endpoint."))
				.when()
					.post("/resolver");

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testViewResolution() {

		createEntity("SchemaNode", "{ name: Test, _blah: String, __vvv: 'id, type, name, blah' }");

		RestAssured

			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
			.when()
				.get("/Person/vvv");

	}

}
