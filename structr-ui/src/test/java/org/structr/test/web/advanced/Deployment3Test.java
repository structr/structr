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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.export.StructrSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.Object;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class Deployment3Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment3Test.class.getName());

	@Test
	public void test31RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test31");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test31");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div", "");
			final Table table1    = createElement(page, div2, "table");
			final Thead thead     = createElement(page, table1, "thead");
			final Tbody tbody     = createElement(page, table1, "tbody");
			final Tr tr1          = createElement(page, thead, "tr");
			final Tr tr2          = createElement(page, tbody, "tr");
			final Td td11         = createElement(page, tr1, "td");
			final Content c1      = createContent(page, td11, "");
			final Td td12         = createElement(page, tr1, "td", "content12");
			final P p1            = createElement(page, td12, "p", "");
			final Ul ul           = createElement(page, p1, "ul");
			final Li li           = createElement(page, ul, "li", "");
			final Td td21         = createElement(page, tr2, "td", "content21");
			final Td td22         = createElement(page, tr2, "td", "content22");
			final Select select   = createElement(page, td11, "select");
			final Option option1  = createElement(page, select, "option", "");
			final Option option2  = createElement(page, select, "option", "value2");
			final Content c2      = createContent(page, div2, "");
			final Table table2    = createElement(page, div2, "table");

			// include visibility flags
			page.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			c1.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			c2.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);

			// modify visibility to produce two consecutive deployment instruction comments
			td12.setProperty(AbstractNode.visibleToPublicUsers, true);
			table2.setProperty(AbstractNode.visibleToPublicUsers, true);

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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test32");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div", " ");
			final Ul ul           = createElement(page, div1, "ul");
			final Li li           = createElement(page, ul, "li", " ");

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

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,                        "method1"),
				new NodeAttribute<>(SchemaMethod.source,                      "source1"),
				new NodeAttribute<>(SchemaMethod.virtualFileName,             "virtualFileName1")
			);

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,                       "method2"),
				new NodeAttribute<>(SchemaMethod.source,                     "source2"),
				new NodeAttribute<>(SchemaMethod.virtualFileName,            "virtualFileName2")
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

			final SchemaMethod method1 = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.name, "method1").getFirst();
			final SchemaMethod method2 = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.name, "method2").getFirst();

			assertNotNull("Invalid deployment result", method1);
			assertNotNull("Invalid deployment result", method2);

			assertEquals("Invalid SchemaMethod deployment result", "method1",          method1.getProperty(SchemaMethod.name));
			assertEquals("Invalid SchemaMethod deployment result", "source1",          method1.getProperty(SchemaMethod.source));
			assertEquals("Invalid SchemaMethod deployment result", "virtualFileName1", method1.getProperty(SchemaMethod.virtualFileName));

			assertEquals("Invalid SchemaMethod deployment result", "method2",          method2.getProperty(SchemaMethod.name));
			assertEquals("Invalid SchemaMethod deployment result", "source2",          method2.getProperty(SchemaMethod.source));
			assertEquals("Invalid SchemaMethod deployment result", "virtualFileName2", method2.getProperty(SchemaMethod.virtualFileName));

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
			final Html html     = createElement(testPage, testPage, "html");
			final Head head     = createElement(testPage, html, "head");
			final Body body     = createElement(testPage, html, "body");
			final Div div       = createElement(testPage, body, "div");
			final Div div2      = createElement(testPage, body, "div");

			div.setProperty(AbstractNode.name, "WidgetTestPage-Div");
			div2.setProperty(AbstractNode.name, "WidgetTestPage-Div2");

			Widget widgetToImport = app.create(Widget.class,
					new NodeAttribute<>(StructrApp.key(Widget.class, "name"),"TestWidget"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "source"),                      "<!-- @structr:content(text/html) --><structr:template>${{Structr.print(\"<div>Test</div>\");}}</structr:template>"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "configuration"),               "{\"processDeploymentInfo\": true}"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToPublicUsers"),        true),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToAuthenticatedUsers"), true)
			);

			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("widgetHostBaseUrl", "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId", widgetToImport.getProperty(new StartNode<>("owner", PrincipalOwnsNode.class)));
			paramMap.put("source", widgetToImport.getProperty(new StringProperty("source")));
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

			Div div = (Div)app.nodeQuery().andName("WidgetTestPage-Div").getFirst();

			assertEquals(1, div.treeGetChildCount());

			Object obj = div.treeGetFirstChild();

			assertTrue(Template.class.isAssignableFrom(obj.getClass()));

			Template template = (Template)obj;

			assertEquals("${{Structr.print(\"<div>Test</div>\");}}", template.getTextContent());
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
			final Html html     = createElement(testPage, testPage, "html");
			final Head head     = createElement(testPage, html, "head");
			final Body body     = createElement(testPage, html, "body");
			final Div div       = createElement(testPage, body, "div");
			final Div div2      = createElement(testPage, body, "div");

			div.setProperty(AbstractNode.name, "WidgetTestPage-Div");
			div2.setProperty(AbstractNode.name, "WidgetTestPage-Div2");

			Widget widgetToImport = app.create(Widget.class,
					new NodeAttribute<>(StructrApp.key(Widget.class, "name"), "TestWidget"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "source"),
						"<structr:component src=\"TestComponent\">\n" +
						"	<div data-structr-meta-name=\"TestComponent\">\n" +
						"		Test123\n" +
						"	</div>\n" +
						"</structr:component>"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "configuration"), ""),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToPublicUsers"), true),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToAuthenticatedUsers"), true)

			);

			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("widgetHostBaseUrl", "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId", widgetToImport.getProperty(new StartNode<>("owner", PrincipalOwnsNode.class)));
			paramMap.put("source", widgetToImport.getProperty(new StringProperty("source")));
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

			Div div = app.nodeQuery(Div.class).andName("WidgetTestPage-Div").getFirst();

			assertEquals(2, div.treeGetChildCount());

			Object obj = null;
			for(DOMNode n: div.getAllChildNodes()){
				obj = n;
				break;
			}

			assertTrue(Div.class.isAssignableFrom(obj.getClass()));

			Div clonedNode = (Div)obj;

			assertEquals(0, clonedNode.getChildNodes().getLength());
			assertEquals(3, app.nodeQuery(Div.class).andName("TestComponent").getAsList().size());

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

			final JsonType pageType = schema.getType("Page");
			final JsonType fileType = schema.getType("File");
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

			page.setProperty(StructrApp.key(Page.class, "displayPosition"), 12);
			page.setProperty(StructrApp.key(Page.class, "icon"),            "icon");

			final Folder folder = app.create(Folder.class, "files");
			folder.setProperty(StructrApp.key(Folder.class, "includeInFrontendExport"), true);

			// create test file with custom attributes
			app.create(File.class,
				new NodeAttribute<>(StructrApp.key(File.class, "name"),        "test.txt"),
				new NodeAttribute<>(StructrApp.key(File.class, "parent"),      folder),
				new NodeAttribute<>(StructrApp.key(File.class, "contentType"), "text/plain"),
				new NodeAttribute<>(StructrApp.key(File.class, "test1"),       123),
				new NodeAttribute<>(StructrApp.key(File.class, "test2"),       "testString")
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

			final Page page = Page.createSimplePage(securityContext, "page1");
			final Div div   = (Div)page.getElementsByTagName("div").item(0);

			try {

				final DOMNode newNode = (DOMNode) page.createTextNode("#template");
				newNode.unlockSystemPropertiesOnce();
				newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(NodeInterface.type, Template.class.getSimpleName()));

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

			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "ExtendedFile"),
				new NodeAttribute<>(SchemaNode.extendsClass, app.nodeQuery(SchemaNode.class).andName("File").getFirst()),
				new NodeAttribute<>(new StringProperty("_test"), "String")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class type       = StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile");
		final PropertyKey test = StructrApp.key(type, "test");

		assertNotNull("Extended file type should exist", type);
		assertNotNull("Extended file property should exist", test);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", type, "test.txt", true);

			node.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
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

			final Folder folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final Folder rootFolder = getRootFolder(folder);

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(StructrApp.key(Folder.class, "includeInFrontendExport"), true);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		doImportExportRoundtrip(true, true, null);

		// check
		try (final Tx tx = app.tx()) {

			final Folder folder = app.nodeQuery(Folder.class).andName("filesystem").getFirst();

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
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test40_1");
			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			// create first template and give it a name
			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			final PropertyMap template1Properties = new PropertyMap();
			template1Properties.put(Template.name, "Test40Template");
			template1.setProperties(template1.getSecurityContext(), template1Properties);


			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test40_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test40_2");
			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

			// create second template and give it the same name as the first one
			final Template template2 = createTemplate(page2, div2, "template source 2 - öäüÖÄÜß'\"'`");
			final PropertyMap template2Properties = new PropertyMap();
			template2Properties.put(Template.name, "Test40Template");
			template2.setProperties(template2.getSecurityContext(), template2Properties);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}
}
