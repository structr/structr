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

import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataAdapterFieldTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.mock.MockServletRequest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.GetSuggestionsCommand;
import org.structr.websocket.command.ReplaceWidgetCommand;
import org.structr.websocket.message.WebSocketMessage;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class WidgetsTest extends DeploymentTestBase {

	@Test
	public void testSimpleReplacementRoundtrip() {

		/**
		 * This test builds a page structure based on the new widget system and checks
		 * if all replacement operations between widgets produce the correct results.
		 */

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			assertEquals("Invalid number of DOMNodes after Widget import.", 12, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a Panel with two children that are also widgets
			expandWidget(page, "Main Content", "Card");
			expandWidget(page, "Card", "Paragraph with static text");
			expandWidget(page, "Card", "Create Button");

			// verify that the content is as expected
			assertEquals("Main Content/+Page Heading/+Card/++p/+++Content[Lorem ]/++button/+++Content[Create]", getCheckString(getDOMNode(page, "Main Content")));

			// replace "Card" with "Card with heading"
			replaceElement(page, "Card", "Card with heading", Map.of());

			// verify that the content is as expected after the replacement
			assertEquals("Main Content/+Page Heading/+Card with heading/++h3/+++Content[Headin]/++div/+++p/++++Content[Lorem ]/+++button/++++Content[Create]", getCheckString(getDOMNode(page, "Main Content")));

			// replace "Card with heading" with "Accordion"
			replaceElement(page, "Card with heading", "Accordion", Map.of());

			// verify that the content is as expected after the replacement
			assertEquals("Main Content/+Page Heading/+Accordion/++details/+++summary/++++Content[Headin]/+++div/++++p/+++++Content[Lorem ]/++++button/+++++Content[Create]", getCheckString(getDOMNode(page, "Main Content")));

			// replace "Accordion" with "Card with heading" again
			replaceElement(page, "Accordion", "Card with heading", Map.of());

			// verify that the content is as expected after the replacement
			assertEquals("Main Content/+Page Heading/+Card with heading/++h3/+++Content[Headin]/++div/+++p/++++Content[Lorem ]/+++button/++++Content[Create]", getCheckString(getDOMNode(page, "Main Content")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuggestedWidgetsForInsert() {

		/**
		 * This test builds a page structure based on the new widget system and checks
		 * if the suggestions based on componentType and selectors are correct.
		 */

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert some widgets
			expandWidget(page, "Main Content", "Card");
			expandWidget(page, "Main Content", "Grid");
			expandWidget(page, "Main Content", "Accordion");


			// check suggestions for inserting into "Main Content" (componentType "canvas")
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Main Content", "insert");

				assertWidgetResult(message, true, "Accordion", "Card", "Card with heading", "Centered Card", "Create Form", "Edit Form", "Gallery", "List", "Table", "Grid", "Page Heading");
			}

			// check suggestions for inserting into "Card" (componentType "container")
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Card", "insert");

				assertWidgetResult(message, true, "Action Button", "Create Button", "Delete Button", "Checkbox", "Login Form", "Textarea", "Textfield", "Grid", "Badge", "Heading", "Label", "Paragraph with static text");
			}

			// check suggestions for inserting into "Grid" (componentType "container")
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Grid", "insert");

				assertWidgetResult(message, true, "Accordion", "Card", "Card with heading", "Centered Card", "Create Form", "Edit Form", "Gallery", "List", "Table", "Grid", "Page Heading");
			}



			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuggestedWidgetsForWrap() {

		/**
		 * This test builds a page structure based on the new widget system and checks
		 * if the suggestions based on componentType and selectors are correct.
		 */

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert some widgets
			expandWidget(page, "Main Content", "Card");
			expandWidget(page, "Main Content", "Grid");
			expandWidget(page, "Main Content", "Accordion");


			// check suggestions for wrapping the "Card" widget (can only be put in "canvas" elements)
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Card", "wrap");

				assertWidgetResult(message, true, "Grid");
			}

			// check suggestions for wrapping the "Grid" widget (grid can be put in components)
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Grid", "wrap");

				assertWidgetResult(message, true, "Card", "Card with heading", "Centered Card", "Grid");
			}

			// check suggestions for wrapping the "Card" widget
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Accordion", "wrap");

				assertWidgetResult(message, true, "Grid");
			}


			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuggestedWidgetsForReplace() {

		/**
		 * This test builds a page structure based on the new widget system and checks
		 * if the suggestions based on componentType and selectors are correct.
		 */

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert some widgets
			expandWidget(page, "Main Content", "Card");
			expandWidget(page, "Main Content", "Grid");
			expandWidget(page, "Main Content", "Accordion");


			// check suggestions for wrapping the "Card" widget (can only be put in "canvas" elements)
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Card", "replace");

				assertWidgetResult(message, true, "Accordion", "Card", "Card with heading", "Centered Card", "Create Form", "Edit Form", "Gallery", "List", "Table", "Grid", "Page Heading");
			}

			// check suggestions for wrapping the "Grid" widget (grid can be put in components)
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Grid", "replace");

				assertWidgetResult(message, true, "Accordion", "Card", "Card with heading", "Centered Card", "Create Form", "Edit Form", "Gallery", "List", "Table", "Grid", "Page Heading");
			}

			// check suggestions for wrapping the "Card" widget
			{
				final WebSocketMessage message = fetchSuggestionsFor(page, "Accordion", "replace");

				assertWidgetResult(message, true, "Accordion", "Card", "Card with heading", "Centered Card", "Create Form", "Edit Form", "Gallery", "List", "Table", "Grid", "Page Heading");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testDeploymentRoundtrip() {

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert some widgets
			expandWidget(page, "Main Content", "Card");
			expandWidget(page, "Main Content", "Grid");
			expandWidget(page, "Main Content", "Accordion");
			expandWidget(page, "Main Content", "Table");
			expandWidget(page, "Main Content", "List");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void testDataDrivenList() {

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();

			schema.addType("Project");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert List widget
			expandWidget(page, "Main Content", "List");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final Page page                     = app.nodeQuery(StructrTraits.PAGE).name("test01").getFirst().as(Page.class);
			final DOMNode listComponent         = getDOMNode(page, "List");
			final ComponentConfiguration config = listComponent.getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			// assert that the component already has a data adapter
			assertNotNull(config.getDataAdapter(), "Component did not create its own DataAdapter automatically.");
			assertNotNull(config.getDataSource(), "Component did not accept SchemaNode as a data source.");

			final Traits fieldTraits = Traits.of(StructrTraits.DATA_ADAPTER_FIELD);

			// data adapter needs a field for name that enables filtering
			app.create(StructrTraits.DATA_ADAPTER_FIELD,
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), config.getDataAdapter()),
				new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name"),
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.IS_SEARCHABLE_PROPERTY), true)
			);

			// create some projects
			for (int i=1; i<10; i++) {
				app.create("Project", "Project #0" + i);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("test01").getFirst().as(Page.class);
			final DOMNode listComponent = getDOMNode(page, "List");


			// test1: check list component attributes and content with a single page
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of());

				// check HTML of component shell
				final Element componentElement = (Element) parsedOutput.getFirst();
				assertAttributes(componentElement, Map.of("data-structr-id", listComponent.getUuid(), "data-channel", "Project"));

				// check HTML of list container
				final Element divElement = componentElement.children().getFirst();
				assertAttributes(divElement, Map.of("class", "list-container"));

				// check HTML of ul element
				final Element listElement = divElement.children().getFirst();
				assertAttributes(listElement, Map.of("role", "list"));

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #01", "Project #02", "Project #03", "Project #04", "Project #05", "Project #06", "Project #07", "Project #08", "Project #09", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForSinglePage(paginationDiv);
			}

			// test2: check sort order descending
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of("project.sort", "name>"));

				final Element componentElement = (Element) parsedOutput.getFirst();
				final Element divElement       = componentElement.children().getFirst();
				final Element listElement      = divElement.children().getFirst();

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #09", "Project #08", "Project #07", "Project #06", "Project #05", "Project #04", "Project #03", "Project #02", "Project #01", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForSinglePage(paginationDiv);
			}

			// test3: check sort order ascending
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of("project.sort", "name<"));

				final Element componentElement = (Element) parsedOutput.getFirst();
				final Element divElement       = componentElement.children().getFirst();
				final Element listElement      = divElement.children().getFirst();

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #01", "Project #02", "Project #03", "Project #04", "Project #05", "Project #06", "Project #07", "Project #08", "Project #09", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForSinglePage(paginationDiv);
			}

			// test4: check filtering
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of("project.filter", "07"));

				final Element componentElement = (Element) parsedOutput.getFirst();
				final Element divElement       = componentElement.children().getFirst();
				final Element listElement      = divElement.children().getFirst();

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #07", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForSinglePage(paginationDiv);
			}

			// create some more projects to test pagination
			for (int i=10; i<100; i++) {
				app.create("Project", "Project #" + i);
			}

			// test5: check pagination with 10 pages
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of("project.sort", "name<"));

				final Element componentElement = (Element) parsedOutput.getFirst();
				final Element divElement       = componentElement.children().getFirst();
				final Element listElement      = divElement.children().getFirst();

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #01", "Project #02", "Project #03", "Project #04", "Project #05", "Project #06", "Project #07", "Project #08", "Project #09", "Project #10", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForMultiplePages(paginationDiv);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check deployment roundtrip
		compare(calculateHash(), true);


		try (final Tx tx = app.tx()) {

			// create some projects
			for (int i=1; i<10; i++) {
				app.create("Project", "Project #0" + i);
			}

			// check pagination again after deployment roundtrip
			{
				final List<Node> parsedOutput = fetchAndParseHTML("test01", "List", Map.of("project.sort", "name<"));

				final Element componentElement = (Element) parsedOutput.getFirst();
				final Element divElement       = componentElement.children().getFirst();
				final Element listElement      = divElement.children().getFirst();

				// check HTML of li elements
				final List<String> expectedItemsAscending = List.of("Project #01", "Project #02", "Project #03", "Project #04", "Project #05", "Project #06", "Project #07", "Project #08", "Project #09", "No items");
				int index = 0;

				for (final Element li : listElement.children()) {
					assertEquals("List item text has wrong value", expectedItemsAscending.get(index++), li.text());
				}

				// check HTML of pagination buttons
				final Element paginationDiv = divElement.children().get(1);
				assertPaginationControlsForSinglePage(paginationDiv);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testDataDrivenTable() {

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType type     = schema.addType("Project");

			type.addStringProperty("description");
			type.addDateProperty("dueDate");
			type.addBooleanProperty("done");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a page template
			expandWidget(page, page, "Default Page");

			// insert List widget
			expandWidget(page, "Main Content", "Table");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Page page                     = app.nodeQuery(StructrTraits.PAGE).name("test01").getFirst().as(Page.class);
			final DOMNode listComponent         = getDOMNode(page, "Table");
			final ComponentConfiguration config = listComponent.getComponentConfiguration();

			config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY), "node:Project");

			// assert that the component already has a data adapter
			assertNotNull(config.getDataAdapter(), "Component did not create its own DataAdapter automatically.");
			assertNotNull(config.getDataSource(), "Component did not accept SchemaNode as a data source.");

			final Traits fieldTraits = Traits.of(StructrTraits.DATA_ADAPTER_FIELD);

			// data adapter for table needs fields
			app.create(StructrTraits.DATA_ADAPTER_FIELD,
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), config.getDataAdapter()),
				new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name"),
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.IS_SEARCHABLE_PROPERTY), true)
			);

			app.create(StructrTraits.DATA_ADAPTER_FIELD,
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), config.getDataAdapter()),
				new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "description")
			);

			app.create(StructrTraits.DATA_ADAPTER_FIELD,
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), config.getDataAdapter()),
				new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "dueDate"),
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.RENDER_TEMPLATE_PROPERTY), "formatted-date"),
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.CONFIG_PROPERTY), "{ \"dateFormat\": \"dd.MM.yyyy\"}")
			);

			app.create(StructrTraits.DATA_ADAPTER_FIELD,
				new NodeAttribute<>(fieldTraits.key(DataAdapterFieldTraitDefinition.DATA_ADAPTER_PROPERTY), config.getDataAdapter()),
				new NodeAttribute<>(fieldTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "done")
			);

			// add fields to fieldSet
			config.setFieldSet("name,description,dueDate,done");

			// create some projects
			for (int i=1; i<6; i++) {

				final NodeInterface project = app.create("Project");
				final Traits traits         = project.getTraits();
				final Date dueDate          = new Date(i);

				dueDate.setYear(126);
				dueDate.setMonth(i);
				dueDate.setDate(1);

				project.setProperty(traits.key("name"),        "Project #0" + i);
				project.setProperty(traits.key("description"), "Description of Project #0" + i);
				project.setProperty(traits.key("dueDate"),     dueDate);
				project.setProperty(traits.key("done"),        i % 2 == 0);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(StructrTraits.PAGE).name("test01").getFirst().as(Page.class);
			final DOMNode tableComponent  = getDOMNode(page, "Table");
			final List<Node> parsedOutput = fetchAndParseHTML("test01", "Table", Map.of());

			// check HTML of component shell
			final Element componentElement = (Element) parsedOutput.getFirst();
			assertAttributes(componentElement, Map.of("data-structr-id", tableComponent.getUuid(), "data-channel", "Project"));

			// check HTML of list container
			final Element divElement = componentElement.children().getFirst();
			assertAttributes(divElement, Map.of("class", "table"));

			// check HTML of ul element
			final Element tableElement = divElement.children().get(1);
			final Element trhead        = tableElement.children().getFirst().children().getFirst();

			final List<String> expectedItemsAscending = List.of("Name", "Description", "Due Date", "Done");
			int index = 0;

			for (final Element th : trhead.children()) {
				assertEquals("Table header cell text has wrong value", expectedItemsAscending.get(index++), th.text());
			}

			final Element tbody = tableElement.children().get(1);
			index = 1;

			for (final Element tr : tbody.children()) {

				final Element nameCell = tr.children().get(0);
				final Element descCell = tr.children().get(1);
				final Element dateCell = tr.children().get(2);
				final Element doneCell = tr.children().get(3);

				assertEquals("Table cell 'name' has wrong value", "Project #0" + index, nameCell.text());
				assertEquals("Table cell 'description' has wrong value", "Description of Project #0" + index, descCell.text());
				assertEquals("Table cell 'dueDate' has wrong value", "01.0" + (index + 1) + ".2026", dateCell.text());
				assertEquals("Table cell 'done' has wrong value", Boolean.toString(index % 2 == 0), doneCell.text());

				index++;
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check deployment roundtrip
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			// create some projects
			for (int i=1; i<10; i++) {
				app.create("Project", "Project #0" + i);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	void replaceElement(final Page page, String replaceName, final String name, final Map<String, Object> values) throws FrameworkException {

		final Widget widget            = StructrApp.getInstance().nodeQuery(StructrTraits.WIDGET).name(name).getFirst().as(Widget.class);
		final DOMNode nodeToReplace    = getDOMNode(page, replaceName);
		final Map<String, Object> data = prepareData(widget);

		data.putAll(values);

		ReplaceWidgetCommand.replaceWidget(securityContext, page, nodeToReplace, null, data, false);
	}

	private void expandWidget(final Page page, final String targetElement, final String widgetName) throws FrameworkException {
		expandWidget(page, getDOMNode(page, targetElement), widgetName);
	}

	private void expandWidget(final Page page, final DOMNode targetElement, final String widgetName) throws FrameworkException {

		final Widget widget            = StructrApp.getInstance().nodeQuery(StructrTraits.WIDGET).name(widgetName).getFirst().as(Widget.class);
		final Map<String, Object> data = prepareData(widget);

		// create temporary parent for Widget to expand in
		final DOMNode tmpParent = page.createElement("div");

		Widget.expandWidget(securityContext, page, tmpParent, null, data, false);

		// move children
		for (final DOMNode newNode : tmpParent.getChildren()) {
			targetElement.appendChild(newNode);
		}

		// remove temporary parent
		StructrApp.getInstance(securityContext).delete(tmpParent);
	}

	private Map<String, Object> prepareData(final Widget widget) {

		final Map<String, Object> parameters = new LinkedHashMap<>();
		final JsonSingleInput singleInput    = new JsonSingleInput();
		final JsonInput input                = new JsonInput();

		singleInput.add(input);

		parameters.put("source", widget.getSource());
		parameters.put("config", singleInput);

		if (widget.getComponentType() != null) {
			input.put("componentType", widget.getComponentType());
		}

		if (widget.getDimensions() != null) {
			input.put("dimensions", widget.getDimensions());
		}

		return parameters;
	}

	private DOMNode getDOMNode(final Page page, final String name) throws FrameworkException {

		for (final NodeInterface node : page.getAllChildNodes()) {

			if (node.getName().equals(name)) {

				return node.as(DOMNode.class);
			}
		}

		return null;
	}

	private DOMNode getContentNode(final Page page, final String content) throws FrameworkException {

		for (final NodeInterface node : page.getAllChildNodes()) {

			if (node.is(StructrTraits.CONTENT)) {

				if (node.as(Content.class).getContent().equals(content)) {
					return node.as(DOMNode.class);
				}
			}
		}

		return null;
	}

	private String getCheckString(final DOMNode node) {

		final List<String> buf = new LinkedList<>();

		getCheckString(buf, node, 0);

		return StringUtils.join(buf, "/");
	}

	private void getCheckString(final List<String> buf, final DOMNode node, final int level) {

		buf.add(StringUtils.repeat("+", level) + ReplaceWidgetCommand.nameOrTag(node));

		for (final DOMNode child : node.getChildren()) {
			getCheckString(buf, child, level + 1);
		}
	}

	private void setupUserAndWidgets() {

		createAdminUser();

		// import widgets
		RestAssured.basePath = "/";
		RestAssured
			.given()
			.header("x-user", "admin")
			.header("x-password", "admin")
			.request()
			.multiPart("file", "widgets.zip", WidgetsTest.class.getResourceAsStream("/test/widgets.zip"))
			.multiPart("mode", "data")
			.expect()
			.statusCode(200)
			.when()
			.post("/structr/deploy");

		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> widgets = app.nodeQuery(StructrTraits.WIDGET).getAsList();

			assertEquals("Invalid number of imported Widgets", 36, widgets.size());

			// print names of all Widgets
			System.out.println(widgets.stream().map(NodeInterface::getName).toList());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	WebSocketMessage mockWebSocketMessage() {

		final WebSocketMessage message = new WebSocketMessage();

		return message;
	}

	private GetSuggestionsCommand mockGetSuggestionsCommand() {

		final GetSuggestionsCommand getSuggestionsCommand = new GetSuggestionsCommand();
		final StructrWebSocket webSocket                  = new StructrWebSocket(null, null, null) {

			@Override
			public SecurityContext getSecurityContext() {
				return securityContext;
			}

			@Override
			public void send(final WebSocketMessage webSocketData, final boolean flush) {
			}
		};

		getSuggestionsCommand.setWebSocket(webSocket);

		return getSuggestionsCommand;
	}

	private void assertWidgetResult(final WebSocketMessage data, final boolean exact, final String... expectedNames) {

		final Iterable<NodeInterface> result   = (Iterable) data.getResult();
		final Iterator<NodeInterface> iterator = result.iterator();
		final int count                        = expectedNames.length;
		int num                                = 0;

		for (final String expectedName : expectedNames) {

			assertTrue("Widget result has too few elements, expected " + count + " but got " + num + ".", iterator.hasNext());

			final NodeInterface next = iterator.next();

			assertEquals("Unexpected widget in result, ", expectedName, next.getName());

			num++;
		}

		// verify that there are no more results
		if (exact) {
			Assert.assertFalse(iterator.hasNext());
		}
	}

	private WebSocketMessage fetchSuggestionsFor(final Page page, final String domNodeName, final String mode) throws FrameworkException {

		final GetSuggestionsCommand getSuggestionsCommand = mockGetSuggestionsCommand();
		final WebSocketMessage message                    = mockWebSocketMessage();
		final DOMNode mainContent                         = getDOMNode(page, domNodeName);

		message.setId(mainContent.getUuid());
		message.getNodeData().put("mode", mode);

		getSuggestionsCommand.processMessage(message);

		return message;
	}

	private void assertAttributes(final Element element, final Map<String, Object> expectedKeyValues) {

		for (final Map.Entry<String, Object> entry : expectedKeyValues.entrySet()) {

			final String key = entry.getKey();

			assertEquals("Component attribute " + key + " has wrong value", entry.getValue(), element.attr(key));
		}
	}

	private void assertPaginationControlsForSinglePage(final Element paginationDiv) {

		final Element prevButton    = paginationDiv.children().get(0);
		final Element windowButtons = paginationDiv.children().get(1);
		final Element nextButton    = paginationDiv.children().get(2);

		final Element firstPageButton    = windowButtons.children().get(0);
		final Element lowEllipsisButton  = windowButtons.children().get(1);
		final Element windowButton1      = windowButtons.children().get(2);
		final Element windowButton2      = windowButtons.children().get(3);
		final Element windowButton3      = windowButtons.children().get(4);
		final Element windowButton4      = windowButtons.children().get(5);
		final Element windowButton5      = windowButtons.children().get(6);
		final Element highEllipsisButton = windowButtons.children().get(7);
		final Element lastPageButton     = windowButtons.children().get(8);

		// all other buttons are hidden
		assertAttributes(prevButton, Map.of("disabled", "true"));
		assertAttributes(firstPageButton, Map.of("data-hidden", "true"));
		assertAttributes(lowEllipsisButton, Map.of("data-hidden", "true"));
		assertAttributes(windowButton1, Map.of("data-hidden", "true"));
		assertAttributes(windowButton2, Map.of("data-hidden", "true"));

		assertAttributes(windowButton3, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "1",
			"data-current-page", "1"
		));

		// all other buttons are hidden
		assertAttributes(windowButton4, Map.of("data-hidden", "true"));
		assertAttributes(windowButton5, Map.of("data-hidden", "true"));
		assertAttributes(highEllipsisButton, Map.of("data-hidden", "true"));
		assertAttributes(lastPageButton, Map.of("data-hidden", "true"));
		assertAttributes(nextButton, Map.of("disabled", "true"));
	}

	private void assertPaginationControlsForMultiplePages(final Element paginationDiv) {

		final Element prevButton    = paginationDiv.children().get(0);
		final Element windowButtons = paginationDiv.children().get(1);
		final Element nextButton    = paginationDiv.children().get(2);

		final Element firstPageButton    = windowButtons.children().get(0);
		final Element lowEllipsisButton  = windowButtons.children().get(1);
		final Element windowButton1      = windowButtons.children().get(2);
		final Element windowButton2      = windowButtons.children().get(3);
		final Element windowButton3      = windowButtons.children().get(4);
		final Element windowButton4      = windowButtons.children().get(5);
		final Element windowButton5      = windowButtons.children().get(6);
		final Element highEllipsisButton = windowButtons.children().get(7);
		final Element lastPageButton     = windowButtons.children().get(8);

		// all other buttons are hidden
		assertAttributes(prevButton, Map.of("disabled", "true"));
		assertAttributes(firstPageButton, Map.of("data-hidden", "true"));
		assertAttributes(lowEllipsisButton, Map.of("data-hidden", "true"));
		assertAttributes(windowButton1, Map.of("data-hidden", "true"));
		assertAttributes(windowButton2, Map.of("data-hidden", "true"));

		assertAttributes(windowButton3, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "1",
			"data-current-page", "1"
		));

		assertAttributes(windowButton4, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "2"
		));

		assertAttributes(windowButton5, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "3"
		));

		// all other buttons are hidden
		assertAttributes(highEllipsisButton, Map.of("disabled", "true"));

		assertAttributes(lastPageButton, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "10"
		));

		assertAttributes(nextButton, Map.of(
			"data-structr-success-target", "[data-channel~='Project']",
			"data-structr-events", "click",
			"data-structr-target", "project.page",
			"data-project.page", "2"
		));
	}

	private List<Node> fetchAndParseHTML(final String pageName, final String componentName, final Map<String, String> parameters) throws FrameworkException {

		final Page page               = app.nodeQuery(StructrTraits.PAGE).name(pageName).getFirst().as(Page.class);
		final DOMNode listComponent   = getDOMNode(page, componentName);

		final HttpServletRequest mockRequest = new MockServletRequest() {
			@Override
			public String getParameter(final String name) {
				return parameters.get(name);
			}
			@Override
			public String[] getParameterValues(final String name) {
				return new String[] { parameters.get(name) };
			}
		};

		final RenderContext ctx = new RenderContext(securityContext, mockRequest, null, RenderContext.EditMode.NONE);

		listComponent.render(ctx, 0);

		final String componentOutput = StringUtils.join(ctx.getBuffer().getQueue(), "");

		System.out.println(componentOutput);

		return Parser.parseXmlFragment(componentOutput, "http://localhost");
	}
}
