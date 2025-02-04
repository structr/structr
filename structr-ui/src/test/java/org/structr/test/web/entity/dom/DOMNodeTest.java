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
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.test.web.advanced.DOMTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;

import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class DOMNodeTest extends DOMTest {

	@Test
	public void testAppendChild() {

		try (final Tx tx = app.tx()) {

			final String domChildrenType = "DOMNodeCONTAINSDOMNode";

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			Content content1 = document.createTextNode("content1");
			Content content2 = document.createTextNode("content2");
			Content content3 = document.createTextNode("content3");

			assertNotNull(content1);
			assertNotNull(content2);
			assertNotNull(content3);

			// first step
			div.appendChild(content1);

			// check for correct relationship management
			List<RelationshipInterface> divRels = toList(div.getOutgoingRelationships(domChildrenType));
			assertEquals(1, divRels.size());
			assertEquals(Integer.valueOf(0), divRels.get(0).getProperty(div.getPositionProperty()));

			// second step
			div.appendChild(content2);

			// check for correct relationship management
			divRels = toList(div.getOutgoingRelationships(domChildrenType));
			assertEquals(2, divRels.size());
			assertEquals(Integer.valueOf(0), divRels.get(0).getProperty(div.getPositionProperty()));
			assertEquals(Integer.valueOf(1), divRels.get(1).getProperty(div.getPositionProperty()));

			// third step: test removal of old parent when appending an existing node
			div.appendChild(content3);

			// assert that div has 3 children now
			assertEquals(3, div.getChildNodes().size());

			// create new container
			DOMElement div2 = document.createElement("div");
			assertNotNull(div2);

			div.appendChild(div2);

			// div should have 4 children by now
			assertEquals(4, div.getChildNodes().size());

			// move text node to div2
			div2.appendChild(content3);

				// div should have 3 children now,
			// div2 should have content3 as a child now
			assertEquals(3, div.getChildNodes().size());
			assertEquals(content3, div2.getChildNodes().get(0));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testGetParentNode() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content content = document.createTextNode("Dies ist ein Test");
			assertNotNull(content);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add child
			div.appendChild(content);

			DOMNode parent = content.getParent();

			assertEquals(div, parent);

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testGetChildNodes() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);

			final List<DOMNode> children = div.getChildNodes();

			assertEquals(test1, children.get(0));
			assertEquals(test2, children.get(1));
			assertEquals(test3, children.get(2));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testRemoveChildNode() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// note that we do NOT add test6 as a child!
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// test remove child node method
			div.removeChild(test3);

			final List<DOMNode> children2 = div.getChildNodes();
			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test4, children2.get(2));
			assertEquals(test5, children2.get(3));

			// test remove child node method
			div.removeChild(test1);

			final List<DOMNode> children3 = div.getChildNodes();
			assertEquals(test2, children3.get(0));
			assertEquals(test4, children3.get(1));
			assertEquals(test5, children3.get(2));

			// and finally, test errors that should be raised
			try {

				div.removeChild(test6);

				fail("Removing a node that is not a child of the given node should raise a DOMException");

			} catch (DOMException dex) {

				assertEquals(DOMException.NOT_FOUND_ERR, dex.code);
			}
			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testSiblingMethods() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");

			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// test first child
			assertEquals(test1, div.getFirstChild());

			// test last child
			assertEquals(test5, div.getLastChild());

			// test sibling methods
			assertNull(test1.getPreviousSibling());
			assertEquals(test3, test4.getPreviousSibling());
			assertEquals(test2, test1.getNextSibling());
			assertNull(test5.getNextSibling());

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testAppendChildErrors() {

		try (final Tx tx = app.tx()) {

			Page wrongDocument = getDocument().as(Page.class);
			Page document = getDocument().as(Page.class);

			assertNotNull(document);
			assertNotNull(wrongDocument);

			Content wrongTextNode = wrongDocument.createTextNode("test");

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);

			final List<DOMNode> children = div.getChildNodes();

			assertEquals(test1, children.get(0));
			assertEquals(test2, children.get(1));
			assertEquals(test3, children.get(2));

			DOMElement div2 = document.createElement("div");
			assertNotNull(div2);

			div.appendChild(div2);

			assertEquals(div, div2.getParent());

			try {

				div.appendChild(wrongTextNode);
				fail("Adding a node that was not created using the correct document should raise a DOMException");

			} catch (FrameworkException dex) {

				assertEquals(DOMException.WRONG_DOCUMENT_ERR, dex.getStatus());
			}

			try {

				div.appendChild(div);
				fail("Adding a node to itself should raise a DOMException");

			} catch (FrameworkException dex) {

				assertEquals(DOMException.HIERARCHY_REQUEST_ERR, dex.getStatus());
			}

			try {

				div2.appendChild(div);
				fail("Adding one of its own ancestors to a node should raise a DOMException");

			} catch (FrameworkException dex) {

				assertEquals(DOMException.HIERARCHY_REQUEST_ERR, dex.getStatus());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testReplaceChild() {

		try (final Tx tx = app.tx()) {


			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// examine children
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// test replace child
			div.replaceChild(test6, test3);

			// examine children
			final List<DOMNode> children2 = div.getChildNodes();
			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test6, children2.get(2));
			assertEquals(test4, children2.get(3));
			assertEquals(test5, children2.get(4));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testInsertBefore() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// examine children
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// test replace child
			div.insertBefore(test6, test3);

			// examine children
			final List<DOMNode> children2 = div.getChildNodes();
			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test6, children2.get(2));
			assertEquals(test3, children2.get(3));
			assertEquals(test4, children2.get(4));
			assertEquals(test5, children2.get(5));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testReplaceChildWithFragment() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			Content test7 = document.createTextNode("test7");
			Content test8 = document.createTextNode("test8");
			Content test9 = document.createTextNode("test9");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);
			assertNotNull(test7);
			assertNotNull(test8);
			assertNotNull(test9);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// examine children
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// examine children
			final List<DOMNode> children2 = div.getChildNodes();

			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test6, children2.get(2));
			assertEquals(test7, children2.get(3));
			assertEquals(test8, children2.get(4));
			assertEquals(test9, children2.get(5));
			assertEquals(test4, children2.get(6));
			assertEquals(test5, children2.get(7));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testInsertBeforeWithFragment() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			Content test7 = document.createTextNode("test7");
			Content test8 = document.createTextNode("test8");
			Content test9 = document.createTextNode("test9");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);
			assertNotNull(test7);
			assertNotNull(test8);
			assertNotNull(test9);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// examine children
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// examine children
			final List<DOMNode> children2 = div.getChildNodes();

			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test6, children2.get(2));
			assertEquals(test7, children2.get(3));
			assertEquals(test8, children2.get(4));
			assertEquals(test9, children2.get(5));
			assertEquals(test3, children2.get(6));
			assertEquals(test4, children2.get(7));
			assertEquals(test5, children2.get(8));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testAppendWithFragment() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			Content test1 = document.createTextNode("test1");
			Content test2 = document.createTextNode("test2");
			Content test3 = document.createTextNode("test3");
			Content test4 = document.createTextNode("test4");
			Content test5 = document.createTextNode("test5");
			Content test6 = document.createTextNode("test6");
			Content test7 = document.createTextNode("test7");
			Content test8 = document.createTextNode("test8");
			Content test9 = document.createTextNode("test9");
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);
			assertNotNull(test7);
			assertNotNull(test8);
			assertNotNull(test9);

			DOMElement div = document.createElement("div");
			assertNotNull(div);

			// add children
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);

			// examine children
			final List<DOMNode> children1 = div.getChildNodes();
			assertEquals(test1, children1.get(0));
			assertEquals(test2, children1.get(1));
			assertEquals(test3, children1.get(2));
			assertEquals(test4, children1.get(3));
			assertEquals(test5, children1.get(4));

			// examine children
			final List<DOMNode> children2 = div.getChildNodes();

			assertEquals(test1, children2.get(0));
			assertEquals(test2, children2.get(1));
			assertEquals(test3, children2.get(2));
			assertEquals(test4, children2.get(3));
			assertEquals(test5, children2.get(4));
			assertEquals(test6, children2.get(5));
			assertEquals(test7, children2.get(6));
			assertEquals(test8, children2.get(7));
			assertEquals(test9, children2.get(8));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	@Test
	public void testNormalize() {

		try (final Tx tx = app.tx()) {

			NodeInterface node = getDocument();
			assertNotNull(node);

			final Page document = node.as(Page.class);

			assertNotNull(document);

			final Content test0 = document.createTextNode("test0");
			final Content test1 = document.createTextNode("test1");
			final Content test2 = document.createTextNode("test2");
			final Content test3 = document.createTextNode("test3");
			final Content test4 = document.createTextNode("test4");
			final Content test5 = document.createTextNode("test5");
			final Content test6 = document.createTextNode("test6");
			final Content test7 = document.createTextNode("test7");
			final Content test8 = document.createTextNode("test8");
			final Content test9 = document.createTextNode("test9");

			assertNotNull(test0);
			assertNotNull(test1);
			assertNotNull(test2);
			assertNotNull(test3);
			assertNotNull(test4);
			assertNotNull(test5);
			assertNotNull(test6);
			assertNotNull(test7);
			assertNotNull(test8);
			assertNotNull(test9);

			final DOMElement div = document.createElement("div");
			assertNotNull(div);

			final DOMElement p0 = document.createElement("p");
			final DOMElement p1 = document.createElement("p");
			final DOMElement p2 = document.createElement("p");
			final DOMElement p3 = document.createElement("p");

			assertNotNull(p0);
			assertNotNull(p1);
			assertNotNull(p2);
			assertNotNull(p3);

			div.appendChild(test0);
			div.appendChild(p0);
			div.appendChild(test1);
			div.appendChild(test2);
			div.appendChild(p1);
			div.appendChild(test3);
			div.appendChild(test4);
			div.appendChild(test5);
			div.appendChild(p2);
			div.appendChild(test6);
			div.appendChild(test7);
			div.appendChild(test8);
			div.appendChild(test9);
			div.appendChild(p3);

			// normalize
			div.normalize();

			final List<DOMNode> children = div.getChildNodes();

			// div should now have 8 children,
			assertEquals(8, children.size());

			// check normalized children
			assertEquals("test0", children.get(0).getNodeValue());
			assertEquals(p0, children.get(1));
			assertEquals("test1test2", children.get(2).getNodeValue());
			assertEquals(p1, children.get(3));
			assertEquals("test3test4test5", children.get(4).getNodeValue());
			assertEquals(p2, children.get(5));
			assertEquals("test6test7test8test9", children.get(6).getNodeValue());
			assertEquals(p3, children.get(7));

			tx.success();

		} catch (FrameworkException fex) {

			fail("unexpected exception");
		}

	}

	private <T extends GraphObject> List<T> toList(final Iterable<T> it) {

		List<T> list = new LinkedList();

		for (T obj : it) {

			list.add(obj);

		}

		return list;
	}
}
