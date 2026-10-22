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
package org.structr.test.core.script;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Data and assertions for the tests of the sort() function and of the sort predicate of find(): a
 * type Person with names, a nick, an age, a team and projects, and helpers that evaluate an
 * expression in StructrScript and in JavaScript and compare the names of the result.
 */
public abstract class SortTestBase extends StructrTest {

	// ----- protected methods -----
	protected void createSortSchema() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType team    = schema.addType("Team");
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType person  = schema.addType("Person");

			person.addStringProperty("lastName");
			person.addStringProperty("firstName");
			person.addStringProperty("nick");
			person.addIntegerProperty("age");

			person.relate(team, "MEMBER_OF", Cardinality.ManyToOne, "members", "team");
			person.relate(project, "WORKS_ON", Cardinality.ManyToMany, "people", "projects");

			// a subtype with a key that only it has
			final JsonObjectType employee = schema.addType("Employee");

			employee.addTrait("Person");
			employee.addStringProperty("department");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	protected void createPeople(final PersonSpec... people) {

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of("Person");

			for (final PersonSpec spec : people) {

				final NodeInterface node = app.create("Person", new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), spec.name));

				node.setProperty(traits.key("lastName"),  spec.lastName);
				node.setProperty(traits.key("firstName"), spec.firstName);
				node.setProperty(traits.key("nick"),      spec.nick);
				node.setProperty(traits.key("age"),       spec.age);

				if (spec.team != null) {

					node.setProperty(traits.key("team"), getOrCreate("Team", spec.team));
				}

				if (spec.projects != null) {

					final List<NodeInterface> projects = new ArrayList<>();

					for (final String project : spec.projects) {

						projects.add(getOrCreate("Project", project));
					}

					node.setProperty(traits.key("projects"), projects);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	protected void createEmployee(final String name, final String department) {

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of("Employee");

			app.create("Employee",
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key("department"),                               department)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	protected NodeInterface getOrCreate(final String type, final String name) throws FrameworkException {

		final NodeInterface existing = app.nodeQuery(type).name(name).getFirst();
		if (existing != null) {

			return existing;
		}

		return app.create(type, name);
	}

	/**
	 * Evaluates the expression in both languages and compares the names of the sorted nodes, or the
	 * sorted strings, with the expected list.
	 */
	protected void assertSorted(final String message, final List<String> expected, final String structrScript, final String javaScript) {

		assertSorted(message, expected, structrScript, javaScript, Locale.ENGLISH);
	}

	protected void assertSorted(final String message, final List<String> expected, final String structrScript, final String javaScript, final Locale locale) {

		assertEquals(message + " (StructrScript)", expected, evaluate("${" + structrScript + "}", locale));
		assertEquals(message + " (JavaScript)",    expected, evaluate("${{ " + javaScript + "; }}", locale));
	}

	/**
	 * The options object for StructrScript, written with single quotes, which become the double quotes of JSON.
	 */
	protected String json(final String text) {

		return "fromJson('" + text.replace("'", "\"") + "')";
	}

	protected void assertSortedJavaScript(final String message, final List<String> expected, final String javaScript) {

		assertEquals(message + " (JavaScript)", expected, evaluate("${{ " + javaScript + "; }}", Locale.ENGLISH));
	}

	/**
	 * Expects the expression to be rejected with 422 in both languages.
	 */
	protected void assertFails(final String message, final String structrScript, final String javaScript) {

		for (final String expression : List.of("${" + structrScript + "}", "${{ " + javaScript + "; }}")) {

			try (final Tx tx = app.tx()) {

				Scripting.evaluate(new ActionContext(securityContext), null, expression, "sortFunctionTest");

				fail(message + " must be rejected: " + expression);

			} catch (FrameworkException fex) {

				assertEquals(message + " must be rejected with 422: " + expression, 422, fex.getStatus());
			}
		}
	}

	protected List<String> evaluate(final String expression, final Locale locale) {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			ctx.setLocale(locale);

			final Object result      = Scripting.evaluate(ctx, null, expression, "sortFunctionTest");
			final List<String> names = new ArrayList<>();

			for (final Object element : toIterable(result)) {

				if (element instanceof GraphObject obj) {

					names.add(obj.getProperty(Traits.of("Person").key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

				} else {

					names.add(element != null ? element.toString() : null);
				}
			}

			tx.success();

			return names;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception in " + expression);
		}

		return null;
	}

	protected Iterable<?> toIterable(final Object result) {

		if (result instanceof Iterable<?> iterable) {

			return iterable;
		}

		if (result instanceof Object[] array) {

			return Arrays.asList(array);
		}

		fail("Expected a collection, got " + result);

		return null;
	}

	protected PersonSpec person(final String name) {

		return new PersonSpec(name);
	}

	// ----- nested classes -----
	protected static class PersonSpec {

		private final String name;
		private String lastName   = null;
		private String firstName  = null;
		private String nick       = null;
		private Integer age       = null;
		private String team       = null;
		private String[] projects = null;

		PersonSpec(final String name) {

			this.name = name;
		}

		PersonSpec lastName(final String value) {

			this.lastName = value;

			return this;
		}

		PersonSpec firstName(final String value) {

			this.firstName = value;

			return this;
		}

		PersonSpec nick(final String value) {

			this.nick = value;

			return this;
		}

		PersonSpec age(final Integer value) {

			this.age = value;

			return this;
		}

		PersonSpec team(final String value) {

			this.team = value;

			return this;
		}

		PersonSpec projects(final String... values) {

			this.projects = values;

			return this;
		}
	}
}
