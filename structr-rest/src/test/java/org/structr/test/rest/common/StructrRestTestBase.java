/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.test.rest.common;


import com.jayway.restassured.RestAssured;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.SuperUserAuthenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.DefaultResourceProvider;
import org.structr.schema.SchemaService;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.fail;

/**
 * Base class for all structr tests.
 * All tests are executed in superuser context.
 */
public abstract class StructrRestTestBase {

	protected final Logger logger             = LoggerFactory.getLogger(StructrRestTestBase.class.getName());
	protected String randomTenantId           = getRandomTenantIdentifier();
	protected SecurityContext securityContext = null;
	protected String basePath                 = null;
	protected App app                         = null;
	private boolean first                     = true;

	protected final String contextPath = "/";
	protected final String restUrl     = "/structr/rest";
	protected final String host        = "127.0.0.1";
	protected final int httpPort       = getNextPortNumber();

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	public void setup(@Optional String testDatabaseConnection) {

		final Set<String> htmlTypes = Set.of(
			"A", "Abbr", "Address", "Area", "Article", "Aside", "Audio", "B", "Base", "Bdi", "Bdo", "Blockquote", "Body", "Br", "Button", "Canvas", "Caption", "Cdata", "Cite", "Code",
			"Col", "Colgroup", "Command", "Comment", "Component", "Content", "ContentContainer", "ContentItem", "CssDeclaration", "CssRule", "CssSelector", "CssSemanticClass","Data",
			"Datalist", "Dd", "Del", "Details", "Dfn", "Dialog", "Div", "Dl", "Dt", "Em", "Embed", "Fieldset", "Figcaption", "Figure", "Footer", "Form", "G", "H1", "H2", "H3", "H4",
			"H5", "H6", "Head", "Header", "Hgroup", "Hr", "Html", "I", "Iframe", "Img", "Input", "Ins", "Kbd", "Keygen", "Label", "Legend", "Li", "Link", "Main", "Map", "Mark", "Menu",
			"Meta", "Meter", "Nav", "Noscript", "Object", "Ol", "Optgroup", "Option", "Output", "P", "Param", "Picture", "Pre", "Progress", "Q", "Rp", "Rt", "Ruby", "S","Samp",
			"Script", "Section", "Select", "Slot", "Small", "Source", "Span", "Strong", "Style", "Sub", "Summary", "Sup", "Table", "Tbody", "Td", "Template", "TemplateElement", "Textarea",
			"Tfoot", "Th", "Thead", "Time", "Title", "Tr", "Track", "U", "Ul", "Var", "Video", "Wbr", "Widget"
		);

		final Set<String> uiTypes = Set.of(
			"AbstractFile", "ActionMapping", "ApplicationConfigurationDataNode", "DOMElement", "DOMNode", "DocumentFragment", "File", "Folder", "Image", "Indexable", "IndexedWord",
			"JavaScriptSource", "LinkSource", "Linkable", "Page", "ParameterMapping", "ShadowDocument", "Site", "Template", "TemplateElement", "User", "Video"
		);

		SchemaService.getBlacklist().addAll(htmlTypes);
		SchemaService.getBlacklist().addAll(uiTypes);

		final long timestamp = System.nanoTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService SchemaService HttpService");
		setupDatabaseConnection(testDatabaseConnection);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.DatabasePath.setValue(basePath + "/db");
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		Settings.ApplicationTitle.setValue("structr unit test app" + timestamp);
		Settings.ApplicationHost.setValue(host);
		Settings.HttpPort.setValue(httpPort);

		Settings.Servlets.setValue("JsonRestServlet OpenAPIServlet");
		Settings.RestAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.RestResourceProvider.setValue(DefaultResourceProvider.class.getName());
		Settings.RestServletPath.setValue(restUrl);
		Settings.RestUserClass.setValue("");
		Settings.OpenAPIAuthenticator.setValue(SuperUserAuthenticator.class.getName());
		Settings.OpenAPIResourceProvider.setValue(DefaultResourceProvider.class.getName());

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		while (!services.isInitialized()) {
			try { Thread.sleep(100); } catch (Throwable t) {}
		}

		securityContext = SecurityContext.getSuperUserInstance();
		app             = StructrApp.getInstance(securityContext);

		// configure RestAssured
		RestAssured.basePath = "/structr/rest";
		RestAssured.baseURI  = "http://" + host + ":" + httpPort;
		RestAssured.port     = httpPort;
	}

	@AfterClass(alwaysRun = true)
	public void teardown() {

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
	}

	@BeforeMethod
	public void cleanDatabase() {

		if (!first) {

			try (final Tx tx = app.tx()) {

				// delete everything
				Services.getInstance().getDatabaseService().cleanDatabase();

				FlushCachesCommand.flushAll();

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}


			try {

				FlushCachesCommand.flushAll();

				SchemaService.ensureBuiltinTypesExist(app);

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to create built-in schema for tenant identifier {}: {}", randomTenantId, t.getMessage());

			}
		}

		first = false;
	}

	@BeforeMethod
	public void starting(Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Starting " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
		System.out.println("######################################################################################");
	}

	@AfterMethod
	public void finished(Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Finished " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
		System.out.println("######################################################################################");
	}

	protected int getNextPortNumber() {

		// allow override via system property (-DhttpPort=...)
		if (System.getProperty("httpPort") != null) {

			return Integer.parseInt(System.getProperty("httpPort"));
		};

		// use locked file to store last used port
		final String fileName = "/tmp/structr.test.port.lock";
		final int max         = 65500;
		final int min         = 8875;
		int port              = min;


		try (final RandomAccessFile raf = new RandomAccessFile(fileName, "rws")) {

			raf.getChannel().lock();

			if (raf.length() > 0) {

				port = raf.readInt();
			}

			port++;

			if (port > max) {
				port = min;
			}

			raf.setLength(0);
			raf.writeInt(port);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return port;
	}

	protected String getRandomTenantIdentifier() {
		return RandomStringUtils.randomAlphabetic(10).toUpperCase();
	}

	protected void setupDatabaseConnection(String testDatabaseConnection) {

		// use database driver from system property, default to MemoryDatabaseService
		Settings.DatabaseDriver.setValue(System.getProperty("testDatabaseDriver", Settings.DEFAULT_REMOTE_DATABASE_DRIVER));
		Settings.ConnectionUser.setValue("neo4j");
		Settings.ConnectionPassword.setValue("admin");
		if (StringUtils.isBlank(testDatabaseConnection)) {
			Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());
		} else {
			Settings.ConnectionUrl.setValue(testDatabaseConnection);
		}
		Settings.ConnectionDatabaseName.setValue("neo4j");
		Settings.TenantIdentifier.setValue(randomTenantId);
	}

	// ----- non-static methods -----
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

	protected String createEntity(String resource, String... body) {

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		return getUuidFromLocation(
			RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(buf.toString())
			.expect().statusCode(201).when().post(resource).getHeader("Location"));
	}

	protected String concat(String... parts) {

		StringBuilder buf = new StringBuilder();

		for (String part : parts) {
			buf.append(part);
		}

		return buf.toString();
	}

	protected String getUuidFromLocation(String location) {
		return location.substring(location.lastIndexOf("/") + 1);
	}

	protected Matcher isEntity(Class<? extends AbstractNode> type) {
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

	protected Class createTestUserType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TestUser");

			type.setExtends(URI.create("#/definitions/Principal"));
			type.overrideMethod("onCreate", true, "set(this, 'name', concat('test', now));");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		return StructrApp.getConfiguration().getNodeEntityClass("TestUser");
	}
}
