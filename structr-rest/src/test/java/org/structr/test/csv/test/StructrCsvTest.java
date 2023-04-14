/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.test.csv.test;


import io.restassured.RestAssured;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.SuperUserAuthenticator;
import org.structr.rest.DefaultResourceProvider;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Base class CSV tests
 *
 * All tests are executed in superuser context.
 */
public class StructrCsvTest extends StructrRestTestBase {

	protected static final String csvUrl = "/structr/csv";

	@Parameters("testDatabaseConnection")
	@BeforeClass
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		final long timestamp = System.nanoTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService SchemaService HttpService");

		setupDatabaseConnection(testDatabaseConnection);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet CsvServlet");
		Settings.RestAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.RestResourceProvider.setValue(DefaultResourceProvider.class.getName());
		Settings.RestUserClass.setValue("");

		Settings.CsvServletPath.setValue(csvUrl);
		Settings.CsvAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.CsvResourceProvider.setValue(DefaultResourceProvider.class.getName());

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		do {
			try { Thread.sleep(100); } catch (Throwable t) {}

		} while (!services.isInitialized());

		securityContext		= SecurityContext.getSuperUserInstance();
		app			= StructrApp.getInstance(securityContext);

		// sleep again to wait for schema initialization
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// configure RestAssured
		RestAssured.basePath = "/";
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;
	}
}
