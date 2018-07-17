/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.graphql;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.rest.common.StructrGraphQLTest;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonFunctionProperty;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public class GraphQLTest extends StructrGraphQLTest {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLTest.class.getName());

	@Test
	public void testBasics() {

		RestAssured.basePath = "/structr/graphql";

		Group group      = null;
		Principal tester = null;

		try (final Tx tx = app.tx()) {

			final PropertyKey<List> membersKey = StructrApp.key(Group.class, "members");

			tester = app.create(Principal.class, new NodeAttribute<>(Principal.name, "tester"));
			group  = app.create(Group.class,
				new NodeAttribute<>(Group.name, "TestGroup"),
				new NodeAttribute<>(membersKey, Arrays.asList(tester))
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String query1 = "{ Group { id, type, name, members { id, type, name } }, Principal(_pageSize: 1) { id, type name }}";
		final String query2 = "{ Group { id, type, name, members { } }}";
		final String query3 = "{ Group(id: \"" + group.getUuid() + "\") { id, type, name }}";

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body(query1)

			.expect()
				.statusCode(200)
				.body("Group",                    hasSize(1))
				.body("Principal",                hasSize(1))
				.body("Group[0].id",              equalTo(group.getUuid()))
				.body("Group[0].type",            equalTo("Group"))
				.body("Group[0].name",            equalTo("TestGroup"))
				.body("Group[0].members[0].id",   equalTo(tester.getUuid()))
				.body("Group[0].members[0].type", equalTo("Principal"))
				.body("Group[0].members[0].name", equalTo("tester"))
				.body("Principal[0].id",          equalTo(group.getUuid()))
				.body("Principal[0].type",        equalTo("Group"))
				.body("Principal[0].name",        equalTo("TestGroup"))

			.when()
				.post("/");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body(query2)

			.expect()
				.statusCode(422)
				.body("message", equalTo("Parse error at } in line 1, column 36"))
				.body("code",    equalTo(422))
				.body("query",   equalTo(query2))

			.when()
				.post("/");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body(query3)

			.expect()
				.statusCode(200)
				.body("Group",                    hasSize(1))
				.body("Group[0].id",              equalTo(group.getUuid()))
				.body("Group[0].type",            equalTo("Group"))
				.body("Group[0].name",            equalTo("TestGroup"))

			.when()
				.post("/");
	}

	@Test
	public void testAdvancedQueries() {

		final List<MailTemplate> templates = new LinkedList<>();
		final List<Principal> team         = new LinkedList<>();
		Group group                        = null;

		try (final Tx tx = app.tx()) {

			final PropertyKey<List> membersKey = StructrApp.key(Group.class, "members");

			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Axel")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Christian")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Christian")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Inès")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Kai")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Lukas")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Michael")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Susanne")));
			team.add(app.create(Principal.class, new NodeAttribute<>(Principal.name, "Tobias")));

			group  = app.create(Group.class,
				new NodeAttribute<>(Group.name, "Structr Team"),
				new NodeAttribute<>(membersKey, team)
			);

			app.create(Group.class,
				new NodeAttribute<>(Group.name, "All teams"),
				new NodeAttribute<>(membersKey, Arrays.asList(group))
			);

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate4"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "zrtsga"),
				new NodeAttribute<>(AbstractNode.owner, team.get(2))
			));

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate2"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "lertdf"),
				new NodeAttribute<>(AbstractNode.owner, team.get(0))
			));

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate5"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "tzegsg"),
				new NodeAttribute<>(AbstractNode.owner, team.get(3))
			));

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate3"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "asgw"),
				new NodeAttribute<>(AbstractNode.owner, team.get(1))
			));

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate6"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "dfjgr"),
				new NodeAttribute<>(AbstractNode.owner, team.get(4))
			));

			templates.add(app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "MailTemplate1"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "abcdef"),
				new NodeAttribute<>(AbstractNode.owner, team.get(0))
			));


			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal(id: \"" + team.get(0).getUuid() + "\") { id, type, name } }");
			assertMapPathValueIs(result, "Principal.#",      1);
			assertMapPathValueIs(result, "Principal.0.id",   team.get(0).getUuid());
			assertMapPathValueIs(result, "Principal.0.type", "Principal");
			assertMapPathValueIs(result, "Principal.0.name", "Axel");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal(name: \"Axel\") { name } }");
			assertMapPathValueIs(result, "Principal.#",      1);
			assertMapPathValueIs(result, "Principal.0.name", "Axel");
			assertMapPathValueIs(result, "Principal.0.type", null);
			assertMapPathValueIs(result, "Principal.0.id",   null);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal { name(_equals: \"Axel\") } }");
			assertMapPathValueIs(result, "Principal.#",      1);
			assertMapPathValueIs(result, "Principal.0.name", "Axel");
			assertMapPathValueIs(result, "Principal.0.type", null);
			assertMapPathValueIs(result, "Principal.0.id",   null);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal { name(_contains: \"a\", _contains: \"l\", _conj: \"and\") } }");
			assertMapPathValueIs(result, "Principal.#",      4);
			assertMapPathValueIs(result, "Principal.0.name", "All teams");
			assertMapPathValueIs(result, "Principal.1.name", "Axel");
			assertMapPathValueIs(result, "Principal.2.name", "Lukas");
			assertMapPathValueIs(result, "Principal.3.name", "Michael");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_pageSize: 1) { name }, Principal(_pageSize: 2, _page: 2) { name(_contains: \"i\") } }");
			assertMapPathValueIs(result, "Group.#",          1);
			assertMapPathValueIs(result, "Group.0.name",     "All teams");
			assertMapPathValueIs(result, "Principal.#",      2);
			assertMapPathValueIs(result, "Principal.0.name", "Inès");
			assertMapPathValueIs(result, "Principal.1.name", "Kai");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { name, members(_pageSize: 2, _page: 2) { name }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           "All teams");
			assertMapPathValueIs(result, "Group.0.members",        new LinkedList<>());
			assertMapPathValueIs(result, "Group.1.name",           "Structr Team");
			assertMapPathValueIs(result, "Group.1.members.#",      2);
			assertMapPathValueIs(result, "Group.1.members.0.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Inès");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { name, members(_sort: \"name\") { name }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           "All teams");
			assertMapPathValueIs(result, "Group.0.members.#",      1);
			assertMapPathValueIs(result, "Group.1.name",           "Structr Team");
			assertMapPathValueIs(result, "Group.1.members.#",      9);
			assertMapPathValueIs(result, "Group.1.members.0.name", "Axel");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.2.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.3.name", "Inès");
			assertMapPathValueIs(result, "Group.1.members.4.name", "Kai");
			assertMapPathValueIs(result, "Group.1.members.5.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.6.name", "Michael");
			assertMapPathValueIs(result, "Group.1.members.7.name", "Susanne");
			assertMapPathValueIs(result, "Group.1.members.8.name", "Tobias");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { name, members(_sort: \"name\", _desc: true) { name }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           "All teams");
			assertMapPathValueIs(result, "Group.0.members.#",      1);
			assertMapPathValueIs(result, "Group.1.name",           "Structr Team");
			assertMapPathValueIs(result, "Group.1.members.#",      9);
			assertMapPathValueIs(result, "Group.1.members.8.name", "Axel");
			assertMapPathValueIs(result, "Group.1.members.7.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.6.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.5.name", "Inès");
			assertMapPathValueIs(result, "Group.1.members.4.name", "Kai");
			assertMapPathValueIs(result, "Group.1.members.3.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.2.name", "Michael");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Susanne");
			assertMapPathValueIs(result, "Group.1.members.0.name", "Tobias");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { members { name(_contains: \"k\", _contains: \"l\", _conj: \"and\") }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           null);
			assertMapPathValueIs(result, "Group.0.members",        new LinkedList<>());
			assertMapPathValueIs(result, "Group.1.members.0.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.1",      null);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { members { name(_contains: \"k\", _contains: \"l\", _conj: \"or\") }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           null);
			assertMapPathValueIs(result, "Group.0.members",        new LinkedList<>());
			assertMapPathValueIs(result, "Group.1.members.#",      4);
			assertMapPathValueIs(result, "Group.1.members.0.name", "Axel");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Kai");
			assertMapPathValueIs(result, "Group.1.members.2.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.3.name", "Michael");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate { id, type, text(_contains: \"2\"), owner(_equals: { name: \"Axel\"}) { name } }}");
			assertMapPathValueIs(result, "MailTemplate.#",             1);
			assertMapPathValueIs(result, "MailTemplate.0.id",          templates.get(1).getUuid());
			assertMapPathValueIs(result, "MailTemplate.0.type",        "MailTemplate");
			assertMapPathValueIs(result, "MailTemplate.0.text",        "MailTemplate2");
			assertMapPathValueIs(result, "MailTemplate.0.name",        null);
			assertMapPathValueIs(result, "MailTemplate.0.owner.name",  "Axel");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(_sort: \"owner.name\") { name, owner { name }}}");
			assertMapPathValueIs(result, "MailTemplate.#",                6);
			assertMapPathValueIs(result, "MailTemplate.0.name",           "abcdef");
			assertMapPathValueIs(result, "MailTemplate.0.owner.name",     "Axel");
			assertMapPathValueIs(result, "MailTemplate.1.name",           "lertdf");
			assertMapPathValueIs(result, "MailTemplate.1.owner.name",     "Axel");
			assertMapPathValueIs(result, "MailTemplate.2.name",           "asgw");
			assertMapPathValueIs(result, "MailTemplate.2.owner.name",     "Christian");
			assertMapPathValueIs(result, "MailTemplate.3.name",           "zrtsga");
			assertMapPathValueIs(result, "MailTemplate.3.owner.name",     "Christian");
			assertMapPathValueIs(result, "MailTemplate.4.name",           "tzegsg");
			assertMapPathValueIs(result, "MailTemplate.4.owner.name",     "Inès");
			assertMapPathValueIs(result, "MailTemplate.5.name",           "dfjgr");
			assertMapPathValueIs(result, "MailTemplate.5.owner.name",     "Kai");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group { name, members(_sort: \"name\", _desc: true) { name }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           "All teams");
			assertMapPathValueIs(result, "Group.0.members.#",      1);
			assertMapPathValueIs(result, "Group.1.name",           "Structr Team");
			assertMapPathValueIs(result, "Group.1.members.#",      9);
			assertMapPathValueIs(result, "Group.1.members.8.name", "Axel");
			assertMapPathValueIs(result, "Group.1.members.7.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.6.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.5.name", "Inès");
			assertMapPathValueIs(result, "Group.1.members.4.name", "Kai");
			assertMapPathValueIs(result, "Group.1.members.3.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.2.name", "Michael");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Susanne");
			assertMapPathValueIs(result, "Group.1.members.0.name", "Tobias");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(_pageSize: 2, _sort: \"name\", owner: { name: { _contains: \"x\" }} ) { id, type, name, owner { name }}}");
			assertMapPathValueIs(result, "MailTemplate.#",            2);
			assertMapPathValueIs(result, "MailTemplate.0.id",         templates.get(5).getUuid());
			assertMapPathValueIs(result, "MailTemplate.0.type",       "MailTemplate");
			assertMapPathValueIs(result, "MailTemplate.0.name",       "abcdef");
			assertMapPathValueIs(result, "MailTemplate.0.owner.name", "Axel");
			assertMapPathValueIs(result, "MailTemplate.1.id",         templates.get(1).getUuid());
			assertMapPathValueIs(result, "MailTemplate.1.type",       "MailTemplate");
			assertMapPathValueIs(result, "MailTemplate.1.name",       "lertdf");
			assertMapPathValueIs(result, "MailTemplate.1.owner.name", "Axel");
		}
	}

	@Test
	public void testAdvancedQueriesManyToMany() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "HAS", Relation.Cardinality.ManyToMany, "projects", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);


			tx.success();

		} catch (URISyntaxException|FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final Class project                = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final Class task                   = StructrApp.getConfiguration().getNodeEntityClass("Task");
		final PropertyKey tasksKey         = StructrApp.getConfiguration().getPropertyKeyForJSONName(project, "tasks");

		try (final Tx tx = app.tx()) {

			tasks.add(app.create(task, "task1"));
			tasks.add(app.create(task, "task2"));
			tasks.add(app.create(task, "task3"));
			tasks.add(app.create(task, "task4"));
			tasks.add(app.create(task, "task5"));

			projects.add(app.create(project, "project1"));
			projects.add(app.create(project, "project2"));
			projects.add(app.create(project, "project3"));
			projects.add(app.create(project, "project4"));
			projects.add(app.create(project, "project5"));

			for (int i=0; i<5; i++) {
				projects.get(i).setProperty(tasksKey, tasks.subList(i, 5));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task1\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task2\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task3\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            3);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task4\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task5\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            5);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            5);
		}




		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _equals: \"project1\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            5);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _equals: \"project2\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _equals: \"project3\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            3);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _equals: \"project4\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _equals: \"project5\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            5);
		}
	}

	@Test
	public void testAdvancedQueriesOneToMany() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "HAS", Relation.Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);


			tx.success();

		} catch (URISyntaxException|FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final Class project                = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final Class task                   = StructrApp.getConfiguration().getNodeEntityClass("Task");
		final PropertyKey tasksKey         = StructrApp.getConfiguration().getPropertyKeyForJSONName(project, "tasks");

		try (final Tx tx = app.tx()) {

			tasks.add(app.create(task, "task0"));
			tasks.add(app.create(task, "task1"));
			tasks.add(app.create(task, "task2"));
			tasks.add(app.create(task, "task3"));
			tasks.add(app.create(task, "task4"));
			tasks.add(app.create(task, "task5"));
			tasks.add(app.create(task, "task6"));
			tasks.add(app.create(task, "task7"));
			tasks.add(app.create(task, "task8"));
			tasks.add(app.create(task, "task9"));

			projects.add(app.create(project, "project1"));
			projects.add(app.create(project, "project2"));
			projects.add(app.create(project, "project3"));
			projects.add(app.create(project, "project4"));
			projects.add(app.create(project, "project5"));


			projects.get(0).setProperty(tasksKey, tasks.subList(0,  2));
			projects.get(1).setProperty(tasksKey, tasks.subList(2,  4));
			projects.get(2).setProperty(tasksKey, tasks.subList(4,  6));
			projects.get(3).setProperty(tasksKey, tasks.subList(6,  8));
			projects.get(4).setProperty(tasksKey, tasks.subList(8, 10));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task1\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task3\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task5\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task7\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task9\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}




		{
			final Map<String, Object> result = fetchGraphQL("{ Task(project: { name: { _equals: \"project1\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task0");
			assertMapPathValueIs(result, "Task.1.name",            "task1");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(project: { name: { _equals: \"project2\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task2");
			assertMapPathValueIs(result, "Task.1.name",            "task3");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(project: { name: { _equals: \"project3\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task4");
			assertMapPathValueIs(result, "Task.1.name",            "task5");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(project: { name: { _equals: \"project4\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task6");
			assertMapPathValueIs(result, "Task.1.name",            "task7");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(project: { name: { _equals: \"project5\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task8");
			assertMapPathValueIs(result, "Task.1.name",            "task9");
		}
	}

	@Test
	public void testAdvancedQueriesManyToOne() {

		// test data setup
		try (final Tx tx = app.tx()) {

			final Principal p1 = app.create(Principal.class, "p1");
			final Principal p2 = app.create(Principal.class, "p2");
			final MailTemplate m1 = app.create(MailTemplate.class, "m1");
			final MailTemplate m2 = app.create(MailTemplate.class, "m2");
			final MailTemplate m3 = app.create(MailTemplate.class, "m3");
			final MailTemplate m4 = app.create(MailTemplate.class, "m4");

			m1.setProperty(MailTemplate.owner, p1);
			m2.setProperty(MailTemplate.owner, p1);
			m3.setProperty(MailTemplate.owner, p2);
			m4.setProperty(MailTemplate.owner, p2);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _equals: \"p2\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
			assertMapPathValueIs(result, "MailTemplate.0.name",            "m3");
			assertMapPathValueIs(result, "MailTemplate.1.name",            "m4");
		}

	}

	@Test
	public void testAdvancedQueriesManyToOneWithEdgeCases() {

		// test data setup
		try (final Tx tx = app.tx()) {

			final Principal p1 = app.create(Principal.class, "First Tester");
			final Principal p2 = app.create(Principal.class, "Second Tester");
			final MailTemplate m1 = app.create(MailTemplate.class, "First Template");
			final MailTemplate m2 = app.create(MailTemplate.class, "Second Template");
			final MailTemplate m3 = app.create(MailTemplate.class, "Third Template");
			final MailTemplate m4 = app.create(MailTemplate.class, "Fourth Template");
			final MailTemplate m5 = app.create(MailTemplate.class, "Fifth Template");
			final MailTemplate m6 = app.create(MailTemplate.class, "Sixth Template");

			m1.setProperty(MailTemplate.owner, p1);
			m2.setProperty(MailTemplate.owner, p1);
			m3.setProperty(MailTemplate.owner, p2);
			m4.setProperty(MailTemplate.owner, p2);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		{
			// expect no results because no owner name matches the given name filter
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _equals: \"none\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 0);
		}

		{
			// expect two results because one owner with two templates matches the given name filter
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _equals: \"First Tester\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
			assertMapPathValueIs(result, "MailTemplate.0.name",            "First Template");
			assertMapPathValueIs(result, "MailTemplate.1.name",            "Second Template");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"f\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"fi\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
		}


		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"fir\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"firs\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"first\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"t\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"te\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"tes\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"test\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(owner: { name: { _contains: \"tester\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 4);
		}

	}

	@Test
	public void testFunctionPropertyQueries() {

		// schema setup
		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType type             = schema.addType("FunctionTest");
			final JsonFunctionProperty stringTest = type.addFunctionProperty("stringTest");
			final JsonFunctionProperty boolTest   = type.addFunctionProperty("boolTest");

			stringTest.setReadFunction("if(eq(this.name, 'test1'), 'true', 'false')");
			stringTest.setIndexed(true);
			stringTest.setTypeHint("String");

			boolTest.setReadFunction("if(eq(this.name, 'test2'), true, false)");
			boolTest.setIndexed(true);
			boolTest.setTypeHint("Boolean");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (URISyntaxException|FrameworkException fex) {
			fex.printStackTrace();
		}

		// test data setup
		try (final Tx tx = app.tx()) {

			final Class type = StructrApp.getConfiguration().getNodeEntityClass("FunctionTest");

			app.create(type, "test1");
			app.create(type, "test2");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(stringTest: {_equals: \"true\"}) { id, type, name, stringTest, boolTest }}");
			assertMapPathValueIs(result, "FunctionTest.#",            1);
			assertMapPathValueIs(result, "FunctionTest.0.name",       "test1");
			assertMapPathValueIs(result, "FunctionTest.0.stringTest", "true");
			assertMapPathValueIs(result, "FunctionTest.0.boolTest",   false);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(stringTest: {_equals: \"false\"}) { id, type, name, stringTest, boolTest }}");
			assertMapPathValueIs(result, "FunctionTest.#",            1);
			assertMapPathValueIs(result, "FunctionTest.0.name",       "test2");
			assertMapPathValueIs(result, "FunctionTest.0.stringTest", "false");
			assertMapPathValueIs(result, "FunctionTest.0.boolTest",   true);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(stringTest: {_contains: \"e\"}) { id, type, name, stringTest, boolTest }}");
			assertMapPathValueIs(result, "FunctionTest.#",            2);
			assertMapPathValueIs(result, "FunctionTest.0.name",       "test1");
			assertMapPathValueIs(result, "FunctionTest.0.stringTest", "true");
			assertMapPathValueIs(result, "FunctionTest.0.boolTest",   false);
			assertMapPathValueIs(result, "FunctionTest.1.name",       "test2");
			assertMapPathValueIs(result, "FunctionTest.1.stringTest", "false");
			assertMapPathValueIs(result, "FunctionTest.1.boolTest",   true);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(boolTest: {_equals: true}) { id, type, name, stringTest, boolTest }}");
			assertMapPathValueIs(result, "FunctionTest.#",            1);
			assertMapPathValueIs(result, "FunctionTest.0.name",       "test2");
			assertMapPathValueIs(result, "FunctionTest.0.stringTest", "false");
			assertMapPathValueIs(result, "FunctionTest.0.boolTest",   true);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(boolTest: {_equals: false}) { id, type, name, stringTest, boolTest }}");
			assertMapPathValueIs(result, "FunctionTest.#",            1);
			assertMapPathValueIs(result, "FunctionTest.0.name",       "test1");
			assertMapPathValueIs(result, "FunctionTest.0.stringTest", "true");
			assertMapPathValueIs(result, "FunctionTest.0.boolTest",   false);
		}
	}

	@Test
	public void testSchema() {

		RestAssured.basePath = "/structr/graphql";

		RestAssured.given()

				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body("{ __schema { types { name } }}")

			.expect()
				.statusCode(200)

			.when()
				.post("/");
	}

	@Test
	public void testMixedSchemaError() {

		RestAssured.basePath = "/structr/graphql";

		/*
		 * Structr uses two different methods of creating a GraphQL result, depending on the type of query. If a __schema query is
		 * sent, Structr delegates query execution to graphql-java, otherwise Structr uses its own query methods and only uses the
		 * structure information from graphql to control the output views on each level. Because of that, Structr cannot support
		 * GraphQL queries that request both schema and data information, and throws an exception.
		 */

		final String query1 = "{ __schema { types { name }}, Group { name }}";
		final String query2 = "{ Group { name }, __schema { types { name }} }";

		RestAssured.given()

				.contentType("application/json; charset=UTF-8")
				.body(query1)

			.expect()
				.statusCode(422)
				.body("message", equalTo("Unsupported query type, schema and data queries cannot be mixed."))
				.body("code",    equalTo(422))
				.body("query",   equalTo(query1))

			.when()
				.post("/");

		RestAssured.given()

				.contentType("application/json; charset=UTF-8")
				.body(query2)

			.expect()
				.statusCode(422)
				.body("message", equalTo("Unsupported query type, schema and data queries cannot be mixed."))
				.body("code",    equalTo(422))
				.body("query",   equalTo(query2))

			.when()
				.post("/");
	}

	@Test
	public void testGraphQLErrorMessages() {

		RestAssured.basePath = "/structr/graphql";

		final String query1 = "{ Group { id. type, name, members } }";
		final String query2 = "{ Group { id. type, name, owner } }";

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body(query1)

			.expect()
				.statusCode(422)
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field members"))
				.body("errors[0].locations[0].line",   equalTo(1))
				.body("errors[0].locations[0].column", equalTo(27))
				.body("errors[0].description",         equalTo("Sub selection required for type Principal of field members"))
				.body("errors[0].validationErrorType", equalTo("SubSelectionRequired"))

			.when()
				.post("/");

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body(query2)

			.expect()
				.statusCode(422)
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field owner"))
				.body("errors[0].locations[0].line",   equalTo(1))
				.body("errors[0].locations[0].column", equalTo(27))
				.body("errors[0].description",         equalTo("Sub selection required for type Principal of field owner"))
				.body("errors[0].validationErrorType", equalTo("SubSelectionRequired"))

			.when()
				.post("/");
	}

	@Test
	public void testFunctionPropertyTypeHint() {

		RestAssured.basePath = "/structr/graphql";

		List<NodeInterface> children = null;
		Principal user               = null;

		try (final Tx tx = app.tx()) {

			final JsonSchema schema             = StructrSchema.createFromDatabase(app);
			final JsonObjectType type           = schema.addType("Test");
			final JsonObjectType tmpType        = schema.addType("Tmp");

			type.relate(tmpType, "TMP", Relation.Cardinality.OneToMany, "parent", "children");

			type.addFunctionProperty("test1").setReadFunction("'test'").setTypeHint("String");
			type.addFunctionProperty("test2").setReadFunction("false").setTypeHint("Boolean");
			type.addFunctionProperty("test3").setReadFunction("int(42)").setTypeHint("Int");
			type.addFunctionProperty("test4").setReadFunction("12.34").setTypeHint("Double");
			type.addFunctionProperty("test5").setReadFunction("7465423674522").setTypeHint("Long");
			type.addFunctionProperty("test6").setReadFunction("this.owner").setTypeHint("Principal");
			type.addFunctionProperty("test7").setReadFunction("this.children").setTypeHint("Tmp[]");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (URISyntaxException | FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// create test node
		try (final Tx tx = app.tx()) {

			user = app.create(Principal.class, "tester");

			final Class tmpType  = StructrApp.getConfiguration().getNodeEntityClass("Tmp");
			final Class testType = StructrApp.getConfiguration().getNodeEntityClass("Test");

			final PropertyKey nameKey     = StructrApp.getConfiguration().getPropertyKeyForJSONName(testType, "name");
			final PropertyKey ownerKey    = StructrApp.getConfiguration().getPropertyKeyForJSONName(testType, "owner");
			final PropertyKey childrenKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(testType, "children");

			children = createTestNodes(tmpType, 10);

			app.create(testType,
				new NodeAttribute<>(nameKey, "Test"),
				new NodeAttribute<>(ownerKey, user),
				new NodeAttribute<>(childrenKey, children)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final Map<String, Object> result = fetchGraphQL("{ Test { test1, test2, test3, test4, test5, test6 { id, type, name }, test7 { id, type } }}");
		assertMapPathValueIs(result, "Test.0.test1",        "test");
		assertMapPathValueIs(result, "Test.0.test2",        false);
		assertMapPathValueIs(result, "Test.0.test3",        42.0);
		assertMapPathValueIs(result, "Test.0.test4",        12.34);
		assertMapPathValueIs(result, "Test.0.test5",        7.465423674522E12);
		assertMapPathValueIs(result, "Test.0.test6.id",     user.getUuid());
		assertMapPathValueIs(result, "Test.0.test6.type",   "Principal");
		assertMapPathValueIs(result, "Test.0.test6.name",   "tester");
		assertMapPathValueIs(result, "Test.0.test7.#",      10);

		for (int i=0; i<10; i++) {
			assertMapPathValueIs(result, "Test.0.test7." + i + ".id",   children.get(i).getUuid());
			assertMapPathValueIs(result, "Test.0.test7." + i + ".type", "Tmp");
		}

		final String query = "{ Test { test6, test7 } }";

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body(query)

			.expect()
				.statusCode(422)
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field test6"))
				.body("errors[0].locations[0].line",   equalTo(1))
				.body("errors[0].locations[0].column", equalTo(10))
				.body("errors[0].description",         equalTo("Sub selection required for type Principal of field test6"))
				.body("errors[0].validationErrorType", equalTo("SubSelectionRequired"))
				.body("errors[1].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Tmp of field test7"))
				.body("errors[1].locations[0].line",   equalTo(1))
				.body("errors[1].locations[0].column", equalTo(17))
				.body("errors[1].description",         equalTo("Sub selection required for type Tmp of field test7"))
				.body("errors[1].validationErrorType", equalTo("SubSelectionRequired"))

			.when()
				.post("/");
	}

	@Test
	public void testInheritedRelationshipProperties() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project     = schema.addType("Project");
			final JsonObjectType task        = schema.addType("Task");
			final JsonObjectType extProject1 = schema.addType("ExtendedProject1");
			final JsonObjectType extProject2 = schema.addType("ExtendedProject2");

			extProject1.setExtends(project);
			extProject2.setExtends(project);

			project.relate(task, "HAS", Relation.Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (URISyntaxException|FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final Class extProject             = StructrApp.getConfiguration().getNodeEntityClass("ExtendedProject1");
		final Class task                   = StructrApp.getConfiguration().getNodeEntityClass("Task");
		final PropertyKey tasksKey         = StructrApp.getConfiguration().getPropertyKeyForJSONName(extProject, "tasks");

		try (final Tx tx = app.tx()) {

			tasks.add(app.create(task, "task0"));
			tasks.add(app.create(task, "task1"));
			tasks.add(app.create(task, "task2"));
			tasks.add(app.create(task, "task3"));
			tasks.add(app.create(task, "task4"));
			tasks.add(app.create(task, "task5"));
			tasks.add(app.create(task, "task6"));
			tasks.add(app.create(task, "task7"));
			tasks.add(app.create(task, "task8"));
			tasks.add(app.create(task, "task9"));

			projects.add(app.create(extProject, "project1"));
			projects.add(app.create(extProject, "project2"));
			projects.add(app.create(extProject, "project3"));
			projects.add(app.create(extProject, "project4"));
			projects.add(app.create(extProject, "project5"));


			projects.get(0).setProperty(tasksKey, tasks.subList(0,  2));
			projects.get(1).setProperty(tasksKey, tasks.subList(2,  4));
			projects.get(2).setProperty(tasksKey, tasks.subList(4,  6));
			projects.get(3).setProperty(tasksKey, tasks.subList(6,  8));
			projects.get(4).setProperty(tasksKey, tasks.subList(8, 10));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task1\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}
	}

	@Test
	public void testCountPropertyType() {

		try (final Tx tx = app.tx()) {

			final SchemaNode projectType = app.create(SchemaNode.class, "Project");
			final SchemaNode taskType    = app.create(SchemaNode.class, "Task");

			final SchemaRelationshipNode rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, projectType),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, taskType),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "TASK"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "*"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "project"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "tasks")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode,   projectType),
				new NodeAttribute<>(SchemaProperty.name,         "taskCount"),
				new NodeAttribute<>(SchemaProperty.propertyType, "Count"),
				new NodeAttribute<>(SchemaProperty.format,       "tasks")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
			final Class taskType    = StructrApp.getConfiguration().getNodeEntityClass("Task");
			final PropertyKey name  = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "name");
			final PropertyKey tasks = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "tasks");


			final List<NodeInterface> taskList = createTestNodes(taskType, 10);

			final NodeInterface project = app.create(projectType,
				new NodeAttribute<>(name, "Test"),
				new NodeAttribute<>(tasks, taskList)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project { id, type, name, taskCount, tasks { id, type, name }}}");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.name",      "Test");
			assertMapPathValueIs(result, "Project.0.tasks.#",   10);
			assertMapPathValueIs(result, "Project.0.taskCount", 10.0);
		}

	}

	@Test
	public void testCompositeQueries() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project     = schema.addType("Project");
			final JsonObjectType task        = schema.addType("Task");

			project.relate(task, "HAS", Relation.Cardinality.OneToMany, "project", "tasks");

			final JsonFunctionProperty p1 = task.addFunctionProperty("projectId");
			p1.setIndexed(true);
			p1.setTypeHint("String");
			p1.setFormat("this.project.id");

			final JsonFunctionProperty p2 = project.addFunctionProperty("taskCount");
			p2.setIndexed(true);
			p2.setTypeHint("Int");
			p2.setFormat("size(this.tasks)");

			final JsonEnumProperty p3 = task.addEnumProperty("status");
			p3.setIndexed(true);
			p3.setEnums("open", "closed", "cancelled");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (URISyntaxException|FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final Class project                = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final Class task                   = StructrApp.getConfiguration().getNodeEntityClass("Task");
		final PropertyKey tasksKey         = StructrApp.getConfiguration().getPropertyKeyForJSONName(project, "tasks");
		final EnumProperty statusKey       = (EnumProperty)StructrApp.getConfiguration().getPropertyKeyForJSONName(task, "status");
		final PropertyKey nameKey          = StructrApp.getConfiguration().getPropertyKeyForJSONName(task, "name");

		;

		try (final Tx tx = app.tx()) {

			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task0"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "open"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task1"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "closed"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task2"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "cancelled"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task3"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "open"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task4"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "closed"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task5"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "cancelled"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task6"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "open"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task7"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "closed"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task8"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "cancelled"))));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task9"), new NodeAttribute<>(statusKey, Enum.valueOf(statusKey.getEnumType(), "open"))));

			projects.add(app.create(project, "project1"));
			projects.add(app.create(project, "project2"));
			projects.add(app.create(project, "project3"));
			projects.add(app.create(project, "project4"));
			projects.add(app.create(project, "project5"));

			projects.get(0).setProperty(tasksKey, tasks.subList(0,  2));
			projects.get(1).setProperty(tasksKey, tasks.subList(2,  4));
			projects.get(2).setProperty(tasksKey, tasks.subList(4,  6));
			projects.get(3).setProperty(tasksKey, tasks.subList(6,  8));
			projects.get(4).setProperty(tasksKey, tasks.subList(8, 10));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project { id, type, name, taskCount, tasks { id, type, name, projectId, status }}}");
			assertMapPathValueIs(result, "Project.#",                   5);
			assertMapPathValueIs(result, "Project.0.name",              "project1");
			assertMapPathValueIs(result, "Project.0.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.0.tasks.#",           2);
			assertMapPathValueIs(result, "Project.0.tasks.0.name",      "task0");
			assertMapPathValueIs(result, "Project.0.tasks.0.status",    "open");
			assertMapPathValueIs(result, "Project.0.tasks.0.projectId", projects.get(0).getUuid());
			assertMapPathValueIs(result, "Project.0.tasks.1.name",      "task1");
			assertMapPathValueIs(result, "Project.0.tasks.1.status",    "closed");
			assertMapPathValueIs(result, "Project.0.tasks.1.projectId", projects.get(0).getUuid());

			assertMapPathValueIs(result, "Project.1.name",              "project2");
			assertMapPathValueIs(result, "Project.1.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.1.tasks.#",           2);
			assertMapPathValueIs(result, "Project.1.tasks.0.name",      "task2");
			assertMapPathValueIs(result, "Project.1.tasks.0.status",    "cancelled");
			assertMapPathValueIs(result, "Project.1.tasks.0.projectId", projects.get(1).getUuid());
			assertMapPathValueIs(result, "Project.1.tasks.1.name",      "task3");
			assertMapPathValueIs(result, "Project.1.tasks.1.status",    "open");
			assertMapPathValueIs(result, "Project.1.tasks.1.projectId", projects.get(1).getUuid());

			assertMapPathValueIs(result, "Project.2.name",              "project3");
			assertMapPathValueIs(result, "Project.2.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.2.tasks.#",           2);
			assertMapPathValueIs(result, "Project.2.tasks.0.name",      "task4");
			assertMapPathValueIs(result, "Project.2.tasks.0.status",    "closed");
			assertMapPathValueIs(result, "Project.2.tasks.0.projectId", projects.get(2).getUuid());
			assertMapPathValueIs(result, "Project.2.tasks.1.name",      "task5");
			assertMapPathValueIs(result, "Project.2.tasks.1.status",    "cancelled");
			assertMapPathValueIs(result, "Project.2.tasks.1.projectId", projects.get(2).getUuid());

			assertMapPathValueIs(result, "Project.3.name",              "project4");
			assertMapPathValueIs(result, "Project.3.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.3.tasks.#",           2);
			assertMapPathValueIs(result, "Project.3.tasks.0.name",      "task6");
			assertMapPathValueIs(result, "Project.3.tasks.0.status",    "open");
			assertMapPathValueIs(result, "Project.3.tasks.0.projectId", projects.get(3).getUuid());
			assertMapPathValueIs(result, "Project.3.tasks.1.name",      "task7");
			assertMapPathValueIs(result, "Project.3.tasks.1.status",    "closed");
			assertMapPathValueIs(result, "Project.3.tasks.1.projectId", projects.get(3).getUuid());

			assertMapPathValueIs(result, "Project.4.name",              "project5");
			assertMapPathValueIs(result, "Project.4.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.4.tasks.#",           2);
			assertMapPathValueIs(result, "Project.4.tasks.0.name",      "task8");
			assertMapPathValueIs(result, "Project.4.tasks.0.status",    "cancelled");
			assertMapPathValueIs(result, "Project.4.tasks.0.projectId", projects.get(4).getUuid());
			assertMapPathValueIs(result, "Project.4.tasks.1.name",      "task9");
			assertMapPathValueIs(result, "Project.4.tasks.1.status",    "open");
			assertMapPathValueIs(result, "Project.4.tasks.1.projectId", projects.get(4).getUuid());
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + tasks.get(0).getUuid() + "\") { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projects.get(0).getUuid());
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task { id(_equals: \"" + tasks.get(0).getUuid() + "\"), name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projects.get(0).getUuid());
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + tasks.get(0).getUuid() + "\", status: { _equals: \"open\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projects.get(0).getUuid());
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + tasks.get(0).getUuid() + "\", status: { _equals: \"closed\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + tasks.get(0).getUuid() + "\", projectId: { _equals: \"" + projects.get(0).getUuid() + "\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projects.get(0).getUuid());
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + tasks.get(0).getUuid() + "\", projectId: { _equals: \"" + projects.get(1).getUuid() + "\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              0);
		}
	}

	@Test
	public void testPropertiesForCorrectInputType() {

		try (final Tx tx = app.tx()) {


			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");

			project.addBooleanProperty("testBoolean").setIndexed(true);
			project.addLongProperty("testLong").setIndexed(true);
			project.addNumberProperty("testDouble").setIndexed(true);
			project.addIntegerProperty("testInt").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");

			final PropertyKey testBoolean = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "testBoolean");
			final PropertyKey testDouble  = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "testDouble");
			final PropertyKey testLong    = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "testLong");
			final PropertyKey testInt     = StructrApp.getConfiguration().getPropertyKeyForJSONName(projectType, "testInt");

			app.create(projectType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  252.52),
				new NodeAttribute<>(testLong,    234532L),
				new NodeAttribute<>(testInt,     4563332)
			);

			app.create(projectType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(testDouble,  124.52),
				new NodeAttribute<>(testLong,    563L),
				new NodeAttribute<>(testInt,     2345)
			);

			app.create(projectType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  323.22),
				new NodeAttribute<>(testLong,    22L),
				new NodeAttribute<>(testInt,     452)
			);

			app.create(projectType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(testDouble,  334.32),
				new NodeAttribute<>(testLong,    5L),
				new NodeAttribute<>(testInt,     235)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(testBoolean: { _equals: true}) { testBoolean, testDouble, testLong, testInt } }");
			assertMapPathValueIs(result, "Project.#",           2);
			assertMapPathValueIs(result, "Project.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.0.testDouble",  252.52);
			assertMapPathValueIs(result, "Project.0.testLong",    234532.0);
			assertMapPathValueIs(result, "Project.0.testInt",     4563332.0);
			assertMapPathValueIs(result, "Project.1.testBoolean", true);
			assertMapPathValueIs(result, "Project.1.testDouble",  323.22);
			assertMapPathValueIs(result, "Project.1.testLong",    22.0);
			assertMapPathValueIs(result, "Project.1.testInt",     452.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(testDouble: { _equals: 334.32}) { testBoolean, testDouble, testLong, testInt } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.testBoolean", false);
			assertMapPathValueIs(result, "Project.0.testDouble",  334.32);
			assertMapPathValueIs(result, "Project.0.testLong",    5.0);
			assertMapPathValueIs(result, "Project.0.testInt",     235.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(testLong: { _equals: 22}) { testBoolean, testDouble, testLong, testInt } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.0.testDouble",  323.22);
			assertMapPathValueIs(result, "Project.0.testLong",    22.0);
			assertMapPathValueIs(result, "Project.0.testInt",     452.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(testInt: { _equals: 2345}) { testBoolean, testDouble, testLong, testInt } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.testBoolean", false);
			assertMapPathValueIs(result, "Project.0.testDouble",  124.52);
			assertMapPathValueIs(result, "Project.0.testLong",    563.0);
			assertMapPathValueIs(result, "Project.0.testInt",     2345.0);
		}

	}

	@Test
	public void testRemotePropertiesForCorrectInputType() {

		try (final Tx tx = app.tx()) {


			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "HAS", Relation.Cardinality.OneToMany, "project", "tasks");

			task.addBooleanProperty("testBoolean").setIndexed(true);
			task.addLongProperty("testLong").setIndexed(true);
			task.addNumberProperty("testDouble").setIndexed(true);
			task.addIntegerProperty("testInt").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
			final Class taskType    = StructrApp.getConfiguration().getNodeEntityClass("Task");

			final PropertyKey projectKey  = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "project");
			final PropertyKey testBoolean = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "testBoolean");
			final PropertyKey testDouble  = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "testDouble");
			final PropertyKey testLong    = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "testLong");
			final PropertyKey testInt     = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "testInt");

			app.create(taskType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  252.52),
				new NodeAttribute<>(testLong,    234532L),
				new NodeAttribute<>(testInt,     4563332),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project1"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(testDouble,  124.52),
				new NodeAttribute<>(testLong,    563L),
				new NodeAttribute<>(testInt,     2345),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project2"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  323.22),
				new NodeAttribute<>(testLong,    22L),
				new NodeAttribute<>(testInt,     452),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project3"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(testDouble,  334.32),
				new NodeAttribute<>(testLong,    5L),
				new NodeAttribute<>(testInt,     235),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project4"))
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { testBoolean: { _equals: true }}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",                     2);
			assertMapPathValueIs(result, "Project.0.name",                "Project1");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  252.52);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    234532.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     4563332.0);
			assertMapPathValueIs(result, "Project.1.name",                "Project3");
			assertMapPathValueIs(result, "Project.1.tasks.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.1.tasks.0.testDouble",  323.22);
			assertMapPathValueIs(result, "Project.1.tasks.0.testLong",    22.0);
			assertMapPathValueIs(result, "Project.1.tasks.0.testInt",     452.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { testDouble: { _equals: 334.32}}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.name",                "Project4");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", false);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  334.32);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    5.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     235.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { testLong: { _equals: 22}}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.name",                "Project3");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  323.22);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    22.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     452.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { testInt: { _equals: 2345}}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",                     1);
			assertMapPathValueIs(result, "Project.0.name",                "Project2");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", false);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  124.52);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    563.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     2345.0);
		}

	}

	@Test
	public void testRemotePropertiesWithMultipleInstances() {

		try (final Tx tx = app.tx()) {


			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "HAS", Relation.Cardinality.OneToOne, "project", "task");

			task.addBooleanProperty("testBoolean").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
			final Class taskType    = StructrApp.getConfiguration().getNodeEntityClass("Task");

			final PropertyKey projectKey  = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "project");
			final PropertyKey testBoolean = StructrApp.getConfiguration().getPropertyKeyForJSONName(taskType, "testBoolean");

			app.create(taskType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project1"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project2"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project3"))
			);

			app.create(taskType,
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(projectKey,  app.create(projectType, "Project4"))
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(task: { testBoolean: { _equals: true }}) { name, task { testBoolean } } }");
			assertMapPathValueIs(result, "Project.#",                     2);
			assertMapPathValueIs(result, "Project.0.name",             "Project1");
			assertMapPathValueIs(result, "Project.0.task.testBoolean", true);
			assertMapPathValueIs(result, "Project.1.name",             "Project3");
			assertMapPathValueIs(result, "Project.1.task.testBoolean", true);
		}
	}

	// ----- private methods -----
	private Map<String, Object> fetchGraphQL(final String query) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body(query)

			.expect()
				.statusCode(200)

			.when()
				.post("/")

			.andReturn()
			.as(Map.class);
	}

	private void assertMapPathValueIs(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					final List list = (List)current;
					if (index >= list.size()) {

						// value for nonexisting fields must be null
						assertEquals("Invalid map path result for " + mapPath, value, null);

						// nothing more to check here
						return;

					} else {

						current = list.get(index);
					}
				}

			} else if ("#".equals(part) && current instanceof List) {

				assertEquals("Invalid collection size for " + mapPath, value, ((List)current).size());

				// nothing more to check here
				return;

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		assertEquals("Invalid map path result for " + mapPath, value, current);
	}
}
