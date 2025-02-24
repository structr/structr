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
import org.hamcrest.Matchers;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.fail;


public class BasicAuthTest extends StructrUiTest {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {
		super.setup(testDatabaseConnection);
		Settings.HttpBasicAuthEnabled.setValue(true);
	}

	@Test
	public void test00BasicAuthOnPage() {

		RestAssured.basePath = "/";
		String userUUID = "";

		try (final Tx tx = app.tx()) {

			final Page page1 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test1"));
			final Page page2 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test2"));
			final Page page3 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test3"));

			page1.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			page1.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			page2.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			page2.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "realm");
			page2.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			page3.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			page3.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "Enter password for ${this.name}");
			page3.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			final NodeInterface tester = createUser();
			tester.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			tester.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToPublicUsers"), true);
			userUUID = tester.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// 1. Status code + Auto-generated Realm
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1\"").when().get("/test1");

		// 2. Status Code + Fixed realm
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("/test2");

		// 3. Status Code + Fixed realm + request parameter
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("/test2?hacks=areReal");

		// 4. Status Code + script realm + current object for page
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"Enter password for test3\"").when().get("/test3/" + userUUID);

		// 5. Status Code + script realm + current object for page + request parameter
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"Enter password for test3\"").when().get("/test3/" + userUUID + "?hacks=areReal");


		// test successful basic auth
		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body("html.head.title", Matchers.equalTo("Test1")).body("html.body.h1", Matchers.equalTo("Test1")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/test1");

		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body("html.head.title", Matchers.equalTo("Test2")).body("html.body.h1", Matchers.equalTo("Test2")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/test2");

		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body("html.head.title", Matchers.equalTo("Test3")).body("html.body.h1", Matchers.equalTo("Test3")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/test3");
	}

	@Test
	public void test01BasicAuthOnFile() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final NodeInterface file1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", StructrTraits.FILE, "test1.txt", true);
			final NodeInterface file2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", StructrTraits.FILE, "test2.txt", true);

			final NodeInterface folder1 = FileHelper.createFolderPath(securityContext, "/myFolder");
			final NodeInterface file3 = FileHelper.createFile(securityContext, "test3".getBytes(), "text/plain", StructrTraits.FILE, "test3.txt", true);
			file3.as(File.class).setParent(folder1.as(Folder.class));

			final NodeInterface file4 = FileHelper.createFile(securityContext, "You said '${request.message}' and your name is '${me.name}'.".getBytes(), "text/plain", StructrTraits.FILE, "test4.txt", true);
			file4.setProperty(Traits.of(StructrTraits.FILE).key("isTemplate"), true);
			file4.as(File.class).setParent(folder1.as(Folder.class));

			file1.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file1.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			file2.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file2.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "realm");
			file2.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			file3.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file3.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);
			file3.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "Enter password for ${this.path}");

			file4.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file4.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);
			file4.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "Enter password for ${this.path}");

			createUser();

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// 1. Status code + Auto-generated Realm
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1.txt\"").when().get("test1.txt");

		// 2. Status Code + Fixed realm
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("test2.txt");

		// 3. Status Code + Fixed realm + request parameter
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").when().get("test2.txt?t=1234567890");

		// 4. Status Code + script realm + file in subfolder
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"Enter password for /myFolder/test3.txt\"").when().get("/myFolder/test3.txt");

		// 5. Status Code + script realm + file in subfolder + request parameter
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"Enter password for /myFolder/test3.txt\"").when().get("/myFolder/test3.txt?t=1234567890");

		// 6. Status Code + script realm + dynamic file in subfolder + request parameter
		RestAssured.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"Enter password for /myFolder/test4.txt\"").when().get("/myFolder/test4.txt?message=Hello");

		// test successful basic auth
		RestAssured
				.given().auth().basic("tester", "test")
				.expect().statusCode(200).body(Matchers.equalTo("test1"))
				.when().get("test1.txt");

		RestAssured
				.given().auth().basic("tester", "test")
				.expect().statusCode(200).body(Matchers.equalTo("test2"))
				.when().get("test2.txt");

		RestAssured
				.given().auth().basic("tester", "test")
				.expect().statusCode(200).body(Matchers.equalTo("test3"))
				.when().get("/myFolder/test3.txt");

		RestAssured
				.given().auth().basic("tester", "test")
				.expect().statusCode(200).body(Matchers.equalTo("You said 'Hello' and your name is 'tester'."))
				.when().get("/myFolder/test4.txt?message=Hello");

	}

	@Test
	public void test02BasicAuthOnPageWithErrorPage() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final Page error = makeVisible(Page.createSimplePage(securityContext, "error"));
			error.setProperty(Traits.of(StructrTraits.PAGE).key("showOnErrorCodes"), "401");

			final Page page1 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test1"));
			final Page page2 = makeVisibleToAuth(Page.createSimplePage(securityContext, "test2"));

			page1.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			page1.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			page2.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			page2.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "realm");
			page2.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test error page as result of unauthorized request
		RestAssured
			.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1\"").body("html.head.title", Matchers.equalTo("Error")).body("html.body.h1", Matchers.equalTo("Error")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/html/test1");

		RestAssured
			.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").body("html.head.title", Matchers.equalTo("Error")).body("html.body.h1", Matchers.equalTo("Error")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/html/test2");


		// test successful basic auth
		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body("html.head.title", Matchers.equalTo("Test1")).body("html.body.h1", Matchers.equalTo("Test1")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/html/test1");

		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body("html.head.title", Matchers.equalTo("Test2")).body("html.body.h1", Matchers.equalTo("Test2")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("/html/test2");

	}

	@Test
	public void test03BasicAuthOnFileWithErrorPage() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final Page error = makeVisible(Page.createSimplePage(securityContext, "error"));
			error.setProperty(Traits.of(StructrTraits.PAGE).key("showOnErrorCodes"), "401");

			final NodeInterface file1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", StructrTraits.FILE, "test1.txt", true);
			final NodeInterface file2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", StructrTraits.FILE, "test2.txt", true);

			file1.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file1.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			file2.setProperty(Traits.of(StructrTraits.PAGE).key("visibleToAuthenticatedUsers"), true);
			file2.setProperty(Traits.of(StructrTraits.PAGE).key("basicAuthRealm"), "realm");
			file2.setProperty(Traits.of(StructrTraits.PAGE).key("enableBasicAuth"), true);

			createUser();

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test error page as result of unauthorized request
		RestAssured
			.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"test1.txt\"").body("html.head.title", Matchers.equalTo("Error")).body("html.body.h1", Matchers.equalTo("Error")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("test1.txt");

		RestAssured
			.expect().statusCode(401).header("WWW-Authenticate", "BASIC realm=\"realm\"").body("html.head.title", Matchers.equalTo("Error")).body("html.body.h1", Matchers.equalTo("Error")).body("html.body.div", Matchers.equalTo("Initial body text"))
			.when().get("test2.txt");


		// test successful basic auth
		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body(Matchers.equalTo("test1"))
			.when().get("test1.txt");

		RestAssured
			.given().auth().basic("tester", "test")
			.expect().statusCode(200).body(Matchers.equalTo("test2"))
			.when().get("test2.txt");
	}

	// ----- private methods -----
	private NodeInterface createUser() throws FrameworkException {

		return createTestNode(StructrTraits.USER,
			new NodeAttribute<>(Traits.of(StructrTraits.USER).key("name"), "tester"),
			new NodeAttribute<>(Traits.of(StructrTraits.USER).key("password"), "test")
		);
	}

	private <T extends DOMNode> T makeVisible(final T src) {

		try {

			src.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToAuthenticatedUsers"), true);
			src.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToPublicUsers"), true);

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {
				n.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToAuthenticatedUsers"), true);
				n.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToPublicUsers"), true);

			} catch (FrameworkException fex) {}
		} );

		return src;
	}

	private <T extends DOMNode> T makeVisibleToAuth(final T src) {

		try {

			src.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToAuthenticatedUsers"), true);

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {
				n.setProperty(Traits.of(StructrTraits.DOM_NODE).key("visibleToAuthenticatedUsers"), true);

			} catch (FrameworkException fex) {}
		} );

		return src;
	}
}
