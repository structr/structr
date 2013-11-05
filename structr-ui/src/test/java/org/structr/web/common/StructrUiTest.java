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
package org.structr.web.common;


import com.jayway.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.FileUtils;


import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.GraphDatabaseCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.URL;
import java.nio.charset.Charset;

import java.util.*;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.structr.Ui;
import org.structr.core.property.PropertyMap;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.files.ftp.FtpService;
import org.structr.server.Structr;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.servlet.WebSocketServlet;

//~--- classes ----------------------------------------------------------------

/**
 * Base class for all structr UI tests
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class StructrUiTest extends TestCase {

	private static final Logger logger = Logger.getLogger(StructrUiTest.class.getName());

	//~--- fields ---------------------------------------------------------

	protected Map<String, String> context = new LinkedHashMap<>();
	protected GraphDatabaseCommand graphDbCommand;
	protected SecurityContext securityContext;
	protected App app;

	// the jetty server
	private boolean running = false;
	private Server server;
	private String basePath;
	
	protected static final String prot = "http://";
//	protected static final String contextPath = "/";
	protected static final String restUrl = "/structr/rest";
	protected static final String htmlUrl = "/structr/html";
	protected static final String wsUrl = "/structr/ws";
	protected static final String host = "localhost";
	protected static final int httpPort = 8875;
	
	protected static String baseUri;
	
	static {

		// check character set
		checkCharset();
		
		baseUri = prot  + host + ":" + httpPort + htmlUrl + "/";
		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = prot + host + ":" + httpPort;
		RestAssured.port = httpPort;
		
	}
	
	//~--- methods --------------------------------------------------------

	protected void init() {

		String name = "structr-ui-test-" + System.nanoTime();
		
		// set up base path
		basePath = "/tmp/" + name;
		
		try {

			// HTML Servlet
			HtmlServlet htmlServlet = new HtmlServlet();
			ServletHolder htmlServletHolder = new ServletHolder(htmlServlet);
			Map<String, String> htmlInitParams = new HashMap<>();

			htmlInitParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
			htmlServletHolder.setInitParameters(htmlInitParams);
			htmlServletHolder.setInitOrder(1);
			
			// CSV Servlet
//			CsvServlet csvServlet     = new CsvServlet(DefaultResourceProvider.class.newInstance(), PropertyView.All, AbstractNode.uuid);
//			ServletHolder csvServletHolder    = new ServletHolder(csvServlet);
//			Map<String, String> servletParams = new HashMap<String, String>();
//
//			servletParams.put("Authenticator", "org.structr.web.auth.HttpAuthenticator");
//			csvServletHolder.setInitParameters(servletParams);
//			csvServletHolder.setInitOrder(2);

			// WebSocket Servlet
			WebSocketServlet wsServlet = new WebSocketServlet(AbstractNode.uuid);
			ServletHolder wsServletHolder = new ServletHolder(wsServlet);
			Map<String, String> wsInitParams = new HashMap<>();

			wsInitParams.put("Authenticator", "org.structr.web.auth.UiAuthenticator");
			wsInitParams.put("IdProperty", "uuid");
			wsServletHolder.setInitParameters(wsInitParams);
			wsServletHolder.setInitOrder(3);

			server = Structr.createServer(Ui.class, "structr UI", httpPort)

				.host(host)
				.basePath(basePath)
				
				.addServlet(htmlUrl + "/*", htmlServletHolder)
				.addServlet(wsUrl + "/*", wsServletHolder)
				//.addServlet("/structr/csv/*", csvServletHolder)
			    
				.addResourceHandler("/structr", "src/main/resources/structr", true, new String[] { "index.html"})
				
				.addConfiguredServices(FtpService.class)
				.ftpPort(8876)
			    
				.enableRewriteFilter()
				//.logRequests(true)
				
				.resourceProvider(UiResourceProvider.class)
				.authenticator(UiAuthenticator.class)
				
			    
				.start(false, true);
			
			running = server.isRunning();

		} catch(Throwable t) {
			
			t.printStackTrace();
		}

	}

	public void test00() {
	}

	@Override
	protected void tearDown() throws Exception {
		
		if (running) {

			// stop structr
			server.stop();

			// do we need to wait here? answer: yes!
			
			do {
				try { Thread.sleep(100); } catch(Throwable t) {}
				
			} while(!server.isStopped());

			server.destroy();

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

		List<Class> classes = new ArrayList<Class>();

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

	protected <T extends AbstractNode> List<T> createTestNodes(final Class<T> type, final int number) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		props.put(AbstractNode.type, type.getSimpleName());

		try {
			List<T> nodes = new LinkedList<>();
			
			app.beginTx();
			for (int i = 0; i < number; i++) {
				props.put(AbstractNode.name, type.getSimpleName() + i);
				nodes.add(app.create(type, props));
			}
			app.commitTx();

			return nodes;
			
		} finally {
			
			app.finishTx();
		}
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number) throws FrameworkException {
		final PropertyMap props = new PropertyMap();
		return createTestNodes(type, number, props);
	}

	protected List<NodeInterface> createTestNodes(final String type, final int number, final PropertyMap props) throws FrameworkException {

		props.put(AbstractNode.type, type);

		try {
			
			List<NodeInterface> nodes = new LinkedList<>();

			app.beginTx();
			for (int i = 0; i < number; i++) {
				nodes.add(app.create(AbstractNode.class, props));
			}

			app.commitTx();
			
			return nodes;

		} finally {
			
			app.finishTx();
		}
	}

	protected List<RelationshipInterface> createTestRelationships(final Class relType, final int number) throws FrameworkException {

		List<NodeInterface> nodes     = createTestNodes("UnknownTestType", 2);
		final NodeInterface startNode = nodes.get(0);
		final NodeInterface endNode   = nodes.get(1);


		try {
			List<RelationshipInterface> rels = new LinkedList<>();

			app.beginTx();
			for (int i = 0; i < number; i++) {
				rels.add(app.create(startNode, endNode, relType));
			}

			return rels;

		} finally {
			
			app.finishTx();
		}


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

		init();

		securityContext           = SecurityContext.getSuperUserInstance();
		graphDbCommand            = Services.command(securityContext, GraphDatabaseCommand.class);

		app = StructrApp.getInstance(securityContext);
	}
	
	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
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
