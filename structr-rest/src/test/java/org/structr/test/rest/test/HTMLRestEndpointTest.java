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
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 *
 *
 */
public class HTMLRestEndpointTest extends StructrRestTestBase {

	@Test
	public void test01Paging() {

		RestAssured

			.given()
				.accept("text/html")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))

			.expect()
				.statusCode(200)
				.contentType("text/html;charset=utf-8")
				.header("Content-Encoding", "gzip")
				.header("Transfer-Encoding", "chunked")

				.body("html.head.title", equalTo("/structr/rest/Group"))
				.body("html.body.div[1].h1", equalTo("/structr/rest/Group"))
				.body("html.body.div[1].ul.li[0].ul.li[0].b", equalTo("\"result\":"))
				.body("html.body.div[1].ul.li[0].ul.li[1].b", equalTo("\"query_time\":"))
				.body("html.body.div[1].ul.li[0].ul.li[2].b", equalTo("\"result_count\":"))
				.body("html.body.div[1].ul.li[0].ul.li[3].b", equalTo("\"page_count\":"))
				.body("html.body.div[1].ul.li[0].ul.li[4].b", equalTo("\"result_count_time\":"))
				.body("html.body.div[1].ul.li[0].ul.li[5].b", equalTo("\"serialization_time\":"))

			.when()
				.get("/Group");
	}
}