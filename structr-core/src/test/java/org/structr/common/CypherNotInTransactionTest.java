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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import org.structr.common.error.FrameworkException;
import org.structr.core.TestRelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.Search;

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

			List<AbstractNode> testNodes = this.createTestNodes("UnknownTestType", 2);

			assertNotNull(testNodes);
			assertTrue(testNodes.size() == 2);

			AbstractRelationship rel = createRelationshipCommand.execute(testNodes.get(0), testNodes.get(1), TestRelType.ONE_TO_ONE);

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
	 * This test passes with Neo4j 1.8, but fails with 1.9.M02-M05
	 */
//	public void test02DeleteRelationshipAfterLookupWithCypherNotInTransaction() {
//
//		try {
//
//			List<AbstractNode> testNodes = this.createTestNodes("UnknownTestType", 2);
//
//			assertNotNull(testNodes);
//			assertTrue(testNodes.size() == 2);
//
//			AbstractRelationship rel = createRelationshipCommand.execute(testNodes.get(0), testNodes.get(1), TestRelType.ONE_TO_ONE);
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
	 * This test passes with Neo4j 1.8, but fails with 1.9.M02-M05
	 */
//	public void test03DeleteNodeAfterLookupWithCypherNotInTransaction() {
//
//		try {
//
//			List<AbstractNode> testNodes = this.createTestNodes("UnknownTestType", 1);
//
//			assertNotNull(testNodes);
//			assertTrue(testNodes.size() == 1);
//
//			GraphDatabaseService graphDb      = graphDbCommand.execute();
//			ExecutionEngine engine            = new ExecutionEngine(graphDb);
//			ExecutionResult result            = engine.execute("start n = node:keywordAllNodes(type = 'UnknownTestType') return n");
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
//				testNodes.get(0).getUuid();
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

			List<AbstractNode> testNodes = this.createTestNodes("UnknownTestType", 2);

			assertNotNull(testNodes);
			assertTrue(testNodes.size() == 2);

			AbstractRelationship rel = createRelationshipCommand.execute(testNodes.get(0), testNodes.get(1), TestRelType.ONE_TO_ONE);

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			
			Transaction tx                    = graphDb.beginTx();

			try {

				testNodes.get(0).getRelationships(Direction.BOTH).iterator().next().getRelationship().delete();
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

			List<AbstractNode> testNodes = this.createTestNodes("UnknownTestType", 2);

			assertNotNull(testNodes);
			assertTrue(testNodes.size() == 2);

			AbstractRelationship rel = createRelationshipCommand.execute(testNodes.get(0), testNodes.get(1), TestRelType.ONE_TO_ONE);

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			
			List<AbstractNode> searchRes = searchNodeCommand.execute(Search.andExactUuid(testNodes.get(0).getUuid())).getResults();
			
			assertTrue(searchRes.size() == 1);
			
			
			Transaction tx                    = graphDb.beginTx();

			try {

				searchRes.get(0).getRelationships(Direction.BOTH).iterator().next().getRelationship().delete();
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
