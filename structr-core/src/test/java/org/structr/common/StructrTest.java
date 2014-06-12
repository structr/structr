/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.log.ReadLogCommand;
import org.structr.core.log.WriteLogCommand;
import org.structr.core.property.PropertyMap;
import org.structr.module.JarConfigurationProvider;

//~--- classes ----------------------------------------------------------------
/**
 * Base class for all structr tests
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class StructrTest extends TestCase {

	private static final Logger logger = Logger.getLogger(StructrTest.class.getName());

	//~--- fields ---------------------------------------------------------
	protected GraphDatabaseCommand graphDbCommand = null;
	protected SecurityContext securityContext = null;
	protected ReadLogCommand readLogCommand = null;
	protected WriteLogCommand writeLogCommand = null;
	protected String basePath = null;
	protected App app = null;
	
	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}
	};
	
	//~--- methods --------------------------------------------------------

	public void test00DbAvailable() {

		GraphDatabaseService graphDb = graphDbCommand.execute();

		assertTrue(graphDb != null);
	}

	@Override
	protected void tearDown() throws Exception {

		Services.getInstance().shutdown();

		try {
			File testDir = new File(basePath);
			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);

			} else {

				testDir.delete();
			}

		} catch (Throwable t) {
		}

		super.tearDown();

		System.out.println("######################################################################################");
		System.out.println("# " + getClass().getSimpleName() + "#" + getName() + " finished.");
		System.out.println("######################################################################################\n");

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

			t.printStackTrace();
		}

		return null;
	}

	protected <T extends AbstractNode> List<T> createTestNodes(final Class<T> type, final int number) throws FrameworkException {

		return createTestNodes(type, number, 0);

	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type) throws FrameworkException {
		return (T) createTestNode(type, new PropertyMap());
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final PropertyMap props) throws FrameworkException {

		props.put(AbstractNode.type, type.getSimpleName());

		try (final Tx tx = app.tx()) {

			final T newNode = app.create(type, props);

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

	protected void assertNodeExists(final String nodeId) throws FrameworkException {
		assertNotNull(app.get(nodeId));

	}

	protected void assertNodeNotFound(final String nodeId) throws FrameworkException {
		assertNull(app.get(nodeId));
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

	//~--- get methods ----------------------------------------------------
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

	@Override
	protected void setUp() throws Exception {

		System.out.println("\n######################################################################################");
		System.out.println("# Starting " + getClass().getSimpleName() + "#" + getName());
		System.out.println("######################################################################################");

		final StructrConf config = Services.getBaseConfiguration();
		final Date now = new Date();
		final long timestamp = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		config.setProperty(Services.CONFIGURED_SERVICES, "NodeService LogService");
		config.setProperty(Services.CONFIGURATION, JarConfigurationProvider.class.getName());
		config.setProperty(Services.TMP_PATH, "/tmp/");
		config.setProperty(Services.BASE_PATH, basePath);
		config.setProperty(Services.DATABASE_PATH, basePath + "/db");
		config.setProperty(Services.FILES_PATH, basePath + "/files");
		config.setProperty(Services.LOG_DATABASE_PATH, basePath + "/logDb.dat");
		config.setProperty(Services.TCP_PORT, "13465");
		config.setProperty(Services.UDP_PORT, "13466");
		config.setProperty(Services.SUPERUSER_USERNAME, "superadmin");
		config.setProperty(Services.SUPERUSER_PASSWORD, "sehrgeheim");

		final Services services = Services.getInstance(config);

		securityContext = SecurityContext.getSuperUserInstance();
		app = StructrApp.getInstance(securityContext);

		graphDbCommand = app.command(GraphDatabaseCommand.class);
		writeLogCommand = app.command(WriteLogCommand.class);
		readLogCommand = app.command(ReadLogCommand.class);

		// wait for service layer to be initialized
		do {
			try {
				Thread.sleep(100);
			} catch (Throwable t) {
			}

		} while (!services.isInitialized());
	}
}
