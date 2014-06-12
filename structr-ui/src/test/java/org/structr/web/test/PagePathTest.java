/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import static junit.framework.TestCase.assertEquals;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.DOMException;

/**
 *
 * @author Axel Morgner
 */
public class PagePathTest extends StructrUiTest {
	
	public void testPagePath() {
		
		final String pageName	= "page-01";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement div    = (DOMElement)page.createElement("div");
			DOMElement div_2  = (DOMElement)page.createElement("div");
			DOMElement div_3  = (DOMElement)page.createElement("div");
			DOMElement h1     = (DOMElement)page.createElement("h1");
			DOMElement h1_2   = (DOMElement)page.createElement("h1");
			
			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);
				title.appendChild(page.createTextNode("Test Page"));
				
				// add DIVs to BODY
				body.appendChild(div);
				body.appendChild(div_2);
				body.appendChild(div_3);
				
				// add H1 elements to DIV
				div_3.appendChild(h1);
				div_3.appendChild(h1_2);
				h1.appendChild(page.createTextNode("Page Title"));
				
			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}
			
			assertEquals(html.getPositionPath(),	"/0");
			assertEquals(head.getPositionPath(),	"/0/0");
			assertEquals(title.getPositionPath(),	"/0/0/0");
			assertEquals(body.getPositionPath(),	"/0/1");
			assertEquals(div.getPositionPath(),	"/0/1/0");
			assertEquals(div_2.getPositionPath(),	"/0/1/1");
			assertEquals(div_3.getPositionPath(),	"/0/1/2");
			assertEquals(h1.getPositionPath(),	"/0/1/2/0");
			assertEquals(h1_2.getPositionPath(),	"/0/1/2/1");
			
			tx.success();
			
		} catch (FrameworkException fex) {

			fex.printStackTrace();
			
			fail("Unexpected exception");
			
		}
	}

}
