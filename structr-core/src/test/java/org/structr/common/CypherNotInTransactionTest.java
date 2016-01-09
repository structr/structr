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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.SixOneOneToOne;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.Tx;

/**
 *
 *
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

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			GraphDatabaseService graphDb      = graphDbCommand.execute();

			try (final Tx tx = app.tx()) {

				Result result                     = graphDb.execute("start n = node(*) match (n)<-[r:ONE_TO_ONE]-() return r");
				final Iterator<Relationship> rels = result.columnAs("r");

				assertTrue(rels.hasNext());

				rels.next().delete();

				tx.success();
			}

			try (final Tx tx = app.tx()) {

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

			try (final Tx tx = app.tx()) {

				rel = app.create(testSix, testOne, SixOneOneToOne.class);
				tx.success();
			}

			assertNotNull(rel);

			try (final Tx tx = app.tx()) {

				GraphObject  searchRes = app.getNodeById(testSix.getUuid());
				assertNotNull(searchRes);
			}

			try (final Tx tx = app.tx()) {

				testSix.getRelationships().iterator().next().getRelationship().delete();

				tx.success();
			}

			try (final Tx tx = app.tx()) {

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
