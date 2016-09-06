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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.SixOneManyToMany;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.CypherQueryCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;

/**
 *
 */
public class CypherResultTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(CypherResultTest.class.getName());

	public void testCypherResultWrapping() {

		try (final Tx tx = app.tx()) {

			List<TestOne> testOnes = createTestNodes(TestOne.class, 10);
			List<TestSix> testSixs = createTestNodes(TestSix.class, 10);

			for (final TestOne testOne : testOnes) {

				testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "", ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			final List<GraphObject> result = app.command(CypherQueryCommand.class).execute("MATCH p = (n:TestOne)-[r]-(m:TestSix) RETURN p ");

			assertEquals("Invalid wrapped cypher query result", 100, result.size());

			for (final GraphObject obj : result) {

				assertEquals("Invalid wrapped cypher query result", GraphObjectMap.class, obj.getClass());
			}

			tx.success();


		} catch (FrameworkException ex) {
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
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
			Logger.getLogger(CypherResultTest.class.getName()).log(Level.SEVERE, null, ex);
		}
		*/
	}
}
