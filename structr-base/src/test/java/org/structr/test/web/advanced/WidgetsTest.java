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
import org.structr.websocket.command.ReplaceWidgetCommand;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

public class WidgetsTest extends StructrUiTest {

	@Test
	public void testWidgets01() {

		/**
		 * This test builds a page structure based on the new widget system and checks
		 * if all replacement operations between widgets produce the correct results.
		 */

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			expandWidget(page, page, "Main Page Template");

			assertEquals("Invalid number of DOMNodes after Widget import.", 12, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a Panel with two children that are also widgets
			expandWidget(page, "Main Content", "Panel");
			expandWidget(page, "Panel", "Paragraph");
			expandWidget(page, "Panel", "Button");

			// check initial result
			assertEquals("Main Content/+Page Heading/+Panel/++p/+++Content[Lorem ]/++button/+++Content[Button]", getCheckString(getDOMNode(page, "Main Content")));

			// check result after replacement
			replaceElement(page, "Panel", "Panel with heading", Map.of("heading", "Panel Header"));
			assertEquals("Main Content/+Page Heading/+Panel Header/++p/+++Content[Lorem ]/++button/+++Content[Button]", getCheckString(getDOMNode(page, "Main Content")));

			// check result after replacement
			replaceElement(page, "Panel Header", "Grid", Map.of("columns", 2));
			assertEquals("Main Content/+Page Heading/+Grid with 2 columns/++p/+++Content[Lorem ]/++button/+++Content[Button]", getCheckString(getDOMNode(page, "Main Content")));

			// check result after replacement round trip
			replaceElement(page, "Grid with 2 columns", "Panel with heading", Map.of("heading", "Panel Header"));
			assertEquals("Main Content/+Page Heading/+Panel Header/++p/+++Content[Lorem ]/++button/+++Content[Button]", getCheckString(getDOMNode(page, "Main Content")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testWidgets02() {

		setupUserAndWidgets();

		try (final Tx tx = app.tx()) {

			assertEquals("No DOMNodes should exist prior to test.", 0, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			final Page page = Page.createNewPage(securityContext, "test01");

			assertEquals("Only one DOMNode should exist after creating a page.", 1, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			expandWidget(page, page, "Main Page Template");

			assertEquals("Invalid number of DOMNodes after Widget import.", 12, app.nodeQuery(StructrTraits.DOM_NODE).getAsList().size());

			// import a Panel with two children that are also widgets
			expandWidget(page, "Main Content", "Panel");
			expandWidget(page, "Panel", "Table");

			// remove placeholder nodes
			app.delete(getContentNode(page, "Name"));
			app.delete(getContentNode(page, "Hello"));

			// insert data
			expandWidget(page, "Table Header Cell", "Badge");
			expandWidget(page, "Table Cell", "Paragraph");
			expandWidget(page, "Table Cell", "List");

			final String checkStringBefore = getCheckString(getDOMNode(page, "Main Content"));

			ReplaceWidgetCommand.print(getDOMNode(page, "Main Content"), 0);

			replaceElement(page, "Table", "Table", Map.of());

			final String checkStringAfter = getCheckString(getDOMNode(page, "Main Content"));

			System.out.println(checkStringBefore);
			System.out.println(checkStringAfter);

			ReplaceWidgetCommand.print(getDOMNode(page, "Main Content"), 0);

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

	private void expandWidget(final Page page, final String parent, final String name) throws FrameworkException {
		expandWidget(page, getDOMNode(page, parent), name);
	}

	private void expandWidget(final Page page, final DOMNode parent, final String name) throws FrameworkException {

		final Widget widget            = StructrApp.getInstance().nodeQuery(StructrTraits.WIDGET).name(name).getFirst().as(Widget.class);
		final Map<String, Object> data = prepareData(widget);

		Widget.expandWidget(securityContext, page, parent, null, data, false);
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

			assertEquals("Invalid number of imported Widgets", 17, widgets.size());

			// print names of all Widgets
			System.out.println(widgets.stream().map(NodeInterface::getName).toList());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}
}
