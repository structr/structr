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
package org.structr.test.web.auth;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.structr.api.config.Settings;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The health, histogram and metrics endpoints serve their data to the addresses on their whitelists, and to a request
 * that carries the login of an admin user from any address. The tests empty the whitelists, so that the address of
 * the test itself, which is localhost, admits nothing.
 */
public class MonitoringAccessTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	private String healthWhitelist    = null;
	private String histogramWhitelist = null;
	private String metricsWhitelist   = null;

	@BeforeMethod
	public void emptyWhitelists() {

		healthWhitelist    = Settings.HealthCheckWhitelist.getValue();
		histogramWhitelist = Settings.HistogramWhitelist.getValue();
		metricsWhitelist   = Settings.MetricsServletWhitelist.getValue();

		Settings.HealthCheckWhitelist.setValue("");
		Settings.HistogramWhitelist.setValue("");
		Settings.MetricsServletWhitelist.setValue("");
	}

	// users are created under the REST path, the monitoring endpoints lie beside it
	private void createUser(final String name, final boolean isAdmin) {

		RestAssured.basePath = restUrl;

		createEntityAsSuperUser("/User", "{ 'name': '" + name + "', 'password': '" + PASSWORD + "', 'isAdmin': " + isAdmin + " }");

		RestAssured.basePath = "/structr";
	}

	@AfterMethod
	public void restoreWhitelists() {

		Settings.HealthCheckWhitelist.setValue(healthWhitelist);
		Settings.HistogramWhitelist.setValue(histogramWhitelist);
		Settings.MetricsServletWhitelist.setValue(metricsWhitelist);

		RestAssured.basePath = restUrl;
	}

	@Test
	public void testARequestWithoutLoginIsRefusedOffTheWhitelist() {

		RestAssured.basePath = "/structr";

		// the health endpoint answers with the status alone
		final Response health = RestAssured.given().expect().statusCode(200).when().get("/health").andReturn();

		assertEquals("the health report must not be served without login off the whitelist", "", health.getBody().asString());

		RestAssured.given().expect().statusCode(403).when().get("/histogram");
		RestAssured.given().expect().statusCode(403).when().get("/metrics");
	}

	@Test
	public void testAnAdminIsServedOffTheWhitelistByCredentials() {

		createUser("boss", true);

		RestAssured
			.given()
				.header("X-User", "boss")
				.header("X-Password", PASSWORD)
			.expect()
				.statusCode(200)
				.header("Cache-Control", equalTo("no-store"))
				.body("status", equalTo("pass"))
			.when()
				.get("/health");

		RestAssured
			.given()
				.header("X-User", "boss")
				.header("X-Password", PASSWORD)
			.expect()
				.statusCode(200)
				.header("Cache-Control", equalTo("no-store"))
			.when()
				.get("/histogram");

		final Response metrics = RestAssured
			.given()
				.header("X-User", "boss")
				.header("X-Password", PASSWORD)
			.expect()
				.statusCode(200)
				.header("Cache-Control", equalTo("no-store"))
			.when()
				.get("/metrics")
			.andReturn();

		assertTrue("the metrics have to be served to an admin", metrics.getBody().asString().contains("structr_http_requests_total"));
	}

	@Test
	public void testAnAdminIsServedOffTheWhitelistBySession() {

		createUser("boss2", true);

		// logging in over REST needs its own permission
		RestAssured.basePath = restUrl;
		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
		RestAssured.basePath = "/structr";

		final String sessionId = RestAssured
			.given()
				.body("{ 'name': 'boss2', 'password': '" + PASSWORD + "' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/rest/login")
			.getSessionId();

		assertNotNull("the login did not answer with a session", sessionId);

		RestAssured
			.given()
				.sessionId(sessionId)
			.expect()
				.statusCode(200)
				.header("Cache-Control", equalTo("no-store"))
				.body("status", equalTo("pass"))
			.when()
				.get("/health");

		RestAssured.given().sessionId(sessionId).expect().statusCode(200).when().get("/histogram");
		RestAssured.given().sessionId(sessionId).expect().statusCode(200).when().get("/metrics");
	}

	@Test
	public void testAUserWhoIsNoAdminIsRefusedOffTheWhitelist() {

		createUser("tester", false);

		final Response health = RestAssured
			.given()
				.header("X-User", "tester")
				.header("X-Password", PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/health")
			.andReturn();

		assertFalse("the health report must not be served to a user who is no admin", health.getBody().asString().contains("status"));

		RestAssured.given().header("X-User", "tester").header("X-Password", PASSWORD).expect().statusCode(403).when().get("/histogram");
		RestAssured.given().header("X-User", "tester").header("X-Password", PASSWORD).expect().statusCode(403).when().get("/metrics");
	}

	@Test
	public void testWrongCredentialsAreRefusedOffTheWhitelist() {

		createUser("boss3", true);

		RestAssured.given().header("X-User", "boss3").header("X-Password", "wrong").expect().statusCode(403).when().get("/histogram");
		RestAssured.given().header("X-User", "boss3").header("X-Password", "wrong").expect().statusCode(403).when().get("/metrics");
	}
}
