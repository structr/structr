package org.structr.web.test;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */
public class XPathTest extends StructrUiTest {
	
	public void testXPath() {
		
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
			
			
		} catch (FrameworkException fex) {

			fex.printStackTrace();
			
			fail("Unexpected exception");
			
		} catch (XPathExpressionException xpeex) {

			xpeex.printStackTrace();
			
			fail("Unexpected exception");

		}
	}
}
