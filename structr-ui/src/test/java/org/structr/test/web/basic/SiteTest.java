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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.SiteTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.AssertJUnit.fail;

/**
 */
public class SiteTest extends StructrUiTest {

	@Test
	public void test01BasicSites() {

		try (final Tx tx = app.tx()) {

			final NodeInterface site1 = createTestNode(StructrTraits.SITE,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "site1"),
					new NodeAttribute<>(Traits.of(StructrTraits.SITE).key(SiteTraitDefinition.HOSTNAME_PROPERTY), "test1.example.com")
			);
			final NodeInterface site2 = createTestNode(StructrTraits.SITE,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "site2"),
					new NodeAttribute<>(Traits.of(StructrTraits.SITE).key(SiteTraitDefinition.HOSTNAME_PROPERTY), "test2.example.com")
			);

			site1.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			site2.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			site1.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			site2.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			final Page page1 = Page.createSimplePage(securityContext, "site1page1");
			final Page page2 = Page.createSimplePage(securityContext, "site1page2");
			final Page page3 = Page.createSimplePage(securityContext, "site2page1");
			final Page page4 = Page.createSimplePage(securityContext, "site2page2");

			makePublicRecursively(page1);
			makePublicRecursively(page2);
			makePublicRecursively(page3);
			makePublicRecursively(page4);

			page1.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page2.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page3.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page4.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);

			page1.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site1));
			page2.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site1));
			page3.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site2));
			page4.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site2));

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

			final NodeInterface site1 = createTestNode(StructrTraits.SITE,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "site1"),
					new NodeAttribute<>(Traits.of(StructrTraits.SITE).key(SiteTraitDefinition.HOSTNAME_PROPERTY), "test1.example.com")
			);
			final NodeInterface site2 = createTestNode(StructrTraits.SITE,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "site2"),
					new NodeAttribute<>(Traits.of(StructrTraits.SITE).key(SiteTraitDefinition.HOSTNAME_PROPERTY), "test2.example.com")
			);

			site1.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			site2.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			site1.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			site2.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			final Page page1 = Page.createSimplePage(securityContext, "site1page1");
			final Page page2 = Page.createSimplePage(securityContext, "site1page2");
			final Page page3 = Page.createSimplePage(securityContext, "site2page1");
			final Page page4 = Page.createSimplePage(securityContext, "site2page2");

			//makePublicRecursively(page1);
			makePublicRecursively(page2);
			//makePublicRecursively(page3);
			makePublicRecursively(page4);

			page1.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page2.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page3.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);
			page4.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);

			page1.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site1));
			page2.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site1));
			page3.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site2));
			page4.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site2));

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

		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

		for (final DOMNode child : node.getChildren()) {

			makePublicRecursively(child);
		}
	}
}
