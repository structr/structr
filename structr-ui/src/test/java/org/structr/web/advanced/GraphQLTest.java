/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.advanced;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Image;

/**
 *
 */
public class GraphQLTest extends StructrUiTest {

	@Test
	public void testInheritedAttributes() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = GraphQLTest.class.getResourceAsStream("/test/test.png")) {

				ImageHelper.createImage(securityContext, is, "image/png", Image.class, "test.png", false);

				is.close();
			}

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/graphql";


		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body("{ Image { id, type, name, path }}")

			.expect()
				.statusCode(200)

			.when()
				.post("/");

	}
}
