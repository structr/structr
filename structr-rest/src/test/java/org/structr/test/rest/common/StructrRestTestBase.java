/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.SuperUserAuthenticator;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsManager;
import org.structr.schema.SchemaService;
import org.structr.schema.export.StructrSchema;
import org.structr.test.helper.ConcurrentPortNumberHelper;
import org.structr.test.rest.traits.definitions.*;
import org.structr.test.rest.traits.definitions.relationships.*;
import org.testng.annotations.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

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
	protected final int httpPort       = ConcurrentPortNumberHelper.getNextPortNumber(getClass());

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
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

		Settings.Servlets.setValue("JsonRestServlet OpenAPIServlet");
		Settings.RestAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.RestServletPath.setValue(restUrl);
		Settings.RestUserClass.setValue("");
		Settings.OpenAPIAuthenticator.setValue(SuperUserAuthenticator.class.getName());

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		while (!services.isInitialized()) {
			try { Thread.sleep(100); } catch (Throwable t) {}
		}

		securityContext = SecurityContext.getSuperUserInstance();
		app             = StructrApp.getInstance(securityContext);

		// configure RestAssured
		RestAssured.basePath = "/structr/rest";
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;
	}

	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		StructrTraits.registerRelationshipType("ElevenTwoOneToMany",  new ElevenTwoOneToMany());
		StructrTraits.registerRelationshipType("FiveOneManyToMany",   new FiveOneManyToMany());
		StructrTraits.registerRelationshipType("FiveOneManyToOne",    new FiveOneManyToOne());
		StructrTraits.registerRelationshipType("FiveOneOneToMany",    new FiveOneOneToMany());
		StructrTraits.registerRelationshipType("FiveThreeOneToOne",   new FiveThreeOneToOne());
		StructrTraits.registerRelationshipType("FourOneManyToMany",   new FourOneManyToMany());
		StructrTraits.registerRelationshipType("FourOneOneToMany",    new FourOneOneToMany());
		StructrTraits.registerRelationshipType("FourThreeOneToOne",   new FourThreeOneToOne());
		StructrTraits.registerRelationshipType("NineEightManyToMany", new NineEightManyToMany());
		StructrTraits.registerRelationshipType("SevenSixOneToMany",   new SevenSixOneToMany());
		StructrTraits.registerRelationshipType("SixEightManyToMany",  new SixEightManyToMany());
		StructrTraits.registerRelationshipType("TenSevenOneToOne",    new TenSevenOneToOne());
		StructrTraits.registerRelationshipType("ThreeFiveOneToMany",  new ThreeFiveOneToMany());
		StructrTraits.registerRelationshipType("ThreeFourOneToMany",  new ThreeFourOneToMany());
		StructrTraits.registerRelationshipType("TwoOneOneToMany",     new TwoOneOneToMany());

		StructrTraits.registerNodeType("TestOne",      new TestOneTraitDefinition());
		StructrTraits.registerNodeType("TestTwo",      new TestTwoTraitDefinition());
		StructrTraits.registerNodeType("TestThree",    new TestThreeTraitDefinition());
		StructrTraits.registerNodeType("TestFour",     new TestFourTraitDefinition());
		StructrTraits.registerNodeType("TestFive",     new TestFiveTraitDefinition());
		StructrTraits.registerNodeType("TestSix",      new TestSixTraitDefinition());
		StructrTraits.registerNodeType("TestSeven",    new TestSevenTraitDefinition());
		StructrTraits.registerNodeType("TestEight",    new TestEightTraitDefinition());
		StructrTraits.registerNodeType("TestNine",     new TestNineTraitDefinition());
		StructrTraits.registerNodeType("TestTen",      new TestTenTraitDefinition());
		StructrTraits.registerNodeType("TestEleven",   new TestElevenTraitDefinition());
		StructrTraits.registerNodeType("TestObject",   new TestObjectTraitDefinition());

		// create new schema instance that includes the modified root schema
		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
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

			SchemaService.reloadSchema(new ErrorBuffer(), null, false, false);
			FlushCachesCommand.flushAll();
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

	protected String getRandomTenantIdentifier() {
		return RandomStringUtils.randomAlphabetic(10).toUpperCase();
	}

	protected void setupDatabaseConnection(String testDatabaseConnection) {

		// use database driver from system property, default to MemoryDatabaseService
		Settings.DatabaseDriver.setValue(System.getProperty("testDatabaseDriver", Settings.DEFAULT_REMOTE_DATABASE_DRIVER));
		Settings.ConnectionUser.setValue("neo4j");
		Settings.ConnectionPassword.setValue("admin123");
		if (StringUtils.isBlank(testDatabaseConnection)) {
			Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());
		} else {
			Settings.ConnectionUrl.setValue(testDatabaseConnection);
		}
		Settings.ConnectionDatabaseName.setValue("neo4j");
		Settings.TenantIdentifier.setValue(randomTenantId);
	}

	// ----- non-static methods -----
	protected List<NodeInterface> createTestNodes(final String type, final int number) throws FrameworkException {

		final App app                   = StructrApp.getInstance(securityContext);
		final List<NodeInterface> nodes = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < number; i++) {

				final NodeInterface node = app.create(type);
				node.setName("test" + i);

				nodes.add(node);
			}

			tx.success();
		}

		return nodes;
	}

	protected String createEntity(final String resource, final String... body) {
		
		RestAssured.basePath = "/structr/rest";

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		return getUuidFromLocation(
			RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}

	protected Map<String, Object> generalPurposePostMethod(final String resource, final String... body) {

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		return RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(buf.toString())
			.when().post(resource).getBody().as(Map.class);
	}

	protected String concat(final String... parts) {

		StringBuilder buf = new StringBuilder();

		for (String part : parts) {
			buf.append(part);
		}

		return buf.toString();
	}

	protected String getUuidFromLocation(final String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected Matcher isEntity(final String type) {
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

	protected String createTestUserType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TestUser");

			type.addTrait(StructrTraits.USER);
			type.addMethod("onCreate", "set(this, 'name', concat('test', now));");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		return "TestUser";
	}

	// ----- static methods -----
	public static void assertMapPathValueIs(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					final List list = (List)current;
					if (index >= list.size()) {

						// value for nonexisting fields must be null
						assertEquals("Invalid map path result for " + mapPath, value, null);

						// nothing more to check here
						return;

					} else {

						current = list.get(index);
					}
				}

			} else if ("#".equals(part)) {

				if (current instanceof List) {

					assertEquals("Invalid collection size for " + mapPath, value, ((List)current).size());

					// nothing more to check here
					return;
				}

				if (current instanceof Map) {

					assertEquals("Invalid map size for " + mapPath, value, ((Map)current).size());

					// nothing more to check here
					return;
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		// ignore type of value if numerical (GSON defaults to double...)
		if (value instanceof Number && current instanceof Number) {

			assertEquals("Invalid map path result for " + mapPath, ((Number)value).doubleValue(), ((Number)current).doubleValue(), 0.0);

		} else {

			assertEquals("Invalid map path result for " + mapPath, value, current);
		}
	}
}
