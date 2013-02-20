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
import org.jsoup.Jsoup;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.File;
import org.structr.core.entity.Folder;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.DOMTest;
import org.structr.web.common.RenderContext;
import org.w3c.dom.Element;

/**
 * Test to render data mixed with markup
 * 
 * @author Axel Morgner
 */
public class RenderDataTest extends DOMTest {


	public void testRenderFolderTree() {
		
		
		try {
			
			final List<Folder> folders = createTestNodes(Folder.class, 7);
			final List<File>   files   = createTestNodes(File.class, 7);
			final Page doc                 = (Page)getDocument();
			
			assertEquals(7, folders.size());
			
			Folder rootNode = folders.get(0); rootNode.setName("rootNode");
			Folder folderA    = folders.get(1); folderA.setName("folderA");
			Folder folderB    = folders.get(2); folderB.setName("folderB");
			Folder folderC    = folders.get(3); folderC.setName("folderC");
			Folder folderD    = folders.get(4); folderD.setName("folderD");
			Folder folderE    = folders.get(5); folderE.setName("folderE");
			Folder folderF    = folders.get(6); folderF.setName("folderF");

			File file1    = files.get(0); file1.setName("file1");
			File file2    = files.get(1); file2.setName("file2");
			File file3    = files.get(2); file3.setName("file3");
			File file4    = files.get(3); file4.setName("file4");
			File file5    = files.get(4); file5.setName("file5");
			File file6    = files.get(5); file6.setName("file6");
			File file7    = files.get(6); file7.setName("file7");
			
			rootNode.treeAppendChild(RelType.CONTAINS, folderA);
			rootNode.treeAppendChild(RelType.CONTAINS, folderB);
			rootNode.treeAppendChild(RelType.CONTAINS, file1);
			rootNode.treeAppendChild(RelType.CONTAINS, file2);
			
			folderA.treeAppendChild(RelType.CONTAINS, folderC);
			folderA.treeAppendChild(RelType.CONTAINS, folderD);
			folderA.treeAppendChild(RelType.CONTAINS, file3);
			folderA.treeAppendChild(RelType.CONTAINS, file4);
			
			folderB.treeAppendChild(RelType.CONTAINS, folderE);
			folderB.treeAppendChild(RelType.CONTAINS, folderF);
			folderB.treeAppendChild(RelType.CONTAINS, file5);
			folderB.treeAppendChild(RelType.CONTAINS, file6);
			
			
			// create dom tree
			Element html = doc.createElement("html"); doc.appendChild(html);
			Element body = doc.createElement("body"); html.appendChild(body);
			Element div  = doc.createElement("div"); body.appendChild(div);
			div.appendChild(doc.createTextNode("${root.name}"));
			Element ul1  = doc.createElement("ul"); div.appendChild(ul1);
			Element li1  = doc.createElement("li"); ul1.appendChild(li1);
			li1.appendChild(doc.createTextNode("${folders.name}"));
			Element li2  = doc.createElement("li"); ul1.appendChild(li2);
			li2.appendChild(doc.createTextNode("${files.name}"));

			Element ul2  = doc.createElement("ul"); li1.appendChild(ul2);
			Element li3  = doc.createElement("li"); ul2.appendChild(li3);
			li3.appendChild(doc.createTextNode("${files.name}"));
			Element li4  = doc.createElement("li"); ul2.appendChild(li4);
			li4.appendChild(doc.createTextNode("${folders.name}"));
			
			// create RENDER_NODE relationship between first ul and rootNode
			PropertyMap properties = new PropertyMap();
			//properties.put(LinkedListNode.keyProperty, key);
			Services.command(securityContext, CreateRelationshipCommand.class).execute((DOMElement)div, rootNode, RelType.RENDER_NODE, properties, false);
			
			((DOMElement) div).setProperty(DOMElement.dataKey, "root");
			
			((DOMElement) li1).setProperty(DOMElement.dataKey, "folders");
			((DOMElement) li2).setProperty(DOMElement.dataKey, "files");
			((DOMElement) li3).setProperty(DOMElement.dataKey, "files");
			((DOMElement) li4).setProperty(DOMElement.dataKey, "folders");
			
			
			RenderContext ctx = new RenderContext(null, null, false, Locale.GERMAN);
			doc.render(securityContext, ctx, 0);
			
			System.out.println(ctx.getBuffer().toString());

			// TODO: fix assertions
			//org.jsoup.nodes.Document parsedDocument = Jsoup.parse(ctx.getBuffer().toString());
			
			//assertEquals("rootNode", parsedDocument.select("html > body > div").get(0).child(0).text());
			//assertEquals("folderA", parsedDocument.select("html > body > div > ul > li").get(1).child(0).text());
			//assertEquals("folderB", parsedDocument.select("html > body > div > ul > li").get(2).child(0).text());
			//assertEquals("file1", parsedDocument.select("html > body > div > ul > li").get(3).child(0).text());
			//assertEquals("file2", parsedDocument.select("html > body > div > ul > li").get(4).child(0).text());
			//assertEquals("nodeE", parsedDocument.select("html > body > div > ul > li").get(5).child(0).text());
			//assertEquals("nodeF", parsedDocument.select("html > body > div > ul > li").get(6).child(0).text());
			
			
			
		} catch (FrameworkException fex) {
			
			fail("unexpected exception");
		}
		
	}
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
//			((DOMElement) div).setProperty(DOMElement.dataKey, "data");
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
