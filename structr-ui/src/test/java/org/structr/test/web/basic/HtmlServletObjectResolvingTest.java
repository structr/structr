/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class HtmlServletObjectResolvingTest extends StructrUiTest {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		// important: these settings must be made before HttpService is initialized..
		Settings.HtmlResolveProperties.setValue("TestOne.anInt, TestOne.aString, TestOne.aDouble");

		super.setup(testDatabaseConnection);
	}

	@Test
	public void testObjectResolvingInHtmlServlet() {

		final List<String> testObjectIDs = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			// setup three different test objects to be found by HtmlServlet
			testObjectIDs.add(app.create("TestOne", new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 123)).getUuid());
			testObjectIDs.add(app.create("TestOne", new NodeAttribute<>(Traits.of("TestOne").key("aDouble"), 0.345)).getUuid());
			testObjectIDs.add(app.create("TestOne", new NodeAttribute<>(Traits.of("TestOne").key("aString"), "abcdefg")).getUuid());

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "testPage");
			if (newPage != null) {

				DOMElement html  = newPage.createElement("html");
				DOMElement head  = newPage.createElement("head");
				DOMElement body  = newPage.createElement("body");
				Content textNode = newPage.createTextNode("${current.id}");

				try {
					// add HTML element to page
					newPage.appendChild(html);
					html.appendChild(head);
					html.appendChild(body);
					body.appendChild(textNode);

				} catch (DOMException dex) {

					logger.warn("", dex);

					throw new FrameworkException(422, dex.getMessage());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;

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

	@Test
	public void testFileResolutionQuery() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", "File", "test.txt", true);

			uuid = file.getUuid();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final Query query = StructrApp.getInstance(securityContext).nodeQuery();

			query
				.and()
					.or()
					.andTypes(Traits.of("Page"))
					.andTypes(Traits.of("File"))
					.parent()
				.and(Traits.idProperty(), uuid);

			// Searching for pages needs super user context anyway
			List<Linkable> results = query.getAsList();

			System.out.println(results);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

	}
}
