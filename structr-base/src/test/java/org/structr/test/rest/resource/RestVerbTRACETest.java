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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.SuperUserAuthenticator;
import org.structr.rest.service.HttpService;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;

public class RestVerbTRACETest extends StructrRestTestBase {

	@Override
	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	public void setup(@Optional String testDatabaseConnection) {

		super.setup(testDatabaseConnection, "JsonRestServlet HtmlServlet CsvServlet UploadServlet ProxyServlet DeploymentServlet FlowServlet LoginServlet LogoutServlet TokenServlet EventSourceServlet HealthCheckServlet HistogramServlet OpenAPIServlet MetricsServlet");
	}

	@Test
	public void test01TRACENotAllowed() {

		expectTraceForPathNotAllowedAndHeaderNotReflected("/");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/rest");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/ws");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/csv");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/docs");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/pdf");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/config");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/metrics");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/eventSource");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/upload");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/proxy");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/login");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/logout");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/token");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/healthcheck");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/histogram");
		expectTraceForPathNotAllowedAndHeaderNotReflected("/structr/openapi");
	}

	// ----- private methods -----
	private void expectTraceForPathNotAllowedAndHeaderNotReflected(final String path) {

		RestAssured.given()
				.header("X-Debug-Echo", "please-do-not-reflect-me")
				.when()
				.request("TRACE", path)
				.then()
				.statusCode(405)
				.body(not(containsString("please-do-not-reflect-me")));
	}
}
