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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.ImageHelper;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class GraphQLTest extends StructrUiTest {

	@Test
	public void testDataPropertyOnThumbnail() {

		NodeInterface image = null;
		// setup
		try (final Tx tx = app.tx()) {

			try (final InputStream is = GraphQLTest.class.getResourceAsStream("/test/test.png")) {

				image = ImageHelper.createImage(securityContext, is, "image/png", StructrTraits.IMAGE, "test.png", false);
				image.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall"));
				is.close();
			}

			createAdminUser();

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface currentImage = image;

			tryWithTimeout(
				() -> {
					// Thumbnail creation happens in the background, in a different thread,
					// so we need to allow this thread to break the transaction isolation..
					currentImage.getNode().invalidate();

					return currentImage.getProperty(Traits.of(StructrTraits.IMAGE).key("tnSmall")) != null;
				},
				() -> fail("Exceeded timeout while waiting for thumbnail to be available"),
				30000, 500);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/graphql";

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.contentType("application/json; charset=UTF-8")
				.body("{ Image(isThumbnail: {_equals: false}) { tnSmall { imageData, base64Data }}}")

			.expect()
				.statusCode(200)
				.body("Image[0].tnSmall.base64Data", equalTo("iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAIAAAD/gAIDAAAA5ElEQVR4Xu3QsQkAMAzAsPz/dLsHCvUujR49h2+zA29mBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFFy3DwZ3Bok0PAAAAAElFTkSuQmCC"))
				.body("Image[0].tnSmall.imageData",  equalTo("iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAIAAAD/gAIDAAAA5ElEQVR4Xu3QsQkAMAzAsPz/dLsHCvUujR49h2+zA29mBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFZgVmBWYFFy3DwZ3Bok0PAAAAAElFTkSuQmCC"))

			.when()
				.post("/");

	}
}
