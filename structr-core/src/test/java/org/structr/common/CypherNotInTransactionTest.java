/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.common;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNotNull;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;

/**
 *
 * @author Axel Morgner
 */
public class CypherNotInTransactionTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(CypherNotInTransactionTest.class.getName());

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01DeleteAfterLookupWithCypherInTransaction() {

		try {

			final TestSix testSix = this.createTestNode(TestSix.class);
			final TestOne testOne = this.createTestNode(TestOne.class);
			SixOneOneToOne rel            = null;

			assertNotNull(testSix);
			assertNotNull(testOne);
			
			try {
				app.beginTx();
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();

			} finally {

				app.finishTx();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			ExecutionEngine engine            = (ExecutionEngine) new ExecutionEngine(graphDb);
			
			Transaction tx                    = graphDb.beginTx();

			try {
				ExecutionResult result            = engine.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");

				final Iterator<Relationship> rels = result.columnAs("r");

				assertTrue(rels.hasNext());

				rels.next().delete();
				tx.success();

			} finally {

				tx.finish();

			}

			String uuid = rel.getUuid();
			assertNull("UUID of deleted relationship should be null", uuid);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * This test passes with Neo4j 1.8, but fails with 1.9.M02-1.9.5
	 */
//	public void test02DeleteRelationshipAfterLookupWithCypherNotInTransaction() {
//
//		try {
//
//			final NodeInterface testNode1 = this.createTestNode(TestSix.class);
//			final NodeInterface testNode2 = this.createTestNode(TestOne.class);
//			SixOneOneToOne rel            = null;
//
//			assertNotNull(testNode1);
//			assertNotNull(testNode2);
//
//			try {
//				app.beginTx();
//				rel = app.create(testNode1, testNode2, SixOneOneToOne.class);
//				app.commitTx();
//
//			} finally {
//
//				app.finishTx();
//			}
//
//			assertNotNull(rel);
//
//			GraphDatabaseService graphDb      = graphDbCommand.execute();
//			ExecutionEngine engine            = new ExecutionEngine(graphDb);
//			ExecutionResult result            = engine.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");
//
//			final Iterator<Relationship> rels = result.columnAs("r");
//			
//			assertTrue(rels.hasNext());
//			
//			Transaction tx                    = graphDb.beginTx();
//
//			try {
//
//				rels.next().delete();
//				tx.success();
//
//			} finally {
//
//				tx.finish();
//
//			}
//
//			try {
//
//				rel.getUuid();
//				fail("Should have raised an org.neo4j.graphdb.NotFoundException");
//
//			} catch (org.neo4j.graphdb.NotFoundException e) {}
//
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			fail("Unexpected exception");
//
//		}
//
//	}

	/**
	 * This test passes with Neo4j 1.8, but fails with 1.9.M02-1.9.5
	 */
//	public void test03DeleteNodeAfterLookupWithCypherNotInTransaction() {
//
//		try {
//
//			AbstractNode testNode = this.createTestNode(TestSix.class);
//
//			assertNotNull(testNode);
//
//			GraphDatabaseService graphDb      = graphDbCommand.execute();
//			ExecutionEngine engine            = new ExecutionEngine(graphDb);
//			ExecutionResult result            = engine.execute("start n = node:keywordAllNodes(type = 'TestSix') return n");
//
//			final Iterator<Node> nodes = result.columnAs("n");
//			
//			assertTrue(nodes.hasNext());
//			
//			Transaction tx                    = graphDb.beginTx();
//
//			try {
//
//				nodes.next().delete();
//				tx.success();
//
//			} finally {
//
//				tx.finish();
//
//			}
//
//			try {
//
//				testNode.getUuid();
//				fail("Should have raised an org.neo4j.graphdb.NotFoundException");
//
//			} catch (org.neo4j.graphdb.NotFoundException e) {}
//
//		} catch (FrameworkException ex) {
//
//			logger.log(Level.SEVERE, ex.toString());
//			fail("Unexpected exception");
//
//		}
//
//	}
		
	public void test03DeleteDirectly() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try {
				app.beginTx();
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();

			} finally {

				app.finishTx();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			
			Transaction tx                    = graphDb.beginTx();

			try {

				testOne.getRelationships().iterator().next().getRelationship().delete();
				tx.success();

			} finally {

				tx.finish();

			}

			String uuid = rel.getUuid();
			assertNull("UUID of deleted relationship should be null", uuid);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test04DeleteAfterIndexLookup() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try {
				app.beginTx();
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();

			} finally {

				app.finishTx();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb = graphDbCommand.execute();
			GraphObject  searchRes       = app.get(testSix.getUuid());
			
			assertNotNull(searchRes);
			
			Transaction tx = graphDb.beginTx();

			try {

				testSix.getRelationships().iterator().next().getRelationship().delete();
				tx.success();

			} finally {

				tx.finish();

			}

			String uuid = rel.getUuid();
			assertNull("UUID of deleted relationship should be null", uuid);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	
}
