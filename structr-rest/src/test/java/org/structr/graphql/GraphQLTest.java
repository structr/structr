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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.rest.common.StructrGraphQLTest;

/**
 *
 *
 */
public class GraphQLTest extends StructrGraphQLTest {

	private static final Logger logger = LoggerFactory.getLogger(GraphQLTest.class.getName());

	@Test
	public void testBasics() {

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

		RestAssured.basePath = "/structr/graphql";

		final String query1 = "{ Group { id, type, name, members { id, type, name } }, Principal(_pageSize: 1) { id, type name }}";
		final String query2 = "{ Group { id, type, name, members { } }}";

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
	}

	@Test
	public void testAdvancedQueries() {

		final List<Principal> team = new LinkedList<>();
		Group group                = null;

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

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/structr/graphql";

		System.out.println(fetchGraphQL("{ Group { members(name: {_contains: \"K\", _contains: \"L\", _conj: \"or\"}) }}}"));
		//System.out.println(fetchGraphQL("{ Group { members { name(_contains: \"K\", _contains: \"L\", _conj: \"or\") }}}"));
		//System.out.println(fetchGraphQL("{ Group { members { members { name(_contains: \"K\", _contains: \"L\", _conj: \"or\") }}}}"));
		//System.out.println(fetchGraphQL("{ Principal { id, type, name(_contains: \"K\", _contains: \"L\") }}"));
		//System.out.println(fetchGraphQL("{ Principal { id, type, name(_equals: \"Axel\") }}"));
		//System.out.println(fetchGraphQL("{ Principal { id, type, name(_contains: \"e\") }}"));

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
}
