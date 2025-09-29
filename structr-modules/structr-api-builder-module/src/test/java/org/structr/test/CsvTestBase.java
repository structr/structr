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
package org.structr.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.schema.action.EvaluationHints;
import org.structr.test.helper.ConcurrentPortNumberHelper;
import org.structr.test.web.entity.traits.definitions.*;
import org.structr.test.web.entity.traits.definitions.relationships.FourThreeOneToOne;
import org.structr.test.web.entity.traits.definitions.relationships.TwoFiveOneToMany;
import org.testng.annotations.Optional;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for CSV tests.
 */
public abstract class CsvTestBase {

	protected final Logger logger             = LoggerFactory.getLogger(CsvTestBase.class.getName());
	protected String randomTenantId           = getRandomTenantIdentifier();
	protected SecurityContext securityContext = null;
	protected String basePath                 = null;
	protected App app                         = null;

	protected final String contextPath = "/";
	protected final String host        = "127.0.0.1";
	protected final int httpPort       = ConcurrentPortNumberHelper.getNextPortNumber(getClass());
	protected final int ftpPort        = ConcurrentPortNumberHelper.getNextPortNumber(getClass());
	protected final String restUrl     = "/structr/rest";
	protected final String htmlUrl     = "/structr/html";
	protected final String wsUrl       = "/structr/ws";
	protected String baseUri           = null;
	protected boolean first            = true;

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	public void setup(@Optional String testDatabaseConnection) {

		final long timestamp = System.currentTimeMillis();

		basePath = "/tmp/structr-test-" + timestamp + System.nanoTime();

		setupDatabaseConnection(testDatabaseConnection);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet WebSocketServlet HtmlServlet GraphQLServlet UploadServlet CsvServlet OpenAPIServlet");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		while (!services.isInitialized()) {
			try { Thread.sleep(100); } catch (Throwable t) {}
		}

		securityContext = SecurityContext.getSuperUserInstance();

		app = StructrApp.getInstance(securityContext);

		baseUri = "http://" + host + ":" + httpPort + htmlUrl + "/";

		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;
	}

	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		StructrTraits.registerTrait(new FourThreeOneToOne());
		StructrTraits.registerTrait(new TwoFiveOneToMany());

		StructrTraits.registerRelationshipType("FourThreeOneToOne", "FourThreeOneToOne");
		StructrTraits.registerRelationshipType("TwoFiveOneToMany",  "TwoFiveOneToMany");

		StructrTraits.registerTrait(new TestOneTraitDefinition());
		StructrTraits.registerTrait(new TestTwoTraitDefinition());
		StructrTraits.registerTrait(new TestThreeTraitDefinition());
		StructrTraits.registerTrait(new TestFourTraitDefinition());
		StructrTraits.registerTrait(new TestFiveTraitDefinition());

		StructrTraits.registerNodeType("TestOne",      "TestOne");
		StructrTraits.registerNodeType("TestTwo",      "TestTwo");
		StructrTraits.registerNodeType("TestThree",    "TestThree");
		StructrTraits.registerNodeType("TestFour",     "TestFour");
		StructrTraits.registerNodeType("TestFive",     "TestFive");

		// create new schema instance that includes the modified root schema
		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
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

	@BeforeMethod
	public void cleanDatabase() {

		if (!first) {

			try (final Tx tx = app.tx()) {

				// delete everything
				Services.getInstance().getDatabaseService().cleanDatabase();

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

	@AfterClass(alwaysRun = true)
	public void stop() throws Exception {

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

			} catch (Throwable t) {
			}

			try {
				Thread.sleep(500);
			} catch (Throwable t) {
			}
		}
	}

	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirs.
	 *
	 * @param directory The base directory
	 * @param packageName The package name for classes found inside the base
	 * directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {

		List<Class> classes = new ArrayList<>();

		if (!directory.exists()) {

			return classes;
		}

		File[] files = directory.listFiles();

		for (File file : files) {

			if (file.isDirectory()) {

				assert !file.getName().contains(".");

				classes.addAll(findClasses(file, packageName + "." + file.getName()));

			} else if (file.getName().endsWith(".class")) {

				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
			}

		}

		return classes;

	}

	protected NodeInterface createTestNode(final String type, final NodeAttribute... attrs) throws FrameworkException {

		final PropertyMap props = new PropertyMap();

		for (final NodeAttribute attr : attrs) {
			props.put(attr.getKey(), attr.getValue());
		}

		return app.create(type, props);
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number) throws FrameworkException {

		final List<NodeInterface> nodes = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			nodes.add(app.create(type, type + i));
		}

		return nodes;
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number, final PropertyMap props) throws FrameworkException {

		final List<NodeInterface> nodes = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			nodes.add(app.create(type, props));
		}

		return nodes;
	}

	public static final String ADMIN_USERNAME    = "admin";
	public static final String ADMIN_PASSWORD    = "admin";
	public static final String X_USER_HEADER     = "X-User";
	public static final String X_PASSWORD_HEADER = "X-Password";

	protected NodeInterface createAdminUser() {

		final PropertyMap properties = new PropertyMap();

		properties.put(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), ADMIN_USERNAME);
		properties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), ADMIN_PASSWORD);
		properties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true);

		NodeInterface user = null;

		try (final Tx tx = app.tx()) {

			user = app.create(StructrTraits.USER, properties);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return user;
	}

	protected List<RelationshipInterface> createTestRelationships(final String relType, final int number) throws FrameworkException {

		List<NodeInterface> nodes = createTestNodes("AbstractNode", 2);
		final NodeInterface startNode = nodes.get(0);
		final NodeInterface endNode   = nodes.get(1);

		List<RelationshipInterface> rels = new LinkedList<>();

		for (int i = 0; i < number; i++) {
			rels.add(app.create(startNode, endNode, relType));
		}

		return rels;
	}

	/**
	 * Get classes in given package and subpackages, accessible from the
	 * context class loader
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected static List<Class> getClasses(final String packageName) throws ClassNotFoundException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assert classLoader != null;

		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<>();

		while (resources.hasMoreElements()) {

			URL resource = resources.nextElement();

			dirs.add(new File(resource.getFile()));

		}

		List<Class> classList = new ArrayList<>();

		for (File directory : dirs) {

			classList.addAll(findClasses(directory, packageName));
		}

		return classList;

	}

	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected void grant(final String signature, final long flags, final boolean reset) {

		if (reset) {

			// delete existing permissions
			RestAssured

				.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, "superadmin")
				.header(X_PASSWORD_HEADER, "sehrgeheim")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))

				.expect()
				.statusCode(200)

				.when()
				.delete("/ResourceAccess");
		}

		// create new grant
		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, "superadmin")
			.header(X_PASSWORD_HEADER, "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.body(" { 'signature' : '" + signature + "', 'flags': " + flags + ", 'visibleToPublicUsers': true } ")

			.expect()
			.statusCode(201)

			.when()
			.post("/ResourceAccess");
	}

	protected void testGet(final String resource, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.get(resource);

	}

	protected void testGet(final String resource, final String username, final String password, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, username)
			.header(X_PASSWORD_HEADER, password)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.get(resource);

	}

	protected void testPost(final String resource, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.body(body)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.post(resource);
	}

	protected void testPost(final String resource, final String username, final String password, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, username)
			.header(X_PASSWORD_HEADER, password)
			.body(body)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.post(resource);
	}

	protected void testPut(final String resource, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.body(body)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.put(resource);
	}

	protected void testPut(final String resource, final String username, final String password, final String body, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, username)
			.header(X_PASSWORD_HEADER, password)
			.body(body)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.put(resource);
	}

	protected void testDelete(final String resource, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.delete(resource);
	}

	protected void testDelete(final String resource, final String username, final String password, final int expectedStatusCode) {

		RestAssured

			.given()
			.contentType("application/json; charset=UTF-8")
			.header(X_USER_HEADER, username)
			.header(X_PASSWORD_HEADER, password)

			.expect()
			.statusCode(expectedStatusCode)

			.when()
			.delete(resource);
	}

	protected void makePublic(final Object... objects) throws FrameworkException {

		for (final Object obj : objects) {

			if (obj instanceof NodeInterface n) {

				n.setVisibility(true, false);
			}
		}

	}

	protected <T> List<T> toList(final T... elements) {
		return Arrays.asList(elements);
	}

	protected Map<String, byte[]> toMap(final Pair... pairs) {

		final Map<String, byte[]> map = new LinkedHashMap<>();

		for (final Pair pair : pairs) {
			map.put(pair.key, pair.value);
		}

		return map;
	}

	public static class Pair {

		public String key   = null;
		public byte[] value = null;

		public Pair(final String key, final byte[] value) {

			this.key = key;
			this.value = value;
		}
	}

	protected String createEntity(String resource, String... body) {

		final StringBuilder buf = new StringBuilder();

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

	protected String createEntityAsUser(final String name, final String password, final String resource, final String... body) {

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
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.header(X_USER_HEADER, name)
				.header(X_PASSWORD_HEADER, password)

				.body(buf.toString())
				.expect().statusCode(201)
				.when().post(resource).getHeader("Location"));
	}

	protected String createEntityAsSuperUser(String resource, String... body) {

		final StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		RestAssured.basePath = "/structr/rest";

		return getUuidFromLocation(
			RestAssured
				.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.header(X_USER_HEADER, Settings.SuperUserName.getValue())
				.header(X_PASSWORD_HEADER, Settings.SuperUserPassword.getValue())

				.body(buf.toString())
				.expect().statusCode(201)
				.when().post(resource).getHeader("Location"));
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

	protected void tryWithTimeout(final Supplier<Boolean> workload, final Runnable onTimeout, final int timeoutInMS) {

		if (workload != null && timeoutInMS >= 0) {
			final long startTime = System.currentTimeMillis();

			do {
				if (workload.get()) {
					return;
				}
			} while ((startTime + timeoutInMS) >= System.currentTimeMillis());
		}

		if (onTimeout != null) {
			onTimeout.run();
		}
	}

	protected void tryWithTimeout(final Supplier<Boolean> workload, final Runnable onTimeout, final int timeoutInMS, final int retryDelayInMS) {

		final long startTime = System.currentTimeMillis();

		if (workload != null && onTimeout != null && timeoutInMS >= 0 && retryDelayInMS > 0) {
			do {
				if (workload.get()) {
					return;
				}

				try {

					Thread.sleep(retryDelayInMS);
				} catch (InterruptedException ex) {

					return;
				}
			} while ((startTime + timeoutInMS) >= System.currentTimeMillis());

			onTimeout.run();
		}
	}

	protected Object invokeMethod(final SecurityContext securityContext, final NodeInterface node, final String methodName, final Map<String, Object> parameters, final boolean throwIfNotExists, final EvaluationHints hints) throws FrameworkException {

		final AbstractMethod method = Methods.resolveMethod(node.getTraits(), methodName);
		if (method != null) {

			hints.reportExistingKey(methodName);

			method.execute(securityContext, node, NamedArguments.fromMap(parameters), new EvaluationHints());
		}

		if (throwIfNotExists) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + node.getType());
		}

		return null;
	}
}
