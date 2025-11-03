/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.html.Option;
import org.structr.web.traits.definitions.html.Select;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class RepeaterTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(RepeaterTest.class);

	@Test
	public void testManagedSelectedAttributeInOptionElementWithNodes() {

		Content content = null;

		// setup 1: create schema and user
		try (final Tx tx = app.tx()) {

			// create schema for Project->Task
			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "TASK", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			createAdminUser();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		final List<String> taskIDs = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			final String project  = "Project";
			final String task     = "Task";
			final PropertyKey key = Traits.of(task).key("project");

			final NodeInterface project1 = app.create(project, "Project 1");

			taskIDs.add(app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Task 1"), new NodeAttribute<>(key, project1)).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Task 2")).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Task 3")).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Task 4"), new NodeAttribute<>(key, project1)).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Task 5"), new NodeAttribute<>(key, project1)).getUuid());

			// create page
			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMNode select = createElement(page1, div,    "select");
			final DOMNode option = createElement(page1, select, "option", "${task.name}");


			select.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Project')");
			select.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "project");
			select.setProperty(Traits.of(StructrTraits.SELECT).key(Select.MULTIPLE_PROPERTY),                         "multiple");

			option.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Task', sort('name'))");
			option.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "task");
			option.setProperty(Traits.of(StructrTraits.OPTION).key(Option.VALUE_PROPERTY),                            "${task.id}");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test 1: assert selected attributes are NOT set (because we didn't set selectedValues)
		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",                          Matchers.equalTo("Page1"))
			.body("html.body.h1",                             Matchers.equalTo("Page1"))
			.body("html.body.div.select.option[0]",           Matchers.equalTo("Task 1"))
			.body("html.body.div.select.option[0].@value",    Matchers.equalTo(taskIDs.get(0)))
			.body("html.body.div.select.option[0].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[1]",           Matchers.equalTo("Task 2"))
			.body("html.body.div.select.option[1].@value",    Matchers.equalTo(taskIDs.get(1)))
			.body("html.body.div.select.option[1].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[2]",           Matchers.equalTo("Task 3"))
			.body("html.body.div.select.option[2].@value",    Matchers.equalTo(taskIDs.get(2)))
			.body("html.body.div.select.option[2].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[3]",           Matchers.equalTo("Task 4"))
			.body("html.body.div.select.option[3].@value",    Matchers.equalTo(taskIDs.get(3)))
			.body("html.body.div.select.option[3].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[4]",           Matchers.equalTo("Task 5"))
			.body("html.body.div.select.option[4].@value",    Matchers.equalTo(taskIDs.get(4)))
			.body("html.body.div.select.option[4].@selected", Matchers.nullValue())
			.when()
			.get("/html/page1");

		// setup 3: set selectedValues expression to activate managed selected attribute

		try (final Tx tx = app.tx()) {

			final NodeInterface page1 = app.nodeQuery(StructrTraits.PAGE).getFirst();
			final DOMNode option      = page1.as(Page.class).getElementsByTagName("option").get(0);

			option.setProperty(Traits.of("Option").key( "selectedValues"), "project.tasks");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		// test 2: assert selected attributes are set now
		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",                          Matchers.equalTo("Page1"))
			.body("html.body.h1",                             Matchers.equalTo("Page1"))
			.body("html.body.div.select.option[0]",           Matchers.equalTo("Task 1"))
			.body("html.body.div.select.option[0].@value",    Matchers.equalTo(taskIDs.get(0)))
			.body("html.body.div.select.option[0].@selected", Matchers.equalTo("selected"))
			.body("html.body.div.select.option[1]",           Matchers.equalTo("Task 2"))
			.body("html.body.div.select.option[1].@value",    Matchers.equalTo(taskIDs.get(1)))
			.body("html.body.div.select.option[1].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[2]",           Matchers.equalTo("Task 3"))
			.body("html.body.div.select.option[2].@value",    Matchers.equalTo(taskIDs.get(2)))
			.body("html.body.div.select.option[2].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[3]",           Matchers.equalTo("Task 4"))
			.body("html.body.div.select.option[3].@value",    Matchers.equalTo(taskIDs.get(3)))
			.body("html.body.div.select.option[3].@selected", Matchers.equalTo("selected"))
			.body("html.body.div.select.option[4]",           Matchers.equalTo("Task 5"))
			.body("html.body.div.select.option[4].@value",    Matchers.equalTo(taskIDs.get(4)))
			.body("html.body.div.select.option[4].@selected", Matchers.equalTo("selected"))
			.when()
			.get("/html/page1");
	}

	@Test
	public void testManagedSelectedAttributeInOptionElementWithEnum() {

		Content content = null;

		// setup 1: create schema and user
		try (final Tx tx = app.tx()) {

			// create schema for Project->Task
			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");

			project.addEnumProperty("test").setFormat("one,two,three");

			StructrSchema.extendDatabaseSchema(app, schema);

			createAdminUser();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			final String project   = "Project";

			final NodeInterface project1  = app.create(project, "Project 1");
			final PropertyKey<String> key = Traits.of(project).key("test");

			// set enum type,
			project1.setProperty(key, "two");

			// create page
			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);
			final DOMNode select = createElement(page1, div,    "select");
			final DOMNode option = createElement(page1, select, "option", "${test.value}");


			select.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Project')");
			select.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "project");

			option.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY),  "enum_info('Project', 'test')");
			option.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),        "test");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test 1: assert selected attributes are NOT set (because we didn't set selectedValues)
		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",                          Matchers.equalTo("Page1"))
			.body("html.body.h1",                             Matchers.equalTo("Page1"))
			.body("html.body.div.select.option[0]",           Matchers.equalTo("one"))
			.body("html.body.div.select.option[0].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[1]",           Matchers.equalTo("two"))
			.body("html.body.div.select.option[1].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[2]",           Matchers.equalTo("three"))
			.body("html.body.div.select.option[2].@selected", Matchers.nullValue())
			.when()
			.get("/html/page1");

		// setup 3: set selectedValues expression to activate managed selected attribute

		try (final Tx tx = app.tx()) {

			final Page page1     = app.nodeQuery(StructrTraits.PAGE).getFirst().as(Page.class);
			final DOMNode option = page1.getElementsByTagName("option").get(0);

			option.setProperty(Traits.of("Option").key( "selectedValues"), "project.test");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		// test 2: assert selected attributes are set now
		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",                          Matchers.equalTo("Page1"))
			.body("html.body.h1",                             Matchers.equalTo("Page1"))
			.body("html.body.div.select.option[0]",           Matchers.equalTo("one"))
			.body("html.body.div.select.option[0].@selected", Matchers.nullValue())
			.body("html.body.div.select.option[1]",           Matchers.equalTo("two"))
			.body("html.body.div.select.option[1].@selected", Matchers.equalTo("selected"))
			.body("html.body.div.select.option[2]",           Matchers.equalTo("three"))
			.body("html.body.div.select.option[2].@selected", Matchers.nullValue())
			.when()
			.get("/html/page1");
	}

	@Test
	public void testGraphObjectMapRepeater() {

		createAdminUser();

		// create page
		final Page page1;

		try (final Tx tx = app.tx()) {

			page1 = Page.createSimplePage(securityContext, "page2");
			final DOMNode div    = page1.getElementsByTagName("div").get(0);

			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "{ [ { id: 1, name: 'test1' }, { id: 2, name: 'test2' }]; }");
			div.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY), "obj");

			div.getFirstChild().setProperty(Traits.of(StructrTraits.CONTENT).key("content"), "${obj.name}");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";

		// test 2: assert selected attributes are set now
		RestAssured
			.given()
			.header(X_USER_HEADER,     ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.expect()
			.statusCode(200)
			.body("html.head.title",                                Matchers.equalTo("Page2"))
			.body("html.body.h1",                                   Matchers.equalTo("Page2"))
			.body("html.body.div[0]",                               Matchers.equalTo("test1"))
			.body("html.body.div[0].@data-repeater-data-object-id",  Matchers.nullValue())
			.body("html.body.div[1]",                               Matchers.equalTo("test2"))
			.body("html.body.div[1].@data-repeater-data-object-id",  Matchers.nullValue())
			.when()
			.get("/html/page2");
	}

	protected DOMElement createElement(final Page page, final DOMNode parent, final String tag, final String... content) throws FrameworkException {

		final DOMElement child = page.createElement(tag);

		parent.appendChild(child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final DOMNode node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}
}
