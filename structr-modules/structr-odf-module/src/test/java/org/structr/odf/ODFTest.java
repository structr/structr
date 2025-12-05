/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import io.restassured.RestAssured;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.odf.traits.definitions.ODFExporterTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.assertNotNull;

public class ODFTest extends ODSTestBase {

	@Test
	public void testODS() {

		final String type = StructrTraits.ODS_EXPORTER;
		File template     = null;
		String uuid       = null;

		assertNotNull("Type ODSExporter should exist", type);

		try (final Tx tx = app.tx()) {

			final Traits odfTraits  = Traits.of(StructrTraits.ODF_EXPORTER);

			createAdminUser();

			// read test file from sources
			try (final InputStream is = ODFTest.class.getResourceAsStream("/test.odt")) {

				template = FileHelper.createFile(securityContext, is, "", StructrTraits.FILE, "template").as(File.class);
			}

			assertNotNull("Test file must exist", template);

			final NodeInterface node = app.create(type,
				new NodeAttribute<>(odfTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "test.ods"),
				new NodeAttribute<>(odfTraits.key(ODFExporterTraitDefinition.DOCUMENT_TEMPLATE_PROPERTY), template)
			);

			uuid = node.getUuid();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to call exported methods on file
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.when()
			.post("/ODSExporter/" + uuid + "/createDocumentFromTemplate");

		// use RestAssured to call exported methods on file
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
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
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo(StructrTraits.ODS_EXPORTER))
			.body("result[0].documentTemplate.type", equalTo(StructrTraits.FILE))
			.body("result[0].documentTemplate.name", equalTo("template"))
			.body("result[0].documentTemplate.path", equalTo("/template"))
			.body("result[0].resultDocument.type",   equalTo(StructrTraits.FILE))
			.body("result[0].resultDocument.name",   equalTo("test.ods_template"))
			.body("result[0].resultDocument.path",   equalTo("/test.ods_template"))
			.when()
			.get("/ODFExporter?_sort=name");
	}
}
