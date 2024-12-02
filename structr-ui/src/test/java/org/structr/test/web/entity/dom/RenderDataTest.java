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
package org.structr.test.web.entity.dom;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.test.web.advanced.DOMTest;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Test to render data mixed with markup
 *
 *
 */
public class RenderDataTest extends DOMTest {

	private static final Logger logger = LoggerFactory.getLogger(RenderDataTest.class.getName());

	@Test
	public void testRenderListFromRestQuery() {

		String name = null;

		try (final Tx tx = app.tx()) {

			final Page doc = (Page) getDocument();
			name = doc.getName();

			final List<User> users = createTestNodes(User.class, 3);

			assertEquals(3, users.size());

			User user1 = users.get(0);
			user1.setProperty(AbstractNode.name, "user1");
			User user2 = users.get(1);
			user2.setProperty(AbstractNode.name, "user2");
			User user3 = users.get(2);
			user3.setProperty(AbstractNode.name, "user3");

			final List<File> files = createTestNodes(File.class, 6);

			assertEquals(6, files.size());

			File nodeA = files.get(0);
			nodeA.setProperty(AbstractNode.name, "fileA");
			File nodeB = files.get(1);
			nodeB.setProperty(AbstractNode.name, "fileB");
			File nodeC = files.get(2);
			nodeC.setProperty(AbstractNode.name, "fileC");
			File nodeD = files.get(3);
			nodeD.setProperty(AbstractNode.name, "fileD");
			File nodeE = files.get(4);
			nodeE.setProperty(AbstractNode.name, "fileE");
			File nodeF = files.get(5);
			nodeF.setProperty(AbstractNode.name, "fileF");

			// create dom tree
			Element html = doc.createElement("html");
			Element body = doc.createElement("body");
			Element b = doc.createElement("b");
			final Element p1 = doc.createElement("p");

			final PropertyMap p1Properties = new PropertyMap();
			p1Properties.put(StructrApp.key(DOMElement.class, "restQuery"), "User?_sort=name");
			p1Properties.put(StructrApp.key(DOMElement.class, "dataKey"), "user");
			((DOMElement) p1).setProperties(((DOMElement) p1).getSecurityContext(), p1Properties);

			Content userNameContentNode = (Content) doc.createTextNode("${user.name}");

			p1.appendChild(userNameContentNode);

			Element div = doc.createElement("div");
			final Element p2 = doc.createElement("p");

			final PropertyMap p2Properties = new PropertyMap();
			p2Properties.put(StructrApp.key(DOMElement.class, "restQuery"), "File?_sort=name");
			p2Properties.put(StructrApp.key(DOMElement.class, "dataKey"), "file");
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

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			Document parsedDocument = Jsoup.connect(baseUri + name).timeout(100000).get();

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
			ex.printStackTrace();
			fail("unexpected exception");
		}

	}
}
