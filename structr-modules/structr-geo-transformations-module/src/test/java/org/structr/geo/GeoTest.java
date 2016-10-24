/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.geo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Structr;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.module.JarConfigurationProvider;

/**
 *
 */
public class GeoTest {

	private static final Logger logger = LoggerFactory.getLogger(GeoTest.class.getName());

	protected static SecurityContext securityContext = null;
	protected static String basePath                 = null;
	protected static App app                         = null;

	@Test
	public void testLatLonToUTM() {

		final LatLonToUTMFunction func = new LatLonToUTMFunction();

		try {

			final Object result1 = func.apply(null, null, new Object[] { 53.85499997165232, 8.081674915658844 });
			Assert.assertEquals("Invalid UTM conversion result", "32U 439596 5967780" , result1);

			final Object result2 = func.apply(null, null, new Object[] { 51.319997116243364, 7.49998773689121 });
			Assert.assertEquals("Invalid UTM conversion result", "32U 395473 5686479", result2);

			final Object result3 = func.apply(null, null, new Object[] { -38.96442577579118, 7.793498600057568 });
			Assert.assertEquals("Invalid UTM conversion result", "32H 395473 5686479", result3);

			final Object result4 = func.apply(null, null, new Object[] { 51.319997116243364, -166.5000122631088});
			Assert.assertEquals("Invalid UTM conversion result", "3U 395473 5686479", result4);

			final Object result5 = func.apply(null, null, new Object[] { -36.59789213337618, -164.5312529421211 });
			Assert.assertEquals("Invalid UTM conversion result", "3H 541926 5949631", result5);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	@Test
	public void testGPXImport() {

		try {

			final ImportGPXFunction func = new ImportGPXFunction();
			final StringBuilder gpx      = new StringBuilder();

			gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			gpx.append("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"Wikipedia\"");
			gpx.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			gpx.append("    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
			gpx.append(" <!-- Kommentare sehen so aus -->");
			gpx.append(" <metadata>");
			gpx.append("  <name>Dateiname</name>");
			gpx.append("  <desc>Validiertes GPX-Beispiel ohne Sonderzeichen</desc>");
			gpx.append("  <author>");
			gpx.append("   <name>Autorenname</name>");
			gpx.append("  </author>");
			gpx.append(" </metadata>");
			gpx.append(" <wpt lat=\"52.518611\" lon=\"13.376111\">");
			gpx.append("  <ele>35.0</ele>");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Reichstag (Berlin)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <wpt lat=\"48.208031\" lon=\"16.358128\">");
			gpx.append("  <ele>179</ele>");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Parlament (Wien)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <wpt lat=\"46.9466\" lon=\"7.44412\">");
			gpx.append("  <time>2011-12-31T23:59:59Z</time>");
			gpx.append("  <name>Bundeshaus (Bern)</name>");
			gpx.append("  <sym>City</sym>");
			gpx.append(" </wpt>");
			gpx.append(" <rte>");
			gpx.append("  <name>Routenname</name>");
			gpx.append("  <desc>Routenbeschreibung</desc>");
			gpx.append("  <rtept lat=\"52.0\" lon=\"13.5\">");
			gpx.append("   <ele>33.0</ele>");
			gpx.append("   <time>2011-12-13T23:59:59Z</time>");
			gpx.append("   <name>rtept 1</name>");
			gpx.append("  </rtept>");
			gpx.append("  <rtept lat=\"49\" lon=\"12\">");
			gpx.append("   <name>rtept 2</name>");
			gpx.append("  </rtept>");
			gpx.append("  <rtept lat=\"47.0\" lon=\"7.5\">");
			gpx.append("  </rtept>");
			gpx.append(" </rte>");
			gpx.append(" <trk>");
			gpx.append("  <name>Trackname1</name>");
			gpx.append("  <desc>Trackbeschreibung</desc>");
			gpx.append("  <trkseg>");
			gpx.append("   <trkpt lat=\"52.520000\" lon=\"13.380000\">");
			gpx.append("    <ele>36.0</ele>");
			gpx.append("    <time>2011-01-13T01:01:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"48.200000\" lon=\"16.260000\">");
			gpx.append("    <ele>180</ele>");
			gpx.append("    <time>2011-01-14T01:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"46.95\" lon=\"7.4\">");
			gpx.append("    <ele>987.654</ele>");
			gpx.append("    <time>2011-01-15T23:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("  </trkseg>");
			gpx.append(" </trk>");
			gpx.append(" <trk>");
			gpx.append("  <name>Trackname2</name>");
			gpx.append("  <trkseg>");
			gpx.append("   <trkpt lat=\"47.2\" lon=\"7.41\">");
			gpx.append("    <time>2011-01-16T23:59:01Z</time>");
			gpx.append("   </trkpt>");
			gpx.append("   <trkpt lat=\"52.53\" lon=\"13.0\">");
			gpx.append("   </trkpt>");
			gpx.append("  </trkseg>");
			gpx.append(" </trk>");
			gpx.append("</gpx>");

			final GraphObjectMap obj = (GraphObjectMap)func.apply(null, null, new Object[] { gpx.toString() });

			System.out.println(obj);


		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testUTMLatLonRoundTrip() {

		final LatLonToUTMFunction latLonUtm = new LatLonToUTMFunction();
		final UTMToLatLonFunction utmLatLon = new UTMToLatLonFunction();
		final String sourceUTM              = "32U 439596 5967780";

		try {
			final GraphObjectMap result1 = (GraphObjectMap)utmLatLon.apply(null, null, new Object[] { sourceUTM });
			final String result2         = (String)latLonUtm.apply(null, null, new Object[] { result1.getProperty(UTMToLatLonFunction.latitudeProperty), result1.getProperty(UTMToLatLonFunction.longitudeProperty) } );

			Assert.assertEquals("Invalid UTM to lat/lon roundtrip result", sourceUTM, result2);

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testLatLonUTMRoundtrip() {

		final LatLonToUTMFunction latLonUtm = new LatLonToUTMFunction();
		final UTMToLatLonFunction utmLatLon = new UTMToLatLonFunction();
		final double latitude               = 51.319997116243364;
		final double longitude              = 7.49998773689121;

		try {
			final String result1         = (String)latLonUtm.apply(null, null, new Object[] { latitude, longitude } );
			final GraphObjectMap result2 = (GraphObjectMap)utmLatLon.apply(null, null, new Object[] { result1 } );

			Assert.assertEquals("Invalid UTM to lat/lon roundtrip result", (Double)latitude,  result2.getProperty(UTMToLatLonFunction.latitudeProperty));
			Assert.assertEquals("Invalid UTM to lat/lon roundtrip result", (Double)longitude, result2.getProperty(UTMToLatLonFunction.longitudeProperty));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testUTMToLatLon() {

		final UTMToLatLonFunction func = new UTMToLatLonFunction();

		try {

			final Object result6 = func.apply(null, null, new Object[] { "32 N 439596 5967780" });
			Assert.assertEquals("Invalid UTM conversion result", 53.85499997165232, get(result6, 0));
			Assert.assertEquals("Invalid UTM conversion result", 8.081674915658844, get(result6, 1));

			final Object result7 = func.apply(null, null, new Object[] { "32U 395473 5686479" });
			Assert.assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result7, 0));
			Assert.assertEquals("Invalid UTM conversion result", 7.49998773689121, get(result7, 1));

			final Object result8 = func.apply(null, null, new Object[] { "32 395473 5686479" });
			Assert.assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result8, 0));
			Assert.assertEquals("Invalid UTM conversion result", 7.49998773689121, get(result8, 1));

			final Object result9 = func.apply(null, null, new Object[] { "32H 395473 5686479" });
			Assert.assertEquals("Invalid UTM conversion result", -38.96442577579118, get(result9, 0));
			Assert.assertEquals("Invalid UTM conversion result", 7.793498600057568, get(result9, 1));

			final Object result10 = func.apply(null, null, new Object[] { "3U 395473 5686479" });
			Assert.assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result10, 0));
			Assert.assertEquals("Invalid UTM conversion result", -166.5000122631088, get(result10, 1));

			final Object result11 = func.apply(null, null, new Object[] { "3 395473 5686479" });
			Assert.assertEquals("Invalid UTM conversion result", 51.319997116243364, get(result11, 0));
			Assert.assertEquals("Invalid UTM conversion result", -166.5000122631088, get(result11, 1));

			final Object result12 = func.apply(null, null, new Object[] { "3H 541926 5949631" });
			Assert.assertEquals("Invalid UTM conversion result", -36.59789213337618, get(result12, 0));
			Assert.assertEquals("Invalid UTM conversion result", -164.5312529421211, get(result12, 1));



		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	private Object get(final Object map, final int index) {

		if (map instanceof GraphObjectMap) {

			switch (index) {

				case 0:
					return ((GraphObjectMap)map).getProperty(UTMToLatLonFunction.latitudeProperty);

				case 1:
					return ((GraphObjectMap)map).getProperty(UTMToLatLonFunction.longitudeProperty);
			}
		}

		return null;
	}

	@Rule
	public TestRule watcher = new TestWatcher() {

		@Override
		protected void starting(Description description) {

			System.out.println("######################################################################################");
			System.out.println("# Starting " + getClass().getSimpleName() + "#" + description.getMethodName());
			System.out.println("######################################################################################");
		}

		@Override
		protected void finished(Description description) {

			System.out.println("######################################################################################");
			System.out.println("# Finished " + getClass().getSimpleName() + "#" + description.getMethodName());
			System.out.println("######################################################################################");
		}
	};

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

	@After
	public void cleanDatabase() {

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery().getAsList()) {
				app.delete(node);
			}

			tx.success();

		} catch (FrameworkException fex) {

			 logger.error("Exception while trying to clean database: {}", fex);
		}
	}

	@BeforeClass
	public static void startSystem() {
		startSystem(Collections.emptyMap());
	}

	public static void startSystem(final Map<String, Object> additionalConfig) {

		final Properties config = Services.getBaseConfiguration();
		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		// enable "just testing" flag to avoid JAR resource scanning
		config.setProperty(Services.TESTING, "true");

		config.setProperty(Services.CONFIGURED_SERVICES, "NodeService LogService SchemaService");
		config.setProperty(Services.CONFIGURATION, JarConfigurationProvider.class.getName());
		config.setProperty(Structr.DATABASE_CONNECTION_URL, Structr.TEST_DATABASE_URL);
		config.setProperty(Services.TMP_PATH, "/tmp/");
		config.setProperty(Services.BASE_PATH, basePath);
		config.setProperty(Structr.DATABASE_PATH, basePath + "/db");
		config.setProperty(Structr.RELATIONSHIP_CACHE_SIZE, "1000");
		config.setProperty(Structr.NODE_CACHE_SIZE, "1000");
		config.setProperty(Services.FILES_PATH, basePath + "/files");
		config.setProperty(Services.LOG_DATABASE_PATH, basePath + "/logDb.dat");
		config.setProperty(Services.TCP_PORT, (System.getProperty("tcpPort") != null ? System.getProperty("tcpPort") : "13465"));
		config.setProperty(Services.UDP_PORT, (System.getProperty("udpPort") != null ? System.getProperty("udpPort") : "13466"));
		config.setProperty(Services.SUPERUSER_USERNAME, "superadmin");
		config.setProperty(Services.SUPERUSER_PASSWORD, "sehrgeheim");

		if (additionalConfig != null) {
			config.putAll(additionalConfig);
		}

		final Services services = Services.getInstanceForTesting(config);

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
}
