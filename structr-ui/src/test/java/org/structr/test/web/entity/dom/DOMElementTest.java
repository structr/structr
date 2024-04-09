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

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.test.web.advanced.DOMTest;
import org.structr.web.entity.dom.DOMAttribute;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class DOMElementTest extends DOMTest {

	@Test
	public void testGetTagName() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			Document doc = getDocument();

			Element elem = doc.createElement("div");

			assertEquals("div", elem.getTagName());

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testAttributeMethods() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			Document doc = getDocument();

			Element elem = doc.createElement("div");

			String name1 = "test1";
			String name2 = "test2";
			String name3 = "test3";

			String value1 = "value1";
			String value2 = "value2";
			String value3 = "value3";

			elem.setAttribute(name1, value1);
			elem.setAttribute(name2, value2);
			elem.setAttribute(name3, value3);

			assertEquals(true, elem.hasAttributes());
			assertEquals(3, elem.getAttributes().getLength());

			assertEquals(value1, elem.getAttribute(name1));
			assertEquals(value2, elem.getAttribute(name2));
			assertEquals(value3, elem.getAttribute(name3));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}
	}

	@Test
	public void testAttributeNodeMethods() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			Page doc = (Page) getDocument();

			DOMElement elem = (DOMElement) doc.createElement("div");

			String name1 = "test1";
			String name2 = "test2";
			String name3 = "test3";

			String value1 = "value1";
			String value2 = "value2";
			String value3 = "value3";

			DOMAttribute attr1 = (DOMAttribute) doc.createAttribute(name1);
			assertNotNull(attr1);

			attr1.setValue(value1);
			elem.setAttributeNode(attr1);
			assertEquals(elem, attr1.getParentNode());

			DOMAttribute attr2 = (DOMAttribute) doc.createAttribute(name2);
			assertNotNull(attr2);

			attr2.setValue(value2);
			elem.setAttributeNode(attr2);
			assertEquals(elem, attr2.getParentNode());

			DOMAttribute attr3 = (DOMAttribute) doc.createAttribute(name3);
			assertNotNull(attr3);

			attr3.setValue(value3);
			elem.setAttributeNode(attr3);

			assertEquals(elem, attr3.getParentNode());

			assertEquals(true, elem.hasAttributes());
			assertEquals(3, elem.getAttributes().getLength());

			assertEquals(value1, elem.getAttribute(name1));
			assertEquals(attr1, elem.getAttributeNode(name1));

			assertEquals(value2, elem.getAttribute(name2));
			assertEquals(attr2, elem.getAttributeNode(name2));

			assertEquals(value3, elem.getAttribute(name3));
			assertEquals(attr3, elem.getAttributeNode(name3));

			// important for xpath: sibling methods
			assertEquals(attr2, attr1.getNextSibling());
			assertEquals(attr3, attr2.getNextSibling());
			assertEquals(attr2, attr3.getPreviousSibling());
			assertEquals(attr1, attr2.getPreviousSibling());

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}
	}

	@Test
	public void testCustomTag() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			Document doc = getDocument();

			Element elem = doc.createElement("svg");

			assertNotNull(elem);

			String name1 = "width";
			String name2 = "height";
			String name3 = "xmlns";

			String value1 = "100";
			String value2 = "200";
			String value3 = "http://www.w3.org/2000/svg";

			elem.setAttribute(name1, value1);
			elem.setAttribute(name2, value2);
			elem.setAttribute(name3, value3);

			assertEquals(true, elem.hasAttributes());
			assertEquals(3, elem.getAttributes().getLength());

			assertEquals(value1, elem.getAttribute(name1));
			assertEquals(value2, elem.getAttribute(name2));
			assertEquals(value3, elem.getAttribute(name3));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}
	}

}
