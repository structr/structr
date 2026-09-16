/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1592, under the premise that only an EXTERNAL user reaching the scripting engine is critical.
 *
 * <p>UploadServlet takes every non-file form field of the upload as a property of the file it creates
 * (it collects them into its params map and writes them as a PropertyMap, with
 * unlockSystemPropertiesOnce() around the write), so the uploader
 * decides what the new node looks like. File.isTemplate would turn the upload into server-side script:
 * a template file has its content evaluated whenever it is read (FileTraitWrapper.getInputStream()),
 * which would hand script execution to every user who may upload - by default every authenticated one,
 * and anonymous ones where application.uploads.allowanonymous is on. Ticket 1589's Content-Disposition
 * is no defence, because the evaluation happens while the file is being read, before anything is
 * delivered.
 *
 * <p>The only thing standing between an uploader and that is UploadServlet's AllowedProperties
 * whitelist, which is a list of property names with no note saying that isTemplate must never join it.
 * This test is that note.
 */
public class UploadedTemplateExecutionTest extends StructrUiTest {

	private static final String TESTER_PASSWORD = "correct-battery-staple";

	@Test
	public void testUploaderCannotTurnTheirUploadIntoAServerSideScript() {

		createTester();

		final String payload = "${{ $.doPrivileged(() => { $.find('User', { name: 'admin' })[0].eMail = 'owned@example.com'; }); }}";

		RestAssured.basePath = "/";

		RestAssured
			.given()
				.header(X_USER_HEADER,     "tester")
				.header(X_PASSWORD_HEADER, TESTER_PASSWORD)
				.multiPart("isTemplate",   "true")
				.multiPart("file", "payload.txt", payload.getBytes(StandardCharsets.UTF_8), "text/plain")
			.expect()
				.statusCode(422)
			.when()
				.post("structr/upload");

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
				.header(X_USER_HEADER,     "tester")
				.header(X_PASSWORD_HEADER, TESTER_PASSWORD)
			.when()
				.get("/payload.txt");

		try (final Tx tx = app.tx()) {

			final NodeInterface admin = app.nodeQuery(StructrTraits.USER).name(ADMIN_USERNAME).getFirst();

			assertNull("an uploaded file was evaluated as a server-side script",
				admin.getProperty(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- private methods -----
	private void createTester() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), TESTER_PASSWORD)
			);

			tx.success();

		} catch (Throwable t) {

			fail("Unexpected exception: " + t.getMessage());
		}
	}
}
