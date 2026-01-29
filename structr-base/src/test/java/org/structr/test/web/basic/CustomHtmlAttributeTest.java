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
package org.structr.test.web.basic;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;


public class CustomHtmlAttributeTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(CustomHtmlAttributeTest.class.getName());

	@Test
	public void testCustomHtmlAttribute() {

		try (final Tx tx = app.tx()) {

			// create a page
			final Page newPage = Page.createNewPage(securityContext, "customAttributeTestPage");

			final DOMElement html  = createElement(newPage, newPage, "html");
			final DOMElement head  = createElement(newPage, html, "head");
			final DOMElement title = createElement(newPage, head, "title", "Test Page for custom html attributes");
			final DOMElement body  = createElement(newPage, html, "body");

			final PropertyKey<String> classKey = Traits.of("Div").key("_html_class");

			final DOMElement div1     = createElement(newPage, body, "div", "DIV with old-style data attribute");
			div1.setProperty(new GenericProperty<String>("data-test-attribute-old-style"), "old-style data attribute");

			final DOMElement div2     = createElement(newPage, body, "div", "DIV with new-style custom html attribute");
			div2.setProperty(new GenericProperty<String>("_custom_html_test-attribute-new-style"), "new-style custom attribute");

			final DOMElement div3     = createElement(newPage, body, "div", "DIV with data-attribute as new-style custom html attribute");
			div3.setProperty(new GenericProperty<String>("_custom_html_data-test-attribute-new-style"), "new-style custom data-attribute");

			final DOMElement div4     = createElement(newPage, body, "div", "DIV with empty string as (old-style) custom data-attribute and class attribute");
			div4.setProperty(new GenericProperty<String>("data-test-attribute-old-style"), "");
			div4.setProperty(classKey, "");

			final DOMElement div5     = createElement(newPage, body, "div", "DIV with empty string as (new-style) custom attribute and class attribute");
			div5.setProperty(new GenericProperty<String>("_custom_html_test-attribute-new-style"), "");
			div5.setProperty(classKey, "");

			final DOMElement div6     = createElement(newPage, body, "div", "DIV with empty string as (new-style) custom data-attribute and class attribute");
			div6.setProperty(new GenericProperty<String>("_custom_html_data-test-attribute-new-style"), "");
			div6.setProperty(classKey, "");


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
					"		<div class=\"\" data-test-attribute-old-style=\"\">DIV with empty string as (old-style) custom data-attribute and class attribute</div>\n" +
					"		<div class=\"\" test-attribute-new-style=\"\">DIV with empty string as (new-style) custom attribute and class attribute</div>\n" +
					"		<div class=\"\" data-test-attribute-new-style=\"\">DIV with empty string as (new-style) custom data-attribute and class attribute</div>\n" +
					"	</body>\n" +
					"</html>";

			assertEquals(expectedHtml, renderedHtml);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	private DOMElement createElement(final Page page, final DOMNode parent, final String tag, final String... content) throws FrameworkException {

		final DOMElement child = page.createElement(tag);
		parent.appendChild(child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final DOMNode node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
