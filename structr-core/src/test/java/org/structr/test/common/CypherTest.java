/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Relationship;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.schema.export.StructrSchema;
import org.structr.test.core.entity.SixOneManyToMany;
import org.structr.test.core.entity.SixOneOneToOne;
import org.structr.test.core.entity.TestOne;
import org.structr.test.core.entity.TestSix;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.*;
import java.util.Map.Entry;
import java.util.LinkedList;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class CypherTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(CypherTest.class);

	@Test
	public void test01DeleteAfterLookupWithCypherInTransaction() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try {

				final TestSix testSix = this.createTestNode(TestSix.class);
				final TestOne testOne = this.createTestNode(TestOne.class);
				SixOneOneToOne rel            = null;

				assertNotNull(testSix);
				assertNotNull(testOne);

				try (final Tx tx = app.tx()) {

					rel = app.create(testSix, testOne, SixOneOneToOne.class);
					tx.success();
				}

				assertNotNull(rel);

				DatabaseService graphDb = app.command(GraphDatabaseCommand.class).execute();

				try (final Tx tx = app.tx()) {

					final NativeQuery<Iterable> query     = graphDb.query("MATCH (n:" + randomTenantId + ")<-[r:ONE_TO_ONE]-() RETURN r", Iterable.class);
					Iterable<Map<String, Object>> result  = graphDb.execute(query);
					final Iterable<Relationship> iterable = Iterables.map(row -> { return (Relationship)row.get("r"); }, result);
					final Iterator<Relationship> rels     = iterable.iterator();

					assertTrue(rels.hasNext());

					rels.next().delete(true);

					tx.success();
				}

				try (final Tx tx = app.tx()) {

					rel.getUuid();
					fail("Accessing a deleted relationship should thow an exception.");

					tx.success();

				} catch (NotFoundException iex) {
				}

			} catch (FrameworkException ex) {

				logger.error(ex.toString());
				fail("Unexpected exception");

			}
		}
	}

	@Test
	public void test03DeleteDirectly() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;
			String uuid = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				uuid = rel.getUuid();
				tx.success();
			}

			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				testOne.getRelationships().iterator().next().getRelationship().delete(true);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List result = app.relationshipQuery().uuid(uuid).getAsList();

				assertEquals("Relationship should have been deleted", 0, result.size());

				tx.success();

			} catch (NotFoundException nfex) {
				assertNotNull(nfex.getMessage());
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void test04DeleteAfterIndexLookup() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try {

				final TestOne testOne  = createTestNode(TestOne.class);
				final TestSix testSix  = createTestNode(TestSix.class);
				SixOneOneToOne rel     = null;

				assertNotNull(testOne);
				assertNotNull(testSix);

				try (final Tx tx = app.tx()) {

					rel = app.create(testSix, testOne, SixOneOneToOne.class);
					tx.success();
				}

				assertNotNull(rel);

				try (final Tx tx = app.tx()) {

					GraphObject  searchRes = app.getNodeById(testSix.getUuid());
					assertNotNull(searchRes);

					tx.success();
				}

				try (final Tx tx = app.tx()) {

					testSix.getRelationships().iterator().next().getRelationship().delete(true);

					tx.success();
				}

				try (final Tx tx = app.tx()) {

					rel.getUuid();
					fail("Accessing a deleted relationship should thow an exception.");

					tx.success();

				} catch (NotFoundException nfex) {
					assertNotNull(nfex.getMessage());
				}

			} catch (FrameworkException ex) {

				logger.error(ex.toString());
				fail("Unexpected exception");

			}
		}
	}

	@Test
	public void test05RollbackDelete() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try {

				final TestOne testOne = createTestNode(TestOne.class);
				final TestSix testSix = createTestNode(TestSix.class);
				String relId          = null;
				SixOneOneToOne rel    = null;

				assertNotNull(testOne);
				assertNotNull(testSix);

				try (final Tx tx = app.tx()) {

					rel   = app.create(testSix, testOne, SixOneOneToOne.class);
					relId = rel.getUuid();
					tx.success();
				}

				assertNotNull(rel);

				try (final Tx tx = app.tx()) {

					// do not commit transaction
					testOne.getRelationships().iterator().next().getRelationship().delete(true);
				}

				try (final Tx tx = app.tx()) {

					assertEquals("UUID of relationship should be readable after rollback", relId, rel.getUuid());
					tx.success();

				} catch (NotFoundException iex) {

				}

			} catch (FrameworkException ex) {

				logger.error(ex.toString());
				fail("Unexpected exception");

			}
		}
	}

	@Test
	public void testCypherResultWrapping() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try (final Tx tx = app.tx()) {

				List<TestOne> testOnes = createTestNodes(TestOne.class, 10);
				List<TestSix> testSixs = createTestNodes(TestSix.class, 10);

				for (final TestOne testOne : testOnes) {

					testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);
				}

				tx.success();

			} catch (FrameworkException ex) {

				logger.warn("", ex);
				fail("Unexpected exception");
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> result = Iterables.toList(app.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ") RETURN DISTINCT n"));

				assertEquals("Invalid wrapped cypher query result", 10, result.size());

				for (final GraphObject obj : result) {

					System.out.println(obj);
					assertEquals("Invalid wrapped cypher query result", TestOne.class, obj.getClass());
				}

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> result  = Iterables.toList(app.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN DISTINCT  n, r, m "));
				final Iterator<GraphObject> rit = result.iterator();

				assertEquals("Invalid wrapped cypher query result", 100, result.size());

				while (rit.hasNext()) {

					final Iterable tuple = (Iterable)rit.next();
					final Iterator it    = tuple.iterator();

					assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
					assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
					assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m
				}

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			try (final Tx tx = app.tx()) {

				final Iterable result = app.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN p ");
				final Iterator rit    = result.iterator();
				int resultCount       = 0;

				while (rit.hasNext()) {

					final Iterable tuple = (Iterable)rit.next();
					final Iterator it    = tuple.iterator();

					assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
					assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
					assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m

					resultCount++;
				}

				assertEquals("Invalid wrapped cypher query result", 100, resultCount);

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> result = Iterables.toList(app.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN { nodes: nodes(p), rels: relationships(p) } "));

				assertEquals("Invalid wrapped cypher query result", 100, result.size());

				for (final GraphObject obj : result) {

					assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, obj.getClass());

					final Object nodes = obj.getProperty(new StringProperty("nodes"));
					final Object rels  = obj.getProperty(new StringProperty("rels"));

					assertTrue("Invalid wrapped cypher query result", nodes instanceof Collection);
					assertTrue("Invalid wrapped cypher query result", rels instanceof Collection);

					final Iterator it = ((Collection)nodes).iterator();
					while (it.hasNext()) {

						assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());
						assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());
					}

					for (final Object node : ((Collection)rels)) {
						assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, node.getClass());
					}

				}

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> result = Iterables.toList(app.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN DISTINCT { path: p, value: 12 } "));

				assertEquals("Invalid wrapped cypher query result", 100, result.size());


				final Iterator it = result.iterator();
				while (it.hasNext()) {

					final Object path  = it.next();
					final Object value = it.next();

					assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, path.getClass());
					assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, value.getClass());
					assertEquals("Invalid wrapped cypher query result", 12L, ((GraphObjectMap)value).getProperty(new StringProperty("value")));
				}

				tx.success();

			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> result = Iterables.toList(app.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN { nodes: { x : { y : { z : nodes(p) } } } } "));

				assertEquals("Invalid wrapped cypher query result", 100, result.size());

				for (final GraphObject obj : result) {

					assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, obj.getClass());

					final Object nodes = obj.getProperty(new StringProperty("nodes"));
					assertTrue("Invalid wrapped cypher query result", nodes instanceof GraphObjectMap);

					final Object x = ((GraphObjectMap)nodes).getProperty(new StringProperty("x"));
					assertTrue("Invalid wrapped cypher query result", x instanceof GraphObjectMap);

					final Object y = ((GraphObjectMap)x).getProperty(new StringProperty("y"));
					assertTrue("Invalid wrapped cypher query result", y instanceof GraphObjectMap);

					final Object z = ((GraphObjectMap)y).getProperty(new StringProperty("z"));
					assertTrue("Invalid wrapped cypher query result", z instanceof Collection);

				}

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}



			String testOneId = null;
			String testSixId = null;

			try (final Tx tx = app.tx()) {

				final TestOne t1 = createTestNode(TestOne.class, "singleTestOne");
				final TestSix t6 = createTestNode(TestSix.class, "singleTestSix");

				t1.setProperty(TestOne.manyToManyTestSixs, Arrays.asList(t6));

				testOneId = t1.getUuid();
				testSixId = t6.getUuid();

				tx.success();

			} catch (FrameworkException ex) {

				logger.warn("", ex);
				fail("Unexpected exception");
			}

			final App anonymousApp = StructrApp.getInstance(SecurityContext.getInstance(null, org.structr.common.AccessMode.Frontend));

			// test that relationships are not returned if the user is not allowed to see SOURCE AND TARGET node of relationship
			try (final Tx tx = anonymousApp.tx()) {

				final List<GraphObject> result = Iterables.toList(anonymousApp.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") WHERE n.id = \"" + testOneId + "\" AND m.id = \"" + testSixId + "\" RETURN r LIMIT 1"));

				assertEquals("Invalid wrapped cypher query result - both end nodes of relationship are not visible, relationship should also not be visible", 0, result.size());

				final GraphObject t1 = app.getNodeById(testOneId);
				t1.setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			// test that relationships are not returned if the user is not allowed to see SOURCE node of relationship
			try (final Tx tx = anonymousApp.tx()) {

				final List<GraphObject> result = Iterables.toList(anonymousApp.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") WHERE n.id = \"" + testOneId + "\" AND m.id = \"" + testSixId + "\" RETURN r LIMIT 1"));

				assertEquals("Invalid wrapped cypher query result - source node of relationship is not visible, relationship should also not be visible", 0, result.size());

				final GraphObject t1 = app.getNodeById(testOneId);
				t1.setProperty(GraphObject.visibleToPublicUsers, false);

				final GraphObject t6 = app.getNodeById(testSixId);
				t6.setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			// test that relationships are not returned if the user is not allowed to see TARGET node of relationship
			try (final Tx tx = anonymousApp.tx()) {

				final List<GraphObject> result = Iterables.toList(anonymousApp.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") WHERE n.id = \"" + testOneId + "\" AND m.id = \"" + testSixId + "\" RETURN r LIMIT 1"));

				assertEquals("Invalid wrapped cypher query result - target node of relationship is not visible, relationship should also not be visible", 0, result.size());

				final GraphObject t1 = app.getNodeById(testOneId);
				t1.setProperty(GraphObject.visibleToPublicUsers, true);

				final GraphObject t6 = app.getNodeById(testSixId);
				t6.setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			// test that relationships ARE returned if the user is allowed to see SOURCE AND TARGET node of relationship
			try (final Tx tx = anonymousApp.tx()) {

				final List<GraphObject> result = Iterables.toList(anonymousApp.command(NativeQueryCommand.class).execute("MATCH (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") WHERE n.id = \"" + testOneId + "\" AND m.id = \"" + testSixId + "\" RETURN r LIMIT 1"));

				assertEquals("Invalid wrapped cypher query result - relationship should be visible", 1, result.size());

				tx.success();

			} catch (FrameworkException ex) {
				logger.error("", ex);
			}
		}
	}

	@Test
	public void testCypherPathWrappingWithPermissions() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			PrincipalInterface tester = null;

			try (final Tx tx = app.tx()) {

				final List<TestOne> testOnes = createTestNodes(TestOne.class, 10);
				final List<TestSix> testSixs = createTestNodes(TestSix.class, 10);
				int count                    = 0;

				tester = app.create(User.class, "tester");

				for (final TestSix testSix : testSixs) {
					testSix.grant(Permission.read, tester);
				}

				for (final TestOne testOne : testOnes) {

					testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);

					if (count++ < 3) {
						testOne.grant(Permission.read, tester);
					}
				}

				tx.success();

			} catch (FrameworkException ex) {

				logger.warn("", ex);
				fail("Unexpected exception");
			}

			try (final Tx tx = app.tx()) {

				final Iterable result = app.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN p");
				final Iterator rit    = result.iterator();
				int resultCount       = 0;

				while (rit.hasNext()) {

					final Iterable tuple = (Iterable)rit.next();
					final Iterator it    = tuple.iterator();

					assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
					assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
					assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m

					resultCount++;
				}

				assertEquals("Invalid path query result", 100, resultCount);

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}

			// test visibility of path elements as well
			final App testerApp = StructrApp.getInstance(SecurityContext.getInstance(tester, AccessMode.Backend));

			try (final Tx tx = testerApp.tx()) {

				final List<GraphObject> result = Iterables.toList(testerApp.command(NativeQueryCommand.class).execute("MATCH p = (n:TestOne:" + randomTenantId + ")-[r]-(m:TestSix:" + randomTenantId + ") RETURN p"));
				final Iterator rit    = result.iterator();
				int resultCount       = 0;

				while (rit.hasNext()) {

					final Iterable tuple = (Iterable)rit.next();
					final Iterator it    = tuple.iterator();

					assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
					assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
					assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m

					resultCount++;
				}

				assertEquals("Invalid path permission resolution result for non-admin user", 30, resultCount);

				tx.success();


			} catch (FrameworkException ex) {
				logger.error("", ex);
			}
		}
	}

	@Test
	public void testPathWrapper() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try {

				try (final Tx tx = app.tx()) {

					final TestSix testSix = this.createTestNode(TestSix.class, "testnode");
					final String uuid     = testSix.getUuid();

					assertNotNull(testSix);

					final Iterable result = app.command(NativeQueryCommand.class).execute("MATCH path = (x:" + randomTenantId + ") WHERE x.name = 'testnode' RETURN path");
					final Iterator rit    = result.iterator();

					while (rit.hasNext()) {

						final Iterable tuple = (Iterable)rit.next();
						final Iterator it    = tuple.iterator();
						final TestSix test   = (TestSix)it.next();

						assertEquals("Invalid wrapped cypher query result", TestSix.class, test.getClass());
						assertEquals("Invalid wrapped cypher query result", uuid,          test.getUuid());
					}

					tx.success();
				}

			} catch (FrameworkException ex) {

				ex.printStackTrace();
				fail("Unexpected exception");
			}
		}
	}

	@Test
	public void testNativeCypherMapping() {

		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			try (final Tx tx = app.tx()) {

				final JsonSchema schema      = StructrSchema.createFromDatabase(app);
				final JsonObjectType project = schema.addType("Project");
				final JsonObjectType task    = schema.addType("Task");

				// create relation
				final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
				rel.setName("ProjectTasks");

				StructrSchema.extendDatabaseSchema(app, schema);

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				fail("Unexpected exception");
			}

			try {

				final Class projectType      = StructrApp.getConfiguration().getNodeEntityClass("Project");
				final Class taskType         = StructrApp.getConfiguration().getNodeEntityClass("Task");
				final PropertyKey projectKey = StructrApp.key(taskType, "project");

				createTestNode(taskType,
					new NodeAttribute<>(AbstractNode.name, "Task1"),
					new NodeAttribute<>(projectKey, createTestNode(projectType, new NodeAttribute<>(AbstractNode.name, "Project1")))
				);

				createTestNode(taskType,
					new NodeAttribute<>(AbstractNode.name, "Task2"),
					new NodeAttribute<>(projectKey, createTestNode(projectType, new NodeAttribute<>(AbstractNode.name, "Project2")))
				);

			} catch (FrameworkException t) {

				t.printStackTrace();
				fail("Unexpected exception");
			}


			final Map<String, String> tests = new LinkedHashMap<>();
			final Gson gson                 = new GsonBuilder().create();

			tests.put("MATCH (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n",                                       "[\"Project\",\"Project\"]");
			tests.put("MATCH (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN r",                                       "[\"has\",\"has\"]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN path",                             "[[\"Project\",\"has\",\"Task\"],[\"Project\",\"has\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN nodes(path)",                      "[[\"Project\",\"Task\"],[\"Project\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN nodes(path), relationships(path)", "[[[\"Project\",\"Task\"],[\"has\"]],[[\"Project\",\"Task\"],[\"has\"]]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n, r, m",                          "[[\"Project\",\"has\",\"Task\"],[\"Project\",\"has\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n, m",                             "[[\"Project\",\"Task\"],[\"Project\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN { n: n, r: r }",                   "[{\"n\":\"Project\",\"r\":\"has\"},{\"n\":\"Project\",\"r\":\"has\"}]");
			tests.put("MATCH (true) RETURN { a: 1, b: 2, c: 3 } LIMIT 1",                                                                             "[{\"a\":1,\"b\":2,\"c\":3}]");

			tests.put("MATCH (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n",                                       "[\"Project\",\"Project\"]");
			tests.put("MATCH (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN r",                                       "[\"has\",\"has\"]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN path",                             "[[\"Project\",\"has\",\"Task\"],[\"Project\",\"has\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN nodes(path)",                      "[[\"Project\",\"Task\"],[\"Project\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN nodes(path), relationships(path)", "[[[\"Project\",\"Task\"],[\"has\"]],[[\"Project\",\"Task\"],[\"has\"]]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n, r, m",                          "[[\"Project\",\"has\",\"Task\"],[\"Project\",\"has\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN n, m",                             "[[\"Project\",\"Task\"],[\"Project\",\"Task\"]]");
			tests.put("MATCH path = (n:Project:" + randomTenantId + ")-[r]->(m:Task:" + randomTenantId + ") RETURN { n: n, r: r }",                   "[{\"n\":\"Project\",\"r\":\"has\"},{\"n\":\"Project\",\"r\":\"has\"}]");
			tests.put("MATCH (true) RETURN { a: 1, b: 2, c: 3 } LIMIT 1",                                                                             "[{\"a\":1,\"b\":2,\"c\":3}]");

			try (final Tx tx = app.tx()) {

				for (final Entry<String, String> test : tests.entrySet()) {

					final String query = test.getKey();
					final String check = test.getValue();

					final Object result    = app.command(NativeQueryCommand.class).execute(query);
					final List list        = Iterables.toList((Iterable)result);
					final String structure = gson.toJson(resolve(list));

					if (!check.equals(structure)) {
						System.out.println("################# " + query);
						System.out.println(structure);
					}

					assertEquals("Invalid native query result structure", check, structure);
				}

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

		}
	}

	// ----- private methods -----
	private Object resolve(final Object src) {

		if (src instanceof Iterable) {

			final List list   = new LinkedList<>();
			final Iterable i  = (Iterable)src;
			final Iterator it = i.iterator();

			while (it.hasNext()) {

				list.add(resolve(it.next()));
			}

			return list;
		}

		if (src instanceof Map) {

			final Map map = new TreeMap<>();

			for (final Entry<String, Object> entry : ((Map<String, Object>)src).entrySet()) {

				map.put(entry.getKey(), resolve(entry.getValue()));
			}

			return map;
		}

		if (src instanceof GraphObjectMap) {

			return resolve(((GraphObjectMap)src).toMap());
		}

		if (src instanceof NodeInterface) {
			return ((NodeInterface)src).getType();
		}

		if (src instanceof RelationshipInterface) {
			return ((RelationshipInterface)src).getRelType().name();
		}

		return src;
	}
}


