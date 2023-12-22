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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;
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

		Image image = null;
		// setup
		try (final Tx tx = app.tx()) {

			try (final InputStream is = GraphQLTest.class.getResourceAsStream("/test/test.png")) {

				image = ImageHelper.createImage(securityContext, is, "image/png", Image.class, "test.png", false);
				image.getProperty(StructrApp.key(Image.class, "tnSmall"));
				is.close();
			}

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image currentImage = image;
			tryWithTimeout(() -> currentImage.getProperty(StructrApp.key(Image.class, "tnSmall")) != null, () -> fail("Exceeded timeout while waiting for thumbnail to be available"), 30000, 500);

			tx.success();
		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/graphql";

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.contentType("application/json; charset=UTF-8")
				.body("{ Image(isThumbnail: {_equals: false}) { tnSmall { imageData, base64Data }}}")

			.expect()
				.statusCode(200)
				.body("Image[0].tnSmall.base64Data", equalTo("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP4DwQACfsD/Wj6HMwAAAAASUVORK5CYII="))
				.body("Image[0].tnSmall.imageData",  equalTo("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP4DwQACfsD/Wj6HMwAAAAASUVORK5CYII="))

			.when()
				.post("/");

	}
}
