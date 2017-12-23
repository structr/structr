/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.basic;

import com.jayway.restassured.RestAssured;
import java.io.IOException;
import org.hamcrest.Matchers;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;


public class BasicAuthTest extends StructrUiTest {

	@Test
	public void test00BasicAuthOnPage() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final Page page1 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test1"));
			final Page page2 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test2"));

			page1.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			page1.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			page2.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			page2.setProperty(StructrApp.key(Page.class, "basicAuthRealm"), "realm");
			page2.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test response status code only
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1\"").when().get("/html/test1");
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("/html/test2");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Test1"))
			.body("html.body.h1", Matchers.equalTo("Test1"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test1");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Test2"))
			.body("html.body.h1", Matchers.equalTo("Test2"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test2");
	}

	@Test
	public void test01BasicAuthOnFile() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final File file1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", File.class, "test1.txt");
			final File file2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", File.class, "test2.txt");

			file1.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			file1.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			file2.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			file2.setProperty(StructrApp.key(Page.class, "basicAuthRealm"), "realm");
			file2.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1.txt\"").when().get("test1.txt");
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("test2.txt");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body(Matchers.equalTo("test1"))
			.when()
			.get("test1.txt");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body(Matchers.equalTo("test2"))
			.when()
			.get("test2.txt");

	}

	@Test
	public void test02BasicAuthOnPageWithErrorPage() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final Page error = makeVisible(Page.createSimplePage(securityContext, "error"));
			error.setProperty(StructrApp.key(Page.class, "showOnErrorCodes"), "401");

			final Page page1 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test1"));
			final Page page2 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test2"));

			page1.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			page1.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			page2.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			page2.setProperty(StructrApp.key(Page.class, "basicAuthRealm"), "realm");
			page2.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured
			.expect()
			.statusCode(401)
			.header("WWW-Authenticate", "BASIC realm=\"test1\"")
			.body("html.head.title", Matchers.equalTo("Error"))
			.body("html.body.h1", Matchers.equalTo("Error"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test1");

		RestAssured
			.expect()
			.statusCode(401)
			.header("WWW-Authenticate", "BASIC realm=\"realm\"")
			.body("html.head.title", Matchers.equalTo("Error"))
			.body("html.body.h1", Matchers.equalTo("Error"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test2");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Test1"))
			.body("html.body.h1", Matchers.equalTo("Test1"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test1");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Test2"))
			.body("html.body.h1", Matchers.equalTo("Test2"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("/html/test2");

	}

	@Test
	public void test03BasicAuthOnFileWithErrorPage() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final Page error = makeVisible(Page.createSimplePage(securityContext, "error"));
			error.setProperty(StructrApp.key(Page.class, "showOnErrorCodes"), "401");

			final File file1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", File.class, "test1.txt");
			final File file2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", File.class, "test2.txt");

			file1.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			file1.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			file2.setProperty(StructrApp.key(Page.class, "visibleToAuthenticatedUsers"), true);
			file2.setProperty(StructrApp.key(Page.class, "basicAuthRealm"), "realm");
			file2.setProperty(StructrApp.key(Page.class, "enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured
			.expect()
			.statusCode(401)
			.header("WWW-Authenticate", "BASIC realm=\"test1.txt\"")
			.body("html.head.title", Matchers.equalTo("Error"))
			.body("html.body.h1", Matchers.equalTo("Error"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("test1.txt");

		RestAssured
			.expect()
			.statusCode(401)
			.header("WWW-Authenticate", "BASIC realm=\"realm\"")
			.body("html.head.title", Matchers.equalTo("Error"))
			.body("html.body.h1", Matchers.equalTo("Error"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get("test2.txt");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body(Matchers.equalTo("test1"))
			.when()
			.get("test1.txt");

		// test successful basic auth
		RestAssured
			.given()
			.authentication()
			.basic("tester", "test")
			.expect()
			.statusCode(200)
			.body(Matchers.equalTo("test2"))
			.when()
			.get("test2.txt");
	}

	// ----- private methods -----
	private void createUser() throws FrameworkException {

		createTestNode(User.class,
			new NodeAttribute<>(StructrApp.key(User.class, "name"), "tester"),
			new NodeAttribute<>(StructrApp.key(User.class, "password"), "test")
		);
	}

	private <T extends DOMNode> T makeVisible(final T src) {

		try {

			src.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), true);
			src.setProperty(StructrApp.key(DOMNode.class, "visibleToPublicUsers"), true);

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {
				n.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), true);
				n.setProperty(StructrApp.key(DOMNode.class, "visibleToPublicUsers"), true);

			} catch (FrameworkException fex) {}
		} );

		return src;
	}

	private <T extends DOMNode> T makeVisibleToAuth(final T src) {

		try {

			src.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), true);

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {
				n.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), true);

			} catch (FrameworkException fex) {}
		} );

		return src;
	}
}
