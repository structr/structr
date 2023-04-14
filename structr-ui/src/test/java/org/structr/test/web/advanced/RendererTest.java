/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class RendererTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(RendererTest.class);

	@Test
	public void testMarkdownRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/markdown");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}

	/*
	@Test
	public void testTextileRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/textile");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}

	@Test
	public void testMediaWikiRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/mediawiki");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}

	@Test
	public void testTracWikiRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/tracwiki");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}

	@Test
	public void testConfluenceRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/confluence");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}

	@Test
	public void testAsciidocRenderer() {

		Content content = null;

		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			final Page page1 = Page.createSimplePage(securityContext, "page1");

			final Element div = (Element)page1.getElementsByTagName("div").item(0);
			content           = (Content)div.getFirstChild();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			// test markdown content
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/asciidoc");
			content.setProperty(StructrApp.key(Content.class, "content"),
				"# Title\n" +
				"This is a test\n\n" +
				"## Another title\n"
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test successful basic auth
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",    Matchers.equalTo("Page1"))
			.body("html.body.h1",       Matchers.equalTo("Page1"))
			.body("html.body.div.h1.a", Matchers.equalTo("Title"))
			.body("html.body.div.p",    Matchers.equalTo("This is a test"))
			.body("html.body.div.h2.a", Matchers.equalTo("Another title"))
			.when()
			.get("/html/page1");
	}
	*/
}
