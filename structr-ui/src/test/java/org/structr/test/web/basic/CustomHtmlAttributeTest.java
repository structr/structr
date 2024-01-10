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
package org.structr.test.web.basic;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.*;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

import static org.testng.AssertJUnit.assertEquals;


public class CustomHtmlAttributeTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(CustomHtmlAttributeTest.class.getName());

	@Test
	public void testCustomHtmlAttribute() {

		try (final Tx tx = app.tx()) {

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "customAttributeTestPage");

			final Html html    = createElement(newPage, newPage, "html");
			final Head head    = createElement(newPage, html, "head");
			final Title title  = createElement(newPage, head, "title", "Test Page for custom html attributes");
			final Body body    = createElement(newPage, html, "body");

			final Div div1     = createElement(newPage, body, "div", "DIV with old-style data attribute");
			div1.setProperty(new GenericProperty<String>("data-test-attribute-old-style"), "old-style data attribute");

			final Div div2     = createElement(newPage, body, "div", "DIV with new-style custom html attribute");
			div2.setProperty(new GenericProperty<String>("_custom_html_test-attribute-new-style"), "new-style custom attribute");

			final Div div3     = createElement(newPage, body, "div", "DIV with data-attribute as new-style custom html attribute");
			div3.setProperty(new GenericProperty<String>("_custom_html_data-test-attribute-new-style"), "new-style custom data-attribute");


			final RenderContext renderContext = new RenderContext(securityContext);
			newPage.render(renderContext, 0);

			final String renderedHtml = StringUtils.join(renderContext.getBuffer().getQueue(), "");

			final String expectedHtml =
					"<!DOCTYPE html>\n" +
					"<html>\n" +
					"	<head>\n" +
					"		<title>Test Page for custom html attributes</title>\n" +
					"	</head>\n" +
					"	<body>\n" +
					"		<div data-test-attribute-old-style=\"old-style data attribute\">DIV with old-style data attribute</div>\n" +
					"		<div test-attribute-new-style=\"new-style custom attribute\">DIV with new-style custom html attribute</div>\n" +
					"		<div data-test-attribute-new-style=\"new-style custom data-attribute\">DIV with data-attribute as new-style custom html attribute</div>\n" +
					"	</body>\n" +
					"</html>";

			assertEquals(expectedHtml, renderedHtml);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

	}

	private <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Node node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
