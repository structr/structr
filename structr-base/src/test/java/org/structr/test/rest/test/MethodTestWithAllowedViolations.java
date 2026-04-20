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
package org.structr.test.rest.test;

import io.restassured.RestAssured;
import org.eclipse.jetty.http.UriCompliance;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpService;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class MethodTestWithAllowedViolations extends StructrRestTestBase {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		super.setup(testDatabaseConnection);

		final HttpService httpService = Services.getInstance().getServiceImplementation(HttpService.class);

		// set this after test setup because otherwise... fubar
		HttpService.UriComplianceAllowedViolations.setValue(String.join(" ", List.of(
				UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT.getName(),		// for empty parts "//"
				UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR.getName(),		// for "%2f" => "/"
				UriCompliance.Violation.AMBIGUOUS_PATH_ENCODING.getName()		// for "%25" => "%"
		)));

		// attempt restart of HttpService to get the updated violations
		if (httpService != null) {

			httpPort = httpService.getAllocatedPort();
			final String serviceName = "HttpService";

			if (httpService.isRunning()) {

				httpService.shutdown();

				try {

					Services.getInstance().startService(serviceName);

				} catch (FrameworkException fex) {

					fail(fex.getMessage());
				}
			}

			Settings.HttpPort.setValue(httpPort);
		}
	}

	@Test
	public void testGETMethodParametersInURL() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");

			// methods
			base.addMethod("test1", "{ return $.methodParameters; }")
				.addParameter("key1", "String")
				.addParameter("key2", "Integer")
				.setHttpVerb("GET");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String base  = createEntity("/BaseType", "{ name: 'BaseType' }");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result.key1", equalTo("value1"))
				.body("result.key2", equalTo(2))
			.when()
				.get("/BaseType/" + base + "/test1/value1/2");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(422)
				.body("code",                equalTo(422))
				.body("message",             equalTo("Cannot parse input for ‛key2‛ in method ‛BaseType.test1‛"))
				.body("errors[0].method",    equalTo("test1"))
				.body("errors[0].parameter", equalTo("key2"))
				.body("errors[0].token",     equalTo("must_be_numerical"))
				.body("errors[0].value",     equalTo("two"))
			.when()
				.get("/BaseType/" + base + "/test1//two");
				// RestAssured translates this to /test1%2f/two - this will fail as soon as these escape characters are not interpreted as actual slashes anymore (for a solution, see DynamicPathsTest)
				// In DynamicPathsTest we use raw HttpRequest to allow us to request such seemingly "broken" resources
		}

}
