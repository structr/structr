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
package org.structr.web.entity.dom;

import java.util.List;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.web.common.DOMTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.relation.RenderNode;
import org.w3c.dom.Element;

/**
 * Test to render data mixed with markup
 *
 *
 */
public class RenderDataTest extends DOMTest {

	private static final Logger logger = LoggerFactory.getLogger(RenderDataTest.class.getName());

	@Test
	public void testRenderFolderTree() {

		String name = null;

		try (final Tx tx = app.tx()) {

			final List<Folder> folders = createTestNodes(Folder.class, 7);
			final List<File> files = createTestNodes(File.class, 7);
			final Page doc = (Page) getDocument();

			name = doc.getName();

			assertEquals(7, folders.size());

			Folder rootNode = folders.get(0);
			rootNode.setProperties(rootNode.getSecurityContext(), new PropertyMap(AbstractNode.name, "rootNode"));
			Folder folderA = folders.get(1);
			folderA.setProperties(folderA.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderA"));
			Folder folderB = folders.get(2);
			folderB.setProperties(folderB.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderB"));
			Folder folderC = folders.get(3);
			folderC.setProperties(folderC.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderC"));
			Folder folderD = folders.get(4);
			folderD.setProperties(folderD.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderD"));
			Folder folderE = folders.get(5);
			folderE.setProperties(folderE.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderE"));
			Folder folderF = folders.get(6);
			folderF.setProperties(folderF.getSecurityContext(), new PropertyMap(AbstractNode.name, "folderF"));

			FileBase file1 = files.get(0);
			file1.setProperties(file1.getSecurityContext(), new PropertyMap(AbstractNode.name, "file1"));
			FileBase file2 = files.get(1);
			file2.setProperties(file2.getSecurityContext(), new PropertyMap(AbstractNode.name, "file2"));
			FileBase file3 = files.get(2);
			file3.setProperties(file3.getSecurityContext(), new PropertyMap(AbstractNode.name, "file3"));
			FileBase file4 = files.get(3);
			file4.setProperties(file4.getSecurityContext(), new PropertyMap(AbstractNode.name, "file4"));
			FileBase file5 = files.get(4);
			file5.setProperties(file5.getSecurityContext(), new PropertyMap(AbstractNode.name, "file5"));
			FileBase file6 = files.get(5);
			file6.setProperties(file6.getSecurityContext(), new PropertyMap(AbstractNode.name, "file6"));
			FileBase file7 = files.get(6);
			file7.setProperties(file7.getSecurityContext(), new PropertyMap(AbstractNode.name, "file7"));

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

			makePublic(rootNode, folderA, folderB, folderC, folderD, folderE, folderF);

			makePublic(file1, file2, file3, file4, file5, file6, file7);

			// create dom tree
			Element html = doc.createElement("html");
			doc.appendChild(html);
			Element body = doc.createElement("body");
			html.appendChild(body);

			final Element div = doc.createElement("div");
			body.appendChild(div);

			Content rootNameContent = (Content) doc.createTextNode("${root.name}");

			div.appendChild(rootNameContent);

			final Element ul1 = doc.createElement("ul");
			div.appendChild(ul1);

			final Element li1 = doc.createElement("li");
			ul1.appendChild(li1);

			Content foldersNameContent = (Content) doc.createTextNode("${folders.name}");
			li1.appendChild(foldersNameContent);

			final Element li2 = doc.createElement("li");
			ul1.appendChild(li2);

			Content filesNameContent = (Content) doc.createTextNode("${files.name}");
			li2.appendChild(filesNameContent);

			final Element ul2 = doc.createElement("ul");
			li1.appendChild(ul2);

			final Element li3 = doc.createElement("li");
			ul2.appendChild(li3);

			Content files2NameContent = (Content) doc.createTextNode("${files.name}");
			li3.appendChild(files2NameContent);

			final Element li4 = doc.createElement("li");
			ul2.appendChild(li4);

			Content folders2NameContent = (Content) doc.createTextNode("${folders.name}");
			li4.appendChild(folders2NameContent);

			makePublic(rootNameContent, foldersNameContent, filesNameContent, files2NameContent, folders2NameContent);

			// create RENDER_NODE relationship between first ul and rootNode
			PropertyMap properties = new PropertyMap();
			//properties.put(LinkedListNode.keyProperty, key);
			StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class).execute((DOMElement) div, (NodeInterface) rootNode, RenderNode.class, properties);

			((DOMElement) div).setProperties(((DOMElement) div).getSecurityContext(), new PropertyMap(DOMElement.dataKey, "root"));

			((DOMElement) li1).setProperties(((DOMElement) li1).getSecurityContext(), new PropertyMap(DOMElement.dataKey, "folders"));
			((DOMElement) li2).setProperties(((DOMElement) li2).getSecurityContext(), new PropertyMap(DOMElement.dataKey, "files"));
			((DOMElement) li3).setProperties(((DOMElement) li3).getSecurityContext(), new PropertyMap(DOMElement.dataKey, "files"));
			((DOMElement) li4).setProperties(((DOMElement) li4).getSecurityContext(), new PropertyMap(DOMElement.dataKey, "folders"));

			makePublic(doc, html, body, div, ul1, ul2, li1, li2, li3, li4);

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			org.jsoup.nodes.Document parsedDocument = Jsoup.connect(baseUri + name).get();

			System.out.println(parsedDocument);

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

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("unexpected exception");
		}
	}

	@Test
	public void testRenderListFromRestQuery() {

		String name = null;

		try (final Tx tx = app.tx()) {

			final Page doc = (Page) getDocument();
			name = doc.getName();

			final List<User> users = createTestNodes(User.class, 3);

			assertEquals(3, users.size());

			User user1 = users.get(0);
			user1.setProperties(user1.getSecurityContext(), new PropertyMap(AbstractNode.name, "user1"));
			User user2 = users.get(1);
			user2.setProperties(user2.getSecurityContext(), new PropertyMap(AbstractNode.name, "user2"));
			User user3 = users.get(2);
			user3.setProperties(user3.getSecurityContext(), new PropertyMap(AbstractNode.name, "user3"));

			final List<File> files = createTestNodes(File.class, 6);

			assertEquals(6, files.size());

			FileBase nodeA = files.get(0);
			nodeA.setProperties(nodeA.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileA"));
			FileBase nodeB = files.get(1);
			nodeB.setProperties(nodeB.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileB"));
			FileBase nodeC = files.get(2);
			nodeC.setProperties(nodeC.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileC"));
			FileBase nodeD = files.get(3);
			nodeD.setProperties(nodeD.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileD"));
			FileBase nodeE = files.get(4);
			nodeE.setProperties(nodeE.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileE"));
			FileBase nodeF = files.get(5);
			nodeF.setProperties(nodeF.getSecurityContext(), new PropertyMap(AbstractNode.name, "fileF"));

			// create dom tree
			Element html = doc.createElement("html");
			Element body = doc.createElement("body");
			Element b = doc.createElement("b");
			final Element p1 = doc.createElement("p");

			final PropertyMap p1Properties = new PropertyMap();
			p1Properties.put(DOMElement.restQuery, "users?sort=name");
			p1Properties.put(DOMElement.dataKey, "user");
			((DOMElement) p1).setProperties(((DOMElement) p1).getSecurityContext(), p1Properties);

			Content userNameContentNode = (Content) doc.createTextNode("${user.name}");

			p1.appendChild(userNameContentNode);

			Element div = doc.createElement("div");
			final Element p2 = doc.createElement("p");

			final PropertyMap p2Properties = new PropertyMap();
			p2Properties.put(DOMElement.restQuery, "files?sort=name");
			p2Properties.put(DOMElement.dataKey, "file");
			((DOMElement) p2).setProperties(((DOMElement) p2).getSecurityContext(), p2Properties);

			Content fileNameContentNode = (Content) doc.createTextNode("${file.name}");

			p2.appendChild(fileNameContentNode);

			doc.appendChild(html);
			html.appendChild(body);
			body.appendChild(b);
			body.appendChild(div);
			b.appendChild(p1);
			div.appendChild(p2);

			makePublic(doc, html, body, div, b, p1, p2, fileNameContentNode, userNameContentNode, nodeA, nodeB, nodeC, nodeD, nodeE, nodeF, user1, user2, user3);

			System.out.println(doc.getContent(RenderContext.EditMode.CONTENT));


			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			logger.warn("", ex);
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			Document parsedDocument = Jsoup.connect(baseUri + name).get();

			System.out.println(parsedDocument.outerHtml());

			assertEquals("user1", parsedDocument.select("html > body > b > p").get(0).ownText());
			assertEquals("user2", parsedDocument.select("html > body > b > p").get(1).ownText());
			assertEquals("user3", parsedDocument.select("html > body > b > p").get(2).ownText());

			assertEquals("fileA", parsedDocument.select("html > body > div > p").get(0).ownText());
			assertEquals("fileB", parsedDocument.select("html > body > div > p").get(1).ownText());
			assertEquals("fileC", parsedDocument.select("html > body > div > p").get(2).ownText());
			assertEquals("fileD", parsedDocument.select("html > body > div > p").get(3).ownText());
			assertEquals("fileE", parsedDocument.select("html > body > div > p").get(4).ownText());
			assertEquals("fileF", parsedDocument.select("html > body > div > p").get(5).ownText());

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("unexpected exception");
		}

	}
}
