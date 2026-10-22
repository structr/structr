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
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

/**
 * A page reached through a URL route: its Node parameter should be the current object, as the last path
 * segment is on a page reached the old way, and partial reloads of its elements should see the route values.
 */
public class UrlRoutingCurrentObjectTest extends StructrUiTest {

	private String projectId  = null;
	private String partialId  = null;

	@Test
	public void testRouteParameterIsTheCurrentObject() {

		setup();

		assertEquals("current=Apollo;id=" + projectId + ";project=Apollo", get("/projects/" + projectId));
	}

	@Test
	public void testRouteValuesAreAvailableInARepeater() {

		setup();

		assertEquals("Apollo in Apollo;Gemini in Apollo;", get("/projects/" + projectId + "/all"));
	}

	@Test
	public void testPartialReloadSeesTheRouteValues() {

		setup();

		// what the frontend requests for a partial with data-current-object-id on a page reached through a route
		assertEquals("current=Apollo;id=" + projectId + ";project=Apollo", get("/" + partialId + "/" + projectId + "?structr-route-path=/projects/" + projectId));

		// without the current object in the URL, the route provides it
		assertEquals("current=Apollo;id=" + projectId + ";project=Apollo", get("/" + partialId + "?structr-route-path=/projects/" + projectId));

		// a route of another page is not applied to this partial
		assertEquals("current=;id=;project=", get("/" + partialId + "?structr-route-path=/projects/" + projectId + "/all"));
	}

	@Test
	public void testMarkedParameterBecomesTheCurrentObject() throws FrameworkException {

		setup();

		final NodeInterface first;
		final NodeInterface second;

		try (final Tx tx = app.tx()) {

			final Page page       = textPage("pair");
			final Template detail = template(page, "current=${current.name}");

			final NodeInterface pagePath = path(page, "/pair/{first}/{second}");

			first  = parameter(pagePath, "first", 0, "Node", false);
			second = parameter(pagePath, "second", 1, "Node", false);

			makePublic(page, detail);

			tx.success();
		}

		final String gemini = groupId("Gemini");

		// two Node parameters and none marked: no current object
		assertEquals("current=", get("/pair/" + projectId + "/" + gemini));

		mark(second, true);

		assertEquals("current=Gemini", get("/pair/" + projectId + "/" + gemini));

		// marking another one unmarks the first
		mark(first, true);

		try (final Tx tx = app.tx()) {

			assertFalse(second.as(PagePathParameter.class).getIsCurrentObject());

			tx.success();
		}

		assertEquals("current=Apollo", get("/pair/" + projectId + "/" + gemini));
	}

	@Test
	public void testMarkedStringParameterIsResolvedAsAnId() throws FrameworkException {

		setup();

		try (final Tx tx = app.tx()) {

			final Page page       = textPage("by-id");
			final Template detail = template(page, "current=${current.name};key=${key}");

			parameter(path(page, "/by-id/{key}"), "key", 0, "String", true);

			makePublic(page, detail);

			tx.success();
		}

		assertEquals("current=Apollo;key=" + projectId, get("/by-id/" + projectId));
		assertEquals("current=;key=not-an-id", get("/by-id/not-an-id"));
	}

	// ----- private methods -----
	private void setup() {

		try (final Tx tx = app.tx()) {

			final NodeInterface apollo = app.create(StructrTraits.GROUP, "Apollo");

			app.create(StructrTraits.GROUP, "Gemini");

			projectId = apollo.getUuid();

			// /projects/{project}: the template shows the current object, the id keyword and the route value
			final Page detailPage   = textPage("project-detail");
			final Template detail   = template(detailPage, "current=${current.name};id=${id};project=${project.name}");

			partialId = detail.getUuid();

			route(detailPage, "/projects/{project}");

			// /projects/{project}/all: a repeater over all groups that uses the route value in every row
			final Page listPage     = textPage("project-list");
			final Template row      = template(listPage, "${group.name} in ${project.name};");

			row.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Group', sort('name'))");
			row.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY), "group");

			route(listPage, "/projects/{project}/all");

			makePublic(detailPage, detail, listPage, row, apollo);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// the repeater lists both groups, so both must be visible
		try (final Tx tx = app.tx()) {

			for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getAsList()) {

				group.setProperty(Traits.of(StructrTraits.GROUP).key("visibleToPublicUsers"), true);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}
	}

	private Page textPage(final String name) throws FrameworkException {

		final Page page = Page.createNewPage(securityContext, name);

		page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

		return page;
	}

	private Template template(final Page page, final String content) throws FrameworkException {

		final Template template = app.create(StructrTraits.TEMPLATE).as(Template.class);

		page.appendChild(template);

		template.setContent(content);
		template.setProperty(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

		return template;
	}

	private void route(final Page page, final String path) throws FrameworkException {

		parameter(path(page, path), "project", 0, "Node", false);
	}

	private NodeInterface path(final Page page, final String path) throws FrameworkException {

		return app.create(StructrTraits.PAGE_PATH,
			new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
			new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), path)
		);
	}

	private NodeInterface parameter(final NodeInterface pagePath, final String name, final int position, final String valueType, final boolean isCurrentObject) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.PAGE_PATH_PARAMETER);

		return app.create(StructrTraits.PAGE_PATH_PARAMETER,
			new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.PATH_PROPERTY),              pagePath),
			new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                  name),
			new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY),          position),
			new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),        valueType),
			new NodeAttribute<>(traits.key(PagePathParameterTraitDefinition.IS_CURRENT_OBJECT_PROPERTY), isCurrentObject)
		);
	}

	private void mark(final NodeInterface parameter, final boolean isCurrentObject) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			parameter.setProperty(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.IS_CURRENT_OBJECT_PROPERTY), isCurrentObject);

			tx.success();
		}
	}

	private String groupId(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String id = app.nodeQuery(StructrTraits.GROUP).name(name).getFirst().getUuid();

			tx.success();

			return id;
		}
	}

	private String get(final String path) {

		RestAssured.basePath = "/";

		return RestAssured.given().expect().statusCode(200).when().get(path).getBody().asString().trim();
	}
}
