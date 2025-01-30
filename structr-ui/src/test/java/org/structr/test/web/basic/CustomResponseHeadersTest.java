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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;


public class CustomResponseHeadersTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(CustomResponseHeadersTest.class.getName());

	@Test
	public void testCustomHtmlAttribute() {

		try (final Tx tx = app.tx()) {

			// create admin user
			createTestNode("User",
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);
			// create a page
			final Page newPage = Page.createNewPage(securityContext, "customHeadersTestPage");

			newPage.setVisibility(true, true);

			final DOMElement html  = createElement(newPage, newPage, "html");
			final DOMElement head  = createElement(newPage, html, "head");
			final DOMElement title = createElement(newPage, head, "title", "Test Page for custom HTTP headers");
			final DOMElement body  = createElement(newPage, html, "body");
			final DOMElement div1  = createElement(newPage, body, "div", "testing..${set_response_header('X-Test', 'abc123')}");
			final DOMElement div2  = createElement(newPage, body, "div", "testing..${set_response_header('Strict-Transport-Security', 'max-age=120')}");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		RestAssured.basePath = htmlUrl;

		RestAssured
		.given()
			.accept("text/html")
			.headers("X-User", "admin" , "X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))

		.expect()
			.statusCode(200)
			.contentType("text/html;charset=utf-8")
			.header("Strict-Transport-Security", "max-age=120")
			.header("X-Content-Type-Options", "nosniff")
			.header("X-Frame-Options", "SAMEORIGIN")
			.header("X-XSS-Protection", "1;mode=block")
			.header("X-Test", "abc123")

		.when()
			.get("/customHeadersTestPage");



	//"Strict-Transport-Security:max-age=60,X-Content-Type-Options:nosniff,X-Frame-Options:SAMEORIGIN,X-XSS-Protection:1;mode=block", "List of custom response headers that will be added to every HTTP response");

	}

	private DOMElement createElement(final Page page, final DOMNode parent, final String tag, final String... content) throws FrameworkException {

		final DOMElement child = page.createElement(tag);

		parent.appendChild(child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final DOMNode node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
