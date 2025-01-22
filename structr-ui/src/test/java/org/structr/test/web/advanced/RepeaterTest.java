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
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Option;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

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

			// create test user
			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		final List<String> taskIDs = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			final Class project   = StructrApp.getConfiguration().getNodeEntityClass("Project");
			final Class task      = StructrApp.getConfiguration().getNodeEntityClass("Task");
			final PropertyKey key = StructrApp.key(task, "project");

			final NodeInterface project1 = app.create(project, "Project 1");

			taskIDs.add(app.create(task, new NodeAttribute<>(AbstractNode.name, "Task 1"), new NodeAttribute<>(key, project1)).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(AbstractNode.name, "Task 2")).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(AbstractNode.name, "Task 3")).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(AbstractNode.name, "Task 4"), new NodeAttribute<>(key, project1)).getUuid());
			taskIDs.add(app.create(task, new NodeAttribute<>(AbstractNode.name, "Task 5"), new NodeAttribute<>(key, project1)).getUuid());

			// create page
			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = (DOMNode)page1.getElementsByTagName("div").item(0);
			final DOMNode select = createElement(page1, div,    "select");
			final DOMNode option = createElement(page1, select, "option", "${task.name}");


			select.setProperty(StructrApp.key(DOMNode.class, "functionQuery"),  "find('Project')");
			select.setProperty(StructrApp.key(DOMNode.class, "dataKey"),        "project");
			select.setProperty(StructrApp.key(DOMNode.class, "_html_multiple"), "multiple");

			option.setProperty(StructrApp.key(DOMNode.class, "functionQuery"),  "find('Task', sort('name'))");
			option.setProperty(StructrApp.key(DOMNode.class, "dataKey"),        "task");
			option.setProperty(StructrApp.key(Option.class, "_html_value"),     "${task.id}");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test 1: assert selected attributes are NOT set (because we didn't set selectedValues)
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
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

			final Page page1     = app.nodeQuery("Page").getFirst();
			final DOMNode option = (DOMNode)page1.getElementsByTagName("option").item(0);

			option.setProperty(StructrApp.key(Option.class,  "selectedValues"), "project.tasks");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		// test 2: assert selected attributes are set now
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
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

			project.addEnumProperty("test").setEnums("one", "two", "three");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			createTestNode(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"), true)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		try (final Tx tx = app.tx()) {

			final Class project   = StructrApp.getConfiguration().getNodeEntityClass("Project");

			final NodeInterface project1 = app.create(project, "Project 1");
			final EnumProperty key       = (EnumProperty)StructrApp.key(project, "test");
			final Class enumType         = key.getEnumType();

			// set enum type,
			project1.setProperty(key, Enum.valueOf(enumType, "two"));

			// create page
			final Page page1     = Page.createSimplePage(securityContext, "page1");
			final DOMNode div    = (DOMNode)page1.getElementsByTagName("div").item(0);
			final DOMNode select = createElement(page1, div,    "select");
			final DOMNode option = createElement(page1, select, "option", "${test.value}");


			select.setProperty(StructrApp.key(DOMNode.class, "functionQuery"), "find('Project')");
			select.setProperty(StructrApp.key(DOMNode.class, "dataKey"),       "project");

			option.setProperty(StructrApp.key(DOMNode.class, "functionQuery"),  "enum_info('Project', 'test')");
			option.setProperty(StructrApp.key(DOMNode.class, "dataKey"),        "test");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		RestAssured.basePath = "/";

		// test 1: assert selected attributes are NOT set (because we didn't set selectedValues)
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
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

			final Page page1     = app.nodeQuery("Page").getFirst();
			final DOMNode option = (DOMNode)page1.getElementsByTagName("option").item(0);

			option.setProperty(StructrApp.key(Option.class,  "selectedValues"), "project.test");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			logger.warn("", fex);
		}

		// test 2: assert selected attributes are set now
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
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
}
