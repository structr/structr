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
package org.structr.test.graphql;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrGraphQLTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.fail;

/**
 *
 *
 */
public class GraphQLTest extends StructrGraphQLTest {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLTest.class.getName());

	@Test
	public void testBasics() {

		RestAssured.basePath = "/structr/graphql";

		NodeInterface group  = null;
		NodeInterface tester = null;
		String groupId       = null;
		String testerId      = null;

		try (final Tx tx = app.tx()) {

			final PropertyKey<List> membersKey = Traits.of(StructrTraits.GROUP).key(GroupTraitDefinition.MEMBERS_PROPERTY);

			tester = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester"));
			group  = app.create(StructrTraits.GROUP,
				new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestGroup"),
				new NodeAttribute<>(membersKey, Arrays.asList(tester))
			);

			groupId  = group.getUuid();
			testerId = tester.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String query1 = "{ Group { id, type, name, members { id, type, name } }, Principal(_pageSize: 1, _sort: \"name\") { id, type name }}";
		final String query2 = "{ Group { id, type, name, members { } }}";
		final String query3 = "{ Group(id: \"" + groupId + "\") { id, type, name }}";

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.body(query1)

			.expect()
				.statusCode(200)
				.body(StructrTraits.GROUP,                    hasSize(1))
				.body(StructrTraits.PRINCIPAL,                hasSize(1))
				.body("Group[0].id",              equalTo(groupId))
				.body("Group[0].type",            equalTo(StructrTraits.GROUP))
				.body("Group[0].name",            equalTo("TestGroup"))
				.body("Group[0].members[0].id",   equalTo(testerId))
				.body("Group[0].members[0].type", equalTo(StructrTraits.USER))
				.body("Group[0].members[0].name", equalTo("tester"))
				.body("Principal[0].id",          equalTo(groupId))
				.body("Principal[0].type",        equalTo(StructrTraits.GROUP))
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
				.body("message", equalTo("Invalid Syntax : offending token '}' at line 2 column 36"))
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
				.body(StructrTraits.GROUP,                    hasSize(1))
				.body("Group[0].id",              equalTo(groupId))
				.body("Group[0].type",            equalTo(StructrTraits.GROUP))
				.body("Group[0].name",            equalTo("TestGroup"))

			.when()
				.post("/");
	}

	@Test
	public void testAdvancedQueries() {

		final List<NodeInterface> templates = new LinkedList<>();
		final List<String> templateIds      = new LinkedList<>();
		final List<NodeInterface> team      = new LinkedList<>();
		final List<String> teamIds          = new LinkedList<>();
		NodeInterface group                 = null;

		try (final Tx tx = app.tx()) {

			final PropertyKey<List> membersKey = Traits.of(StructrTraits.GROUP).key(GroupTraitDefinition.MEMBERS_PROPERTY);

			final NodeInterface christian2 = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Christian"));
			final NodeInterface susanne    = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Susanne"));
			final NodeInterface lukas      = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Lukas"));
			final NodeInterface kai        = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Kai"));
			final NodeInterface michael    = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Michael"));
			final NodeInterface ines       = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Inès"));
			final NodeInterface axel       = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Axel"));
			final NodeInterface christian1 = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Christian"));
			final NodeInterface tobias     = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Tobias"));

			team.add(axel);
			team.add(christian1);
			team.add(christian2);
			team.add(ines);
			team.add(kai);
			team.add(lukas);
			team.add(michael);
			team.add(susanne);
			team.add(tobias);

			group  = app.create(StructrTraits.GROUP,
				new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Structr Team"),
				new NodeAttribute<>(membersKey, team)
			);

			app.create(StructrTraits.GROUP,
				new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "All teams"),
				new NodeAttribute<>(membersKey, Arrays.asList(group))
			);

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate4"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "zrtsga"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(2))
			));

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate2"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "lertdf"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(0))
			));

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate5"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "tzegsg"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(3))
			));

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate3"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "asgw"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(1))
			));

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate6"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "dfjgr"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(4))
			));

			templates.add(app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "MailTemplate1"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "abcdef"),
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), team.get(0))
			));

			for (final NodeInterface t : templates) {
				templateIds.add(t.getUuid());
			}

			for (final NodeInterface t : team) {
				teamIds.add(t.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal(_sort: \"name\") { id, type, name } }");
			assertMapPathValueIs(result, "Principal.#",      11);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Principal(id: \"" + teamIds.get(0) + "\") { id, type, name } }");
			assertMapPathValueIs(result, "Principal.#",      1);
			assertMapPathValueIs(result, "Principal.0.id",   teamIds.get(0));
			assertMapPathValueIs(result, "Principal.0.type", StructrTraits.USER);
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
			final Map<String, Object> result = fetchGraphQL("{ Principal(_sort: \"name\") { name(_contains: \"a\", _contains: \"l\", _conj: \"and\") } }");
			assertMapPathValueIs(result, "Principal.#",      4);
			assertMapPathValueIs(result, "Principal.0.name", "All teams");
			assertMapPathValueIs(result, "Principal.1.name", "Axel");
			assertMapPathValueIs(result, "Principal.2.name", "Lukas");
			assertMapPathValueIs(result, "Principal.3.name", "Michael");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_pageSize: 1, _sort: \"name\") { name }, Principal(_pageSize: 2, _page: 2, _sort: \"name\") { name(_contains: \"i\") } }");
			assertMapPathValueIs(result, "Group.#",          1);
			assertMapPathValueIs(result, "Group.0.name",     "All teams");
			assertMapPathValueIs(result, "Principal.#",      2);
			assertMapPathValueIs(result, "Principal.0.name", "Inès");
			assertMapPathValueIs(result, "Principal.1.name", "Kai");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { name, members(_pageSize: 2, _page: 2) { name }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           "All teams");
			assertMapPathValueIs(result, "Group.0.members",        new LinkedList<>());
			assertMapPathValueIs(result, "Group.1.name",           "Structr Team");
			assertMapPathValueIs(result, "Group.1.members.#",      2);
			assertMapPathValueIs(result, "Group.1.members.0.name", "Christian");
			assertMapPathValueIs(result, "Group.1.members.1.name", "Inès");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { name, members(_sort: \"name\") { name }}}");
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
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { name, members(_sort: \"name\", _desc: true) { name }}}");
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
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { members { name(_contains: \"k\", _contains: \"l\", _conj: \"and\") }}}");
			assertMapPathValueIs(result, "Group.#",                2);
			assertMapPathValueIs(result, "Group.0.name",           null);
			assertMapPathValueIs(result, "Group.0.members",        new LinkedList<>());
			assertMapPathValueIs(result, "Group.1.members.0.name", "Lukas");
			assertMapPathValueIs(result, "Group.1.members.1",      null);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { members { name(_contains: \"k\", _contains: \"l\", _conj: \"or\") }}}");
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
			assertMapPathValueIs(result, "MailTemplate.0.id",          templateIds.get(1));
			assertMapPathValueIs(result, "MailTemplate.0.type",        StructrTraits.MAIL_TEMPLATE);
			assertMapPathValueIs(result, "MailTemplate.0.text",        "MailTemplate2");
			assertMapPathValueIs(result, "MailTemplate.0.name",        null);
			assertMapPathValueIs(result, "MailTemplate.0.owner.name",  "Axel");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(_sort: \"owner.name\") { name, owner { name }}}");
			assertMapPathValueIs(result, "MailTemplate.#",                6);
			assertMapPathValueIs(result, "MailTemplate.0.owner.name",     "Axel");
			assertMapPathValueIs(result, "MailTemplate.1.owner.name",     "Axel");
			assertMapPathValueIs(result, "MailTemplate.2.owner.name",     "Christian");
			assertMapPathValueIs(result, "MailTemplate.3.owner.name",     "Christian");
			assertMapPathValueIs(result, "MailTemplate.4.owner.name",     "Inès");
			assertMapPathValueIs(result, "MailTemplate.5.owner.name",     "Kai");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Group(_sort: \"name\") { name, members(_sort: \"name\", _desc: true) { name }}}");
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
			assertMapPathValueIs(result, "MailTemplate.0.id",         templateIds.get(5));
			assertMapPathValueIs(result, "MailTemplate.0.type",       StructrTraits.MAIL_TEMPLATE);
			assertMapPathValueIs(result, "MailTemplate.0.name",       "abcdef");
			assertMapPathValueIs(result, "MailTemplate.0.owner.name", "Axel");
			assertMapPathValueIs(result, "MailTemplate.1.id",         templateIds.get(1));
			assertMapPathValueIs(result, "MailTemplate.1.type",       StructrTraits.MAIL_TEMPLATE);
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

			project.relate(task, "HAS", Cardinality.ManyToMany, "projects", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final String project               = "Project";
		final String task                  = "Task";
		final PropertyKey tasksKey         = Traits.of(project).key("tasks");

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
			//final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: \"task1\"}}) { name, tasks { name }}}");
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: [\"task1\", \"task2\", \"task3\", \"task4\", \"task5\"] }}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: [\"task2\", \"task3\", \"task4\", \"task5\"] }}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: [\"task3\", \"task4\", \"task5\"] }}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: [\"task4\", \"task5\"] }}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _equals: [\"task5\"] }}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            5);
		}




		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project1\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            5);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project2\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            4);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project3\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            3);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project4\"}}) { name, projects { name }}}");
			assertMapPathValueIs(result, "Task.#",            2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projects: { name: { _contains: \"project5\"}}) { name, projects { name }}}");
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

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);


			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final String project                = "Project";
		final String task                   = "Task";
		final PropertyKey tasksKey         = Traits.of(project).key("tasks");

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
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task1\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task3\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task5\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task7\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task9\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}




		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", project: { name: { _contains: \"project1\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task0");
			assertMapPathValueIs(result, "Task.1.name",            "task1");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", project: { name: { _contains: \"project2\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task2");
			assertMapPathValueIs(result, "Task.1.name",            "task3");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", project: { name: { _contains: \"project3\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task4");
			assertMapPathValueIs(result, "Task.1.name",            "task5");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", project: { name: { _contains: \"project4\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task6");
			assertMapPathValueIs(result, "Task.1.name",            "task7");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", project: { name: { _contains: \"project5\"}}) { name, project { name }}}");
			assertMapPathValueIs(result, "Task.#",                 2);
			assertMapPathValueIs(result, "Task.0.name",            "task8");
			assertMapPathValueIs(result, "Task.1.name",            "task9");
		}
	}

	@Test
	public void testAdvancedQueriesManyToOne() {

		// test data setup
		try (final Tx tx = app.tx()) {

			final NodeInterface p1 = app.create(StructrTraits.USER, "p1");
			final NodeInterface p2 = app.create(StructrTraits.USER, "p2");
			final NodeInterface m1 = app.create(StructrTraits.MAIL_TEMPLATE, "m1");
			final NodeInterface m2 = app.create(StructrTraits.MAIL_TEMPLATE, "m2");
			final NodeInterface m3 = app.create(StructrTraits.MAIL_TEMPLATE, "m3");
			final NodeInterface m4 = app.create(StructrTraits.MAIL_TEMPLATE, "m4");

			m1.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p1);
			m2.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p1);
			m3.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p2);
			m4.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p2);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate(_sort: \"name\", owner: { name: { _equals: \"p2\"}}) { name }}");
			assertMapPathValueIs(result, "MailTemplate.#",                 2);
			assertMapPathValueIs(result, "MailTemplate.0.name",            "m3");
			assertMapPathValueIs(result, "MailTemplate.1.name",            "m4");
		}

	}

	@Test
	public void testAdvancedQueriesManyToOneWithEdgeCases() {

		// test data setup
		try (final Tx tx = app.tx()) {

			final NodeInterface p2 = app.create(StructrTraits.USER, "Second Tester");
			final NodeInterface p1 = app.create(StructrTraits.USER, "First Tester");
			final NodeInterface m3 = app.create(StructrTraits.MAIL_TEMPLATE, "Third Template");
			final NodeInterface m2 = app.create(StructrTraits.MAIL_TEMPLATE, "Second Template");
			final NodeInterface m5 = app.create(StructrTraits.MAIL_TEMPLATE, "Fifth Template");
			final NodeInterface m1 = app.create(StructrTraits.MAIL_TEMPLATE, "First Template");
			final NodeInterface m6 = app.create(StructrTraits.MAIL_TEMPLATE, "Sixth Template");
			final NodeInterface m4 = app.create(StructrTraits.MAIL_TEMPLATE, "Fourth Template");

			m1.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p1);
			m2.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p1);
			m3.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p2);
			m4.setProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p2);

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
			final Map<String, Object> result = fetchGraphQL("{ MailTemplate( _sort: \"name\", owner: { name: { _equals: \"First Tester\"}}) { name }}");
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

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// test data setup
		try (final Tx tx = app.tx()) {

			final String type = "FunctionTest";

			app.create(type, "test2");
			app.create(type, "test1");

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
			final Map<String, Object> result = fetchGraphQL("{ FunctionTest(stringTest: {_contains: \"e\"}, _sort: \"name\") { id, type, name, stringTest, boolTest }}");
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
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field members @ 'Group/members'"))
				.body("errors[0].locations[0].line",   equalTo(2))
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
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field owner @ 'Group/owner'"))
				.body("errors[0].locations[0].line",   equalTo(2))
				.body("errors[0].locations[0].column", equalTo(27))
				.body("errors[0].description",         equalTo("Sub selection required for type Principal of field owner"))
				.body("errors[0].validationErrorType", equalTo("SubSelectionRequired"))

			.when()
				.post("/");
	}

	@Test
	public void testFunctionPropertyTypeHint() {

		RestAssured.basePath = "/structr/graphql";

		List<String> childrenIds     = new LinkedList<>();
		List<NodeInterface> children = null;
		NodeInterface user           = null;
		String userId                = null;

		try (final Tx tx = app.tx()) {

			final JsonSchema schema             = StructrSchema.createFromDatabase(app);
			final JsonObjectType type           = schema.addType("Test");
			final JsonObjectType tmpType        = schema.addType("Tmp");

			type.relate(tmpType, "TMP", Cardinality.OneToMany, "parent", "children");

			type.addFunctionProperty("test1").setReadFunction("'test'").setTypeHint("String");
			type.addFunctionProperty("test2").setReadFunction("false").setTypeHint("Boolean");
			type.addFunctionProperty("test3").setReadFunction("int(42)").setTypeHint("Int");
			type.addFunctionProperty("test4").setReadFunction("12.34").setTypeHint("Double");
			type.addFunctionProperty("test5").setReadFunction("7465423674522").setTypeHint("Long");
			type.addFunctionProperty("test6").setReadFunction("this.owner").setTypeHint(StructrTraits.PRINCIPAL);
			type.addFunctionProperty("test7").setReadFunction("this.children").setTypeHint("Tmp[]");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();


		} catch (Throwable t) {

			t.printStackTrace();

			fail("Unexpected exception.");
		}

		// create test node
		try (final Tx tx = app.tx()) {

			user = app.create(StructrTraits.USER, "tester");

			userId = user.getUuid();

			final String tmpType  = "Tmp";
			final String testType = "Test";

			final PropertyKey nameKey     = Traits.of(testType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey ownerKey    = Traits.of(testType).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY);
			final PropertyKey childrenKey = Traits.of(testType).key("children");

			children = createTestNodes(tmpType, 10);

			for (final NodeInterface c : children) {
				childrenIds.add(c.getUuid());
			}

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
		assertMapPathValueIs(result, "Test.0.test6.id",     userId);
		assertMapPathValueIs(result, "Test.0.test6.type",   StructrTraits.USER);
		assertMapPathValueIs(result, "Test.0.test6.name",   "tester");
		assertMapPathValueIs(result, "Test.0.test7.#",      10);

		for (int i=0; i<10; i++) {
			assertMapPathValueIs(result, "Test.0.test7." + i + ".id",   childrenIds.get(i));
			assertMapPathValueIs(result, "Test.0.test7." + i + ".type", "Tmp");
		}

		final String query = "{ Test { test6, test7 } }";

		RestAssured.given()

				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.body(query)

			.expect()
				.statusCode(422)
				.body("errors[0].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Principal of field test6 @ 'Test/test6'"))
				.body("errors[0].locations[0].line",   equalTo(2))
				.body("errors[0].locations[0].column", equalTo(10))
				.body("errors[0].description",         equalTo("Sub selection required for type Principal of field test6"))
				.body("errors[0].validationErrorType", equalTo("SubSelectionRequired"))
				.body("errors[1].message",             equalTo("Validation error of type SubSelectionRequired: Sub selection required for type Tmp of field test7 @ 'Test/test7'"))
				.body("errors[1].locations[0].line",   equalTo(2))
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

			extProject1.addTrait("Project");
			extProject2.addTrait("Project");

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final String extProject            = "ExtendedProject1";
		final String task                  = "Task";
		final PropertyKey tasksKey         = Traits.of(extProject).key("tasks");

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
			final Map<String, Object> result = fetchGraphQL("{ Project(tasks: { name: { _contains: \"task1\"}}) { name, tasks { name }}}");
			assertMapPathValueIs(result, "Project.#",            1);
		}
	}

	@Test
	public void testCountPropertyType() {

		try (final Tx tx = app.tx()) {

			final NodeInterface projectType = app.create(StructrTraits.SCHEMA_NODE, "Project");
			final NodeInterface taskType    = app.create(StructrTraits.SCHEMA_NODE, "Task");

			final NodeInterface rel = app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY), projectType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY), taskType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), "TASK"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "1"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY), "project"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY), "tasks")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   projectType),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),         "taskCount"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Count"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY),       "tasks")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final String projectType = "Project";
			final String taskType    = "Task";
			final PropertyKey name  = Traits.of(projectType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey tasks = Traits.of(projectType).key("tasks");


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

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");

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
			p3.setFormat("open,closed,cancelled");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> projects = new LinkedList<>();
		final List<NodeInterface> tasks    = new LinkedList<>();
		final List<String> projectIds      = new LinkedList<>();
		final List<String> taskIds         = new LinkedList<>();
		final String project                = "Project";
		final String task                   = "Task";
		final PropertyKey tasksKey         = Traits.of(project).key("tasks");
		final PropertyKey statusKey        = Traits.of(task).key("status");
		final PropertyKey nameKey          = Traits.of(task).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = app.tx()) {

			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task0"), new NodeAttribute<>(statusKey, "open")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task1"), new NodeAttribute<>(statusKey, "closed")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task2"), new NodeAttribute<>(statusKey, "cancelled")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task3"), new NodeAttribute<>(statusKey, "open")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task4"), new NodeAttribute<>(statusKey, "closed")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task5"), new NodeAttribute<>(statusKey, "cancelled")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task6"), new NodeAttribute<>(statusKey, "open")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task7"), new NodeAttribute<>(statusKey, "closed")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task8"), new NodeAttribute<>(statusKey, "cancelled")));
			tasks.add(app.create(task, new NodeAttribute<>(nameKey, "task9"), new NodeAttribute<>(statusKey, "open")));

			final NodeInterface project3 = app.create(project, "project3");
			final NodeInterface project4 = app.create(project, "project4");
			final NodeInterface project1 = app.create(project, "project1");
			final NodeInterface project2 = app.create(project, "project2");
			final NodeInterface project5 = app.create(project, "project5");

			projects.add(project1);
			projects.add(project2);
			projects.add(project3);
			projects.add(project4);
			projects.add(project5);

			projects.get(0).setProperty(tasksKey, tasks.subList(0,  2));
			projects.get(1).setProperty(tasksKey, tasks.subList(2,  4));
			projects.get(2).setProperty(tasksKey, tasks.subList(4,  6));
			projects.get(3).setProperty(tasksKey, tasks.subList(6,  8));
			projects.get(4).setProperty(tasksKey, tasks.subList(8, 10));

			for (final NodeInterface p : projects) {
				projectIds.add(p.getUuid());
			}

			for (final NodeInterface t : tasks) {
				taskIds.add(t.getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(_sort: \"name\") { id, type, name, taskCount, tasks { id, type, name, projectId, status }}}");
			assertMapPathValueIs(result, "Project.#",                   5);
			assertMapPathValueIs(result, "Project.0.name",              "project1");
			assertMapPathValueIs(result, "Project.0.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.0.tasks.#",           2);
			assertMapPathValueIs(result, "Project.0.tasks.0.name",      "task0");
			assertMapPathValueIs(result, "Project.0.tasks.0.status",    "open");
			assertMapPathValueIs(result, "Project.0.tasks.0.projectId", projectIds.get(0));
			assertMapPathValueIs(result, "Project.0.tasks.1.name",      "task1");
			assertMapPathValueIs(result, "Project.0.tasks.1.status",    "closed");
			assertMapPathValueIs(result, "Project.0.tasks.1.projectId", projectIds.get(0));

			assertMapPathValueIs(result, "Project.1.name",              "project2");
			assertMapPathValueIs(result, "Project.1.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.1.tasks.#",           2);
			assertMapPathValueIs(result, "Project.1.tasks.0.name",      "task2");
			assertMapPathValueIs(result, "Project.1.tasks.0.status",    "cancelled");
			assertMapPathValueIs(result, "Project.1.tasks.0.projectId", projectIds.get(1));
			assertMapPathValueIs(result, "Project.1.tasks.1.name",      "task3");
			assertMapPathValueIs(result, "Project.1.tasks.1.status",    "open");
			assertMapPathValueIs(result, "Project.1.tasks.1.projectId", projectIds.get(1));

			assertMapPathValueIs(result, "Project.2.name",              "project3");
			assertMapPathValueIs(result, "Project.2.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.2.tasks.#",           2);
			assertMapPathValueIs(result, "Project.2.tasks.0.name",      "task4");
			assertMapPathValueIs(result, "Project.2.tasks.0.status",    "closed");
			assertMapPathValueIs(result, "Project.2.tasks.0.projectId", projectIds.get(2));
			assertMapPathValueIs(result, "Project.2.tasks.1.name",      "task5");
			assertMapPathValueIs(result, "Project.2.tasks.1.status",    "cancelled");
			assertMapPathValueIs(result, "Project.2.tasks.1.projectId", projectIds.get(2));

			assertMapPathValueIs(result, "Project.3.name",              "project4");
			assertMapPathValueIs(result, "Project.3.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.3.tasks.#",           2);
			assertMapPathValueIs(result, "Project.3.tasks.0.name",      "task6");
			assertMapPathValueIs(result, "Project.3.tasks.0.status",    "open");
			assertMapPathValueIs(result, "Project.3.tasks.0.projectId", projectIds.get(3));
			assertMapPathValueIs(result, "Project.3.tasks.1.name",      "task7");
			assertMapPathValueIs(result, "Project.3.tasks.1.status",    "closed");
			assertMapPathValueIs(result, "Project.3.tasks.1.projectId", projectIds.get(3));

			assertMapPathValueIs(result, "Project.4.name",              "project5");
			assertMapPathValueIs(result, "Project.4.taskCount",         2.0);
			assertMapPathValueIs(result, "Project.4.tasks.#",           2);
			assertMapPathValueIs(result, "Project.4.tasks.0.name",      "task8");
			assertMapPathValueIs(result, "Project.4.tasks.0.status",    "cancelled");
			assertMapPathValueIs(result, "Project.4.tasks.0.projectId", projectIds.get(4));
			assertMapPathValueIs(result, "Project.4.tasks.1.name",      "task9");
			assertMapPathValueIs(result, "Project.4.tasks.1.status",    "open");
			assertMapPathValueIs(result, "Project.4.tasks.1.projectId", projectIds.get(4));
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + taskIds.get(0) + "\") { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projectIds.get(0));
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task { id(_equals: \"" + taskIds.get(0) + "\"), name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projectIds.get(0));
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + taskIds.get(0) + "\", status: { _equals: \"open\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projectIds.get(0));
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + taskIds.get(0) + "\", status: { _equals: \"closed\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + taskIds.get(0) + "\", projectId: { _equals: \"" + projectIds.get(0) + "\" }) { name, status, projectId }}");

			assertMapPathValueIs(result, "Task.#",              1);
			assertMapPathValueIs(result, "Task.0.name",         "task0");
			assertMapPathValueIs(result, "Task.0.status",       "open");
			assertMapPathValueIs(result, "Task.0.projectId",    projectIds.get(0));
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(id: \"" + taskIds.get(0) + "\", projectId: { _equals: \"" + projectIds.get(1) + "\" }) { name, status, projectId }}");

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
			project.addDoubleProperty("testDouble").setIndexed(true);
			project.addIntegerProperty("testInt").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final String projectType = "Project";

			final PropertyKey<String> nameKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey testBoolean = Traits.of(projectType).key("testBoolean");
			final PropertyKey testDouble  = Traits.of(projectType).key("testDouble");
			final PropertyKey testLong    = Traits.of(projectType).key("testLong");
			final PropertyKey testInt     = Traits.of(projectType).key("testInt");

			app.create(projectType,
				new NodeAttribute<>(nameKey,     "Project1"),
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  252.52),
				new NodeAttribute<>(testLong,    234532L),
				new NodeAttribute<>(testInt,     4563332)
			);

			app.create(projectType,
				new NodeAttribute<>(nameKey,     "Project2"),
				new NodeAttribute<>(testBoolean, false),
				new NodeAttribute<>(testDouble,  124.52),
				new NodeAttribute<>(testLong,    563L),
				new NodeAttribute<>(testInt,     2345)
			);

			app.create(projectType,
				new NodeAttribute<>(nameKey,     "Project3"),
				new NodeAttribute<>(testBoolean, true),
				new NodeAttribute<>(testDouble,  323.22),
				new NodeAttribute<>(testLong,    22L),
				new NodeAttribute<>(testInt,     452)
			);

			app.create(projectType,
				new NodeAttribute<>(nameKey,     "Project4"),
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
			final Map<String, Object> result = fetchGraphQL("{ Project(testBoolean: { _equals: true}, _sort: \"name\") { testBoolean, testDouble, testLong, testInt } }");
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

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");

			task.addBooleanProperty("testBoolean").setIndexed(true);
			task.addLongProperty("testLong").setIndexed(true);
			task.addDoubleProperty("testDouble").setIndexed(true);
			task.addIntegerProperty("testInt").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final String projectType = "Project";
			final String taskType    = "Task";

			final PropertyKey projectKey  = Traits.of(taskType).key("project");
			final PropertyKey testBoolean = Traits.of(taskType).key("testBoolean");
			final PropertyKey testDouble  = Traits.of(taskType).key("testDouble");
			final PropertyKey testLong    = Traits.of(taskType).key("testLong");
			final PropertyKey testInt     = Traits.of(taskType).key("testInt");

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
			final Map<String, Object> result = fetchGraphQL("{ Project( _sort: \"name\", tasks: { testBoolean: { _equals: true }}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
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
			final Map<String, Object> result = fetchGraphQL("{ Project( _sort: \"name\", tasks: { testDouble: { _equals: 334.32}}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.name",                "Project4");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", false);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  334.32);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    5.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     235.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project( _sort: \"name\", tasks: { testLong: { _contains: 22 }}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
			assertMapPathValueIs(result, "Project.#",           1);
			assertMapPathValueIs(result, "Project.0.name",                "Project3");
			assertMapPathValueIs(result, "Project.0.tasks.0.testBoolean", true);
			assertMapPathValueIs(result, "Project.0.tasks.0.testDouble",  323.22);
			assertMapPathValueIs(result, "Project.0.tasks.0.testLong",    22.0);
			assertMapPathValueIs(result, "Project.0.tasks.0.testInt",     452.0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Project( _sort: \"name\", tasks: { testInt: { _contains: 2345}}) { name, tasks { testBoolean, testDouble, testLong, testInt } } }");
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

			project.relate(task, "HAS", Cardinality.OneToOne, "project", "task");

			task.addBooleanProperty("testBoolean").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final String projectType = "Project";
			final String taskType    = "Task";

			final PropertyKey projectKey  = Traits.of(taskType).key("project");
			final PropertyKey testBoolean = Traits.of(taskType).key("testBoolean");

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
			final Map<String, Object> result = fetchGraphQL("{ Project( _sort: \"name\", task: { testBoolean: { _equals: true }}) { name, task { testBoolean } } }");
			assertMapPathValueIs(result, "Project.#",                     2);
			assertMapPathValueIs(result, "Project.0.name",             "Project1");
			assertMapPathValueIs(result, "Project.0.task.testBoolean", true);
			assertMapPathValueIs(result, "Project.1.name",             "Project3");
			assertMapPathValueIs(result, "Project.1.task.testBoolean", true);
		}
	}

	@Test
	public void testCombinedFilteringOnMultipleProperties() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project    = schema.addType("Project");
			final JsonObjectType identifier = schema.addType("Identifier");

			project.relate(identifier, "HAS",    Cardinality.OneToOne, "project", "identifier");

			identifier.addStringProperty("test1").setIndexed(true);
			identifier.addStringProperty("test2").setIndexed(true);
			identifier.addStringProperty("test3").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<NodeInterface> identifiers = new LinkedList<>();
		final String project                   = "Project";
		final String identifier                = "Identifier";
		final PropertyKey projectNameKey      = Traits.of(project).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey identifierKey       = Traits.of(project).key("identifier");
		final PropertyKey test1Key            = Traits.of(identifier).key("test1");
		final PropertyKey test2Key            = Traits.of(identifier).key("test2");
		final PropertyKey test3Key            = Traits.of(identifier).key("test3");

		try (final Tx tx = app.tx()) {

			identifiers.add(app.create(identifier, new NodeAttribute<>(test1Key, "aaa"), new NodeAttribute<>(test2Key, "bbb"), new NodeAttribute<>(test3Key, "ddd")));
			identifiers.add(app.create(identifier, new NodeAttribute<>(test1Key, "aaa"), new NodeAttribute<>(test2Key, "bbb"), new NodeAttribute<>(test3Key, "eee")));
			identifiers.add(app.create(identifier, new NodeAttribute<>(test1Key, "aaa"), new NodeAttribute<>(test2Key, "ccc"), new NodeAttribute<>(test3Key, "fff")));
			identifiers.add(app.create(identifier, new NodeAttribute<>(test1Key, "aaa"), new NodeAttribute<>(test2Key, "ccc"), new NodeAttribute<>(test3Key, "ggg")));
			identifiers.add(app.create(identifier, new NodeAttribute<>(test1Key, "zzz"), new NodeAttribute<>(test2Key, "zzz"), new NodeAttribute<>(test3Key, "zzz")));

			app.create(project, new NodeAttribute<>(projectNameKey, "project1"), new NodeAttribute<>(identifierKey, identifiers.get(0)));
			app.create(project, new NodeAttribute<>(projectNameKey, "project2"), new NodeAttribute<>(identifierKey, identifiers.get(1)));
			app.create(project, new NodeAttribute<>(projectNameKey, "project3"), new NodeAttribute<>(identifierKey, identifiers.get(2)));
			app.create(project, new NodeAttribute<>(projectNameKey, "project4"), new NodeAttribute<>(identifierKey, identifiers.get(3)));
			app.create(project, new NodeAttribute<>(projectNameKey, "project5"), new NodeAttribute<>(identifierKey, identifiers.get(4)));
			app.create(project, new NodeAttribute<>(projectNameKey, "project6"));
			app.create(project, new NodeAttribute<>(projectNameKey, "project7"));
			app.create(project, new NodeAttribute<>(projectNameKey, "project8"));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		final String body = "{ name, identifier { test1, test2, test3 }}";

		// check _equals
		assertCount("{ Project " + body + "}", "Project.#", 8);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }}) " + body + "}", "Project.#", 4);
		assertCount("{ Project(identifier: { test1: { _equals: \"xxx\" }}) " + body + "}", "Project.#", 0);

		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }, test2: { _equals: \"bbb\" }}) " + body + "}", "Project.#", 2);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }, test2: { _equals: \"ddd\" }}) " + body + "}", "Project.#", 0);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }}, identifier: { test2: { _equals: \"bbb\" }}) " + body + "}", "Project.#", 2);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }}, identifier: { test2: { _equals: \"ddd\" }}) " + body + "}", "Project.#", 0);

		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }, test2: { _equals: \"bbb\" }, test3: { _equals: \"eee\" }}) " + body + "}", "Project.#", 1);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }, test2: { _equals: \"bbb\" }, test3: { _equals: \"fff\" }}) " + body + "}", "Project.#", 0);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }}, identifier: { test2: { _equals: \"bbb\" }}, identifier: {  test3: { _equals: \"eee\" }}) " + body + "}", "Project.#", 1);
		assertCount("{ Project(identifier: { test1: { _equals: \"aaa\" }}, identifier: { test2: { _equals: \"bbb\" }}, identifier: {  test3: { _equals: \"fff\" }}) " + body + "}", "Project.#", 0);

		// same for _contains
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }}) " + body + "}", "Project.#", 4);

		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }, test2: { _contains: \"b\" }}) " + body + "}", "Project.#", 2);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }, test2: { _contains: \"d\" }}) " + body + "}", "Project.#", 0);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }}, identifier: { test2: { _contains: \"b\" }}) " + body + "}", "Project.#", 2);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }}, identifier: { test2: { _contains: \"d\" }}) " + body + "}", "Project.#", 0);
		assertCount("{ Project(_page: 1, _pageSize: 100, _sort: \"name\", _desc: false, identifier: { test1: { _contains: \"a\" }, test2: { _contains: \"b\" }}) " + body + "}", "Project.#", 2);

		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }, test2: { _contains: \"b\" }, test3: { _contains: \"e\" }}) " + body + "}", "Project.#", 1);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }, test2: { _contains: \"b\" }, test3: { _contains: \"f\" }}) " + body + "}", "Project.#", 0);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }}, identifier: { test2: { _contains: \"b\" }}, identifier: {  test3: { _contains: \"e\" }}) " + body + "}", "Project.#", 1);
		assertCount("{ Project(identifier: { test1: { _contains: \"a\" }}, identifier: { test2: { _contains: \"b\" }}, identifier: {  test3: { _contains: \"f\" }}) " + body + "}", "Project.#", 0);
	}

	@Test
	public void testCombinedFilteringOnMultiplePropertiesAndCardinalities() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType root      = schema.addType("Root");
			final JsonObjectType oneToOne  = schema.addType("OneToOneTest");
			final JsonObjectType oneToMany = schema.addType("OneToManyTest");
			final JsonObjectType manyToOne = schema.addType("ManyToOneTest");
			final JsonObjectType manyToMany = schema.addType("ManyToManyTest");

			root.relate(oneToOne,   "oneToOne",   Cardinality.OneToOne,   "rootOneToOne",   "oneToOne");
			root.relate(oneToMany,  "oneToMany",  Cardinality.OneToMany,  "rootOneToMany",  "oneToMany");
			root.relate(manyToOne,  "manyToOne",  Cardinality.ManyToOne,  "rootManyToOne",  "manyToOne");
			root.relate(manyToMany, "manyToMany", Cardinality.ManyToMany, "rootManyToMany", "manyToMany");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String root               = "Root";
		final String oneToOne           = "OneToOneTest";
		final String oneToMany          = "OneToManyTest";
		final String manyToOne          = "ManyToOneTest";
		final String manyToMany         = "ManyToManyTest";
		final PropertyKey nameKey       = Traits.of(root).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey oneToOneKey   = Traits.of(root).key("oneToOne");
		final PropertyKey oneToManyKey  = Traits.of(root).key("oneToMany");
		final PropertyKey manyToOneKey  = Traits.of(root).key("manyToOne");
		final PropertyKey manyToManyKey = Traits.of(root).key("manyToMany");

		try (final Tx tx = app.tx()) {

			final NodeInterface oneToOne0 = app.create(oneToOne, "oneToOne0");
			final NodeInterface oneToOne1 = app.create(oneToOne, "oneToOne1");
			final NodeInterface oneToOne2 = app.create(oneToOne, "oneToOne2");
			final NodeInterface oneToOne3 = app.create(oneToOne, "oneToOne3");
			final NodeInterface oneToOne4 = app.create(oneToOne, "oneToOne4");
			final NodeInterface oneToOne5 = app.create(oneToOne, "oneToOne5");
			final NodeInterface oneToOne6 = app.create(oneToOne, "oneToOne6");
			final NodeInterface oneToOne7 = app.create(oneToOne, "oneToOne7");

			final NodeInterface oneToMany00 = app.create(oneToMany, "oneToMany00");
			final NodeInterface oneToMany01 = app.create(oneToMany, "oneToMany01");
			final NodeInterface oneToMany02 = app.create(oneToMany, "oneToMany02");
			final NodeInterface oneToMany03 = app.create(oneToMany, "oneToMany03");
			final NodeInterface oneToMany04 = app.create(oneToMany, "oneToMany04");
			final NodeInterface oneToMany05 = app.create(oneToMany, "oneToMany05");
			final NodeInterface oneToMany06 = app.create(oneToMany, "oneToMany06");
			final NodeInterface oneToMany07 = app.create(oneToMany, "oneToMany07");
			final NodeInterface oneToMany08 = app.create(oneToMany, "oneToMany08");
			final NodeInterface oneToMany09 = app.create(oneToMany, "oneToMany09");
			final NodeInterface oneToMany10 = app.create(oneToMany, "oneToMany10");
			final NodeInterface oneToMany11 = app.create(oneToMany, "oneToMany11");
			final NodeInterface oneToMany12 = app.create(oneToMany, "oneToMany12");
			final NodeInterface oneToMany13 = app.create(oneToMany, "oneToMany13");
			final NodeInterface oneToMany14 = app.create(oneToMany, "oneToMany14");
			final NodeInterface oneToMany15 = app.create(oneToMany, "oneToMany15");

			final NodeInterface manyToOne0 = app.create(manyToOne, "manyToOne0");
			final NodeInterface manyToOne1 = app.create(manyToOne, "manyToOne1");
			final NodeInterface manyToOne2 = app.create(manyToOne, "manyToOne2");
			final NodeInterface manyToOne3 = app.create(manyToOne, "manyToOne3");
			final NodeInterface manyToOne4 = app.create(manyToOne, "manyToOne4");
			final NodeInterface manyToOne5 = app.create(manyToOne, "manyToOne5");
			final NodeInterface manyToOne6 = app.create(manyToOne, "manyToOne6");
			final NodeInterface manyToOne7 = app.create(manyToOne, "manyToOne7");
			final NodeInterface manyToOne8 = app.create(manyToOne, "manyToOne8");
			final NodeInterface manyToOne9 = app.create(manyToOne, "manyToOne9");

			final NodeInterface manyToMany0 = app.create(manyToMany, "manyToMany0");
			final NodeInterface manyToMany1 = app.create(manyToMany, "manyToMany1");
			final NodeInterface manyToMany2 = app.create(manyToMany, "manyToMany2");
			final NodeInterface manyToMany3 = app.create(manyToMany, "manyToMany3");
			final NodeInterface manyToMany4 = app.create(manyToMany, "manyToMany4");
			final NodeInterface manyToMany5 = app.create(manyToMany, "manyToMany5");
			final NodeInterface manyToMany6 = app.create(manyToMany, "manyToMany6");
			final NodeInterface manyToMany7 = app.create(manyToMany, "manyToMany7");
			final NodeInterface manyToMany8 = app.create(manyToMany, "manyToMany8");

			final KeyData keys = new KeyData(nameKey, oneToOneKey, oneToManyKey, manyToOneKey, manyToManyKey);

			createTestData(app, root, "root00", keys, null,      null,                                    null,       null);   // 0000
			createTestData(app, root, "root01", keys, oneToOne0, null,                                    null,       null);   // 1000
			createTestData(app, root, "root02", keys, null,      Arrays.asList(oneToMany00, oneToMany08), null,       null);   // 0100
			createTestData(app, root, "root03", keys, oneToOne1, Arrays.asList(oneToMany01, oneToMany09), null,       null);   // 1100
			createTestData(app, root, "root04", keys, null,      null,                                    manyToOne0, null);   // 0010
			createTestData(app, root, "root05", keys, null,      null,                                    manyToOne8, null);   // 0010
			createTestData(app, root, "root06", keys, oneToOne2, null,                                    manyToOne1, null);   // 1010
			createTestData(app, root, "root07", keys, null,      Arrays.asList(oneToMany02, oneToMany10), manyToOne2, null);   // 0110
			createTestData(app, root, "root08", keys, oneToOne3, Arrays.asList(oneToMany03, oneToMany11), manyToOne3, null);   // 1110

			createTestData(app, root, "root09", keys, null,      null,                                    null,       Arrays.asList( manyToMany0, manyToMany1 ));   // 0001
			createTestData(app, root, "root10", keys, oneToOne4, null,                                    null,       Arrays.asList( manyToMany1, manyToMany2 ));   // 1001
			createTestData(app, root, "root11", keys, null,      Arrays.asList(oneToMany04, oneToMany12), null,       Arrays.asList( manyToMany2, manyToMany3 ));   // 0101
			createTestData(app, root, "root12", keys, oneToOne5, Arrays.asList(oneToMany05, oneToMany13), null,       Arrays.asList( manyToMany3, manyToMany4 ));   // 1101
			createTestData(app, root, "root13", keys, null,      null,                                    manyToOne4, Arrays.asList( manyToMany4, manyToMany5 ));   // 0011
			createTestData(app, root, "root14", keys, null,      null,                                    manyToOne9, Arrays.asList( manyToMany4, manyToMany5 ));   // 0011
			createTestData(app, root, "root15", keys, oneToOne6, null,                                    manyToOne5, Arrays.asList( manyToMany5, manyToMany6 ));   // 1011
			createTestData(app, root, "root16", keys, null,      Arrays.asList(oneToMany06, oneToMany14), manyToOne6, Arrays.asList( manyToMany6, manyToMany7 ));   // 0111
			createTestData(app, root, "root17", keys, oneToOne7, Arrays.asList(oneToMany07, oneToMany15), manyToOne7, Arrays.asList( manyToMany7, manyToMany8 ));   // 1111

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		final String body = "{ name, oneToOne { name }, oneToMany { name }, manyToOne { name }, manyToMany { name } }";

		// test results for _equals
		assertCount("{ Root"                                                                                                                                                      + body + "}", "Root.#", 18);
		assertCount("{ Root(oneToOne:   " + eq("error")                                                                                                                    + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("error")                                                                                 + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany08")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne1") + ",oneToMany: " + eq("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + eq("error")                                            + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne2")                                      + ",manyToOne: " + eq("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany02") + ",manyToOne: " + eq("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne3") + ",oneToMany: " + eq("oneToMany03") + ",manyToOne: " + eq("manyToOne3")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("error")       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne4")                                                                          + ",manyToMany: " + eq("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany04")                                     + ",manyToMany: " + eq("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne5") + ",oneToMany: " + eq("oneToMany05")                                     + ",manyToMany: " + eq("manyToMany3") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne4") + ",manyToMany: " + eq("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne6")                                      + ",manyToOne: " + eq("manyToOne5") + ",manyToMany: " + eq("manyToMany5") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany06") + ",manyToOne: " + eq("manyToOne6") + ",manyToMany: " + eq("manyToMany6") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne7") + ",oneToMany: " + eq("oneToMany07") + ",manyToOne: " + eq("manyToOne7") + ",manyToMany: " + eq("manyToMany7") + ") " + body + "}", "Root.#", 1);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + eq("oneToOne2")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne3") + ",oneToMany: " + eq("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne4")                                      + ",manyToOne: " + eq("manyToOne1")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany02") + ",manyToOne: " + eq("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne5") + ",oneToMany: " + eq("oneToMany03") + ",manyToOne: " + eq("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne6")                                                                          + ",manyToMany: " + eq("manyToMany1") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany04")                                     + ",manyToMany: " + eq("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne7") + ",oneToMany: " + eq("oneToMany05")                                     + ",manyToMany: " + eq("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne4") + ",manyToMany: " + eq("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne0")                                      + ",manyToOne: " + eq("manyToOne5") + ",manyToMany: " + eq("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany06") + ",manyToOne: " + eq("manyToOne6") + ",manyToMany: " + eq("manyToMany6") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne1") + ",oneToMany: " + eq("oneToMany07") + ",manyToOne: " + eq("manyToOne7") + ",manyToMany: " + eq("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + eq("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany02")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne1") + ",oneToMany: " + eq("oneToMany03")                                                                           + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne2")                                      + ",manyToOne: " + eq("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany04") + ",manyToOne: " + eq("manyToOne2")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne3") + ",oneToMany: " + eq("oneToMany05") + ",manyToOne: " + eq("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne4")                                                                          + ",manyToMany: " + eq("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany06")                                     + ",manyToMany: " + eq("manyToMany2") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne5") + ",oneToMany: " + eq("oneToMany07")                                     + ",manyToMany: " + eq("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne4") + ",manyToMany: " + eq("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne6")                                      + ",manyToOne: " + eq("manyToOne5") + ",manyToMany: " + eq("manyToMany5") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany00") + ",manyToOne: " + eq("manyToOne6") + ",manyToMany: " + eq("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne7") + ",oneToMany: " + eq("oneToMany01") + ",manyToOne: " + eq("manyToOne7") + ",manyToMany: " + eq("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + eq("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne1") + ",oneToMany: " + eq("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne2")                                      + ",manyToOne: " + eq("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany02") + ",manyToOne: " + eq("manyToOne4")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne3") + ",oneToMany: " + eq("oneToMany03") + ",manyToOne: " + eq("manyToOne5")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne4")                                                                          + ",manyToMany: " + eq("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany04")                                     + ",manyToMany: " + eq("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne5") + ",oneToMany: " + eq("oneToMany05")                                     + ",manyToMany: " + eq("manyToMany3") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne6") + ",manyToMany: " + eq("manyToMany4") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne6")                                      + ",manyToOne: " + eq("manyToOne7") + ",manyToMany: " + eq("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany06") + ",manyToOne: " + eq("manyToOne0") + ",manyToMany: " + eq("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne7") + ",oneToMany: " + eq("oneToMany07") + ",manyToOne: " + eq("manyToOne1") + ",manyToMany: " + eq("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + eq("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne1") + ",oneToMany: " + eq("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne2")                                      + ",manyToOne: " + eq("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany02") + ",manyToOne: " + eq("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne3") + ",oneToMany: " + eq("oneToMany03") + ",manyToOne: " + eq("manyToOne3")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                                                             manyToMany: " + eq("manyToMany2") + ") " + body + "}", "Root.#", 2);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne4")                                                                          + ",manyToMany: " + eq("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany04")                                     + ",manyToMany: " + eq("manyToMany4") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne5") + ",oneToMany: " + eq("oneToMany05")                                     + ",manyToMany: " + eq("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + eq("manyToOne4") + ",manyToMany: " + eq("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne6")                                      + ",manyToOne: " + eq("manyToOne5") + ",manyToMany: " + eq("manyToMany7") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + eq("oneToMany06") + ",manyToOne: " + eq("manyToOne6") + ",manyToMany: " + eq("manyToMany0") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + eq("oneToOne7") + ",oneToMany: " + eq("oneToMany07") + ",manyToOne: " + eq("manyToOne7") + ",manyToMany: " + eq("manyToMany1") + ") " + body + "}", "Root.#", 0);



		// test results for _contains
		assertCount("{ Root"                                                                                                                                                      + body + "}", "Root.#", 18);
		assertCount("{ Root(oneToOne:   " + ct("error")                                                                                                                    + ") " + body + "}", "Root.#", 0);


		assertCount("{ Root(oneToOne:   " + ct("o")                                                                                                                        + ") " + body + "}", "Root.#", 8);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne1") + ",oneToMany: " + ct("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne2")                                      + ",manyToOne: " + ct("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany02") + ",manyToOne: " + ct("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne3") + ",oneToMany: " + ct("oneToMany03") + ",manyToOne: " + ct("manyToOne3")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                                                             manyToMany: " + ct("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne4")                                                                          + ",manyToMany: " + ct("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany04")                                     + ",manyToMany: " + ct("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne5") + ",oneToMany: " + ct("oneToMany05")                                     + ",manyToMany: " + ct("manyToMany3") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne4") + ",manyToMany: " + ct("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne6")                                      + ",manyToOne: " + ct("manyToOne5") + ",manyToMany: " + ct("manyToMany5") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany06") + ",manyToOne: " + ct("manyToOne6") + ",manyToMany: " + ct("manyToMany6") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne7") + ",oneToMany: " + ct("oneToMany07") + ",manyToOne: " + ct("manyToOne7") + ",manyToMany: " + ct("manyToMany7") + ") " + body + "}", "Root.#", 1);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + ct("oneToOne2")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne3") + ",oneToMany: " + ct("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne4")                                      + ",manyToOne: " + ct("manyToOne1")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany02") + ",manyToOne: " + ct("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne5") + ",oneToMany: " + ct("oneToMany03") + ",manyToOne: " + ct("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + ct("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne6")                                                                          + ",manyToMany: " + ct("manyToMany1") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany04")                                     + ",manyToMany: " + ct("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne7") + ",oneToMany: " + ct("oneToMany05")                                     + ",manyToMany: " + ct("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne4") + ",manyToMany: " + ct("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne0")                                      + ",manyToOne: " + ct("manyToOne5") + ",manyToMany: " + ct("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany06") + ",manyToOne: " + ct("manyToOne6") + ",manyToMany: " + ct("manyToMany6") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne1") + ",oneToMany: " + ct("oneToMany07") + ",manyToOne: " + ct("manyToOne7") + ",manyToMany: " + ct("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + ct("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany02")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne1") + ",oneToMany: " + ct("oneToMany03")                                                                           + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne2")                                      + ",manyToOne: " + ct("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany04") + ",manyToOne: " + ct("manyToOne2")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne3") + ",oneToMany: " + ct("oneToMany05") + ",manyToOne: " + ct("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + ct("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne4")                                                                          + ",manyToMany: " + ct("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany06")                                     + ",manyToMany: " + ct("manyToMany2") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne5") + ",oneToMany: " + ct("oneToMany07")                                     + ",manyToMany: " + ct("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne4") + ",manyToMany: " + ct("manyToMany4") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne6")                                      + ",manyToOne: " + ct("manyToOne5") + ",manyToMany: " + ct("manyToMany5") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany00") + ",manyToOne: " + ct("manyToOne6") + ",manyToMany: " + ct("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne7") + ",oneToMany: " + ct("oneToMany01") + ",manyToOne: " + ct("manyToOne7") + ",manyToMany: " + ct("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + ct("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne1") + ",oneToMany: " + ct("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne2")                                      + ",manyToOne: " + ct("manyToOne3")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany02") + ",manyToOne: " + ct("manyToOne4")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne3") + ",oneToMany: " + ct("oneToMany03") + ",manyToOne: " + ct("manyToOne5")                                       + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                                                             manyToMany: " + ct("manyToMany0") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne4")                                                                          + ",manyToMany: " + ct("manyToMany1") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany04")                                     + ",manyToMany: " + ct("manyToMany2") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne5") + ",oneToMany: " + ct("oneToMany05")                                     + ",manyToMany: " + ct("manyToMany3") + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne6") + ",manyToMany: " + ct("manyToMany4") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne6")                                      + ",manyToOne: " + ct("manyToOne7") + ",manyToMany: " + ct("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany06") + ",manyToOne: " + ct("manyToOne0") + ",manyToMany: " + ct("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne7") + ",oneToMany: " + ct("oneToMany07") + ",manyToOne: " + ct("manyToOne1") + ",manyToMany: " + ct("manyToMany7") + ") " + body + "}", "Root.#", 0);

		// test wrong results
		assertCount("{ Root(oneToOne:   " + ct("oneToOne0")                                                                                                                + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany00")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne1") + ",oneToMany: " + ct("oneToMany01")                                                                           + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne0")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne2")                                      + ",manyToOne: " + ct("manyToOne1")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany02") + ",manyToOne: " + ct("manyToOne2")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne3") + ",oneToMany: " + ct("oneToMany03") + ",manyToOne: " + ct("manyToOne3")                                       + ") " + body + "}", "Root.#", 1);
		assertCount("{ Root(                                                                                                             manyToMany: " + ct("manyToMany2") + ") " + body + "}", "Root.#", 2);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne4")                                                                          + ",manyToMany: " + ct("manyToMany3") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany04")                                     + ",manyToMany: " + ct("manyToMany4") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne5") + ",oneToMany: " + ct("oneToMany05")                                     + ",manyToMany: " + ct("manyToMany5") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                                                         manyToOne: " + ct("manyToOne4") + ",manyToMany: " + ct("manyToMany6") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne6")                                      + ",manyToOne: " + ct("manyToOne5") + ",manyToMany: " + ct("manyToMany7") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(                                    oneToMany: " + ct("oneToMany06") + ",manyToOne: " + ct("manyToOne6") + ",manyToMany: " + ct("manyToMany0") + ") " + body + "}", "Root.#", 0);
		assertCount("{ Root(oneToOne:   " + ct("oneToOne7") + ",oneToMany: " + ct("oneToMany07") + ",manyToOne: " + ct("manyToOne7") + ",manyToMany: " + ct("manyToMany1") + ") " + body + "}", "Root.#", 0);

	}

	@Test
	public void testFunctionPropertyIndexing() {

		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "TASK", Cardinality.OneToMany, "project", "tasks");

			// add function property that extracts the project ID
			final JsonFunctionProperty projectId = task.addFunctionProperty("projectId");

			projectId.setIndexed(true);
			projectId.setReadFunction("this.project.id");
			projectId.setTypeHint("String");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String project          = "Project";
		final String task             = "Task";
		final PropertyKey projectKey = Traits.of(task).key("project");
		final PropertyKey tasksKey   = Traits.of(project).key("tasks");

		String project1Id            = null;
		String project2Id            = null;

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> tasks1 = new LinkedList<>();
			final List<NodeInterface> tasks2 = new LinkedList<>();

			final NodeInterface project1     = app.create(project, "Project1");
			final NodeInterface project2     = app.create(project, "Project2");

			project1Id = project1.getUuid();
			project2Id = project2.getUuid();

			final NodeInterface task4 = app.create(task, "Task1.4");
			final NodeInterface task6 = app.create(task, "Task1.6");
			final NodeInterface task7 = app.create(task, "Task1.7");
			final NodeInterface task2 = app.create(task, "Task1.2");
			final NodeInterface task1 = app.create(task, "Task1.1");
			final NodeInterface task3 = app.create(task, "Task1.3");
			final NodeInterface task5 = app.create(task, "Task1.5");

			tasks1.add(task1);
			tasks1.add(task2);
			tasks1.add(task3);
			tasks1.add(task4);
			tasks1.add(task5);
			tasks1.add(task6);
			tasks1.add(task7);

			final NodeInterface task09 = app.create(task, "Task2.2");
			final NodeInterface task13 = app.create(task, "Task2.6");
			final NodeInterface task11 = app.create(task, "Task2.4");
			final NodeInterface task12 = app.create(task, "Task2.5");
			final NodeInterface task10 = app.create(task, "Task2.3");
			final NodeInterface task08 = app.create(task, "Task2.1");
			final NodeInterface task14 = app.create(task, "Task2.7");

			tasks2.add(task08);
			tasks2.add(task09);
			tasks2.add(task10);
			tasks2.add(task11);
			tasks2.add(task12);
			tasks2.add(task13);
			tasks2.add(task14);

			project1.setProperty(tasksKey, tasks1);
			project2.setProperty(tasksKey, tasks2);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		// verify that all tasks are found
		assertCount("{ Task { id }}", "Task.#", 14);

		// verify that all tasks with a given project ID are found
		{
			final Map<String, Object> result = fetchGraphQL("{ Task( _sort: \"name\", projectId: { _equals: \"" + project1Id + "\"}) { name }}");
			assertMapPathValueIs(result, "Task.#",                   7);
			assertMapPathValueIs(result, "Task.0.name",             "Task1.1");
			assertMapPathValueIs(result, "Task.1.name",             "Task1.2");
			assertMapPathValueIs(result, "Task.2.name",             "Task1.3");
			assertMapPathValueIs(result, "Task.3.name",             "Task1.4");
			assertMapPathValueIs(result, "Task.4.name",             "Task1.5");
			assertMapPathValueIs(result, "Task.5.name",             "Task1.6");
			assertMapPathValueIs(result, "Task.6.name",             "Task1.7");
		}

		// verify that all tasks with a given project ID are found
		{
			final Map<String, Object> result = fetchGraphQL("{ Task( _sort: \"name\", projectId: { _equals: \"" + project2Id + "\"}) { name }}");
			assertMapPathValueIs(result, "Task.#",                  7);
			assertMapPathValueIs(result, "Task.0.name",             "Task2.1");
			assertMapPathValueIs(result, "Task.1.name",             "Task2.2");
			assertMapPathValueIs(result, "Task.2.name",             "Task2.3");
			assertMapPathValueIs(result, "Task.3.name",             "Task2.4");
			assertMapPathValueIs(result, "Task.4.name",             "Task2.5");
			assertMapPathValueIs(result, "Task.5.name",             "Task2.6");
			assertMapPathValueIs(result, "Task.6.name",             "Task2.7");
		}

		// modify data, remove some tasks from projects
		try (final Tx tx = app.tx()) {

			app.nodeQuery(task).name("Task1.3").getFirst().setProperty(projectKey, null);
			app.nodeQuery(task).name("Task1.4").getFirst().setProperty(projectKey, null);
			app.nodeQuery(task).name("Task1.5").getFirst().setProperty(projectKey, null);

			app.nodeQuery(task).name("Task2.1").getFirst().setProperty(projectKey, null);
			app.nodeQuery(task).name("Task2.2").getFirst().setProperty(projectKey, null);
			app.nodeQuery(task).name("Task2.3").getFirst().setProperty(projectKey, null);
			app.nodeQuery(task).name("Task2.7").getFirst().setProperty(projectKey, null);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// verify that all tasks are found
		assertCount("{ Task { id }}", "Task.#", 14);

		// verify that only tasks with the given project ID are found
		{
			final Map<String, Object> result = fetchGraphQL("{ Task( _sort: \"name\", projectId: { _equals: \"" + project1Id + "\"}) { name }}");
			assertMapPathValueIs(result, "Task.#",                   4);
			assertMapPathValueIs(result, "Task.0.name",             "Task1.1");
			assertMapPathValueIs(result, "Task.1.name",             "Task1.2");
			assertMapPathValueIs(result, "Task.2.name",             "Task1.6");
			assertMapPathValueIs(result, "Task.3.name",             "Task1.7");
		}

		// verify that only tasks with the given project ID are found
		{
			final Map<String, Object> result = fetchGraphQL("{ Task( _sort: \"name\", projectId: { _equals: \"" + project2Id + "\"}) { name }}");
			assertMapPathValueIs(result, "Task.#",                  3);
			assertMapPathValueIs(result, "Task.0.name",             "Task2.4");
			assertMapPathValueIs(result, "Task.1.name",             "Task2.5");
			assertMapPathValueIs(result, "Task.2.name",             "Task2.6");
		}
	}

	@Test
	public void testOrConjuctionOnIds() {

		// setup
		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType taskGroup = schema.addType("TaskGroup");
			final JsonObjectType project   = schema.addType("Project");
			final JsonObjectType task      = schema.addType("Task");

			project.relate(task,   "TASK",    Cardinality.OneToMany, "project",   "tasks");
			taskGroup.relate(task, "PART_OF", Cardinality.OneToMany, "taskGroup", "tasks");

			// add function property that extracts the project ID
			final JsonFunctionProperty projectId = task.addFunctionProperty("projectId");

			projectId.setIndexed(true);
			projectId.setReadFunction("this.project.id");
			projectId.setTypeHint("String");

			// add function property that extracts the project ID
			final JsonFunctionProperty taskGroupId = task.addFunctionProperty("taskGroupId");

			taskGroupId.setIndexed(true);
			taskGroupId.setReadFunction("this.taskGroup.id");
			taskGroupId.setTypeHint("String");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String taskGroup             = "TaskGroup";
		final String project               = "Project";
		final String task                  = "Task";
		final PropertyKey projectKey      = Traits.of(task).key("project");
		final PropertyKey projectTasksKey = Traits.of(project).key("tasks");
		final PropertyKey groupTasksKey   = Traits.of(taskGroup).key("tasks");

		String group1Id            = null;
		String group2Id            = null;
		String group3Id            = null;
		String group4Id            = null;
		String project1Id          = null;
		String project2Id          = null;

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> tasks1 = new LinkedList<>();
			final List<NodeInterface> tasks2 = new LinkedList<>();

			final NodeInterface project1     = app.create(project, "Project1");
			final NodeInterface project2     = app.create(project, "Project2");

			project1Id = project1.getUuid();
			project2Id = project2.getUuid();

			final NodeInterface group1 = app.create(taskGroup, "Group1");
			final NodeInterface group2 = app.create(taskGroup, "Group2");
			final NodeInterface group3 = app.create(taskGroup, "Group3");
			final NodeInterface group4 = app.create(taskGroup, "Group4");

			group1Id = group1.getUuid();
			group2Id = group2.getUuid();
			group3Id = group3.getUuid();
			group4Id = group4.getUuid();

			tasks1.add(app.create(task, "Task1.1"));
			tasks1.add(app.create(task, "Task1.2"));
			tasks1.add(app.create(task, "Task1.3"));
			tasks1.add(app.create(task, "Task1.4"));
			tasks1.add(app.create(task, "Task1.5"));
			tasks1.add(app.create(task, "Task1.6"));
			tasks1.add(app.create(task, "Task1.7"));

			tasks2.add(app.create(task, "Task2.1"));
			tasks2.add(app.create(task, "Task2.2"));
			tasks2.add(app.create(task, "Task2.3"));
			tasks2.add(app.create(task, "Task2.4"));
			tasks2.add(app.create(task, "Task2.5"));
			tasks2.add(app.create(task, "Task2.6"));
			tasks2.add(app.create(task, "Task2.7"));

			project1.setProperty(projectTasksKey, tasks1);
			project2.setProperty(projectTasksKey, tasks2);

			group1.setProperty(groupTasksKey, Arrays.asList( tasks1.get(0), tasks1.get(2), tasks1.get(3)));
			group2.setProperty(groupTasksKey, Arrays.asList( tasks2.get(2), tasks2.get(4), tasks2.get(5)));
			group3.setProperty(groupTasksKey, Arrays.asList( tasks1.get(1), tasks1.get(4), tasks1.get(5)));
			group4.setProperty(groupTasksKey, Arrays.asList( tasks2.get(0), tasks2.get(1), tasks2.get(3)));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Task { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  14);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(taskGroupId: {_equals: \"" + group1Id + "\"}) { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  3);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(taskGroupId: {_contains: \"" + group1Id + "\", _contains: \"" + group2Id + "\", _conj: \"OR\"}) { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  6);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(taskGroupId: {_contains: \"" + group2Id + "\", _contains: \"" + group3Id + "\", _conj: \"OR\"}) { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  6);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projectId: {_contains: \"" + project1Id + "\", _contains: \"" + project2Id + "\", _conj: \"OR\"}) { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  14);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(projectId: {_contains: \"" + project1Id + "\"}, taskGroupId: {_contains: \"" + group2Id + "\", _contains: \"" + group3Id + "\", _conj: \"OR\"}) { id, type, name, projectId, taskGroupId, project { id, name }, taskGroup { id, name } }}");
			assertMapPathValueIs(result, "Task.#",                  3);
		}
	}

	@Test
	public void testBooleanFunctionPropertyFiltering() {

		// setup
		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType testType     = schema.addType("Test");
			final JsonFunctionProperty filter = testType.addFunctionProperty("doFilter");
			final JsonBooleanProperty test    = testType.addBooleanProperty("test");

			filter.setIndexed(true);
			filter.setReadFunction("this.test");
			filter.setTypeHint("Boolean");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String type     = "Test";
		final PropertyKey key = Traits.of(type).key("test");

		try (final Tx tx = app.tx()) {

			app.create(type, "test1").setProperty(key, true);
			app.create(type, "test2").setProperty(key, false);
			app.create(type, "test3").setProperty(key, true);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}


		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Test(doFilter: {_equals: true}) { id, type, name, test, doFilter }}");
			assertMapPathValueIs(result, "Test.#",  2);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Test(doFilter: {_equals: false}) { id, type, name, test, doFilter }}");
			assertMapPathValueIs(result, "Test.#",  1);
		}
	}

	@Test
	public void testFunctionPropertyIndexUpdate() {

		// setup
		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project   = schema.addType("Project");
			final JsonObjectType task      = schema.addType("Task");

			project.relate(task,   "TASK",    Cardinality.OneToMany, "project",   "tasks");

			// add function property that extracts the project ID
			final JsonFunctionProperty hasProject = task.addFunctionProperty("hasProject");

			hasProject.setIndexed(true);
			hasProject.setReadFunction("not(empty(this.project))");
			hasProject.setTypeHint("Boolean");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String projectType = "Project";
		final String taskType    = "Task";
		final PropertyKey projectTasksKey      = Traits.of(projectType).key("tasks");

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> tasks = new LinkedList<>();
			final NodeInterface project     = app.create(projectType, "Project1");

			tasks.add(app.create(taskType, "Task1.1"));
			tasks.add(app.create(taskType, "Task1.3"));
			tasks.add(app.create(taskType, "Task1.5"));
			tasks.add(app.create(taskType, "Task1.6"));

			app.create(taskType, "Task1.2");
			app.create(taskType, "Task1.4");
			app.create(taskType, "Task1.7");

			project.setProperty(projectTasksKey, tasks);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", hasProject: {_equals: true}) { id, type, name }}");
			assertMapPathValueIs(result, "Task.#",                  4);
			assertMapPathValueIs(result, "Task.0.name",            "Task1.1");
			assertMapPathValueIs(result, "Task.1.name",            "Task1.3");
			assertMapPathValueIs(result, "Task.2.name",            "Task1.5");
			assertMapPathValueIs(result, "Task.3.name",            "Task1.6");
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", hasProject: {_equals: false}) { id, type, name }}");
			assertMapPathValueIs(result, "Task.#",                  3);
			assertMapPathValueIs(result, "Task.0.name",            "Task1.2");
			assertMapPathValueIs(result, "Task.1.name",            "Task1.4");
			assertMapPathValueIs(result, "Task.2.name",            "Task1.7");
		}

		// delete project via REST
		RestAssured.basePath = "/structr/rest";
		RestAssured
			.given().contentType("application/json; charset=UTF-8")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect().statusCode(200)
			.when().delete("/Project");

		// test index update
		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", hasProject: {_equals: true}) { id, type, name }}");
			assertMapPathValueIs(result, "Task.#",                  0);
		}

		{
			final Map<String, Object> result = fetchGraphQL("{ Task(_sort: \"name\", hasProject: {_equals: false}) { id, type, name }}");
			assertMapPathValueIs(result, "Task.#",                  7);
			assertMapPathValueIs(result, "Task.0.name",            "Task1.1");
			assertMapPathValueIs(result, "Task.1.name",            "Task1.2");
			assertMapPathValueIs(result, "Task.2.name",            "Task1.3");
			assertMapPathValueIs(result, "Task.3.name",            "Task1.4");
			assertMapPathValueIs(result, "Task.4.name",            "Task1.5");
			assertMapPathValueIs(result, "Task.5.name",            "Task1.6");
			assertMapPathValueIs(result, "Task.6.name",            "Task1.7");
		}
	}

	@Test
	public void testInheritance() {

		// setup
		try (final Tx tx = app.tx()) {

			JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType baseType = schema.addType("BaseType");
			final JsonObjectType project  = schema.addType("Project");
			final JsonObjectType task     = schema.addType("Task");

			project.addTrait("BaseType");
			task.addTrait("BaseType");

			baseType.addBooleanProperty("isChecked").setIndexed(true);

			project.relate(task, "TASK",  Cardinality.OneToMany, "project",   "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String baseType             = "BaseType";
		final String projectType          = "Project";
		final String taskType             = "Task";
		final PropertyKey checkedKey      = Traits.of(baseType).key("isChecked");
		final PropertyKey projectTasksKey = Traits.of(projectType).key("tasks");
		final PropertyKey<String> nameKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> tasks1 = new LinkedList<>();
			final List<NodeInterface> tasks2 = new LinkedList<>();
			final List<NodeInterface> tasks3 = new LinkedList<>();
			final List<NodeInterface> tasks4 = new LinkedList<>();
			final List<NodeInterface> tasks5 = new LinkedList<>();

			final NodeInterface project1     = app.create(projectType, new NodeAttribute<>(nameKey, "Project1"), new NodeAttribute<>(checkedKey, true));
			final NodeInterface project2     = app.create(projectType, new NodeAttribute<>(nameKey, "Project2"), new NodeAttribute<>(checkedKey, false));
			final NodeInterface project3     = app.create(projectType, new NodeAttribute<>(nameKey, "Project3"), new NodeAttribute<>(checkedKey, true));
			final NodeInterface project4     = app.create(projectType, new NodeAttribute<>(nameKey, "Project4"), new NodeAttribute<>(checkedKey, false));
			final NodeInterface project5     = app.create(projectType, new NodeAttribute<>(nameKey, "Project5"), new NodeAttribute<>(checkedKey, true));

			tasks1.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task1.1"), new NodeAttribute<>(checkedKey, false)));
			tasks1.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task1.3"), new NodeAttribute<>(checkedKey, true)));
			tasks1.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task1.5"), new NodeAttribute<>(checkedKey, false)));
			tasks1.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task1.6"), new NodeAttribute<>(checkedKey, true)));

			tasks2.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task2.1"), new NodeAttribute<>(checkedKey, false)));
			tasks2.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task2.2"), new NodeAttribute<>(checkedKey, true)));
			tasks2.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task2.3"), new NodeAttribute<>(checkedKey, false)));
			tasks2.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task2.4"), new NodeAttribute<>(checkedKey, true)));

			tasks3.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task3.1"), new NodeAttribute<>(checkedKey, false)));
			tasks3.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task3.2"), new NodeAttribute<>(checkedKey, true)));
			tasks3.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task3.3"), new NodeAttribute<>(checkedKey, false)));
			tasks3.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task3.4"), new NodeAttribute<>(checkedKey, true)));

			tasks4.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task4.1"), new NodeAttribute<>(checkedKey, false)));
			tasks4.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task4.2"), new NodeAttribute<>(checkedKey, true)));
			tasks4.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task4.3"), new NodeAttribute<>(checkedKey, false)));
			tasks4.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task4.4"), new NodeAttribute<>(checkedKey, true)));

			tasks5.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task5.1"), new NodeAttribute<>(checkedKey, false)));
			tasks5.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task5.2"), new NodeAttribute<>(checkedKey, false)));
			tasks5.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task5.3"), new NodeAttribute<>(checkedKey, false)));
			tasks5.add(app.create(taskType, new NodeAttribute<>(nameKey, "Task5.4"), new NodeAttribute<>(checkedKey, false)));

			project1.setProperty(projectTasksKey, tasks1);
			project2.setProperty(projectTasksKey, tasks2);
			project3.setProperty(projectTasksKey, tasks3);
			project4.setProperty(projectTasksKey, tasks4);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		{
			final Map<String, Object> result = fetchGraphQL("{ Project(_sort: \"name\", isChecked: { _equals: true}, tasks: { isChecked: { _contains: true }}) { id, type, name, isChecked, tasks { id, type, name, isChecked }}}");

			assertMapPathValueIs(result, "Project.#",                  2);
			assertMapPathValueIs(result, "Project.0.name",            "Project1");
			assertMapPathValueIs(result, "Project.0.isChecked",       true);
			assertMapPathValueIs(result, "Project.0.tasks.0.name",    "Task1.1");
			assertMapPathValueIs(result, "Project.0.tasks.1.name",    "Task1.3");
			assertMapPathValueIs(result, "Project.0.tasks.2.name",    "Task1.5");
			assertMapPathValueIs(result, "Project.0.tasks.3.name",    "Task1.6");
			assertMapPathValueIs(result, "Project.1.name",            "Project3");
			assertMapPathValueIs(result, "Project.1.isChecked",       true);
			assertMapPathValueIs(result, "Project.1.tasks.0.name",    "Task3.1");
			assertMapPathValueIs(result, "Project.1.tasks.1.name",    "Task3.2");
			assertMapPathValueIs(result, "Project.1.tasks.2.name",    "Task3.3");
			assertMapPathValueIs(result, "Project.1.tasks.3.name",    "Task3.4");
		}

		/* failing test, to be implemented...
		{
			//final Map<String, Object> result = fetchGraphQL("{ Project(_sort: \"name\", isChecked: { _equals: true}) { id, type, name, isChecked, tasks( isChecked: { _equals: true }, _sort: \"name\", _desc: true ) { id, type, name, isChecked }}}");
			final Map<String, Object> result = fetchGraphQL("{ Project(_sort: \"name\", isChecked: { _equals: true}) { id, type, name, isChecked, tasks(_sort: \"name\", _desc: true ) { id, type, name, isChecked( _equals: true) }}}");

			assertMapPathValueIs(result, "Project.#",                  2);
			assertMapPathValueIs(result, "Project.0.name",            "Project1");
			assertMapPathValueIs(result, "Project.0.isChecked",       true);
			assertMapPathValueIs(result, "Project.0.tasks.0.name",    "Task1.6");
			assertMapPathValueIs(result, "Project.0.tasks.1.name",    "Task1.3");
			assertMapPathValueIs(result, "Project.1.name",            "Project3");
			assertMapPathValueIs(result, "Project.1.isChecked",       true);
			assertMapPathValueIs(result, "Project.1.tasks.0.name",    "Task3.4");
			assertMapPathValueIs(result, "Project.1.tasks.1.name",    "Task3.2");
		}
		*/

	}


	// ----- private methods -----
	private String eq(final String value) {
		return "{ name: { _contains: \"" + value + "\" }}";
	}

	private String ct(final String value) {
		return "{ name: { _contains: \"" + value + "\" }}";
	}

	private void createTestData(final App app, final String type, final String name, final KeyData keys, final Object ... data) throws FrameworkException {

		final PropertyMap map = new PropertyMap();

		map.put(keys.name, name);

		switch (data.length) {

			case 4: if (data[3] != null) { map.put(keys.t3, data[3]); }
			case 3: if (data[2] != null) { map.put(keys.t2, data[2]); }
			case 2: if (data[1] != null) { map.put(keys.t1, data[1]); }
			case 1: if (data[0] != null) { map.put(keys.t0, data[0]); }
		}

		app.create(type, map);
	}

	private void assertCount(final String query, final String path, final int count) {
		assertMapPathValueIs(fetchGraphQL(query), path, count);
	}

	private Map<String, Object> fetchGraphQL(final String query) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.body(query)

			.expect()
				.statusCode(200)

			.when()
				.post("/")

			.andReturn()
			.as(Map.class);
	}

	public static Object getMapPathValue(final Map<String, Object> map, final String mapPath) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					final List list = (List)current;
					if (index >= list.size()) {

						return null;

					} else {

						current = list.get(index);
					}
				}

			} else if ("#".equals(part)) {

				if (current instanceof List) {

					return ((List)current).size();
				}

				if (current instanceof Map) {

					return ((Map)current).size();
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		return current;
	}

	// ----- nested classes -----
	private static class KeyData {

		public PropertyKey name = null;
		public PropertyKey t0   = null;
		public PropertyKey t1   = null;
		public PropertyKey t2   = null;
		public PropertyKey t3   = null;

		public KeyData(final PropertyKey name, final PropertyKey t0, final PropertyKey t1, final PropertyKey t2, final PropertyKey t3) {

			this.name = name;
			this.t0 = t0;
			this.t1 = t1;
			this.t2 = t2;
			this.t3 = t3;
		}
	}
}
