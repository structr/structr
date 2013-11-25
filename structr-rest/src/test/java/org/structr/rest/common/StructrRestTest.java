/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.rest.common;


import com.jayway.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;

import java.util.*;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.hamcrest.Matcher;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.module.ModuleService;
import org.structr.rest.entity.TestOne;
import org.structr.rest.service.RestService;
import org.structr.rest.servlet.JsonRestServlet;

//~--- classes ----------------------------------------------------------------

/**
 * Base class for all structr tests
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class StructrRestTest extends TestCase {

	private static final Logger logger = Logger.getLogger(StructrRestTest.class.getName());

	//~--- fields ---------------------------------------------------------

	protected SecurityContext securityContext = null;
	protected App app                         = null;
	protected String basePath                 = null;
	
	protected static final String contextPath = "/";
	protected static final String restUrl = "/structr/rest";
	protected static final String host = "127.0.0.1";
	protected static final int httpPort = 8875;
	
	static {

		// check character set
		checkCharset();
		
		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = "http://" + host + ":" + httpPort;
		RestAssured.port = httpPort;
	}
	

	public void test00DbAvailable() {

		GraphDatabaseService graphDb = app.getGraphDatabaseService();

		assertTrue(graphDb != null);
	}

	@Override
	protected void tearDown() throws Exception {

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

		super.tearDown();

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

		try {
			app.beginTx();
			for (int i = 0; i < number; i++) {
				nodes.add(app.create(type));
			}
			app.commitTx();

		} finally {

			app.finishTx();
		}
		
		return nodes;
	}

	protected <T extends Relation> List<T> createTestRelationships(final Class<T> relType, final int number) throws FrameworkException {

		final App app             = StructrApp.getInstance(securityContext);
		final List<TestOne> nodes = createTestNodes(TestOne.class, 2);
		final TestOne startNode   = nodes.get(0);
		final TestOne endNode     = nodes.get(1);
		final List<T> rels        = new LinkedList<>();

		try {
			app.beginTx();

			for (int i = 0; i < number; i++) {

				rels.add(app.create(startNode, endNode, relType));
			}
			app.commitTx();

		} finally {
			
			app.finishTx();
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
		
		return getUuidFromLocation(RestAssured.given().contentType("application/json; charset=UTF-8")
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

	//~--- set methods ----------------------------------------------------

	@Override
	protected void setUp() throws Exception {

		final Properties context = Services.getDefaultConfiguration();
		final Date now           = new Date();
		final long timestamp     = now.getTime();
		
		basePath = "/tmp/structr-test-" + timestamp;

		// enable "just testing" flag to avoid JAR resource scanning
		context.setProperty(Services.TESTING, "true");
		
		context.setProperty(Services.CONFIGURED_SERVICES, "NodeService LogService RestService");
		context.setProperty(Services.CONFIGURATION, ModuleService.class.getName());
		context.setProperty(Services.APPLICATION_TITLE, "structr unit test app" + timestamp);
		context.setProperty(Services.APPLICATION_HOST, host);
		context.setProperty(Services.APPLICATION_HTTP_PORT, Integer.toString(httpPort));
		context.setProperty(Services.TMP_PATH, "/tmp/");
		context.setProperty(Services.BASE_PATH, basePath);
		context.setProperty(Services.DATABASE_PATH, basePath + "/db");
		context.setProperty(Services.FILES_PATH, basePath + "/files");
		context.setProperty(Services.LOG_DATABASE_PATH, basePath + "/logDb.dat");
		context.setProperty(Services.TCP_PORT, "13465");
		context.setProperty(Services.UDP_PORT, "13466");
		context.setProperty(Services.SUPERUSER_USERNAME, "superadmin");
		context.setProperty(Services.SUPERUSER_PASSWORD, "sehrgeheim");
		
		// configure JsonRestServlet
		context.setProperty(RestService.SERVLETS, "JsonRestServlet");
		context.setProperty("JsonRestServlet.class", JsonRestServlet.class.getName());
		context.setProperty("JsonRestServlet.path", restUrl);
		context.setProperty("JsonRestServlet.resourceprovider", TestResourceProvider.class.getName());
		context.setProperty("JsonRestServlet.authenticator", SuperUserAuthenticator.class.getName());
		context.setProperty("JsonRestServlet.user.class", "");
		context.setProperty("JsonRestServlet.user.autocreate", "false");
		context.setProperty("JsonRestServlet.defaultview", PropertyView.Public);
		context.setProperty("JsonRestServlet.outputdepth", "3");
		
		final Services services = Services.getInstance(context);

		securityContext		= SecurityContext.getSuperUserInstance();
		app			= StructrApp.getInstance(securityContext);

		// wait for service layer to be initialized
		do {
			try { Thread.sleep(100); } catch(Throwable t) {}
			
		} while(!services.isInitialized());
	}

	
	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}
	
	protected static Matcher isEntity(Class<? extends AbstractNode> type) {
		return new EntityMatcher(type);
	}
	
	private static void checkCharset() {
		
		System.out.println("######### Charset settings ##############");
		System.out.println("Default Charset=" + Charset.defaultCharset());
		System.out.println("file.encoding=" + System.getProperty("file.encoding"));
		System.out.println("Default Charset=" + Charset.defaultCharset());
		System.out.println("Default Charset in Use=" + getEncodingInUse());
		System.out.println("This should look like the umlauts of 'a', 'o', 'u' and 'ss': äöüß");		
		System.out.println("#########################################");
		
	}
	

	private static String getEncodingInUse() {
		OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
		return writer.getEncoding();
	}

	// disabled to be able to test on windows systems
//	public void testCharset() {
//		assertTrue(StringUtils.remove(getEncodingInUse().toLowerCase(), "-").equals("utf8"));
//	}

}
