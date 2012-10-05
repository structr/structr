/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.rest.common;


import com.jayway.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.URL;
import java.nio.charset.Charset;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.hamcrest.Matcher;
import org.structr.common.SecurityContext;
import org.structr.context.ApplicationContextListener;
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

	protected Map<String, String> context = new ConcurrentHashMap<String, String>(20, 0.9f, 8);
	protected Command createNodeCommand;
	protected Command createRelationshipCommand;
	protected Command deleteNodeCommand;
	protected Command findNodeCommand;
	protected Command graphDbCommand;
	protected SecurityContext securityContext;
	protected Command transactionCommand;

	// the jetty server
	private boolean running = false;
	private Server server;
	private String basePath;
	
	protected static final String contextPath = "/";
	protected static final String restUrl = "/structr/rest";
	protected static final String host = "127.0.0.1";
	protected static final int restPort = 8875;
	
	static {

		// check character set
		checkCharset();
		
		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = "http://" + host + ":" + restPort;
		RestAssured.port = restPort;
	}
	
	//~--- methods --------------------------------------------------------

	protected void init() {

		/*
		Date now       = new Date();
		long timestamp = now.getTime();
		
		context.put(Services.CONFIGURED_SERVICES, "ModuleService NodeService");
		context.put(Services.APPLICATION_TITLE, "structr unit test app" + timestamp);
		context.put(Services.TMP_PATH, "/tmp/");
		context.put(Services.BASE_PATH, "/tmp/structr-test-" + timestamp);
		context.put(Services.DATABASE_PATH, "/tmp/structr-test-" + timestamp + "/db");
		context.put(Services.FILES_PATH, "/tmp/structr-test-" + timestamp + "/files");
		context.put(Services.TCP_PORT, "13465");
		context.put(Services.SERVER_IP, "127.0.0.1");
		context.put(Services.UDP_PORT, "13466");
		context.put(Services.SUPERUSER_USERNAME, "superadmin");
		context.put(Services.SUPERUSER_PASSWORD, "sehrgeheim");
		
		Services.initialize(context);
		*/
		
		String name = "structr-rest-test-" + System.nanoTime();
		
		// set up base path
		basePath = "/tmp/" + name;
		
		try {
			// create test directory
			File basePathFile                    = new File(basePath);
			
			basePathFile.mkdirs();
			
			
			String sourceJarName                 = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
			File confFile                        = checkStructrConf(basePath, sourceJarName);
			List<Connector> connectors           = new LinkedList<Connector>();
			HandlerCollection handlerCollection  = new HandlerCollection();

			server = new Server(restPort);
			
			ServletContextHandler servletContext = new ServletContextHandler(server, contextPath, ServletContextHandler.SESSIONS);

			// create resource collection from base path & source JAR
			servletContext.setBaseResource(new ResourceCollection(Resource.newResource(basePath), JarResource.newJarResource(Resource.newResource(sourceJarName))));
			servletContext.setInitParameter("configfile.path", basePath + "/structr.conf");

			// configure JSON REST servlet
			JsonRestServlet structrRestServlet     = new JsonRestServlet();
			ServletHolder structrRestServletHolder = new ServletHolder(structrRestServlet);

			Map<String, String> servletParams = new LinkedHashMap<String, String>();
			servletParams.put("PropertyFormat", "FlatNameValue");
			servletParams.put("ResourceProvider", DefaultResourceProvider.class.getName());
			servletParams.put("Authenticator", DefaultAuthenticator.class.getName());
			servletParams.put("IdProperty", "uuid");

			structrRestServletHolder.setInitParameters(servletParams);
			structrRestServletHolder.setInitOrder(0);

			// add to servlets
			Map<String, ServletHolder> servlets = new LinkedHashMap<String, ServletHolder>();
			servlets.put(restUrl + "/*", structrRestServletHolder);

			// add servlet elements
			int position = 1;
			for (Map.Entry<String, ServletHolder> servlet : servlets.entrySet()) {

				String path                 = servlet.getKey();
				ServletHolder servletHolder = servlet.getValue();

				servletHolder.setInitOrder(position++);

				logger.log(Level.INFO, "Adding servlet {0} for {1}", new Object[] { servletHolder, path } );

				servletContext.addServlet(servletHolder, path);
			}

			// register structr application context listener
			servletContext.addEventListener(new ApplicationContextListener());
			handlerCollection.addHandler(servletContext);

			server.setHandler(handlerCollection);

			if (host != null && !host.isEmpty() && restPort > -1) {

				SelectChannelConnector httpConnector = new SelectChannelConnector();

				httpConnector.setHost(host);
				httpConnector.setPort(restPort);
				httpConnector.setMaxIdleTime(30000);
				httpConnector.setRequestHeaderSize(8192);

				connectors.add(httpConnector);

			} else {

				logger.log(Level.WARNING, "Unable to configure REST port, please make sure that application.host, application.rest.port and application.rest.path are set correctly in structr.conf.");
			}

			if (!connectors.isEmpty()) {

				server.setConnectors(connectors.toArray(new Connector[0]));

			} else {

				logger.log(Level.SEVERE, "No connectors configured, aborting.");
				System.exit(0);
			}

			server.setGracefulShutdown(1000);
			server.setStopAtShutdown(true);

			server.start();
			
			running = server.isRunning();

		} catch(Throwable t) {
			
			t.printStackTrace();
		}
		

	}

	public void test00DbAvailable() {

		try {

			GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();

			assertTrue(graphDb != null);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	@Override
	protected void tearDown() throws Exception {

		if (running) {

			// stop structr
			server.stop();

			// do we need to wait here?

			File testDir = new File(basePath);

			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);

			} else {

				testDir.delete();
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

	protected List<AbstractNode> createTestNodes(final String type, final int number) throws FrameworkException {

		final Map<String, Object> props = new HashMap<String, Object>();

		props.put(AbstractNode.Key.type.name(), type);

		return (List<AbstractNode>) transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractNode> nodes = new LinkedList<AbstractNode>();

				for (int i = 0; i < number; i++) {

					nodes.add((AbstractNode) createNodeCommand.execute(props));
				}

				return nodes;

			}

		});

	}

	protected List<AbstractRelationship> createTestRelationships(final RelationshipType relType, final int number) throws FrameworkException {

		List<AbstractNode> nodes     = createTestNodes("UnknownTestType", 2);
		final AbstractNode startNode = nodes.get(0);
		final AbstractNode endNode   = nodes.get(1);

		return (List<AbstractRelationship>) transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

				for (int i = 0; i < number; i++) {

					rels.add((AbstractRelationship) createRelationshipCommand.execute(startNode, endNode, relType));
				}

				return rels;

			}

		});

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
		List<File> dirs            = new ArrayList<File>();

		while (resources.hasMoreElements()) {

			URL resource = resources.nextElement();

			dirs.add(new File(resource.getFile()));

		}

		List<Class> classList = new ArrayList<Class>();

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
		createNodeCommand         = Services.command(securityContext, CreateNodeCommand.class);
		createRelationshipCommand = Services.command(securityContext, CreateRelationshipCommand.class);
		deleteNodeCommand         = Services.command(securityContext, DeleteNodeCommand.class);
		transactionCommand        = Services.command(securityContext, TransactionCommand.class);
		graphDbCommand            = Services.command(securityContext, GraphDatabaseCommand.class);
		findNodeCommand           = Services.command(securityContext, FindNodeCommand.class);

	}


	private File checkStructrConf(String basePath, String sourceJarName) throws IOException {

		// create and register config file
		String confPath = basePath + "/structr.conf";
		File confFile   = new File(confPath);

		// Create structr.conf if not existing
		if (!confFile.exists()) {

			// synthesize a config file
			List<String> config = new LinkedList<String>();

			config.add("##################################");
			config.add("# structr global config file     #");
			config.add("##################################");
			config.add("");
			
			if (sourceJarName.endsWith(".jar") || sourceJarName.endsWith(".war")) {
				
				config.add("# resources");
				config.add("resources = " + sourceJarName);
				config.add("");
			}
			
			config.add("# JSON output nesting depth");
			config.add("json.depth = 1");
			config.add("");
			config.add("# base directory");
			config.add("base.path = " + basePath);
			config.add("");
			config.add("# temp files directory");
			config.add("tmp.path = /tmp");
			config.add("");
			config.add("# database files directory");
			config.add("database.path = " + basePath + "/db");
			config.add("");
			config.add("# binary files directory");
			config.add("files.path = " + basePath + "/files");
			config.add("");
			config.add("# REST server settings");
			config.add("application.host = " + host);
			config.add("application.rest.port = " + restPort);
			config.add("application.rest.path = " + restUrl);
			config.add("");
			config.add("application.https.enabled = false");
			config.add("application.https.port = ");
			config.add("application.keystore.path = ");
			config.add("application.keystore.password = ");
			config.add("");
			config.add("# SMPT settings");
			config.add("smtp.host = localhost");
			config.add("smtp.port = 25");
			config.add("");
			config.add("superuser.username = superadmin");
			config.add("superuser.password = sehrgeheim");
			config.add("");
			config.add("# services");
			config.add("configured.services = ModuleService NodeService");
			config.add("");
			config.add("log.requests = false");
			config.add("log.name = structr-yyyy_mm_dd.request.log");

			confFile.createNewFile();
			FileUtils.writeLines(confFile, "UTF-8", config);
		}
		
		return confFile;
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
	
	public void testCharset() {
		assertTrue(StringUtils.remove(getEncodingInUse().toLowerCase(), "-").equals("utf8"));
	}

}
