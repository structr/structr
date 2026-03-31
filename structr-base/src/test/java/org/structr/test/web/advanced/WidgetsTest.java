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
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.GetSuggestionsCommand;
import org.structr.websocket.command.ReplaceWidgetCommand;
import org.structr.websocket.message.WebSocketMessage;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class WidgetsTest extends StructrUiTest {

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
			assertFalse(iterator.hasNext());
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
}
