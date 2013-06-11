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


package org.structr.web.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.Page;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.select.Elements;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

//~--- classes ----------------------------------------------------------------

/**
 * Create a simple test page
 *
 * @author Axel Morgner
 */
public class CreatePageTest extends StructrUiTest {

	private static final Logger logger = Logger.getLogger(CreatePageTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01CreatePage() {

		final String pageName	= "page-01";
		final String pageTitle	= "Page Title";
		final String bodyText	= "Body Text";
		
		final String h1ClassAttr = "heading";
		final String divClassAttr = "main";

		try {

			Page page = Page.createNewPage(securityContext, pageName);
			if (page != null) {
				
				Element html  = page.createElement("html");
				Element head  = page.createElement("head");
				Element body  = page.createElement("body");
				Element title = page.createElement("title");
				Element h1    = page.createElement("h1");
				Element div   = page.createElement("div");
				
				Text titleText   = page.createTextNode(pageTitle);
				Text heading     = page.createTextNode(pageTitle);
				Text bodyContent = page.createTextNode(bodyText);
					
				makeNodesPublic(page, html, head, body, title, h1, div, titleText, heading, bodyContent);
				
				try {
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
					
				} catch (DOMException dex) {
					
					throw new FrameworkException(422, dex.getMessage());
				}
			}

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			try {

				Document doc = Jsoup.connect(baseUri + pageName).get();
				
				System.out.println(doc.html());

				assertFalse(doc.select("html").isEmpty());
				assertFalse(doc.select("html > head").isEmpty());
				assertFalse(doc.select("html > head > title").isEmpty());
				assertFalse(doc.select("html > body").isEmpty());
				
				assertEquals(doc.select("html > head > title").first().text(), pageTitle);
				
				Elements h1 = doc.select("html > body > h1");
				assertFalse(h1.isEmpty());
				assertEquals(h1.first().text(), pageTitle);
				assertEquals(h1.first().attr("class"), h1ClassAttr);

				Elements div = doc.select("html > body > div");
				assertFalse(div.isEmpty());
				assertEquals(div.first().text(), bodyText);
				assertEquals(div.first().attr("class"), divClassAttr);
				

			} catch (IOException ioex) {

				logger.log(Level.SEVERE, ioex.toString());
				fail("Unexpected IOException");

			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	private void makeNodesPublic(final Node... nodes) {
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (Node node : nodes) {
						((GraphObject) node).setProperty(GraphObject.visibleToPublicUsers, true);
					}
					
					return null;
				}
			});
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
	}
}
