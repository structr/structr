/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import static junit.framework.TestCase.fail;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.common.DOMTest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 *
 * @author Christian Morgner
 */
public class ContentTest extends DOMTest {

	public void testSplitText() {

		try (final Tx tx = app.tx()) {

			Document document = getDocument();

			Text content = document.createTextNode("Dies ist ein Test");
			assertNotNull(content);

			Element div = document.createElement("div");
			assertNotNull(div);

			// add child
			div.appendChild(content);

			// test basic setting of content
			content.setData("Dies ist ein Test");
			assertEquals("Dies ist ein Test", content.getData());

			// test split method
			Content secondPart = (Content) content.splitText(8);
			assertNotNull(secondPart);

			assertEquals("Dies ist", content.getData());
			assertEquals(" ein Test", secondPart.getData());

			// check that parent has two children
			NodeList children = div.getChildNodes();
			assertNotNull(children);
			assertEquals(2, children.getLength());
			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}
	}

	public void testIsElementContentWhitespace() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		assertEquals(true, content.isElementContentWhitespace());
	}

	public void testGetData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());
	}

	public void testSetData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

	}

	public void testGetLength() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		assertEquals(17, content.getLength());
	}

	public void testSubstringData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		String substringData = content.substringData(5, 3);
		assertEquals("ist", substringData);
	}

	public void testAppendData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist");
		assertEquals("Dies ist", content.getData());

		content.appendData(" ein Test");
		assertEquals("Dies ist ein Test", content.getData());
	}

	public void testInsertData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ein Test");
		assertEquals("Dies ein Test", content.getData());

		content.insertData(5, "ist ");
		assertEquals("Dies ist ein Test", content.getData());
	}

	public void testDeleteData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		content.deleteData(5, 4);
		assertEquals("Dies ein Test", content.getData());
	}

	public void testReplaceData() {

		Content content = getContentNode();
		assertNotNull(content);

		// test basic setting of content
		content.setData("Dies ist ein Test");
		assertEquals("Dies ist ein Test", content.getData());

		content.replaceData(5, 3, "war");
		assertEquals("Dies war ein Test", content.getData());
	}

	public void testGetWholeText() {

	}

	public void testReplaceWholeText() {

	}
}
