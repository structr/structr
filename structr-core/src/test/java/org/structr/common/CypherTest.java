/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.SixOneManyToMany;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;

/**
 *
 */
public class CypherTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(CypherTest.class);

	@Test
	public void test01DeleteAfterLookupWithCypherInTransaction() {

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

				NativeResult<Relationship> result = graphDb.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");
				final Iterator<Relationship> rels = result.columnAs("r");

				assertTrue(rels.hasNext());

				rels.next().delete();

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

	@Test
	public void test03DeleteDirectly() {

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

				testOne.getRelationships().iterator().next().getRelationship().delete();
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

	@Test
	public void test04DeleteAfterIndexLookup() {

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

				testSix.getRelationships().iterator().next().getRelationship().delete();

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

	@Test
	public void test05RollbackDelete() {

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
				testOne.getRelationships().iterator().next().getRelationship().delete();
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

	@Test
	public void testCypherResultWrapping() {

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

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH (n:TestOne) RETURN DISTINCT n");

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

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH (n:TestOne)-[r]-(m:TestSix) RETURN DISTINCT  n, r, m ");
			final Iterator<GraphObject> it = result.iterator();

			assertEquals("Invalid wrapped cypher query result", 300, result.size());

			while (it.hasNext()) {

				assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
				assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
				assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m
			}

			tx.success();


		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN p ");

			assertEquals("Invalid wrapped cypher query result", 100, result.size());

			for (final GraphObject obj : result) {

				assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, obj.getClass());
			}

			tx.success();


		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN { nodes: nodes(p), rels: relationships(p) } ");

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

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN DISTINCT { path: p, value: 12 } ");

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

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN { nodes: { x : { y : { z : nodes(p) } } } } ");

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

		/*
		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN p");

			assertEquals("Invalid wrapped cypher query result", 100, result.size());

			for (final GraphObject obj : result) {

				assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, obj.getClass());

				final Object paths = obj.getProperty(new StringProperty("p"));

				assertTrue("Invalid wrapped cypher query result", paths instanceof Iterable);

				final Iterator it = ((Iterable)paths).iterator();
				while (it.hasNext()) {

					assertEquals("Invalid wrapped cypher query result", TestOne.class, it.next().getClass());		// n
					assertEquals("Invalid wrapped cypher query result", SixOneManyToMany.class, it.next().getClass());	// r
					assertEquals("Invalid wrapped cypher query result", TestSix.class, it.next().getClass());		// m
				}
			}

			tx.success();


		} catch (FrameworkException ex) {
			logger.error("", ex);
		}
		*/
	}
}
