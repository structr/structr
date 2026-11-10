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
import org.structr.core.graph.Tx;
import org.structr.rest.common.UsageStatistics;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.AssertJUnit.fail;

/**
 * The usage statistics shown in the resource access table and the page tree: which grants decided
 * a request and how, and which pages were viewed.
 */
public class GrantAndPageUsageTest extends StructrUiTest {

	@BeforeMethod
	public void forgetEarlierRequests() {

		UsageStatistics.clear();
	}

	@Test
	public void testRequestsAreCountedPerSignatureFlagAndOutcome() {

		createAdminUser();

		grant("Group", UiAuthenticator.NON_AUTH_USER_GET, true);

		RestAssured.given().expect().statusCode(200).when().get("/Group");
		RestAssured.given().expect().statusCode(200).when().get("/Group");
		RestAssured.given().contentType("application/json; charset=UTF-8").body("{ \"name\": \"Item\" }").expect().statusCode(401).when().post("/Group");

		// administrators do not need a grant, so their requests do not count for one
		RestAssured.given().headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/Group");

		// no grant for this signature, so nothing to count
		RestAssured.given().expect().statusCode(401).when().get("/MailTemplate");

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result.days", equalTo(30))
				.body("result.usage.'Group NON_AUTH_USER_GET allowed'.total", equalTo(2))
				.body("result.usage.'Group NON_AUTH_USER_POST denied'.total", equalTo(1))
				.body("result.usage.'Group AUTH_USER_GET allowed'", nullValue())
				.body("result.usage.'MailTemplate NON_AUTH_USER_GET denied'", nullValue())
			.when()
				.get("/stats?usage=grants");
	}

	@Test
	public void testPageViewsAreCountedWithoutPreviews() {

		createAdminUser();

		String pageId = null;

		try (final Tx tx = app.tx()) {

			pageId = Page.createSimplePage(securityContext, "usagePage").getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = htmlUrl;

		RestAssured.given().headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/usagePage");
		RestAssured.given().headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/usagePage");

		// the preview in the pages area is not a view
		RestAssured.given().headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/usagePage?edit=2");

		RestAssured.basePath = restUrl;

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result.usage.'" + pageId + "'.total", equalTo(2))
			.when()
				.get("/stats?usage=pages");
	}

	@Test
	public void testUsageStatisticsAreRestrictedToAdmins() {

		createAdminUser();

		// a monitoring account with the stats grant gets the request counts, but not the usage
		grant("stats", UiAuthenticator.NON_AUTH_USER_GET, true);

		RestAssured.given().expect().statusCode(200).when().get("/stats");
		RestAssured.given().expect().statusCode(403).when().get("/stats?usage=grants");

		RestAssured.given().headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(422).when().get("/stats?usage=files");
	}
}
