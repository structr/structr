package org.structr.web.entity.dom;

import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */
public class PageTest extends StructrUiTest {

	public void testGetElementsByTagName() {

		final String pageName	= "page-01";

		try {

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);
			

			DOMNode html   = (DOMNode)page.createElement("html");
			DOMNode head   = (DOMNode)page.createElement("head");
			DOMNode body   = (DOMNode)page.createElement("body");
			DOMNode title  = (DOMNode)page.createElement("title");
			DOMNode h1     = (DOMNode)page.createElement("h1");
			DOMNode div1   = (DOMNode)page.createElement("div");
			DOMNode div2   = (DOMNode)page.createElement("div");
			DOMNode div3   = (DOMNode)page.createElement("div");

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);

				// add H1 element to BODY
				body.appendChild(h1);

				// add DIV element 1 to BODY
				body.appendChild(div1);
				
				// add DIV element 2 to DIV
				div1.appendChild(div2);
				
				// add DIV element 3 to DIV
				div2.appendChild(div3);

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			NodeList divs = page.getElementsByTagName("div");
			assertEquals(div1, divs.item(0));
			assertEquals(div2, divs.item(1));
			assertEquals(div3, divs.item(2));
			

		} catch (FrameworkException ex) {

			fail("Unexpected exception");

		}

	}

}
