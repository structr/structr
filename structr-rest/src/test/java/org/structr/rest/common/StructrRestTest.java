/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.rest.common;


import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.DefaultResourceProvider;
import org.structr.rest.entity.TestOne;

//~--- classes ----------------------------------------------------------------

/**
 * Base class for all structr tests
 *
 * All tests are executed in superuser context
 *
 *
 */
public class StructrRestTest {

	private static final Logger logger = LoggerFactory.getLogger(StructrRestTest.class.getName());
	protected static final Map<String, Object> staticConfig = new HashMap<>();

	//~--- fields ---------------------------------------------------------

	protected static SecurityContext securityContext = null;
	protected static App app                         = null;
	protected static String basePath                 = null;

	protected static final String contextPath = "/";
	protected static final String restUrl = "/structr/rest";
	protected static final String host = "127.0.0.1";
	protected static final int httpPort = (System.getProperty("httpPort") != null ? Integer.parseInt(System.getProperty("httpPort")) : 8875);

	static {

		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = "http://" + host + ":" + httpPort;
		RestAssured.port = httpPort;
	}

	@Rule
	public TestRule watcher = new TestWatcher() {

		@Override
		protected void starting(Description description) {

			System.out.println("######################################################################################");
			System.out.println("# Starting " + description.getClassName() + "#" + description.getMethodName());
			System.out.println("######################################################################################");
		}

		@Override
		protected void finished(Description description) {

			System.out.println("######################################################################################");
			System.out.println("# Finished " + description.getClassName() + "#" + description.getMethodName());
			System.out.println("######################################################################################");
		}
	};

	@AfterClass
	public static void stop() throws Exception {

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

			} catch(Throwable t) {

				logger.warn("", t);
			}

			try { Thread.sleep(100); } catch(Throwable t) {}
		}
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
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

	protected <T extends Relation> List<T> createTestRelationships(final Class<T> relType, final int number) throws FrameworkException {

		final App app             = StructrApp.getInstance(securityContext);
		final List<TestOne> nodes = createTestNodes(TestOne.class, 2);
		final TestOne startNode   = nodes.get(0);
		final TestOne endNode     = nodes.get(1);
		final List<T> rels        = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < number; i++) {

				rels.add((T)app.create(startNode, endNode, relType));
			}

			tx.success();
		}

		return rels;
	}

	protected String concat(String... parts) {

		StringBuilder buf = new StringBuilder();

		for (String part : parts) {
			buf.append(part);
		}

		return buf.toString();
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
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}

	protected String createEntityAsSuperUser(String resource, String... body) {

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
			.header("X-User", Settings.SuperUserName.getValue())
			.header("X-Password", Settings.SuperUserPassword.getValue())
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Get classes in given package and subpackages, accessible from the context class loader
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected static List<Class> getClasses(String packageName) throws ClassNotFoundException, IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		assert classLoader != null;

		String path                = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs            = new ArrayList<>();

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

	@After
	public void cleanDatabase() {

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery().getAsList()) {
				app.delete(node);
			}

			// delete remaining nodes without UUIDs etc.
			app.cypher("MATCH (n)-[r]-(m) DELETE n, r, m", Collections.emptyMap());

			tx.success();

		} catch (FrameworkException fex) {

			 logger.error("Exception while trying to clean database: {}", fex);
		}
	}

	@BeforeClass
	public static void start() {
		start(null);
	}

	public static void start(final Map<String, Object> additionalConfig) {

		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		// enable "just testing" flag to avoid JAR resource scanning
		Settings.Testing.setValue(true);
		Settings.Services.setValue("NodeService LogService HttpService SchemaService");
		Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");
		Settings.LogDatabasePath.setValue(basePath + "/logDb.dat");

		Settings.RelationshipCacheSize.setValue(1000);
		Settings.NodeCacheSize.setValue(1000);

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

		securityContext		= SecurityContext.getSuperUserInstance();
		app			= StructrApp.getInstance(securityContext);

		// wait for service layer to be initialized
		do {
			try { Thread.sleep(100); } catch (Throwable t) {}

		} while (!services.isInitialized());
	}


	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected static Matcher isEntity(Class<? extends AbstractNode> type) {
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
}
