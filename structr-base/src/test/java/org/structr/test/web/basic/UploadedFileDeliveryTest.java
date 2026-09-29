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
import org.structr.core.entity.Principal;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.app.StructrApp;
import org.structr.common.SecurityContext;
import org.structr.common.AccessMode;
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
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1589: an upload keeps the content type UploadServlet reads off the multipart part, contentType
 * is an ordinary writable property, and HtmlServlet streams the file with exactly that type. A file
 * uploaded as text/html and made public therefore runs as a document in the application's own origin -
 * send the link to an administrator and the script has their session against /structr/rest and the
 * websocket backend.
 *
 * <p>The answer is the sandbox policy, not a forced download: the document gets an opaque origin and no
 * scripts, so the payload cannot run at all. Delivery stays what RFC 6266 defines, inline unless the
 * CALLER asks for a download via downloadAsFilename.
 */
public class UploadedFileDeliveryTest extends StructrUiTest {

	@Test
	public void testScriptableContentTypesAreSandboxedAndServedInline() {

		for (final String contentType : new String[] { "text/html", "image/svg+xml", "application/xhtml+xml" }) {

			final String name = "payload-" + contentType.replaceAll("[^a-z]", "") + ".bin";

			createPublicFile(name, contentType, "<script>document.title='xss'</script>");

			RestAssured.basePath = htmlUrl;

			RestAssured
				.given()
				.expect()
					.statusCode(200)
					// no forced download: RFC 6266 makes a response without the header inline, and the
					// sandbox policy is what keeps the script from running in this origin
					.header("Content-Disposition", (String) null)
					.header("Content-Security-Policy", "sandbox; frame-ancestors 'none'")
					.header("X-Content-Type-Options", "nosniff")
				.when()
					.get("/" + name);
		}
	}

	/**
	 * The caller may still ask for a download, and then it is a download.
	 */
	@Test
	public void testDownloadAsFilenameStillForcesAttachment() {

		createPublicFile("report.html", "text/html", "<h1>report</h1>");

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
			.expect()
				.statusCode(200)
				.header("Content-Disposition", startsWith("attachment"))
			.when()
				.get("/report.html?_filename=report.html");
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

	/**
	 * A file an admin uploaded from a browser is the one case where nobody else wrote the content, so it
	 * is delivered as its content type says, with none of the protection an untrusted file needs.
	 */
	@Test
	public void testTrustedFilesAreServedWithoutProtection() {

		createPublicFile("developer.html", "text/html", "<h1>ours</h1>");
		markTrusted("developer.html");

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
			.expect()
				.statusCode(200)
				.contentType(startsWith("text/html"))
				.header("Content-Disposition", (String) null)
				.header("Content-Security-Policy", (String) null)
			.when()
				.get("/developer.html");
	}

	/**
	 * An untrusted response depends on who is asking and where they came from, so it must not be stored
	 * and handed to the next caller. Ticket 1589 follow-up: the 304 path used to answer from an entry
	 * that carried the treatment of whoever fetched the file first.
	 */
	@Test
	public void testUntrustedFilesAreNotCached() {

		createPublicFile("volatile.html", "text/html", "<h1>x</h1>");

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
			.expect()
				.statusCode(200)
				.header("Cache-Control", "private, no-store, max-age=0, must-revalidate")
				.header("Vary", "Cookie, Referer")
			.when()
				.get("/volatile.html");
	}

	/**
	 * An admin following a link from outside is the shape of the attack, so that request gets the
	 * download even where an anonymous visitor would be served the sandboxed document. A missing
	 * referrer counts as outside, because the page sending the admin here decides whether one is sent.
	 */
	@Test
	public void testAdminWithoutSameOriginReferrerGetsTheDownload() {

		createPublicFile("lure.html", "text/html", "<script>document.title='xss'</script>");

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the admin user: " + fex.getMessage());
		}

		RestAssured.basePath = htmlUrl;

		// anonymous: sandboxed, but displayed
		RestAssured
			.given()
			.expect()
				.statusCode(200)
				.header("Content-Disposition", (String) null)
			.when()
				.get("/lure.html");

		// admin, no referrer
		RestAssured
			.given()
				.header("X-User", "admin")
				.header("X-Password", "admin")
			.expect()
				.statusCode(200)
				.header("Content-Disposition", startsWith("attachment"))
			.when()
				.get("/lure.html");

		// admin, arriving from a page of this instance
		RestAssured
			.given()
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.header("Referer", baseUri + "some-page")
			.expect()
				.statusCode(200)
				.header("Content-Disposition", (String) null)
			.when()
				.get("/lure.html");
	}

	/**
	 * The two flags decide how content is treated, not what it says, so readOnly is not enough: any
	 * script can call unlockReadonlyPropertiesOnce().
	 */
	@Test
	public void testOnlyAdminsMaySetTheFlags() {

		createPublicFile("flagged.html", "text/html", "<h1>x</h1>");

		SecurityContext userContext = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER, "ordinary");
			user.setProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "ordinary");

			userContext = SecurityContext.getInstance(user.as(Principal.class), AccessMode.Backend);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception creating the user: " + fex.getMessage());
		}

		try (final Tx tx = StructrApp.getInstance(userContext).tx()) {

			final NodeInterface file = StructrApp.getInstance(userContext).nodeQuery(StructrTraits.FILE).name("flagged.html").getFirst();

			file.unlockReadOnlyPropertiesOnce();
			file.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.TRUSTED_PROPERTY), true);

			tx.success();

			fail("Setting trusted as a non-admin must be refused");

		} catch (FrameworkException expected) {

			assertEquals("Wrong status code refusing the flag", 403, expected.getStatus());
		}
	}

	// ----- private methods -----
	private void markTrusted(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).name(name).getFirst();

			file.unlockReadOnlyPropertiesOnce();
			file.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.TRUSTED_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception marking the file trusted: " + fex.getMessage());
		}
	}

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
