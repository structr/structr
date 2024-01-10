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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class XPathTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(XPathTest.class.getName());

	@Test
	public void testSimpleXPath() {

		final String pageName	= "page-01";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement h1     = (DOMElement)page.createElement("h1");

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);
				title.appendChild(page.createTextNode("Test Page"));

				// add H1 element to BODY
				body.appendChild(h1);
				h1.appendChild(page.createTextNode("Page Title"));

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			assertEquals(html, page.getChildNodes().item(1));
			assertEquals(head, html.getChildNodes().item(0));
			assertEquals(body, html.getChildNodes().item(1));

			// test XPath support of structr nodes..
			XPathFactory factory            = XPathFactory.newInstance();
			XPath xpath                     = factory.newXPath();

			// let xpath cache first..
			assertEquals("Page Title", xpath.evaluate("/html/body/h1/text()", page, XPathConstants.STRING));
			assertEquals(h1, xpath.evaluate("/html/body/h1", page, XPathConstants.NODE));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");

		} catch (XPathExpressionException xpeex) {

			logger.warn("", xpeex);

			fail("Unexpected exception");

		}
	}

	@Test
	public void testXPathAttributes() {

		final String pageName	= "page-02";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement div    = (DOMElement)page.createElement("div");
			DOMElement p1     = (DOMElement)page.createElement("p");
			DOMElement p2     = (DOMElement)page.createElement("p");
			DOMElement p3     = (DOMElement)page.createElement("p");

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);
				title.appendChild(page.createTextNode("Test Page"));

				// add H1 element to BODY
				body.appendChild(div);

				div.appendChild(p1);
				div.appendChild(p2);
				div.appendChild(p3);

				// test
				p2.setAttribute("blah", "wurst");
				p3.setAttribute("index", "42");

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			// test XPath support of structr nodes..
			XPathFactory factory            = XPathFactory.newInstance();
			XPath xpath                     = factory.newXPath();

			// let xpath cache first..
			assertEquals(div, xpath.evaluate("/html/body/div", page, XPathConstants.NODE));
			assertEquals(p2, xpath.evaluate("/html/body/div/p[@blah='wurst']", page, XPathConstants.NODE));
			assertEquals(p3, xpath.evaluate("/html/body/div/p[@index=42]", page, XPathConstants.NODE));
			assertEquals(p3, xpath.evaluate("/html/body/div/p[@index>40]", page, XPathConstants.NODE));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");

		} catch (XPathExpressionException xpeex) {

			logger.warn("", xpeex);

			fail("Unexpected exception");

		}
	}
}
