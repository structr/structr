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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.Site;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.AssertJUnit.fail;

/**
 */
public class SiteTest extends StructrUiTest {

	@Test
	public void test01BasicSites() {

		try (final Tx tx = app.tx()) {

			final Site site1 = createTestNode(Site.class, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "site1"), new NodeAttribute<>(StructrApp.key(Site.class, "hostname"), "test1.example.com"));
			final Site site2 = createTestNode(Site.class, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "site2"), new NodeAttribute<>(StructrApp.key(Site.class, "hostname"), "test2.example.com"));

			site1.setProperty(StructrApp.key(Site.class, "visibleToAuthenticatedUsers"), true);
			site2.setProperty(StructrApp.key(Site.class, "visibleToAuthenticatedUsers"), true);
			site1.setProperty(StructrApp.key(Site.class, "visibleToPublicUsers"), true);
			site2.setProperty(StructrApp.key(Site.class, "visibleToPublicUsers"), true);

			final Page page1 = Page.createSimplePage(securityContext, "site1page1");
			final Page page2 = Page.createSimplePage(securityContext, "site1page2");
			final Page page3 = Page.createSimplePage(securityContext, "site2page1");
			final Page page4 = Page.createSimplePage(securityContext, "site2page2");

			makePublicRecursively(page1);
			makePublicRecursively(page2);
			makePublicRecursively(page3);
			makePublicRecursively(page4);

			page1.setProperty(StructrApp.key(Page.class, "position"), 10);
			page2.setProperty(StructrApp.key(Page.class, "position"), 10);
			page3.setProperty(StructrApp.key(Page.class, "position"), 10);
			page4.setProperty(StructrApp.key(Page.class, "position"), 10);

			page1.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site1));
			page2.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site1));
			page3.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site2));
			page4.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site2));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// tests
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/site1page1");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/site1page2");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/site2page1");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/site2page2");

		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(404).when().get("/site1page1");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(404).when().get("/site1page2");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(200).when().get("/site2page1");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(200).when().get("/site2page2");
	}

	@Test
	public void test02Sites() {

		try (final Tx tx = app.tx()) {

			final Site site1 = createTestNode(Site.class, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "site1"), new NodeAttribute<>(StructrApp.key(Site.class, "hostname"), "test1.example.com"));
			final Site site2 = createTestNode(Site.class, new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "site2"), new NodeAttribute<>(StructrApp.key(Site.class, "hostname"), "test2.example.com"));

			site1.setProperty(StructrApp.key(Site.class, "visibleToAuthenticatedUsers"), true);
			site2.setProperty(StructrApp.key(Site.class, "visibleToAuthenticatedUsers"), true);
			site1.setProperty(StructrApp.key(Site.class, "visibleToPublicUsers"), true);
			site2.setProperty(StructrApp.key(Site.class, "visibleToPublicUsers"), true);

			final Page page1 = Page.createSimplePage(securityContext, "site1page1");
			final Page page2 = Page.createSimplePage(securityContext, "site1page2");
			final Page page3 = Page.createSimplePage(securityContext, "site2page1");
			final Page page4 = Page.createSimplePage(securityContext, "site2page2");

			//makePublicRecursively(page1);
			makePublicRecursively(page2);
			//makePublicRecursively(page3);
			makePublicRecursively(page4);

			page1.setProperty(StructrApp.key(Page.class, "position"), 10);
			page2.setProperty(StructrApp.key(Page.class, "position"), 10);
			page3.setProperty(StructrApp.key(Page.class, "position"), 10);
			page4.setProperty(StructrApp.key(Page.class, "position"), 10);

			page1.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site1));
			page2.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site1));
			page3.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site2));
			page4.setProperty(StructrApp.key(Page.class, "sites"), Arrays.asList(site2));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// tests
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/site1page1");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/site1page2");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/site2page1");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/site2page2");

		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(404).when().get("/site1page1");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(404).when().get("/site1page2");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(404).when().get("/site2page1");
		RestAssured.given().header("Host", "test2.example.com").expect().statusCode(200).when().get("/site2page2");
	}

	private void makePublicRecursively(final DOMNode node) throws FrameworkException {

		node.setProperty(StructrApp.key(DOMNode.class, "visibleToAuthenticatedUsers"), true);
		node.setProperty(StructrApp.key(DOMNode.class, "visibleToPublicUsers"), true);

		for (final DOMNode child : node.getChildren()) {

			makePublicRecursively(child);
		}
	}
}
