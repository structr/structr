package org.structr.web.test;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
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
			XPathExpression xpathExpression = xpath.compile("/html/body/h1/text()");
		
			// let xpath cache first..
			//assertEquals("Dies ist ein Test", xpath.evaluate("/html/body/h1/text()", page, XPathConstants.STRING));
			System.out.println("#################################################################################################################################");
			System.out.println("#############################: " + xpathExpression.evaluate(page));
			
			printDocument(page);
			System.out.println("#################################################################################################################################");

			page.setDebugging(true);
			
//			assertEquals("Dies ist ein Test", xpath.evaluate("/html/body/h1/text()", page, XPathConstants.STRING));

			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("/home/chrisi/test.html"));

				System.out.println("#################################################################################################################################");
				System.out.println("#############################: " + xpathExpression.evaluate(doc));
				
				printDocument(doc);
				
				System.out.println("#################################################################################################################################");
				
				
				
				
			} catch (Throwable t) {
				
				t.printStackTrace();
			}
			
			
			
			
			
		} catch (FrameworkException fex) {

			fex.printStackTrace();
			
			fail("Unexpected exception");
			
		} catch (XPathExpressionException xpeex) {

			xpeex.printStackTrace();
			
			fail("Unexpected exception");

		}
	}
	
	private void printDocument(Document doc) {
		
		StringBuilder buf = new StringBuilder();
		
		buf.append(outputNode(doc.getFirstChild()));
		buf.append("\n");
		buf.append(outputNode(doc.getDocumentElement()));
		buf.append("\n");
		buf.append(outputNode(doc.getChildNodes().item(0)));
		buf.append("\n");
		buf.append(outputNode(doc.getChildNodes().item(1)));
		buf.append("\n");

		System.out.println(buf.toString());
		
		printNode(doc, 0);
	}
	
	private void printNode(Node node, int depth) {
	
		for (int i=0; i<depth; i++) {
			System.out.print("    ");
		}
		
		System.out.println(node.getNodeName() + " (" + node.getNodeType() + ") " + node.getLocalName() + ", " + node.getPrefix());
		
		NodeList children = node.getChildNodes();
		if (children != null) {
			
			int len = children.getLength();
			for (int i=0; i<len; i++) {
				printNode(children.item(i), depth+1);
			}
		}
	}
	
	private String outputNode(Node node) {
		
		StringBuilder buf = new StringBuilder();
		
		if (node != null)  {
			buf.append(node.getNodeName());
			buf.append(" (");
			buf.append(node.getNodeType());
			buf.append(")");
		}
		
		return buf.toString();
	}
}
