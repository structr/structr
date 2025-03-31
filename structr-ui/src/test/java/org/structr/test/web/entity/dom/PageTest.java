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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.wrappers.dom.DOMNodeTraitWrapper;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;

import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class PageTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(PageTest.class.getName());

	@Test
	public void testGetElementsByTagName() {

		final String pageName = "page-01";
		try {
			try (final Tx tx = app.tx()) {

				Page page = Page.createNewPage(securityContext, pageName);

				assertTrue(page != null);
				assertTrue(page.is(StructrTraits.PAGE));

				DOMNode html  = page.createElement("html");
				DOMNode head  = page.createElement("head");
				DOMNode body  = page.createElement("body");
				DOMNode title = page.createElement("title");
				DOMNode h1    = page.createElement("h1");
				DOMNode div1  = page.createElement("div");
				DOMNode p1    = page.createElement("p");
				DOMNode div2  = page.createElement("div");
				DOMNode p2    = page.createElement("p");
				DOMNode div3  = page.createElement("div");
				DOMNode p3    = page.createElement("p");

				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);

				// add H1 element to BODY
				body.appendChild(h1);

				// add DIV element 1 to BODY
				body.appendChild(div1);
				div1.appendChild(p1);

				// add DIV element 2 to DIV
				div1.appendChild(div2);
				div2.appendChild(p2);

				// add DIV element 3 to DIV
				div2.appendChild(div3);
				div3.appendChild(p3);

				final List<DOMNode> divs = page.getElementsByTagName("p");
				assertEquals(p1, divs.get(0));
				assertEquals(p2, divs.get(1));
				assertEquals(p3, divs.get(2));

				tx.success();

			} catch (Exception ex) {

				throw new FrameworkException(422, ex.getMessage());

			}

		} catch (FrameworkException ex) {

			fail("Unexpected exception");

		}

	}

	@Test
	public void testAdoptNodes() {

		try {
			try (final Tx tx = app.tx()) {

				Page srcPage = Page.createNewPage(securityContext, "srcPage");

				assertTrue(srcPage != null);
				assertTrue(srcPage instanceof Page);

				DOMElement html = srcPage.createElement("html");
				DOMElement head = srcPage.createElement("head");
				DOMElement body = srcPage.createElement("body");
				DOMElement title = srcPage.createElement("title");
				DOMElement h1 = srcPage.createElement("h1");
				DOMElement div = srcPage.createElement("div");
				DOMElement p = srcPage.createElement("p");

				// add HTML element to page
				srcPage.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);

				// add H1 element to BODY
				body.appendChild(h1);

				// add DIV element to BODY
				body.appendChild(div);
				div.appendChild(p);

				// add text element to P
				p.appendChild(srcPage.createTextNode("First Paragraph"));

				Page dstPage = Page.createNewPage(securityContext, "dstPage");
				assertNotNull(dstPage);

				// test adopt method
				dstPage.adoptNode(html);

				// has parent been removed for the source element?
				assertNull(html.getParent());

				// has owner changed for all elements?
				assertEquals(dstPage, html.getOwnerDocument());
				assertEquals(dstPage, head.getOwnerDocument());
				assertEquals(dstPage, body.getOwnerDocument());
				assertEquals(dstPage, title.getOwnerDocument());
				assertEquals(dstPage, h1.getOwnerDocument());
				assertEquals(dstPage, div.getOwnerDocument());
				assertEquals(dstPage, p.getOwnerDocument());

				// have parents been kept for all other elements?
				assertEquals(html, head.getParent());
				assertEquals(html, body.getParent());
				assertEquals(head, title.getParent());
				assertEquals(body, h1.getParent());
				assertEquals(body, div.getParent());
				assertEquals(div, p.getParent());

				// srcPage should not have a document element any more
				//assertNull(srcPage.getDocumentElement());

				final List<DOMNode> srcChildren = srcPage.getChildNodes();
				final List<DOMNode> dstChildren = dstPage.getChildNodes();

				// srcPage should have no children any more
				assertEquals(0, srcChildren.size());

				tx.success();

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

		} catch (FrameworkException ex) {

			fail("Unexpected exception");
		}
	}

	@Test
	public void testImportNodesDeep() {

		try (final Tx tx = app.tx()) {

			Page srcPage = Page.createNewPage(securityContext, "srcPage");

			assertTrue(srcPage != null);
			assertTrue(srcPage instanceof Page);

			DOMElement html = srcPage.createElement("html");
			DOMElement head = srcPage.createElement("head");
			DOMElement body = srcPage.createElement("body");
			DOMElement title = srcPage.createElement("title");
			DOMElement h1 = srcPage.createElement("h1");
			DOMElement div = srcPage.createElement("div");
			DOMElement p = srcPage.createElement("p");

			// add HTML element to page
			srcPage.appendChild(html);

			// add HEAD and BODY elements to HTML
			html.appendChild(head);
			html.appendChild(body);

			// add TITLE element to HEAD
			head.appendChild(title);

			// add H1 element to BODY
			body.appendChild(h1);

			// add DIV element to BODY
			body.appendChild(div);
			div.appendChild(p);

			// add text element to P
			p.appendChild(srcPage.createTextNode("First Paragraph"));

			Page dstPage = Page.createNewPage(securityContext, "dstPage");
			assertNotNull(dstPage);

			// test
			assertEquals(srcPage, html.getOwnerDocument());

			makePublic(srcPage, dstPage, html, head, body, title, h1, div, p);

			// test import method
			dstPage.importNode(html, true);

			// has parent been removed for the source element?
			assertNull(html.getParent());

			// same owner for all elements?
			assertEquals(srcPage, html.getOwnerDocument());
			assertEquals(srcPage, head.getOwnerDocument());
			assertEquals(srcPage, body.getOwnerDocument());
			assertEquals(srcPage, title.getOwnerDocument());
			assertEquals(srcPage, h1.getOwnerDocument());
			assertEquals(srcPage, div.getOwnerDocument());
			assertEquals(srcPage, p.getOwnerDocument());

			// have parents been kept for all other elements?
			assertEquals(html, head.getParent());
			assertEquals(html, body.getParent());
			assertEquals(head, title.getParent());
			assertEquals(body, h1.getParent());
			assertEquals(body, div.getParent());
			assertEquals(div, p.getParent());

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			Document srcDoc = Jsoup.connect(baseUri + "srcPage").get();
			Document dstDoc = Jsoup.connect(baseUri + "dstPage").get();

			// pages should render exactly identical
			assertEquals(srcDoc.outerHtml(), dstDoc.outerHtml());

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception");
		}
	}

	@Test
	public void testCloneNode() {

		try {
			try (final Tx tx = app.tx()) {

				Page page = Page.createNewPage(securityContext, "srcPage");

				assertTrue(page != null);
				assertTrue(page.is(StructrTraits.PAGE));
				DOMElement html = page.createElement("html");
				DOMElement head = page.createElement("head");
				DOMElement body = page.createElement("body");
				DOMElement title = page.createElement("title");
				DOMElement h1 = page.createElement("h1");
				DOMElement div = page.createElement("div");
				DOMElement p = page.createElement("p");

				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);

				// add H1 element to BODY
				body.appendChild(h1);

				// add DIV element to BODY
				body.appendChild(div);
				div.appendChild(p);

				// add text element to P
				p.appendChild(page.createTextNode("First Paragraph"));

				DOMNode clone = body.cloneNode(false);

				assertTrue(isClone(clone, body));

				tx.success();
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testClonePageWithTemplateNode() {

		Page pageToClone = null;
		Page newPage     = null;

		// setup
		try (final Tx tx = app.tx()) {

			pageToClone = app.create(StructrTraits.PAGE, "test").as(Page.class);

			final Template templ = app.create(StructrTraits.TEMPLATE, "#template").as(Template.class);

			templ.setContent("Template: ${render(children)}");
			templ.setContentType("text/html");

			pageToClone.adoptNode(templ);
			pageToClone.appendChild(templ);

			System.out.println(pageToClone.getContent(RenderContext.EditMode.NONE));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		// clone page
		try (final Tx tx = app.tx()) {

			newPage = pageToClone.cloneNode(false).as(Page.class);

			newPage.setName(pageToClone.getName() + "-" + newPage.getNode().getId().toString());

			DOMNode firstChild = pageToClone.getFirstChild();
			if (firstChild != null) {

				final DOMNode newHtmlNode = DOMNodeTraitWrapper.cloneAndAppendChildren(securityContext, firstChild);

				newPage.adoptNode(newHtmlNode);
				newPage.appendChild(newHtmlNode);
			}

			System.out.println(newPage.getContent(RenderContext.EditMode.NONE));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		// modify pages
		try (final Tx tx = app.tx()) {

			// change all templates
			for (final NodeInterface node : app.nodeQuery(StructrTraits.TEMPLATE).getAsList()) {

				final Template template = node.as(Template.class);
				final Page page         = template.getOwnerDocument();

				assertNotNull("Page must have pageId set", page);

				final DOMElement div1 = page.createElement("div");
				div1.appendChild(page.createTextNode("div1"));

				template.appendChild(div1);
			}

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final String content1 = pageToClone.getContent(RenderContext.EditMode.NONE);
			final String content2 = newPage.getContent(RenderContext.EditMode.NONE);

			assertEquals("Content comparison after page clone failed", content1, content2);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

	}

	private boolean isClone(final DOMNode n1, final DOMNode n2) throws FrameworkException {

		final String content1 = n1.getNodeValue();
		final String content2 = n2.getNodeValue();
		boolean isClone       = true;

		isClone &= StringUtils.equals(n1.getType(), n2.getType());
		isClone &= StringUtils.equals(content1, content2);

		final Set<PropertyKey> compareKeys = n1.getTraits().getPropertyKeysForView(PropertyView.Html);
		for (final PropertyKey key : compareKeys) {

			final Object value1 = n1.getProperty(key);
			final Object value2 = n2.getProperty(key);

			isClone &= isEqualOrNull(value1, value2);
		}

		return isClone;
	}

	private boolean isEqualOrNull(final Object o1, final Object o2) {

		if (o1 == null && o2 != null) {
			return false;
		}

		if (o1 != null && o2 == null) {
			return false;
		}

		if (o1 == null && o2 == null) {
			return true;
		}

		return o1.equals(o2);
	}
}
