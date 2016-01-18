/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import org.structr.web.common.StructrUiTest;

/**
 *
 *
 */
public class RelativeXPathTest extends StructrUiTest {

	public void testNothing() {}
	
	// FIXME: this test fails because of bad programming in DOM2DTM
	//        comparing object equality using ==. We cannot do anything
	//        about that except from trying to serve the exact same
	//        object to the xpath implementation, which is not going
	//        to happen soon, so this test is disabled until the xpath
	//        implementation is fixed.
	
	/*
		public void testRelativeXPath() {

		final String pageName	= "page-01";

		try (final Tx tx = app.tx()) {

			System.setProperty("javax.xml.xpath.XPathFactory:" + XPathConstants.DOM_OBJECT_MODEL, "org.apache.xpath.jaxp.XPathFactoryImpl");

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement h1     = (DOMElement)page.createElement("h1");

			assertTrue(h1 == h1);

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
//				html.appendChild(head);
				html.appendChild(body);

//				// add TITLE element to HEAD
//				head.appendChild(title);
//				title.appendChild(page.createTextNode("Test Page"));

				// add H1 element to BODY
				body.appendChild(h1);
				h1.appendChild(page.createTextNode("Page Title"));

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			// FIXME: this bug exists because the XPath implementation
			// compare objects using ==, and there are two h1 nodes
			// that refer to the same database entity..
			// we need to either implement request-based caching in
			// SuperUserSecurityContext or make DOMNode cache its
			// children etc..
			// test XPath support of structr nodes..
			XPathFactory factory            = XPathFactory.newInstance();
			XPath xpath                     = factory.newXPath();

			// let xpath cache first..
			assertEquals(body, xpath.evaluate("../.", h1, XPathConstants.NODE));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception");

		} catch (XPathExpressionException xpeex) {

			xpeex.printStackTrace();

			fail("Unexpected exception");

		}
	}

	public void scan(final Node node) {
		scan(node, 0);
	}

	private void scan(final Node node, final int depth) {

		if (node != null) {

			System.out.println(node);

			Node child = node.getFirstChild();

			while (child != null) {

				scan(child, depth + 1);

				child = child.getNextSibling();
			}
		}
	}
	*/
}
