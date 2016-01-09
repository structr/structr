/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.TestOne;
import org.structr.web.entity.dom.Page;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *
 *
 */
public class HtmlServletObjectResolvingTest extends StructrUiTest {

	public void testObjectResolvingInHtmlServlet() {

		final List<String> testObjectIDs = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			// setup three different test objects to be found by HtmlServlet
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.anInt, 123)).getUuid());
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.aDouble, 0.345)).getUuid());
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.aString, "abcdefg")).getUuid());

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "testPage");
			if (newPage != null) {

				Element html  = newPage.createElement("html");
				Element head  = newPage.createElement("head");
				Element body  = newPage.createElement("body");
				Text textNode = newPage.createTextNode("${current.id}");

				try {
					// add HTML element to page
					newPage.appendChild(html);
					html.appendChild(head);
					html.appendChild(body);
					body.appendChild(textNode);

				} catch (DOMException dex) {

					dex.printStackTrace();

					throw new FrameworkException(422, dex.getMessage());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/html";

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(0)))
			.when()
			.get("/testPage/123");

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(1)))
			.when()
			.get("/testPage/0.345");

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(2)))
			.when()
			.get("/testPage/abcdefg");
	}

	@Override
	protected void setUp() throws Exception {

		final Map<String, Object> additionalConfig = new HashMap<>();

		additionalConfig.put(HtmlServlet.OBJECT_RESOLUTION_PROPERTIES, "TestOne.anInt, TestOne.aString, TestOne.aDouble");

		setUp(additionalConfig);
	}

}
