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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DummyNodeServiceCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Site;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Html;
import org.structr.web.importer.Importer;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.*;


public class DOMAndPageTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(DOMAndPageTest.class.getName());

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

		final PropertyKey<String> eMail = StructrApp.key(User.class, "eMail");

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(eMail, "user@structr.test")
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
				new NodeAttribute(eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(eMail, "User@Structr.test")
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
				((DOMNode) html).setProperties(((DOMNode) html).getSecurityContext(), new PropertyMap(DOMNode.visibleToPublicUsers, true));

				final Text textNode = pageOne.createTextNode("page-1");
				((DOMNode) textNode).setProperties(((DOMNode) textNode).getSecurityContext(), new PropertyMap(DOMNode.visibleToPublicUsers, true));

				pageOne.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final Page pageTwo = app.create(Page.class, "page-two");

			try {
				final Element html = pageTwo.createElement("html");
				((DOMNode) html).setProperties(((DOMNode) html).getSecurityContext(), new PropertyMap(DOMNode.visibleToPublicUsers, true));

				final Text textNode = pageTwo.createTextNode("page-2");
				((DOMNode) textNode).setProperties(((DOMNode) textNode).getSecurityContext(), new PropertyMap(DOMNode.visibleToPublicUsers, true));

				pageTwo.appendChild(html);
				html.appendChild(textNode);

			} catch (DOMException dex) {
				logger.warn("", dex);
				throw new FrameworkException(422, dex.getMessage());
			}

			final PropertyMap siteOneProperties        = new PropertyMap();
			final PropertyKey<Iterable<Site>> sitesKey = StructrApp.key(Page.class, "sites");
			final PropertyKey<Integer> positionKey     = StructrApp.key(Page.class, "position");
			final PropertyKey<Integer> portKey         = StructrApp.key(Site.class, "port");
			final PropertyKey<String> hostnameKey      = StructrApp.key(Site.class, "hostname");

			siteOneProperties.put(Site.name, "site-one");
			siteOneProperties.put(Site.visibleToPublicUsers, true);
			siteOneProperties.put(hostnameKey, "localhost");
			siteOneProperties.put(portKey, httpPort);

			final PropertyMap siteTwoProperties = new PropertyMap();
			siteTwoProperties.put(Site.name, "site-two");
			siteTwoProperties.put(Site.visibleToPublicUsers, true);
			siteTwoProperties.put(hostnameKey, "127.0.0.1");
			siteTwoProperties.put(portKey, httpPort);

			final Site siteOne = app.create(Site.class, siteOneProperties);
			final Site siteTwo = app.create(Site.class, siteTwoProperties);

			final PropertyMap pageOneProperties = new PropertyMap();
			pageOneProperties.put(sitesKey, Arrays.asList(siteOne));
			pageOneProperties.put(Page.visibleToPublicUsers, true);
			pageOneProperties.put(positionKey, 10);
			pageOne.setProperties(pageOne.getSecurityContext(), pageOneProperties);

			final PropertyMap pageTwoProperties = new PropertyMap();
			pageTwoProperties.put(sitesKey, Arrays.asList(siteTwo));
			pageTwoProperties.put(Page.visibleToPublicUsers, true);
			pageTwoProperties.put(positionKey, 10);
			pageTwo.setProperties(pageTwo.getSecurityContext(), pageTwoProperties);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

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
			.get("http://localhost:" + httpPort);

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
			.get("http://127.0.0.1:" + httpPort);


		RestAssured.basePath = "/structr/rest";
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
					assertEquals("PAGE", r.getRelType().name());

				}

				html.appendChild(head);

				for (AbstractRelationship r : head.getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertEquals("CONTAINS", r.getRelType().name());

				}

				head.appendChild(title);
				title.appendChild(titleText);

				for (AbstractRelationship r : ((DOMNode) titleText).getIncomingRelationships()) {
					System.out.println("============ Relationship: " + r.toString());
					assertEquals("CONTAINS", r.getRelType().name());

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

			assertTrue("Iteration of 100 nodes via getNextSibling should not take longer than 200ms, took " + duration + "!", duration < 200);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");

		}

	}

	@Test
	public void test01PathProperty() {

		// create a folder and a subfolder

		String folder01 = createEntityAsSuperUser("/Folder", "{ name: 'folder 01', visibleToPublicUsers: true }");
		String folder02 = createEntityAsSuperUser("/Folder", "{ name: 'folder 02', visibleToPublicUsers: true, parent: '" + folder01 + "'}");

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
				.get("/Folder?name=folder 01");


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
				.get("/Folder?name=folder 02");

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
				.get("/Folder?path=/folder 01");

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
				.get("/Folder?path=/folder 01/folder 02");
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

				final PropertyMap pageProperties = new PropertyMap();
				pageProperties.put(StructrApp.key(Page.class, "showOnErrorCodes"), "404");
				pageProperties.put(StructrApp.key(Page.class, "position"), 0);
				page.setProperties(page.getSecurityContext(), pageProperties);

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
			for (long i = 1; i <= 5000; i++) {
				if (i % 1000 == 0) {
					logger.info("Making connection #{}", i);
				}
				doc = Jsoup.connect(baseUri + pageName).timeout(0).get();
			}

			final long max = 1000;



			long t0 = System.currentTimeMillis();

			for (long i = 0; i < max; i++) {
				doc = Jsoup.connect(baseUri).timeout(0).get();
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

			logger.warn("", ex);

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
			File test1 = null;

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

				final List<File> files = app.nodeQuery(File.class).sort(StructrApp.key(File.class, "path")).getAsList();

				assertEquals("Invalid indexing sort result", "aaaaa", files.get(0).getName());
				assertEquals("Invalid indexing sort result", "bbbbb", files.get(1).getName());
				assertEquals("Invalid indexing sort result", "ccccc", files.get(2).getName());
				assertEquals("Invalid indexing sort result", "ddddd", files.get(3).getName());
				assertEquals("Invalid indexing sort result", "eeeee", files.get(4).getName());

				tx.success();
			}


			// modify file name to move the first file to the end of the sorted list
			try (final Tx tx = app.tx()) {

				test1.setProperties(test1.getSecurityContext(), new PropertyMap(AbstractNode.name, "zzzzz"));

				tx.success();
			}


			// check final sort order
			try (final Tx tx = app.tx()) {

				final List<File> files = app.nodeQuery(File.class).sort(StructrApp.key(File.class, "path")).getAsList();

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
	public void testHttpResponseHeaders() {

		try (final Tx tx = app.tx()) {

			Page.createSimplePage(securityContext, "test");

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unepxected exception.");
		}

		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.response()
			.contentType("text/html")
			.header("Expires", "Thu, 01 Jan 1970 00:00:00 GMT")
			.header("X-Structr-Edition", "Community")
			.header("Cache-Control", "private, max-age=0, s-maxage=0, no-cache, no-store, must-revalidate")
			.header("Pragma", "no-cache, no-store")
			.header("Content-Type", "text/html;charset=utf-8")
			.header("Strict-Transport-Security", "max-age=60")
			.header("X-Content-Type-Options", "nosniff")
			.header("X-Frame-Options", "SAMEORIGIN")
			.header("X-XSS-Protection", "1;mode=block")
			.header("Vary", "Accept-Encoding")
			.header("Content-Length", "133")
			.statusCode(200)
			.when()
			.get(baseUri + "test");
	}

	@Test
	public void testIncreasePageVersion() {

		Page page = null;

		try (final Tx tx = app.tx()) {

			page = Page.createSimplePage(securityContext, "test");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unepxected exception.");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Page version is not increased on modification", 0, page.getVersion());

			final Element div = page.createElement("div");

			// add new element
			page.getElementsByTagName("div").item(0).appendChild(div);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unepxected exception.");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Page version is not increased on modification", 3, page.getVersion());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unepxected exception.");
		}
	}

	@Test
	public void testIncreaseFileVersion() {

		File file = null;

		try (final Tx tx = app.tx()) {

			file = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) { }

		try (final Tx tx = app.tx()) {

			assertEquals("File version is not increased on modification", Integer.valueOf(0), file.getVersion());

			try (final OutputStream os = file.getOutputStream(true, true)) {

				os.write("test".getBytes("utf-8"));
				os.flush();

			} catch (IOException ioex) {
			}

			assertEquals("File version is not increased on modification", Integer.valueOf(1), file.getVersion());

			tx.success();

		} catch (FrameworkException fex) { }
	}

	@Test
	public void testNameCheckInDOMNode() {

		try (final Tx tx = app.tx()) {

			Page.createSimplePage(securityContext, "te/st");

			tx.success();

			fail("DOMNode names may not contain the slash character.");

		} catch (FrameworkException fex) { }
	}

	@Test
	public void testPublicAccessToSharedComponent() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "testExceptionHandling");
			final Html html         = createElement(page, page, "html");
			final Body body         = createElement(page, html, "body");
			final Div div1          = createElement(page, body, "div");
			final Content content1  = createContent(page, div1, "content");
			final DOMNode component = new CreateComponentCommand().create(div1);

			makePublic(page, html, body, div1, content1, component);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.body("html.body.div",   Matchers.equalTo("content"))
			.statusCode(200)
			.when()
			.get("/html/testExceptionHandling");
	}

	@Test
	public void testSharedComponentShowConditionError() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page         = Page.createNewPage(securityContext, "testExceptionHandling");
			final Html html         = createElement(page, page, "html");
			final Body body         = createElement(page, html, "body");
			final Div div1          = createElement(page, body, "div");
			final Content content1  = createContent(page, div1, "content");
			final DOMNode component = new CreateComponentCommand().create(div1);

			component.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "assert(1, 2, 3)");

			makePublic(page, html, body, div1, content1, component);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.body("html.body.div", Matchers.equalTo("content"))
			.statusCode(200)
			.when()
			.get("/html/testExceptionHandling");
	}

	@Test
	public void testAccessFlagsForPages() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page pub  = Page.createSimplePage(securityContext, "pub");
			final Page auth = Page.createSimplePage(securityContext, "auth");

			setFlagsRecursively(pub,   true, false);
			setFlagsRecursively(auth, false,  true);

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "tester"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "test")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// test anonymous user
		RestAssured.given().expect().statusCode(200).when().get("/html/pub");
		RestAssured.given().expect().statusCode(404).when().get("/html/auth");

		// test authenticated user
		RestAssured.given().header("X-User", "tester").header("X-Password", "test").expect().statusCode(404).when().get("/html/pub");
		RestAssured.given().header("X-User", "tester").header("X-Password", "test").expect().statusCode(200).when().get("/html/auth");

	}

	@Test
	public void testPagePerformance() {

		// setup
		try (final Tx tx = app.tx()) {

			final String address    = "http://structr.github.io/structr/getbootstrap.com/docs/3.3/examples/jumbotron/";
			final Importer importer = new Importer(securityContext, null, address, "test", true, true, false, false);

			importer.parse();

			// create page from source
			final Page test = importer.readPage();

			setFlagsRecursively(test, true, false);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		long average = 0;
		long count   = 0;

		// test anonymous user
		for (int i=0; i<100; i++) {

			final long t0 = System.currentTimeMillis();

			RestAssured.given().expect().statusCode(200).when().get("/html/test");

			final long t1 = System.currentTimeMillis();

			average += (t1 - t0);
			count++;
		}

		logger.info("Rendering a test page {} times took {} ms on average", count, (average / count));
	}

	@Test
	public void testDetailsObjectInPageUrlWithCaching() {

		String uuid = null;

		// setup
		try (final Tx tx = app.tx()) {

			final Page test  = Page.createSimplePage(securityContext, "test");
			final Text text  = test.createTextNode(" / ${current.id}");

			test.getElementsByTagName("div").item(0).appendChild(text);

			final User user = app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			uuid = user.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.body("html.body.div",   Matchers.equalTo("Initial body text / " + uuid))
			.statusCode(200)
			.when()
			.get("/html/test/" + uuid);

		// make sure that the second request (which comes from the cache) also contains the details object!
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.body("html.body.div",   Matchers.equalTo("Initial body text / " + uuid))
			.statusCode(200)
			.when()
			.get("/html/test/" + uuid);
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

	// copied from DeploymentTestBase (sorry)
	protected <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Node node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}

	// copied from DeploymentTestBase (sorry)
	protected <T> T createContent(final Page page, final DOMNode parent, final String content) {

		final T child = (T)page.createTextNode(content);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private void setFlagsRecursively(final DOMNode node, final boolean visibleToPublic, final boolean visibleToAuth) throws FrameworkException {

		node.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), visibleToAuth);
		node.setProperty(StructrApp.key(DOMNode.class, "visibleToPublicUsers"), visibleToPublic);

		for (final DOMNode child : node.getChildren()) {

			setFlagsRecursively(child, visibleToPublic, visibleToAuth);
		}
	}
}
