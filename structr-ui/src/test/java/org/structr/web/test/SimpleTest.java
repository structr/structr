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

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalTo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.DummyNodeServiceCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.schema.importer.GraphGistImporter;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Site;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.relation.PageLink;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 *
 * @author Christian Morgner
 */
public class SimpleTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class.getName());

	@Test
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

			logger.warn("", ex);

			logger.error(ex.toString());
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

			logger.warn("", ex);

			logger.error(ex.toString());
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected Exception");

		}

	}

	@Test
	public void test001EMailAddressConstraint() {

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "User@Structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();
	}

	@Test
	public void testSites() {

		try (final Tx tx = app.tx()) {

			// setup two pages and two sites
			// page one -> site one, listens on one:8875
			// page two -> site two, listens on two:8875
			final Page pageOne = app.create(Page.class, "page-one");

			try {
				final Element html = pageOne.createElement("html");
				((DOMNode) html).setProperty(DOMNode.visibleToPublicUsers, true);

				final Text textNode = pageOne.createTextNode("page-1");
				((DOMNode) textNode).setProperty(DOMNode.visibleToPublicUsers, true);

				pageOne.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final Page pageTwo = app.create(Page.class, "page-two");

			try {
				final Element html = pageTwo.createElement("html");
				((DOMNode) html).setProperty(DOMNode.visibleToPublicUsers, true);

				final Text textNode = pageTwo.createTextNode("page-2");
				((DOMNode) textNode).setProperty(DOMNode.visibleToPublicUsers, true);

				pageTwo.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final Site siteOne = app.create(Site.class, "site-one");
			siteOne.setProperty(Site.visibleToPublicUsers, true);

			final Site siteTwo = app.create(Site.class, "site-two");
			siteTwo.setProperty(Site.visibleToPublicUsers, true);

			pageOne.setProperty(Page.site, siteOne);
			pageOne.setProperty(Page.visibleToPublicUsers, true);
			pageOne.setProperty(Page.position, 10);

			pageTwo.setProperty(Page.site, siteTwo);
			pageTwo.setProperty(Page.visibleToPublicUsers, true);
			pageTwo.setProperty(Page.position, 10);

			siteOne.setProperty(Site.hostname, "localhost");
			siteOne.setProperty(Site.port, 8875);

			siteTwo.setProperty(Site.hostname, "127.0.0.1");
			siteTwo.setProperty(Site.port, 8875);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}


		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString("page-1"))
			.when()
			.get("http://localhost:8875/");

		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.expect()
			.response()
			.contentType("text/html")
			.statusCode(200)
			.body(Matchers.containsString("page-2"))
			.when()
			.get("http://127.0.0.1:8875/");

	}

	@Test
	public void test01DOMChildren() {

		final String pageName	= "page-01";
		final String pageTitle	= "Page Title";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);
			if (page != null) {

				DOMElement html  = (DOMElement) page.createElement("html");
				DOMElement head  = (DOMElement) page.createElement("head");
				DOMElement title = (DOMElement) page.createElement("title");
				Text titleText   = page.createTextNode(pageTitle);

				for (AbstractRelationship r : page.getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof PageLink);

				}

				html.appendChild(head);

				for (AbstractRelationship r : head.getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof DOMChildren);

				}

				head.appendChild(title);
				title.appendChild(titleText);

				for (AbstractRelationship r : ((DOMNode) titleText).getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertTrue(r instanceof DOMChildren);

				}



			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void testSiblingPerformance() {

		try (final Tx tx = app.tx()) {

			// create document
			final org.w3c.dom.Document document = getDocument();
			assertNotNull(document);

			// create div
			final Element div = document.createElement("div");
			assertNotNull(div);

			try {

				app.command(DummyNodeServiceCommand.class).bulkTransaction(securityContext, 1000, new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						div.appendChild(document.createTextNode("test"));

						return null;
					}

				}, new Predicate<Long>() {

					@Override
					public boolean accept(Long obj) {

						return obj.longValue() > 100;
					}

				});

			} catch (Throwable t) {

				fail("Unexpected exception: " + t.getMessage());
			}

			// iterate over all siblings using the nextSibling method
			long t0 = System.currentTimeMillis();

			Node it = div.getFirstChild();
			while (it != null) {
				it = it.getNextSibling();
			}

			long t1 = System.currentTimeMillis();
			long duration = t1 - t0;

			assertTrue("Iteration of 100 nodes via getNextSibling should not take longer than 50ms, took " + duration + "!", duration < 50);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");

		}

	}

	@Test
	public void test01PathProperty() {

		// create a folder and a subfolder

		String folder01 = createEntityAsSuperUser("/folder", "{ name: 'folder 01', visibleToPublicUsers: true }");
		String folder02 = createEntityAsSuperUser("/folder", "{ name: 'folder 02', visibleToPublicUsers: true, parent: '" + folder01 + "'}");

		grant("Folder", 4095, true);

		// find folder by name
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder01))

			.when()
				.get("/folder?name=folder 01");


		// find subfolder by name
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder02))

			.when()
				.get("/folder?name=folder 02");

		// find folder by path
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder01))

			.when()
				.get("/folder?path=/folder 01");

		// find subfolder by path
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))

			.expect()
				.statusCode(200)

				.body("result[0].id", equalTo(folder02))

			.when()
				.get("/folder?path=/folder 01/folder 02");
	}

	@Test
	public void test01PagePerformance() {

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

				page.setProperty(Page.showOnErrorCodes, "404");
				page.setProperty(Page.position, 0);

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
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

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected Exception");
		}

		assertTrue(page != null);
		assertTrue(page instanceof Page);

		try (final Tx tx = app.tx()) {

			Document doc = null;


			// Warm-up caches and JVM
			for (long i = 0; i < 50000; i++) {
				doc = Jsoup.connect(baseUri + pageName).get();
			}

			final long max = 1000;



			long t0 = System.currentTimeMillis();

			for (long i = 0; i < max; i++) {
				doc = Jsoup.connect(baseUri).get();
			}

			long t1 = System.currentTimeMillis();

			DecimalFormat decimalFormat = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000.0;
			Double rate                 = max / ((t1 - t0) / 1000.0);

			logger.info("------> Time to render {} the base URI: {} seconds ({} per s)", new Object[] { max, decimalFormat.format(time), decimalFormat.format(rate) });

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



			t0 = System.currentTimeMillis();

			for (long i = 0; i < max; i++) {
				doc = Jsoup.connect(baseUri + pageName).get();
			}

			t1 = System.currentTimeMillis();

			time                 = (t1 - t0) / 1000.0;
			rate                 = max / ((t1 - t0) / 1000.0);

			logger.info("------> Time to render {} the test page by name: {} seconds ({} per s)", new Object[] { max, decimalFormat.format(time), decimalFormat.format(rate) });


			assertFalse(doc.select("html").isEmpty());
			assertFalse(doc.select("html > head").isEmpty());
			assertFalse(doc.select("html > head > title").isEmpty());
			assertFalse(doc.select("html > body").isEmpty());

			assertEquals(doc.select("html > head > title").first().text(), pageTitle);

			h1Elements = doc.select("html > body > h1");
			assertFalse(h1Elements.isEmpty());
			assertEquals(h1Elements.first().text(), pageTitle);
			assertEquals(h1Elements.first().attr("class"), h1ClassAttr);

			divElements = doc.select("html > body > div");
			assertFalse(divElements.isEmpty());
			assertEquals(divElements.first().text(), bodyText);
			assertEquals(divElements.first().attr("class"), divClassAttr);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();

			logger.error(ex.toString());
			fail("Unexpected Exception");

		}

	}

	@Test
	public void testPagePath() {

		final String pageName	= "page-01";

		try (final Tx tx = app.tx()) {

			Page page = Page.createNewPage(securityContext, pageName);

			assertTrue(page != null);
			assertTrue(page instanceof Page);

			DOMElement html   = (DOMElement)page.createElement("html");
			DOMElement head   = (DOMElement)page.createElement("head");
			DOMElement body   = (DOMElement)page.createElement("body");
			DOMElement title  = (DOMElement)page.createElement("title");
			DOMElement div    = (DOMElement)page.createElement("div");
			DOMElement div_2  = (DOMElement)page.createElement("div");
			DOMElement div_3  = (DOMElement)page.createElement("div");
			DOMElement h1     = (DOMElement)page.createElement("h1");
			DOMElement h1_2   = (DOMElement)page.createElement("h1");

			try {
				// add HTML element to page
				page.appendChild(html);

				// add HEAD and BODY elements to HTML
				html.appendChild(head);
				html.appendChild(body);

				// add TITLE element to HEAD
				head.appendChild(title);
				title.appendChild(page.createTextNode("Test Page"));

				// add DIVs to BODY
				body.appendChild(div);
				body.appendChild(div_2);
				body.appendChild(div_3);

				// add H1 elements to DIV
				div_3.appendChild(h1);
				div_3.appendChild(h1_2);
				h1.appendChild(page.createTextNode("Page Title"));

			} catch (DOMException dex) {

				throw new FrameworkException(422, dex.getMessage());
			}

			assertEquals(html.getPositionPath(),	"/0");
			assertEquals(head.getPositionPath(),	"/0/0");
			assertEquals(title.getPositionPath(),	"/0/0/0");
			assertEquals(body.getPositionPath(),	"/0/1");
			assertEquals(div.getPositionPath(),	"/0/1/0");
			assertEquals(div_2.getPositionPath(),	"/0/1/1");
			assertEquals(div_3.getPositionPath(),	"/0/1/2");
			assertEquals(h1.getPositionPath(),	"/0/1/2/0");
			assertEquals(h1_2.getPositionPath(),	"/0/1/2/1");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");

		}
	}

	@Test
	public void test08PassiveIndexing() {

		try {

			// create some test nodes
			FileBase test1 = null;

			// check initial sort order
			try (final Tx tx = app.tx()) {

				// create some test nodes
				test1 = createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "aaaaa"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "bbbbb"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "ccccc"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "ddddd"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "eeeee"));

				tx.success();
			}

			// check initial sort order
			try (final Tx tx = app.tx()) {

				final List<FileBase> files = app.nodeQuery(FileBase.class).sort(File.path).getAsList();

				assertEquals("Invalid indexing sort result", "aaaaa", files.get(0).getName());
				assertEquals("Invalid indexing sort result", "bbbbb", files.get(1).getName());
				assertEquals("Invalid indexing sort result", "ccccc", files.get(2).getName());
				assertEquals("Invalid indexing sort result", "ddddd", files.get(3).getName());
				assertEquals("Invalid indexing sort result", "eeeee", files.get(4).getName());

				tx.success();
			}


			// modify file name to move the first file to the end of the sorted list
			try (final Tx tx = app.tx()) {

				test1.setProperty(AbstractNode.name, "zzzzz");

				tx.success();
			}


			// check final sort order
			try (final Tx tx = app.tx()) {

				final List<FileBase> files = app.nodeQuery(FileBase.class).sort(File.path).getAsList();

				assertEquals("Invalid indexing sort result", "bbbbb", files.get(0).getName());
				assertEquals("Invalid indexing sort result", "ccccc", files.get(1).getName());
				assertEquals("Invalid indexing sort result", "ddddd", files.get(2).getName());
				assertEquals("Invalid indexing sort result", "eeeee", files.get(3).getName());
				assertEquals("Invalid indexing sort result", "zzzzz", files.get(4).getName());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void testImportAndSchemaAnalyzer() {

		final GraphGistImporter importer = app.command(GraphGistImporter.class);
		final String source =
			"== Test setup\n" +
			"\n" +
			"[source, cypher]\n" +
			"----\n" +
			"CREATE (c:Company { name: 'Company 1', comp_id: '12345', string_name: 'company1', year: 2013, month: 6, day: 7, status: 'test'})\n" +
			"CREATE (p:Company { name: 'Company 2'})\n" +
			"----\n";

		final List<String> sourceLines = importer.extractSources(new ByteArrayInputStream(source.getBytes(Charset.forName("utf-8"))));

		// import (uses Neo4j transaction)
		importer.importCypher(sourceLines);
		importer.analyzeSchema();

		try (final Tx tx = app.tx()) {

			final SchemaNode schemaNode           = app.nodeQuery(SchemaNode.class).andName("Company").getFirst();
			final List<SchemaProperty> properties = schemaNode.getProperty(SchemaNode.schemaProperties);
			final Map<String, SchemaProperty> map = new HashMap<>();

			for (final SchemaProperty prop : properties) {
				map.put(prop.getProperty(SchemaProperty.name), prop);
			}

			assertNotNull("A schema node with name 'Company' should have been created: ", schemaNode);

			assertEquals("Company schema node should have a 'name' property with value 'String': ",        "String", map.get("name").getPropertyType().name());
			assertEquals("Company schema node should have a 'comp_id' property with value 'String': ",     "String", map.get("comp_id").getPropertyType().name());
			assertEquals("Company schema node should have a 'string_name' property with value 'String': ", "String", map.get("string_name").getPropertyType().name());
			assertEquals("Company schema node should have a 'year' property with value 'Long': ",          "Long",   map.get("year").getPropertyType().name());
			assertEquals("Company schema node should have a 'month' property with value 'Long': ",         "Long",   map.get("month").getPropertyType().name());
			assertEquals("Company schema node should have a 'day' property with value 'Long': ",           "Long",   map.get("day").getPropertyType().name());
			assertEquals("Company schema node should have a 'status' property with value 'String': ",      "String", map.get("status").getPropertyType().name());

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


	}

	// ----- private methods -----
	private void check() {

		try (final Tx tx = app.tx()) {

			final List<User> users = app.nodeQuery(User.class).getAsList();

			assertEquals("Expected no users to be created because of constraints", 0, users.size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

	}

	private org.w3c.dom.Document getDocument() {

		try {

			List<Page> pages = this.createTestNodes(Page.class, 1);

			if (!pages.isEmpty()) {

				return pages.get(0);
			}

		} catch (FrameworkException fex) {

			logger.warn("Unable to create test data", fex);
		}

		return null;


	}
}
