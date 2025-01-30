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
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.test.web.advanced.DOMTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

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

			final List<NodeInterface> users = createTestNodes("User", 3);

			assertEquals(3, users.size());

			NodeInterface user1 = users.get(0);
			user1.setProperty(Traits.of("NodeInterface").key("name"), "user1");
			NodeInterface user2 = users.get(1);
			user2.setProperty(Traits.of("NodeInterface").key("name"), "user2");
			NodeInterface user3 = users.get(2);
			user3.setProperty(Traits.of("NodeInterface").key("name"), "user3");

			final List<NodeInterface> files = createTestNodes("File", 6);

			assertEquals(6, files.size());

			NodeInterface nodeA = files.get(0);
			nodeA.setProperty(Traits.of("NodeInterface").key("name"), "fileA");
			NodeInterface nodeB = files.get(1);
			nodeB.setProperty(Traits.of("NodeInterface").key("name"), "fileB");
			NodeInterface nodeC = files.get(2);
			nodeC.setProperty(Traits.of("NodeInterface").key("name"), "fileC");
			NodeInterface nodeD = files.get(3);
			nodeD.setProperty(Traits.of("NodeInterface").key("name"), "fileD");
			NodeInterface nodeE = files.get(4);
			nodeE.setProperty(Traits.of("NodeInterface").key("name"), "fileE");
			NodeInterface nodeF = files.get(5);
			nodeF.setProperty(Traits.of("NodeInterface").key("name"), "fileF");

			// create dom tree
			DOMElement html = doc.createElement("html");
			DOMElement body = doc.createElement("body");
			DOMElement b = doc.createElement("b");
			final DOMElement p1 = doc.createElement("p");

			final PropertyMap p1Properties = new PropertyMap();
			p1Properties.put(Traits.of("DOMElement").key("restQuery"), "User?_sort=name");
			p1Properties.put(Traits.of("DOMElement").key("dataKey"), "user");
			((DOMElement) p1).setProperties(((DOMElement) p1).getSecurityContext(), p1Properties);

			Content userNameContentNode = (Content) doc.createTextNode("${user.name}");

			p1.appendChild(userNameContentNode);

			DOMElement div = doc.createElement("div");
			final DOMElement p2 = doc.createElement("p");

			final PropertyMap p2Properties = new PropertyMap();
			p2Properties.put(Traits.of("DOMElement").key("restQuery"), "File?_sort=name");
			p2Properties.put(Traits.of("DOMElement").key("dataKey"), "file");
			p2.setProperties(p2.getSecurityContext(), p2Properties);

			Content fileNameContentNode = doc.createTextNode("${file.name}");

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
