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

import org.structr.core.property.PropertyMap;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.GraphDatabaseCommand;

//~--- JDK imports ------------------------------------------------------------

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
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.log.ReadLogCommand;
import org.structr.core.log.WriteLogCommand;

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

	protected Map<String, String> context         = new LinkedHashMap<>();
	protected GraphDatabaseCommand graphDbCommand = null;
	protected SecurityContext securityContext     = null;
	protected ReadLogCommand readLogCommand;
	protected WriteLogCommand writeLogCommand;

	
	//	protected CreateNodeCommand createNodeCommand;
//	protected CreateRelationshipCommand createRelationshipCommand;
//	protected DeleteNodeCommand deleteNodeCommand;
//	protected DeleteRelationshipCommand deleteRelationshipCommand;
//	protected FindNodeCommand findNodeCommand;
//	protected SearchNodeCommand searchNodeCommand;
//	protected SearchRelationshipCommand searchRelationshipCommand;
//	protected TransactionCommand transactionCommand;
	
	protected App app = null;

	//~--- methods --------------------------------------------------------

	public void test00DbAvailable() {

		GraphDatabaseService graphDb = graphDbCommand.execute();

		assertTrue(graphDb != null);
	}

	@Override
	protected void tearDown() throws Exception {

		Services.shutdown();

		try {
			File testDir = new File(context.get(Services.BASE_PATH));

			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);
			} else {

				testDir.delete();
			}
			
		} catch(Throwable t) {
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

	protected List<NodeInterface> createTestNodes(final Class type, final int number, final long delay) throws FrameworkException {

		app.beginTx();
		
		List<NodeInterface> nodes = new LinkedList<>();

		for (int i = 0; i < number; i++) {

			nodes.add(app.create(type));
			
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {}
		}
		
		app.commitTx();

		return nodes;
	}

	protected List<NodeInterface> createTestNodes(final Class type, final int number) throws FrameworkException {
		
		return createTestNodes(type, number, 0);

	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type) throws FrameworkException {
		return (T)createTestNode(type, new PropertyMap());
	}

	protected <T extends AbstractNode> T createTestNode(final Class<T> type, final PropertyMap props) throws FrameworkException {

		props.put(AbstractNode.type, type.getSimpleName());

		app.beginTx();
		
		final T newNode = app.create(type, props);
		
		app.commitTx();
		
		return newNode;

	}

	protected <T extends Relation> List<T> createTestRelationships(final Class<T> relType, final int number) throws FrameworkException {

		List<NodeInterface> nodes     = createTestNodes(GenericNode.class, 2);
		final NodeInterface startNode = nodes.get(0);
		final NodeInterface endNode   = nodes.get(1);

		app.beginTx();

		List<T> rels = new LinkedList<>();

		for (int i = 0; i < number; i++) {

			rels.add(app.create(startNode, endNode, relType));
		}
		
		app.commitTx();
		
		return rels;

	}

	protected <T extends Relation> T createTestRelationship(final AbstractNode startNode, final AbstractNode endNode, final Class<T> relType) throws FrameworkException {

		app.beginTx();

		final T rel = app.create(startNode, endNode, relType);

		app.commitTx();
		
		return rel;
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


		Date now       = new Date();
		long timestamp = now.getTime();

		context.put(Services.CONFIGURED_SERVICES, "ModuleService NodeService LogService");
		context.put(Services.APPLICATION_TITLE, "structr unit test app" + timestamp);
		context.put(Services.TMP_PATH, "/tmp/");
		context.put(Services.BASE_PATH, "/tmp/structr-test-" + timestamp);
		context.put(Services.DATABASE_PATH, "/tmp/structr-test-" + timestamp + "/db");
		context.put(Services.FILES_PATH, "/tmp/structr-test-" + timestamp + "/files");
		context.put(Services.LOG_DATABASE_PATH, "/tmp/structr-test-" + timestamp + "/logDb.dat");
		context.put(Services.TCP_PORT, "13465");
		context.put(Services.SERVER_IP, "127.0.0.1");
		context.put(Services.UDP_PORT, "13466");
		context.put(Services.SUPERUSER_USERNAME, "superadmin");
		context.put(Services.SUPERUSER_PASSWORD, "sehrgeheim");
		
		Services.initialize(context);

		securityContext           = SecurityContext.getSuperUserInstance();
		
//		createNodeCommand         = Services.command(securityContext, CreateNodeCommand.class);
//		createRelationshipCommand = Services.command(securityContext, CreateRelationshipCommand.class);
//		deleteNodeCommand         = Services.command(securityContext, DeleteNodeCommand.class);
//		deleteRelationshipCommand = Services.command(securityContext, DeleteRelationshipCommand.class);
//		transactionCommand        = Services.command(securityContext, TransactionCommand.class);
		graphDbCommand            = Services.command(securityContext, GraphDatabaseCommand.class);
//		findNodeCommand           = Services.command(securityContext, FindNodeCommand.class);
//		searchNodeCommand         = Services.command(securityContext, SearchNodeCommand.class);
//		searchRelationshipCommand = Services.command(securityContext, SearchRelationshipCommand.class);
		writeLogCommand           = Services.command(securityContext, WriteLogCommand.class);
		readLogCommand            = Services.command(securityContext, ReadLogCommand.class);

		// wait for service layer to be initialized
		do {
			try { Thread.sleep(100); } catch(Throwable t) {}
			
		} while(!Services.isInitialized());

		app = StructrApp.getInstance(securityContext);
		
	}

}
