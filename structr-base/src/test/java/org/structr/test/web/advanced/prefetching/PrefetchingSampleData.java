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
package org.structr.test.web.advanced.prefetching;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The sample application for the prefetching measurements: the data of a project management application, an HTML
 * page that shows it with nested repeaters, schema methods that work through it, and a non-admin user who reads it
 * through group permissions.
 *
 * <p>The shape of the data is what decides whether a learned prefetch pays off, so it is chosen to resemble a real
 * application rather than to suit prefetching:
 * <ul>
 * <li>Project sizes are skewed: every tenth project has many times the tasks of the others. A request for one
 * project then uses a small or a large share of all tasks, depending on which project it asks for.</li>
 * <li>The relationship type name HAS is used three times (Project to Task, Project to Milestone, Milestone to Task),
 * like applications do, so a learned pattern for one of them sees relationships of the others.</li>
 * <li>There are to-one, to-many and many-to-many relationships, and chains of them (task, assignee, team).</li>
 * </ul>
 *
 * <p>Everything is generated deterministically, so two runs at the same scale have the same data.
 */
final class PrefetchingSampleData {

	static final String MEMBER_NAME     = "member";
	static final String MEMBER_PASSWORD = "member-password";
	static final String PAGE_NAME       = "portfolio";

	static final List<String> TYPES = List.of("Customer", "Project", "Milestone", "Task", "Comment", "Person", "Team", "Tag");

	/**
	 * The size of the sample data. Every tenth project is a large one with largeProjectFactor times the tasks.
	 */
	record Scale(String name, int customers, int projectsPerCustomer, int tasksPerProject, int largeProjectFactor, int milestonesPerProject, int commentsPerTask, int tagsPerTask, int persons, int teams, int tags) {

		int projects() {

			return customers * projectsPerCustomer;
		}

		int tasksOf(final int projectIndex) {

			return projectIndex % 10 == 0 ? tasksPerProject * largeProjectFactor : tasksPerProject;
		}

		int tasks() {

			int sum = 0;

			for (int p = 0; p < projects(); p++) {

				sum += tasksOf(p);
			}

			return sum;
		}

		@Override
		public String toString() {

			return name + " (" + customers + " customers, " + projects() + " projects, " + tasks() + " tasks, " + (tasks() * commentsPerTask) + " comments, " + persons + " persons)";
		}
	}

	static final Scale SMALL  = new Scale("small",   3,  4,  6,  4, 2, 2, 2,  12,  3, 10);
	static final Scale MEDIUM = new Scale("medium",  5, 10, 10, 10, 3, 2, 2,  40,  5, 20);
	static final Scale LARGE  = new Scale("large",  20, 20, 20, 10, 4, 3, 3, 200, 10, 50);

	/**
	 * The UUIDs the scenarios address, in creation order, and the number of tasks.
	 */
	record Data(List<String> customers, List<String> projects, List<String> smallProjects, List<String> largeProjects, int tasks) {
	}

	private PrefetchingSampleData() {
	}

	static Scale scale(final String name) {

		return switch (name) {

			case "small"  -> SMALL;
			case "large"  -> LARGE;
			default       -> MEDIUM;
		};
	}

	static void createSchema(final App app) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema        = StructrSchema.createFromDatabase(app);
			final JsonObjectType customer  = schema.addType("Customer");
			final JsonObjectType project   = schema.addType("Project");
			final JsonObjectType milestone = schema.addType("Milestone");
			final JsonObjectType task      = schema.addType("Task");
			final JsonObjectType comment   = schema.addType("Comment");
			final JsonObjectType person    = schema.addType("Person");
			final JsonObjectType team      = schema.addType("Team");
			final JsonObjectType tag       = schema.addType("Tag");

			customer.relate(project,  "HAS_PROJECT", Cardinality.OneToMany,  "customer",      "projects");
			project.relate(task,      "HAS",         Cardinality.OneToMany,  "project",       "tasks");
			project.relate(milestone, "HAS",         Cardinality.OneToMany,  "project",       "milestones");
			milestone.relate(task,    "HAS",         Cardinality.OneToMany,  "milestone",     "milestoneTasks");
			task.relate(person,       "ASSIGNED_TO", Cardinality.ManyToOne,  "assignedTasks", "assignee");
			task.relate(comment,      "HAS_COMMENT", Cardinality.OneToMany,  "task",          "comments");
			comment.relate(person,    "WRITTEN_BY",  Cardinality.ManyToOne,  "comments",      "author");
			task.relate(tag,          "TAGGED",      Cardinality.ManyToMany, "tasks",         "tags");
			person.relate(team,       "MEMBER_OF",   Cardinality.ManyToOne,  "members",       "team");

			task.addIntegerProperty("revision");

			// the view the REST scenarios request, nested down to the team of the author of a comment
			addView(customer,  "name", "projects");
			addView(project,   "name", "customer", "milestones", "tasks");
			addView(milestone, "name");
			addView(task,      "name", "assignee", "milestone", "tags", "comments");
			addView(comment,   "name", "author");
			addView(person,    "name", "team");
			addView(team,      "name");
			addView(tag,       "name");

			// a report over the tasks of a project, like an export or a dashboard would compute it
			project.addMethod("taskSummary", """
				{
					const rows = [];

					for (const task of $.this.tasks) {

						const tags = [];
						let comments = 0;

						for (const tag of task.tags) {
							tags.push(tag.name);
						}

						for (const comment of task.comments) {
							comments++;
						}

						rows.push([task.name, task.assignee ? task.assignee.name : '', task.assignee && task.assignee.team ? task.assignee.team.name : '', task.milestone ? task.milestone.name : '', comments, tags.join(',')]);
					}

					return rows;
				}
				""");

			// the tasks of all projects of a customer, counted by the team of their assignee
			customer.addMethod("overview", """
				{
					const result = {};

					for (const project of $.this.projects) {

						for (const task of project.tasks) {

							const team = task.assignee && task.assignee.team ? task.assignee.team.name : 'none';

							result[team] = (result[team] || 0) + 1;
						}
					}

					return result;
				}
				""");

			// a request that writes: updates every task of a project and adds a comment to the first one
			project.addMethod("touch", """
				{
					let first = null;

					for (const task of $.this.tasks) {

						if (first === null) {
							first = task;
						}

						task.revision = (task.revision || 0) + 1;
					}

					if (first !== null) {
						$.create('Comment', { name: 'touched', task: first });
					}

					return null;
				}
				""");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();
		}
	}

	/**
	 * Creates the data, one transaction per project, so that a large scale does not end up in one huge transaction.
	 */
	static Data createData(final App app, final Scale scale) throws FrameworkException {

		final List<String> customerIds     = new ArrayList<>();
		final List<String> projectIds      = new ArrayList<>();
		final List<String> smallProjectIds = new ArrayList<>();
		final List<String> largeProjectIds = new ArrayList<>();
		final List<String> personIds       = new ArrayList<>();
		final List<String> tagIds          = new ArrayList<>();

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> teams = new ArrayList<>();
			final PropertyKey team          = Traits.of("Person").key("team");

			for (int i = 0; i < scale.teams(); i++) {

				teams.add(app.create("Team", String.format("team%02d", i)));
			}

			for (int i = 0; i < scale.persons(); i++) {

				final NodeInterface person = app.create("Person", String.format("person%03d", i));

				person.setProperty(team, teams.get(i % teams.size()));
				personIds.add(person.getUuid());
			}

			for (int i = 0; i < scale.tags(); i++) {

				tagIds.add(app.create("Tag", String.format("tag%02d", i)).getUuid());
			}

			for (int i = 0; i < scale.customers(); i++) {

				customerIds.add(app.create("Customer", String.format("customer%02d", i)).getUuid());
			}

			tx.success();
		}

		final PropertyKey projectCustomer  = Traits.of("Project").key("customer");
		final PropertyKey milestoneProject = Traits.of("Milestone").key("project");
		final PropertyKey taskProject      = Traits.of("Task").key("project");
		final PropertyKey taskMilestone    = Traits.of("Task").key("milestone");
		final PropertyKey taskAssignee     = Traits.of("Task").key("assignee");
		final PropertyKey taskTags         = Traits.of("Task").key("tags");
		final PropertyKey commentTask      = Traits.of("Comment").key("task");
		final PropertyKey commentAuthor    = Traits.of("Comment").key("author");
		int taskCounter                    = 0;

		for (int p = 0; p < scale.projects(); p++) {

			try (final Tx tx = app.tx()) {

				// nodes must not be used outside the transaction they were loaded in, so they are looked up again in each one
				final Map<String, NodeInterface> nodes = new HashMap<>();
				final String name                      = String.format("project%03d", p);
				final NodeInterface project            = app.create("Project", name);
				final List<NodeInterface> milestones   = new ArrayList<>();

				project.setProperty(projectCustomer, node(app, nodes, customerIds.get(p % customerIds.size())));

				for (int m = 0; m < scale.milestonesPerProject(); m++) {

					final NodeInterface milestone = app.create("Milestone", name + "-m" + m);

					milestone.setProperty(milestoneProject, project);
					milestones.add(milestone);
				}

				for (int t = 0; t < scale.tasksOf(p); t++) {

					final NodeInterface task      = app.create("Task", String.format("%s-t%03d", name, t));
					final List<NodeInterface> tags = new ArrayList<>();

					task.setProperty(taskProject,   project);
					task.setProperty(taskMilestone, milestones.get(t % milestones.size()));
					task.setProperty(taskAssignee,  node(app, nodes, personIds.get((taskCounter * 7) % personIds.size())));

					for (int g = 0; g < scale.tagsPerTask(); g++) {

						tags.add(node(app, nodes, tagIds.get((taskCounter + g * 3) % tagIds.size())));
					}

					task.setProperty(taskTags, tags);

					for (int c = 0; c < scale.commentsPerTask(); c++) {

						final NodeInterface comment = app.create("Comment", task.getName() + "-c" + c);

						comment.setProperty(commentTask,   task);
						comment.setProperty(commentAuthor, node(app, nodes, personIds.get((taskCounter + c) % personIds.size())));
					}

					taskCounter++;
				}

				projectIds.add(project.getUuid());

				if (scale.tasksOf(p) > scale.tasksPerProject()) {

					largeProjectIds.add(project.getUuid());

				} else {

					smallProjectIds.add(project.getUuid());
				}

				tx.success();
			}
		}

		return new Data(customerIds, projectIds, smallProjectIds, largeProjectIds, scale.tasks());
	}

	/**
	 * Creates the page /portfolio: all customers, their projects and the tasks of each project with assignee,
	 * milestone and number of comments, in three nested repeaters.
	 */
	static void createPage(final App app, final SecurityContext securityContext) throws FrameworkException {

		final PropertyKey functionQuery = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY);
		final PropertyKey dataKey       = Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY);

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, PAGE_NAME);
			final DOMNode div = page.getElementsByTagName("div").get(0);
			final DOMElement customers = repeater(page, div, functionQuery, dataKey, "find('Customer', sort('name'))", "customer");

			customers.appendChild(page.createTextNode("${customer.name}"));

			final DOMElement projects = repeater(page, customers, functionQuery, dataKey, "customer.projects", "project");
			projects.appendChild(page.createTextNode("${project.name}"));

			final DOMElement tasks = repeater(page, projects, functionQuery, dataKey, "project.tasks", "task");
			tasks.appendChild(page.createTextNode("${task.name} ${task.assignee.name} ${task.assignee.team.name} ${task.milestone.name} ${size(task.comments)}"));

			tx.success();
		}

		// the non-admin user must be able to read the page
		try (final Tx tx = app.tx()) {

			final PropertyKey visible = Traits.of(StructrTraits.DOM_NODE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);

			for (final NodeInterface node : app.nodeQuery(StructrTraits.DOM_NODE).getAsList()) {

				node.setProperty(visible, true);
			}

			tx.success();
		}
	}

	/**
	 * Creates a non-admin user who can read the sample data through a schema grant to a group two levels above
	 * them, so that every request of theirs resolves group memberships and permissions, like a real application does.
	 */
	static void createMember(final App app, final SecurityContext securityContext) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Group company    = app.create(StructrTraits.GROUP, "company").as(Group.class);
			final Group staff      = app.create(StructrTraits.GROUP, "staff").as(Group.class);
			final Principal member = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   MEMBER_NAME),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), MEMBER_PASSWORD)
			).as(Principal.class);

			company.addMember(securityContext, staff);
			staff.addMember(securityContext, member);

			final Traits grantTraits = Traits.of(StructrTraits.SCHEMA_GRANT);

			for (final String type : TYPES) {

				final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name(type).getFirst();

				app.create(StructrTraits.SCHEMA_GRANT,
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY),          schemaNode),
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY),           company),
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY),           true),
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_WRITE_PROPERTY),          false),
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_DELETE_PROPERTY),         false),
					new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_ACCESS_CONTROL_PROPERTY),  false)
				);
			}

			tx.success();
		}
	}

	// ----- private methods -----
	private static NodeInterface node(final App app, final Map<String, NodeInterface> nodes, final String uuid) throws FrameworkException {

		NodeInterface node = nodes.get(uuid);
		if (node == null) {

			node = app.getNodeById(uuid);
			nodes.put(uuid, node);
		}

		return node;
	}

	private static void addView(final JsonObjectType type, final String... properties) {

		for (final String property : properties) {

			type.addViewProperty("report", property);
		}
	}

	private static DOMElement repeater(final Page page, final DOMNode parent, final PropertyKey functionQuery, final PropertyKey dataKey, final String query, final String key) throws FrameworkException {

		final DOMElement list = page.createElement("ul");
		final DOMElement item = page.createElement("li");

		parent.appendChild(list);
		list.appendChild(item);

		item.setProperty(functionQuery, query);
		item.setProperty(dataKey, key);

		return item;
	}
}
