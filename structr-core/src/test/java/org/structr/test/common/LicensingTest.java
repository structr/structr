/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.test.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 *
 */
public class LicensingTest {

	private static final Logger logger = LoggerFactory.getLogger(LicensingTest.class.getName());

	protected static SecurityContext securityContext = null;
	protected static String basePath                 = null;
	protected static App app                         = null;
	protected static String randomTenantId           = RandomStringUtils.randomAlphabetic(10).toUpperCase();

	@BeforeMethod
	protected void starting(final Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Starting " + getClass().getName() + "#" + method.getName());
		System.out.println("######################################################################################");
	}

	@AfterMethod
	protected void finished(final Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Finished " + getClass().getName() + "#" + method.getName());
		System.out.println("######################################################################################");
	}

	@AfterMethod
	public void cleanDatabaseAndSchema() {

		try (final Tx tx = app.tx()) {

			// delete everything
			Services.getInstance().getService(NodeService.class).getDatabaseService().cleanDatabase();

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

	@BeforeClass
	public static void startSystem() {

		Services.disableTestingMode();

		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService LogService SchemaService");
		Settings.DatabaseDriverMode.setValue("remote");
		Settings.ConnectionUser.setValue("neo4j");
		Settings.ConnectionPassword.setValue("admin");
		Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());
		Settings.TenantIdentifier.setValue(randomTenantId);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.RelationshipCacheSize.setValue(1000);
		Settings.NodeCacheSize.setValue(1000);

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		do {
			try {
				Thread.sleep(100);
			} catch (Throwable t) {
			}

		} while (!services.isInitialized());

		securityContext = SecurityContext.getSuperUserInstance();
		app = StructrApp.getInstance(securityContext);
	}

	@AfterClass
	public static void stopSystem() {

		Services.getInstance().shutdown();

		try {
			File testConf = new File("structr.conf");
			if (testConf.isFile()) {
				testConf.delete();
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}


		try {
			File testDir = new File(basePath);
			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);

			} else {

				testDir.delete();
			}

		} catch (Throwable t) {
			logger.warn("", t);
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

	protected <T extends AbstractNode> List<T> createTestNodes(final Class<T> type, final int number, final long delay) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			List<T> nodes = new LinkedList<>();

			for (int i = 0; i < number; i++) {

				nodes.add(app.create(type));

				try {
					Thread.sleep(delay);
				} catch (InterruptedException ex) {
				}
			}

			tx.success();

			return nodes;

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return null;
	}

	protected <T extends AbstractNode> List<T> createTestNodes(final Class<T> type, final int number) throws FrameworkException {

		return createTestNodes(type, number, 0);

	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type) throws FrameworkException {
		return (T) createTestNode(type, new PropertyMap());
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final String name) throws FrameworkException {

		final PropertyMap map = new PropertyMap();

		map.put(AbstractNode.name, name);

		return (T) createTestNode(type, map);
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final PropertyMap props) throws FrameworkException {

		props.put(AbstractNode.type, type.getSimpleName());

		try (final Tx tx = app.tx()) {

			final T newNode = app.create(type, props);

			tx.success();

			return newNode;
		}

	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final NodeAttribute... attributes) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final T newNode = app.create(type, attributes);

			tx.success();

			return newNode;
		}

	}

	protected <T extends Relation> List<T> createTestRelationships(final Class<T> relType, final int number) throws FrameworkException {

		List<GenericNode> nodes = createTestNodes(GenericNode.class, 2);
		final NodeInterface startNode = nodes.get(0);
		final NodeInterface endNode = nodes.get(1);

		try (final Tx tx = app.tx()) {

			List<T> rels = new LinkedList<>();

			for (int i = 0; i < number; i++) {

				rels.add((T) app.create(startNode, endNode, relType));
			}

			tx.success();

			return rels;
		}

	}

	protected <T extends Relation> T createTestRelationship(final AbstractNode startNode, final AbstractNode endNode, final Class<T> relType) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final T rel = (T) app.create(startNode, endNode, relType);

			tx.success();

			return rel;
		}
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final Principal owner) throws FrameworkException {
		return (T)createTestNode(type, new PropertyMap(), owner);
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final PropertyMap props, final Principal owner) throws FrameworkException {

		final App backendApp = StructrApp.getInstance(SecurityContext.getInstance(owner, AccessMode.Backend));

		try (final Tx tx = backendApp.tx()) {

			final T result = backendApp.create(type, props);
			tx.success();

			return result;
		}
	}

	protected void assertNodeExists(final String nodeId) throws FrameworkException {
		assertNotNull(app.getNodeById(nodeId));

	}

	protected void assertNodeNotFound(final String nodeId) throws FrameworkException {
		assertNull(app.getNodeById(nodeId));
	}

	protected <T> List<T> toList(T... elements) {
		return Arrays.asList(elements);
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

	/**
	 * Get classes in given package and subpackages, accessible from the
	 * context class loader
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	protected static List<Class> getClasses(String packageName) throws ClassNotFoundException, IOException {

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
}
