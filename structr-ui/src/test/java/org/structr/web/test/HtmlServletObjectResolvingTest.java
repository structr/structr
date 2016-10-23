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
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Structr;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.module.JarConfigurationProvider;
import org.structr.rest.service.HttpService;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.UiResourceProvider;
import org.structr.web.entity.TestOne;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.servlet.WebSocketServlet;
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
	protected static App app                             = null;
	protected static String basePath                     = null;

	// the jetty server
	private boolean running = false;

	protected static final String prot = "http://";
//	protected static final String contextPath = "/";
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

		config = Services.getBaseConfiguration();

		final long timestamp = System.currentTimeMillis();

		basePath = "/tmp/structr-test-" + timestamp + "-" + System.nanoTime();

		config.put(HtmlServlet.OBJECT_RESOLUTION_PROPERTIES, "TestOne.anInt, TestOne.aString, TestOne.aDouble");

		// enable "just testing" flag to avoid JAR resource scanning
		config.setProperty(Services.TESTING, "true");

		config.setProperty(Services.CONFIGURATION, JarConfigurationProvider.class.getName());
		config.setProperty(Services.CONFIGURED_SERVICES, "NodeService HttpService SchemaService");
		config.setProperty(Structr.DATABASE_CONNECTION_URL, Structr.TEST_DATABASE_URL);
		config.setProperty(Services.TMP_PATH, "/tmp/");
		config.setProperty(Services.BASE_PATH, basePath);
		config.setProperty(Structr.DATABASE_PATH, basePath + "/db");
		config.setProperty(Services.FILES_PATH, basePath + "/files");
		config.setProperty(Services.LOG_DATABASE_PATH, basePath + "/logDb.dat");
		config.setProperty(Services.TCP_PORT, (System.getProperty("tcpPort") != null ? System.getProperty("tcpPort") : "13465"));
		config.setProperty(Services.UDP_PORT, (System.getProperty("udpPort") != null ? System.getProperty("udpPort") : "13466"));
		config.setProperty(Services.SUPERUSER_USERNAME, "superadmin");
		config.setProperty(Services.SUPERUSER_PASSWORD, "sehrgeheim");

		// configure servlets
		config.setProperty(HttpService.APPLICATION_TITLE, "structr unit test app" + timestamp);
		config.setProperty(HttpService.APPLICATION_HOST, host);
		config.setProperty(HttpService.APPLICATION_HTTP_PORT, Integer.toString(httpPort));
		config.setProperty(HttpService.SERVLETS, "JsonRestServlet WebSocketServlet HtmlServlet");

		config.setProperty("JsonRestServlet.class", JsonRestServlet.class.getName());
		config.setProperty("JsonRestServlet.path", restUrl);
		config.setProperty("JsonRestServlet.resourceprovider", UiResourceProvider.class.getName());
		config.setProperty("JsonRestServlet.authenticator", UiAuthenticator.class.getName());
		config.setProperty("JsonRestServlet.user.class", User.class.getName());
		config.setProperty("JsonRestServlet.user.autocreate", "false");
		config.setProperty("JsonRestServlet.defaultview", PropertyView.Public);
		config.setProperty("JsonRestServlet.outputdepth", "3");

		config.setProperty("WebSocketServlet.class", WebSocketServlet.class.getName());
		config.setProperty("WebSocketServlet.path", wsUrl);
		config.setProperty("WebSocketServlet.resourceprovider", UiResourceProvider.class.getName());
		config.setProperty("WebSocketServlet.authenticator", UiAuthenticator.class.getName());
		config.setProperty("WebSocketServlet.user.class", User.class.getName());
		config.setProperty("WebSocketServlet.user.autocreate", "false");
		config.setProperty("WebSocketServlet.defaultview", PropertyView.Public);
		config.setProperty("WebSocketServlet.outputdepth", "3");

		config.setProperty("HtmlServlet.class", HtmlServlet.class.getName());
		config.setProperty("HtmlServlet.path", htmlUrl);
		config.setProperty("HtmlServlet.resourceprovider", UiResourceProvider.class.getName());
		config.setProperty("HtmlServlet.authenticator", UiAuthenticator.class.getName());
		config.setProperty("HtmlServlet.user.class", User.class.getName());
		config.setProperty("HtmlServlet.user.autocreate", "false");
		config.setProperty("HtmlServlet.defaultview", PropertyView.Public);
		config.setProperty("HtmlServlet.outputdepth", "3");

		// Configure resource handlers
		config.setProperty(HttpService.RESOURCE_HANDLERS, "StructrUiHandler");

		config.setProperty("StructrUiHandler.contextPath", "/structr");
		config.setProperty("StructrUiHandler.resourceBase", "src/main/resources/structr");
		config.setProperty("StructrUiHandler.directoriesListed", Boolean.toString(false));
		config.setProperty("StructrUiHandler.welcomeFiles", "index.html");

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

		graphDbCommand = app.command(GraphDatabaseCommand.class);

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
