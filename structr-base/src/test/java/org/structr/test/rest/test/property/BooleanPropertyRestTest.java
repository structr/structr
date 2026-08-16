/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.rest.test.property;

import io.restassured.RestAssured;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

public class BooleanPropertyRestTest extends StructrRestTestBase {

	@Test
	public void testBasics() {

		String uuid =  getUuidFromLocation(RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'booleanProperty' : true } ")
		.expect()
			.statusCode(201)
		.when()
			.post("/TestThree")
			.getHeader("Location"));

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].booleanProperty", equalTo(Boolean.TRUE))
		.when()
			.get("/TestThree");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'constantBooleanProperty' : false } ")
		.expect()
			.statusCode(422)
		.when()
			.put("/TestThree/" + uuid);

		// PUT with old value should be ignored
		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(" { 'constantBooleanProperty' : true } ")
		.expect()
			.statusCode(200)
		.when()
			.put("/TestThree/" + uuid);

	}

	@Test
	public void testInputTypes() {

		// Every spelling of a boolean is accepted, in JSON and in its textual form. An input that is not a
		// boolean at all is rejected: it says nothing about which value was meant, so silently storing
		// false would be a guess.

		for (final String accepted : new String[] { "true", "'true'", "'True'", "'on'", "'1'", "1" }) {

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'booleanProperty' : " + accepted + " } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/TestThree");
		}

		for (final String accepted : new String[] { "false", "'false'", "'off'", "'0'", "0", "''" }) {

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'booleanProperty' : " + accepted + " } ")
			.expect()
				.statusCode(201)
			.when()
				.post("/TestThree");
		}

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result.findAll { it.booleanProperty == true }.size()",  equalTo(6))
			.body("result.findAll { it.booleanProperty == false }.size()", equalTo(6))
		.when()
			.get("/TestThree");

		// a blank value is treated as "not given", anything else is an error instead of a silent false
		for (final String rejected : new String[] { "'banana'", "2", "'yes'", "[]", "{}" }) {

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body(" { 'booleanProperty' : " + rejected + " } ")
			.expect()
				.statusCode(422)
			.when()
				.post("/TestThree");
		}
	}
}
