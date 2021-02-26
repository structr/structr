/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.test.rest.common;


import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.SuperUserAuthenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.DefaultResourceProvider;
import org.structr.schema.SchemaService;
import org.structr.schema.export.StructrSchema;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for all structr tests.
 * All tests are executed in superuser context.
 */
public abstract class StructrRestTestBase {

	protected final Logger logger             = LoggerFactory.getLogger(StructrRestTestBase.class.getName());
	protected String randomTenantId           = getRandomTenantIdentifier();
	protected SecurityContext securityContext = null;
	protected String basePath                 = null;
	protected App app                         = null;
	private boolean first                     = true;

	protected final String contextPath = "/";
	protected final String restUrl     = "/structr/rest";
	protected final String host        = "127.0.0.1";
	protected final int httpPort       = getNextPortNumber();

	@BeforeClass(alwaysRun = true)
	public void setup() {

		final long timestamp = System.nanoTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService SchemaService HttpService");
		setupDatabaseConnection();

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet");
		Settings.RestAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.RestResourceProvider.setValue(DefaultResourceProvider.class.getName());
		Settings.RestServletPath.setValue(restUrl);
		Settings.RestUserClass.setValue("");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		do {
			try { Thread.sleep(100); } catch (Throwable t) {}

		} while (!services.isInitialized());

		securityContext = SecurityContext.getSuperUserInstance();
		app             = StructrApp.getInstance(securityContext);

		// sleep again to wait for schema initialization
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// configure RestAssured
		RestAssured.basePath = "/structr/rest";
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;
	}

	@AfterClass(alwaysRun = true)
	public void teardown() {

		Services.getInstance().shutdown();

		File testDir = new File(basePath);
		int count = 0;

		// try up to 10 times to delete the directory
		while (testDir.exists() && count++ < 10) {

			try {

				if (testDir.isDirectory()) {

					FileUtils.deleteDirectory(testDir);

				} else {

					testDir.delete();
				}

			} catch(Throwable t) {}

			try { Thread.sleep(500); } catch(Throwable t) {}
		}
	}

	@BeforeMethod
	public void cleanDatabase() {

		if (!first) {

			try (final Tx tx = app.tx()) {

				// delete everything
				Services.getInstance().getDatabaseService().cleanDatabase();

				FlushCachesCommand.flushAll();

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}


			try {

				SchemaService.ensureBuiltinTypesExist(app);

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}
		}

		first = false;
	}

	@BeforeMethod
	public void starting(Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Starting " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
		System.out.println("######################################################################################");
	}

	@AfterMethod
	public void finished(Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Finished " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
		System.out.println("######################################################################################");
	}

	protected int getNextPortNumber() {

		// allow override via system property (-DhttpPort=...)
		if (System.getProperty("httpPort") != null) {

			return Integer.parseInt(System.getProperty("httpPort"));
		};

		// return
		return 8875 + new Random().nextInt(1000);
	}

	protected String getRandomTenantIdentifier() {
		return RandomStringUtils.randomAlphabetic(10).toUpperCase();
	}

	protected void setupDatabaseConnection() {

		// use database driver from system property, default to MemoryDatabaseService
		Settings.DatabaseDriver.setValue(System.getProperty("testDatabaseDriver", Settings.DEFAULT_DATABASE_DRIVER));
		Settings.ConnectionUser.setValue("neo4j");
		Settings.ConnectionPassword.setValue("admin");
		Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());
		Settings.TenantIdentifier.setValue(randomTenantId);
	}

	// ----- non-static methods -----
	protected <T extends NodeInterface> List<T> createTestNodes(final Class<T> type, final int number) throws FrameworkException {

		final App app       = StructrApp.getInstance(securityContext);
		final List<T> nodes = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < number; i++) {
				nodes.add(app.create(type));
			}

			tx.success();
		}

		return nodes;
	}

	protected String createEntity(String resource, String... body) {

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		return getUuidFromLocation(
			RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}

	protected String concat(String... parts) {

		StringBuilder buf = new StringBuilder();

		for (String part : parts) {
			buf.append(part);
		}

		return buf.toString();
	}

	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected Matcher isEntity(Class<? extends AbstractNode> type) {
		return new EntityMatcher(type);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1) {
		return toMap(key1, value1, null, null);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1, final String key2, final Object value2) {
		return toMap(key1, value1, key2, value2, null, null);
	}

	protected Map<String, Object> toMap(final String key1, final Object value1, final String key2, final Object value2, final String key3, final Object value3) {

		final Map<String, Object> map = new LinkedHashMap<>();

		if (key1 != null && value1 != null) {
			map.put(key1, value1);
		}

		if (key2 != null && value2 != null) {
			map.put(key2, value2);
		}

		if (key3 != null && value3 != null) {
			map.put(key3, value3);
		}

		return map;
	}

	protected Class createTestUserType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TestUser");

			type.setExtends(URI.create("#/definitions/Principal"));
			type.overrideMethod("onCreate", true, "set(this, 'name', concat('test', now));");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		return StructrApp.getConfiguration().getNodeEntityClass("TestUser");
	}
}
