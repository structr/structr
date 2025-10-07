/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.files;

import io.restassured.RestAssured;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;


/**
 */
public class DynamicFileTest extends StructrUiTest {

	@Test
	public void testDynamicFileWithDynamicContentTypeAndFilename() {

		RestAssured.basePath = "/";

		try (final Tx tx = app.tx()) {

			final NodeInterface templateFile = FileHelper.createFile(securityContext, """
	${{
		$.setResponseHeader('Content-Type', 'application/json');
		$.setResponseHeader('Content-Disposition', `attachment; filename="data.json"`);
		
		$.print(JSON.stringify({ result: 'success' }));
	}}
	""".getBytes(), "text/plain", StructrTraits.FILE, "myfile.txt", true);

			templateFile.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), true);
			templateFile.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			tx.success();

		} catch (FrameworkException | IOException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String expected = """
  {"result":"success"}
  """;

		RestAssured
				.given()
				.expect()
					.statusCode(200)
					.header("Content-Type", "application/json")
					.header("Content-Disposition", Matchers.containsString("filename=\"data.json\""))
					.body(Matchers.equalTo(expected))
				.when()
					.get("myfile.txt");
	}
}
