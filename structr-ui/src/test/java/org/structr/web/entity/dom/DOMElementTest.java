/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.util.List;
import java.util.Locale;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.LinkedListNode;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.DOMTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.DataNode;
import org.w3c.dom.Element;

/**
 *
 * @author Christian Morgner
 */
public class DOMElementTest extends DOMTest {

//	public void testGetTagName() {
//		
//		Document doc = getDocument();
//		
//		Element elem = doc.createElement("div");
//		
//		assertEquals("div", elem.getTagName());
//	}
//	
//	public void testAttributeMethods() {
//		
//		Document doc = getDocument();
//		
//		Element elem = doc.createElement("div");
//
//		String name1  = "test1";
//		String name2  = "test2";
//		String name3  = "test3";
//
//		String value1 = "value1";
//		String value2 = "value2";
//		String value3 = "value3";
//		
//		elem.setAttribute(name1, value1);
//		elem.setAttribute(name2, value2);
//		elem.setAttribute(name3, value3);
//		
//		assertEquals(true, elem.hasAttributes());
//		assertEquals(3, elem.getAttributes().getLength());
//		
//		assertEquals(value1, elem.getAttribute(name1));
//		assertEquals(value2, elem.getAttribute(name2));
//		assertEquals(value3, elem.getAttribute(name3));
//		
//		
//	}
//	
//	public void testAttributeNodeMethods() {
//		
//		Document doc = getDocument();
//		
//		Element elem = doc.createElement("div");
//
//		String name1  = "test1";
//		String name2  = "test2";
//		String name3  = "test3";
//
//		String value1 = "value1";
//		String value2 = "value2";
//		String value3 = "value3";
//		
//		DOMAttribute attr1 = (DOMAttribute)doc.createAttribute(name1);
//		assertNotNull(attr1);
//		
//		attr1.setValue(value1);
//		elem.setAttributeNode(attr1);
//		assertEquals(elem, attr1.getParentNode());
//		
//		DOMAttribute attr2 = (DOMAttribute)doc.createAttribute(name2);
//		assertNotNull(attr2);
//		
//		attr2.setValue(value2);
//		elem.setAttributeNode(attr2);
//		assertEquals(elem, attr2.getParentNode());
//		
//		DOMAttribute attr3 = (DOMAttribute)doc.createAttribute(name3);
//		assertNotNull(attr3);
//
//		attr3.setValue(value3);
//		elem.setAttributeNode(attr3);
//		assertEquals(elem, attr3.getParentNode());
//
//		assertEquals(true, elem.hasAttributes());
//		assertEquals(3, elem.getAttributes().getLength());
//		
//		assertEquals(value1, elem.getAttribute(name1));
//		assertEquals(attr1, elem.getAttributeNode(name1));
//		
//		assertEquals(value2, elem.getAttribute(name2));
//		assertEquals(attr2, elem.getAttributeNode(name2));
//		
//		assertEquals(value3, elem.getAttribute(name3));
//		assertEquals(attr3, elem.getAttributeNode(name3));
//		
//		// important for xpath: sibling methods
//		assertEquals(attr2, attr1.getNextSibling());
//		assertEquals(attr3, attr2.getNextSibling());
//		assertEquals(attr2, attr3.getPreviousSibling());
//		assertEquals(attr1, attr2.getPreviousSibling());
//	}
//	
//	public void testRenderTree() {
//		
//		
//		try {
//			
//			final List<DataNode> dataNodes = this.createTestNodes(DataNode.class, 7);
//			final Page doc                 = (Page)getDocument();
//			final String key               = "TEST";
//			
//			assertEquals(7, dataNodes.size());
//			
//			DataNode rootNode = dataNodes.get(0);
//			DataNode nodeA    = dataNodes.get(1);
//			DataNode nodeB    = dataNodes.get(2);
//			DataNode nodeC    = dataNodes.get(3);
//			DataNode nodeD    = dataNodes.get(4);
//			DataNode nodeE    = dataNodes.get(5);
//			DataNode nodeF    = dataNodes.get(6);
//			
//			rootNode.appendChild(key, nodeA);
//			rootNode.appendChild(key, nodeB);
//			
//			nodeA.appendChild(key, nodeC);
//			nodeA.appendChild(key, nodeD);
//			
//			nodeB.appendChild(key, nodeE);
//			nodeB.appendChild(key, nodeF);
//
//			
//			
//			// create dom tree
//			Element html = doc.createElement("html");
//			Element body = doc.createElement("body");
//			Element div  = doc.createElement("div");
//			Element ul1  = doc.createElement("ul");
//			Element li1  = doc.createElement("li");
//			Element p1   = doc.createElement("p");
//			p1.appendChild(doc.createTextNode("${data.name}"));
//			Element ul2  = doc.createElement("ul");
//			Element li2  = doc.createElement("li");
//			Element p2   = doc.createElement("p");
//			p2.appendChild(doc.createTextNode("${data.name}"));
//			
//			
//			// create RENDER_TREE relationship between div and rootNode
//			PropertyMap properties = new PropertyMap();
//			properties.put(LinkedListNode.keyProperty, key);
//			Services.command(securityContext, CreateRelationshipCommand.class).execute((DOMElement)div, rootNode, RelType.RENDER_TREE, properties, false);
//			
//			
//			doc.appendChild(html);
//			html.appendChild(body);
//			body.appendChild(div);
//			div.appendChild(ul1);
//			ul1.appendChild(li1);
//			li1.appendChild(p1);
//			li1.appendChild(ul2);
//			ul2.appendChild(li2);
//			li2.appendChild(p2);
//			
//			
//			RenderContext ctx = new RenderContext(null, null, false, Locale.GERMAN);
//			doc.render(securityContext, ctx, 0);
//			
//			System.out.println(ctx.getBuffer().toString());
//			
//			
//			
//		} catch (FrameworkException fex) {
//			
//			fail("unexpected exception");
//		}
//		
//	}
//	
//	public void testRenderList() {
//		
//		
//		try {
//			
//			final List<DataNode> dataNodes = this.createTestNodes(DataNode.class, 7);
//			final Page doc                 = (Page)getDocument();
//			final String key               = "TEST";
//			
//			assertEquals(7, dataNodes.size());
//			
//			DataNode rootNode = dataNodes.get(0); rootNode.setName("rootNode");
//			DataNode nodeA    = dataNodes.get(1); nodeA.setName("nodeA");
//			DataNode nodeB    = dataNodes.get(2); nodeB.setName("nodeB");
//			DataNode nodeC    = dataNodes.get(3); nodeC.setName("nodeC");
//			DataNode nodeD    = dataNodes.get(4); nodeD.setName("nodeD");
//			DataNode nodeE    = dataNodes.get(5); nodeE.setName("nodeE");
//			DataNode nodeF    = dataNodes.get(6); nodeF.setName("nodeF");
//			
//			rootNode.add(key, nodeA);
//			nodeA.add(key, nodeB);
//			nodeB.add(key, nodeC);
//			nodeC.add(key, nodeD);
//			nodeD.add(key, nodeE);
//			nodeE.add(key, nodeF);
//			
//			// create dom tree
//			Element html = doc.createElement("html");
//			Element body = doc.createElement("body");
//			Element div  = doc.createElement("div");
//			Element ul1  = doc.createElement("ul");
//			Element li1  = doc.createElement("li");
//			Element p1   = doc.createElement("p");
//			p1.appendChild(doc.createTextNode("${data.name}"));
//			Element ul2  = doc.createElement("ul");
//			Element li2  = doc.createElement("li");
//			Element p2   = doc.createElement("p");
//			p2.appendChild(doc.createTextNode("${data.name}"));
//			
//			
//			// create RENDER_LIST relationship between div and rootNode
//			PropertyMap properties = new PropertyMap();
//			properties.put(DataNode.keyProperty, key);
//			Services.command(securityContext, CreateRelationshipCommand.class).execute((DOMElement)div, rootNode, RelType.RENDER_LIST, properties, false);
//			
//			doc.appendChild(html);
//			html.appendChild(body);
//			body.appendChild(div);
//			div.appendChild(ul1);
//			ul1.appendChild(li1);
//			li1.appendChild(p1);
//			li1.appendChild(ul2);
//			ul2.appendChild(li2);
//			li2.appendChild(p2);
//			
//			// test rendered document
//			RenderContext ctx = new RenderContext(null, null, false, Locale.GERMAN);
//			doc.render(securityContext, ctx, 0);
//			
//			org.jsoup.nodes.Document parsedDocument = Jsoup.parse(ctx.getBuffer().toString());
//			
//			assertEquals("rootNode", parsedDocument.select("html > body > div > ul > li").get(0).child(0).text());
//			assertEquals("nodeA", parsedDocument.select("html > body > div > ul > li").get(1).child(0).text());
//			assertEquals("nodeB", parsedDocument.select("html > body > div > ul > li").get(2).child(0).text());
//			assertEquals("nodeC", parsedDocument.select("html > body > div > ul > li").get(3).child(0).text());
//			assertEquals("nodeD", parsedDocument.select("html > body > div > ul > li").get(4).child(0).text());
//			assertEquals("nodeE", parsedDocument.select("html > body > div > ul > li").get(5).child(0).text());
//			assertEquals("nodeF", parsedDocument.select("html > body > div > ul > li").get(6).child(0).text());
//			
//			
//			
//			
//		} catch (FrameworkException fex) {
//			
//			fail("unexpected exception");
//		}
//		
//	}
}
