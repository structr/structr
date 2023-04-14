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
package org.structr.odf;

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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.odf.entity.ODFExporter;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.assertNotNull;

public class ODFTest extends StructrUiTest {

	@Test
	public void testODS() {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("ODSExporter");
		File template    = null;
		String uuid      = null;

		assertNotNull("Type ODSExporter should exist", type);

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			// read test file from sources
			try (final InputStream is = ODFTest.class.getResourceAsStream("/test.odt")) {

				template = FileHelper.createFile(securityContext, is, "", File.class, "template");
			}

			assertNotNull("Test file must exist", template);

			final NodeInterface node = app.create(type,
				new NodeAttribute<>(StructrApp.key(ODFExporter.class, "name"),             "test.ods"),
				new NodeAttribute<>(StructrApp.key(ODFExporter.class, "documentTemplate"), template)
			);

			uuid = node.getUuid();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to call exported methods on file
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.post("/ODSExporter/" + uuid + "/createDocumentFromTemplate");

		// use RestAssured to call exported methods on file
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.body("result_count",    equalTo(2))
			.body("result",          hasSize(2))
			.body("result[0].name",  equalTo("template"))
			.body("result[1].name",  equalTo("test.ods_template"))
			.when()
			.get("/File?_sort=name");

		// use RestAssured to call exported methods on file
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo("ODSExporter"))
			.body("result[0].documentTemplate.type", equalTo("File"))
			.body("result[0].documentTemplate.name", equalTo("template"))
			.body("result[0].documentTemplate.path", equalTo("/template"))
			.body("result[0].resultDocument.type",   equalTo("File"))
			.body("result[0].resultDocument.name",   equalTo("test.ods_template"))
			.body("result[0].resultDocument.path",   equalTo("/test.ods_template"))
			.when()
			.get("/ODFExporter?_sort=name");
	}
}
