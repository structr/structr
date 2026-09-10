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
import org.jsoup.nodes.Element;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the rendering of partials (reload targets) via /structr/html/&lt;uuid&gt;.
 */
public class PartialReloadTest extends StructrUiTest {

	@Test
	public void testCurrentObjectInPartial() {

		String partialId = null;
		String groupId   = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div       = page1.getElementsByTagName("div").get(0);
			final DOMElement btn    = page1.createElement("button");
			final DOMElement target = page1.createElement("section");
			final Content text      = page1.createTextNode("${current.name}");

			div.appendChild(btn);
			div.appendChild(target);
			target.appendChild(text);

			target.setProperty(Traits.of("Section").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "content");

			// action mapping that reloads the section
			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY), List.of(target));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");

			// the current object
			final NodeInterface group = app.create(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "current-group"));

			partialId = target.getUuid();
			groupId   = group.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		// full page with current object
		final Document page      = Jsoup.parse(fetchHtml("/html/page1/" + groupId));
		final Element pageTarget = page.getElementById("content");

		assertNotNull("Reload target not rendered in page", pageTarget);
		assertEquals("Wrong current object in page", "current-group", pageTarget.text());
		assertEquals("Reload target must carry the current object ID", groupId, pageTarget.attr("data-current-object-id"));

		// partial with current object, like frontend.js requests it (data-current-object-id => path, request parameters => query)
		final Document partial1 = Jsoup.parse(fetchHtml("/structr/html/" + partialId + "/" + groupId));

		assertEquals("Wrong current object in partial", "current-group", partial1.getElementById("content").text());
		assertEquals("Partial must carry the current object ID", groupId, partial1.getElementById("content").attr("data-current-object-id"));

		final Document partial2 = Jsoup.parse(fetchHtml("/structr/html/" + partialId + "/" + groupId + "?page=2&structr-encoded-render-state="));

		assertEquals("Wrong current object in partial with request parameters", "current-group", partial2.getElementById("content").text());

		// partial without current object
		final Document partial3 = Jsoup.parse(fetchHtml("/structr/html/" + partialId));

		assertEquals("Partial without current object must render empty text", "", partial3.getElementById("content").text());
	}

	@Test
	public void testCurrentObjectForSelectorTarget() {

		// A reload target that is addressed by a CSS selector (#content) is not linked to the action mapping,
		// but it must be recognized as reload target at render time nevertheless (data-structr-id and
		// data-current-object-id), otherwise the current object is lost in the partial reload.
		String groupId    = null;
		String sectionId  = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page1        = Page.createSimplePage(securityContext, "page1");
			final DOMNode div       = page1.getElementsByTagName("div").get(0);
			final DOMElement btn    = page1.createElement("button");
			final DOMElement target = page1.createElement("section");
			final Content text      = page1.createTextNode("${current.name}");

			div.appendChild(btn);
			div.appendChild(target);
			target.appendChild(text);

			btn.setProperty(Traits.of("Button").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "button");
			target.setProperty(Traits.of("Section").key(DOMElementTraitDefinition._HTML_ID_PROPERTY), "content");

			final NodeInterface eam = app.create(StructrTraits.ACTION_MAPPING);

			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(btn));
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.EVENT_PROPERTY), "click");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.ACTION_PROPERTY), "create");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY), "Project");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), "partial-refresh");
			eam.setProperty(Traits.of(StructrTraits.ACTION_MAPPING).key(ActionMappingTraitDefinition.SUCCESS_PARTIAL_PROPERTY), "#content");

			final NodeInterface group = app.create(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "current-group"));

			groupId   = group.getUuid();
			sectionId = target.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		final Document page   = Jsoup.parse(fetchHtml("/html/page1/" + groupId));
		final Element button  = page.getElementById("button");
		final Element section = page.getElementById("content");

		assertEquals("Wrong success target",                                 "#content",      button.attr("data-structr-success-target"));
		assertEquals("Wrong current object in page",                         "current-group", section.text());
		assertEquals("Selector target must be rendered as reload target",    sectionId,       section.attr("data-structr-id"));
		assertEquals("Selector target must carry the current object",        groupId,         section.attr("data-current-object-id"));
		assertEquals("Trigger element must carry the current object as well", groupId,        button.attr("data-current-object-id"));

		// the partial reload as frontend.js does it with the attributes of the reload target
		final Document partial = Jsoup.parse(fetchHtml("/structr/html/" + sectionId + "/" + groupId));

		assertEquals("Wrong current object in partial", "current-group", partial.getElementById("content").text());
		assertEquals("Partial must carry the current object", groupId, partial.getElementById("content").attr("data-current-object-id"));
	}

	// ----- private methods -----
	private String fetchHtml(final String path) {

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
