/*
 *  Copyright (C) 2010-2013 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.Content;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.H1;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.HtmlElement;
import org.structr.web.entity.html.Title;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.select.Elements;
import org.structr.web.entity.relation.ChildrenRelationship;

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

			Page page = transactionCommand.execute(new StructrTransaction<Page>() {

				@Override
				public Page execute() throws FrameworkException {

					Page page = (Page) createElement(null, Page.class.getSimpleName(), 0, null, pageName);

					page.setProperty(Page.contentType, "text/html");

					Html html   = (Html) createElement(page, Html.class.getSimpleName(), 0, page);
					Head head   = (Head) createElement(page, Head.class.getSimpleName(), 0, html);
					Body body   = (Body) createElement(page, Body.class.getSimpleName(), 1, html);
					Title title = (Title) createElement(page, Title.class.getSimpleName(), 0, head);

					// nodeData.put(Content.UiKey.content.name(), "Page Title");
					Content content = (Content) createElement(page, Content.class.getSimpleName(), 0, title);

					// nodeData.remove(Content.UiKey.content.name());
					content.setProperty(Content.content, pageTitle);

					H1 h1 = (H1) createElement(page, H1.class.getSimpleName(), 0, body);
					h1.setProperty(Div._class, h1ClassAttr);

					// nodeData.put(Content.UiKey.content.name(), "Page Title");
					Content h1Content = (Content) createElement(page, Content.class.getSimpleName(), 0, h1);
					

					// nodeData.remove(Content.UiKey.content.name());
					h1Content.setProperty(Content.content, pageTitle);

					Div div = (Div) createElement(page, Div.class.getSimpleName(), 1, body);
					div.setProperty(Div._class, divClassAttr);

					// nodeData.put(Content.UiKey.content.name(), "Body Text");
					Content divContent = (Content) createElement(page, Content.class.getSimpleName(), 0, div);

					divContent.setProperty(Content.content, bodyText);

					return page;

				}

			});

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

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	private AbstractNode createElement(final AbstractNode page, final String type, final int position, final AbstractNode parentElement) throws FrameworkException {

		return createElement(page, type, position, parentElement, null);

	}

	private AbstractNode createElement(final AbstractNode page, final String type, final int position, final AbstractNode parentElement, final String name) throws FrameworkException {

		PropertyMap nodeData = new PropertyMap();

		nodeData.put(AbstractNode.name, name != null
						? name
						: type.toLowerCase());
		nodeData.put(AbstractNode.visibleToPublicUsers, true);

		PropertyMap relData = new PropertyMap();
		relData.put(ChildrenRelationship.position, position);

		nodeData.put(AbstractNode.type, type);

		if (!Content.class.getSimpleName().equals(type)) {

			nodeData.put(HtmlElement.tag, type.toLowerCase());
		}

		AbstractNode element = createNodeCommand.execute(nodeData);

		if (parentElement != null) {

			createRelationshipCommand.execute(parentElement, element, RelType.CONTAINS, relData, false);
		}

		return element;

	}

}
