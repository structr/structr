/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
package org.structr.web.entity.dom;

import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import static junit.framework.TestCase.assertEquals;
import org.jsoup.Jsoup;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.DOMTest;
import org.structr.web.common.RenderContext;
import org.w3c.dom.Element;
import static org.mockito.Mockito.*;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.User;
import org.structr.web.entity.relation.RenderNode;

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

			try {
				app.beginTx();

				Folder rootNode   = folders.get(0); rootNode.setProperty(AbstractNode.name, "rootNode");
				Folder folderA    = folders.get(1); folderA.setProperty(AbstractNode.name, "folderA");
				Folder folderB    = folders.get(2); folderB.setProperty(AbstractNode.name, "folderB");
				Folder folderC    = folders.get(3); folderC.setProperty(AbstractNode.name, "folderC");
				Folder folderD    = folders.get(4); folderD.setProperty(AbstractNode.name, "folderD");
				Folder folderE    = folders.get(5); folderE.setProperty(AbstractNode.name, "folderE");
				Folder folderF    = folders.get(6); folderF.setProperty(AbstractNode.name, "folderF");

				File file1    = files.get(0); file1.setProperty(AbstractNode.name, "file1");
				File file2    = files.get(1); file2.setProperty(AbstractNode.name, "file2");
				File file3    = files.get(2); file3.setProperty(AbstractNode.name, "file3");
				File file4    = files.get(3); file4.setProperty(AbstractNode.name, "file4");
				File file5    = files.get(4); file5.setProperty(AbstractNode.name, "file5");
				File file6    = files.get(5); file6.setProperty(AbstractNode.name, "file6");
				File file7    = files.get(6); file7.setProperty(AbstractNode.name, "file7");

				rootNode.treeAppendChild(folderA);
				rootNode.treeAppendChild(folderB);
				rootNode.treeAppendChild(file1);
				rootNode.treeAppendChild(file2);

				folderA.treeAppendChild(folderC);
				folderA.treeAppendChild(folderD);
				folderA.treeAppendChild(file3);
				folderA.treeAppendChild(file4);

				folderB.treeAppendChild(folderE);
				folderB.treeAppendChild(folderF);
				folderB.treeAppendChild(file5);
				folderB.treeAppendChild(file6);


				// create dom tree
				Element html = doc.createElement("html"); doc.appendChild(html);
				Element body = doc.createElement("body"); html.appendChild(body);

				final Element div  = doc.createElement("div"); body.appendChild(div);
				div.appendChild(doc.createTextNode("${root.name}"));

				final Element ul1  = doc.createElement("ul"); div.appendChild(ul1);

				final Element li1  = doc.createElement("li"); ul1.appendChild(li1);
				li1.appendChild(doc.createTextNode("${folders.name}"));

				final Element li2  = doc.createElement("li"); ul1.appendChild(li2);
				li2.appendChild(doc.createTextNode("${files.name}"));

				final Element ul2  = doc.createElement("ul"); li1.appendChild(ul2);

				final Element li3  = doc.createElement("li"); ul2.appendChild(li3);
				li3.appendChild(doc.createTextNode("${files.name}"));

				final Element li4  = doc.createElement("li"); ul2.appendChild(li4);
				li4.appendChild(doc.createTextNode("${folders.name}"));

				// create RENDER_NODE relationship between first ul and rootNode
				PropertyMap properties = new PropertyMap();
				//properties.put(LinkedListNode.keyProperty, key);
				StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class).execute((DOMElement)div, (NodeInterface)rootNode, RenderNode.class, properties);

				((DOMElement) div).setProperty(DOMElement.dataKey, "root");

				((DOMElement) li1).setProperty(DOMElement.dataKey, "folders");
				((DOMElement) li2).setProperty(DOMElement.dataKey, "files");
				((DOMElement) li3).setProperty(DOMElement.dataKey, "files");
				((DOMElement) li4).setProperty(DOMElement.dataKey, "folders");

				app.commitTx();

			} finally {
				app.finishTx();
			}
			
			RenderContext ctx = new RenderContext(null, null, EditMode.NONE, Locale.GERMAN);
			doc.render(securityContext, ctx, 0);
			
			System.out.println(ctx.getBuffer().toString());

			org.jsoup.nodes.Document parsedDocument = Jsoup.parse(ctx.getBuffer().toString());
			
			assertEquals("rootNode", parsedDocument.select("html > body > div").get(0).ownText());
			assertEquals("folderA", parsedDocument.select("html > body > div > ul > li").get(0).ownText());
			assertEquals("folderB", parsedDocument.select("html > body > div > ul > li").get(1).ownText());
			assertEquals("file1", parsedDocument.select("html > body > div > ul > li").get(2).ownText());
			assertEquals("file2", parsedDocument.select("html > body > div > ul > li").get(3).ownText());
			assertEquals("file3", parsedDocument.select("html > body > div > ul > li > ul > li").get(0).ownText());
			assertEquals("file4", parsedDocument.select("html > body > div > ul > li > ul > li").get(1).ownText());
			assertEquals("folderC", parsedDocument.select("html > body > div > ul > li > ul > li").get(2).ownText());
			assertEquals("folderD", parsedDocument.select("html > body > div > ul > li > ul > li").get(3).ownText());

			assertEquals("file5", parsedDocument.select("html > body > div > ul > li").get(1).child(0).child(0).ownText());
			assertEquals("file6", parsedDocument.select("html > body > div > ul > li").get(1).child(0).child(1).ownText());
			assertEquals("folderE", parsedDocument.select("html > body > div > ul > li").get(1).child(0).child(2).ownText());
			assertEquals("folderF", parsedDocument.select("html > body > div > ul > li").get(1).child(0).child(3).ownText());
			
		} catch (FrameworkException fex) {
			
			fail("unexpected exception");
		}
		
	}
	
	public void testRenderListFromRestQuery() {
		
		
		try {
			final Page doc = (Page)getDocument();
			
			try {
				app.beginTx();

				final List<User> users = createTestNodes(User.class, 3);

				assertEquals(3, users.size());

				User user1    = users.get(0); user1.setProperty(AbstractNode.name, "user1");
				User user2    = users.get(1); user2.setProperty(AbstractNode.name, "user2");
				User user3    = users.get(2); user3.setProperty(AbstractNode.name, "user3");

				final List<File> files = createTestNodes(File.class, 6);

				assertEquals(6, files.size());

				File nodeA    = files.get(0); nodeA.setProperty(AbstractNode.name, "fileA");
				File nodeB    = files.get(1); nodeB.setProperty(AbstractNode.name, "fileB");
				File nodeC    = files.get(2); nodeC.setProperty(AbstractNode.name, "fileC");
				File nodeD    = files.get(3); nodeD.setProperty(AbstractNode.name, "fileD");
				File nodeE    = files.get(4); nodeE.setProperty(AbstractNode.name, "fileE");
				File nodeF    = files.get(5); nodeF.setProperty(AbstractNode.name, "fileF");

				// create dom tree
				Element html     = doc.createElement("html");
				Element body     = doc.createElement("body");
				Element b        = doc.createElement("b");
				final Element p1 = doc.createElement("p");

				((DOMElement) p1).setProperty(DOMElement.restQuery, "users?sort=name");
				((DOMElement) p1).setProperty(DOMElement.dataKey, "user");


				p1.appendChild(doc.createTextNode("${user.name}"));

				Element div      = doc.createElement("div");
				final Element p2 = doc.createElement("p");

				((DOMElement) p2).setProperty(DOMElement.restQuery, "files?sort=name");
				((DOMElement) p2).setProperty(DOMElement.dataKey, "file");


				p2.appendChild(doc.createTextNode("${file.name}"));

				doc.appendChild(html);
				html.appendChild(body);
				body.appendChild(b);
				body.appendChild(div);
				b.appendChild(p1);
				div.appendChild(p2);

				app.commitTx();

			} finally {
				app.finishTx();
			}
						
			HttpServletRequest request = mock(HttpServletRequest.class);
			
			// test rendered document
			RenderContext ctx = new RenderContext(request, null, EditMode.NONE, Locale.GERMAN);
			doc.render(securityContext, ctx, 0);
			
			System.out.println(ctx.getBuffer().toString());
			
			org.jsoup.nodes.Document parsedDocument = Jsoup.parse(ctx.getBuffer().toString());

			assertEquals("user1", parsedDocument.select("html > body > b > p").get(0).ownText());
			assertEquals("user2", parsedDocument.select("html > body > b > p").get(1).ownText());
			assertEquals("user3", parsedDocument.select("html > body > b > p").get(2).ownText());
			
			assertEquals("fileA", parsedDocument.select("html > body > div > p").get(0).ownText());
			assertEquals("fileB", parsedDocument.select("html > body > div > p").get(1).ownText());
			assertEquals("fileC", parsedDocument.select("html > body > div > p").get(2).ownText());
			assertEquals("fileD", parsedDocument.select("html > body > div > p").get(3).ownText());
			assertEquals("fileE", parsedDocument.select("html > body > div > p").get(4).ownText());
			assertEquals("fileF", parsedDocument.select("html > body > div > p").get(5).ownText());
			
		} catch (FrameworkException fex) {
			
			fail("unexpected exception");
		}
		
	}	
}
