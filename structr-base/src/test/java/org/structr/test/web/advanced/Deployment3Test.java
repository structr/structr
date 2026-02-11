/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.advanced;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.schema.export.StructrSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.*;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.WidgetTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class Deployment3Test extends DeploymentTestBase {

	@Test
	public void test30Localizations() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.LOCALIZATION,
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "localization1"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.DOMAIN_PROPERTY),         "domain1"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALE_PROPERTY),         "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY), "localizedName1")
			);

			app.create(StructrTraits.LOCALIZATION,
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "localization2"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.DOMAIN_PROPERTY),         "domain2"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALE_PROPERTY),         "en"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY), "localizedName2")
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

			final NodeInterface localization1 = app.nodeQuery(StructrTraits.LOCALIZATION).key(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "localization1").getFirst();
			final NodeInterface localization2 = app.nodeQuery(StructrTraits.LOCALIZATION).key(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "localization2").getFirst();

			assertNotNull("Invalid deployment result", localization1);
			assertNotNull("Invalid deployment result", localization2);

			assertEquals("Invalid Localization deployment result", "localization1",  localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "domain1",        localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.DOMAIN_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "de_DE",          localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALE_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "localizedName1", localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY)));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));

			assertEquals("Invalid Localization deployment result", "localization2",  localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "domain2",        localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.DOMAIN_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "en",             localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALE_PROPERTY)));
			assertEquals("Invalid Localization deployment result", "localizedName2", localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY)));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test31IncreasingIndentationCountInRoundtrip() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test31");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test31");

			final DOMElement body     = createElement(page, html, "body");
			final DOMElement div1     = createElement(page, body, "div");
			final DOMElement div2     = createElement(page, div1, "div", "This is a test.");
			final DOMElement table1   = createElement(page, div2, "table");
			final DOMElement thead    = createElement(page, table1, "thead");
			final DOMElement tbody    = createElement(page, table1, "tbody");
			final DOMElement tr1      = createElement(page, thead, "tr");
			final DOMElement tr2      = createElement(page, tbody, "tr");
			final DOMElement td11     = createElement(page, tr1, "td", "content11", "Content before <select>");
			final DOMElement td12     = createElement(page, tr1, "td", "content12");
			final DOMElement td21     = createElement(page, tr2, "td", "content21");
			final DOMElement td22     = createElement(page, tr2, "td", "content22");
			final DOMElement select   = createElement(page, td11, "select");
			final DOMElement option1  = createElement(page, select, "option", "value1");
			final DOMElement option2  = createElement(page, select, "option", "value2");

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
			page.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			c1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			c2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			// modify visibility to produce two consecutive deployment instruction comments
			td12.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			table2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test33RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test33");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test33");

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
	public void test34SchemaMethods() {

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, "TestType");

			// create one method with a schema node
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY),                testType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "method1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),                     "source1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY),        false),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.TAGS_PROPERTY),                       new String[] { "tag1", "tag2" }),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SUMMARY_PROPERTY),                    "summary"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY),                "description"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY),                  true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY),                 true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.RETURN_RAW_RESULT_PROPERTY),          true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY),                  "GET")
			);

			// and one without (i.e. user-defined function)
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "method2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),                     "source2"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.VIRTUAL_FILE_NAME_PROPERTY),          "virtualFileName2")
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

			final NodeInterface method1 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "method1").getFirst();
			final NodeInterface method2 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "method2").getFirst();

			assertNotNull("Invalid deployment result", method1);
			assertNotNull("Invalid deployment result", method2);

			assertEquals("Invalid SchemaMethod deployment result", "method1",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "source1",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", false,          (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "tag1",         ((Object[])method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.TAGS_PROPERTY)))[0]);
			assertEquals("Invalid SchemaMethod deployment result", "tag2",         ((Object[])method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.TAGS_PROPERTY)))[1]);
			assertEquals("Invalid SchemaMethod deployment result", "summary",      method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SUMMARY_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "description",  method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", true,           (boolean)method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.RETURN_RAW_RESULT_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "GET",          method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY)));


			// Add new SchemaMethod properties here to make sure they are included in the schema import/export!

			assertEquals("Invalid SchemaMethod deployment result", "method2",          method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "source2",          method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "virtualFileName2", method2.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.VIRTUAL_FILE_NAME_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test35WidgetWithTemplate() {

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
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.SOURCE_PROPERTY),                      "<!-- @structr:content(text/html) --><structr:template>${{Structr.print(\"<div>Test</div>\");}}</structr:template>"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.CONFIGURATION_PROPERTY),               "{\"processDeploymentInfo\": true}"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        true),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)
			);

			Map<String,Object> paramMap = new HashMap<>();

			paramMap.put("widgetHostBaseUrl",    "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId",              widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
			paramMap.put("source",                widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.SOURCE_PROPERTY)));
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

			NodeInterface div = app.nodeQuery().and().name("WidgetTestPage-Div").getFirst();

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
	public void test36WidgetWithSharedComponentCreation() {

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
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.SOURCE_PROPERTY),
						"<structr:component src=\"TestComponent\">\n" +
						"	<div data-structr-meta-name=\"TestComponent\">\n" +
						"		Test123\n" +
						"	</div>\n" +
						"</structr:component>"),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.CONFIGURATION_PROPERTY), ""),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.WIDGET).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)

			);

			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("widgetHostBaseUrl",     "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId",              widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
			paramMap.put("source",                widgetToImport.getProperty(Traits.of(StructrTraits.WIDGET).key(WidgetTraitDefinition.SOURCE_PROPERTY)));
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

			DOMNode div = app.nodeQuery("Div").name("WidgetTestPage-Div").getFirst().as(DOMNode.class);

			assertEquals(2, div.treeGetChildCount());

			DOMNode obj = null;

			for (final NodeInterface n: div.getAllChildNodes()){
				obj = n.as(DOMNode.class);
				break;
			}

			DOMNode clonedNode = obj;

			assertEquals(0, Iterables.toList(clonedNode.getChildren()).size());
			assertEquals(3, app.nodeQuery("Div").name("TestComponent").getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test37BuiltInTypesWithProperties() {

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
			folder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			// create test file with custom attributes
			app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.CONTENT_TYPE_PROPERTY),   "text/plain"),
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
	public void test38SharedComponentTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			try {

				final DOMNode newNode = page.createTemplate("#template");

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
	public void test39DynamicFileExport() {

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "ExtendedFile"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { StructrTraits.FILE })
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   node),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),         "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String")
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

			node.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			node.setProperty(test, "test");

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}
}
