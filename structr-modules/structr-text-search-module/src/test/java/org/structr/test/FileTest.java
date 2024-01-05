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
package org.structr.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.test.web.advanced.HttpFunctionsTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

public class FileTest extends StructrUiTest {

	@Test
	public void testGetSearchContextMethod() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			// create admin user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			// create test file
			final File newFile = app.create(File.class, "test.txt");

			try (final PrintWriter writer = new PrintWriter(newFile.getOutputStream())) {

				writer.print("This is a test file that contains some words.");
			}

			uuid = newFile.getUuid();

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
		}

		// wait for content to be extracted so we can use getSearchContext
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// test REST call
		RestAssured.basePath = "/structr/rest";
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.header("X-User",     "admin")
				.header("X-Password", "admin")
				.body("{ searchString: 'that', contextLength: 5 }")
			.expect()
				.statusCode(200)
				.body("result.context[0]", equalTo("test file that contains some"))
			.when()
				.post("/File/" + uuid + "/getSearchContext");

		// test scripting call with two different argument types (unnamed and map)
		try (final Tx tx = app.tx()) {

			final File file           = app.get(File.class, uuid);
			final GraphObjectMap map1 = (GraphObjectMap)Scripting.evaluate(new ActionContext(securityContext), file, "${{ return $.this.getSearchContext('that', 5); }}", "testGetSearchContextMethod");
			final GraphObjectMap map2 = (GraphObjectMap)Scripting.evaluate(new ActionContext(securityContext), file, "${{ return $.this.getSearchContext({ searchString: 'that', contextLength: 5 }); }}", "testGetSearchContextMethod");

			HttpFunctionsTest.assertMapPathValueIs(map1.toMap(), "context.0", "test file that contains some");
			HttpFunctionsTest.assertMapPathValueIs(map2.toMap(), "context.0", "test file that contains some");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testExtractStructureMethod() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			// create admin user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			try (final InputStream is = FileTest.class.getResourceAsStream("/test/test.pdf")) {

				final File newFile = FileHelper.createFile(securityContext, is, "application/pdf", File.class, "test.pdf");

				uuid = newFile.getUuid();
			}

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
		}

		// wait for content to be extracted so we can use getSearchContext
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// test REST call (with incorrect arguments)
		RestAssured.basePath = "/structr/rest";
		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.header("X-User",     "admin")
				.header("X-Password", "admin")
				.body("{ searchString: 'that', contextLength: 5 }")
			.expect()
				.statusCode(200)
			.when()
				.post("/File/" + uuid + "/extractStructure");

		// test scripting calls with two different argument types (no arguments and empty map)
		try (final Tx tx = app.tx()) {

			final File file = app.get(File.class, uuid);
			final Map map1  = (Map)Scripting.evaluate(new ActionContext(securityContext), file, "${{ return $.this.extractStructure(); }}", "testExtractStructureMethod");
			final Map map2  = (Map)Scripting.evaluate(new ActionContext(securityContext), file, "${{ return $.this.extractStructure({ }); }}", "testExtractStructureMethod");

			HttpFunctionsTest.assertMapPathValueIs(map1, "success", true);
			HttpFunctionsTest.assertMapPathValueIs(map2, "success", true);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}
}
