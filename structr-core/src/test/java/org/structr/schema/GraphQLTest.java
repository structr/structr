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
package org.structr.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.Validator;
import graphql.validation.ValidationError;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.StructrTest;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ISO8601DateProperty;

/**
 *
 */
public class GraphQLTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLTest.class.getName());

	@Test
	public void testGraphQLBasics() {

		try (final Tx tx = app.tx()) {

			final Group group1 = createTestNode(Group.class, "group1");
			final Group group2 = createTestNode(Group.class, "group2");
			final Group group3 = createTestNode(Group.class, "group3");
			final Principal p1 = createTestNode(Principal.class, "principal1");

			group1.addMember(p1);

			group1.setProperty(NodeInterface.owner, p1);

			final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
			final ExecutionResult result = graphQL.execute("{ Group { id, type, name, createdDate, owner { id, name }, members { id, type, name } } }");
			final Gson gson              = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

			if (result != null) {

				final Map<String, Object> data = result.getData();
				if (data != null) {

					Assert.assertTrue("GraphQL result should contain a field named Group", data.containsKey("Group"));

					final SimpleDateFormat df              = new SimpleDateFormat(ISO8601DateProperty.getDefaultFormat());
					final List<Map<String, Object>> groups = (List<Map<String, Object>>)data.get("Group");

					Assert.assertEquals("Result should contain 3 elements", 3, groups.size());

					final Map<String, Object> g1 = groups.get(0);
					final Map<String, Object> g2 = groups.get(1);
					final Map<String, Object> g3 = groups.get(2);

					// test first group
					Assert.assertEquals("ID mismatch for first group",   group1.getUuid(), g1.get("id"));
					Assert.assertEquals("Type mismatch for first group", group1.getType(), g1.get("type"));
					Assert.assertEquals("Name mismatch for first group", group1.getName(), g1.get("name"));
					Assert.assertEquals("Date mismatch for first group", df.format(group1.getCreatedDate()), g1.get("createdDate"));

					Assert.assertTrue("First group should contain a field named owner", g1.containsKey("owner"));

					final Map<String, Object> owner = (Map<String, Object>)g1.get("owner");

					Assert.assertEquals("ID mismatch for owner of first group", p1.getUuid(), owner.get("id"));
					Assert.assertEquals("Name mismatch for owner of first group", p1.getName(), owner.get("name"));

					Assert.assertTrue("First group should contain a field named members", g1.containsKey("members"));

					final List<Map<String, Object>> members = (List<Map<String, Object>>)g1.get("members");

					Assert.assertEquals("Members of first group should contain 1 element", 1, members.size());

					final Map<String, Object> firstMember = members.get(0);

					Assert.assertEquals("ID mismatch for first member of first group", p1.getUuid(), firstMember.get("id"));
					Assert.assertEquals("Name mismatch for first member of first group", p1.getName(), firstMember.get("name"));

					// test second group
					Assert.assertEquals("ID mismatch for second group",   group2.getUuid(), g2.get("id"));
					Assert.assertEquals("Type mismatch for second group", group2.getType(), g2.get("type"));
					Assert.assertEquals("Name mismatch for second group", group2.getName(), g2.get("name"));
					Assert.assertEquals("Date mismatch for second group", df.format(group2.getCreatedDate()), g2.get("createdDate"));

					// test third group
					Assert.assertEquals("ID mismatch for third group",   group3.getUuid(), g3.get("id"));
					Assert.assertEquals("Type mismatch for third group", group3.getType(), g3.get("type"));
					Assert.assertEquals("Name mismatch for third group", group3.getName(), g3.get("name"));
					Assert.assertEquals("Date mismatch for third group", df.format(group3.getCreatedDate()), g3.get("createdDate"));

				} else {

					for (final GraphQLError error : result.getErrors()) {

						System.out.println(error.getMessage());
					}
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	@Test
	public void testGraphQLArguments() {

		try (final Tx tx = app.tx()) {

			final Group group1 = createTestNode(Group.class, "group1");
			final Group group2 = createTestNode(Group.class, "group2");
			final Group group3 = createTestNode(Group.class, "group3");

			final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
			final ExecutionResult result = graphQL.execute("{ Group(name: \"group2\") { id, type, name } }");

			if (result != null) {

				final Map<String, Object> data = result.getData();
				if (data != null) {

					Assert.assertTrue("GraphQL result should contain a field named Group", data.containsKey("Group"));

					final List<Map<String, Object>> groups = (List<Map<String, Object>>)data.get("Group");

					Assert.assertEquals("Result should contain 1 element", 1, groups.size());

					final Map<String, Object> g1 = groups.get(0);

					// test first group
					Assert.assertEquals("ID mismatch for first group",   group2.getUuid(), g1.get("id"));
					Assert.assertEquals("Type mismatch for first group", group2.getType(), g1.get("type"));
					Assert.assertEquals("Name mismatch for first group", group2.getName(), g1.get("name"));

				} else {

					for (final GraphQLError error : result.getErrors()) {

						System.out.println(error.getMessage());
					}
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	@Test
	public void testGraphQLPagination() {

		try (final Tx tx = app.tx()) {

			final GraphQL graphQL = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
			final Group group1    = createTestNode(Group.class, "group1");
			final Group group2    = createTestNode(Group.class, "group2");
			final Group group3    = createTestNode(Group.class, "group3");
			final Group group4    = createTestNode(Group.class, "group4");
			final Group group5    = createTestNode(Group.class, "group5");

			// test pagination and sorting
			final ExecutionResult result1 = graphQL.execute("{ Group(_pageSize: 1, _sort: \"name\") { id, type, name } }");
			if (result1 != null) {

				final Map<String, Object> data = result1.getData();
				if (data != null) {

					Assert.assertTrue("GraphQL result should contain a field named Group", data.containsKey("Group"));

					final List<Map<String, Object>> groups = (List<Map<String, Object>>)data.get("Group");

					Assert.assertEquals("Result should contain 1 element", 1, groups.size());

					final Map<String, Object> g1 = groups.get(0);

					// test first group
					Assert.assertEquals("Name mismatch for first group", group1.getName(), g1.get("name"));
					Assert.assertEquals("ID mismatch for first group",   group1.getUuid(), g1.get("id"));
					Assert.assertEquals("Type mismatch for first group", group1.getType(), g1.get("type"));

				} else {

					for (final GraphQLError error : result1.getErrors()) {

						System.out.println(error.getMessage());
					}
				}
			}

			// test pagination and sorting
			final ExecutionResult result2 = graphQL.execute("{ Group(_pageSize: 1, _page: 3, _sort: \"name\") { id, type, name } }");
			if (result2 != null) {

				final Map<String, Object> data = result2.getData();
				if (data != null) {

					Assert.assertTrue("GraphQL result should contain a field named Group", data.containsKey("Group"));

					final List<Map<String, Object>> groups = (List<Map<String, Object>>)data.get("Group");

					Assert.assertEquals("Result should contain 1 element", 1, groups.size());

					final Map<String, Object> g1 = groups.get(0);

					// test first group
					Assert.assertEquals("Name mismatch for first group", group3.getName(), g1.get("name"));
					Assert.assertEquals("ID mismatch for first group",   group3.getUuid(), g1.get("id"));
					Assert.assertEquals("Type mismatch for first group", group3.getType(), g1.get("type"));

				} else {

					for (final GraphQLError error : result2.getErrors()) {

						System.out.println(error.getMessage());
					}
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	@Test
	public void testGraphQLSchema() {

		try (final Tx tx = app.tx()) {

			final GraphQL graphQL        = GraphQL.newGraphQL(SchemaService.getGraphQLSchema()).build();
			final ExecutionResult result = graphQL.execute("{ __schema { types { name, fields { name, type { name, kind } } } } }");
			final Gson gson              = new GsonBuilder().setPrettyPrinting().create();

			if (result != null) {

				final Object data = result.getData();
				if (data != null) {

					System.out.println(gson.toJson(data));

				} else {

					for (final GraphQLError error : result.getErrors()) {

						System.out.println(error.getMessage());
					}
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	@Test
	public void testSchemaWalk() {

		try (final Tx tx = app.tx()) {

			final GraphQLSchema schema = SchemaService.getGraphQLSchema();
			final GraphQL graphQL      = GraphQL.newGraphQL(schema).build();

			final Parser parser                = new Parser();
			final Document doc                 = parser.parseDocument("{ Group(_pageSize: 1, _page: 3, _sort: \"name\") { id, type, name }, User }");
			//final Document doc                 = parser.parseDocument("{ Group { id, type, name, createdDate, owner { id, name }, members { id, type, name } } }");
			final Validator validator          = new Validator();
			final List<ValidationError> errors = validator.validateDocument(schema, doc);

			for (final Node child : doc.getChildren()) {

				if (child instanceof OperationDefinition) {

					final OperationDefinition operationDefinition = (OperationDefinition)child;
					final SelectionSet selectionSet               = operationDefinition.getSelectionSet();

					print(selectionSet, 0);
				}
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// ----- private methods -----
	private void print(final SelectionSet set, final int depth) {

		for (final Selection selection : set.getSelections()) {

			if (selection instanceof Field) {

				final Field field = (Field)selection;

				for (int i=0; i<depth; i++) { System.out.print("    "); }
				System.out.println("Field " + field.getName());
				System.out.println("    Arguments: " + field.getArguments());

				final SelectionSet subSet = field.getSelectionSet();
				if (subSet != null) {

					print(field.getSelectionSet(), depth+1);
				}

			}
		}
	}
}
