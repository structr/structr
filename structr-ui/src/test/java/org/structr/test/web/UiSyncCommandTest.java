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
package org.structr.test.web;

/**
 *
 *
 */
public class UiSyncCommandTest extends StructrUiTest {
	/*

	private static final Logger logger = LoggerFactory.getLogger(UiSyncCommandTest.class.getName());

	@Test
	public void testExportErrors() {

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.body("{}")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("application/json")
			.statusCode(400)
			.body("code", equalTo(400))
			.body("message", equalTo("Please specify mode, must be one of (import|export)"))
			.when()
			.post("/maintenance/syncUi");

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.body("{ mode: export }")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("application/json")
			.statusCode(400)
			.body("code", equalTo(400))
			.body("message", equalTo("Please specify file name using the file parameter."))
			.when()
			.post("/maintenance/syncUi");

	}

	@Test
	public void testSimpleExportRoundtrip() {

		final String fileName = super.basePath + "/exportTest.zip";
		Page testPage         = null;
		Head head             = null;
		File textFile     = null;
		File jsFile       = null;

		try (final Tx tx = app.tx()) {

			testPage = Page.createSimplePage(securityContext, "TestPage");
			head     = app.nodeQuery("Head").getFirst();
			textFile = app.create(StructrTraits.FILE, "testfile.txt");
			jsFile   = app.create(StructrTraits.FILE, "test.js");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			textFile.setProperties(textFile.getSecurityContext(), new PropertyMap(File.contentType, "text/plain"));
			IOUtils.write("This is a test file", textFile.getOutputStream());

			jsFile.setProperties(jsFile.getSecurityContext(), new PropertyMap(File.contentType, "application/javascript"));
			IOUtils.write("function test() {\n\tconsole.log('Test!');\n}", jsFile.getOutputStream());

			// link script to JS file
			final Script script = (Script)testPage.createElement("script");

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Script._src, "${link.name}?${link.version}");
			changedProperties.put(Script.linkable, jsFile);
			script.setProperties(script.getSecurityContext(), changedProperties);

			// link script into head
			head.appendChild(script);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}


		// verify that the database contains the expected number of nodes
		try (final Tx tx = app.tx()) {

			assertEquals("Database should contain 1 page",           1, app.nodeQuery(StructrTraits.PAGE).getAsList().size());
			assertEquals("Database should contain 10 DOM nodes",    11, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());
			assertEquals("Database should contain 3 content nodes",  3, app.nodeQuery(StructrTraits.CONTENT).getAsList().size());
			assertEquals("Database should contain 2 files",          2, app.nodeQuery(StructrTraits.FILE).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}


		// do export
		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.body("{ mode: export, file: '" + fileName + "' }")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("application/json")
			.statusCode(200)
			.when()
			.post("/maintenance/syncUi");


		// export done, now clean database
		try {
			app.command(ClearDatabase.class).execute();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}


		// verify that the database is empty
		try (final Tx tx = app.tx()) {

			assertEquals("Database should contain no pages",         0, app.nodeQuery(StructrTraits.PAGE).getAsList().size());
			assertEquals("Database should contain no DOM nodes",     0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());
			assertEquals("Database should contain no content nodes", 0, app.nodeQuery(StructrTraits.CONTENT).getAsList().size());
			assertEquals("Database should contain no files",         0, app.nodeQuery(StructrTraits.FILE).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}


		// do import
		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.body("{ mode: import, file: '" + fileName + "' }")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("application/json")
			.statusCode(200)
			.when()
			.post("/maintenance/syncUi");


		// verify that the database contains the expected number of nodes
		// (only one file is expected to be imported because it is
		// referenced in the page)
		try (final Tx tx = app.tx()) {

			assertEquals("Database should contain 1 page",           1, app.nodeQuery(StructrTraits.PAGE).getAsList().size());
			assertEquals("Database should contain 10 DOM nodes",    11, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());
			assertEquals("Database should contain 3 content nodes",  3, app.nodeQuery(StructrTraits.CONTENT).getAsList().size());
			assertEquals("Database should contain 1 file",           1, app.nodeQuery(StructrTraits.FILE).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}
	*/
}
