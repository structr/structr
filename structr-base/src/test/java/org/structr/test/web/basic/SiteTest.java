/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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

	@Test
	public void test03PageWithoutSiteStaysReachableOnUnclaimedHosts() {

		try (final Tx tx = app.tx()) {

			createPublicPage("sitepage", createSite("site1", "test1.example.com", null));
			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// the host this site claims: only its own page is served there
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/orphanpage");

		// a host no site claims: the page without a site has to stay reachable
		RestAssured.given().header("Host", "other.example.com").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "other.example.com").expect().statusCode(200).when().get("/orphanpage");
	}

	@Test
	public void test04SitePortIsPartOfTheMatch() {

		try (final Tx tx = app.tx()) {

			createPublicPage("sitepage", createSite("site1", "test1.example.com", 8875));
			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// hostname and port both match
		RestAssured.given().header("Host", "test1.example.com:8875").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com:8875").expect().statusCode(404).when().get("/orphanpage");

		// right hostname, wrong port: the site does not claim this request
		RestAssured.given().header("Host", "test1.example.com:9999").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com:9999").expect().statusCode(200).when().get("/orphanpage");

		// right port, wrong hostname
		RestAssured.given().header("Host", "other.example.com:8875").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "other.example.com:8875").expect().statusCode(200).when().get("/orphanpage");
	}

	@Test
	public void test05SiteWithNeitherHostnameNorPortClaimsNothing() {

		try (final Tx tx = app.tx()) {

			createPublicPage("sitepage", createSite("site1", null, null));
			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// a misconfigured site never becomes visible, and it claims no host from anyone else
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/orphanpage");

		RestAssured.given().header("Host", "other.example.com:9999").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "other.example.com:9999").expect().statusCode(200).when().get("/orphanpage");
	}

	@Test
	public void test06SiteWithPortOnlyClaimsThatPortOnEveryHost() {

		try (final Tx tx = app.tx()) {

			createPublicPage("sitepage", createSite("site1", null, 8876));
			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// the site configures only a port, so that port is claimed whatever the hostname is
		RestAssured.given().header("Host", "test1.example.com:8876").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com:8876").expect().statusCode(404).when().get("/orphanpage");

		RestAssured.given().header("Host", "other.example.com:8876").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "other.example.com:8876").expect().statusCode(404).when().get("/orphanpage");

		// any other port is untouched by it
		RestAssured.given().header("Host", "test1.example.com:9999").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com:9999").expect().statusCode(200).when().get("/orphanpage");
	}

	@Test
	public void test07PageWithTwoSitesIsVisibleOnBoth() {

		try (final Tx tx = app.tx()) {

			final Page page = createPublicPage("sitepage", null);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY),
				Arrays.asList(createSite("site1", "test1.example.com", null), createSite("site2", "test2.example.com", 8877)));

			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		RestAssured.given().header("Host", "test1.example.com:1234").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "test2.example.com:8877").expect().statusCode(200).when().get("/sitepage");

		// the second site names a port, so the page is not served on test2 through another one
		RestAssured.given().header("Host", "test2.example.com:9999").expect().statusCode(404).when().get("/sitepage");

		// both hosts are claimed, so the page without a site is served on neither
		RestAssured.given().header("Host", "test1.example.com:1234").expect().statusCode(404).when().get("/orphanpage");
		RestAssured.given().header("Host", "test2.example.com:8877").expect().statusCode(404).when().get("/orphanpage");
		RestAssured.given().header("Host", "test2.example.com:9999").expect().statusCode(200).when().get("/orphanpage");
	}

	@Test
	public void test08SiteRoutesEvenWhenItIsNotVisibleToTheVisitor() {

		try (final Tx tx = app.tx()) {

			final NodeInterface site = createSite("site1", "test1.example.com", null);

			// which host serves a page is configuration, so an anonymous visitor not being allowed to see the
			// site node must not turn its pages into pages without a site
			site.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), false);
			site.setProperty(Traits.of(StructrTraits.SITE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);

			createPublicPage("sitepage", site);
			createPublicPage("orphanpage", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "";

		// the site still claims its host for everyone
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(200).when().get("/sitepage");
		RestAssured.given().header("Host", "test1.example.com").expect().statusCode(404).when().get("/orphanpage");

		// and still keeps its own page off every other host
		RestAssured.given().header("Host", "other.example.com").expect().statusCode(404).when().get("/sitepage");
		RestAssured.given().header("Host", "other.example.com").expect().statusCode(200).when().get("/orphanpage");
	}

	private NodeInterface createSite(final String name, final String hostname, final Integer port) throws FrameworkException {

		final Traits traits      = Traits.of(StructrTraits.SITE);
		final NodeInterface site = createTestNode(StructrTraits.SITE,
			new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name)
		);

		if (hostname != null) {
			site.setProperty(traits.key(SiteTraitDefinition.HOSTNAME_PROPERTY), hostname);
		}

		if (port != null) {
			site.setProperty(traits.key(SiteTraitDefinition.PORT_PROPERTY), port);
		}

		site.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
		site.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

		return site;
	}

	private Page createPublicPage(final String name, final NodeInterface site) throws FrameworkException {

		final Page page = Page.createSimplePage(securityContext, name);

		makePublicRecursively(page);

		page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY), 10);

		if (site != null) {
			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SITES_PROPERTY), Arrays.asList(site));
		}

		return page;
	}

	private void makePublicRecursively(final DOMNode node) throws FrameworkException {

		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
		node.setProperty(Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

		for (final DOMNode child : node.getChildren()) {

			makePublicRecursively(child);
		}
	}
}
