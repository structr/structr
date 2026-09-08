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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the server-side building blocks that frontend.js uses to resolve partial reload
 * targets that are addressed by HTML id or CSS class instead of data-structr-id.
 */
public class ReloadTargetLookupTest extends StructrUiTest {

	@Test
	public void testTriggerElementCarriesPageId() {

		String pageId = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMElement btn = page1.createElement("button");

			div.appendChild(btn);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			pageId = page1.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		final Document doc = Jsoup.parse(fetchPageHtml("/html/page1"));

		// frontend.js needs the ID of the current page to restrict the lookup of reload targets by HTML id / class
		assertEquals("Trigger element must carry the page ID", pageId, doc.getElementById("button").attr("data-structr-page"));
		assertEquals("Passive elements must not carry the page ID", "", doc.selectFirst("html").attr("data-structr-page"));
	}

	@Test
	public void testHtmlIdLookupCanBeRestrictedToPage() {

		String page2Id = null;
		String div2Id  = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1  = Page.createSimplePage(securityContext, "page1");
			final Page page2  = Page.createSimplePage(securityContext, "page2");
			final DOMNode div1 = page1.getElementsByTagName("div").get(0);
			final DOMNode div2 = page2.getElementsByTagName("div").get(0);

			div1.setProperty(Traits.of("Div").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "shared");
			div2.setProperty(Traits.of("Div").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "shared");

			// element with the same HTML id in the trash (no page)
			app.create("Div", new NodeAttribute<>(Traits.of("Div").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "shared"));

			page2Id = page2.getUuid();
			div2Id  = div2.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";

		// the lookup by HTML id alone is ambiguous
		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(3))
			.when()
				.get("/DOMElement/ui?_html_id=shared");

		// restricted to the page, only the element of that page is found
		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result_count",     equalTo(1))
				.body("result[0].id",     equalTo(div2Id))
				.body("result[0].pageId", equalTo(page2Id))
			.when()
				.get("/DOMElement/ui?_html_id=shared&pageId=" + page2Id);
	}

	@Test
	public void testHtmlClassLookupWithInexactSearch() {

		String pageId = null;
		String divId  = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1  = Page.createSimplePage(securityContext, "page1");
			final DOMNode div = page1.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of("Div").key(DOMElementTraitDefinition._HTML_CLASS_PROPERTY), "container fluid main");

			pageId = page1.getUuid();
			divId  = div.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";

		// exact search does not find an element with additional classes
		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result_count", equalTo(0))
			.when()
				.get("/DOMElement/ui?_html_class=fluid");

		// inexact search finds it, combined with the page restriction and multiple classes (AND)
		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result_count",          equalTo(1))
				.body("result[0].id",          equalTo(divId))
				.body("result[0]._html_class", equalTo("container fluid main"))
			.when()
				.get("/DOMElement/ui?_html_class=main,fluid&_inexact=1&pageId=" + pageId);
	}

	// ----- private methods -----
	private String fetchPageHtml(final String path) {

		return RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get(path)
			.andReturn()
				.body().asString();
	}
}
