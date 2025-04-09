/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.csv.CSVModule;
import org.structr.excel.ExcelModule;
import org.structr.feed.DataFeedsModule;
import org.structr.files.FileAccessModule;
import org.structr.geo.GeoTransformationsModule;
import org.structr.ldap.LDAPModule;
import org.structr.mail.AdvancedMailModule;
import org.structr.media.MediaModule;
import org.structr.messaging.engine.MessageEngineModule;
import org.structr.odf.ODFModule;
import org.structr.payment.PaymentsModule;
import org.structr.pdf.PDFModule;
import org.structr.schema.SchemaService;
import org.structr.schema.action.EvaluationHints;
import org.structr.text.TextSearchModule;
import org.structr.transform.APIBuilderModule;
import org.structr.translation.TranslationModule;
import org.structr.xmpp.XMPPModule;
import org.testng.annotations.Optional;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for all structr UI tests.
 */
public abstract class FullStructrTest {

	protected final Logger logger             = LoggerFactory.getLogger(FullStructrTest.class.getName());
	protected String randomTenantId           = getRandomTenantIdentifier();
	protected SecurityContext securityContext = null;
	protected String basePath                 = null;
	protected App app                         = null;

	protected final String contextPath = "/";
	protected final String host        = "127.0.0.1";
	protected final int httpPort       = getNextPortNumber();
	protected final int ftpPort        = getNextPortNumber();
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

		Settings.Servlets.setValue("JsonRestServlet WebSocketServlet HtmlServlet GraphQLServlet UploadServlet OpenAPIServlet");

		// allow use of EncryptedStringProperty
		Settings.GlobalSecret.setValue("test_secret");

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

		// instantiate all modules so we have access to the types defined in there
		new APIBuilderModule();
		new AdvancedMailModule();
		new CSVModule();
		new DataFeedsModule();
		new ExcelModule();
		new FileAccessModule();
		new GeoTransformationsModule();
		new LDAPModule();
		new MediaModule();
		new MessageEngineModule();
		new ODFModule();
		new PaymentsModule();
		new PDFModule();
		new TextSearchModule();
		new TranslationModule();
		new XMPPModule();
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
					.header("X-User", "superadmin")
					.header("X-Password", "sehrgeheim")
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
				.header("X-User", "superadmin")
				.header("X-Password", "sehrgeheim")
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
				.header("X-User", username)
				.header("X-Password", password)

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
				.header("X-User", username)
				.header("X-Password", password)
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
				.header("X-User", username)
				.header("X-Password", password)
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
				.header("X-User", username)
				.header("X-Password", password)

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
				.header("X-User", name)
				.header("X-Password", password)

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
				.header("X-User", Settings.SuperUserName.getValue())
				.header("X-Password", Settings.SuperUserPassword.getValue())

			.body(buf.toString())
				.expect().statusCode(201)
			.when().post(resource).getHeader("Location"));
	}

	protected int getNextPortNumber() {

		// allow override via system property (-DhttpPort=...)
		if (System.getProperty("httpPort") != null) {

			return Integer.parseInt(System.getProperty("httpPort"));
		};

		// use locked file to store last used port
		final String fileName = "/tmp/structr.test.port.lock";
		final int max         = 65500;
		final int min         = 8875;
		int port              = min;


		try (final RandomAccessFile raf = new RandomAccessFile(fileName, "rws")) {

			raf.getChannel().lock();

			if (raf.length() > 0) {

				port = raf.readInt();
			}

			port++;

			if (port > max) {
				port = min;
			}

			raf.setLength(0);
			raf.writeInt(port);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return port;
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

			method.execute(securityContext, node, Arguments.fromMap(parameters), new EvaluationHints());
		}

		if (throwIfNotExists) {
			throw new FrameworkException(400, "Method " + methodName + " not found in type " + node.getType());
		}

		return null;
	}

	protected SchemaNode getSchemaNode(final String name) throws FrameworkException {
		return app.nodeQuery(StructrTraits.SCHEMA_NODE).name(name).getFirst().as(SchemaNode.class);
	}

	protected Set<String> getSchemaNodeTraits(final String name) throws FrameworkException {
		return getSchemaNode(name).getInheritedTraits();
	}

	protected String createNodeWithCypher(final Set<String> labels, final Map<String, Object> nodeData) throws FrameworkException {

		final Map<String, Object> data = new LinkedHashMap<>();
		final String uuid              = NodeServiceCommand.getNextUuid();
		final String labelsString      = org.apache.commons.lang.StringUtils.join(labels, ":");

		data.put("id", uuid);
		data.putAll(nodeData);

		// create database contents manually
		app.query("CREATE (n:" + randomTenantId + ":PropertyContainer:GraphObject:NodeInterface:AccessControllable:" + labelsString + " $data)",
			Map.of("data", data)
		);

		return uuid;
	}

	protected String createRelationshipWithCypher(final String id1, final String id2, final String type, final String relType, final Map<String, Object> relData) throws FrameworkException {

		final Map<String, Object> data = new LinkedHashMap<>();
		final String uuid              = NodeServiceCommand.getNextUuid();

		data.put("id", uuid);
		data.putAll(relData);

		// create database contents manually
		app.query("MATCH (n:NodeInterface:" + randomTenantId + " { id: $id1 }), (m:NodeInterface:" + randomTenantId + " { id: $id2 }) MERGE (n)-[r:" + relType + " { id: $uuid, type: $type }]->(m) RETURN r",
			Map.of(
				"relData", data,
				"id1", id1,
				"id2", id2,
				"uuid", uuid,
				"type", type
			)
		);

		return uuid;
	}
}
