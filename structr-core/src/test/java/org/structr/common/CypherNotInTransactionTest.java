/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

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
import org.structr.core.graph.TransactionCommand;

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
			
			try (final TransactionCommand cmd = app.beginTx()) {
				
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			ExecutionEngine engine            = (ExecutionEngine) new ExecutionEngine(graphDb);
			
			try (final TransactionCommand cmd = app.beginTx()) {
				
				ExecutionResult result            = engine.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");
				final Iterator<Relationship> rels = result.columnAs("r");

				assertTrue(rels.hasNext());

				rels.next().delete();
				
				app.commitTx();
			}

			try (final TransactionCommand cmd = app.beginTx()) {
				
				String uuid = rel.getUuid();
				assertNull("UUID of deleted relationship should be null", uuid);
			} catch (IllegalStateException iex) {
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
		
	public void test03DeleteDirectly() {

		try {

			final TestOne testOne  = createTestNode(TestOne.class);
			final TestSix testSix  = createTestNode(TestSix.class);
			SixOneOneToOne rel     = null;

			assertNotNull(testOne);
			assertNotNull(testSix);

			try (final TransactionCommand cmd = app.beginTx()) {
				
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();
			Transaction tx                    = graphDb.beginTx();

			try (final TransactionCommand cmd = app.beginTx()) {
				
				testOne.getRelationships().iterator().next().getRelationship().delete();
				app.commitTx();
			}

			try (final TransactionCommand cmd = app.beginTx()) {
				
				String uuid = rel.getUuid();
				assertNull("UUID of deleted relationship should be null", uuid);
			} catch (IllegalStateException iex) {
				
			}

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

			try (final TransactionCommand cmd = app.beginTx()) {
				
				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				app.commitTx();
			}

			assertNotNull(rel);

			try (final TransactionCommand cmd = app.beginTx()) {
				
				GraphObject  searchRes = app.get(testSix.getUuid());
				assertNotNull(searchRes);
			}
			
			try (final TransactionCommand cmd = app.beginTx()) {
				
				testSix.getRelationships().iterator().next().getRelationship().delete();

				app.commitTx();
			}

			try (final TransactionCommand cmd = app.beginTx()) {
				
				String uuid = rel.getUuid();
				assertNull("UUID of deleted relationship should be null", uuid);
			} catch (IllegalStateException iex) {
				
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}	
}
