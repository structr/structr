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
package org.structr.web.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.Page;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.select.Elements;
import org.structr.core.graph.Tx;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

//~--- classes ----------------------------------------------------------------
/**
 * Create a simple test page
 *
 *
 */
public class CreateSimplePageTest extends StructrUiTest {

	private static final Logger logger = Logger.getLogger(CreateSimplePageTest.class.getName());

	//~--- methods --------------------------------------------------------
//	@Override
//	public void test00DbAvailable() {
//
//		super.test00DbAvailable();
//
//	}
	public void test01CreatePage() {

		final String pageName = "page-01";
		final String pageTitle = "Page Title";
		final String bodyText = "Body Text";

		final String h1ClassAttr = "heading";
		final String divClassAttr = "main";

		Page page = null;
		Element html = null;
		Element head = null;
		Element body = null;
		Element title = null;
		Element h1 = null;
		Element div = null;

		Text titleText = null;
		Text heading = null;
		Text bodyContent = null;

		try (final Tx tx = app.tx()) {

			page = Page.createNewPage(securityContext, pageName);

			if (page != null) {

				html = page.createElement("html");
				head = page.createElement("head");
				body = page.createElement("body");
				title = page.createElement("title");
				h1 = page.createElement("h1");
				div = page.createElement("div");

				titleText = page.createTextNode(pageTitle);
				heading = page.createTextNode(pageTitle);
				bodyContent = page.createTextNode(bodyText);

				makePublic(page, html, head, body, title, h1, div, titleText, heading, bodyContent);

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {
			// add HTML element to page
			page.appendChild(html);

			// add HEAD and BODY elements to HTML
			html.appendChild(head);
			html.appendChild(body);

			// add TITLE element to HEAD
			head.appendChild(title);

			// add H1 element to BODY
			body.appendChild(h1);
			h1.setAttribute("class", h1ClassAttr);

			// add DIV element
			body.appendChild(div);
			div.setAttribute("class", divClassAttr);

			// add text nodes
			title.appendChild(titleText);
			h1.appendChild(heading);
			div.appendChild(bodyContent);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected Exception");
		}

		assertTrue(page != null);
		assertTrue(page instanceof Page);

		try (final Tx tx = app.tx()) {

			Document doc = Jsoup.connect(baseUri + pageName).get();

			System.out.println(doc.html());

			assertFalse(doc.select("html").isEmpty());
			assertFalse(doc.select("html > head").isEmpty());
			assertFalse(doc.select("html > head > title").isEmpty());
			assertFalse(doc.select("html > body").isEmpty());

			assertEquals(doc.select("html > head > title").first().text(), pageTitle);

			Elements h1Elements = doc.select("html > body > h1");
			assertFalse(h1Elements.isEmpty());
			assertEquals(h1Elements.first().text(), pageTitle);
			assertEquals(h1Elements.first().attr("class"), h1ClassAttr);

			Elements divElements = doc.select("html > body > div");
			assertFalse(divElements.isEmpty());
			assertEquals(divElements.first().text(), bodyText);
			assertEquals(divElements.first().attr("class"), divClassAttr);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected Exception");

		}

	}

}
