/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.web.advanced;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class Deployment3Test extends DeploymentTestBase {

	@Test
	public void test31RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test31");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test31");

			final DOMElement body     = createElement(page, html, "body");
			final DOMElement div1     = createElement(page, body, "div");
			final DOMElement div2     = createElement(page, div1, "div", "");
			final DOMElement table1   = createElement(page, div2, "table");
			final DOMElement thead    = createElement(page, table1, "thead");
			final DOMElement tbody    = createElement(page, table1, "tbody");
			final DOMElement tr1      = createElement(page, thead, "tr");
			final DOMElement tr2      = createElement(page, tbody, "tr");
			final DOMElement td11     = createElement(page, tr1, "td");
			final Content c1          = createContent(page, td11, "");
			final DOMElement td12     = createElement(page, tr1, "td", "content12");
			final DOMElement p1       = createElement(page, td12, "p", "");
			final DOMElement ul       = createElement(page, p1, "ul");
			final DOMElement li       = createElement(page, ul, "li", "");
			final DOMElement td21     = createElement(page, tr2, "td", "content21");
			final DOMElement td22     = createElement(page, tr2, "td", "content22");
			final DOMElement select   = createElement(page, td11, "select");
			final DOMElement option1  = createElement(page, select, "option", "");
			final DOMElement option2  = createElement(page, select, "option", "value2");
			final Content c2          = createContent(page, div2, "");
			final DOMElement table2   = createElement(page, div2, "table");

			// include visibility flags
			page.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToAuthenticatedUsers"), true);
			c1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToAuthenticatedUsers"), true);
			c2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToAuthenticatedUsers"), true);

			// modify visibility to produce two consecutive deployment instruction comments
			td12.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToPublicUsers"), true);
			table2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToPublicUsers"), true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test32RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test32");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test32");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1 = createElement(page, body, "div");
			final DOMElement div2 = createElement(page, div1, "div", " ");
			final DOMElement ul   = createElement(page, div1, "ul");
			final DOMElement li   = createElement(page, ul, "li", " ");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test33SchemaMethods() {

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, "TestType");

			// create one method with a schema node
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("schemaNode"),                 testType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                       "method1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"),                     "source1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("includeInOpenAPI"),           false),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("tags"),                       new String[] { "tag1", "tag2" }),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("summary"),                    "summary"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("description"),                "description"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("isStatic"),                   true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("isPrivate"),                  true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult"),            true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("httpVerb"),                   "GET")
			);

			// and one without (i.e. user-defined function)
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "method2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("source"),                    "source2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key("virtualFileName"),           "virtualFileName2")
			);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface method1 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).and(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "method1").getFirst();
			final NodeInterface method2 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).and(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "method2").getFirst();

			assertNotNull("Invalid deployment result", method1);
			assertNotNull("Invalid deployment result", method2);

			assertEquals("Invalid SchemaMethod deployment result", "method1",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "source1",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("source")));
			assertEquals("Invalid SchemaMethod deployment result", false,          (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("includeInOpenAPI")));
			assertEquals("Invalid SchemaMethod deployment result", "tag1",         ((Object[])method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("tags")))[0]);
			assertEquals("Invalid SchemaMethod deployment result", "tag2",         ((Object[])method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("tags")))[1]);
			assertEquals("Invalid SchemaMethod deployment result", "summary",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("summary")));
			assertEquals("Invalid SchemaMethod deployment result", "description",  method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("description")));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("isStatic")));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("isPrivate")));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("returnRawResult")));
			assertEquals("Invalid SchemaMethod deployment result", "GET",          method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("httpVerb")));


			// Add new SchemaMethod properties here to make sure they are included in the schema import/export!

			assertEquals("Invalid SchemaMethod deployment result", "method2",          method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "source2",          method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("source")));
			assertEquals("Invalid SchemaMethod deployment result", "virtualFileName2", method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key("virtualFileName")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test34WidgetWithTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page testPage = Page.createNewPage(securityContext, "WidgetTestPage");
			final DOMElement html     = createElement(testPage, testPage, "html");
			final DOMElement head     = createElement(testPage, html, "head");
			final DOMElement body     = createElement(testPage, html, "body");
			final DOMElement div       = createElement(testPage, body, "div");
			final DOMElement div2      = createElement(testPage, body, "div");

			div.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "WidgetTestPage-Div");
			div2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "WidgetTestPage-Div2");

			NodeInterface widgetToImport = app.create(StructrTraits.WIDGET,
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),"TestWidget"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("source"),                      "<!-- @structr:content(text/html) --><structr:template>${{Structr.print(\"<div>Test</div>\");}}</structr:template>"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("configuration"),               "{\"processDeploymentInfo\": true}"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("visibleToPublicUsers"),        true),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("visibleToAuthenticatedUsers"), true)
			);

			Map<String,Object> paramMap = new HashMap<>();

			paramMap.put("widgetHostBaseUrl",    "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId",              widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
			paramMap.put("source",                widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key("source")));
			paramMap.put("processDeploymentInfo", true);

			Widget.expandWidget(securityContext, testPage, div, baseUri, paramMap, true);

			makePublic(testPage, html,head, body, div, div2);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// test
		try (final Tx tx = app.tx()) {

			NodeInterface div = app.nodeQuery().andName("WidgetTestPage-Div").getFirst();

			assertEquals(1, div.as(DOMNode.class).treeGetChildCount());

			NodeInterface obj = div.as(DOMNode.class).treeGetFirstChild();

			assertTrue(obj.is(StructrTraits.TEMPLATE));

			Template template = obj.as(Template.class);

			assertEquals("${{Structr.print(\"<div>Test</div>\");}}", template.getContent());
			assertEquals("text/html", template.getContentType());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test35WidgetWithSharedComponentCreation() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page testPage = Page.createNewPage(securityContext, "WidgetTestPage");
			final DOMElement html     = createElement(testPage, testPage, "html");
			final DOMElement head     = createElement(testPage, html, "head");
			final DOMElement body     = createElement(testPage, html, "body");
			final DOMElement div       = createElement(testPage, body, "div");
			final DOMElement div2      = createElement(testPage, body, "div");

			div.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "WidgetTestPage-Div");
			div2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "WidgetTestPage-Div2");

			NodeInterface widgetToImport = app.create(StructrTraits.WIDGET,
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestWidget"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("source"),
						"<structr:component src=\"TestComponent\">\n" +
						"	<div data-structr-meta-name=\"TestComponent\">\n" +
						"		Test123\n" +
						"	</div>\n" +
						"</structr:component>"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("configuration"), ""),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("visibleToPublicUsers"), true),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key("visibleToAuthenticatedUsers"), true)

			);

			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("widgetHostBaseUrl",     "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId",              widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
			paramMap.put("source",                widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key("source")));
			paramMap.put("processDeploymentInfo", false);

			Widget.expandWidget(securityContext, testPage, div, baseUri, paramMap, false);
			Widget.expandWidget(securityContext, testPage, div, baseUri, paramMap, false);

			makePublic(testPage, html,head, body, div, div2);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// test
		try (final Tx tx = app.tx()) {

			DOMNode div = app.nodeQuery("Div").andName("WidgetTestPage-Div").getFirst().as(DOMNode.class);

			assertEquals(2, div.treeGetChildCount());

			DOMNode obj = null;

			for (final NodeInterface n: div.getAllChildNodes()){
				obj = n.as(DOMNode.class);
				break;
			}

			DOMNode clonedNode = obj;

			assertEquals(0, clonedNode.getChildNodes().size());
			assertEquals(3, app.nodeQuery("Div").andName("TestComponent").getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test36BuiltInTypesWithProperties() {

		// setup schema
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			assertNotNull("StructrSchema must return a valid schema object", schema);

			final JsonType pageType = schema.getType(StructrTraits.PAGE);
			final JsonType fileType = schema.getType(StructrTraits.FILE);
			assertNotNull("Type Page must exist in every schema", pageType);
			assertNotNull("Type File must exist in every schema", fileType);

			pageType.addIntegerProperty("displayPosition");
			pageType.addStringProperty("icon");

			fileType.addIntegerProperty("test1");
			fileType.addStringProperty("test2");

			// install schema
			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "page1");

			page.setProperty(Traits.of(StructrTraits.PAGE).key("displayPosition"), 12);
			page.setProperty(Traits.of(StructrTraits.PAGE).key("icon"),            "icon");

			final NodeInterface folder = app.create(StructrTraits.FOLDER, "files");
			folder.setProperty(Traits.of(StructrTraits.FOLDER).key("includeInFrontendExport"), true);

			// create test file with custom attributes
			app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),        "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("parent"),      folder),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("contentType"), "text/plain"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("test1"),       123),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("test2"),       "testString")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test37SharedComponentTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			try {

				final DOMNode newNode = page.createTextNode("#template");
				newNode.unlockSystemPropertiesOnce();

				newNode.setProperty(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), StructrTraits.TEMPLATE);

				// append template
				div.appendChild(newNode);

				// create component from div
				createComponent(div);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test38DynamicFileExport() {

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "ExtendedFile"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("inheritedTraits"), new String[] { StructrTraits.FILE })
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("schemaNode"),   node),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),         "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key("propertyType"), "String")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String type      = "ExtendedFile";
		final PropertyKey test = Traits.of(type).key("test");

		assertNotNull("Extended file type should exist", type);
		assertNotNull("Extended file property should exist", test);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", type, "test.txt", true);

			node.setProperty(Traits.of(StructrTraits.FILE).key("includeInFrontendExport"), true);
			node.setProperty(test, "test");

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test39EmptyFolderInDeployment() {

		final String folderPath = "/empty/folders/in/filesystem";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key("includeInFrontendExport"), true);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		doImportExportRoundtrip(true, true, null);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).andName("filesystem").getFirst();

			assertNotNull("Invalid deployment result - empty folder from export was not imported!", folder);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test40TwoTemplatesWithSameNameInTwoPages() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test40_1");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test40_1");
			final DOMElement body1 = createElement(page1, html1, "body");
			final DOMElement div1   = createElement(page1, body1, "div");

			// create first template and give it a name
			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			final PropertyMap template1Properties = new PropertyMap();
			template1Properties.put(Traits.of(StructrTraits.TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test40Template");
			template1.setProperties(template1.getSecurityContext(), template1Properties);


			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test40_2");
			final DOMElement html2 = createElement(page2, page2, "html");
			final DOMElement head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test40_2");
			final DOMElement body2 = createElement(page2, html2, "body");
			final DOMElement div2   = createElement(page2, body2, "div");

			// create second template and give it the same name as the first one
			final Template template2 = createTemplate(page2, div2, "template source 2 - öäüÖÄÜß'\"'`");
			final PropertyMap template2Properties = new PropertyMap();
			template2Properties.put(Traits.of(StructrTraits.TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test40Template");
			template2.setProperties(template2.getSecurityContext(), template2Properties);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}
}
