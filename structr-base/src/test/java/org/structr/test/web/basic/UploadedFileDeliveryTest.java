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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1589: an upload keeps the content type UploadServlet reads off the multipart part, contentType
 * is an ordinary writable property, and HtmlServlet streams the file with exactly that type. A file
 * uploaded as text/html and made public therefore runs as a document in the application's own origin -
 * send the link to an administrator and the script has their session against /structr/rest and the
 * websocket backend.
 *
 * <p>Content-Disposition was only set when the CALLER asked for it via downloadAsFilename, which is the
 * one thing an attacker sending a link will not do.
 */
public class UploadedFileDeliveryTest extends StructrUiTest {

	@Test
	public void testScriptableContentTypesAreServedAsAttachment() {

		for (final String contentType : new String[] { "text/html", "image/svg+xml", "application/xhtml+xml" }) {

			final String name = "payload-" + contentType.replaceAll("[^a-z]", "") + ".bin";

			createPublicFile(name, contentType, "<script>document.title='xss'</script>");

			RestAssured.basePath = htmlUrl;

			RestAssured
				.given()
				.expect()
					.statusCode(200)
					.header("Content-Disposition", startsWith("attachment"))
					.header("X-Content-Type-Options", "nosniff")
				.when()
					.get("/" + name);
		}
	}

	/**
	 * The counterpart: the rule must not turn the file area into a download-only area. An image is not a
	 * document and keeps being served inline.
	 */
	@Test
	public void testOrdinaryContentTypesAreStillServedInline() {

		createPublicFile("harmless.txt", "text/plain", "just text");

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
			.expect()
				.statusCode(200)
				.contentType(startsWith("text/plain"))
				.header("Content-Disposition", (String) null)
				.header("X-Content-Type-Options", "nosniff")
			.when()
				.get("/harmless.txt");
	}

	// ----- private methods -----
	private void createPublicFile(final String name, final String contentType, final String content) {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, content.getBytes(), contentType, StructrTraits.FILE, name, false);

			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			// the attacker sets the type; that it survives the upload is the point of the ticket
			file.as(File.class).setProperty(Traits.of(StructrTraits.FILE).key("contentType"), contentType);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception creating the file: " + ex.getMessage());
		}
	}
}
