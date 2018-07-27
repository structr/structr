/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.basic;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
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
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.SchemaService;
import org.structr.web.entity.TestOne;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *
 *
 */
public class HtmlServletObjectResolvingTest {

	private static final Logger logger = LoggerFactory.getLogger(HtmlServletObjectResolvingTest.class.getName());

	@Test
	public void testObjectResolvingInHtmlServlet() {

		final List<String> testObjectIDs = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			// setup three different test objects to be found by HtmlServlet
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.anInt, 123)).getUuid());
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.aDouble, 0.345)).getUuid());
			testObjectIDs.add(app.create(TestOne.class, new NodeAttribute<>(TestOne.aString, "abcdefg")).getUuid());

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "testPage");
			if (newPage != null) {

				Element html  = newPage.createElement("html");
				Element head  = newPage.createElement("head");
				Element body  = newPage.createElement("body");
				Text textNode = newPage.createTextNode("${current.id}");

				try {
					// add HTML element to page
					newPage.appendChild(html);
					html.appendChild(head);
					html.appendChild(body);
					body.appendChild(textNode);

				} catch (DOMException dex) {

					logger.warn("", dex);

					throw new FrameworkException(422, dex.getMessage());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		RestAssured.basePath = "/structr/html";

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(0)))
			.when()
			.get("/testPage/123");

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(1)))
			.when()
			.get("/testPage/0.345");

		RestAssured
			.given()
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.statusCode(200)
			.body(Matchers.containsString(testObjectIDs.get(2)))
			.when()
			.get("/testPage/abcdefg");
	}

	protected static Properties config                   = new Properties();
	protected static GraphDatabaseCommand graphDbCommand = null;
	protected static SecurityContext securityContext     = null;
	protected static boolean needsCleanup                = false;
	protected static App app                             = null;
	protected static String basePath                     = null;

	// the jetty server
	private boolean running = false;

	protected static final String prot = "http://";
	protected static final String restUrl = "/structr/rest";
	protected static final String htmlUrl = "/structr/html";
	protected static final String wsUrl = "/structr/ws";
	protected static final String host = "localhost";
	protected static final int httpPort = (System.getProperty("httpPort") != null ? Integer.parseInt(System.getProperty("httpPort")) : 8875);
	protected static final int ftpPort = (System.getProperty("ftpPort") != null ? Integer.parseInt(System.getProperty("ftpPort")) : 8876);

	protected static String baseUri;

	static {

		baseUri = prot + host + ":" + httpPort + htmlUrl + "/";
		// configure RestAssured
		RestAssured.basePath = restUrl;
		RestAssured.baseURI = prot + host + ":" + httpPort;
		RestAssured.port = httpPort;

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

	@BeforeClass
	public static void start() throws Exception {
		start(null);
	}

	public static void start(final Map<String, Object> additionalConfig) {

		final long timestamp = System.currentTimeMillis();

		basePath = "/tmp/structr-test-" + timestamp + "-" + System.nanoTime();

		Settings.HtmlResolveProperties.setValue("TestOne.anInt, TestOne.aString, TestOne.aDouble");

		Settings.Services.setValue("NodeService SchemaService HttpService");
		Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.RelationshipCacheSize.setValue(1000);
		Settings.NodeCacheSize.setValue(1000);

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet WebSocketServlet HtmlServlet");

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

		graphDbCommand = app.command(GraphDatabaseCommand.class);

	}

	@Before
	public void cleanDatabase() {

		if (needsCleanup) {

			try (final Tx tx = app.tx()) {

				// delete remaining nodes without UUIDs etc.
				app.cypher("MATCH (n) WHERE NOT n:SchemaReloadingNode DETACH DELETE n", Collections.emptyMap());

				tx.success();

				FlushCachesCommand.flushAll();

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}
		}
	}

	public void cleanDatabaseAndSchema() {

		try (final Tx tx = app.tx()) {

			// delete everything
			app.cypher("MATCH (n) DETACH DELETE n", Collections.emptyMap());

			FlushCachesCommand.flushAll();

			SchemaService.ensureBuiltinTypesExist(app);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			logger.error("Exception while trying to clean database: {}", t.getMessage());
		}
	}

	@After
	public void enableCleanup() {
		needsCleanup = true;
	}

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

			} catch (Throwable t) {
			}

			try {
				Thread.sleep(500);
			} catch (Throwable t) {
			}
		}
	}
}
